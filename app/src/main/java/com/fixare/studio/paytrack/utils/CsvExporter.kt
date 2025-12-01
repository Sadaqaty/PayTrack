package com.fixare.studio.paytrack.utils

import com.fixare.studio.paytrack.data.Expense
import com.fixare.studio.paytrack.data.PaymentLog
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter {

    fun exportPaymentLogs(logs: List<PaymentLog>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter()
        writer.use {
            it.write("ID,Client ID,Amount,Date,Status,Note\n")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            for (log in logs) {
                val dateStr = dateFormat.format(Date(log.date))
                val noteEscaped = log.note.replace(",", " ")
                it.write("${log.id},${log.clientId},${log.amount},$dateStr,${log.status},$noteEscaped\n")
            }
        }
    }

    fun exportExpenses(expenses: List<Expense>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter()
        writer.use {
            it.write("ID,Amount,Category,Date,Note\n")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            for (expense in expenses) {
                val dateStr = dateFormat.format(Date(expense.date))
                val noteEscaped = expense.note.replace(",", " ")
                it.write("${expense.id},${expense.amount},${expense.category},$dateStr,$noteEscaped\n")
            }
        }
    }
}
