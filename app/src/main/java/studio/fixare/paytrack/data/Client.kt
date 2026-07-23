package studio.fixare.paytrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val projectName: String,
    val contractStartDate: Long, // Timestamp
    val paymentCycle: PaymentCycle,
    val rate: Double,
    val currency: String = "USD", // The currency the client pays in
    val status: ClientStatus = ClientStatus.ACTIVE,
    val notes: String = "",
    val documentPath: String? = null
)

enum class PaymentCycle {
    HOURLY, DAILY, WEEKLY, MONTHLY
}

enum class ClientStatus {
    ACTIVE, COMPLETED
}
