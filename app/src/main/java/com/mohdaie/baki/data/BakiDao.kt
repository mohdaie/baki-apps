package com.mohdaie.baki.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BakiDao {

    // ---- transactions ----
    @Query("SELECT * FROM transactions WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    fun observeTransactions(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: Long): TransactionEntity?

    @Insert
    suspend fun insertTransaction(t: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(t: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE source LIKE 'notif:%' AND ABS(amount - :amount) < 0.005 AND timestamp >= :since")
    suspend fun countRecentNotificationTx(amount: Double, since: Long): Int

    // ---- pending (captured, waiting for review) ----
    @Query("SELECT * FROM pending ORDER BY postedAt ASC")
    fun observePending(): Flow<List<PendingEntity>>

    @Query("SELECT * FROM pending WHERE id = :id")
    suspend fun getPending(id: Long): PendingEntity?

    @Insert
    suspend fun insertPending(p: PendingEntity): Long

    @Query("DELETE FROM pending WHERE id = :id")
    suspend fun deletePending(id: Long)

    @Query("SELECT COUNT(*) FROM pending WHERE ABS(amount - :amount) < 0.005 AND postedAt >= :since")
    suspend fun countRecentPending(amount: Double, since: Long): Int

    // ---- commitments ----
    @Query("SELECT * FROM commitments ORDER BY dueDay ASC, name ASC")
    fun observeCommitments(): Flow<List<CommitmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommitment(c: CommitmentEntity): Long

    @Query("DELETE FROM commitments WHERE id = :id")
    suspend fun deleteCommitment(id: Long)

    @Query("SELECT * FROM commitment_payments WHERE yearMonth = :yearMonth")
    fun observePayments(yearMonth: String): Flow<List<CommitmentPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(p: CommitmentPaymentEntity)

    @Query("DELETE FROM commitment_payments WHERE commitmentId = :commitmentId AND yearMonth = :yearMonth")
    suspend fun deletePayment(commitmentId: Long, yearMonth: String)

    @Query("DELETE FROM commitment_payments WHERE commitmentId = :commitmentId")
    suspend fun deletePaymentsFor(commitmentId: Long)

    // ---- budgets ----
    @Query("SELECT * FROM budgets")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(b: BudgetEntity)

    // ---- learned merchant categories ----
    @Query("SELECT category FROM merchant_rules WHERE merchantKey = :key")
    suspend fun ruleFor(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(r: MerchantRuleEntity)

    // ---- notification sources ----
    @Query("SELECT * FROM app_sources ORDER BY lastSeen DESC")
    fun observeSources(): Flow<List<AppSourceEntity>>

    @Query("SELECT * FROM app_sources WHERE packageName = :pkg")
    suspend fun getSource(pkg: String): AppSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSource(s: AppSourceEntity)

    // ---- settings ----
    @Query("SELECT * FROM settings")
    fun observeSettings(): Flow<List<SettingEntity>>

    @Query("SELECT value FROM settings WHERE name = :name")
    suspend fun getSetting(name: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(s: SettingEntity)
}
