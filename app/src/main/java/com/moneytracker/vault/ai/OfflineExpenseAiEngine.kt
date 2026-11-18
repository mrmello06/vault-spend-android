package com.moneytracker.vault.ai

import android.content.Context
import com.moneytracker.vault.data.local.LearnedMerchantEntity
import com.moneytracker.vault.data.local.TransactionEntity
import com.moneytracker.vault.domain.model.SpendCategory
import com.moneytracker.vault.domain.model.TransactionType
import java.security.MessageDigest

object OfflineExpenseAiEngine {

    sealed class AiParseOutcome {
        data class BlockedOtp(val reason: String) : AiParseOutcome()
        data class IgnoredMessage(val reason: String) : AiParseOutcome()
        data class Parsed(val transaction: TransactionEntity) : AiParseOutcome()
    }

    private const val NUM_CAPTURE = """([0-9]{1,3}(?:,[0-9]{2,3})+(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)(?!\d)"""

    private val amountPatterns = listOf(
        Regex("""(?:₹|rs\.?|inr)\s*$NUM_CAPTURE""", RegexOption.IGNORE_CASE),
        Regex("""(?<!\d)$NUM_CAPTURE\s*(?:₹|rs\.?|inr)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:debited|credited|paid|sent|spent|deducted|withdrawn)\s+(?:by|with|for|of)\s*(?:₹|rs\.?|inr)?\s*$NUM_CAPTURE""", RegexOption.IGNORE_CASE)
    )

    private val mccRegex = Regex("""\bmcc\s*[:=-]?\s*(\d{4})\b""", RegexOption.IGNORE_CASE)

    // Calibrated against real HDFC, Kotak, Axis, ICICI, SBI, PNB, PayZapp, MobiKwik, Amazon Pay SMS formats
    private val merchantExtractionPatterns = listOf(
        // 1. 4+ segment UPI slash (stops cleanly before Axis "Not you?"): UPI/P2M/632910441/SmartQ Not you?
        Regex("""\bUPI/(?:P2M|P2A|DR|CR|INT|MOB|COLLECT)/[^/\s]+/([^/\n\r.;,()]+?)(?=/|\.|,|\(|\s+Not\s+you|\s+Ref|\s+UPI|\s+on\b|\s+Avl|\s*$)""", RegexOption.IGNORE_CASE),
        // 2. Kotak / IDFC 3-segment UPI slash where middle is merchant: UPI/Zomato/zomato@paytm
        Regex("""\bUPI/([A-Za-z][A-Za-z0-9 *._&-]{1,35}?)/[A-Za-z0-9._@-]+""", RegexOption.IGNORE_CASE),
        // 3. SBI 3-segment UPI slash where middle is RRN digits: UPI/431298765432/MERCHANT NAME
        Regex("""\bUPI/\d{6,16}/([^/\n\r.;,()]{2,38}?)(?=/|\.|,|\s+Not\s+you|\s+Bal|\s+Avl|\s+Ref|\s*$)""", RegexOption.IGNORE_CASE),
        // 4. ICICI / Axis colon or hyphen UPI: "-UPI:Zomato(UPI Ref..." or "Info: UPI-UBER INDIA."
        Regex("""\bUPI[-:]([A-Za-z][A-Za-z0-9 *._&'-]{1,38}?)(?=[-/(.;,]|\s+Not\s+you|\s+Ref|\s+UPI|\s+Bal|\s+Avl|\s*$)""", RegexOption.IGNORE_CASE),
        // 5. VPA with optional leading asterisk (e.g. HDFC "from VPA *hypg@yespay" or "to VPA googleone@okaxis")
        Regex("""\b(?:to|towards|from)\s+VPA\s+([*A-Za-z0-9._-]+@[A-Za-z0-9]+)(?:\s*\(([^)]+)\))?""", RegexOption.IGNORE_CASE),
        // 6. ICICI / Axis semicolon format (requires starting letter so "Rs.1000.00 credited" never matches!)
        Regex("""(?:;|\.\s+)\s*([A-Za-z][A-Za-z0-9 *._&'-]{2,42}?)\s+credited(?:\.|\s+UPI|\s*$)""", RegexOption.IGNORE_CASE),
        // 7. HDFC "From HDFC Bank A/C *1234 To <MERCHANT> On 17/11/26" OR Mandate "To <MERCHANT> 10/11/26"
        Regex("""\bFrom\s+[A-Za-z0-9 *xX#./-]{3,35}?\s+To\s+([A-Za-z0-9 *@._&'-]{2,42}?)(?=\s+On\s+\d|\s+\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\s+Ref|\s+UPI|\s+Not\s+You|\.\s|\.$|$)""", RegexOption.IGNORE_CASE),
        // 8. Kotak / SBI incoming: "Received Rs.188.00 in your Kotak Bank AC 1234 from PRIYA SHARMA on 08-11-26" or "transfer from RAHUL S Ref No"
        Regex("""\b(?:received\b.{0,65}?\bfrom|transfer\s+from|refund\s+from|credited\s+by)\s+([A-Za-z][A-Za-z0-9 *@._&'-]{1,38}?)(?=\s+on\s+\d|\s+Ref|\.\s*UPI|\s+UPI|\.\s|\.$|,|$)""", RegexOption.IGNORE_CASE),
        // 9. Info block (excluding bare EMI numbers handled separately)
        Regex("""\bInfo\s*:\s*(?:BIL/[A-Z]+/[A-Za-z0-9]+/)?(?!EMI\b)([A-Za-z][A-Za-z0-9 *@._&'-]{2,42}?)(?:\.|,|\s+Avl|\s+Bal|\s+UPI\s+Ref|\s+on\b|\s+Not\s+you|$)""", RegexOption.IGNORE_CASE),
        // 10. NEFT / IMPS / RTGS / ACH / NACH structured tags (supports both numeric & alphanumeric bank refs like NEFT/AUBLH26123/NAME)
        Regex("""\b(?:IMPS|MMT/IMPS|NEFT|RTGS)/(?:[A-Z0-9]+/)?[A-Za-z0-9]+/([^/\n\r.;,]{2,38})""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:through|by|via)\s+(?:ACH(?:\s*C)?|NACH|CMS|ECS(?:\s*CR)?|NEFT|RTGS)\s*[:/ \t-]+(?:[A-Z]{4}0?[A-Z0-9]{6,16}\s+)?([A-Za-z][A-Za-z0-9 .'& -]{2,42}?)(?=\.\s|\.$|,|\s+Avl|\s+Bal|$)""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:ACH|NACH|ECS)\s*(?:D|C|DR|CR)?[\s*-]+([A-Za-z][A-Za-z0-9 .'& -]{2,36}?)(?:[*-]\d|\.|,|\s+Avl|\s+Bal|\s+on\b|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bNEFT[*:-][A-Z0-9]{6,18}[*:\s-]+([A-Za-z][A-Za-z0-9 .'& -]{2,36}?)(?:[*:-]|\.|,|\s+Avl|\s+Bal|$)""", RegexOption.IGNORE_CASE),
        // 11. Axis "debited from A/c no. XX001234 on PAYTM UTILI 28-10-2026"
        Regex("""\bdebited\s+from\s+(?:a/c|acct)\b.{0,30}?\bon\s+([A-Za-z][A-Za-z0-9 *._&-]{2,35}?)(?=\s+\d{1,2}[-/]\d{1,2})""", RegexOption.IGNORE_CASE),
        // 12. SBI "trf to FOODAHOLIC Refno 4263..."
        Regex("""\b(?:trf|transfer|transferred)\s+to\s+([A-Za-z0-9 *@._&'-]{2,42}?)(?=\s+Ref|\s+Refno|\s+UPI|\s+on\s+\d|\s+If\s+not|\.\s|\.$|,|$)""", RegexOption.IGNORE_CASE),
        // 12. ICICI Card "spent using ICICI Bank Card XX1234 on 08-Nov-26 on AMAZON PAY IN G."
        Regex("""\bspent\b.{0,55}?\bon\s+\d{1,2}[-/][A-Za-z0-9]{2,4}[-/]\d{2,4}\s+(?:on|at)\s+([A-Za-z0-9 *._&'-]{2,40}?)(?=\.\s|\.$|,|\s+Avl|\s+Curr|\s+Ref|$)""", RegexOption.IGNORE_CASE),
        // 13. Card / POS / RuPay UPI "at GOOGLE*GOOGLE ONE on ..." or "at KAMAL K on ..."
        Regex("""\b(?:spent|used|charged|purchase|txn|transaction|debited|paid).{0,80}?\bat\s+([A-Za-z0-9 *._&'/-]{2,42}?)(?=\s+on\s+\d|\s+On\s+\d|\s+for\s+|\s+via\s+|\s+through\s+|\s+Ref|\s+Trxn|\s+TXN|\s+UPI|\s+Avl|\s+Bal|\s+Curr|\.\s|\.$|,|$)""", RegexOption.IGNORE_CASE),
        // 14. Generic "paid to / sent to / debited ... to / towards <MERCHANT>"
        Regex("""\b(?:paid\s+to|sent\s+to|debited\s+towards|(?:debited|sent|paid)\b.{0,70}?\bto)\s+([A-Za-z0-9 *@._&'-]{2,42}?)(?=\s+on\s+\d|\s+On\s+\d|\s+for\s+(?:₹|rs\.?|inr)|\s+at\s+\d|\s+Ref|\s+Refno|\s+UPI|\s+via\b|\s+from\b|\s+Avl|\s+Bal|\s+If\s+not|\s+Not\s+you|\s+Not\s+done|\.\s|\.$|,|$)""", RegexOption.IGNORE_CASE),
        // 15. Standalone UPI VPA (word boundary + negative lookahead prevents matching emails like @gmail.com or @mobikwik.com!)
        Regex("""\b([*A-Za-z0-9._-]{3,40}@[A-Za-z]{2,15})\b(?!\.[A-Za-z]{2,4}\b)""")
    )

    private val strongUserDebitRegex = Regex(
        """(?:\b(?:amt|amount)\s+sent\b|\bsent\s+(?:rs\.?|inr|₹|\d)|\b(?:spent|withdrawn|deducted|charged)\b|\b(?:purchase\s+(?:of|worth)|txn\s+of|transaction\s+of)\b|\b(?:a/c|ac|acct|account|card|wallet)\b[^.;]{0,35}?[-\s]debited\b|\bdebited\s+(?:from|by|with|for|via|towards)\b|\bhas\s+been\s+debited\b|\bis\s+debited\b|\bdebit\s+alert\b|\bdebit\s+of\b|\bpaid\s+(?:to|from|via|rs\.?|inr|₹)\b|\bcredited\s+to\s+(?:vpa\b|[a-z0-9*._-]+@)|\bautopay\b|\bmandate\s+executed\b)""",
        RegexOption.IGNORE_CASE
    )

    private val reversalOrRefundRegex = Regex(
        """\b(?:refund(?:ed)?|cashback|reversed|reversal\s+of|credited\s+back)\b""",
        RegexOption.IGNORE_CASE
    )

    private val strongUserCreditRegex = Regex(
        """(?:\bcredit\s+alert\b|\bpayment\s+of\b.{0,35}?\bhas\s+been\s+received\b|\b(?:a/c|ac|acct|account|card|wallet)\b[^.;]{0,35}?[-\s]credited\b|\bcredited\s+(?:to\s+(?:your\s+)?(?:a/c|ac|acct|account|card|wallet|\w+\s+bank)|with|by|in\s+your|\s*a/c\s+no)\b|\bhas\s+been\s+credited\b|\bis\s+credited\b|\bsalary\s+credited\b|\b(?:deposited|(?:received|rcvd)\s+(?:rs\.?|inr|₹|in\s+your|from)|money\s+received)\b)""",
        RegexOption.IGNORE_CASE
    )

    private val fallbackDebitWord = Regex("""\b(?:debited|debit|dr\.?|paid|sent|spent|trf\s+to)\b""", RegexOption.IGNORE_CASE)
    private val fallbackCreditWord = Regex("""\b(?:credited|credit|cr\.?|received)\b""", RegexOption.IGNORE_CASE)

    fun parseSms(
        senderAddress: String,
        body: String,
        timestampMillis: Long,
        learnedMap: Map<String, LearnedMerchantEntity> = emptyMap(),
        context: Context? = null
    ): AiParseOutcome {
        val singleLineBody = body
            .replace("\r\n", " ")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()

        // Stage 1: Sender + Coupon + Statement + Pre-Mandate + OTP Firewall
        val firewall = OtpFirewall.inspect(singleLineBody, senderAddress)
        if (!firewall.allowed) {
            return if (firewall.isOtpBlocked) {
                AiParseOutcome.BlockedOtp(firewall.reason)
            } else {
                AiParseOutcome.IgnoredMessage(firewall.reason)
            }
        }

        // Stage 2: Grammatical Direction & Exact Amount
        val direction = detectTransactionDirection(singleLineBody)
            ?: return AiParseOutcome.IgnoredMessage("Unable to determine debit/credit direction")

        val amount = extractPrimaryTransactionAmount(singleLineBody)
            ?: return AiParseOutcome.IgnoredMessage("No valid currency amount found")

        if (amount <= 0.0) {
            return AiParseOutcome.IgnoredMessage("Zero transaction amount")
        }

        // Stage 3: Institution, Account Last-4, Wallet & UPI App Detection
        val institution = SenderBankRecognizer.analyze(senderAddress, singleLineBody)

        // Stage 4: Raw Seller Code / VPA / MCC Extraction & Deep Resolution
        val mccCode = mccRegex.find(singleLineBody)?.groupValues?.getOrNull(1)
        val rawMerchant = extractRawMerchant(singleLineBody)

        val resolution = MerchantKnowledgeBase.resolve(
            rawExtractedMerchant = rawMerchant,
            fullSmsBody = singleLineBody,
            mccCode = mccCode,
            paymentRail = institution.paymentRail,
            upiApp = institution.upiApp,
            learnedMap = learnedMap,
            context = context
        )

        val isSelfTransfer = resolution.category == SpendCategory.SELF_TRANSFER ||
            hasDualOwnAccountTransfer(singleLineBody)

        val salaryResolution = if (direction == TransactionType.CREDIT && !isSelfTransfer) {
            detectVerifiedSalaryCredit(
                body = singleLineBody,
                amount = amount,
                paymentRail = institution.paymentRail,
                extractedDisplayName = resolution.displayName,
                learnedOverridePresent = learnedMap.containsKey(MerchantKnowledgeBase.normalizeForLookup(rawMerchant))
            )
        } else null

        val finalCategory = when {
            isSelfTransfer -> SpendCategory.SELF_TRANSFER
            salaryResolution != null -> SpendCategory.SALARY
            else -> resolution.category
        }
        val finalDisplayName = when {
            isSelfTransfer && resolution.category != SpendCategory.SELF_TRANSFER -> "Self Account Transfer"
            salaryResolution != null -> salaryResolution
            else -> resolution.displayName
        }

        val fingerprint = computeSmsFingerprint(
            timestampMillis = timestampMillis,
            senderAddress = senderAddress,
            singleLineBody = singleLineBody
        )

        val entity = TransactionEntity(
            smsFingerprint = fingerprint,
            amount = amount,
            type = direction.name,
            merchantRawCode = if (salaryResolution != null && resolution.rawCode.length < 3) "SALARY" else resolution.rawCode,
            merchantDisplayName = finalDisplayName,
            category = finalCategory.name,
            confidenceScore = if (isSelfTransfer || salaryResolution != null) 0.99f else resolution.confidence,
            matchReason = when {
                isSelfTransfer -> resolution.reason
                salaryResolution != null -> "Verified corporate salary / payroll credit"
                else -> resolution.reason
            },
            bankName = institution.bankName,
            accountLast4 = institution.accountLast4,
            paymentRail = institution.paymentRail.name,
            upiApp = institution.upiApp,
            mccCode = mccCode,
            isSubscription = resolution.isSubscription && !isSelfTransfer,
            timestamp = timestampMillis,
            senderAddress = senderAddress,
            sanitizedPreview = OtpFirewall.sanitizePreview(singleLineBody),
            isSelfTransfer = isSelfTransfer,
            parserVersion = 7,
            fullSmsBody = singleLineBody
        )

        return AiParseOutcome.Parsed(entity)
    }

    private val explicitSalaryKeywordRegex = Regex(
        """\b(?:salary|sal\s+cr|by[\s_-]?salary|payroll|stipend|monthly\s+wages|sal\s+for|towards\s+(?:neft[\s_-]*)?salary)\b""",
        RegexOption.IGNORE_CASE
    )

    private val bulkCorporatePayrollRailRegex = Regex(
        """\b(?:ACH\s*C\b|ACH\s*-\s*[A-Za-z]|NACH[-/\s]|CMS[-/\s]|ECS\s*CR\b|BULK\s+POSTING)\b""",
        RegexOption.IGNORE_CASE
    )

    private val corporateEntityMarkerRegex = Regex(
        """\b(?:PVT\.?\s*LTD|PRIVATE\s+LIMITED|LIMITED|LTD|LLP|INC|CORP|CORPORATION|TECHNOLOGIES|TECHNOLOGY|SOLUTIONS|SYSTEMS|SERVICES|INFOTECH|CONSULTANCY|CONSULTING|ENTERPRISES|GLOBAL)\b""",
        RegexOption.IGNORE_CASE
    )

    private val nonSalaryCreditDisqualifiersRegex = Regex(
        """\b(?:refund(?:ed)?|cashback|reversal|reversed|dividend|div\s+cr|ach\s*c[- ]*div|interest|int\.?\s+pd|fd\s+maturity|rd\s+maturity|mutual\s+fund|redemption|ncl|iccl|cams|kfintech|zerodha|groww|upstox|claim|insurance|loan|disburs|cash\s+deposit|cdm|atm)\b""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Precision Salary Credit Classifier calibrated on HDFC, ICICI, SBI, Axis, Kotak, IDFC & TRAI SMS templates:
     * - Never selects random UPI or casual P2P credits.
     * - Requires explicit payroll markers (SALARY / PAYROLL / STIPEND / BY SALARY / SAL CR) OR
     *   verified corporate bulk clearing (ACH C- / NACH / CMS) from a registered corporate entity without dividend/refund/MF tags.
     */
    private fun detectVerifiedSalaryCredit(
        body: String,
        amount: Double,
        paymentRail: com.moneytracker.vault.domain.model.PaymentRail,
        extractedDisplayName: String,
        learnedOverridePresent: Boolean
    ): String? {
        if (learnedOverridePresent) return null
        if (amount < 1000.0) return null
        if (nonSalaryCreditDisqualifiersRegex.containsMatchIn(body)) return null

        val hasExplicitSalaryWord = explicitSalaryKeywordRegex.containsMatchIn(body)
        // P2P UPI handles (@okaxis, @ybl, UPI/) are never corporate payroll unless the bank explicitly tagged SALARY in narrative
        val hasCasualUpiMarker = paymentRail == com.moneytracker.vault.domain.model.PaymentRail.UPI ||
            body.contains("UPI/", ignoreCase = true) ||
            Regex("""@[A-Za-z]{2,15}\b""").containsMatchIn(body)

        if (hasCasualUpiMarker && !hasExplicitSalaryWord) return null

        val isCorporateBulkPayroll = !hasCasualUpiMarker &&
            amount >= 8000.0 &&
            bulkCorporatePayrollRailRegex.containsMatchIn(body) &&
            corporateEntityMarkerRegex.containsMatchIn(body)

        if (!hasExplicitSalaryWord && !isCorporateBulkPayroll) return null

        // Format a clean employer / salary display title
        val cleanedEmployer = extractedDisplayName
            .replace(Regex("""(?i)[-\s]*(?:salary(?:\s+[a-z]{3,9})?|payroll|sal\s*cr|by\s*salary)\b.*"""), "")
            .replace(Regex("""(?i)^(?:ach\s*c?[-\s]*|nach[-\s]*|neft[-\s]*|cms[-\s]*)"""), "")
            .trim()

        val genericNames = setOf(
            "BANK TRANSFER", "UPI PAYMENT", "DIRECT SPEND", "SALARY", "MONTHLY",
            "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER",
            "JAN", "FEB", "MAR", "APR", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
        )

        return if (cleanedEmployer.length >= 3 &&
            cleanedEmployer.uppercase() !in genericNames &&
            !cleanedEmployer.startsWith("UPI ", ignoreCase = true) &&
            cleanedEmployer.any { it.isLetter() }
        ) {
            MerchantKnowledgeBase.prettifyRawName(cleanedEmployer)
        } else {
            "Salary Credit"
        }
    }

    private fun detectTransactionDirection(body: String): TransactionType? {
        if (reversalOrRefundRegex.containsMatchIn(body) && !body.contains("debited", ignoreCase = true)) {
            return TransactionType.CREDIT
        }
        if (reversalOrRefundRegex.containsMatchIn(body) && strongUserCreditRegex.containsMatchIn(body)) {
            return TransactionType.CREDIT
        }

        val userDebitMatch = strongUserDebitRegex.find(body)
        val userCreditMatch = strongUserCreditRegex.find(body)

        if (userDebitMatch != null && userCreditMatch == null) {
            return TransactionType.DEBIT
        }
        if (userCreditMatch != null && userDebitMatch == null) {
            return TransactionType.CREDIT
        }
        if (userDebitMatch != null && userCreditMatch != null) {
            return if (userDebitMatch.range.first <= userCreditMatch.range.first) {
                TransactionType.DEBIT
            } else {
                TransactionType.CREDIT
            }
        }

        val fbDebit = fallbackDebitWord.find(body)
        val fbCredit = fallbackCreditWord.find(body)
        return when {
            fbDebit != null && fbCredit != null ->
                if (fbDebit.range.first <= fbCredit.range.first) TransactionType.DEBIT else TransactionType.CREDIT
            fbDebit != null -> TransactionType.DEBIT
            fbCredit != null -> TransactionType.CREDIT
            else -> null
        }
    }

    private fun extractPrimaryTransactionAmount(body: String): Double? {
        val balancePrefixRegex = Regex(
            """(?:avl|avail|available|curr|current|clear|updated|closing|remaining|total|min|minimum)?\s*(?:bal|balance|lmt|limit|due)\s*(?:is|:|-|rs\.?|inr)?\s*$""",
            RegexOption.IGNORE_CASE
        )

        for (pattern in amountPatterns) {
            for (match in pattern.findAll(body)) {
                val prefixStart = (match.range.first - 28).coerceAtLeast(0)
                val precedingText = body.substring(prefixStart, match.range.first)
                if (balancePrefixRegex.containsMatchIn(precedingText)) {
                    continue
                }
                val numeric = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (numeric != null && numeric > 0.0) {
                    return numeric
                }
            }
        }
        return null
    }

    private fun extractRawMerchant(body: String): String {
        val invalidExtractions = setOf(
            "A/C", "AC", "ACCT", "ACCOUNT", "UPI", "VPA", "YOUR", "SELF", "BANK",
            "CARD", "POS", "IMPS", "NEFT", "RTGS", "INR", "RS"
        )

        for (pattern in merchantExtractionPatterns) {
            for (match in pattern.findAll(body)) {
                val group2 = match.groupValues.getOrNull(2)?.trim().orEmpty()
                // Ignore parenthetical "(UPI 664083728809)" or "(Ref 12345)" so we keep the real VPA in group 1!
                val isGroup2RealName = group2.length >= 2 &&
                    !group2.startsWith("UPI", ignoreCase = true) &&
                    !group2.startsWith("Ref", ignoreCase = true) &&
                    !group2.startsWith("Txn", ignoreCase = true) &&
                    group2.count { it.isDigit() } < 5
                val candidate = if (isGroup2RealName) group2 else match.groupValues[1].trim()

                val cleaned = candidate
                    .replace(Regex("""^(?:VPA|A/C|AC|ACCOUNT|UPI[-:/])\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s*(?:UPI\s*Ref|Ref\s*No|Refno|Not\s+you).*$""", RegexOption.IGNORE_CASE), "")
                    .trim(' ', '.', ',', '-', ':', ';', '(', ')')

                val upperCleaned = cleaned.uppercase()
                if (cleaned.length >= 2 &&
                    upperCleaned !in invalidExtractions &&
                    !upperCleaned.matches(Regex("""^[X*#]+\d+$""")) &&
                    !upperCleaned.matches(Regex("""^\d+(?:[.\s]\d+)?$"""))
                ) {
                    return cleaned
                }
            }
        }
        return ""
    }

    private fun hasDualOwnAccountTransfer(body: String): Boolean {
        val m = Regex(
            """debited\s+from\s+(?:a/c|acct|account)\s*[xX*#.-]*(\d{3,4})\b.{0,45}?\bcredited\s+to\s+(?:a/c|acct|account)\s*[xX*#.-]*(\d{3,4})\b""",
            RegexOption.IGNORE_CASE
        ).find(body) ?: return false
        return m.groupValues[1] != m.groupValues[2]
    }

    private fun computeSmsFingerprint(
        timestampMillis: Long,
        senderAddress: String,
        singleLineBody: String
    ): String {
        val bucket = timestampMillis / 5_000L
        val cleanSender = senderAddress.uppercase().takeLast(6)
        val raw = "$bucket|$cleanSender|${singleLineBody.lowercase().take(140)}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }
}
