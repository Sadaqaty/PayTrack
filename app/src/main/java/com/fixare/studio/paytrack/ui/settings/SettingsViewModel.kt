package com.fixare.studio.paytrack.ui.settings

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixare.studio.paytrack.data.Expense
import com.fixare.studio.paytrack.data.PayTrackRepository
import com.fixare.studio.paytrack.data.PaymentLog
import com.fixare.studio.paytrack.data.PaymentStatus
import com.fixare.studio.paytrack.data.UserPreferencesRepository
import com.fixare.studio.paytrack.utils.CsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(
    private val repository: PayTrackRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val localCurrency: Flow<String> = userPreferencesRepository.localCurrency
    val userName: Flow<String> = userPreferencesRepository.userName
    val companyName: Flow<String> = userPreferencesRepository.companyName

    fun setLocalCurrency(currency: String) {
        viewModelScope.launch {
            userPreferencesRepository.setLocalCurrency(currency)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserName(name)
        }
    }

    fun setCompanyName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCompanyName(name)
        }
    }

    fun exportData(context: Context) {
        viewModelScope.launch {
            try {
                val logs = repository.getAllPaymentLogs().first()
                val expenses = repository.getAllExpenses().first()
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                
                withContext(Dispatchers.IO) {
                    // Export Payments
                    exportToCsv(context, "PayTrack_Payments_$timestamp.csv") { stream ->
                        CsvExporter().exportPaymentLogs(logs, stream)
                    }

                    // Export Expenses
                    exportToCsv(context, "PayTrack_Expenses_$timestamp.csv") { stream ->
                        CsvExporter().exportExpenses(expenses, stream)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export complete! Check Downloads folder.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun exportToCsv(context: Context, fileName: String, writeBlock: (java.io.OutputStream) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                try {
                    resolver.openOutputStream(it)?.use { stream ->
                        writeBlock(stream)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    throw e
                }
            } ?: throw IOException("Failed to create file in Downloads")
        } else {
            // Legacy storage handling for Android 9 and below
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            try {
                FileOutputStream(file).use { stream ->
                    writeBlock(stream)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                throw e
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val fileName = getFileName(context, uri) ?: "Unknown"
                
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IOException("Could not open file")
                    val reader = BufferedReader(InputStreamReader(inputStream))

                    reader.use {
                        val header = it.readLine()
                        if (header != null) {
                            if (header.startsWith("ID,Client ID,Amount")) {
                                // Import Payments
                                importPayments(it)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Imported payments from $fileName", Toast.LENGTH_SHORT).show()
                                }
                            } else if (header.startsWith("ID,Amount,Category")) {
                                // Import Expenses
                                importExpenses(it)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Imported expenses from $fileName", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Unknown CSV format", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result?.substring(cut + 1)
                }
            }
        }
        return result
    }

    private suspend fun importPayments(reader: BufferedReader) {
        var line = reader.readLine()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        while (line != null) {
            val tokens = line.split(",")
            // Expect: ID,Client ID,Amount,Date,Status,Note
            if (tokens.size >= 6) {
                try {
                     // Note: We are ignoring ID to let Room auto-generate new ones to avoid conflicts
                     val clientIdStr = tokens[1]
                     val clientId = if(clientIdStr == "null") null else clientIdStr.toInt()
                     val amount = tokens[2].toDouble()
                     val date = dateFormat.parse(tokens[3])?.time ?: System.currentTimeMillis()
                     val status = PaymentStatus.valueOf(tokens[4])
                     val note = if(tokens.size > 5) tokens[5] else ""
                     
                     repository.insertPaymentLog(
                         PaymentLog(
                             clientId = clientId,
                             amount = amount,
                             date = date,
                             status = status,
                             note = note
                         )
                     )
                } catch (e: Exception) {
                    // skip invalid line
                }
            }
            line = reader.readLine()
        }
    }

    private suspend fun importExpenses(reader: BufferedReader) {
        var line = reader.readLine()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        while (line != null) {
            val tokens = line.split(",")
            // Expect: ID,Amount,Category,Date,Note
            if (tokens.size >= 5) {
                try {
                    val amount = tokens[1].toDouble()
                    val category = tokens[2]
                    val date = dateFormat.parse(tokens[3])?.time ?: System.currentTimeMillis()
                    val note = if(tokens.size > 4) tokens[4] else ""
                    
                    repository.insertExpense(
                        Expense(
                            amount = amount,
                            category = category,
                            date = date,
                            note = note
                        )
                    )
                } catch (e: Exception) {
                    // skip invalid line
                }
            }
             line = reader.readLine()
        }
    }
}
