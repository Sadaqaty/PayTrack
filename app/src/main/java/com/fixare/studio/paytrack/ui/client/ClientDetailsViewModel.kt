package com.fixare.studio.paytrack.ui.client

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixare.studio.paytrack.data.Client
import com.fixare.studio.paytrack.data.ClientStatus
import com.fixare.studio.paytrack.data.PayTrackRepository
import com.fixare.studio.paytrack.data.PaymentCycle
import com.fixare.studio.paytrack.data.PaymentLog
import com.fixare.studio.paytrack.data.PaymentStatus
import com.fixare.studio.paytrack.data.UserPreferencesRepository
import com.fixare.studio.paytrack.utils.PdfInvoiceGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar

class ClientDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PayTrackRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val clientId: Int = checkNotNull(savedStateHandle["clientId"])
    
    private val _uiState = MutableStateFlow(ClientDetailsUiState())
    val uiState: StateFlow<ClientDetailsUiState> = _uiState.asStateFlow()

    init {
        loadClientDetails()
    }

    private fun loadClientDetails() {
        viewModelScope.launch {
            val client = repository.getClientById(clientId) ?: return@launch
            
            repository.getPaymentLogsForClient(clientId).collect { logs ->
                val expectedPayments = if (client.status == ClientStatus.ACTIVE) {
                    generateExpectedPayments(client, logs)
                } else {
                    emptyList()
                }
                
                _uiState.value = ClientDetailsUiState(
                    client = client,
                    paymentHistory = logs,
                    expectedPayments = expectedPayments
                )
            }
        }
    }

    private fun generateExpectedPayments(client: Client, logs: List<PaymentLog>): List<ExpectedPayment> {
        val expectedList = mutableListOf<ExpectedPayment>()
        val calendar = Calendar.getInstance()
        
        // Start checking from the contract start date
        calendar.timeInMillis = client.contractStartDate

        // First payment should be AFTER one cycle, not on the join date
        // Increment cycle once before starting the check loop
        when (client.paymentCycle) {
            PaymentCycle.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            PaymentCycle.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            PaymentCycle.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            PaymentCycle.HOURLY -> calendar.add(Calendar.HOUR_OF_DAY, 1) 
        }
        
        // Limit to current date (don't show future months as pending yet)
        val today = Calendar.getInstance()
        
        while (calendar.before(today) || isSamePeriod(calendar, today, client.paymentCycle)) {
            val periodDate = calendar.timeInMillis
            
            // Check if this specific cycle period is covered by a log
            val existingLog = logs.find { log -> 
                 val logCal = Calendar.getInstance().apply { timeInMillis = log.date }
                 isSamePeriod(logCal, calendar, client.paymentCycle)
            }
            
            val status = if (existingLog != null) {
                PaymentStatus.PAID 
            } else {
                // If period date is in the past relative to today (with some buffer), it's overdue or pending
                // If strictly "till current month", we include it.
                if (calendar.before(today)) PaymentStatus.OVERDUE else PaymentStatus.PENDING
            }

            // Only add to "Expected/Pending" list if it's NOT paid yet
            if (existingLog == null) {
                 expectedList.add(
                    ExpectedPayment(
                        dueDate = periodDate,
                        amount = client.rate,
                        status = status
                    )
                )
            }

            // Increment cycle
            when (client.paymentCycle) {
                PaymentCycle.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                PaymentCycle.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                PaymentCycle.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                PaymentCycle.HOURLY -> calendar.add(Calendar.HOUR_OF_DAY, 1) 
            }
        }
        
        // Sort: Overdue first, then pending/future
        return expectedList.sortedBy { it.dueDate }
    }
    
    private fun isSamePeriod(cal1: Calendar, cal2: Calendar, cycle: PaymentCycle): Boolean {
        return when(cycle) {
            PaymentCycle.MONTHLY -> {
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
            }
            PaymentCycle.WEEKLY -> {
                val diff = kotlin.math.abs(cal1.timeInMillis - cal2.timeInMillis)
                val daysDiff = diff / (1000 * 60 * 60 * 24)
                daysDiff < 4
            }
            PaymentCycle.DAILY -> {
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
            }
            PaymentCycle.HOURLY -> {
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY)
            }
        }
    }

    fun markAsPaid(expectedPayment: ExpectedPayment) {
        viewModelScope.launch {
            val client = _uiState.value.client ?: return@launch
            val targetCurrency = userPreferencesRepository.localCurrency.first()
            val rates = userPreferencesRepository.exchangeRates.first()
            
            // Auto-convert currency
            val clientRate = rates[client.currency] ?: 1.0
            val targetRate = rates[targetCurrency] ?: 1.0
            
            // Convert to Base (USD) then to Target
            
            val amountInBase = if (clientRate != 0.0) expectedPayment.amount / clientRate else expectedPayment.amount
            val convertedAmount = amountInBase * targetRate
            
            repository.insertPaymentLog(
                PaymentLog(
                    clientId = client.id,
                    amount = convertedAmount, // In User's Local Currency
                    originalAmount = expectedPayment.amount, // In Client's Currency
                    originalCurrency = client.currency,
                    date = expectedPayment.dueDate, 
                    status = PaymentStatus.PAID,
                    note = "Auto-converted from ${client.currency}"
                )
            )
        }
    }
    
    fun markClientAsCompleted() {
        viewModelScope.launch {
            val client = _uiState.value.client ?: return@launch
            repository.updateClient(client.copy(status = ClientStatus.COMPLETED))
            // Reload to update UI state
            loadClientDetails()
        }
    }
    
    fun deleteClient() {
        viewModelScope.launch {
             val client = _uiState.value.client ?: return@launch
             repository.deleteClient(client)
        }
    }

    fun generateInvoice(context: Context, paymentLog: PaymentLog) {
        viewModelScope.launch {
            val client = _uiState.value.client ?: return@launch
            val userName = userPreferencesRepository.userName.first()
            val companyName = userPreferencesRepository.companyName.first()
            
            try {
                val fileName = "Invoice_${client.name}_${paymentLog.id}.pdf"
                
                withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                        uri?.let {
                            resolver.openOutputStream(it)?.use { stream ->
                                PdfInvoiceGenerator(context).generateInvoice(paymentLog, client, stream, userName, companyName)
                            }
                        } ?: throw IOException("Failed to create file")
                    } else {
                        // Legacy storage handling for Android 9 and below
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val file = File(downloadsDir, fileName)
                        FileOutputStream(file).use { stream ->
                            PdfInvoiceGenerator(context).generateInvoice(paymentLog, client, stream, userName, companyName)
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Invoice saved to Downloads", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error generating invoice: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun generatePendingInvoice(context: Context, expectedPayment: ExpectedPayment) {
        viewModelScope.launch {
            val client = _uiState.value.client ?: return@launch
            val userName = userPreferencesRepository.userName.first()
            val companyName = userPreferencesRepository.companyName.first()
            
            val pendingLog = PaymentLog(
                id = 0, 
                clientId = client.id,
                amount = expectedPayment.amount, 
                originalAmount = expectedPayment.amount,
                originalCurrency = client.currency,
                date = expectedPayment.dueDate,
                status = expectedPayment.status
            )
            
            try {
                val fileName = "Invoice_${client.name}_PENDING_${expectedPayment.dueDate}.pdf"

                withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }

                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                        uri?.let {
                            resolver.openOutputStream(it)?.use { stream ->
                                PdfInvoiceGenerator(context).generateInvoice(pendingLog, client, stream, userName, companyName)
                            }
                        } ?: throw IOException("Failed to create file")
                    } else {
                         // Legacy storage handling for Android 9 and below
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val file = File(downloadsDir, fileName)
                        FileOutputStream(file).use { stream ->
                            PdfInvoiceGenerator(context).generateInvoice(pendingLog, client, stream, userName, companyName)
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Pending Invoice saved to Downloads", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error generating invoice: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

data class ClientDetailsUiState(
    val client: Client? = null,
    val paymentHistory: List<PaymentLog> = emptyList(),
    val expectedPayments: List<ExpectedPayment> = emptyList()
)

data class ExpectedPayment(
    val dueDate: Long,
    val amount: Double, // In Client Currency
    val status: PaymentStatus
)
