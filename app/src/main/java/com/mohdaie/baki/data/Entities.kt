package com.mohdaie.baki.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A saved transaction shown in Activity. Amount is always in RM. */
@Entity(tableName = "transactions", indices = [Index("timestamp")])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val category: String?,
    val merchant: String,
    val account: String,
    val timestamp: Long,
    val fxText: String? = null,
    /** "manual" or "notif:<package name>" */
    val source: String = "manual",
    val rawText: String? = null,
)

/** A captured notification waiting for you to confirm it in the popup. */
@Entity(tableName = "pending")
data class PendingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val rawText: String,
    val amount: Double,
    val fxText: String?,
    val merchant: String,
    val type: String,
    val category: String?,
    val account: String,
    val postedAt: Long,
)

/** A monthly commitment (loan, rent, subscription...). */
@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDay: Int,
    val kind: String,
)

/** "This commitment is paid for this month" — one row per commitment per month. */
@Entity(tableName = "commitment_payments", primaryKeys = ["commitmentId", "yearMonth"])
data class CommitmentPaymentEntity(
    val commitmentId: Long,
    /** e.g. "2026-10" */
    val yearMonth: String,
    val paidAt: Long,
)

/** Budget for one category: a fixed RM amount or a % of what's left after commitments. */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val mode: String,
    val value: Double,
)

/** Learned "this merchant is that category" from your corrections. */
@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey val merchantKey: String,
    val category: String,
)

/** An app that has sent money notifications, how to name it, and whether to ignore it. */
@Entity(tableName = "app_sources")
data class AppSourceEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val accountName: String,
    val ignored: Boolean = false,
    val lastSeen: Long = 0,
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val name: String,
    val value: String,
)
