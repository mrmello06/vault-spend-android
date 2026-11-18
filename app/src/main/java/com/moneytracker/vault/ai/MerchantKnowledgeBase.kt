package com.moneytracker.vault.ai

import android.content.Context
import com.moneytracker.vault.data.local.LearnedMerchantEntity
import com.moneytracker.vault.domain.model.PaymentRail
import com.moneytracker.vault.domain.model.SpendCategory
import org.json.JSONObject
import java.io.File
import java.util.Locale

object MerchantKnowledgeBase {

    data class MerchantEntry(
        val codePattern: String,
        val normalizedCompact: String,
        val normalizedSpaced: String,
        val displayName: String,
        val category: SpendCategory,
        val isSubscription: Boolean = false
    )

    data class QrAggregatorRule(
        val prefixes: List<String>,
        val displayName: String
    )

    data class MerchantResolution(
        val rawCode: String,
        val displayName: String,
        val category: SpendCategory,
        val confidence: Float,
        val reason: String,
        val isSubscription: Boolean
    )

    private val gatewayPrefixes = listOf(
        "RAZORPAY*", "RAZORPAY ", "RZPX*", "RZP*", "RSP*", "PAYU*", "PAYU ", "EPAY*",
        "PAYTM*", "PAYTMPAY*", "PAY*", "PHONEPE*", "BHARATPE*", "PINE LABS*", "PINELABS*",
        "BILLDESK*", "BILLDESK ", "CCAVENUE*", "CASHFREE*", "STRIPE*", "JUSPAY*",
        "SIMPL*", "POS*", "POS ", "ECOM*", "ECOM ", "IPS*", "VPS*", "SI*", "MANDATE*",
        "UPI-", "UPI:", "UPI/", "WWW.", "WWW "
    )

    @Volatile
    private var loadedCatalog: List<MerchantEntry>? = null

    @Volatile
    private var loadedQrRules: List<QrAggregatorRule>? = null

    @Synchronized
    fun ensureInitialized(context: Context? = null) {
        if (loadedCatalog != null) return

        val rawJson = try {
            if (context != null) {
                context.assets.open("merchant_intelligence_db.json").bufferedReader().use { it.readText() }
            } else {
                val candidates = listOf(
                    File("src/main/assets/merchant_intelligence_db.json"),
                    File("app/src/main/assets/merchant_intelligence_db.json")
                )
                candidates.firstOrNull { it.exists() }?.readText()
            }
        } catch (_: Exception) {
            null
        }

        if (!rawJson.isNullOrBlank()) {
            parseJsonDatabase(rawJson)
        } else {
            loadedCatalog = fallbackCoreCatalog()
            loadedQrRules = defaultQrRules()
        }
    }

    private fun parseJsonDatabase(jsonString: String) {
        val entries = mutableListOf<MerchantEntry>()
        val qrRules = mutableListOf<QrAggregatorRule>()

        try {
            val root = JSONObject(jsonString)
            val merchantsArr = root.optJSONArray("merchants")
            if (merchantsArr != null) {
                for (i in 0 until merchantsArr.length()) {
                    val obj = merchantsArr.getJSONObject(i)
                    val displayName = obj.getString("name")
                    val cat = SpendCategory.fromNameOrDefault(obj.getString("cat"))
                    val sub = obj.optBoolean("sub", false)
                    val codesArr = obj.getJSONArray("codes")
                    for (j in 0 until codesArr.length()) {
                        val code = codesArr.getString(j)
                        entries.add(
                            MerchantEntry(
                                codePattern = code,
                                normalizedCompact = normalizeCompactRaw(code),
                                normalizedSpaced = normalizeWithSpaces(code),
                                displayName = displayName,
                                category = cat,
                                isSubscription = sub
                            )
                        )
                    }
                }
            }

            val qrArr = root.optJSONArray("qr_aggregators")
            if (qrArr != null) {
                for (i in 0 until qrArr.length()) {
                    val obj = qrArr.getJSONObject(i)
                    val name = obj.getString("name")
                    val pArr = obj.getJSONArray("prefixes")
                    val prefixes = (0 until pArr.length()).map { pArr.getString(it).uppercase(Locale.US) }
                    qrRules.add(QrAggregatorRule(prefixes = prefixes, displayName = name))
                }
            }
        } catch (_: Exception) {
            // Fallback for JVM test org.json stub
        }

        if (entries.isEmpty()) {
            entries.addAll(parseJsonWithoutOrgJson(jsonString))
        }

        loadedCatalog = entries.sortedByDescending { it.normalizedCompact.length }
        loadedQrRules = qrRules.ifEmpty { defaultQrRules() }
    }

    private fun parseJsonWithoutOrgJson(jsonString: String): List<MerchantEntry> {
        val list = mutableListOf<MerchantEntry>()
        val lineRegex = Regex("""\{\s*"codes"\s*:\s*\[([^\]]+)\]\s*,\s*"name"\s*:\s*"([^"]+)"\s*,\s*"cat"\s*:\s*"([^"]+)"\s*,\s*"sub"\s*:\s*(true|false)\s*\}""")
        val strRegex = Regex(""""([^"]+)"""")
        for (match in lineRegex.findAll(jsonString)) {
            val codesBlock = match.groupValues[1]
            val name = match.groupValues[2]
            val cat = SpendCategory.fromNameOrDefault(match.groupValues[3])
            val sub = match.groupValues[4].toBoolean()
            for (codeMatch in strRegex.findAll(codesBlock)) {
                val code = codeMatch.groupValues[1]
                list.add(
                    MerchantEntry(
                        codePattern = code,
                        normalizedCompact = normalizeCompactRaw(code),
                        normalizedSpaced = normalizeWithSpaces(code),
                        displayName = name,
                        category = cat,
                        isSubscription = sub
                    )
                )
            }
        }
        return list.ifEmpty { fallbackCoreCatalog() }
    }

    private fun defaultQrRules(): List<QrAggregatorRule> = listOf(
        QrAggregatorRule(listOf("PAYTMQR", "PAYTM-", "PAYTM.", "PAYTM S"), "Paytm QR Merchant"),
        QrAggregatorRule(listOf("BHARATPE", "BP-"), "BharatPe Store"),
        QrAggregatorRule(listOf("GPAY-", "GOOGLEPAY-"), "Google Pay Store"),
        QrAggregatorRule(listOf("PHONEPEQR", "Q0", "Q1", "Q2", "Q3", "Q4", "Q5", "Q6", "Q7", "Q8", "Q9"), "PhonePe QR Store"),
        QrAggregatorRule(listOf("VYAPAR."), "Vyapar Store"),
        QrAggregatorRule(listOf("PINELABS", "EZETAP", "MSWIPE"), "Retail POS Store")
    )

    private fun fallbackCoreCatalog(): List<MerchantEntry> = listOf(
        "GOOGLEONE" to ("Google One" to SpendCategory.ENTERTAINMENT),
        "SMARTQ" to ("SmartQ Cafeteria" to SpendCategory.FOOD_DINING),
        "BUNDL TECHNOLOGIES" to ("Swiggy" to SpendCategory.FOOD_DINING),
        "SWIGGY" to ("Swiggy" to SpendCategory.FOOD_DINING),
        "ZOMATO" to ("Zomato" to SpendCategory.FOOD_DINING),
        "KIRANAKART" to ("Zepto" to SpendCategory.GROCERIES),
        "ZEPTO" to ("Zepto" to SpendCategory.GROCERIES),
        "BLINKIT" to ("Blinkit" to SpendCategory.GROCERIES),
        "CRED CLUB" to ("CRED Club" to SpendCategory.CREDIT_CARD_BILL)
    ).map { (code, pair) ->
        MerchantEntry(code, normalizeCompactRaw(code), normalizeWithSpaces(code), pair.first, pair.second, code == "GOOGLEONE")
    }

    private val semanticKeywordRules: List<Pair<SpendCategory, List<String>>> = listOf(
        SpendCategory.FOOD_DINING to listOf(
            "RESTAURANT", "RESTAURANTS", "CAFE", "COFFEE", "BAKERY", "BAKERS", "BAKES", "BAKE",
            "BAKEHOUSE", "BIRYANI", "DHABA", "PIZZA", "BURGER", "KITCHEN", "FOODS", "FOODAHOLIC",
            "FOOD", "CANTEEN", "MESS", "TIFFIN", "TIFFINS", "SWEETS", "MITHAI", "ICE CREAM",
            "GELATO", "JUICE", "TEA", "CHAI", "BISTRO", "DINER", "GRILL", "TANDOOR", "SHAWARMA",
            "MOMOS", "CAKE", "CAKES", "PATISSERIE", "CATERERS", "CATERING", "SNACKS", "DOSA",
            "IDLI", "THALI", "BHAVAN", "HOTEL", "HOTELS", "REFRESHMENTS", "EATERY"
        ),
        SpendCategory.GROCERIES to listOf(
            "SUPERMARKET", "SUPER MARKET", "SUPER", "HYPERMARKET", "KIRANA", "PROVISION",
            "PROVISIONS", "GROCERY", "GROCERIES", "MART", "VEGETABLES", "VEG", "FRUITS",
            "DAIRY", "MILK", "ORGANIC", "MEAT", "CHICKEN", "MUTTON", "FISH", "SEAFOOD",
            "DEPARTMENTAL", "FRESH", "DAILY NEEDS", "GENERAL STORE", "GENERAL STORES", "BHANDAR", "BAZAAR"
        ),
        SpendCategory.HEALTH_WELLNESS to listOf(
            "MEDICAL", "MEDICALS", "MEDIC", "MEDICINES", "PHARMA", "PHARMACY", "CHEMIST",
            "DRUGGIST", "HOSPITAL", "HOSPITALS", "CLINIC", "CLINICS", "DIAGNOSTIC", "DIAGNOSTICS",
            "PATHLAB", "PATHOLOGY", "LABS", "SCANS", "DENTAL", "DENTIST", "ORTHO", "EYE CARE",
            "OPTICAL", "OPTICALS", "GYM", "FITNESS", "YOGA", "PILATES", "SALON", "SPA",
            "PARLOUR", "BARBER", "WELLNESS", "AYURVEDA", "AYURVEDIC", "HOMEOPATHY"
        ),
        SpendCategory.FUEL_AUTO to listOf(
            "PETROL", "DIESEL", "FUEL", "FILLING STATION", "SERVICE STATION", "PETROLEUM",
            "AUTO", "AUTOMOBILES", "MOTORS", "GARAGE", "TYRES", "TIRES", "LUBRICANTS",
            "CAR WASH", "AUTO CARE", "EV CHARGE", "PUMP"
        ),
        SpendCategory.TRANSPORT_TRAVEL to listOf(
            "TRAVELS", "TOUR", "TOURS", "CABS", "CAB", "TAXI", "METRO", "AIRLINES", "AIRWAYS",
            "AVIATION", "RESORT", "RESORTS", "STAY", "STAYS", "LODGE", "INN", "TOLL",
            "PARKING", "RAILWAY", "RAILWAYS", "TRANSIT", "TRANSPORT", "TPT", "LOGISTICS", "MOVERS"
        ),
        SpendCategory.BILLS_UTILITIES to listOf(
            "ELECTRICITY", "ELECTRIC", "POWER", "DISCOM", "BROADBAND", "FIBER", "TELECOM",
            "RECHARGE", "GAS", "LPG", "CYLINDER", "WATER", "MUNICIPAL", "MAINTENANCE",
            "SOCIETY", "APARTMENT", "BBPS", "UTILITY", "CABLE", "INTERNET"
        ),
        SpendCategory.ENTERTAINMENT to listOf(
            "CINEMA", "CINEMAS", "MOVIES", "MULTIPLEX", "THEATRE", "THEATER", "GAMING",
            "ARCADE", "STREAM", "SUBSCRIPTION", "MUSIC", "CLUB", "LOUNGE", "PUB", "BAR",
            "BREWERY", "BOWLING", "TURF", "BADMINTON", "ARENA", "SPORTS CLUB"
        ),
        SpendCategory.EDUCATION to listOf(
            "SCHOOL", "COLLEGE", "UNIVERSITY", "ACADEMY", "INSTITUTE", "COACHING",
            "TUITION", "CLASSES", "BOOKS", "BOOK", "STATIONERY", "XEROX", "PRINTERS", "LIBRARY"
        ),
        SpendCategory.SHOPPING to listOf(
            "FASHION", "FASHIONS", "GARMENTS", "TEXTILES", "SILKS", "SAREE", "SAREES",
            "APPAREL", "CLOTHING", "BOUTIQUE", "TAILORS", "FOOTWEAR", "SHOES", "ELECTRONICS",
            "MOBILES", "MOBILE", "CELLULAR", "COMPUTERS", "LAPTOP", "JEWELLERS", "JEWELLERY",
            "GOLD", "SILVER", "FURNITURE", "FURNISHINGS", "HARDWARE", "ELECTRICALS", "PAINTS",
            "PLYWOOD", "GIFT", "GIFTS", "TOYS", "WATCHES", "EMPORIUM", "MALL", "RETAIL",
            "TRADERS", "ENTERPRISES", "ENTERPRISE", "STORES", "STORE", "SHOP", "COLLECTION"
        ),
        SpendCategory.INVESTMENTS_FINANCE to listOf(
            "SECURITIES", "BROKING", "MUTUAL FUND", "MUTUAL FUNDS", "INSURANCE", "ASSURANCE",
            "FINANCE", "FINSERV", "CAPITAL", "WEALTH", "CHIT", "GOLD LOAN", "CLEARING", "EMI"
        )
    )

    fun resolve(
        rawExtractedMerchant: String,
        fullSmsBody: String,
        mccCode: String?,
        paymentRail: PaymentRail,
        upiApp: String,
        learnedMap: Map<String, LearnedMerchantEntity>,
        context: Context? = null
    ): MerchantResolution {
        ensureInitialized(context)
        val catalog = loadedCatalog.orEmpty()
        val qrRules = loadedQrRules.orEmpty()

        val cleanedRaw = unwrapGatewayAndClean(rawExtractedMerchant)
        val normalizedKey = normalizeForLookup(cleanedRaw)

        // 0. Explicit Self-Transfer / Credit Card Bill / Own-Account phrases in SMS
        if (isExplicitSelfTransferText(fullSmsBody, cleanedRaw)) {
            return MerchantResolution(
                rawCode = cleanedRaw.ifBlank { "SELF TRANSFER" },
                displayName = if (fullSmsBody.contains("Credit Card", ignoreCase = true)) {
                    "Credit Card Bill Received"
                } else {
                    "Self Account Transfer"
                },
                category = SpendCategory.SELF_TRANSFER,
                confidence = 0.99f,
                reason = "Transfer between your accounts / cards",
                isSubscription = false
            )
        }

        // 1. ATM Cash shortcut
        if (paymentRail == PaymentRail.ATM_CASH || normalizedKey.contains("ATMWDL") || normalizedKey.startsWith("NFS")) {
            return MerchantResolution(
                rawCode = cleanedRaw.ifBlank { "ATM CASH" },
                displayName = "ATM Cash Withdrawal",
                category = SpendCategory.TRANSFERS,
                confidence = 0.98f,
                reason = "ATM Cash Withdrawal",
                isSubscription = false
            )
        }

        // 2. User Self-Learned Memory (Highest Priority)
        if (normalizedKey.isNotBlank()) {
            learnedMap[normalizedKey]?.let { learned ->
                return MerchantResolution(
                    rawCode = cleanedRaw,
                    displayName = learned.displayName,
                    category = SpendCategory.fromNameOrDefault(learned.category),
                    confidence = 1.0f,
                    reason = "Learned from your custom preference",
                    isSubscription = false
                )
            }
        }

        // 3. Exact & Substring Brand / Code Match against 750+ Curated Knowledge Base
        val candidatesToCheck = buildList {
            if (cleanedRaw.isNotBlank()) add(cleanedRaw)
            extractEmbeddedVpaUsername(fullSmsBody)?.let { vpaUser ->
                if (vpaUser.isNotBlank()) add(vpaUser)
            }
        }

        for (candidate in candidatesToCheck) {
            val candCompact = normalizeCompactRaw(candidate)
            val candSpaced = normalizeWithSpaces(candidate)
            if (candCompact.length < 2) continue

            for (entry in catalog) {
                val isMatch = if (entry.normalizedCompact.length <= 3) {
                    // Short codes like "VI", "PVR", "KFC", "OLA", "MMT" must match as whole tokens!
                    candCompact == entry.normalizedCompact || hasWholeTokenSequence(candSpaced, entry.normalizedSpaced)
                } else {
                    candCompact == entry.normalizedCompact ||
                        (entry.normalizedCompact.length >= 5 && candCompact.contains(entry.normalizedCompact)) ||
                        hasWholeTokenSequence(candSpaced, entry.normalizedSpaced)
                }

                if (isMatch) {
                    return MerchantResolution(
                        rawCode = cleanedRaw.ifBlank { entry.codePattern },
                        displayName = entry.displayName,
                        category = entry.category,
                        confidence = 0.99f,
                        reason = "Matched '${entry.codePattern}'",
                        isSubscription = entry.isSubscription
                    )
                }
            }
        }

        // 4. QR Aggregator VPA Detection (paytmqr..., paytm.s..., bharatpe..., q123456@ybl, gpay-...)
        val rawUpper = rawExtractedMerchant.uppercase(Locale.US)
        val cleanUpper = cleanedRaw.uppercase(Locale.US)
        for (qr in qrRules) {
            if (qr.prefixes.any { prefix ->
                    rawUpper.startsWith(prefix) || cleanUpper.startsWith(prefix) ||
                        rawUpper.contains(":$prefix") || rawUpper.contains("/$prefix")
                }
            ) {
                return MerchantResolution(
                    rawCode = cleanedRaw.ifBlank { qr.prefixes.first() },
                    displayName = qr.displayName,
                    category = SpendCategory.SHOPPING,
                    confidence = 0.85f,
                    reason = "Merchant QR payment (${qr.displayName})",
                    isSubscription = false
                )
            }
        }

        // Masked incoming VPA like "*16-3@okaxis" or "*06-4@okaxis" -> clean "UPI Transfer (*16-3)"
        if (rawExtractedMerchant.trim().startsWith("*") && cleanedRaw.matches(Regex("""^\d{1,4}[-\s]?\d{0,2}$"""))) {
            return MerchantResolution(
                rawCode = "VPA-${cleanedRaw.replace(" ", "-")}",
                displayName = "UPI Transfer (••${cleanedRaw.replace(" ", "")})",
                category = SpendCategory.TRANSFERS,
                confidence = 0.82f,
                reason = "Masked bank VPA transfer",
                isSubscription = false
            )
        }

        // 5. ISO 18245 MCC Lookup
        val mccInfo = MccDirectory.resolve(mccCode)
        if (mccInfo != null) {
            return MerchantResolution(
                rawCode = cleanedRaw.ifBlank { "MCC-${mccInfo.code}" },
                displayName = prettifyRawName(cleanedRaw.ifBlank { mccInfo.description }),
                category = mccInfo.category,
                confidence = 0.94f,
                reason = "MCC ${mccInfo.code} (${mccInfo.description})",
                isSubscription = false
            )
        }

        // 6. Fuzzy Bigram Similarity for truncated POS descriptors (e.g. GOOGLE DIGITA)
        if (normalizedKey.length >= 6) {
            var bestEntry: MerchantEntry? = null
            var bestScore = 0.0f
            for (entry in catalog) {
                if (entry.normalizedCompact.length < 6) continue
                val score = diceBigramSimilarity(normalizedKey, entry.normalizedCompact)
                if (score > bestScore) {
                    bestScore = score
                    bestEntry = entry
                }
            }
            if (bestEntry != null && bestScore >= 0.84f) {
                return MerchantResolution(
                    rawCode = cleanedRaw,
                    displayName = bestEntry.displayName,
                    category = bestEntry.category,
                    confidence = bestScore,
                    reason = "Fuzzy-matched '${bestEntry.codePattern}'",
                    isSubscription = bestEntry.isSubscription
                )
            }
        }

        // 7. Semantic Local Business Keyword & Morphology Classifier
        val spacedNormalized = normalizeWithSpaces(cleanedRaw)
        for ((category, keywords) in semanticKeywordRules) {
            val matchedKeyword = keywords.firstOrNull { kw ->
                hasWholeTokenSequence(spacedNormalized, kw) ||
                    (kw.length >= 6 && spacedNormalized.contains(kw))
            }
            if (matchedKeyword != null) {
                return MerchantResolution(
                    rawCode = cleanedRaw,
                    displayName = prettifyRawName(cleanedRaw),
                    category = category,
                    confidence = 0.88f,
                    reason = "Matched business keyword '$matchedKeyword'",
                    isSubscription = false
                )
            }
        }

        // 8. Direct Bank Account IFSC VPA (e.g. 836710110010112@bkid0008367.ifsc.npci) or Phone-Number UPI ID
        if (fullSmsBody.contains(".ifsc.npci", ignoreCase = true)) {
            val accDigits = cleanedRaw.filter { it.isDigit() }
            val suffix = accDigits.takeLast(4).ifBlank { "BANK" }
            return MerchantResolution(
                rawCode = "IFSC-$suffix",
                displayName = "Bank A/c Transfer ••$suffix",
                category = SpendCategory.TRANSFERS,
                confidence = 0.88f,
                reason = "Direct IFSC Bank Account UPI transfer",
                isSubscription = false
            )
        }

        val digitsOnly = cleanedRaw.replace(Regex("""-\d$"""), "").filter { it.isDigit() }
        if (digitsOnly.length in 9..16 && cleanedRaw.count { it.isLetter() } <= 2) {
            return MerchantResolution(
                rawCode = "UPI-${digitsOnly.takeLast(4)}",
                displayName = "UPI Contact ••${digitsOnly.takeLast(4)}",
                category = SpendCategory.TRANSFERS,
                confidence = 0.80f,
                reason = "Numeric UPI / Account transfer",
                isSubscription = false
            )
        }

        // 9. Person-to-Person Name (supports South Indian initials like PRANESH M, DIVAAGAR S R, KAMAL K, M ANBUCHEZHIYAN)
        if (looksLikePersonName(spacedNormalized)) {
            return MerchantResolution(
                rawCode = cleanedRaw,
                displayName = prettifyRawName(cleanedRaw),
                category = SpendCategory.TRANSFERS,
                confidence = 0.80f,
                reason = "Direct transfer to individual",
                isSubscription = false
            )
        }

        // 10. Clean Descriptive Fallback
        if (cleanedRaw.isNotBlank() && !cleanedRaw.equals("UNKNOWN", ignoreCase = true)) {
            return MerchantResolution(
                rawCode = cleanedRaw,
                displayName = prettifyRawName(cleanedRaw),
                category = SpendCategory.OTHERS,
                confidence = 0.60f,
                reason = "Extracted from SMS — tap to categorize",
                isSubscription = false
            )
        }

        // Check if it's an EMI debit ("Info: EMI ...")
        if (fullSmsBody.contains("Info: EMI", ignoreCase = true)) {
            return MerchantResolution(
                rawCode = "LOAN-EMI",
                displayName = "Bank Loan EMI",
                category = SpendCategory.INVESTMENTS_FINANCE,
                confidence = 0.90f,
                reason = "Auto-debit Loan EMI",
                isSubscription = true
            )
        }

        val railFallbackName = when {
            upiApp.isNotBlank() && upiApp != "Bank UPI" -> "$upiApp Transfer"
            paymentRail == PaymentRail.UPI -> "UPI Payment"
            paymentRail == PaymentRail.CREDIT_CARD -> "Credit Card Spend"
            paymentRail == PaymentRail.DEBIT_CARD -> "Debit Card Spend"
            paymentRail == PaymentRail.WALLET -> "Wallet Transfer"
            else -> "Bank Transfer"
        }

        return MerchantResolution(
            rawCode = paymentRail.badgeText,
            displayName = railFallbackName,
            category = SpendCategory.OTHERS,
            confidence = 0.45f,
            reason = "No merchant name included in SMS",
            isSubscription = false
        )
    }

    private fun isExplicitSelfTransferText(body: String, cleanedMerchant: String): Boolean {
        val upperBody = body.uppercase(Locale.US)
        val upperMerch = cleanedMerchant.uppercase(Locale.US)
        if (upperMerch in setOf("SELF", "SELF TRANSFER", "OWN ACCOUNT", "OWN AC")) return true

        // Received in bank by an A/C linked to own mobile or wallet settlement
        if (upperBody.contains("BY AN A/C LINKED TO MOBILE") ||
            upperBody.contains("/SELF TR") ||
            upperBody.contains("IMPS -MOBIKWIK-")
        ) {
            return true
        }

        val selfPhrases = listOf(
            "SELF TRANSFER", "TRF TO SELF", "TRANSFER TO SELF", "TO OWN A/C",
            "TO YOUR OWN", "SWEEP TO", "SWEEP FROM", "AUTOSWEEP", "AUTO-SWEEP"
        )
        return selfPhrases.any { upperBody.contains(it) }
    }

    private fun extractEmbeddedVpaUsername(body: String): String? {
        val m = Regex("""\b([*A-Za-z0-9._-]{3,40})@[A-Za-z]{2,15}\b(?!\.[A-Za-z]{2,4}\b)""").find(body) ?: return null
        return m.groupValues[1].trimStart('*')
    }

    fun unwrapGatewayAndClean(raw: String): String {
        var working = raw.trim()
        if (working.isBlank()) return ""

        // Strip Axis / HDFC / Kotak trailing fraud-warning suffixes ("Not you? SMS BLOCKUPI...", "Not done by you?...")
        working = working
            .replace(Regex("""\s+(?:not\s+you\??|not\s+done\s+by\s+you\??|if\s+not\s+you\b|sms\s+block).*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+for\s+(?:₹|rs\.?|inr)\s*[\d,.]+.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(\s*(?:upi|ref|txn|trxn).*""", RegexOption.IGNORE_CASE), "")
            .trim()

        if (working.contains("@")) {
            working = working.substringBefore("@")
        }

        working = working.trimStart('*')

        var upper = working.uppercase(Locale.US)
        var changed = true
        while (changed) {
            changed = false
            for (prefix in gatewayPrefixes) {
                if (upper.startsWith(prefix)) {
                    working = working.substring(prefix.length).trim()
                    upper = working.uppercase(Locale.US)
                    changed = true
                }
            }
        }

        working = working
            .replace("*", " ")
            .replace("_", " ")
            .replace(".", " ")
            .replace(Regex("""\b(?:PVT|LTD|LIMITED|PRIVATE|PRIVAT|LIMIT|LLP|INDIA|IND|BLR|BENGALURU|BANGALORE|MUM|MUMBAI|DEL|DELHI|GURGAON|GURUGRAM|NOIDA|HYD|HYDERABAD|CHN|CHENNAI|PUN|PUNE)\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', '/', ':', ';', ',', '.')

        return working
    }

    fun normalizeForLookup(input: String): String {
        return normalizeCompactRaw(unwrapGatewayAndClean(input))
    }

    private fun normalizeCompactRaw(input: String): String {
        return input.uppercase(Locale.US).replace(Regex("""[^A-Z0-9]"""), "")
    }

    private fun normalizeWithSpaces(input: String): String {
        return input
            .uppercase(Locale.US)
            .replace(Regex("""[^A-Z0-9]+"""), " ")
            .trim()
    }

    private fun hasWholeTokenSequence(haystackSpaced: String, needleSpaced: String): Boolean {
        if (haystackSpaced.isBlank() || needleSpaced.isBlank()) return false
        return " $haystackSpaced ".contains(" $needleSpaced ")
    }

    private fun looksLikePersonName(spacedUpper: String): Boolean {
        // Strip VPA username trailing digits/bank tags like "manbu1967 4" or "rahul02" or "magesh08032001"
        val cleaned = spacedUpper
            .replace(Regex("""\b([A-Z]{3,})\d{1,8}(?:\s+\d{1,2})?\b"""), "$1")
            .trim()
        val tokens = cleaned.split(" ").filter { it.isNotBlank() }
        if (tokens.size !in 1..4) return false

        // Must have at least one name word of length >= 3, and all tokens must be purely alphabetic (allowing 1-letter initials!)
        if (tokens.none { it.length >= 3 }) return false
        if (tokens.any { token -> !token.all { it.isLetter() } }) return false

        val nonPersonTokens = setOf(
            "STORE", "STORES", "ENTERPRISES", "ENTERPRISE", "TRADERS", "SERVICES", "SOLUTIONS",
            "PAY", "CORP", "SHOP", "AGENCY", "TECH", "TECHNOLOGIES", "SYSTEMS", "SYSTEM",
            "RETAIL", "ONLINE", "DIGITAL", "PAYMENT", "PAYMENTS", "UPI", "BANK", "CARD",
            "DIRECT", "SPEND", "MERCHANT", "CREDIT", "DEBIT", "WALLET", "CLUB", "SOCIETY", "CO"
        )
        return tokens.none { it in nonPersonTokens }
    }

    fun prettifyRawName(raw: String): String {
        val strippedDigitsSuffix = raw
            .replace(Regex("""(?<=[A-Za-z]{3})\d{1,8}(?:-\d{1,2})?$"""), "")
        val clean = strippedDigitsSuffix
            .replace(Regex("""[^A-Za-z0-9 &'.-]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (clean.isBlank()) return "Merchant"
        return clean.split(" ").joinToString(" ") { word ->
            if (word.length <= 2 && word.all { it.isUpperCase() || it.isDigit() }) {
                word
            } else {
                word.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
            }
        }
    }

    private fun diceBigramSimilarity(a: String, b: String): Float {
        if (a == b) return 1.0f
        if (a.length < 2 || b.length < 2) return 0.0f
        val bigramsA = (0 until a.length - 1).map { a.substring(it, it + 2) }
        val bigramsB = (0 until b.length - 1).map { b.substring(it, it + 2) }.toMutableList()
        var matches = 0
        for (bg in bigramsA) {
            val idx = bigramsB.indexOf(bg)
            if (idx >= 0) {
                matches++
                bigramsB.removeAt(idx)
            }
        }
        return (2.0f * matches) / (bigramsA.size + (b.length - 1))
    }
}
