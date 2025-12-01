package com.fixare.studio.paytrack.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixare.studio.paytrack.data.Expense
import com.fixare.studio.paytrack.data.PayTrackRepository
import com.fixare.studio.paytrack.data.PaymentLog
import com.fixare.studio.paytrack.data.PaymentStatus
import com.fixare.studio.paytrack.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletViewModel(
    private val repository: PayTrackRepository
) : ViewModel() {
    
    val walletUiState: StateFlow<WalletUiState> = combine(
        repository.getAllPaymentLogs(),
        repository.getAllExpenses()
    ) { payments, expenses ->
        val totalIncome = payments.filter { it.status == PaymentStatus.PAID }.sumOf { it.amount }
        val totalExpenses = expenses.sumOf { it.amount }
        val balance = totalIncome - totalExpenses
        
        WalletUiState(
            balance = balance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            recentExpenses = expenses.take(10)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WalletUiState()
    )

    fun addExpense(amount: Double, category: String, date: Long, note: String) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    amount = amount,
                    category = category,
                    date = date,
                    note = note
                )
            )
        }
    }
    
    fun addExpenseWithCurrency(amount: Double, category: String, date: Long, note: String, currency: String, userPrefs: UserPreferencesRepository) {
        viewModelScope.launch {
            val targetCurrency = userPrefs.localCurrency.first()
            val rates = userPrefs.exchangeRates.first()
            
            // Convert if different
            val finalAmount = if (currency != targetCurrency) {
                val fromRate = rates[currency] ?: 1.0
                val toRate = rates[targetCurrency] ?: 1.0
                // Convert: From -> USD -> To
                val amountInBase = if (fromRate != 0.0) amount / fromRate else amount
                amountInBase * toRate
            } else {
                amount
            }
            
            repository.insertExpense(
                Expense(
                    amount = finalAmount,
                    category = category,
                    date = date,
                    note = "$note (Original: $currency $amount)",
                    originalAmount = amount,
                    originalCurrency = currency
                )
            )
        }
    }
    
    fun addManualIncome(amount: Double, date: Long, note: String) {
        viewModelScope.launch {
            repository.insertPaymentLog(
                PaymentLog(
                    clientId = null, // Manual income has no client
                    amount = amount,
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
    val recentExpenses: List<Expense> = emptyList()
)
