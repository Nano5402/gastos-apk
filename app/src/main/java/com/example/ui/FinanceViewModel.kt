package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Transaction
import com.example.data.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(db.transactionDao())
    }

    // List of all transactions from DB
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter states
    private val _selectedMonth = MutableStateFlow<String?>(null) // Format: "YYYY-MM" (null means current month, "TODOS" means all)
    val selectedMonth: StateFlow<String?> = _selectedMonth.asStateFlow()

    private val _selectedType = MutableStateFlow<String>("TODOS") // "TODOS", "INGRESO", "EGRESO", "PRESTAMO"
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>("TODAS") // "TODAS" or specific category
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Combined filtered transactions
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        _selectedMonth,
        _selectedType,
        _selectedCategory
    ) { txList, month, type, cat ->
        var filtered = txList

        // Filter by month
        val currentMonthStr = getCurrentMonthStr()
        val targetMonth = month ?: currentMonthStr

        if (targetMonth != "TODOS") {
            filtered = filtered.filter { tx ->
                getYearMonthStr(tx.dateMillis) == targetMonth
            }
        }

        // Filter by type
        if (type != "TODOS") {
            filtered = filtered.filter { it.type == type }
        }

        // Filter by category
        if (cat != "TODAS") {
            filtered = filtered.filter { it.category == cat }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Summary statistics for the filtered period
    val summaryState = filteredTransactions.map { txList ->
        var ingresos = 0.0
        var egresos = 0.0
        var prestamosRecibidos = 0.0
        var prestamosPagados = 0.0

        for (tx in txList) {
            when (tx.type) {
                "INGRESO" -> ingresos += tx.amount
                "EGRESO" -> egresos += tx.amount
                "PRESTAMO" -> {
                    prestamosRecibidos += tx.amount
                    prestamosPagados += tx.loanPaidAmount
                }
            }
        }

        FinanceSummary(
            totalIngresos = ingresos,
            totalEgresos = egresos,
            totalPrestamos = prestamosRecibidos,
            totalPrestamosPagados = prestamosPagados,
            balanceNeto = ingresos - egresos // Balance of actual cash flow (excluding pending debt logic or including as requested)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceSummary()
    )

    // All unique months present in transactions to populate month selector
    val availableMonths: StateFlow<List<String>> = allTransactions.map { txList ->
        val monthsSet = mutableSetOf<String>()
        val currentMonth = getCurrentMonthStr()
        monthsSet.add(currentMonth) // Always include current month

        for (tx in txList) {
            monthsSet.add(getYearMonthStr(tx.dateMillis))
        }

        monthsSet.sortedDescending().toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(getCurrentMonthStr())
    )

    fun selectMonth(month: String?) {
        _selectedMonth.value = month
    }

    fun selectType(type: String) {
        _selectedType.value = type
        // Reset category filter when switching types to avoid empty intersections
        _selectedCategory.value = "TODAS"
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // Add transaction
    fun addTransaction(
        amount: Double,
        type: String,
        category: String,
        notes: String,
        dateMillis: Long,
        source: String?,
        destination: String?
    ) {
        viewModelScope.launch {
            repository.insert(
                Transaction(
                    amount = amount,
                    type = type,
                    category = category,
                    notes = notes,
                    dateMillis = dateMillis,
                    source = source,
                    destination = destination
                )
            )
        }
    }

    // Update transaction
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    // Delete transaction
    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    // Pay off some part of a loan
    fun repayLoan(loan: Transaction, paymentAmount: Double) {
        viewModelScope.launch {
            val newPaidAmount = (loan.loanPaidAmount + paymentAmount).coerceAtMost(loan.amount)
            val isSettled = newPaidAmount >= loan.amount
            
            // 1. Update the loan transaction
            repository.update(
                loan.copy(
                    loanPaidAmount = newPaidAmount,
                    isLoanSettled = isSettled
                )
            )

            // 2. Automatically record an EGRESO for the payment so it shows up in monthly expenses!
            repository.insert(
                Transaction(
                    amount = paymentAmount,
                    type = "EGRESO",
                    category = "Pago Préstamo",
                    notes = "Pago para préstamo: '${loan.notes.ifEmpty { loan.category }}' (Acreedor: ${loan.source ?: "N/A"})",
                    dateMillis = System.currentTimeMillis(),
                    destination = loan.destination // Goes from the same account/destination
                )
            )
        }
    }

    // Helper functions for month formats
    private fun getCurrentMonthStr(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getYearMonthStr(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    // Helper to format string month like "2026-07" into "Julio 2026"
    fun formatMonthDisplay(monthStr: String): String {
        if (monthStr == "TODOS") return "Todos los meses"
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = sdfInput.parse(monthStr)
            val sdfOutput = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            date?.let { sdfOutput.format(it).replaceFirstChar { char -> char.titlecase() } } ?: monthStr
        } catch (e: Exception) {
            monthStr
        }
    }
}

data class FinanceSummary(
    val totalIngresos: Double = 0.0,
    val totalEgresos: Double = 0.0,
    val totalPrestamos: Double = 0.0,
    val totalPrestamosPagados: Double = 0.0,
    val balanceNeto: Double = 0.0
)

class FinanceViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
