package com.mohdaie.baki.parser

import com.mohdaie.baki.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationParserTest {

    private fun parse(title: String, body: String) = NotificationParser.parse(title, body, sgdRate = 3.19)

    @Test fun maybankPayment() {
        val p = parse("Maybank2u", "You have made a payment of RM12.80 to MCDONALDS JB CITY SQ.")!!
        assertEquals(12.80, p.amountRm, 0.001)
        assertEquals(TxType.EXPENSE, p.type)
        assertEquals("Mcdonalds JB City SQ", p.merchant)
        assertEquals("food", p.categoryId)
    }

    @Test fun tngToll() {
        val p = parse("TNG eWallet", "Payment of RM4.20 to PLUS TOLL was successful.")!!
        assertEquals(4.20, p.amountRm, 0.001)
        assertEquals("Plus Toll", p.merchant)
        assertEquals("transport", p.categoryId)
    }

    @Test fun wiseSgdIsConverted() {
        val p = parse("Wise", "You spent 2.20 SGD at OLD TEA HUT")!!
        assertEquals(7.02, p.amountRm, 0.001)
        assertEquals("SGD 2.20", p.fxText)
        assertEquals("Old Tea Hut", p.merchant)
        assertEquals("food", p.categoryId)
    }

    @Test fun incomeFromDuitNow() {
        val p = parse("Maybank2u", "You have received RM150.00 from AHMAD BIN ALI via DuitNow.")!!
        assertEquals(TxType.INCOME, p.type)
        assertEquals(150.0, p.amountRm, 0.001)
        assertEquals("Ahmad Bin Ali", p.merchant)
    }

    @Test fun walletTopUpIsTransfer() {
        val p = parse("MAE", "You've paid RM50.00 to GRABPAY MALAYSIA Oct 08:26 AM")!!
        assertEquals(TxType.TRANSFER, p.type)
        assertEquals("Grabpay Malaysia", p.merchant)
    }

    @Test fun balanceIsNotTheAmount() {
        val p = parse(
            "Maybank2u",
            "RM100.00 has been debited from your account for payment to TOYYIBPAY Oct 18:10:05. Avail bal: RM1,234.56",
        )!!
        assertEquals(100.0, p.amountRm, 0.001)
        assertEquals("Toyyibpay", p.merchant)
    }

    @Test fun billPaymentConfirmationIsExpense() {
        val p = parse("Unifi", "We have received your payment of RM129.00. Thank you.")!!
        assertEquals(TxType.EXPENSE, p.type)
        assertEquals("bills", p.categoryId)
    }

    @Test fun thousandsSeparator() {
        val p = parse("CIMB", "Transaction of MYR 1,250.00 at IKEA DAMANSARA on 03/10/2026 was approved.")!!
        assertEquals(1250.0, p.amountRm, 0.001)
        assertEquals("Ikea Damansara", p.merchant)
        assertEquals("shopping", p.categoryId)
    }

    @Test fun promoIsIgnored() {
        assertNull(parse("Shopee", "Flash sale! Get RM10 off your next order"))
        assertNull(parse("Lazada", "Payday deals: spend RM100 and earn RM15 cashback"))
    }

    @Test fun tacIsIgnored() {
        assertNull(parse("Maybank", "Your TAC for payment of RM12.80 to KFC is 123456"))
    }

    @Test fun cleansTimestampsFromMerchant() {
        assertEquals("Grabpay Malaysia", NotificationParser.cleanMerchant("GRABPAY MALAYSIA Oct 08:26 Am"))
        assertEquals("Mcdonalds", NotificationParser.cleanMerchant("BOOST*MCDONALDS"))
    }
}
