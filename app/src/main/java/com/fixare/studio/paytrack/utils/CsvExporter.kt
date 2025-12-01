package com.fixare.studio.paytrack.utils

import com.fixare.studio.paytrack.data.Expense
import com.fixare.studio.paytrack.data.PaymentLog
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class CsvExporter {
    fun exportPaymentLogs(logs: List<PaymentLog>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        
        writer.write("ID,Client ID,Amount,Date,Status,Note\n")
        logs.forEach { log ->
            val dateStr = dateFormat.format(java.util.Date(log.date))
            val noteEscaped = log.note.replace(",", " ") // basic escape
            writer.write("${log.id},${log.clientId},${log.amount},$dateStr,${log.status},$noteEscaped\n")
        }
        writer.flush()
    }

    fun exportExpenses(expenses: List<Expense>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        
        writer.write("ID,Amount,Category,Date,Note\n")
        expenses.forEach { expense ->
            val dateStr = dateFormat.format(java.util.Date(expense.date))
            val noteEscaped = expense.note.replace(",", " ")
            writer.write("${expense.id},${expense.amount},${expense.category},$dateStr,$noteEscaped\n")
        }
        writer.flush()
    }
}
