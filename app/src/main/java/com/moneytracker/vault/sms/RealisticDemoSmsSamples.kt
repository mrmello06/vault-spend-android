package com.moneytracker.vault.sms

object RealisticDemoSmsSamples {

    data class RawSmsSample(
        val sender: String,
        val body: String,
        val timestamp: Long
    )

    private const val HOUR = 3_600_000L
    private const val DAY = 24 * HOUR

    fun buildSamples(nowMillis: Long): List<RawSmsSample> {
        return listOf(
            // 1. User's exact GOOGLEONE 70 Rs example!
            RawSmsSample(
                sender = "DEMO-AD-HDFCBK",
                body = "Rs. 70.00 debited from A/c XX4821 on 18-11-26 to VPA googleone@okaxis. Info: UPI/P2M/632910441/GOOGLEONE. Never share OTP/PIN with anyone.",
                timestamp = nowMillis - 2 * HOUR
            ),
            // 2. OTP message (Should be BLOCKED by OtpFirewall and NEVER stored!)
            RawSmsSample(
                sender = "DEMO-AD-HDFCBK",
                body = "849201 is your OTP for txn of Rs. 2,499.00 at Amazon on HDFC Bank Card ending 9012. Valid for 5 mins. Do not share this OTP.",
                timestamp = nowMillis - 3 * HOUR
            ),
            // 3. Swiggy via corporate billing code BUNDL TECHNOLOGIES
            RawSmsSample(
                sender = "DEMO-VM-ICICIB",
                body = "ICICI Bank Acct XX7710 debited for Rs 465.00 on 17-Nov-26; BUNDL TECHNOLOGIES credited. UPI:6321889012@ybl.",
                timestamp = nowMillis - 9 * HOUR
            ),
            // 4. Zepto via corporate billing code KIRANAKART
            RawSmsSample(
                sender = "DEMO-AD-HDFCBK",
                body = "Sent Rs. 342.00 From HDFC Bank A/C *4821 To KIRANAKART TECHNOLOGIES via UPI Ref 4328119201 (zepto@okicici).",
                timestamp = nowMillis - 1 * DAY - 3 * HOUR
            ),
            // 5. Credit Card spend at Netflix
            RawSmsSample(
                sender = "DEMO-VM-SBICRD",
                body = "Rs. 649.00 spent on your SBI Credit Card ending 5529 at NETFLIX.COM on 16/11/26. Trxn Ref XX9921.",
                timestamp = nowMillis - 2 * DAY - 5 * HOUR
            ),
            // 6. Uber via UPI
            RawSmsSample(
                sender = "DEMO-JX-AXISBK",
                body = "INR 289.00 debited from A/c no. XX3319 on 15-11-26 at UBER INDIA SYSTEMS via UPI/P2M/99201122/UBER@axl.",
                timestamp = nowMillis - 3 * DAY - 2 * HOUR
            ),
            // 7. Domino's via corporate code JUBILANT FOODWORKS
            RawSmsSample(
                sender = "DEMO-AD-ONECRD",
                body = "Txn of INR 589.00 on OneCard Credit Card XX8044 at POS*JUBILANT FOODWORKS on 14-11-26. MCC 5814.",
                timestamp = nowMillis - 4 * DAY - 6 * HOUR
            ),
            // 8. Another OTP message (Blocked!)
            RawSmsSample(
                sender = "DEMO-VM-SBIINB",
                body = "Dear Customer, OTP for login to YONO SBI is 391044. Never share this verification code with anyone.",
                timestamp = nowMillis - 4 * DAY - 7 * HOUR
            ),
            // 9. IndianOil Fuel
            RawSmsSample(
                sender = "DEMO-VM-SBIPSG",
                body = "Your A/c X1104 is debited by Rs. 2,150.00 on 12Nov26 trf to IOCL PETROL PUMP Ref No 4312990812.",
                timestamp = nowMillis - 6 * DAY - 4 * HOUR
            ),
            // 10. Amazon Shopping on Credit Card
            RawSmsSample(
                sender = "DEMO-AD-HDFCBK",
                body = "Rs 3,499.00 spent on HDFC Bank Credit Card XX9012 at AMAZON PAY INDIA on 10-11-2026.",
                timestamp = nowMillis - 8 * DAY - 2 * HOUR
            ),
            // 11. BESCOM Electricity Utility Bill via CRED UPI
            RawSmsSample(
                sender = "DEMO-AD-HDFCBK",
                body = "Rs. 1,840.00 debited from A/c XX4821 to VPA bescom@credaxis Info: UPI/P2M/77123901/BESCOM.",
                timestamp = nowMillis - 10 * DAY - 5 * HOUR
            ),
            // 12. Local Pharmacy tested via Semantic AI Classifier
            RawSmsSample(
                sender = "DEMO-VM-ICICIB",
                body = "Rs. 620.00 paid to SRI BALAJI MEDICALS AND PHARMA from ICICI Bank A/c XX7710 via PhonePe @ybl.",
                timestamp = nowMillis - 11 * DAY - 3 * HOUR
            ),
            // 13. Zerodha / Mutual Fund SIP
            RawSmsSample(
                sender = "DEMO-AD-HDFCBK",
                body = "Rs. 5,000.00 debited from HDFC Bank A/c XX4821 towards INDIAN CLEARING CORP via UPI Mandate.",
                timestamp = nowMillis - 13 * DAY - 4 * HOUR
            ),
            // 14. Refund Credit (Tests Credit handling!)
            RawSmsSample(
                sender = "DEMO-AD-HDFCBK",
                body = "Rs. 499.00 credited to HDFC Bank Credit Card XX9012 as refund from MYNTRA DESIGNS on 04-11-26.",
                timestamp = nowMillis - 14 * DAY - 2 * HOUR
            ),
            // 15. Previous month transactions for multi-month trend bars!
            RawSmsSample(
                sender = "DEMO-VM-SBICRD",
                body = "Rs. 6,420.00 spent on your SBI Credit Card ending 5529 at INTERGLOBE AVIATION on 19/10/26.",
                timestamp = nowMillis - 30 * DAY
            ),
            RawSmsSample(
                sender = "DEMO-AD-HDFCBK",
                body = "Rs. 2,890.00 debited from A/c XX4821 on 12-10-26 to VPA avenue.dmart@okhdfcbank Info: UPI/P2M/551029/AVENUE SUPERMARTS.",
                timestamp = nowMillis - 37 * DAY
            ),
            RawSmsSample(
                sender = "DEMO-VM-ICICIB",
                body = "Rs. 1,299.00 spent on ICICI Bank Card XX7710 at SPOTIFYSI on 05-10-26.",
                timestamp = nowMillis - 44 * DAY
            ),
            RawSmsSample(
                sender = "DEMO-AD-ONECRD",
                body = "Txn of INR 4,250.00 on OneCard Credit Card XX8044 at ZUDIO TRENT LIMITED on 18-09-26.",
                timestamp = nowMillis - 61 * DAY
            ),
            RawSmsSample(
                sender = "DEMO-AD-HDFCBK",
                body = "Rs. 3,120.00 debited from A/c XX4821 on 20-08-26 at APOLLO PHARMACY via UPI @okaxis.",
                timestamp = nowMillis - 90 * DAY
            ),
            // 16. Third OTP (Blocked!)
            RawSmsSample(
                sender = "DEMO-AX-GOOGLE",
                body = "G-772910 is your Google verification code.",
                timestamp = nowMillis - 1 * HOUR
            )
        )
    }
}
