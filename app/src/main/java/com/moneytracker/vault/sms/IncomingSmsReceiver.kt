package com.moneytracker.vault.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.moneytracker.vault.ai.IterativeLedgerAnalyzer
import com.moneytracker.vault.ai.MerchantKnowledgeBase
import com.moneytracker.vault.ai.OfflineExpenseAiEngine
import com.moneytracker.vault.data.local.VaultDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IncomingSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages.firstOrNull()?.displayOriginatingAddress.orEmpty()
        val combinedBody = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                MerchantKnowledgeBase.ensureInitialized(appContext)
                val dao = VaultDatabase.getInstance(appContext).vaultDao()
                val learnedMap = dao.getAllLearnedMerchants().associateBy {
                    MerchantKnowledgeBase.normalizeForLookup(it.normalizedKey)
                }

                when (val outcome = OfflineExpenseAiEngine.parseSms(sender, combinedBody, timestamp, learnedMap, appContext)) {
                    is OfflineExpenseAiEngine.AiParseOutcome.Parsed -> {
                        val txn = outcome.transaction
                        val nearby = dao.getTransactionsInWindow(txn.timestamp - 120_000L, txn.timestamp + 120_000L)
                        val isDuplicate = nearby.any { existing ->
                            OfflineExpenseAiEngine.isNearDuplicateTransaction(existing, txn)
                        }
                        if (!isDuplicate) {
                            dao.insertTransactionIgnoreDuplicate(txn)
                            IterativeLedgerAnalyzer.analyzeAndRefineLedger(dao)
                        }
                    }
                    else -> Unit
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
