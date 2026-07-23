package studio.fixare.paytrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_logs")
data class PaymentLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int? = null, // Nullable for manual side-hustle income
    val amount: Double, // Amount in USER'S local currency
    val originalAmount: Double? = null, // Amount in CLIENT'S currency
    val originalCurrency: String? = null, // Currency code of client
    val date: Long,
    val status: PaymentStatus,
    val note: String = "",
    val isManualIncome: Boolean = false
)

enum class PaymentStatus {
    PAID, PENDING, OVERDUE
}
