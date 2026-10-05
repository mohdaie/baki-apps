package com.mohdaie.baki.model

object TxType {
    const val EXPENSE = "expense"
    const val INCOME = "income"
    const val TRANSFER = "transfer"
}

/** Category value used on an income transaction to mark it as the month's net salary. */
const val SALARY_TAG = "salary"

object BudgetMode {
    const val AMOUNT = "amt"
    const val PERCENT = "pct"
}

enum class Category(val id: String, val label: String) {
    FOOD("food", "Food & Drink"),
    BILLS("bills", "Bills & Utilities"),
    TRANSPORT("transport", "Transport"),
    HEALTH("health", "Health"),
    SHOPPING("shopping", "Shopping"),
    ENTERTAINMENT("entertainment", "Entertainment");

    companion object {
        fun of(id: String?): Category? = entries.firstOrNull { it.id == id }
    }
}

val COMMITMENT_KINDS = listOf("Loan", "Rent", "Utility", "Insurance", "Subscription", "Other")
