package com.moneytracker.vault.ai

import java.util.Locale

object OtpFirewall {

    // Non-bank retail/brand/biller senders whose SMS are coupons, promos, or duplicate biller receipts
    // (Real money movements are already reported by the user's Bank, Credit Card, or Regulated Wallet!)
    private val blockedNonBankSenderCodes = setOf(
        "ONTIRA", "ATTIRA", "BYTIRA", "TIRA",
        "SNITCH", "MYCLIQ", "MYNTRA", "AJIO", "NYKAA", "MEESHO", "LENSKT", "ZUDIO",
        "WOOHOO", "GYFTRR", "GYFTR",
        "GKIWIP", // Kiwi app bill-paid confirmation (duplicates Bank/UPI debit SMS)
        "JIOPAY", "JIOFBR", "JIOINF", "MYJIO", // Jio voucher promos & JioHome bill receipts (duplicates Bank/Card debit SMS)
        "HDBFSL", // HDB welcome & loan payment receipt (duplicates Bank EMI/UPI debit SMS)
        "RZRPAY"  // Razorpay checkout receipt (duplicates Bank/Card debit SMS)
    )

    private val explicitOtpDeliveryPatterns = listOf(
        Regex("""\b(\d{4,8})\s+is\s+(?:your\s+|the\s+)?(?:otp|one[\s-]?time[\s-]?password|verification\s+code|login\s+code|auth(?:entication)?\s+code|secret\s+code)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:otp|one[\s-]?time[\s-]?password|verification\s+code|security\s+code|login\s+code|passcode)\s*(?:is|:|-|for\s+txn|for\s+transaction|to\s+login|to\s+verify)?\s*(?:is\s*)?\b\d{4,8}\b""", RegexOption.IGNORE_CASE),
        Regex("""\buse\s+(?:code|otp)\s+[A-Za-z0-9]{4,12}\b""", RegexOption.IGNORE_CASE),
        Regex("""\bsecret\s+code\s*:\s*[A-Za-z0-9]+\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:cvv|mpin|upi\s*pin|atm\s*pin)\s*(?:is|:)\s*\d{3,6}\b""", RegexOption.IGNORE_CASE)
    )

    // Blocks store coupons, vouchers, cashback gift cards, e-mandate future warnings, card statements, and bill-due reminders
    private val promoCouponStatementOrFuturePatterns = listOf(
        // Coupons, promos, store credits & vouchers (e.g. Tira Rs.1100 off coupon, Snitch, Jio vouchers, CLiQ Cash)
        Regex("""\b(?:coupon|off\s+coupon|voucher|vouchers|e-gift\s+card|gift\s+card\s+of|cliq\s+cash|supercoin|supercoins|reward\s+points|loyalty\s+points|contest\s+prize|promotional|promotions\.)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:upto|up\s+to)\s*(?:₹|rs\.?|inr)\s*\d+""", RegexOption.IGNORE_CASE),
        Regex("""(?:₹|rs\.?|inr)\s*\d+\*?\s*off\b""", RegexOption.IGNORE_CASE),
        Regex("""\bredeem\s+on\s+min\b""", RegexOption.IGNORE_CASE),
        Regex("""\bvalidity\s+\d+\s*(?:hours?|hrs?|days?)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bvalid\s+only\s+for\s+\d+""", RegexOption.IGNORE_CASE),

        // Pre-debit E-Mandate / Future-tense notifications (NOT yet deducted!)
        Regex("""\b(?:will|shall)\s+be\s+(?:deducted|debited|charged|credited)\b""", RegexOption.IGNORE_CASE),
        Regex("""^e-mandate!""", RegexOption.IGNORE_CASE),
        Regex("""\bmandate\s+(?:created|registered|initiated|revoked|cancelled)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bcooling\s+period\s+limit\b""", RegexOption.IGNORE_CASE),

        // Credit Card Statements, Bill Due Reminders & Loan Welcome Messages
        Regex("""\bstatement\s+(?:is\s+sent|for\s+\d+|has\s+been\s+sent|is\s+ready|generated)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:total\s+due\s+of|min(?:imum)?\s+(?:amount\s+)?due|is\s+due\s+(?:on|tomorrow)|bill\s+of\s+rs\.?[\d,.]+\s+is\s+due|pay\s+now\s+on)\b""", RegexOption.IGNORE_CASE),
        Regex("""\brepayment\s+schedule\b""", RegexOption.IGNORE_CASE),

        // Duplicate Biller / Credit Card Bill Received / Loan / Merchant Order Confirmation Receipts
        // (Bank Debit SMS already records your CRED Club / Card Bill / EMI / Jio payment!)
        Regex("""\bpayment\s+of\b.*\b(?:has\s+been\s+)?received\s+(?:on|towards)\b.*\bcredit\s+card\b""", RegexOption.IGNORE_CASE),
        Regex("""\btowards\s+your\s+loan\s+account\b.*\bhas\s+been\s+received\b""", RegexOption.IGNORE_CASE),
        Regex("""\bbill\s+of\s+(?:₹|rs\.?|inr).*?\bhas\s+been\s+successfully\s+paid\s+via\b""", RegexOption.IGNORE_CASE),
        Regex("""\bfor\s+your\s+jiohome\s+connection\b""", RegexOption.IGNORE_CASE),
        Regex("""\bhas\s+been\s+successfully\s+captured\b""", RegexOption.IGNORE_CASE)
    )

    private val completedTransactionVerbPattern = Regex(
        """\b(?:debited|credited|spent|paid|sent|received|withdrawn|deducted|charged|purchase\s+of|purchase\s+worth|txn\s+of|transaction\s+of|trf\s+to|transferred)\b""",
        RegexOption.IGNORE_CASE
    )

    private val genericOtpMentionPattern = Regex(
        """\b(?:otp|one[\s-]?time[\s-]?password|verification\s+code|auth\s+code|login\s+code)\b""",
        RegexOption.IGNORE_CASE
    )

    private val failedTransactionPattern = Regex(
        """\b(?:declined|failed|could\s+not\s+be\s+processed|unsuccessful|insufficient\s+balance|insufficient\s+funds|blocked\s+due\s+to)\b""",
        RegexOption.IGNORE_CASE
    )

    data class FirewallDecision(
        val allowed: Boolean,
        val reason: String,
        val isOtpBlocked: Boolean = false
    )

    fun inspect(rawMessage: String, senderAddress: String = ""): FirewallDecision {
        val text = rawMessage.trim()
        if (text.length < 12) {
            return FirewallDecision(allowed = false, reason = "Message too short")
        }

        val upperSender = senderAddress.uppercase(Locale.US).removePrefix("DEMO-").trim()

        // 0. Block TRAI DLT Promotional (-P) messages immediately
        if (upperSender.endsWith("-P")) {
            return FirewallDecision(
                allowed = false,
                reason = "Promotional (-P) marketing SMS ignored"
            )
        }

        // Strip 2-char DLT operator prefix (e.g. AX-atTIRA-S -> ATTIRA)
        val coreSender = upperSender
            .replace(Regex("""^[A-Z]{2}-"""), "")
            .replace(Regex("""-[A-Z]$"""), "")

        if (blockedNonBankSenderCodes.any { coreSender.contains(it) }) {
            return FirewallDecision(
                allowed = false,
                reason = "Retail coupon or duplicate biller receipt ignored ($coreSender)"
            )
        }

        // 1. Block explicit OTP / secret code messages
        for (pattern in explicitOtpDeliveryPatterns) {
            if (pattern.containsMatchIn(text)) {
                return FirewallDecision(
                    allowed = false,
                    reason = "OTP / Verification code skipped",
                    isOtpBlocked = true
                )
            }
        }

        // 2. Block coupons, vouchers, pre-debit e-mandates, statements & bill-due alerts
        for (pattern in promoCouponStatementOrFuturePatterns) {
            if (pattern.containsMatchIn(text)) {
                return FirewallDecision(
                    allowed = false,
                    reason = "Coupon, statement, or pre-debit reminder ignored"
                )
            }
        }

        val hasCompletedVerb = completedTransactionVerbPattern.containsMatchIn(text)

        if (genericOtpMentionPattern.containsMatchIn(text) && !hasCompletedVerb) {
            return FirewallDecision(
                allowed = false,
                reason = "Authentication message skipped",
                isOtpBlocked = true
            )
        }

        if (failedTransactionPattern.containsMatchIn(text)) {
            return FirewallDecision(
                allowed = false,
                reason = "Declined or failed transaction ignored"
            )
        }

        if (!hasCompletedVerb) {
            return FirewallDecision(
                allowed = false,
                reason = "No completed transaction verb detected"
            )
        }

        return FirewallDecision(allowed = true, reason = "Valid financial transaction")
    }

    fun sanitizePreview(rawMessage: String): String {
        return rawMessage
            .replace(Regex("""https?://\S+""", RegexOption.IGNORE_CASE), "[link]")
            .replace(Regex("""(?:never|do\s+not|don't)\s+share\s+(?:your\s+)?(?:otp|pin|cvv|password).*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\b\d{6,16}\b""")) { match ->
                val v = match.value
                "••" + v.takeLast(4)
            }
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(180)
    }
}
