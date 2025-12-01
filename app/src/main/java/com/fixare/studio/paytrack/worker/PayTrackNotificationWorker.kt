package com.fixare.studio.paytrack.worker

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fixare.studio.paytrack.R
import com.fixare.studio.paytrack.data.Client
import com.fixare.studio.paytrack.data.ClientStatus
import com.fixare.studio.paytrack.data.PayTrackDatabase
import com.fixare.studio.paytrack.data.PaymentCycle
import com.fixare.studio.paytrack.data.PaymentLog
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PayTrackNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = PayTrackDatabase.getDatabase(applicationContext)
        val clientDao = database.clientDao()
        val paymentLogDao = database.paymentLogDao()

        // Get only ACTIVE clients
        val clients = clientDao.getAllClients().first().filter { it.status == ClientStatus.ACTIVE }
        val logs = paymentLogDao.getAllPaymentLogs().first()

        val duePayments = checkForDuePayments(clients, logs)

        if (duePayments.isNotEmpty()) {
            sendNotification(duePayments)
        }

        return Result.success()
    }

    private fun checkForDuePayments(clients: List<Client>, logs: List<PaymentLog>): List<Pair<Client, Long>> {
        val dueClients = mutableListOf<Pair<Client, Long>>() // Client and Days Overdue/Due in
        val today = Calendar.getInstance()

        clients.forEach { client ->
            val nextDueDate = calculateNextDueDate(client, logs)
            
            val dueCal = Calendar.getInstance().apply { timeInMillis = nextDueDate }
            
            val diff = dueCal.timeInMillis - today.timeInMillis
            val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)

            // Notify if due today, tomorrow, or OVERDUE (negative days)
            // We want to keep reminding if it's overdue
            if (daysDiff <= 1) { 
                dueClients.add(Pair(client, daysDiff))
            }
        }
        return dueClients
    }

    private fun calculateNextDueDate(client: Client, logs: List<PaymentLog>): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = client.contractStartDate
        
        // First payment is due AFTER one cycle, not on join day
        incrementCycle(calendar, client.paymentCycle)
        
        val clientLogs = logs.filter { it.clientId == client.id }
        val today = Calendar.getInstance()
        
        // Same logic as ViewModel: find first unpaid period
        // But check only up to "today + 1 cycle" to avoid infinite loops if logic fails, though while(before) is safe
        
        while (calendar.before(today) || isSamePeriod(calendar, today, client.paymentCycle)) {
             // Check if this period is paid
             val isPaid = clientLogs.any { log ->
                 val logCal = Calendar.getInstance().apply { timeInMillis = log.date }
                 isSamePeriod(logCal, calendar, client.paymentCycle)
             }
             
             if (!isPaid) {
                 return calendar.timeInMillis
             }
             
             incrementCycle(calendar, client.paymentCycle)
        }
        return calendar.timeInMillis // Next future due date
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
                val diff = kotlin.math.abs(cal1.timeInMillis - cal2.timeInMillis)
                val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)
                daysDiff < 20
            }
            PaymentCycle.WEEKLY -> {
                val diff = kotlin.math.abs(cal1.timeInMillis - cal2.timeInMillis)
                val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)
                daysDiff < 4
            }
            PaymentCycle.DAILY -> cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
            else -> false 
        }
    }

    private fun sendNotification(dueList: List<Pair<Client, Long>>) {
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val channelId = "paytrack_reminders"
        
        // Craft a more detailed message
        val contentTitle = "Payment Reminders"
        val contentText = if (dueList.size == 1) {
            val (client, days) = dueList[0]
            if (days < 0) {
                "Payment OVERDUE for ${client.name} by ${kotlin.math.abs(days)} days!"
            } else if (days == 0L) {
                "Payment due TODAY for ${client.name}"
            } else {
                "Payment due tomorrow for ${client.name}"
            }
        } else {
            "You have ${dueList.size} payments due or overdue."
        }
        
        val bigTextStyle = NotificationCompat.BigTextStyle()
        val sb = StringBuilder()
        dueList.forEach { (client, days) ->
            if (days < 0) {
                sb.append("• ${client.name}: OVERDUE (${kotlin.math.abs(days)} days)\n")
            } else if (days == 0L) {
                sb.append("• ${client.name}: Due Today\n")
            } else {
                sb.append("• ${client.name}: Due Tomorrow\n")
            }
        }
        bigTextStyle.bigText(sb.toString())

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for money matters!
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
