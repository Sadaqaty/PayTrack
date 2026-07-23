package studio.fixare.paytrack.worker

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import studio.fixare.paytrack.R
import studio.fixare.paytrack.data.Client
import studio.fixare.paytrack.data.ClientStatus
import studio.fixare.paytrack.data.PayTrackDatabase
import studio.fixare.paytrack.data.PaymentCycle
import studio.fixare.paytrack.data.PaymentLog
import studio.fixare.paytrack.data.UserPreferencesRepository
import studio.fixare.paytrack.data.dataStore
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PayTrackNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefsRepo = UserPreferencesRepository(applicationContext.dataStore)
        val notificationsEnabled = prefsRepo.notificationsEnabled.first()
        if (!notificationsEnabled) return Result.success()

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
            
            // Ensure we are comparing dates correctly by stripping time potentially, but let's rely on strict millisecond if cycles are clean
            // Or better, allow "today" to include everything up to end of today
            
            val diff = dueCal.timeInMillis - today.timeInMillis
            val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)

            // Notify if due today (0), tomorrow (1), or OVERDUE (negative days)
            // We check if nextDueDate is valid.
            
            // Correct calculation for days diff including negative values properly
            // Use helper to get accurate day difference
            val preciseDaysDiff = getDaysDifference(today, dueCal)

            if (preciseDaysDiff <= 1) { 
                dueClients.add(Pair(client, preciseDaysDiff))
            }
        }
        return dueClients
    }
    
    private fun getDaysDifference(today: Calendar, due: Calendar): Long {
        val t = today.clone() as Calendar
        val d = due.clone() as Calendar
        
        // Set both to start of day to avoid time issues
        t.set(Calendar.HOUR_OF_DAY, 0); t.set(Calendar.MINUTE, 0); t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0)
        d.set(Calendar.HOUR_OF_DAY, 0); d.set(Calendar.MINUTE, 0); d.set(Calendar.SECOND, 0); d.set(Calendar.MILLISECOND, 0)
        
        val diff = d.timeInMillis - t.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    private fun calculateNextDueDate(client: Client, logs: List<PaymentLog>): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = client.contractStartDate
        
        // First payment is due AFTER one cycle, not on join day
        incrementCycle(calendar, client.paymentCycle)
        
        val clientLogs = logs.filter { it.clientId == client.id }
        val today = Calendar.getInstance()
        
        // Check up to today + buffer
        // If a payment is overdue, it should return that past date.
        
        // If logic is correct, we iterate until we find a date > today OR an unpaid date <= today
        
        while (true) {
             // Check if this period is paid
             val isPaid = clientLogs.any { log ->
                 val logCal = Calendar.getInstance().apply { timeInMillis = log.date }
                 isSamePeriod(logCal, calendar, client.paymentCycle)
             }
             
             if (!isPaid) {
                 // Found the first unpaid date.
                 // It could be in the past (Overdue) or future (Next Due)
                 return calendar.timeInMillis
             }
             
             // If this date is paid, move to next cycle
             incrementCycle(calendar, client.paymentCycle)
             
             // Safety break: if we are way in the future (e.g., more than 1 cycle ahead of today), just return it as next due
             // We want to find the *first* unpaid, but we don't want to infinite loop if something is wrong.
             // Let's say if calendar is > today + 1 year, break?
             // Actually, logical break is when we pass today significantly if all previous are paid.
             // But we need to return the *first* unpaid.
             // If everything up to today is paid, the loop continues until it finds a future date which is naturally unpaid.
             // So 'true' loop is fine as long as we return.
        }
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
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
            }
            PaymentCycle.WEEKLY -> {
                val diff = kotlin.math.abs(cal1.timeInMillis - cal2.timeInMillis)
                val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)
                daysDiff < 4
            }
            PaymentCycle.DAILY -> cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
            PaymentCycle.HOURLY -> {
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY)
            }
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
            .setSmallIcon(R.drawable.paytrack) // Updated icon
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
