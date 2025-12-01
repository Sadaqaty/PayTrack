package com.fixare.studio.paytrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixare.studio.paytrack.data.PayTrackRepository
import com.fixare.studio.paytrack.data.PaymentLog
import com.fixare.studio.paytrack.data.PaymentStatus
import com.fixare.studio.paytrack.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class DashboardViewModel(
    private val repository: PayTrackRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _paymentLogs = repository.getAllPaymentLogs()

    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        _paymentLogs,
        repository.getAllExpenses(),
        userPreferencesRepository.localCurrency,
        userPreferencesRepository.exchangeRates
    ) { payments, expenses, currency, rates ->
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Filter for current month
        val currentMonthPayments = payments.filter {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
        }
        
        val currentMonthExpenses = expenses.filter {
             calendar.timeInMillis = it.date
             calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
        }
        
        // Yearly calculations
        val currentYearPayments = payments.filter {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.YEAR) == currentYear
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
            
        val totalExpenseThisMonth = currentMonthExpenses.sumOf { it.amount } // Expenses assumed in local or handled separately if needed
        val totalExpenseThisYear = currentYearExpenses.sumOf { it.amount }

        val pendingPayments = payments.filter { it.status == PaymentStatus.PENDING }
        val overduePayments = payments.filter { it.status == PaymentStatus.OVERDUE }

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

    private fun getCurrencySymbol(currencyCode: String): String {
        return try {
            java.util.Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            currencyCode
        }
    }

    private fun convert(log: PaymentLog, toCurrency: String, rates: Map<String, Double>): Double {
        // If we have original data, recalculate fresh conversion to current target currency
        if (log.originalAmount != null && log.originalCurrency != null) {
             val fromRate = rates[log.originalCurrency] ?: 1.0
             val toRate = rates[toCurrency] ?: 1.0
             
             // Convert: Client -> USD -> Target
             // Formula: (Amount / ClientRate) * TargetRate
             // Assuming rates are "USD to X"
             
             // Safety check for zero rate
             val effectiveFromRate = if (fromRate == 0.0) 1.0 else fromRate
             
             return (log.originalAmount / effectiveFromRate) * toRate
        }
        
        // If no original currency (Manual Income), we assume it's 1:1 value OR we can't easily convert it without knowing what it was.
        // For now, return raw amount.
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
