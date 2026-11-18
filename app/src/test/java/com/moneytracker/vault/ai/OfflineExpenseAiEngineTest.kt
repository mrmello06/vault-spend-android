package com.moneytracker.vault.ai

import com.moneytracker.vault.data.local.LearnedMerchantEntity
import com.moneytracker.vault.domain.model.PaymentRail
import com.moneytracker.vault.domain.model.SpendCategory
import com.moneytracker.vault.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineExpenseAiEngineTest {

    @Test
    fun `blocks Tira coupons, Snitch promos, E-Mandate pre-debits, Credit Card statements, and Kiwi duplicate receipts`() {
        val tiraCoupon = "Rs.1100 off coupon credited in your Tira account! secret code: PAYDAY Valid only for 6 hours! promotions.tirabeauty.com"
        val tiraUpto = "ALERT: Upto Rs.1100 off credited Valid only for 6 hours! Use code EXTRAPAY on Tira"
        val snitchPromo = "Transaction Alert! INR 600 has been credited to your SNITCH account! redeem on min Rs 2,399"
        val eMandateFuture = "E-Mandate! Rs.5000.00 will be deducted on 10/11/26, 00:00:00 For MUTUAL FUNDS NCL AUTOPAY mandate UMN 01a0dd"
        val iciciStatement = "ICICI Bank Credit Card XX1234 Statement is sent to user******01@gmail.com. Total of Rs 747.37 or minimum of Rs 100.00"
        val kiwiDuplicate = "Your HDFC Bank Credit Card XXXX-5678 bill of Rs. 18767.0 has been successfully paid via Kiwi."

        assertFalse(OfflineExpenseAiEngine.parseSms("JX-atTIRA-S", tiraCoupon, 1L) is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        assertFalse(OfflineExpenseAiEngine.parseSms("VA-onTIRA-S", tiraUpto, 2L) is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        assertFalse(OfflineExpenseAiEngine.parseSms("AD-SNITCH-P", snitchPromo, 3L) is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        assertFalse(OfflineExpenseAiEngine.parseSms("AD-HDFCBK-S", eMandateFuture, 4L) is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        assertFalse(OfflineExpenseAiEngine.parseSms("BP-ICICIT-S", iciciStatement, 5L) is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        assertFalse(OfflineExpenseAiEngine.parseSms("JX-GKIWIP-S", kiwiDuplicate, 6L) is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
    }

    @Test
    fun `classifies CRED Club as CREDIT_CARD_BILL (not SELF_TRANSFER) and drops duplicate Credit Card receipts`() {
        val credDebit = "Sent Rs.5944.00 From HDFC Bank A/C *4821 To CRED Club On 05/11/26 Ref 12340936 Not You? Call 18002586161"
        val cardCreditReceipt = "Payment of Rs 5,969.00 has been received on your ICICI Bank Credit Card XX1234 through Bharat Bill Payment System"

        val out1 = (OfflineExpenseAiEngine.parseSms("JD-HDFCBN-S", credDebit, 10L) as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertFalse(out1.isSelfTransfer)
        assertEquals("CRED Club", out1.merchantDisplayName)
        assertEquals(SpendCategory.CREDIT_CARD_BILL.name, out1.category)

        val out2 = OfflineExpenseAiEngine.parseSms("BP-ICICIT-S", cardCreditReceipt, 20L)
        assertFalse(out2 is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
    }

    @Test
    fun `cleans Axis Not you suffix, resolves SmartQ, and extracts HDFC Credit Alert VPA without matching amount`() {
        val axisSmartQ = "INR 30.00 debited A/c no. XX1234 25-06-26, 20:44:45 UPI/P2M/11225969/SmartQ Not you? SMS BLOCKUPI Cust ID to 919951860002 Axis Bank"
        val hdfcCreditVpa = "Credit Alert! Rs.262.00 credited to HDFC Bank A/c XX4821 on 17-11-26 from VPA *hypg@yespay (UPI 88118793)"
        val kotakReceived = "Received Rs.188.00 in your Kotak Bank AC 9012 from PRIYA SHARMA on 08-11-26.UPI Ref:99112277"

        val tSmartQ = (OfflineExpenseAiEngine.parseSms("AD-AXISBK-S", axisSmartQ, 30L) as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals("SmartQ Cafeteria", tSmartQ.merchantDisplayName)
        assertEquals(SpendCategory.FOOD_DINING.name, tSmartQ.category)

        val tHdfc = (OfflineExpenseAiEngine.parseSms("JX-HDFCBK-S", hdfcCreditVpa, 40L) as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals(262.0, tHdfc.amount, 0.001)
        assertEquals(TransactionType.CREDIT.name, tHdfc.type)
        assertEquals("Flipkart", tHdfc.merchantDisplayName)

        val tKotak = (OfflineExpenseAiEngine.parseSms("AD-KOTAKB-S", kotakReceived, 50L) as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals("Priya Sharma", tKotak.merchantDisplayName)
        assertEquals(SpendCategory.TRANSFERS.name, tKotak.category)
    }

    @Test
    fun `extracts unmasked VPA before parenthetical UPI ref and resolves real inbox codes BDPG2 IR and MYBMTCDQR`() {
        val hdfcFullVpa = "Credit Alert! Rs.5000.00 credited to HDFC Bank A/c XX4821 on 15-11-26 from VPA rahulsharma-1@okicici (UPI 664083728809)"
        val irctcSms = "Sent Rs.745.00 From HDFC Bank A/C *4821 To BDPG2 IR On 14/11/26 Ref 998812"
        val bmtcSms = "Sent Rs.35.00 From HDFC Bank A/C *4821 To MYBMTCDQR On 14/11/26 Ref 112233"

        val tVpa = (OfflineExpenseAiEngine.parseSms("JX-HDFCBK-S", hdfcFullVpa, 51L) as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertFalse(tVpa.merchantDisplayName.startsWith("Upi ", ignoreCase = true))
        assertTrue(tVpa.merchantRawCode.contains("rahulsharma", ignoreCase = true))

        val tRail = (OfflineExpenseAiEngine.parseSms("AD-HDFCBK-S", irctcSms, 52L) as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals("IRCTC Railways", tRail.merchantDisplayName)
        assertEquals(SpendCategory.TRANSPORT_TRAVEL.name, tRail.category)

        val tBus = (OfflineExpenseAiEngine.parseSms("AD-HDFCBK-S", bmtcSms, 53L) as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals("BMTC Bus", tBus.merchantDisplayName)
        assertEquals(SpendCategory.TRANSPORT_TRAVEL.name, tBus.category)
    }

    @Test
    fun `parses uncomma 4-digit Rs 9992 spend accurately as DEBIT 9992 and never +999`() {
        val mobikwik = "Rs.9992.0 has been debited from your MobiKwik wallet. Remaining balance: Rs.0.26. Not you? Please email fraudalerts@mobikwik.com"
        val out = OfflineExpenseAiEngine.parseSms("AD-MOBIKW-S", mobikwik, 60L)
        assertTrue(out is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        val t = (out as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals(9992.0, t.amount, 0.001)
        assertEquals(TransactionType.DEBIT.name, t.type)
        assertEquals(PaymentRail.WALLET.name, t.paymentRail)
        assertFalse(t.merchantDisplayName.equals("Fraudalerts", ignoreCase = true))
    }

    @Test
    fun `resolves Rs 70 at GOOGLEONE as Google One under Entertainment`() {
        val sms = "Rs. 70.00 debited from A/c XX4821 on 18-11-26 to VPA googleone@okaxis. Info: UPI/P2M/632910441/GOOGLEONE. Never share OTP/PIN with anyone."
        val outcome = OfflineExpenseAiEngine.parseSms("AD-HDFCBK", sms, 70L)

        assertTrue(outcome is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        val txn = (outcome as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals(70.0, txn.amount, 0.001)
        assertEquals("Google One", txn.merchantDisplayName)
        assertEquals(SpendCategory.ENTERTAINMENT.name, txn.category)
    }

    @Test
    fun `applies self-learned user merchant rule over default`() {
        val learnedKey = MerchantKnowledgeBase.normalizeForLookup("CUSTOMCORP99")
        val learnedMap = mapOf(
            learnedKey to LearnedMerchantEntity(
                normalizedKey = learnedKey,
                displayName = "Office Cafeteria",
                category = SpendCategory.FOOD_DINING.name
            )
        )

        val sms = "Rs. 120.00 debited from A/c XX4821 at CUSTOMCORP99 on 18-11-26."
        val outcome = OfflineExpenseAiEngine.parseSms("AD-HDFCBK", sms, 80L, learnedMap)
        assertTrue(outcome is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        val txn = (outcome as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals("Office Cafeteria", txn.merchantDisplayName)
        assertEquals(SpendCategory.FOOD_DINING.name, txn.category)
    }
}
