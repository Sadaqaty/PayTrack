package studio.fixare.paytrack.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toPaymentCycle(value: String) = enumValueOf<PaymentCycle>(value)

    @TypeConverter
    fun fromPaymentCycle(value: PaymentCycle) = value.name

    @TypeConverter
    fun toPaymentStatus(value: String) = enumValueOf<PaymentStatus>(value)

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus) = value.name

    @TypeConverter
    fun toClientStatus(value: String) = enumValueOf<ClientStatus>(value)

    @TypeConverter
    fun fromClientStatus(value: ClientStatus) = value.name
}
