package com.moneytracker.vault.ai

import com.moneytracker.vault.data.local.TransactionEntity
import com.moneytracker.vault.data.local.VaultDao
import com.moneytracker.vault.domain.model.PaymentRail
import com.moneytracker.vault.domain.model.SpendCategory
import com.moneytracker.vault.domain.model.TransactionType
import java.util.Locale
import kotlin.math.abs

/**
 * Multi-Iteration Convergence Self-Trainer & Ledger Intelligence Engine (v6).
 *
 * Trains iteratively across the user's entire SMS ledger until convergence (0 new updates):
 * - Strictly protects CREDIT_CARD_BILL (CRED Club, Kiwi, etc.) and verified retail brands from ever
 *   being misclassified as SELF_TRANSFER.
 * - Learns the device owner's human name(s) (e.g. RAHUL S, RAHUL SHARMA) and own masked VPA suffixes
 *   while blocking all brand/fintech names from owner-identity matching.
 * - Cross-resolves masked incoming HDFC VPAs (e.g. *ul02@ptyes -> Rahul, *am09@oksbi -> Vikram)
 *   against unmasked outgoing VPAs in the user's SMS history.
 */
object IterativeLedgerAnalyzer {

    data class TrainingSummary(
        val passesExecuted: Int,
        val totalTransactions: Int,
        val cardBillsIdentified: Int,
        val selfTransfersIdentified: Int,
        val merchantsResolved: Int
    )

    private const val PAIRING_WINDOW_MS = 5 * 60 * 1000L

    // Brand / Fintech / Non-person words that can NEVER be learned as the user's human name!
    private val nonHumanBrandTokens = setOf(
        "CRED", "CLUB", "KIWI", "GOKIWI", "CHEQ", "DREAMPLUG",
        "PAYTM", "PHONEPE", "GPAY", "GOOGLE", "AMAZON", "FLIPKART", "MYNTRA",
        "SWIGGY", "ZOMATO", "ZEPTO", "BLINKIT", "SMARTQ", "UBER", "OLA", "RAPIDO",
        "IRCTC", "BMTC", "NETFLIX", "SPOTIFY", "APPLE", "SAMSUNG", "PAYZAPP",
        "MOBIKWIK", "FREECHARGE", "GROWW", "ZERODHA", "UPSTOX", "HDFC", "ICICI",
        "AXIS", "KOTAK", "SBI", "PNB", "BANK", "CARD", "CREDIT", "DEBIT", "WALLET",
        "UPI", "TRANSFER", "PAYMENT", "MERCHANT", "STORE", "SHOP", "BILL", "LOAN", "EMI"
    )

    // Categories that represent real purchases or Credit Card Bills and must NEVER be overwritten to SELF_TRANSFER
    private val protectedNonSelfCategories = setOf(
        SpendCategory.CREDIT_CARD_BILL.name,
        SpendCategory.FOOD_DINING.name,
        SpendCategory.GROCERIES.name,
        SpendCategory.SHOPPING.name,
        SpendCategory.ENTERTAINMENT.name,
        SpendCategory.TRANSPORT_TRAVEL.name,
        SpendCategory.FUEL_AUTO.name,
        SpendCategory.BILLS_UTILITIES.name,
        SpendCategory.HEALTH_WELLNESS.name,
        SpendCategory.INVESTMENTS_FINANCE.name,
        SpendCategory.EDUCATION.name
    )

    suspend fun analyzeAndRefineLedger(dao: VaultDao): TrainingSummary {
        var passes = 0
        var passUpdates: Int

        do {
            passes++
            passUpdates = runSingleTrainingPass(dao)
        } while (passUpdates > 0 && passes < 5)

        val finalSnapshot = dao.getAllTransactionsSnapshot()
        return TrainingSummary(
            passesExecuted = passes,
            totalTransactions = finalSnapshot.size,
            cardBillsIdentified = finalSnapshot.count { it.category == SpendCategory.CREDIT_CARD_BILL.name },
            selfTransfersIdentified = finalSnapshot.count { it.isSelfTransfer || it.category == SpendCategory.SELF_TRANSFER.name },
            merchantsResolved = finalSnapshot.count { it.category != SpendCategory.OTHERS.name }
        )
    }

    private suspend fun runSingleTrainingPass(dao: VaultDao): Int {
        val all = dao.getAllTransactionsSnapshot()
        if (all.isEmpty()) return 0

        var updatesInPass = 0

        // 0. Build outgoing VPA suffix dictionary for resolving HDFC masked "*xxxx@handle" incoming VPAs!
        val unmaskedVpaSuffixToPerson = mutableMapOf<String, Pair<String, String>>() // suffix4 -> (displayName, category)
        val vpaRegex = Regex("""\b([A-Za-z][A-Za-z0-9._-]{3,35})@[A-Za-z]{2,15}\b""")
        for (txn in all) {
            if (txn.category == SpendCategory.OTHERS.name) continue
            val m = vpaRegex.find(txn.fullSmsBody) ?: continue
            val userPart = m.groupValues[1].lowercase(Locale.US)
            if (userPart.length >= 4 && !userPart.startsWith("*")) {
                val last4 = userPart.takeLast(4)
                unmaskedVpaSuffixToPerson.putIfAbsent(last4, txn.merchantDisplayName to txn.category)
            }
        }

        // Resolve any masked VPA rows (e.g. "*ul02@ptyes" -> last4 "ul02" -> matches "rahul02@ptyes" -> "Rahul")
        val maskedVpaRegex = Regex("""from\s+VPA\s+\*([A-Za-z0-9-]{3,6})@""", RegexOption.IGNORE_CASE)
        for (txn in all) {
            if (txn.isUserModified || txn.category != SpendCategory.OTHERS.name) continue
            val m = maskedVpaRegex.find(txn.fullSmsBody) ?: continue
            val maskSuffix = m.groupValues[1].lowercase(Locale.US).takeLast(4)
            val resolved = unmaskedVpaSuffixToPerson[maskSuffix] ?: continue
            val isSelf = resolved.second == SpendCategory.SELF_TRANSFER.name
            dao.updateSingleTransactionFull(
                transactionId = txn.id,
                displayName = resolved.first,
                category = resolved.second,
                note = txn.note,
                isIncludedInSpend = txn.isIncludedInSpend,
                isSelfTransfer = isSelf
            )
            // Keep isUserModified = 0 on AI-resolved rows so user edits remain distinct
            dao.resetUserModifiedFlag(txn.id)
            updatesInPass++
        }

        // 1. Pair eligible Bank/Wallet transfers (NEVER pairing CREDIT_CARD_BILL or retail purchases!)
        val ownAccountLast4s = all
            .map { it.accountLast4 }
            .filter { it.length == 4 && it != "0000" }
            .toSet()

        val eligibleDebits = all.filter {
            it.type == TransactionType.DEBIT.name &&
                !it.isUserModified &&
                it.category !in protectedNonSelfCategories
        }
        val eligibleCredits = all.filter {
            it.type == TransactionType.CREDIT.name &&
                !it.isUserModified &&
                it.category !in protectedNonSelfCategories
        }

        val selfTransferIds = mutableSetOf<Long>()
        val usedCreditIds = mutableSetOf<Long>()
        val selfPairNameCounts = mutableMapOf<String, Int>()

        for (txn in all) {
            if (txn.category in protectedNonSelfCategories) continue
            if (txn.fullSmsBody.contains("/Self Tr", ignoreCase = true) ||
                txn.sanitizedPreview.contains("/Self Tr", ignoreCase = true)
            ) {
                extractStrictHumanName(txn.merchantDisplayName)?.let { normName ->
                    selfPairNameCounts[normName] = (selfPairNameCounts[normName] ?: 0) + 3
                }
            }
        }

        for (debit in eligibleDebits.sortedBy { it.timestamp }) {
            val otherOwnDigits = ownAccountLast4s - debit.accountLast4
            if (otherOwnDigits.any { digits ->
                    debit.merchantRawCode.endsWith(digits) ||
                        debit.sanitizedPreview.contains("XX$digits", ignoreCase = true)
                }
            ) {
                selfTransferIds.add(debit.id)
            }

            val matchingCredit = eligibleCredits
                .filter { credit ->
                    credit.id !in usedCreditIds &&
                        abs(credit.amount - debit.amount) < 0.01 &&
                        abs(credit.timestamp - debit.timestamp) <= PAIRING_WINDOW_MS &&
                        (credit.bankName != debit.bankName ||
                            credit.accountLast4 != debit.accountLast4 ||
                            credit.paymentRail == PaymentRail.WALLET.name ||
                            debit.paymentRail == PaymentRail.WALLET.name) &&
                        !credit.sanitizedPreview.contains("refund", ignoreCase = true) &&
                        !credit.sanitizedPreview.contains("cashback", ignoreCase = true)
                }
                .minByOrNull { credit -> abs(credit.timestamp - debit.timestamp) }

            if (matchingCredit != null) {
                usedCreditIds.add(matchingCredit.id)
                selfTransferIds.add(debit.id)
                selfTransferIds.add(matchingCredit.id)

                extractStrictHumanName(debit.merchantDisplayName)?.let { name ->
                    selfPairNameCounts[name] = (selfPairNameCounts[name] ?: 0) + 1
                }
                extractStrictHumanName(matchingCredit.merchantDisplayName)?.let { name ->
                    selfPairNameCounts[name] = (selfPairNameCounts[name] ?: 0) + 1
                }
            }
        }

        // 2. Propagate Verified Human Account-Owner Name(s) (e.g., "RAHUL S", "RAHUL SHARMA")
        val verifiedOwnerNames = selfPairNameCounts
            .filterValues { it >= 2 }
            .keys
            .toSet()

        val ownerFirstAndInitial = verifiedOwnerNames.mapNotNull { full ->
            val parts = full.split(" ")
            if (parts.size >= 2 && parts[0].length >= 3) parts[0] to parts[1].first() else null
        }

        if (verifiedOwnerNames.isNotEmpty()) {
            for (txn in all) {
                if (txn.isUserModified || txn.isSelfTransfer || txn.category in protectedNonSelfCategories) continue
                val cand = extractStrictHumanName(txn.merchantDisplayName) ?: continue
                val isExactOwner = cand in verifiedOwnerNames
                val isExpandedOwner = run {
                    val parts = cand.split(" ")
                    parts.size == 2 && ownerFirstAndInitial.any { (first, initChar) ->
                        parts[0] == first && parts[1].startsWith(initChar)
                    }
                }
                val isCompactOwner = verifiedOwnerNames.any { owner ->
                    cand.replace(" ", "") == owner.replace(" ", "") ||
                        ownerFirstAndInitial.any { (first, initChar) ->
                            cand.startsWith(first) && cand.length > first.length && cand[first.length] == initChar
                        }
                }
                if (isExactOwner || isExpandedOwner || isCompactOwner) {
                    selfTransferIds.add(txn.id)
                }
            }
        }

        val newlyFlaggedSelfIds = selfTransferIds.filter { id ->
            val existing = all.firstOrNull { it.id == id }
            existing != null && !existing.isSelfTransfer && existing.category !in protectedNonSelfCategories
        }

        if (newlyFlaggedSelfIds.isNotEmpty()) {
            newlyFlaggedSelfIds.chunked(200).forEach { chunk ->
                dao.markIdsAsSelfTransfer(
                    ids = chunk,
                    reason = "Self transfer between your accounts"
                )
            }
            updatesInPass += newlyFlaggedSelfIds.size
        }

        // 3. Cross-SMS Merchant Entity Propagation
        val highConfidenceByCode: Map<String, TransactionEntity> = all
            .filter {
                it.category != SpendCategory.OTHERS.name &&
                    it.category != SpendCategory.SELF_TRANSFER.name &&
                    it.confidenceScore >= 0.88f &&
                    it.merchantRawCode.length >= 4
            }
            .groupBy { it.merchantRawCode.uppercase(Locale.US) }
            .mapValues { (_, list) -> list.maxBy { it.confidenceScore } }

        val unresolvedCodes = all
            .filter { !it.isUserModified && it.category == SpendCategory.OTHERS.name && it.merchantRawCode.length >= 4 }
            .map { it.merchantRawCode.uppercase(Locale.US) }
            .toSet()

        for (rawUpper in unresolvedCodes) {
            val resolved = highConfidenceByCode[rawUpper] ?: continue
            dao.propagateCrossLearnedMerchant(
                rawCode = rawUpper,
                resolvedName = resolved.merchantDisplayName,
                resolvedCategory = resolved.category
            )
            updatesInPass++
        }

        return updatesInPass
    }

    private fun extractStrictHumanName(displayName: String): String? {
        val upper = displayName.uppercase(Locale.US).trim()
        val words = upper.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.size !in 1..3) return null
        if (words.any { w -> !w.all { it.isLetter() } }) return null
        if (words.first().length < 3) return null
        if (words.any { it in nonHumanBrandTokens }) return null
        return words.joinToString(" ")
    }
}
