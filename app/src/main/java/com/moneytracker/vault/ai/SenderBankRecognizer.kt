package com.moneytracker.vault.ai

import com.moneytracker.vault.domain.model.PaymentRail

object SenderBankRecognizer {

    data class InstitutionMeta(
        val bankName: String,
        val accountLast4: String,
        val paymentRail: PaymentRail,
        val upiApp: String
    )

    private val senderCodeToBank: List<Pair<String, String>> = listOf(
        "HDFCBK" to "HDFC Bank",
        "HDFCBN" to "HDFC Bank",
        "HDFCCC" to "HDFC Credit Card",
        "PAYZAP" to "PayZapp Wallet",
        "PNBCCD" to "PNB Credit Card",
        "SBICRD" to "SBI Card",
        "SBIINB" to "SBI",
        "SBIPSG" to "SBI",
        "SBIUPI" to "SBI",
        "SBIBNK" to "SBI",
        "CBSSBI" to "SBI",
        "ATMSBI" to "SBI",
        "ICICIB" to "ICICI Bank",
        "ICICIT" to "ICICI Bank",
        "ICICIC" to "ICICI Credit Card",
        "AXISBK" to "Axis Bank",
        "AXSBNK" to "Axis Bank",
        "AXISCC" to "Axis Credit Card",
        "KOTAKB" to "Kotak Bank",
        "KOTAKM" to "Kotak Bank",
        "IDFCFB" to "IDFC FIRST Bank",
        "IDFCBK" to "IDFC FIRST Bank",
        "YESBNK" to "Yes Bank",
        "YESBCM" to "Yes Bank",
        "INDUSB" to "IndusInd Bank",
        "BOBTXN" to "Bank of Baroda",
        "BOBSMS" to "Bank of Baroda",
        "BARODA" to "Bank of Baroda",
        "PNBSMS" to "PNB",
        "PUNBNB" to "PNB",
        "CANBNK" to "Canara Bank",
        "UNIONB" to "Union Bank",
        "FEDBNK" to "Federal Bank",
        "IOBCHN" to "Indian Overseas Bank",
        "IDBIBK" to "IDBI Bank",
        "BOIIND" to "Bank of India",
        "CENTBK" to "Central Bank",
        "MAHABK" to "Bank of Maharashtra",
        "UCOBNK" to "UCO Bank",
        "INDBNK" to "Indian Bank",
        "INDNBN" to "Indian Bank",
        "KVBANK" to "Karur Vysya Bank",
        "KARURV" to "Karur Vysya Bank",
        "CUBANK" to "City Union Bank",
        "SIBSMS" to "South Indian Bank",
        "KARNBN" to "Karnataka Bank",
        "BANDHN" to "Bandhan Bank",
        "AUBANK" to "AU Bank",
        "EQUITB" to "Equitas Bank",
        "UJJIVN" to "Ujjivan Bank",
        "RBLBNK" to "RBL Bank",
        "RBLCRD" to "RBL Card",
        "ONECRD" to "OneCard",
        "FPLCRD" to "OneCard",
        "AMEXIN" to "American Express",
        "HSBCIN" to "HSBC",
        "SCBANK" to "Standard Chartered",
        "CITIBK" to "Axis / Citi",
        "DBSBNK" to "DBS Bank",
        "SLICE" to "Slice",
        "UNICRD" to "Uni Card",
        "JUPITR" to "Jupiter",
        "FIBANK" to "Fi Money",
        "EPIFI" to "Fi Money",
        "SCAPIA" to "Scapia Card",
        "KIWICC" to "Kiwi Card",
        "PAYTMB" to "Paytm",
        "PAYTM" to "Paytm",
        "AMZPAY" to "Amazon Pay Wallet",
        "JUSPAY" to "Amazon Pay Wallet",
        "MOBIKW" to "MobiKwik Wallet",
        "FREECH" to "Freecharge Wallet",
        "IRSMSA" to "IRCTC RWallet",
        "PHONPE" to "PhonePe Wallet",
        "LZYPAY" to "LazyPay Wallet",
        "SIMPL" to "Simpl Wallet",
        "PLUXEE" to "Pluxee Wallet",
        "SODEXO" to "Pluxee Wallet",
        "JIOBNK" to "Jio Payments Bank",
        "AIRBNK" to "Airtel Payments Bank",
        "IPPB" to "India Post Bank"
    )

    private val bodyBankHints: List<Pair<String, String>> = listOf(
        "PNB CREDIT CARD" to "PNB Credit Card",
        "HDFC BANK CREDIT CARD" to "HDFC Credit Card",
        "ICICI BANK CREDIT CARD" to "ICICI Credit Card",
        "AXIS BANK CREDIT CARD" to "Axis Credit Card",
        "SBI CREDIT CARD" to "SBI Card",
        "SBI CARD" to "SBI Card",
        "PAYZAPP WALLET" to "PayZapp Wallet",
        "APAY WALLET" to "Amazon Pay Wallet",
        "AMAZON PAY BALANCE" to "Amazon Pay Wallet",
        "AMAZON PAY WALLET" to "Amazon Pay Wallet",
        "MOBIKWIK WALLET" to "MobiKwik Wallet",
        "PHONEPE GIFT CARD" to "PhonePe Wallet",
        "PHONEPE WALLET" to "PhonePe Wallet",
        "SWIGGY MONEY" to "Swiggy Money Wallet",
        "RWALLET" to "IRCTC RWallet",
        "PAYTM WALLET" to "Paytm Wallet",
        "FASTAG WALLET" to "FASTag Wallet",
        "HDFC BANK" to "HDFC Bank",
        "STATE BANK OF INDIA" to "SBI",
        "DEAR SBI USER" to "SBI",
        "-SBI" to "SBI",
        "SBI UPI" to "SBI",
        "ICICI BANK" to "ICICI Bank",
        "AXIS BANK" to "Axis Bank",
        "KOTAK BANK" to "Kotak Bank",
        "KOTAK" to "Kotak Bank",
        "IDFC FIRST" to "IDFC FIRST Bank",
        "INDUSIND" to "IndusInd Bank",
        "YES BANK" to "Yes Bank",
        "ONECARD" to "OneCard",
        "AMERICAN EXPRESS" to "American Express",
        "AMEX" to "American Express",
        "RBL BANK" to "RBL Bank",
        "BANK OF BARODA" to "Bank of Baroda",
        "CANARA BANK" to "Canara Bank",
        "FEDERAL BANK" to "Federal Bank",
        "UNION BANK" to "Union Bank",
        "PUNJAB NATIONAL" to "PNB"
    )

    private val accountLast4Patterns = listOf(
        Regex("""(?:a/c|ac|acct|account|card)\s*(?:no\.?|ending(?:\s+with)?|xx+|\*+|x+|:)?\s*[xX*#.-]*(\d{3,4})\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(\d{4})[xX*]{2,6}\b"""),
        Regex("""\b[xX*]{1,8}(\d{3,4})\b""")
    )

    fun analyze(senderAddress: String, body: String): InstitutionMeta {
        val cleanSender = senderAddress.uppercase()
            .removePrefix("DEMO-")
            .replace(Regex("""^[A-Z]{2}-"""), "")
            .replace(Regex("""-[A-Z]$"""), "")

        val upperBody = body.uppercase()

        val walletDetected = detectWalletName(cleanSender, upperBody)
        val bankFromBody = bodyBankHints.firstOrNull { (hint, _) ->
            upperBody.contains(hint)
        }?.second

        val bankFromSender = senderCodeToBank.firstOrNull { (code, _) ->
            cleanSender.contains(code)
        }?.second

        // Detect ICICI / HDFC / Axis Card vs Bank A/c accurately!
        var resolvedBank = walletDetected
            ?: bankFromBody
            ?: bankFromSender
            ?: if (cleanSender.isNotBlank() && cleanSender.any { it.isLetter() }) {
                cleanSender.take(12)
            } else {
                "Bank Account"
            }

        // Upgrade e.g. "ICICI Bank" + "ICICI Bank Card XX4006" (not Debit Card) -> "ICICI Credit Card"
        if (!upperBody.contains("DEBIT CARD")) {
            if (resolvedBank == "ICICI Bank" && upperBody.contains("ICICI BANK CARD")) {
                resolvedBank = "ICICI Credit Card"
            } else if (resolvedBank == "HDFC Bank" && upperBody.contains("HDFC BANK CARD")) {
                resolvedBank = "HDFC Credit Card"
            } else if (resolvedBank == "Axis Bank" && upperBody.contains("AXIS BANK CARD")) {
                resolvedBank = "Axis Credit Card"
            }
        }

        val last4 = extractLast4(body)
        val upiApp = detectUpiApp(body, cleanSender)
        val rail = detectPaymentRail(upperBody, resolvedBank, walletDetected != null, upiApp)

        return InstitutionMeta(
            bankName = resolvedBank,
            accountLast4 = last4,
            paymentRail = rail,
            upiApp = upiApp
        )
    }

    private fun detectWalletName(cleanSender: String, upperBody: String): String? {
        // Note: If Bank A/C sends money "To PayZapp Wallet" or "To Amazon Pay Balance", the sender is the BANK (not the wallet itself!)
        val isOutgoingFromBank = upperBody.contains("FROM HDFC BANK A/C") ||
            upperBody.contains("FROM KOTAK BANK") ||
            upperBody.contains("DEBITED A/C NO") ||
            upperBody.contains("DEBITED FROM A/C")
        if (isOutgoingFromBank) return null

        return when {
            upperBody.contains("PAYZAPP WALLET") || cleanSender.contains("PAYZAP") -> "PayZapp Wallet"
            upperBody.contains("APAY WALLET") || upperBody.contains("AMAZON PAY BALANCE") ||
                upperBody.contains("AMAZON PAY WALLET") || cleanSender.contains("JUSPAY") ||
                (cleanSender.contains("AMZPAY") && !upperBody.contains("UPI")) -> "Amazon Pay Wallet"
            upperBody.contains("MOBIKWIK WALLET") || cleanSender.contains("MOBIKW") -> "MobiKwik Wallet"
            upperBody.contains("PHONEPE WALLET") || upperBody.contains("PHONEPE GIFT CARD") ||
                upperBody.contains("UPI LITE") || cleanSender.contains("PHONPE") -> "PhonePe Wallet"
            upperBody.contains("SWIGGY MONEY") -> "Swiggy Money Wallet"
            upperBody.contains("RWALLET") || cleanSender.contains("IRSMSA") -> "IRCTC RWallet"
            upperBody.contains("PAYTM WALLET") || upperBody.contains("PAYTM GIFT") -> "Paytm Wallet"
            upperBody.contains("FREECHARGE") || cleanSender.contains("FREECH") -> "Freecharge Wallet"
            upperBody.contains("PLUXEE") || upperBody.contains("SODEXO") -> "Pluxee Wallet"
            upperBody.contains("SIMPL") && !upperBody.contains("SIMPLE") -> "Simpl Wallet"
            upperBody.contains("LAZYPAY") || cleanSender.contains("LZYPAY") -> "LazyPay Wallet"
            upperBody.contains("FASTAG WALLET") || upperBody.contains("FASTAG BALANCE") -> "FASTag Wallet"
            else -> null
        }
    }

    private fun extractLast4(body: String): String {
        for (pattern in accountLast4Patterns) {
            val match = pattern.find(body)
            if (match != null) {
                return match.groupValues[1].padStart(4, '0').takeLast(4)
            }
        }
        return "0000"
    }

    private fun detectUpiApp(body: String, cleanSender: String): String {
        val lower = body.lowercase()
        return when {
            lower.contains("@okaxis") || lower.contains("@okaxi") || lower.contains("@oksbi") ||
                lower.contains("@okhdfcbank") || lower.contains("@okhdfc") || lower.contains("@okicici") ||
                lower.contains("@okbizaxis") || lower.contains("gpay") || lower.contains("google pay") -> "Google Pay"
            lower.contains("@ybl") || lower.contains("@ibl") || lower.contains("@axl") ||
                (lower.contains("phonepe") && !lower.contains("gift card")) -> "PhonePe"
            lower.contains("@paytm") || lower.contains("@ptyes") || lower.contains("@pty") ||
                lower.contains("@ptaxis") || lower.contains("@ptsbi") || lower.contains("@pthdfc") ||
                lower.contains("paytmqr") || lower.contains("paytm.") -> "Paytm UPI"
            lower.contains("@apl") || lower.contains("@yapl") || lower.contains("@rapl") -> "Amazon Pay UPI"
            lower.contains("@cred") || lower.contains("cred.club") || lower.contains("dreamplug") ||
                (lower.contains("@axisb") && cleanSender.contains("CRED")) -> "CRED UPI"
            lower.contains("@waaxis") || lower.contains("@wahdfcbank") || lower.contains("@waicici") || lower.contains("@wasbi") -> "WhatsApp Pay"
            lower.contains("@superyes") || lower.contains("@fkaxis") -> "super.money UPI"
            lower.contains("@jupiteraxis") -> "Jupiter UPI"
            lower.contains("@naviaxis") -> "Navi UPI"
            lower.contains("@slice") -> "Slice UPI"
            lower.contains("bharatpe") || lower.contains("@fbpe") -> "BharatPe UPI"
            lower.contains("@pz") -> "PayZapp UPI"
            lower.contains("@yespay") -> "Merchant UPI"
            lower.contains("@sbi") || lower.contains("sbi upi") || lower.contains("dear upi user") -> "SBI / BHIM UPI"
            lower.contains("@upi") || lower.contains("bhim") -> "BHIM UPI"
            lower.contains("upi") || lower.contains("vpa ") -> "Bank UPI"
            else -> ""
        }
    }

    private fun detectPaymentRail(
        upperBody: String,
        bankName: String,
        isExplicitWallet: Boolean,
        upiApp: String
    ): PaymentRail {
        return when {
            isExplicitWallet || bankName.endsWith("Wallet") -> PaymentRail.WALLET
            upperBody.contains("ATM WDL") || upperBody.contains("CASH WITHDRAWAL") ||
                upperBody.contains("WITHDRAWN AT ATM") || upperBody.contains("WITHDRAWN FROM ATM") ||
                upperBody.contains("NFS*") || upperBody.contains("ATW-") -> PaymentRail.ATM_CASH
            upperBody.contains("DEBIT CARD") -> PaymentRail.DEBIT_CARD
            upperBody.contains("CREDIT CARD") || bankName.contains("Credit Card", ignoreCase = true) ||
                bankName in setOf("SBI Card", "OneCard", "American Express", "RBL Card", "Scapia Card", "Uni Card", "Kiwi Card") -> PaymentRail.CREDIT_CARD
            upperBody.contains("UPI") || upperBody.contains("VPA ") || upiApp.isNotEmpty() -> PaymentRail.UPI
            upperBody.contains("POS ") || upperBody.contains("POS*") || upperBody.contains("ECOM ") || upperBody.contains("ECOM*") -> PaymentRail.DEBIT_CARD
            upperBody.contains("CARD") && !upperBody.contains("PAN CARD") && !upperBody.contains("AADHAAR") && !upperBody.contains("GIFT CARD") -> PaymentRail.CREDIT_CARD
            else -> PaymentRail.BANK_TRANSFER
        }
    }
}
