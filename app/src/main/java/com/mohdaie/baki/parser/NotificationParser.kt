package com.mohdaie.baki.parser

import com.mohdaie.baki.model.TxType
import java.util.Locale
import kotlin.math.max
import kotlin.math.round

data class ParsedNotification(
    /** Amount in RM (foreign amounts already converted when a rate is known). */
    val amountRm: Double,
    /** Original foreign amount, e.g. "SGD 2.20", or null when it was charged in RM. */
    val fxText: String?,
    val type: String,
    val merchant: String,
    val categoryId: String?,
)

/**
 * Turns a bank / e-wallet notification into a transaction.
 * Pure Kotlin, no Android dependencies, so it is covered by plain unit tests.
 */
object NotificationParser {

    const val UNKNOWN_MERCHANT = "Unknown merchant"

    private const val NUM = """(\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)"""
    private const val FX_CODES = "SGD|USD|EUR|GBP|AUD|THB|IDR|JPY|CNY|HKD|KRW|TWD"

    private val RM = Regex("""(?i)(?<![A-Za-z])(?:RM|MYR)\s?$NUM""")
    private val FX_BEFORE = Regex("""(?i)(?<![A-Za-z])($FX_CODES)\s?$NUM""")
    private val FX_AFTER = Regex("""(?i)$NUM\s?(MYR|$FX_CODES)(?![A-Za-z])""")

    private val MONEY_WORDS = Regex(
        """(?i)\b(payments?|paid|pay|spent|spend|purchases?|purchased|debited|debit|charged|charge|transactions?|transferred|transfers?|received|receive|credited|refunds?|refunded|top[ -]?up|topped up|reload(?:ed)?|withdrawals?|withdrawn|deducted|successful(?:ly)?|approved|duitnow|sent)\b"""
    )
    private val PROMO_WORDS = Regex(
        """(?i)(%\s*off|\boff\b|vouchers?|promos?|promotion|discount|\bdeals?\b|\bwin\b|\bfree\b|\bsale\b|coupons?|\bearn\b|rewards?|\bclaim\b|cashback|giveaway|limited time|shop now|\bbuy\b|\bget rm)"""
    )
    private val STRONG_WORDS = Regex(
        """(?i)(payment of|you(?:'ve| have)? (?:made|paid|spent|received|sent|transferred)|has been (?:debited|credited|deducted|charged)|\bdebited\b|\bcredited\b|successful(?:ly)?|was charged|been charged|transaction of)"""
    )
    private val SECURITY_WORDS = Regex("""(?i)\b(tac|otp|one[- ]time|verification code|security code|passcode)\b""")
    private val INCOME_WORDS = Regex(
        """(?i)(\breceived\b|\bcredited\b|\brefund(?:ed)?\b|\bincoming\b|\bdeposit(?:ed)?\b|money in|cash ?in|sent you)"""
    )
    private val NOT_INCOME = Regex("""(?i)(your payment|payment of|you(?:'ve| have)? (?:paid|spent|made)|debited)""")
    private val TRANSFER_WORDS = Regex("""(?i)(top[ -]?up|topped up|\breload(?:ed)?\b|own account|added to (?:your )?(?:wallet|balance))""")
    private val WALLETS = Regex(
        """(?i)(?<![A-Za-z])(grab ?pay|touch ?'?n ?'?go|tng(?: digital| ewallet)?|boost|shopee ?pay|big ?pay|setel|wise|mae wallet)(?![A-Za-z])"""
    )
    private val BALANCE_BEFORE = Regex("""(?i)(bal(?:ance)?|available|avail|limit)\W{0,4}$""")

    private const val STOP =
        """(?=\s+(?:on|via|using|with|ref|reference|for|from|at|was|is|has|have|had|been|successful(?:ly)?|completed|approved|dated|date|in|amount|amt|rm|myr)\b|\s*[.,;!\n(|]|\s+-\s|$)"""
    private val TO_AT = Regex("""(?i)(?:\bto|\bat|@|\bmerchant:?)\s+([A-Za-z0-9&*'][^\n]{0,60}?)$STOP""")
    private val FROM = Regex("""(?i)\bfrom\s+([A-Za-z0-9&*'][^\n]{0,60}?)$STOP""")
    private val BAD_START = Regex("""(?i)^(your|you|my|the account|account|own|be|ensure|avoid)\b""")

    private val TIME = Regex("""(?i)\b\d{1,2}:\d{2}(?::\d{2})?(?:\s*[ap]\.?m\.?)?""")
    private val DAY_MONTH = Regex(
        """(?i)\b\d{1,2}\s*(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\.?(?:\s+\d{2,4})?\b"""
    )
    private val DATE = Regex("""\b\d{1,2}[/.-]\d{1,2}(?:[/.-]\d{2,4})?\b""")
    private val TRAIL_MONTH = Regex("""(?i)\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)\.?$""")
    private val LONG_NUMBER = Regex("""(?i)(?:\bref(?:erence)?\.?\s*(?:no\.?)?\s*:?\s*)?\b\d{6,}\b""")
    private val STAR_PREFIX = Regex("""^[A-Za-z]{2,10}\*\s*""")
    private val SPACES = Regex("""\s+""")

    fun parse(title: String, body: String, sgdRate: Double): ParsedNotification? {
        val text = listOf(title, body)
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .replace(' ', ' ')
            .replace('’', '\'')
        if (text.isBlank()) return null
        if (!MONEY_WORDS.containsMatchIn(text)) return null
        if (SECURITY_WORDS.containsMatchIn(text)) return null
        if (PROMO_WORDS.containsMatchIn(text) && !STRONG_WORDS.containsMatchIn(text)) return null

        val amount = findAmount(text, sgdRate) ?: return null
        if (amount.rm <= 0.0) return null

        var type = detectType(text)
        val merchant = extractMerchant(text, type) ?: fallbackMerchant(title)
        // Paying a wallet (GrabPay, TNG, Wise...) from a bank is moving your own money.
        if (type == TxType.EXPENSE && WALLETS.containsMatchIn(merchant)) type = TxType.TRANSFER
        val category = if (type == TxType.EXPENSE) {
            CategoryGuesser.guess(merchant) ?: CategoryGuesser.guess(title)
        } else {
            null
        }
        return ParsedNotification(amount.rm, amount.fxText, type, merchant, category)
    }

    private class Amount(val rm: Double, val fxText: String?)

    private fun findAmount(text: String, sgdRate: Double): Amount? {
        RM.findAll(text).firstOrNull { !isBalance(text, it.range.first) }?.let {
            return Amount(toNum(it.groupValues[1]), null)
        }
        FX_BEFORE.findAll(text).firstOrNull { !isBalance(text, it.range.first) }?.let {
            return foreign(it.groupValues[1], toNum(it.groupValues[2]), sgdRate)
        }
        FX_AFTER.findAll(text).firstOrNull { !isBalance(text, it.range.first) }?.let {
            return foreign(it.groupValues[2], toNum(it.groupValues[1]), sgdRate)
        }
        return null
    }

    private fun foreign(code: String, value: Double, sgdRate: Double): Amount {
        return when (val c = code.uppercase(Locale.ROOT)) {
            "MYR" -> Amount(value, null)
            "SGD" -> Amount(round2(value * sgdRate), "SGD " + fmt(value))
            else -> Amount(value, "$c ${fmt(value)} · not converted")
        }
    }

    private fun isBalance(text: String, start: Int): Boolean =
        BALANCE_BEFORE.containsMatchIn(text.substring(max(0, start - 16), start))

    private fun detectType(text: String): String = when {
        TRANSFER_WORDS.containsMatchIn(text) -> TxType.TRANSFER
        INCOME_WORDS.containsMatchIn(text) && !NOT_INCOME.containsMatchIn(text) -> TxType.INCOME
        else -> TxType.EXPENSE
    }

    private fun extractMerchant(text: String, type: String): String? {
        val patterns = if (type == TxType.INCOME) listOf(FROM, TO_AT) else listOf(TO_AT, FROM)
        for (pattern in patterns) {
            for (match in pattern.findAll(text)) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isEmpty() || BAD_START.containsMatchIn(candidate)) continue
                val cleaned = cleanMerchant(candidate)
                if (cleaned.length >= 2 && cleaned.any { it.isLetter() }) return cleaned
            }
        }
        return null
    }

    private fun fallbackMerchant(title: String): String {
        val t = cleanMerchant(title)
        return if (t.length >= 2 && !MONEY_WORDS.containsMatchIn(t) && t.none { it.isDigit() }) t else UNKNOWN_MERCHANT
    }

    /** "BOOST*MCDONALDS JB 05 Oct 18:10:05" -> "Mcdonalds JB" */
    fun cleanMerchant(raw: String): String {
        var s = raw.replace(STAR_PREFIX, "")
        s = s.replace(TIME, " ").replace(DAY_MONTH, " ").replace(DATE, " ").replace(LONG_NUMBER, " ")
        s = s.replace(SPACES, " ").trim()
        repeat(2) { s = s.replace(TRAIL_MONTH, "").trim() }
        s = s.trim(' ', '-', '*', ':', '.', ',', '\'', '"', '/')
        if (s.length > 40) s = s.take(40).trim()
        return titleCase(s)
    }

    private fun titleCase(s: String): String {
        if (s.any { it.isLowerCase() }) return s
        return s.split(' ').joinToString(" ") { w ->
            val keepUpper = w.length <= 2 || (w.length <= 4 && w.none { it in "AEIOU" })
            if (keepUpper) w else w.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }

    private fun toNum(s: String): Double = s.replace(",", "").toDoubleOrNull() ?: 0.0
    private fun round2(v: Double): Double = round(v * 100) / 100
    private fun fmt(v: Double): String = String.format(Locale.US, "%,.2f", v)
}
