package com.mohdaie.baki.data

import android.content.Context
import com.mohdaie.baki.BakiApp
import com.mohdaie.baki.model.SALARY_TAG
import com.mohdaie.baki.model.TxType
import com.mohdaie.baki.parser.NotificationParser
import com.mohdaie.baki.service.Notifier
import com.mohdaie.baki.util.startMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.util.Locale

/** What the user confirmed or typed in the transaction form. */
data class TxDraft(
    val amount: Double,
    val type: String,
    val category: String?,
    val merchant: String,
    val account: String,
)

class Repository(private val context: Context, db: AppDatabase) {

    val dao: BakiDao = db.dao()

    fun observeMonth(month: YearMonth): Flow<List<TransactionEntity>> =
        dao.observeTransactions(month.startMillis(), month.plusMonths(1).startMillis())

    fun observeRange(from: YearMonth, toExclusive: YearMonth): Flow<List<TransactionEntity>> =
        dao.observeTransactions(from.startMillis(), toExclusive.startMillis())

    fun observeSettings(): Flow<Map<String, String>> =
        dao.observeSettings().map { list -> list.associate { it.name to it.value } }

    suspend fun setSetting(name: String, value: String) = dao.upsertSetting(SettingEntity(name, value))

    suspend fun sgdRate(): Double =
        dao.getSetting(SGD_RATE)?.toDoubleOrNull()?.takeIf { it > 0 } ?: DEFAULT_SGD_RATE

    /**
     * Called for every notification posted on the phone.
     * Returns the id of the new pending transaction, or null if it wasn't a money notification.
     */
    suspend fun handleNotification(
        pkg: String,
        label: String,
        title: String,
        body: String,
        postedAt: Long,
        skipDuplicateCheck: Boolean = false,
    ): Long? {
        val existing = dao.getSource(pkg)
        if (existing?.ignored == true) return null

        val parsed = NotificationParser.parse(title, body, sgdRate()) ?: return null

        // Chat apps can contain "I paid RM20" — ignore them unless the user turns them on.
        if (existing == null && pkg in DEFAULT_IGNORED) {
            dao.upsertSource(AppSourceEntity(pkg, label, label, ignored = true, lastSeen = postedAt))
            return null
        }

        // The same spend often triggers two notifications (bank + wallet, or an update).
        if (!skipDuplicateCheck) {
            val since = postedAt - DUPLICATE_WINDOW_MS
            if (dao.countRecentPending(parsed.amountRm, since) > 0) return null
            if (dao.countRecentNotificationTx(parsed.amountRm, since) > 0) return null
        }

        val account = existing?.accountName ?: label
        dao.upsertSource(
            AppSourceEntity(pkg, existing?.label ?: label, account, ignored = false, lastSeen = postedAt),
        )

        val learned = dao.ruleFor(merchantKey(parsed.merchant))
        // Expenses: learned or guessed category. Income: pre-tick "net salary" if you ticked it last time.
        val category = when (parsed.type) {
            TxType.EXPENSE -> learned?.takeIf { it != SALARY_TAG } ?: parsed.categoryId
            TxType.INCOME -> learned?.takeIf { it == SALARY_TAG }
            else -> null
        }
        val pending = PendingEntity(
            packageName = pkg,
            appLabel = label,
            rawText = listOf(title, body).filter { it.isNotBlank() }.joinToString("\n"),
            amount = parsed.amountRm,
            fxText = parsed.fxText,
            merchant = parsed.merchant,
            type = parsed.type,
            category = category,
            account = account,
            postedAt = postedAt,
        )
        val id = dao.insertPending(pending)
        val saved = pending.copy(id = id)

        // If Baki is already open, its own popup sheet shows it.
        // Otherwise: heads-up notification + (if allowed) a popup over the current app.
        if ((context as? BakiApp)?.mainVisible != true) {
            Notifier.showPending(context, saved)
            Notifier.tryOpenPopup(context, id)
        }
        return id
    }

    suspend fun confirmPending(id: Long, draft: TxDraft? = null) {
        val p = dao.getPending(id) ?: return
        val d = draft ?: TxDraft(p.amount, p.type, p.category, p.merchant, p.account)
        val category = storedCategory(d)
        dao.insertTransaction(
            TransactionEntity(
                amount = d.amount,
                type = d.type,
                category = category,
                merchant = d.merchant,
                account = d.account,
                timestamp = p.postedAt,
                fxText = p.fxText,
                source = "notif:${p.packageName}",
                rawText = p.rawText,
            ),
        )
        if (category != null) {
            learn(p.merchant, category)
            learn(d.merchant, category)
        }
        if (category == SALARY_TAG) setSetting(SALARY, d.amount.toString())
        dao.deletePending(id)
        Notifier.cancel(context, id)
    }

    suspend fun ignorePending(id: Long) {
        dao.deletePending(id)
        Notifier.cancel(context, id)
    }

    suspend fun saveTransaction(id: Long?, d: TxDraft, timestamp: Long) {
        val category = storedCategory(d)
        if (id == null) {
            dao.insertTransaction(
                TransactionEntity(
                    amount = d.amount,
                    type = d.type,
                    category = category,
                    merchant = d.merchant,
                    account = d.account,
                    timestamp = timestamp,
                    source = "manual",
                ),
            )
        } else {
            val old = dao.getTransaction(id) ?: return
            dao.updateTransaction(
                old.copy(amount = d.amount, type = d.type, category = category, merchant = d.merchant, account = d.account),
            )
        }
        if (category != null) learn(d.merchant, category)
        if (category == SALARY_TAG) setSetting(SALARY, d.amount.toString())
    }

    /** Expenses keep their category; income keeps only the "net salary" mark; transfers keep nothing. */
    private fun storedCategory(d: TxDraft): String? = when (d.type) {
        TxType.EXPENSE -> d.category?.takeIf { it != SALARY_TAG }
        TxType.INCOME -> d.category?.takeIf { it == SALARY_TAG }
        else -> null
    }

    suspend fun deleteTransaction(id: Long) = dao.deleteTransaction(id)

    suspend fun saveCommitment(c: CommitmentEntity) {
        dao.upsertCommitment(c)
    }

    suspend fun deleteCommitment(id: Long) {
        dao.deletePaymentsFor(id)
        dao.deleteCommitment(id)
    }

    suspend fun setPaid(commitmentId: Long, month: YearMonth, paid: Boolean) {
        if (paid) {
            dao.insertPayment(CommitmentPaymentEntity(commitmentId, month.toString(), System.currentTimeMillis()))
        } else {
            dao.deletePayment(commitmentId, month.toString())
        }
    }

    suspend fun setBudget(category: String, mode: String, value: Double) =
        dao.upsertBudget(BudgetEntity(category, mode, value))

    suspend fun setSourceAccount(pkg: String, name: String) {
        val s = dao.getSource(pkg) ?: return
        dao.upsertSource(s.copy(accountName = name))
    }

    suspend fun setSourceIgnored(pkg: String, ignored: Boolean) {
        val s = dao.getSource(pkg) ?: return
        dao.upsertSource(s.copy(ignored = ignored))
    }

    private suspend fun learn(merchant: String, category: String) {
        if (merchant.isBlank() || merchant == NotificationParser.UNKNOWN_MERCHANT) return
        dao.upsertRule(MerchantRuleEntity(merchantKey(merchant), category))
    }

    companion object {
        const val SALARY = "salary"
        const val SGD_RATE = "sgd_rate"
        const val DEFAULT_SGD_RATE = 3.19
        const val DUPLICATE_WINDOW_MS = 3 * 60 * 1000L
        const val TEST_PACKAGE = "com.mohdaie.baki.sample"

        val DEFAULT_IGNORED = setOf(
            "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "org.thunderdog.challegram",
            "com.facebook.orca", "com.facebook.katana", "com.instagram.android", "com.discord",
            "jp.naver.line.android", "com.tencent.mm", "com.twitter.android", "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill", "com.google.android.gm", "com.microsoft.teams", "com.Slack",
            "com.reddit.frontpage", "com.google.android.youtube", "com.google.android.apps.messaging",
        )

        fun merchantKey(merchant: String): String =
            merchant.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "")
    }
}
