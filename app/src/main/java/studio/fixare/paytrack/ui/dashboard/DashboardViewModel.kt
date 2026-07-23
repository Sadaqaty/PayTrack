package studio.fixare.paytrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import studio.fixare.paytrack.data.Client
import studio.fixare.paytrack.data.ClientStatus
import studio.fixare.paytrack.data.PayTrackRepository
import studio.fixare.paytrack.data.PaymentCycle
import studio.fixare.paytrack.data.PaymentLog
import studio.fixare.paytrack.data.PaymentStatus
import studio.fixare.paytrack.data.UserPreferencesRepository
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
        
        val allDuePayments = (virtualDuePayments + manualPendingPayments)
            .sortedBy { it.date }

        // Map to UI Model with Client Name
        val pendingItems = allDuePayments.filter { it.status == PaymentStatus.PENDING }
            .map { log -> createDashboardPaymentItem(log, clients) }
            
        val overdueItems = allDuePayments.filter { it.status == PaymentStatus.OVERDUE }
            .map { log -> createDashboardPaymentItem(log, clients) }

        DashboardUiState(
            totalEarnedThisMonth = totalEarnedThisMonth,
            totalEarnedThisYear = totalEarnedThisYear,
            totalEarnedAllTime = totalEarnedAllTime,
            totalExpenseThisMonth = totalExpenseThisMonth,
            totalExpenseThisYear = totalExpenseThisYear,
            pendingPayments = pendingItems,
            overduePayments = overdueItems,
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
    
    private fun createDashboardPaymentItem(log: PaymentLog, clients: List<Client>): DashboardPaymentItem {
        val client = clients.find { it.id == log.clientId }
        return DashboardPaymentItem(
            log = log,
            clientName = client?.name ?: "Unknown Client",
            clientCurrency = log.originalCurrency ?: client?.currency ?: ""
        )
    }

    private fun calculateDuePayments(
        client: Client, 
        logs: List<PaymentLog>, 
        targetCurrency: String, 
        rates: Map<String, Double>
    ): List<PaymentLog> {
        val dueList = mutableListOf<PaymentLog>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = client.contractStartDate
        
        incrementCycle(calendar, client.paymentCycle)
        
        val today = Calendar.getInstance()
        val clientLogs = logs.filter { it.clientId == client.id }
        
        while (calendar.before(today) || isSamePeriod(calendar, today, client.paymentCycle)) {
            val periodDate = calendar.timeInMillis
            
            val isPaid = clientLogs.any { log ->
                 val logCal = Calendar.getInstance().apply { timeInMillis = log.date }
                 isSamePeriod(logCal, calendar, client.paymentCycle)
            }
            
            if (!isPaid) {
                val isOverdue = calendar.before(today) && !isSameDay(calendar, today)
                val status = if (isOverdue) PaymentStatus.OVERDUE else PaymentStatus.PENDING
                
                val clientRate = rates[client.currency] ?: 1.0
                val targetRate = rates[targetCurrency] ?: 1.0
                val baseAmount = if (clientRate != 0.0) client.rate / clientRate else client.rate
                val convertedAmount = baseAmount * targetRate
                
                dueList.add(PaymentLog(
                    id = (-1 * (client.id * 100000L + periodDate / 1000)).toInt(),
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
            PaymentCycle.HOURLY -> {
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY)
            }
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

data class DashboardPaymentItem(
    val log: PaymentLog,
    val clientName: String,
    val clientCurrency: String
)

data class DashboardUiState(
    val totalEarnedThisMonth: Double = 0.0,
    val totalEarnedThisYear: Double = 0.0,
    val totalEarnedAllTime: Double = 0.0,
    val totalExpenseThisMonth: Double = 0.0,
    val totalExpenseThisYear: Double = 0.0,
    val pendingPayments: List<DashboardPaymentItem> = emptyList(),
    val overduePayments: List<DashboardPaymentItem> = emptyList(),
    val recentActivity: List<ActivityItem> = emptyList(),
    val currencySymbol: String = "$"
)

sealed class ActivityItem {
    abstract val date: Long
    data class Payment(val log: PaymentLog) : ActivityItem() { override val date = log.date }
    data class ExpenseItem(val expense: studio.fixare.paytrack.data.Expense) : ActivityItem() { override val date = expense.date }
}
