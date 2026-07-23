package com.fixare.studio.paytrack.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixare.studio.paytrack.data.Client
import com.fixare.studio.paytrack.data.Expense
import com.fixare.studio.paytrack.data.PayTrackRepository
import com.fixare.studio.paytrack.data.PaymentLog
import com.fixare.studio.paytrack.data.PaymentLogWithClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed class TransactionItem {
    abstract val date: Long
    
    data class Income(val data: PaymentLogWithClient) : TransactionItem() {
        override val date: Long = data.paymentLog.date
    }
    
    data class ManualIncome(val log: PaymentLog) : TransactionItem() {
        override val date: Long = log.date
    }
    
    data class ExpenseItem(val data: Expense) : TransactionItem() {
        override val date: Long = data.date
    }
}

class LogViewModel(private val repository: PayTrackRepository) : ViewModel() {
    
    private val _filterClientId = MutableStateFlow<Int?>(null)
    val clients: StateFlow<List<Client>> = repository.getAllClients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<TransactionItem>> = combine(
        repository.getAllPaymentLogsWithClient(),
        repository.getAllExpenses(),
        _filterClientId
    ) { payments, expenses, filterId ->

        val filteredPayments = if (filterId != null) {
            payments.filter { it.client?.id == filterId }
        } else {
            payments
        }

        val incomeItems = filteredPayments.map { TransactionItem.Income(it) }

        val expenseItems = if (filterId == null) {
            expenses.map { TransactionItem.ExpenseItem(it) }
        } else {
            emptyList()
        }

        (incomeItems + expenseItems).sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun setClientFilter(clientId: Int?) {
        _filterClientId.value = clientId
    }
}
