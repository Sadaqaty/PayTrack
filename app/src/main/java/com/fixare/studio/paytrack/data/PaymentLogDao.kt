package com.fixare.studio.paytrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentLogDao {
    @Query("SELECT * FROM payment_logs ORDER BY date DESC")
    fun getAllPaymentLogs(): Flow<List<PaymentLog>>

    @Transaction
    @Query("SELECT * FROM payment_logs ORDER BY date DESC")
    fun getAllPaymentLogsWithClient(): Flow<List<PaymentLogWithClient>>

    @Query("SELECT * FROM payment_logs WHERE clientId = :clientId ORDER BY date DESC")
    fun getPaymentLogsForClient(clientId: Int): Flow<List<PaymentLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(paymentLog: PaymentLog)

    @Query("SELECT SUM(amount) FROM payment_logs WHERE status = 'PAID' AND date >= :startDate AND date <= :endDate")
    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Double?>
    
    @Query("SELECT * FROM payment_logs WHERE clientId IS NULL ORDER BY date DESC")
    fun getManualIncomeLogs(): Flow<List<PaymentLog>>
}
