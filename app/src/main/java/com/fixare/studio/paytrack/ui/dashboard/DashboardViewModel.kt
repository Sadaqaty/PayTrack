package com.fixare.studio.paytrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixare.studio.paytrack.data.Client
import com.fixare.studio.paytrack.data.ClientStatus
import com.fixare.studio.paytrack.data.PayTrackRepository
import com.fixare.studio.paytrack.data.PaymentCycle
import com.fixare.studio.paytrack.data.PaymentLog
import com.fixare.studio.paytrack.data.PaymentStatus
import com.fixare.studio.paytrack.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DashboardViewModel(
    private val repository: PayTrackRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        repository.getAllPaymentLogs(),
        repository.getAllClients(),
        repository.getAllExpenses(),
        userPreferencesRepository.localCurrency,
        userPreferencesRepository.exchangeRates
    ) { payments, clients, expenses, currency, rates ->
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // 1. Financial Totals (Based on ACTUAL PAID logs only)
        
        val currentMonthPayments = payments.filter {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
        }
        
        val currentYearPayments = payments.filter {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.YEAR) == currentYear
        }
        
        val currentMonthExpenses = expenses.filter {
             calendar.timeInMillis = it.date
             calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
        }
        
        val currentYearExpenses = expenses.filter {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.YEAR) == currentYear
        }

        val totalEarnedThisMonth = currentMonthPayments
            .filter { it.status == PaymentStatus.PAID }
            .sumOf { convert(it, currency, rates) }
            
        val totalEarnedThisYear = currentYearPayments
            .filter { it.status == PaymentStatus.PAID }
            .sumOf { convert(it, currency, rates) }
            
        val totalEarnedAllTime = payments
            .filter { it.status == PaymentStatus.PAID }
            .sumOf { convert(it, currency, rates) }
            
        val totalExpenseThisMonth = currentMonthExpenses.sumOf { it.amount }
        val totalExpenseThisYear = currentYearExpenses.sumOf { it.amount }

        // 2. Pending / Overdue Payments (Includes Virtual/Calculated payments)
        
        // A. Calculated dues from Active Clients
        val virtualDuePayments = clients
            .filter { it.status == ClientStatus.ACTIVE }
            .flatMap { client -> calculateDuePayments(client, payments, currency, rates) }
            
        // B. Existing Manual Pending/Overdue logs
        val manualPendingPayments = payments.filter { it.status == PaymentStatus.PENDING || it.status == PaymentStatus.OVERDUE }
        
        // Combine them (Prefer existing logs if they match a date? 
        // Logic in calculateDuePayments already skips periods covered by existing logs.
        // So we can just concat.)
        
        val allDuePayments = (virtualDuePayments + manualPendingPayments)
            .sortedBy { it.date } // Sort by date (oldest first usually implies overdue)

        val pendingPayments = allDuePayments.filter { it.status == PaymentStatus.PENDING }
        val overduePayments = allDuePayments.filter { it.status == PaymentStatus.OVERDUE }

        DashboardUiState(
            totalEarnedThisMonth = totalEarnedThisMonth,
            totalEarnedThisYear = totalEarnedThisYear,
            totalEarnedAllTime = totalEarnedAllTime,
            totalExpenseThisMonth = totalExpenseThisMonth,
            totalExpenseThisYear = totalExpenseThisYear,
            pendingPayments = pendingPayments,
            overduePayments = overduePayments,
            recentActivity = (payments.take(10).map { ActivityItem.Payment(it) } + expenses.take(10).map { ActivityItem.ExpenseItem(it) })
                .sortedByDescending { it.date }
                .take(10),
            currencySymbol = getCurrencySymbol(currency)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private fun calculateDuePayments(
        client: Client, 
        logs: List<PaymentLog>, 
        targetCurrency: String, 
        rates: Map<String, Double>
    ): List<PaymentLog> {
        val dueList = mutableListOf<PaymentLog>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = client.contractStartDate
        
        // Logic: First payment due AFTER one cycle
        incrementCycle(calendar, client.paymentCycle)
        
        val today = Calendar.getInstance()
        val clientLogs = logs.filter { it.clientId == client.id }
        
        // Iterate until today
        while (calendar.before(today) || isSamePeriod(calendar, today, client.paymentCycle)) {
            val periodDate = calendar.timeInMillis
            
            val isPaid = clientLogs.any { log ->
                 val logCal = Calendar.getInstance().apply { timeInMillis = log.date }
                 isSamePeriod(logCal, calendar, client.paymentCycle)
            }
            
            if (!isPaid) {
                // Overdue if strictly before today (not just same day)
                // Actually, if it's today, it's "Due Today" which is usually treated as Pending or Action Required.
                // Let's say anything before today is Overdue. Today is Pending.
                val isOverdue = calendar.before(today) && !isSameDay(calendar, today)
                val status = if (isOverdue) PaymentStatus.OVERDUE else PaymentStatus.PENDING
                
                // Convert Amount
                val clientRate = rates[client.currency] ?: 1.0
                val targetRate = rates[targetCurrency] ?: 1.0
                val baseAmount = if (clientRate != 0.0) client.rate / clientRate else client.rate
                val convertedAmount = baseAmount * targetRate
                
                dueList.add(PaymentLog(
                    id = (-1 * periodDate / 1000).toInt(), // Virtual ID (Negative to avoid collision with DB ids)
                    clientId = client.id,
                    amount = convertedAmount,
                    originalAmount = client.rate,
                    originalCurrency = client.currency,
                    date = periodDate,
                    status = status,
                    isManualIncome = false,
                    note = "Expected Payment"
                ))
            }
            
            incrementCycle(calendar, client.paymentCycle)
        }
        return dueList
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && 
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun incrementCycle(calendar: Calendar, cycle: PaymentCycle) {
        when (cycle) {
            PaymentCycle.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            PaymentCycle.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            PaymentCycle.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            PaymentCycle.HOURLY -> calendar.add(Calendar.HOUR_OF_DAY, 1)
        }
    }

    private fun isSamePeriod(cal1: Calendar, cal2: Calendar, cycle: PaymentCycle): Boolean {
        return when(cycle) {
            PaymentCycle.MONTHLY -> {
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && 
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
            }
            PaymentCycle.WEEKLY -> {
                val diff = abs(cal1.timeInMillis - cal2.timeInMillis)
                val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)
                daysDiff < 4
            }
            PaymentCycle.DAILY -> isSameDay(cal1, cal2)
            else -> false 
        }
    }

    private fun getCurrencySymbol(currencyCode: String): String {
        return try {
            java.util.Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            currencyCode
        }
    }

    private fun convert(log: PaymentLog, toCurrency: String, rates: Map<String, Double>): Double {
        if (log.originalAmount != null && log.originalCurrency != null) {
             val fromRate = rates[log.originalCurrency] ?: 1.0
             val toRate = rates[toCurrency] ?: 1.0
             val effectiveFromRate = if (fromRate == 0.0) 1.0 else fromRate
             return (log.originalAmount / effectiveFromRate) * toRate
        }
        return log.amount
    }
}

data class DashboardUiState(
    val totalEarnedThisMonth: Double = 0.0,
    val totalEarnedThisYear: Double = 0.0,
    val totalEarnedAllTime: Double = 0.0,
    val totalExpenseThisMonth: Double = 0.0,
    val totalExpenseThisYear: Double = 0.0,
    val pendingPayments: List<PaymentLog> = emptyList(),
    val overduePayments: List<PaymentLog> = emptyList(),
    val recentActivity: List<ActivityItem> = emptyList(),
    val currencySymbol: String = "$"
)

sealed class ActivityItem {
    abstract val date: Long
    data class Payment(val log: PaymentLog) : ActivityItem() { override val date = log.date }
    data class ExpenseItem(val expense: com.fixare.studio.paytrack.data.Expense) : ActivityItem() { override val date = expense.date }
}
