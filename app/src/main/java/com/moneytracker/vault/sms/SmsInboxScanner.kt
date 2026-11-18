package com.moneytracker.vault.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.moneytracker.vault.ai.IterativeLedgerAnalyzer
import com.moneytracker.vault.ai.MerchantKnowledgeBase
import com.moneytracker.vault.ai.OfflineExpenseAiEngine
import com.moneytracker.vault.data.local.TransactionEntity
import com.moneytracker.vault.data.local.VaultDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsInboxScanner {

    data class SyncResult(
        val scannedMessages: Int,
        val parsedTransactions: Int,
        val newlyInserted: Int,
        val trainingPasses: Int,
        val cardBillsCount: Int,
        val selfTransfersCount: Int,
        val merchantsResolvedCount: Int,
        val permissionMissing: Boolean = false
    )

    private const val PREFS_NAME = "vault_ai_training_stats"

    suspend fun scanInbox(context: Context, maxMessages: Int = 5000): SyncResult = withContext(Dispatchers.IO) {
        val hasReadSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasReadSms) {
            return@withContext SyncResult(0, 0, 0, 0, 0, 0, 0, permissionMissing = true)
        }

        MerchantKnowledgeBase.ensureInitialized(context)
        val dao = VaultDatabase.getInstance(context).vaultDao()
        val learnedMap = dao.getAllLearnedMerchants().associateBy {
            MerchantKnowledgeBase.normalizeForLookup(it.normalizedKey)
        }

        val parsedBatch = mutableListOf<TransactionEntity>()
        var scanned = 0

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext() && scanned < maxMessages) {
                scanned++
                val sender = cursor.getString(addrIdx).orEmpty()
                val body = cursor.getString(bodyIdx).orEmpty()
                val timestamp = cursor.getLong(dateIdx)

                when (val outcome = OfflineExpenseAiEngine.parseSms(sender, body, timestamp, learnedMap, context)) {
                    is OfflineExpenseAiEngine.AiParseOutcome.Parsed -> {
                        parsedBatch.add(outcome.transaction)
                    }
                    else -> Unit
                }
            }
        }

        val insertResults = if (parsedBatch.isNotEmpty()) {
            dao.insertTransactionsIgnoreDuplicates(parsedBatch)
        } else {
            emptyList()
        }
        val newCount = insertResults.count { it != -1L }

        val missingBodySet = dao.getFingerprintsMissingFullSmsBody().toHashSet()
        if (missingBodySet.isNotEmpty()) {
            for (txn in parsedBatch) {
                if (txn.smsFingerprint in missingBodySet && txn.fullSmsBody.isNotBlank()) {
                    dao.backfillFullSmsBody(txn.smsFingerprint, txn.fullSmsBody)
                }
            }
        }

        val training = IterativeLedgerAnalyzer.analyzeAndRefineLedger(dao)
        saveTrainingStats(context, scanned, training)

        SyncResult(
            scannedMessages = scanned,
            parsedTransactions = parsedBatch.size,
            newlyInserted = newCount,
            trainingPasses = training.passesExecuted,
            cardBillsCount = training.cardBillsIdentified,
            selfTransfersCount = training.selfTransfersIdentified,
            merchantsResolvedCount = training.merchantsResolved
        )
    }

    suspend fun injectDemoSmsDataset(context: Context): SyncResult = withContext(Dispatchers.IO) {
        MerchantKnowledgeBase.ensureInitialized(context)
        val dao = VaultDatabase.getInstance(context).vaultDao()
        val learnedMap = dao.getAllLearnedMerchants().associateBy {
            MerchantKnowledgeBase.normalizeForLookup(it.normalizedKey)
        }

        val samples = RealisticDemoSmsSamples.buildSamples(System.currentTimeMillis())
        val parsedBatch = mutableListOf<TransactionEntity>()

        for (sample in samples) {
            when (val outcome = OfflineExpenseAiEngine.parseSms(sample.sender, sample.body, sample.timestamp, learnedMap, context)) {
                is OfflineExpenseAiEngine.AiParseOutcome.Parsed -> parsedBatch.add(outcome.transaction)
                else -> Unit
            }
        }

        val insertIds = dao.insertTransactionsIgnoreDuplicates(parsedBatch)
        val newlyInserted = insertIds.count { it != -1L }
        val training = IterativeLedgerAnalyzer.analyzeAndRefineLedger(dao)
        saveTrainingStats(context, samples.size, training)

        SyncResult(
            scannedMessages = samples.size,
            parsedTransactions = parsedBatch.size,
            newlyInserted = newlyInserted,
            trainingPasses = training.passesExecuted,
            cardBillsCount = training.cardBillsIdentified,
            selfTransfersCount = training.selfTransfersIdentified,
            merchantsResolvedCount = training.merchantsResolved
        )
    }

    private fun saveTrainingStats(
        context: Context,
        scannedSms: Int,
        summary: IterativeLedgerAnalyzer.TrainingSummary
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt("scanned_sms", scannedSms)
            .putInt("passes", summary.passesExecuted)
            .putInt("card_bills", summary.cardBillsIdentified)
            .putInt("self_transfers", summary.selfTransfersIdentified)
            .putInt("resolved", summary.merchantsResolved)
            .apply()
    }

    fun getTrainingStatsLabel(context: Context): String {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val passes = p.getInt("passes", 0)
        val resolved = p.getInt("resolved", 0)
        val bills = p.getInt("card_bills", 0)
        val selfs = p.getInt("self_transfers", 0)
        if (passes == 0) return "AI Engine v6 Ready"
        return "AI Trained ($passes passes) • $resolved categorized • $bills Card Bills • $selfs Self Transfers"
    }
}
