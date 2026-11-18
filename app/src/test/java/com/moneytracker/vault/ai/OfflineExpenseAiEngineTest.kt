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

    @Test
    fun `accurately identifies corporate salary credits across HDFC, ICICI, SBI, and Axis without catching casual credits`() {
        val hdfcSalary = "A/c XX4821 credited with Rs 65,000.00 on 31-Oct-26 by NEFT-ACMETECH-SALARY FOR OCT. Ref: HDFCN12345. Avl Bal: Rs 1,12,450.50."
        val iciciSalary = "Acct XX5678 credited with INR 75,400.00 on 31-OCT-26 towards NEFT-SALARY FROM GOOGLE INDIA PVT LTD. Avl Bal: INR 90,210.00."
        val sbiSalary = "A/c Lok-XXXXXX12345 credited by Rs.50,000.00 on 31-Oct-26 trf from BULK POSTING-BY SALARY-INFOSYS. Avl Bal Rs 65,200.00."
        val axisSalary = "INR 82,000.00 credited to A/c no. XX9876 on 31-Oct-26 through NEFT-SALARY. Ref: UTIBN881122. Avl Bal: INR 1,05,300.00."

        val outHdfc = OfflineExpenseAiEngine.parseSms("AD-HDFCBK-S", hdfcSalary, 101L)
        assertTrue(outHdfc is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        val tHdfc = (outHdfc as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals(SpendCategory.SALARY.name, tHdfc.category)
        assertEquals(TransactionType.CREDIT.name, tHdfc.type)
        assertEquals(65000.0, tHdfc.amount, 0.001)

        val outIcici = OfflineExpenseAiEngine.parseSms("BP-ICICIT-S", iciciSalary, 102L)
        assertTrue(outIcici is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        val tIcici = (outIcici as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals(SpendCategory.SALARY.name, tIcici.category)
        assertEquals(75400.0, tIcici.amount, 0.001)

        val outSbi = OfflineExpenseAiEngine.parseSms("SB-SBIINB-S", sbiSalary, 103L)
        assertTrue(outSbi is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        val tSbi = (outSbi as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals(SpendCategory.SALARY.name, tSbi.category)
        assertEquals(50000.0, tSbi.amount, 0.001)

        val outAxis = OfflineExpenseAiEngine.parseSms("AD-AXISBK-S", axisSalary, 104L)
        assertTrue(outAxis is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        val tAxis = (outAxis as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals(SpendCategory.SALARY.name, tAxis.category)
        assertEquals(82000.0, tAxis.amount, 0.001)

        // Ensure non-salary credits are NOT categorized as SALARY
        val casualP2p = "Credit Alert! Rs.5000.00 credited to HDFC Bank A/c XX4821 on 15-11-26 from VPA friend@okaxis (UPI 664083728809)"
        val dividend = "Rs. 4,500.00 credited to A/c XX4821 on 10-11-26 by ACH C-DIVIDEND TCS LTD."
        val refund = "Rs. 1,299.00 credited to A/c XX4821 on 10-11-26 transfer from AMAZON PAY Ref 123456"

        val tP2p = (OfflineExpenseAiEngine.parseSms("AD-HDFCBK-S", casualP2p, 105L) as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertFalse(tP2p.category == SpendCategory.SALARY.name)

        val tDiv = (OfflineExpenseAiEngine.parseSms("AD-HDFCBK-S", dividend, 106L) as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertFalse(tDiv.category == SpendCategory.SALARY.name)

        val tRef = (OfflineExpenseAiEngine.parseSms("AD-HDFCBK-S", refund, 107L) as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertFalse(tRef.category == SpendCategory.SALARY.name)
        // Test real HDFC corporate payroll deposited format with Larsen & Toubro
        val realHdfcLntSalary = "Update! INR 27,536.00 deposited in HDFC Bank A/c XX4192 on 28-OCT-26 for NEFT Cr-ICIC0099999-LARSEN AND TOUBRO LIMITED-AJAY  A-INXXXXXXXXXX2810.Avl bal INR 37,184.02. Cheque deposits in A/C are subject to clearing"
        val outHdfcLnt = OfflineExpenseAiEngine.parseSms("JD-HDFCBK-S", realHdfcLntSalary, 108L)
        assertTrue(outHdfcLnt is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        val tHdfcLnt = (outHdfcLnt as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals(SpendCategory.SALARY.name, tHdfcLnt.category)
        assertEquals(TransactionType.CREDIT.name, tHdfcLnt.type)
        assertEquals(27536.0, tHdfcLnt.amount, 0.001)
        assertEquals("Larsen & Toubro", tHdfcLnt.merchantDisplayName)

        // Test real Axis corporate payroll NEFT format with LARS
        val realAxisLntSalary = "INR 33402.00 credited to A/c no. XX5694 on 25-06-26 at 13:32:49 IST. Info - NEFT/IN22617635727901/LARS. Chk Bal https://ccm.axis.bank.in/AXISBK/ltt3Dvko - Axis Bank"
        val outAxisLnt = OfflineExpenseAiEngine.parseSms("JX-AXISBK-S", realAxisLntSalary, 109L)
        assertTrue(outAxisLnt is OfflineExpenseAiEngine.AiParseOutcome.Parsed)
        val tAxisLnt = (outAxisLnt as OfflineExpenseAiEngine.AiParseOutcome.Parsed).transaction
        assertEquals(SpendCategory.SALARY.name, tAxisLnt.category)
        assertEquals(TransactionType.CREDIT.name, tAxisLnt.type)
        assertEquals(33402.0, tAxisLnt.amount, 0.001)
        assertEquals("Larsen & Toubro", tAxisLnt.merchantDisplayName)
    }
}
