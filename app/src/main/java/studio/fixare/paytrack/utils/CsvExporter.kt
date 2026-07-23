package studio.fixare.paytrack.utils

import studio.fixare.paytrack.data.Client
import studio.fixare.paytrack.data.Expense
import studio.fixare.paytrack.data.PaymentLog
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class CsvExporter {
    
    private fun escape(value: String): String {
        var processed = value.replace("\"", "\"\"") // Escape double quotes
        if (processed.contains(",") || processed.contains("\n") || value.contains("\"")) {
            processed = "\"$processed\""
        }
        return processed
    }

    fun exportClients(clients: List<Client>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter()
        writer.write("ID,Name,Project Name,Contract Start,Payment Cycle,Rate,Currency,Status,Notes\n")
        clients.forEach { client ->
            val name = escape(client.name)
            val project = escape(client.projectName)
            val notes = escape(client.notes)
            writer.write("${client.id},$name,$project,${client.contractStartDate},${client.paymentCycle},${client.rate},${client.currency},${client.status},$notes\n")
        }
        writer.flush()
    }

    fun exportPaymentLogs(logs: List<PaymentLog>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        
        writer.write("ID,Client ID,Amount,Date,Status,Note,IsManual,OriginalAmount,OriginalCurrency\n")
        logs.forEach { log ->
            val dateStr = dateFormat.format(java.util.Date(log.date))
            val note = escape(log.note)
            writer.write("${log.id},${log.clientId},${log.amount},$dateStr,${log.status},$note,${log.isManualIncome},${log.originalAmount ?: ""},${log.originalCurrency ?: ""}\n")
        }
        writer.flush()
    }

    fun exportExpenses(expenses: List<Expense>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        
        writer.write("ID,Amount,Category,Date,Note\n")
        expenses.forEach { expense ->
            val dateStr = dateFormat.format(java.util.Date(expense.date))
            val category = escape(expense.category)
            val note = escape(expense.note)
            writer.write("${expense.id},${expense.amount},$category,$dateStr,$note\n")
        }
        writer.flush()
    }
}
