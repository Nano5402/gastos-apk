package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "INGRESO", "EGRESO", "PRESTAMO"
    val category: String, // e.g., "Comida", "Transporte", "Salud", "Sueldo", "Préstamo", "Otros"
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val source: String? = null, // De dónde viene (para préstamos o ingresos)
    val destination: String? = null, // A dónde va (para egresos o préstamos)
    val loanPaidAmount: Double = 0.0, // Cantidad pagada del préstamo
    val isLoanSettled: Boolean = false // Si el préstamo ya fue saldado
)
