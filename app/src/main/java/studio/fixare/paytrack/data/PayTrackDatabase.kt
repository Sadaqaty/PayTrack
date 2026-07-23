package studio.fixare.paytrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Client::class, PaymentLog::class, Expense::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PayTrackDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun paymentLogDao(): PaymentLogDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var Instance: PayTrackDatabase? = null

        fun getDatabase(context: Context): PayTrackDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, PayTrackDatabase::class.java, "paytrack_database")
                    .fallbackToDestructiveMigrationFrom(1, 2)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
