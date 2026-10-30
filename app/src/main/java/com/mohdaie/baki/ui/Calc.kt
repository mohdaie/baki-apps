package com.mohdaie.baki.ui

import androidx.compose.ui.graphics.Color
import com.mohdaie.baki.data.BudgetEntity
import com.mohdaie.baki.data.CommitmentEntity
import com.mohdaie.baki.data.CommitmentPaymentEntity
import com.mohdaie.baki.data.PendingEntity
import com.mohdaie.baki.data.Period
import com.mohdaie.baki.data.TransactionEntity
import com.mohdaie.baki.model.BudgetMode
import com.mohdaie.baki.model.Category
import com.mohdaie.baki.model.SALARY_TAG
import com.mohdaie.baki.model.TxType
import com.mohdaie.baki.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class MonthSummary(
    val salary: Double,
    val commitTotal: Double,
    val paidTotal: Double,
    val unpaid: List<CommitmentEntity>,
    val overdue: List<CommitmentEntity>,
    val nextDue: CommitmentEntity?,
    val spent: Double,
    /** Income recorded in Activity since the latest salary (refunds, transfers in, side income...). */
    val income: Double,
    /** Net salary + income − paid commitments − spending since salary. */
    val left: Double,
    val daysLeft: Int,
    val dailyAvailable: Double,
    /** Net salary − all commitments: what the Plan divides between categories. */
    val availableToPlan: Double,
    val spentByCategory: Map<String?, Double>,
    val allocationByCategory: Map<String, Double>,
    val totalAllocated: Double,
)

fun allocationFor(budget: BudgetEntity?, availableToPlan: Double): Double = when {
    budget == null -> 0.0
    budget.mode == BudgetMode.PERCENT -> max(0.0, availableToPlan) * budget.value / 100.0
    else -> budget.value
}

fun summarize(
    salary: Double,
    commitments: List<CommitmentEntity>,
    payments: List<CommitmentPaymentEntity>,
    monthTx: List<TransactionEntity>,
    budgets: Map<String, BudgetEntity>,
    period: Period,
    today: LocalDate,
): MonthSummary {
    val paidIds = payments.map { it.commitmentId }.toSet()
    val commitTotal = commitments.sumOf { it.amount }
    val paidTotal = commitments.filter { it.id in paidIds }.sumOf { it.amount }
    val unpaid = commitments.filter { it.id !in paidIds }
    val dueSorted = unpaid.sortedBy { dueDateIn(it, period) }
    val overdue = dueSorted.filter { dueDateIn(it, period) < today }
    val nextDue = dueSorted.firstOrNull { dueDateIn(it, period) >= today }

    // A payment linked to a paid commitment is already in paidTotal.
    val expenses = monthTx.filter { it.type == TxType.EXPENSE && it.commitmentId !in paidIds }
    val spent = expenses.sumOf { it.amount }
    // Income ticked as "net salary" is already the salary figure, so it isn't added again.
    val income = monthTx.filter { it.type == TxType.INCOME && it.category != SALARY_TAG }.sumOf { it.amount }
    val left = salary + income - paidTotal - spent
    val daysLeft = max(1L, ChronoUnit.DAYS.between(today, period.endDate)).toInt()
    val available = salary - commitTotal
    val spentByCategory = expenses
        .groupBy { Category.of(it.category)?.id }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
    val allocation = Category.entries.associate { it.id to allocationFor(budgets[it.id], available) }

    return MonthSummary(
        salary = salary,
        commitTotal = commitTotal,
        paidTotal = paidTotal,
        unpaid = unpaid,
        overdue = overdue,
        nextDue = nextDue,
        spent = spent,
        income = income,
        left = left,
        daysLeft = daysLeft,
        dailyAvailable = max(0.0, left) / max(1, daysLeft),
        availableToPlan = available,
        spentByCategory = spentByCategory,
        allocationByCategory = allocation,
        totalAllocated = allocation.values.sum(),
    )
}

fun dueDayIn(c: CommitmentEntity, month: YearMonth): Int = min(c.dueDay, month.lengthOfMonth())

/** The first due date on or after the period start (a salary on the 25th makes a day-5 bill fall next month). */
fun dueDateIn(c: CommitmentEntity, period: Period): LocalDate {
    val startMonth = YearMonth.from(period.startDate)
    val date = startMonth.atDay(dueDayIn(c, startMonth))
    if (date >= period.startDate) return date
    val next = startMonth.plusMonths(1)
    return next.atDay(dueDayIn(c, next))
}

data class DueStatus(val text: String, val background: Color, val foreground: Color)

fun dueStatus(c: CommitmentEntity, paid: Boolean, period: Period, today: LocalDate): DueStatus {
    if (paid) return DueStatus("Paid", AccentSoft, Green)
    val diff = ChronoUnit.DAYS.between(today, dueDateIn(c, period)).toInt()
    return when {
        diff < 0 -> DueStatus("Overdue", RedSoft, RedText)
        diff == 0 -> DueStatus("Due today", Amber, AmberText)
        diff <= 3 -> DueStatus(if (diff == 1) "Due tomorrow" else "Due in $diff days", Amber, AmberText)
        else -> DueStatus("Upcoming", Track, Muted)
    }
}

/** This month's commitments that aren't ticked as paid yet. */
fun unpaidCommitments(
    commitments: List<CommitmentEntity>,
    payments: List<CommitmentPaymentEntity>,
): List<CommitmentEntity> {
    val paidIds = payments.map { it.commitmentId }.toSet()
    return commitments.filter { it.id !in paidIds }
}

/** An unpaid commitment with the same amount as a captured notification — probably that bill. */
fun matchCommitment(
    p: PendingEntity,
    commitments: List<CommitmentEntity>,
    payments: List<CommitmentPaymentEntity>,
): CommitmentEntity? {
    if (p.type == TxType.INCOME) return null
    val paidIds = payments.map { it.commitmentId }.toSet()
    return commitments.firstOrNull { it.id !in paidIds && abs(it.amount - p.amount) < 0.01 }
}
