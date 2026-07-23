package studio.fixare.paytrack.data

import androidx.room.Embedded
import androidx.room.Relation

data class PaymentLogWithClient(
    @Embedded val paymentLog: PaymentLog,
    @Relation(
        parentColumn = "clientId",
        entityColumn = "id"
    )
    val client: Client?
)
