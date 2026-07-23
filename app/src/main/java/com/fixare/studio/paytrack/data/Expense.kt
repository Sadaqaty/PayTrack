package com.fixare.studio.paytrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String,
    val date: Long,
    val note: String = "",
    val originalAmount: Double? = null,
    val originalCurrency: String? = null
)
