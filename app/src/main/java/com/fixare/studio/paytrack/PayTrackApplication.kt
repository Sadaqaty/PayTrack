package com.fixare.studio.paytrack

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fixare.studio.paytrack.data.PayTrackDatabase
import com.fixare.studio.paytrack.data.PayTrackRepository
import com.fixare.studio.paytrack.data.PayTrackRepositoryImpl
import com.fixare.studio.paytrack.data.UserPreferencesRepository
import com.fixare.studio.paytrack.data.dataStore
import com.fixare.studio.paytrack.worker.CurrencySyncWorker
import com.fixare.studio.paytrack.worker.PayTrackNotificationWorker
import java.util.concurrent.TimeUnit

class PayTrackApplication : Application() {
    lateinit var repository: PayTrackRepository
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        val database = PayTrackDatabase.getDatabase(this)
        repository = PayTrackRepositoryImpl(database.clientDao(), database.paymentLogDao(), database.expenseDao())
        userPreferencesRepository = UserPreferencesRepository(dataStore)

        createNotificationChannel()
        setupWorker()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Payment Reminders"
            val descriptionText = "Notifications for due payments"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("paytrack_reminders", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupWorker() {
        // Notification Worker
        val notificationWorkRequest = PeriodicWorkRequestBuilder<PayTrackNotificationWorker>(
            12, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueue(notificationWorkRequest)
        
        // Currency Sync Worker (Daily)
        val currencySyncWorkRequest = PeriodicWorkRequestBuilder<CurrencySyncWorker>(
            1, TimeUnit.DAYS
        ).build()
        
        WorkManager.getInstance(this).enqueue(currencySyncWorkRequest)
    }
}
