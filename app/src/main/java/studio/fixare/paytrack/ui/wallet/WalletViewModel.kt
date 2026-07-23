package studio.fixare.paytrack.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import studio.fixare.paytrack.data.Expense
import studio.fixare.paytrack.data.PayTrackRepository
import studio.fixare.paytrack.data.PaymentLog
import studio.fixare.paytrack.data.PaymentStatus
import studio.fixare.paytrack.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletViewModel(
    private val repository: PayTrackRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    val walletUiState: StateFlow<WalletUiState> = combine(
        repository.getAllPaymentLogs(),
        repository.getAllExpenses(),
        userPreferencesRepository.localCurrency,
        userPreferencesRepository.exchangeRates
    ) { payments, expenses, targetCurrency, rates ->
        
        fun convert(amount: Double, originalAmount: Double?, originalCurrency: String?): Double {
            if (originalCurrency != null && originalAmount != null) {
                if (originalCurrency == targetCurrency) return originalAmount
                
                // Rates are relative to USD (Base)
                // Rate = how many units of currency per 1 USD
                // e.g. USD=1.0, EUR=0.92
                
                val fromRate = rates[originalCurrency] ?: 1.0
                val toRate = rates[targetCurrency] ?: 1.0
                
                // Convert to Base (USD)
                val amountInBase = if (fromRate != 0.0) originalAmount / fromRate else originalAmount
                // Convert to Target
                return amountInBase * toRate
            }
            // Fallback: assume amount is already in target currency (legacy behavior)
            // or we can't convert.
            return amount
        }

        val totalIncome = payments.filter { it.status == PaymentStatus.PAID }.sumOf { 
            convert(it.amount, it.originalAmount, it.originalCurrency) 
        }
        
        val totalExpenses = expenses.sumOf { 
            convert(it.amount, it.originalAmount, it.originalCurrency)
        }
        
        val balance = totalIncome - totalExpenses
        
        // We should probably also update the "amount" field in the expense list for display 
        // if we want the UI to show the converted amount.
        // However, the Expense data class is immutable from DB. 
        // We can wrap it or just let the UI show the stored amount (which might be wrong currency).
        // Ideally, we return a UI model. For now, we'll stick to total calculation fixes.
        // But wait, if the list shows "100" but total is calculated as "90" (converted), it's confusing.
        // The ExpenseItem in UI just shows expense.amount.
        // We should map expenses to a UI model with converted amount.
        
        val convertedExpenses = expenses.map { expense ->
            expense.copy(
                amount = convert(expense.amount, expense.originalAmount, expense.originalCurrency)
            )
        }

        WalletUiState(
            balance = balance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            localCurrency = targetCurrency,
            recentExpenses = convertedExpenses.sortedByDescending { it.date }.take(10)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WalletUiState()
    )

    fun addExpense(amount: Double, category: String, date: Long, note: String) {
        viewModelScope.launch {
            val currentCurrency = userPreferencesRepository.localCurrency.first()
            repository.insertExpense(
                Expense(
                    amount = amount,
                    category = category,
                    date = date,
                    note = note,
                    originalAmount = amount,
                    originalCurrency = currentCurrency
                )
            )
        }
    }
    
    fun addManualIncome(amount: Double, date: Long, note: String) {
        viewModelScope.launch {
            val currentCurrency = userPreferencesRepository.localCurrency.first()
            repository.insertPaymentLog(
                PaymentLog(
                    clientId = null, // Manual income has no client
                    amount = amount,
                    originalAmount = amount,
                    originalCurrency = currentCurrency,
                    date = date,
                    status = PaymentStatus.PAID,
                    note = note,
                    isManualIncome = true
                )
            )
        }
    }
}

data class WalletUiState(
    val balance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val localCurrency: String = "USD",
    val recentExpenses: List<Expense> = emptyList()
)
