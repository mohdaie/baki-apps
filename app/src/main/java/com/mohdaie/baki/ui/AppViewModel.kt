package com.mohdaie.baki.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mohdaie.baki.BakiApp
import com.mohdaie.baki.data.AppSourceEntity
import com.mohdaie.baki.data.BudgetEntity
import com.mohdaie.baki.data.CommitmentEntity
import com.mohdaie.baki.data.CommitmentPaymentEntity
import com.mohdaie.baki.data.PendingEntity
import com.mohdaie.baki.data.Repository
import com.mohdaie.baki.data.TransactionEntity
import com.mohdaie.baki.data.TxDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: Repository = (app as BakiApp).repository
    private val started = SharingStarted.WhileSubscribed(5_000)

    val currentMonth: YearMonth = YearMonth.now()

    private val _activityMonth = MutableStateFlow(currentMonth)
    val activityMonth: StateFlow<YearMonth> = _activityMonth.asStateFlow()

    /** null until loaded from the database. */
    val settings: StateFlow<Map<String, String>?> =
        repo.observeSettings().stateIn(viewModelScope, started, null)

    val monthTx: StateFlow<List<TransactionEntity>> =
        repo.observeMonth(currentMonth).stateIn(viewModelScope, started, emptyList())

    val activityTx: StateFlow<List<TransactionEntity>> =
        _activityMonth.flatMapLatest { repo.observeMonth(it) }.stateIn(viewModelScope, started, emptyList())

    /** The last 4 months including this one, for Statistics. */
    val historyTx: StateFlow<List<TransactionEntity>> =
        repo.observeRange(currentMonth.minusMonths(3), currentMonth.plusMonths(1))
            .stateIn(viewModelScope, started, emptyList())

    val commitments: StateFlow<List<CommitmentEntity>> =
        repo.dao.observeCommitments().stateIn(viewModelScope, started, emptyList())

    val payments: StateFlow<List<CommitmentPaymentEntity>> =
        repo.dao.observePayments(currentMonth.toString()).stateIn(viewModelScope, started, emptyList())

    /** null until loaded. */
    val budgets: StateFlow<Map<String, BudgetEntity>?> =
        repo.dao.observeBudgets()
            .map { list -> list.associateBy { it.category } }
            .stateIn(viewModelScope, started, null)

    /** null until loaded. */
    val pending: StateFlow<List<PendingEntity>?> =
        repo.dao.observePending().stateIn(viewModelScope, started, null)

    val sources: StateFlow<List<AppSourceEntity>> =
        repo.dao.observeSources().stateIn(viewModelScope, started, emptyList())

    /** Account names to offer in the transaction form. */
    val accounts: StateFlow<List<String>> =
        combine(sources, historyTx) { s, h ->
            (s.filter { !it.ignored }.map { it.accountName } + h.map { it.account })
                .filter { it.isNotBlank() }
                .distinct()
        }.stateIn(viewModelScope, started, emptyList())

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }

    fun shiftActivityMonth(delta: Long) {
        val next = _activityMonth.value.plusMonths(delta)
        if (next <= currentMonth) _activityMonth.value = next
    }

    fun setSalary(value: Double) = io { repo.setSetting(Repository.SALARY, value.toString()) }
    fun setSgdRate(value: Double) = io { if (value > 0) repo.setSetting(Repository.SGD_RATE, value.toString()) }
    fun setBudget(category: String, mode: String, value: Double) = io { repo.setBudget(category, mode, value) }

    fun setPaid(c: CommitmentEntity, paid: Boolean) = io { repo.setPaid(c.id, currentMonth, paid) }
    fun saveCommitment(c: CommitmentEntity) = io { repo.saveCommitment(c) }
    fun deleteCommitment(id: Long) = io { repo.deleteCommitment(id) }

    fun saveTransaction(id: Long?, draft: TxDraft, timestamp: Long) = io { repo.saveTransaction(id, draft, timestamp) }
    fun deleteTransaction(id: Long) = io { repo.deleteTransaction(id) }

    fun confirmPending(id: Long, draft: TxDraft) = io { repo.confirmPending(id, draft) }
    fun ignorePending(id: Long) = io { repo.ignorePending(id) }

    /** "This notification is my Car loan" — tick the commitment instead of adding an expense. */
    fun markPendingAsCommitment(pendingId: Long, c: CommitmentEntity) = io {
        repo.setPaid(c.id, currentMonth, true)
        repo.ignorePending(pendingId)
    }

    fun setSourceAccount(pkg: String, name: String) = io { repo.setSourceAccount(pkg, name) }
    fun setSourceIgnored(pkg: String, ignored: Boolean) = io { repo.setSourceIgnored(pkg, ignored) }

    private var sampleIndex = 0
    private val samples = listOf(
        "Maybank2u" to "You have made a payment of RM12.80 to MCDONALDS JB CITY SQ.",
        "TNG eWallet" to "Payment of RM4.20 to PLUS TOLL was successful.",
        "Wise" to "You spent 18.40 SGD at FAIRPRICE XTRA with your Wise card.",
        "Maybank2u" to "You have received RM150.00 from AHMAD BIN ALI via DuitNow.",
    )

    /** Runs a fake bank notification through the real capture pipeline. */
    fun sendTestNotification() = io {
        val (title, body) = samples[sampleIndex % samples.size]
        sampleIndex++
        repo.handleNotification(
            pkg = Repository.TEST_PACKAGE,
            label = "Sample bank",
            title = title,
            body = body,
            postedAt = System.currentTimeMillis(),
            skipDuplicateCheck = true,
        )
    }
}
