package com.fixare.studio.paytrack.data

import kotlinx.coroutines.flow.Flow

interface PayTrackRepository {
    fun getAllClients(): Flow<List<Client>>
    suspend fun getClientById(id: Int): Client?
    suspend fun insertClient(client: Client)
    suspend fun updateClient(client: Client)
    suspend fun deleteClient(client: Client)

    fun getAllPaymentLogs(): Flow<List<PaymentLog>>
    fun getAllPaymentLogsWithClient(): Flow<List<PaymentLogWithClient>>
    fun getPaymentLogsForClient(clientId: Int): Flow<List<PaymentLog>>
    fun getManualIncomeLogs(): Flow<List<PaymentLog>>
    suspend fun insertPaymentLog(paymentLog: PaymentLog)
    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Double?>

    fun getAllExpenses(): Flow<List<Expense>>
    suspend fun insertExpense(expense: Expense)
    fun getTotalExpenses(startDate: Long, endDate: Long): Flow<Double?>
}

class PayTrackRepositoryImpl(
    private val clientDao: ClientDao,
    private val paymentLogDao: PaymentLogDao,
    private val expenseDao: ExpenseDao
) : PayTrackRepository {
    override fun getAllClients(): Flow<List<Client>> = clientDao.getAllClients()
    override suspend fun getClientById(id: Int): Client? = clientDao.getClientById(id)
    override suspend fun insertClient(client: Client) = clientDao.insert(client)
    override suspend fun updateClient(client: Client) = clientDao.update(client)
    override suspend fun deleteClient(client: Client) = clientDao.delete(client)

    override fun getAllPaymentLogs(): Flow<List<PaymentLog>> = paymentLogDao.getAllPaymentLogs()
    override fun getAllPaymentLogsWithClient(): Flow<List<PaymentLogWithClient>> = paymentLogDao.getAllPaymentLogsWithClient()
    override fun getPaymentLogsForClient(clientId: Int): Flow<List<PaymentLog>> = paymentLogDao.getPaymentLogsForClient(clientId)
    override fun getManualIncomeLogs(): Flow<List<PaymentLog>> = paymentLogDao.getManualIncomeLogs()
    override suspend fun insertPaymentLog(paymentLog: PaymentLog) = paymentLogDao.insert(paymentLog)
    override fun getTotalIncome(startDate: Long, endDate: Long): Flow<Double?> = paymentLogDao.getTotalIncome(startDate, endDate)

    override fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
    override suspend fun insertExpense(expense: Expense) = expenseDao.insert(expense)
    override fun getTotalExpenses(startDate: Long, endDate: Long): Flow<Double?> = expenseDao.getTotalExpenses(startDate, endDate)
}
