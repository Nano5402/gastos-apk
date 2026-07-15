package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.data.Profile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository = TransactionRepository(
        AppDatabase.getDatabase(application).transactionDao()
    )

    private val prefs = application.getSharedPreferences("finance_prefs", android.content.Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null = system, true = dark, false = light
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()

    val allProfiles: StateFlow<List<Profile>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Load dark mode preference
        val savedDarkMode = prefs.getString("dark_mode", "system")
        _isDarkMode.value = when (savedDarkMode) {
            "dark" -> true
            "light" -> false
            else -> null
        }

        // Ensure we always have at least one profile and auto-select last profile
        viewModelScope.launch {
            repository.allProfiles.collect { profiles ->
                if (profiles.isEmpty()) {
                    repository.insertProfile(Profile(id = 1, name = "Mi Perfil", avatarEmoji = "👤", colorHex = "#6750A4"))
                } else {
                    if (_selectedProfile.value == null) {
                        val savedId = prefs.getInt("selected_profile_id", -1)
                        if (savedId != -1) {
                            val savedProfile = profiles.find { it.id == savedId }
                            if (savedProfile != null) {
                                _selectedProfile.value = savedProfile
                            }
                        }
                    } else {
                        // Keep current profile updated if it changes in database
                        val currentId = _selectedProfile.value?.id
                        val updatedProfile = profiles.find { it.id == currentId }
                        if (updatedProfile != null && updatedProfile != _selectedProfile.value) {
                            _selectedProfile.value = updatedProfile
                        }
                    }
                }
            }
        }
    }

    // List of all transactions from DB for the selected profile
    val allTransactions: StateFlow<List<Transaction>> = combine(
        _selectedProfile,
        repository.allTransactions
    ) { profile, txList ->
        if (profile == null) {
            emptyList()
        } else {
            txList.filter { it.profileId == profile.id }
        }
    }.stateIn(
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
        var prestamosOtorgados = 0.0
        var prestamosOtorgadosPagados = 0.0

        for (tx in txList) {
            when (tx.type) {
                "INGRESO" -> ingresos += tx.amount
                "EGRESO" -> egresos += tx.amount
                "PRESTAMO" -> {
                    if (tx.loanDirection == "OTORGADO") {
                        prestamosOtorgados += tx.amount
                        prestamosOtorgadosPagados += tx.loanPaidAmount
                    } else {
                        prestamosRecibidos += tx.amount
                        prestamosPagados += tx.loanPaidAmount
                    }
                }
            }
        }

        val balanceNeto = ingresos - egresos + prestamosRecibidos - prestamosOtorgados

        FinanceSummary(
            totalIngresos = ingresos,
            totalEgresos = egresos,
            totalPrestamos = prestamosRecibidos,
            totalPrestamosPagados = prestamosPagados,
            totalPrestamosOtorgados = prestamosOtorgados,
            totalPrestamosOtorgadosPagados = prestamosOtorgadosPagados,
            balanceNeto = balanceNeto
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
        destination: String?,
        loanDirection: String = "RECIBIDO"
    ) {
        viewModelScope.launch {
            val currentProfileId = _selectedProfile.value?.id ?: 1
            repository.insert(
                Transaction(
                    amount = amount,
                    type = type,
                    category = category,
                    notes = notes,
                    dateMillis = dateMillis,
                    source = source,
                    destination = destination,
                    loanDirection = loanDirection,
                    profileId = currentProfileId
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

            // 2. Automatically record an EGRESO/INGRESO for the payment so it shows up in monthly transactions!
            if (loan.loanDirection == "OTORGADO") {
                repository.insert(
                    Transaction(
                        amount = paymentAmount,
                        type = "INGRESO",
                        category = "Cobro Préstamo",
                        notes = "Cobro de préstamo: '${loan.notes.ifEmpty { loan.category }}' (Deudor: ${loan.destination ?: "N/A"})",
                        dateMillis = System.currentTimeMillis(),
                        source = loan.source,
                        profileId = loan.profileId
                    )
                )
            } else {
                repository.insert(
                    Transaction(
                        amount = paymentAmount,
                        type = "EGRESO",
                        category = "Pago Préstamo",
                        notes = "Pago para préstamo: '${loan.notes.ifEmpty { loan.category }}' (Acreedor: ${loan.source ?: "N/A"})",
                        dateMillis = System.currentTimeMillis(),
                        destination = loan.destination,
                        profileId = loan.profileId
                    )
                )
            }
        }
    }

    fun selectProfile(profile: Profile?) {
        _selectedProfile.value = profile
        prefs.edit().putInt("selected_profile_id", profile?.id ?: -1).apply()
    }

    fun addProfile(name: String, emoji: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertProfile(
                Profile(
                    name = name,
                    avatarEmoji = emoji,
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateProfile(id: Int, name: String, emoji: String, colorHex: String) {
        viewModelScope.launch {
            val updated = Profile(id = id, name = name, avatarEmoji = emoji, colorHex = colorHex)
            repository.updateProfile(updated)
            if (_selectedProfile.value?.id == id) {
                _selectedProfile.value = updated
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            repository.deleteProfileById(profile.id)
            if (_selectedProfile.value?.id == profile.id) {
                selectProfile(null)
            }
        }
    }

    fun toggleDarkMode() {
        val newValue = when (_isDarkMode.value) {
            true -> false
            false -> true
            else -> true
        }
        _isDarkMode.value = newValue
        prefs.edit().putString("dark_mode", when (newValue) {
            true -> "dark"
            false -> "light"
            else -> "system"
        }).apply()
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
    val totalPrestamos: Double = 0.0, // Me prestaron
    val totalPrestamosPagados: Double = 0.0, // He pagado de lo que me prestaron
    val totalPrestamosOtorgados: Double = 0.0, // Yo presté
    val totalPrestamosOtorgadosPagados: Double = 0.0, // Me han pagado de lo que presté
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
