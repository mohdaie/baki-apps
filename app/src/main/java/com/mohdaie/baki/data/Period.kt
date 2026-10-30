package com.mohdaie.baki.data

import com.mohdaie.baki.util.startMillis
import com.mohdaie.baki.util.toLocalDate
import java.time.LocalDate
import java.time.YearMonth

/**
 * The budget period: it starts when the latest net salary comes in, not on the 1st.
 * Spending, commitment ticks and "left to spend" all count from [startMillis].
 * Until a salary has been recorded it falls back to the calendar month.
 */
data class Period(
    val startMillis: Long,
    val startDate: LocalDate,
    /** First day after the period (one month after the salary, or the 1st of next month). */
    val endDate: LocalDate,
    /** Key for commitment_payments.yearMonth; changes when a new salary arrives, which clears the ticks. */
    val key: String,
    val fromSalary: Boolean,
) {
    companion object {
        fun of(salaryTime: Long?, today: LocalDate = LocalDate.now()): Period {
            if (salaryTime == null) {
                val month = YearMonth.from(today)
                return Period(month.startMillis(), month.atDay(1), month.plusMonths(1).atDay(1), month.toString(), false)
            }
            val start = salaryTime.toLocalDate()
            return Period(salaryTime, start, start.plusMonths(1), "s$salaryTime", true)
        }
    }
}
