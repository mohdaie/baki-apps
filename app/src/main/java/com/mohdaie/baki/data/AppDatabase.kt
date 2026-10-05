package com.mohdaie.baki.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionEntity::class,
        PendingEntity::class,
        CommitmentEntity::class,
        CommitmentPaymentEntity::class,
        BudgetEntity::class,
        MerchantRuleEntity::class,
        AppSourceEntity::class,
        SettingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): BakiDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "baki.db").build()
    }
}
