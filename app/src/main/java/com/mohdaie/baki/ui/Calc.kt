package com.mohdaie.baki.ui

import androidx.compose.ui.graphics.Color
import com.mohdaie.baki.data.BudgetEntity
import com.mohdaie.baki.data.CommitmentEntity
import com.mohdaie.baki.data.CommitmentPaymentEntity
import com.mohdaie.baki.data.PendingEntity
import com.mohdaie.baki.data.TransactionEntity
import com.mohdaie.baki.model.BudgetMode
import com.mohdaie.baki.model.Category
import com.mohdaie.baki.model.TxType
import com.mohdaie.baki.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
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
    /** Income recorded in Activity this month (refunds, transfers in, side income...). */
    val income: Double,
    /** Net salary + income − paid commitments − this month's spending. */
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
    month: YearMonth,
    today: LocalDate,
): MonthSummary {
    val paidIds = payments.map { it.commitmentId }.toSet()
    val commitTotal = commitments.sumOf { it.amount }
    val paidTotal = commitments.filter { it.id in paidIds }.sumOf { it.amount }
    val unpaid = commitments.filter { it.id !in paidIds }.sortedBy { it.dueDay }
    val isCurrent = YearMonth.from(today) == month
    val overdue = if (isCurrent) unpaid.filter { dueDayIn(it, month) < today.dayOfMonth } else emptyList()
    val nextDue = if (isCurrent) unpaid.firstOrNull { dueDayIn(it, month) >= today.dayOfMonth } else unpaid.firstOrNull()

    val expenses = monthTx.filter { it.type == TxType.EXPENSE }
    val spent = expenses.sumOf { it.amount }
    val income = monthTx.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val left = salary + income - paidTotal - spent
    val daysLeft = if (isCurrent) month.lengthOfMonth() - today.dayOfMonth + 1 else month.lengthOfMonth()
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

data class DueStatus(val text: String, val background: Color, val foreground: Color)

fun dueStatus(c: CommitmentEntity, paid: Boolean, month: YearMonth, today: LocalDate): DueStatus {
    if (paid) return DueStatus("Paid", AccentSoft, Green)
    if (YearMonth.from(today) != month) return DueStatus("Unpaid", Track, Muted)
    val diff = dueDayIn(c, month) - today.dayOfMonth
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
