package com.fixare.studio.paytrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses WHERE date >= :startDate AND date <= :endDate")
    fun getTotalExpenses(startDate: Long, endDate: Long): Flow<Double?>
}
