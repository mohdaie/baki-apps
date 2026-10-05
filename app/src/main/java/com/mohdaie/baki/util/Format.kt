package com.mohdaie.baki.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val symbols = DecimalFormatSymbols(Locale.US)

    /** 1234.5 -> "1,234.50" */
    fun money(v: Double): String = DecimalFormat("#,##0.00", symbols).format(v)

    /** 1234.5 -> "1,235" */
    fun money0(v: Double): String = DecimalFormat("#,##0", symbols).format(v)

    /** For editable fields: 1200.0 -> "1200", 12.8 -> "12.8" */
    fun plain(v: Double): String = DecimalFormat("0.##", symbols).format(v)

    /** For the amount field: 12.8 -> "12.80" */
    fun fixed2(v: Double): String = String.format(Locale.US, "%.2f", v)

    val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    val monShort: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    val dayHeader: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM", Locale.ENGLISH)
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
}

fun String.toAmount(): Double = replace(",", "").trim().toDoubleOrNull() ?: 0.0

fun YearMonth.startMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun Long.toLocalDate(): LocalDate = toLocalDateTime().toLocalDate()
