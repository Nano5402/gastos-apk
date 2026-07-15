package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceDashboardScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val isDarkModeState by viewModel.isDarkMode.collectAsState()
    val systemInDark = isSystemInDarkTheme()
    val useDarkTheme = isDarkModeState ?: systemInDark

    if (selectedProfile == null) {
        ProfileSelectionScreen(
            profiles = allProfiles,
            onSelectProfile = { viewModel.selectProfile(it) },
            onAddProfile = { name, emoji, color -> viewModel.addProfile(name, emoji, color) },
            onEditProfile = { id, name, emoji, color -> viewModel.updateProfile(id, name, emoji, color) },
            onDeleteProfile = { viewModel.deleteProfile(it) },
            isDarkMode = useDarkTheme,
            onToggleDarkMode = { viewModel.toggleDarkMode() },
            modifier = modifier
        )
    } else {
        val transactions by viewModel.filteredTransactions.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        val searchedTransactions = remember(transactions, searchQuery) {
            if (searchQuery.isBlank()) {
                transactions
            } else {
                transactions.filter {
                    it.notes.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
                }
            }
        }
        val summary by viewModel.summaryState.collectAsState()
        val availableMonths by viewModel.availableMonths.collectAsState()
        val selectedMonth by viewModel.selectedMonth.collectAsState()
        val selectedType by viewModel.selectedType.collectAsState()
        val selectedCategory by viewModel.selectedCategory.collectAsState()

        var currentTab by remember { mutableStateOf("DASHBOARD") } // "DASHBOARD" or "LOANS"
        var showAddDialog by remember { mutableStateOf(false) }
        var showAddLoanDialog by remember { mutableStateOf(false) }
        var showRepayDialog by remember { mutableStateOf<Transaction?>(null) }
        var showDeleteConfirmDialog by remember { mutableStateOf<Transaction?>(null) }

        var animationType by remember { mutableStateOf<String?>(null) } // "INGRESO", "EGRESO", "PRESTAMO_RECIBIDO", "PRESTAMO_OTORGADO", "ABONO_PRESTAMO"
        var animatedAmount by remember { mutableStateOf(0.0) }

        val currentMonthStr = remember(availableMonths) {
            if (availableMonths.isNotEmpty()) availableMonths.first() else "2026-07"
        }
        val activeMonth = selectedMonth ?: currentMonthStr

        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
            topBar = {
                if (currentTab == "DASHBOARD") {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Control de Gastos",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = "Hola, ${selectedProfile?.name}!",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { viewModel.selectProfile(null) },
                                modifier = Modifier.testTag("switch_profile_button_dashboard")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(selectedProfile?.avatarEmoji ?: "👤", fontSize = 18.sp)
                                }
                            }
                        },
                        actions = {
                            // Light/Dark mode toggler
                            IconButton(
                                onClick = { viewModel.toggleDarkMode() },
                                modifier = Modifier.testTag("theme_toggle_button_dashboard")
                            ) {
                                Icon(
                                    imageVector = if (useDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Cambiar Tema"
                                )
                            }

                            // Month selector dropdown
                            var expandedMonth by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.padding(end = 8.dp)) {
                                Button(
                                    onClick = { expandedMonth = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("month_selector_button")
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Mes", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = viewModel.formatMonthDisplay(activeMonth),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }

                                DropdownMenu(
                                    expanded = expandedMonth,
                                    onDismissRequest = { expandedMonth = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Ver todos los meses", fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.selectMonth("TODOS")
                                            expandedMonth = false
                                        }
                                    )
                                    HorizontalDivider()
                                    availableMonths.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(viewModel.formatMonthDisplay(m)) },
                                            onClick = {
                                                viewModel.selectMonth(m)
                                                expandedMonth = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Control de Préstamos",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = "Préstamos de: ${selectedProfile?.name}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { viewModel.selectProfile(null) },
                                modifier = Modifier.testTag("switch_profile_button_loans")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(selectedProfile?.avatarEmoji ?: "👤", fontSize = 18.sp)
                                }
                            }
                        },
                        actions = {
                            // Light/Dark mode toggler
                            IconButton(
                                onClick = { viewModel.toggleDarkMode() },
                                modifier = Modifier.testTag("theme_toggle_button_loans")
                            ) {
                                Icon(
                                    imageVector = if (useDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Cambiar Tema"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                    )
                }
            },
        floatingActionButton = {
            if (currentTab == "DASHBOARD") {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFFD0BCFF), // Matching HTML FAB bg: #D0BCFF
                    contentColor = Color(0xFF21005D), // Matching HTML FAB text: #21005D
                    shape = RoundedCornerShape(18.dp), // Modern very rounded container
                    modifier = Modifier
                        .testTag("add_transaction_fab")
                        .padding(bottom = 12.dp, end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar Transacción")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nueva", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = { showAddLoanDialog = true },
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF21005D),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .testTag("add_loan_fab")
                        .padding(bottom = 12.dp, end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo Préstamo")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nuevo Préstamo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "DASHBOARD",
                    onClick = { currentTab = "DASHBOARD" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Inicio") },
                    label = { Text("Gastos") },
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == "LOANS",
                    onClick = { currentTab = "LOANS" },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Préstamos") },
                    label = { Text("Préstamos") },
                    modifier = Modifier.testTag("tab_loans")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (currentTab == "DASHBOARD") {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("transactions_list"),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    item {
                        SummaryCardsSection(summary = summary)
                    }

                    item {
                        FilterPillsSection(
                            selectedType = selectedType,
                            selectedCategory = selectedCategory,
                            onTypeSelected = { viewModel.selectType(it) },
                            onCategorySelected = { viewModel.selectCategory(it) }
                        )
                    }

                    item {
                        val allDbTransactions by viewModel.allTransactions.collectAsState()
                        val context = LocalContext.current
                        MonthlyIncomeExpensesChart(
                            transactions = allDbTransactions,
                            onExportCsv = {
                                exportTransactionsToCsv(context, allDbTransactions)
                            }
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar por descripción o categoría...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("transaction_search_bar"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            )
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (activeMonth == "TODOS") "Todas las Transacciones" else "Transacciones de ${viewModel.formatMonthDisplay(activeMonth)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${searchedTransactions.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inbox,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No hay transacciones registradas",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Agrega una nueva transacción usando el botón flotante inferior.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else if (searchedTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Sin resultados para tu búsqueda",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Intenta buscando por otra descripción o categoría.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(searchedTransactions, key = { it.id }) { tx ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                TransactionItem(
                                    transaction = tx,
                                    onRepayClick = { showRepayDialog = tx },
                                    onDeleteClick = { showDeleteConfirmDialog = tx }
                                )
                            }
                        }
                    }
                }
            } else {
                LoansSection(
                    viewModel = viewModel,
                    onRepayClick = { showRepayDialog = it },
                    onDeleteClick = { showDeleteConfirmDialog = it }
                )
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { amount, type, category, notes, date, source, destination ->
                viewModel.addTransaction(amount, type, category, notes, date, source, destination)
                showAddDialog = false
                animationType = type
                animatedAmount = amount
            }
        )
    }

    if (showAddLoanDialog) {
        AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onSave = { amount, direction, person, notes, account ->
                viewModel.addTransaction(
                    amount = amount,
                    type = "PRESTAMO",
                    category = if (direction == "RECIBIDO") "Préstamo Recibido" else "Préstamo Otorgado",
                    notes = notes,
                    dateMillis = System.currentTimeMillis(),
                    source = if (direction == "RECIBIDO") person else account,
                    destination = if (direction == "RECIBIDO") account else person,
                    loanDirection = direction
                )
                showAddLoanDialog = false
                animationType = if (direction == "RECIBIDO") "PRESTAMO_RECIBIDO" else "PRESTAMO_OTORGADO"
                animatedAmount = amount
            }
        )
    }

    showRepayDialog?.let { loan ->
        RepayLoanDialog(
            loan = loan,
            onDismiss = { showRepayDialog = null },
            onRepay = { amount ->
                viewModel.repayLoan(loan, amount)
                showRepayDialog = null
                animationType = "ABONO_PRESTAMO"
                animatedAmount = amount
            }
        )
    }

    showDeleteConfirmDialog?.let { tx ->
        val txName = if (tx.type == "PRESTAMO") {
            val person = if (tx.loanDirection == "OTORGADO") tx.destination else tx.source
            person?.ifEmpty { null } ?: tx.notes.ifEmpty { tx.category }
        } else {
            tx.notes.ifEmpty { tx.category }
        }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Confirmar eliminación") },
            text = {
                Text(
                    "¿Estás seguro de borrar \"$txName\"?\n\nEsta acción es irreversible y se verá reflejada en tu balance."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    animationType?.let { type ->
        TransactionSuccessOverlay(
            type = type,
            amount = animatedAmount,
            onDismiss = { animationType = null }
        )
    }
}
}
}

@Composable
fun IncomeExpenseChart(totalIngresos: Double, totalEgresos: Double) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val maxVal = maxOf(totalIngresos, totalEgresos, 1.0)
    val total = totalIngresos + totalEgresos

    val incomeHeightFactor = (totalIngresos / maxVal).toFloat()
    val expenseHeightFactor = (totalEgresos / maxVal).toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Comparativa Mensual",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (totalIngresos == 0.0 && totalEgresos == 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Registra ingresos o gastos para ver la gráfica",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Income Bar Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = formatCurrency(totalIngresos),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(maxOf((100 * incomeHeightFactor).dp, 4.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isDark) {
                                            listOf(Color(0xFF2E7D32), Color(0xFF81C784))
                                        } else {
                                            listOf(Color(0xFFC4EED0), Color(0xFF2E7D32))
                                        }
                                    ),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ingresos",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Spacer/Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(100.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    // Expense Bar Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = formatCurrency(totalEgresos),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(maxOf((100 * expenseHeightFactor).dp, 4.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isDark) {
                                            listOf(Color(0xFFC62828), Color(0xFFFF8A80))
                                        } else {
                                            listOf(Color(0xFFFFD8E4), Color(0xFFC62828))
                                        }
                                    ),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gastos",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Savings insights
                val savings = totalIngresos - totalEgresos
                val savingsPct = if (totalIngresos > 0.0) (savings / totalIngresos * 100).toInt() else 0
                val insightText = if (savings >= 0) {
                    "Has ahorrado el $savingsPct% de tus ingresos este mes."
                } else {
                    "Tus gastos superan tus ingresos por ${formatCurrency(-savings)}."
                }
                val insightColor = if (savings >= 0) {
                    if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                } else {
                    if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(insightColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (savings >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = insightColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = insightText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = insightColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCardsSection(summary: FinanceSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Main Balance Card - Styled with premium Glassmorphic Frosted Gradient
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF6750A4)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6750A4),
                                Color(0xFF9278D1)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                // Background decorative glowing circle (Frosted Glass flare)
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .offset(x = 100.dp, y = (-70).dp)
                        .background(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )

                Column(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(
                        text = "BALANCE TOTAL DISPONIBLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(summary.balanceNeto),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.testTag("net_balance_value")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Glass badge
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Excluyendo deudas de préstamos",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Wallet,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterEnd)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val isDark = MaterialTheme.colorScheme.background.red < 0.5f
        val greenBorder = if (isDark) Color(0xFF81C784).copy(alpha = 0.25f) else Color(0xFF2E7D32).copy(alpha = 0.15f)
        val greenIconBg = if (isDark) Color(0xFF1B3D23) else Color(0xFFC4EED0)
        val greenIconTint = if (isDark) Color(0xFF81C784) else Color(0xFF072711)
        val greenText = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)

        val redBorder = if (isDark) Color(0xFFFF8A80).copy(alpha = 0.25f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        val redIconBg = if (isDark) Color(0xFF3B1F20) else Color(0xFFFFD8E4)
        val redIconTint = if (isDark) Color(0xFFFF8A80) else Color(0xFF31111D)
        val redText = if (isDark) Color(0xFFFF8A80) else MaterialTheme.colorScheme.error

        // Incomes & Expenses - Side-by-side row (2 columns is perfect for mobile!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Incomes Card (Ingresos)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    greenBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(greenIconBg, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = greenIconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Ingresos",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = formatCurrency(summary.totalIngresos),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = greenText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Expenses Card (Gastos)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    redBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(redIconBg, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = redIconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Gastos",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = formatCurrency(summary.totalEgresos),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = redText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Smart Loans Insight Card (Full Width)
        val pendingReceived = summary.totalPrestamos - summary.totalPrestamosPagados
        val pendingLent = summary.totalPrestamosOtorgados - summary.totalPrestamosOtorgadosPagados
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (pendingReceived > 0 || pendingLent > 0) 
                                MaterialTheme.colorScheme.secondaryContainer 
                            else 
                                Color(0xFFC4EED0),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (pendingReceived > 0 || pendingLent > 0) 
                            Icons.Default.Info 
                        else 
                            Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (pendingReceived > 0 || pendingLent > 0) 
                            MaterialTheme.colorScheme.onSecondaryContainer 
                        else 
                            Color(0xFF072711),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (pendingReceived > 0 || pendingLent > 0) {
                        Text(
                            text = "Insight de Préstamos",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = buildString {
                                if (pendingLent > 0) {
                                    append("Te deben: ${formatCurrency(pendingLent)}")
                                }
                                if (pendingLent > 0 && pendingReceived > 0) {
                                    append(" | ")
                                }
                                if (pendingReceived > 0) {
                                    append("Debes: ${formatCurrency(pendingReceived)}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                        Text(
                            text = "¡Al día con tus préstamos!",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "No tienes deudas activas ni montos pendientes por cobrar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        IncomeExpenseChart(totalIngresos = summary.totalIngresos, totalEgresos = summary.totalEgresos)
    }
}

@Composable
fun FilterPillsSection(
    selectedType: String,
    selectedCategory: String,
    onTypeSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    val types = listOf(
        "TODOS" to "Todos",
        "INGRESO" to "Ingresos",
        "EGRESO" to "Gastos",
        "PRESTAMO" to "Préstamos"
    )

    // Dynamic category list based on type selection to filter further
    val categoriesForType = remember(selectedType) {
        when (selectedType) {
            "INGRESO" -> listOf("TODAS", "Sueldo", "Negocio", "Inversiones", "Regalos", "Otros")
            "EGRESO" -> listOf("TODAS", "Comida", "Transporte", "Renta", "Servicios", "Entretenimiento", "Salud", "Educación", "Pago Préstamo", "Otros")
            "PRESTAMO" -> listOf("TODAS", "Banco", "Familiar", "Amigo", "Trabajo", "Otros")
            else -> listOf("TODAS")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Types Selector Scroll Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            types.forEach { (typeKey, typeValue) ->
                val isSelected = selectedType == typeKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelected(typeKey) },
                    label = { Text(typeValue, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_type_$typeKey")
                )
            }
        }

        // Categories Selector Scroll Row (only shown if not "TODOS")
        if (selectedType != "TODOS" && categoriesForType.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categoría:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                categoriesForType.forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    FilterChip(
                        selected = isCatSelected,
                        onClick = { onCategorySelected(cat) },
                        label = { Text(if (cat == "TODAS") "Todas" else cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("filter_category_$cat")
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onRepayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) // Crisp, solid, premium glass feel in light mode
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) // Distinct high-visibility border in light mode
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Icon + Details
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type/Category-Specific Circle Icon mapped directly to premium HTML styling spec
                    val (icon, tint, bg) = when (transaction.type) {
                        "INGRESO" -> {
                            if (transaction.category == "Sueldo" || transaction.category == "Negocio") {
                                if (isDark) {
                                    Triple(Icons.Default.Payments, Color(0xFFC4EED0), Color(0xFF0F3E1E))
                                } else {
                                    Triple(Icons.Default.Payments, Color(0xFF072711), Color(0xFFC4EED0))
                                }
                            } else {
                                if (isDark) {
                                    Triple(Icons.Default.ArrowUpward, Color(0xFF81C784), Color(0xFF1B5E20))
                                } else {
                                    Triple(Icons.Default.ArrowUpward, Color(0xFF2E7D32), Color(0xFFE8F5E9))
                                }
                            }
                        }
                        "EGRESO" -> {
                            when (transaction.category) {
                                "Comida", "Alimentos" -> {
                                    if (isDark) {
                                        Triple(Icons.Default.ShoppingBag, Color(0xFFFFD8E4), Color(0xFF492230))
                                    } else {
                                        Triple(Icons.Default.ShoppingBag, Color(0xFF31111D), Color(0xFFFFD8E4))
                                    }
                                }
                                "Renta", "Servicios" -> {
                                    if (isDark) {
                                        Triple(Icons.Default.ReceiptLong, Color(0xFFE8DEF8), Color(0xFF381E72))
                                    } else {
                                        Triple(Icons.Default.ReceiptLong, Color(0xFF21005D), Color(0xFFE8DEF8))
                                    }
                                }
                                "Pago Préstamo" -> {
                                    if (isDark) {
                                        Triple(Icons.Default.CreditCard, Color(0xFFD3E3FD), Color(0xFF0D3E8E))
                                    } else {
                                        Triple(Icons.Default.CreditCard, Color(0xFF041E49), Color(0xFFD3E3FD))
                                    }
                                }
                                else -> {
                                    if (isDark) {
                                        Triple(Icons.Default.ArrowDownward, Color(0xFFFFB4AB), Color(0xFF850016))
                                    } else {
                                        Triple(Icons.Default.ArrowDownward, Color(0xFFB3261E), Color(0xFFFFEBEE))
                                    }
                                }
                            }
                        }
                        else -> { // PRESTAMO
                            if (transaction.loanDirection == "OTORGADO") {
                                if (isDark) {
                                    Triple(Icons.Default.ArrowDownward, Color(0xFFFFB4AB), Color(0xFF850016))
                                } else {
                                    Triple(Icons.Default.ArrowDownward, Color(0xFFB3261E), Color(0xFFFFEBEE))
                                }
                            } else {
                                if (isDark) {
                                    Triple(Icons.Default.ArrowUpward, Color(0xFF81C784), Color(0xFF1B5E20))
                                } else {
                                    Triple(Icons.Default.ArrowUpward, Color(0xFF2E7D32), Color(0xFFE8F5E9))
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    }

                    // Text Details
                    Column {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (transaction.notes.isNotEmpty()) {
                            Text(
                                text = transaction.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = formatDate(transaction.dateMillis),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }

                // Right Side: Amount & Options
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val (amountText, amountColor) = when (transaction.type) {
                        "INGRESO" -> {
                            val color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                            "+" + formatCurrency(transaction.amount) to color
                        }
                        "EGRESO" -> {
                            "-" + formatCurrency(transaction.amount) to MaterialTheme.colorScheme.error
                        }
                        else -> {
                            if (transaction.loanDirection == "OTORGADO") {
                                val color = if (isDark) Color(0xFFFF8A80) else Color(0xFFB3261E)
                                "-" + formatCurrency(transaction.amount) to color
                            } else {
                                val color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                "+" + formatCurrency(transaction.amount) to color
                            }
                        }
                    }

                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = amountColor,
                        modifier = Modifier.testTag("transaction_amount_display")
                    )

                    // Icons for context actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (transaction.type == "PRESTAMO") {
                            val remaining = transaction.amount - transaction.loanPaidAmount
                            val isLent = transaction.loanDirection == "OTORGADO"
                            if (remaining > 0.0) {
                                Button(
                                    onClick = onRepayClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isLent) {
                                            if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32)
                                        } else {
                                            if (isDark) Color(0xFFFF9800) else Color(0xFFE65100)
                                        },
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .height(24.dp)
                                        .testTag("repay_button")
                                ) {
                                    Text(if (isLent) "Cobrar" else "Abonar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isDark) Color(0xFF1B5E20) else Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Saldado",
                                        color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("delete_transaction_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Loan Additional details (Source & Destination & Repayment progress)
            if (transaction.type == "PRESTAMO") {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(4.dp))
                val isLent = transaction.loanDirection == "OTORGADO"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLent) "Deudor: ${transaction.destination ?: "Amigo"}" else "Acreedor: ${transaction.source ?: "Familiar"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isLent) "Origen: ${transaction.source ?: "Efectivo"}" else "Destinado a: ${transaction.destination ?: "Efectivo"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Progress Bar of Loan Payment
                val progress = (transaction.loanPaidAmount / transaction.amount).toFloat().coerceIn(0f, 1f)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isLent) "Cobrado: ${formatCurrency(transaction.loanPaidAmount)}" else "Pagado: ${formatCurrency(transaction.loanPaidAmount)}",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                        )
                        Text(
                            text = if (isLent) "Por cobrar: ${formatCurrency(transaction.amount - transaction.loanPaidAmount)}" else "Restante: ${formatCurrency(transaction.amount - transaction.loanPaidAmount)}",
                            fontSize = 10.sp,
                            color = if (isLent) {
                                if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                            } else {
                                if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
                            }
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isLent) {
                            if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                        } else {
                            if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
                        },
                        trackColor = if (isLent) {
                            if (isDark) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
                        } else {
                            if (isDark) Color(0xFF5D4037) else Color(0xFFFFCC80)
                        }
                    )
                }
            }

            // Expense Additional details (Destination - where did it go)
            if (transaction.type == "EGRESO" && transaction.destination != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pagado desde: ${transaction.destination}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Income Additional details (Source - where did it come from)
            if (transaction.type == "INGRESO" && transaction.source != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Origen: ${transaction.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun LoansSummaryCards(totalDebo: Double, totalMeDeben: Double) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val greenBorder = if (isDark) Color(0xFF81C784).copy(alpha = 0.25f) else Color(0xFF2E7D32).copy(alpha = 0.15f)
    val greenIconBg = if (isDark) Color(0xFF1B3D23) else Color(0xFFC4EED0)
    val greenIconTint = if (isDark) Color(0xFF81C784) else Color(0xFF072711)
    val greenText = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)

    val redBorder = if (isDark) Color(0xFFFF8A80).copy(alpha = 0.25f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    val redIconBg = if (isDark) Color(0xFF3B1F20) else Color(0xFFFFD8E4)
    val redIconTint = if (isDark) Color(0xFFFF8A80) else Color(0xFF31111D)
    val redText = if (isDark) Color(0xFFFF8A80) else MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Debo Card (Matches Expenses / Red card design)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, redBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(redIconBg, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = redIconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Le debo a otros",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = formatCurrency(totalDebo),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = redText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Me deben Card (Matches Incomes / Green card design)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, greenBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(greenIconBg, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = greenIconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Me deben a mí",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = formatCurrency(totalMeDeben),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = greenText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansSection(
    viewModel: FinanceViewModel,
    onRepayClick: (Transaction) -> Unit,
    onDeleteClick: (Transaction) -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val loanTxs = remember(allTransactions) {
        allTransactions.filter { it.type == "PRESTAMO" }
    }

    var loanFilter by remember { mutableStateOf("PENDIENTES") }

    val filteredLoans = remember(loanTxs, loanFilter) {
        when (loanFilter) {
            "PENDIENTES" -> loanTxs.filter { !it.isLoanSettled }
            "ME_PRESTARON" -> loanTxs.filter { !it.isLoanSettled && it.loanDirection == "RECIBIDO" }
            "PRESTE" -> loanTxs.filter { !it.isLoanSettled && it.loanDirection == "OTORGADO" }
            "SALDADOS" -> loanTxs.filter { it.isLoanSettled }
            else -> loanTxs // "TODOS"
        }
    }

    val totalDebo = remember(loanTxs) {
        loanTxs.filter { it.loanDirection == "RECIBIDO" && !it.isLoanSettled }
            .sumOf { it.amount - it.loanPaidAmount }
    }
    val totalMeDeben = remember(loanTxs) {
        loanTxs.filter { it.loanDirection == "OTORGADO" && !it.isLoanSettled }
            .sumOf { it.amount - it.loanPaidAmount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Loan-specific Header Cards
        LoansSummaryCards(totalDebo = totalDebo, totalMeDeben = totalMeDeben)

        // Filter Pills Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                "PENDIENTES" to "Pendientes",
                "ME_PRESTARON" to "Me prestaron",
                "PRESTE" to "Yo presté",
                "SALDADOS" to "Saldados",
                "TODOS" to "Ver todos"
            )

            filters.forEach { (fKey, fLabel) ->
                val isSelected = loanFilter == fKey
                FilterChip(
                    selected = isSelected,
                    onClick = { loanFilter = fKey },
                    label = { Text(fLabel, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Subtitle / List Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lista de Préstamos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${filteredLoans.size} préstamos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Loans list
        if (filteredLoans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay préstamos en esta lista",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("loans_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLoans, key = { it.id }) { loan ->
                    TransactionItem(
                        transaction = loan,
                        onRepayClick = { onRepayClick(loan) },
                        onDeleteClick = { onDeleteClick(loan) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String, Long, String?, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EGRESO") } // "EGRESO", "INGRESO", "PRESTAMO"
    var selectedCategory by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") } // From (Préstamo)
    var destination by remember { mutableStateOf("") } // To (Expense/Préstamo)

    val incomeCategories = listOf("Sueldo", "Negocio", "Inversiones", "Regalos", "Otros")
    val expenseCategories = listOf("Comida", "Transporte", "Renta", "Servicios", "Entretenimiento", "Salud", "Educación", "Otros")
    val loanCategories = listOf("Préstamo Personal", "Préstamo Bancario", "Préstamo Familiar", "Préstamo Amigo", "Otros")

    val destinations = listOf("Efectivo", "Tarjeta de Débito", "Tarjeta de Crédito", "Cuenta de Ahorros", "Mercado Pago", "Otros")
    val sources = listOf("Banco", "Cooperativa", "Amigo", "Familiar", "Trabajo", "Otros")

    // Default category based on selection
    LaunchedEffect(type) {
        selectedCategory = when (type) {
            "INGRESO" -> incomeCategories.first()
            "EGRESO" -> expenseCategories.first()
            else -> loanCategories.first()
        }
    }

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                TopAppBar(
                    title = { Text("Nueva Transacción", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Button(
                        onClick = {
                            val amtDouble = amount.toDoubleOrNull() ?: 0.0
                            if (amtDouble > 0.0 && selectedCategory.isNotEmpty()) {
                                onSave(
                                    amtDouble,
                                    type,
                                    selectedCategory,
                                    notes,
                                    System.currentTimeMillis(),
                                    if (type == "PRESTAMO" || type == "INGRESO") source.ifEmpty { "Familiar" } else null,
                                    if (type == "EGRESO" || type == "PRESTAMO") destination.ifEmpty { "Efectivo" } else null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp)
                            .testTag("save_transaction_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar Transacción", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Segmented Button / Custom Selector for Type
                Text("Tipo de movimiento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        "EGRESO" to "Gasto",
                        "INGRESO" to "Ingreso"
                    )
                    types.forEach { (tKey, tLabel) ->
                        val isSelected = type == tKey
                        val containerColor = if (isSelected) {
                            when (tKey) {
                                "INGRESO" -> Color(0xFF2E7D32)
                                "EGRESO" -> MaterialTheme.colorScheme.error
                                else -> Color(0xFFE65100)
                            }
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                        val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Button(
                            onClick = { type = tKey },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_select_$tKey")
                        ) {
                            Text(tLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Category selection dropdown
                var expandedCategory by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría / Concepto") },
                        trailingIcon = {
                            IconButton(onClick = { expandedCategory = true }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCategory = true }
                            .testTag("category_input_field")
                    )
                    DropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        val categories = when (type) {
                            "INGRESO" -> incomeCategories
                            "EGRESO" -> expenseCategories
                            else -> loanCategories
                        }
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                // CONDITIONAL FIELD: De dónde viene el préstamo (Source)
                if (type == "PRESTAMO") {
                    var expandedSource by remember { mutableStateOf(false) }
                    Text("Origen del préstamo (¿De quién viene?)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = source,
                            onValueChange = { source = it },
                            label = { Text("Proveedor del préstamo") },
                            trailingIcon = {
                                IconButton(onClick = { expandedSource = true }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                            },
                            placeholder = { Text("Ej: Banco, Amigo, Familiar") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_source_input")
                        )
                        DropdownMenu(
                            expanded = expandedSource,
                            onDismissRequest = { expandedSource = false }
                        ) {
                            sources.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = {
                                        source = s
                                        expandedSource = false
                                    }
                                )
                            }
                        }
                    }
                }

                // CONDITIONAL FIELD: A dónde va el gasto / préstamo (Destination)
                if (type == "EGRESO" || type == "PRESTAMO") {
                    var expandedDest by remember { mutableStateOf(false) }
                    val labelText = if (type == "EGRESO") "¿A dónde va este gasto? (Método de pago)" else "¿A dónde se depositará este dinero?"
                    Text(labelText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            label = { Text("Medio de destino") },
                            trailingIcon = {
                                IconButton(onClick = { expandedDest = true }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                            },
                            placeholder = { Text("Ej: Efectivo, Tarjeta, Banco") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_destination_input")
                        )
                        DropdownMenu(
                            expanded = expandedDest,
                            onDismissRequest = { expandedDest = false }
                        ) {
                            destinations.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d) },
                                    onClick = {
                                        destination = d
                                        expandedDest = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Description Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Detalles (Opcional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_notes_input"),
                    placeholder = { Text("Ej: Compra mensual, Préstamo sin intereses") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanDialog(
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String, String) -> Unit // amount, direction ("RECIBIDO"/"OTORGADO"), person, notes, account
) {
    var amount by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("RECIBIDO") } // "RECIBIDO" (me prestaron) or "OTORGADO" (presté)
    var person by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("Efectivo") }

    val accounts = listOf("Efectivo", "Cuenta de Ahorros", "Mercado Pago", "Tarjeta de Débito", "Otros")

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                TopAppBar(
                    title = { Text("Nuevo Préstamo", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Button(
                        onClick = {
                            val amtDouble = amount.toDoubleOrNull() ?: 0.0
                            if (amtDouble > 0.0 && person.isNotBlank()) {
                                onSave(amtDouble, direction, person, notes, account)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp)
                            .testTag("save_loan_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar Préstamo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type selector
                Text("Tipo de Préstamo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { direction = "RECIBIDO" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (direction == "RECIBIDO") Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (direction == "RECIBIDO") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("loan_direction_received")
                    ) {
                        Text("Me prestaron", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = { direction = "OTORGADO" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (direction == "OTORGADO") Color(0xFFB3261E) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (direction == "OTORGADO") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("loan_direction_given")
                    ) {
                        Text("Yo presté", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_amount_input"),
                    singleLine = true
                )

                // Person Field (Creditor / Debtor)
                val personLabel = if (direction == "RECIBIDO") "¿Quién te prestó dinero?" else "¿A quién le prestaste dinero?"
                val personPlaceholder = if (direction == "RECIBIDO") "Ej: Mamá, Banco, Amigo" else "Ej: Juan, Compañero de trabajo"
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text(personLabel) },
                    placeholder = { Text(personPlaceholder) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_person_input"),
                    singleLine = true
                )

                // Account / Medium Selection
                val accountLabel = if (direction == "RECIBIDO") "¿Dónde se depositará el dinero?" else "¿De dónde sale el dinero?"
                Text(accountLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                var expandedAccount by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = account,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Medio de Dinero") },
                        trailingIcon = {
                            IconButton(onClick = { expandedAccount = true }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedAccount = true }
                            .testTag("loan_account_input")
                    )
                    DropdownMenu(
                        expanded = expandedAccount,
                        onDismissRequest = { expandedAccount = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc) },
                                onClick = {
                                    account = acc
                                    expandedAccount = false
                                }
                            )
                        }
                    }
                }

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Detalles (Opcional)") },
                    placeholder = { Text("Ej: Compra de boletos, Sin intereses") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_notes_input")
                )
            }
        }
    }
}

@Composable
fun RepayLoanDialog(
    loan: Transaction,
    onDismiss: () -> Unit,
    onRepay: (Double) -> Unit
) {
    var payAmount by remember { mutableStateOf("") }
    val remaining = loan.amount - loan.loanPaidAmount
    val isLent = loan.loanDirection == "OTORGADO"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (isLent) "Registrar Cobro de Préstamo" else "Abonar al Préstamo", 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Concepto: ${loan.category}")
                if (isLent) {
                    Text("Deudor: ${loan.destination ?: "Amigo"}")
                } else {
                    Text("Acreedor / Prestador: ${loan.source ?: "Familiar"}")
                }
                Text("Total del préstamo: ${formatCurrency(loan.amount)}")
                Text(
                    text = if (isLent) "Ya cobrado: ${formatCurrency(loan.loanPaidAmount)}" 
                           else "Ya pagado: ${formatCurrency(loan.loanPaidAmount)}"
                )
                val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                Text(
                    text = if (isLent) "Restante por cobrar: ${formatCurrency(remaining)}" 
                           else "Restante pendiente: ${formatCurrency(remaining)}", 
                    fontWeight = FontWeight.Bold, 
                    color = if (isLent) {
                        if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                    } else {
                        if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = payAmount,
                    onValueChange = { payAmount = it },
                    label = { Text(if (isLent) "Monto a cobrar" else "Monto a abonar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repay_amount_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val isDark = MaterialTheme.colorScheme.background.red < 0.5f
            Button(
                onClick = {
                    val payDouble = payAmount.toDoubleOrNull() ?: 0.0
                    if (payDouble > 0.0) {
                        onRepay(payDouble)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLent) {
                        if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32)
                    } else {
                        if (isDark) Color(0xFFFF9800) else Color(0xFFE65100)
                    }
                ),
                modifier = Modifier.testTag("submit_repay_button")
            ) {
                Text(if (isLent) "Registrar Cobro" else "Registrar Abono", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Helpers
fun formatCurrency(amount: Double): String {
    return try {
        val format = java.text.NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        format.format(amount)
    } catch (e: Exception) {
        String.format(Locale.getDefault(), "$%.2f", amount)
    }
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "MX"))
    return sdf.format(Date(millis))
}

@Composable
fun ProfileSelectionScreen(
    profiles: List<com.example.data.Profile>,
    onSelectProfile: (com.example.data.Profile) -> Unit,
    onAddProfile: (String, String, String) -> Unit,
    onEditProfile: (Int, String, String, String) -> Unit,
    onDeleteProfile: (com.example.data.Profile) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<com.example.data.Profile?>(null) }
    var profileToDelete by remember { mutableStateOf<com.example.data.Profile?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkMode) {
                        listOf(
                            Color(0xFF1C1B1F),
                            Color(0xFF121212)
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.surface
                        )
                    }
                )
            )
            .drawBehind {
                // Ambient soft glows
                val primaryColor = if (isDarkMode) Color(0x189278D1) else Color(0x1F6750A4)
                val secondaryColor = if (isDarkMode) Color(0x1000B0FF) else Color(0x1503A9F4)
                
                drawCircle(
                    color = primaryColor,
                    radius = size.width * 0.5f,
                    center = androidx.compose.ui.geometry.Offset(0f, size.height * 0.15f)
                )
                drawCircle(
                    color = secondaryColor,
                    radius = size.width * 0.6f,
                    center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.85f)
                )
            }
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header with Dark Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleDarkMode) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Cambiar Tema"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logo and Title inside a premium container
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("💰", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Control de Gastos",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Elige tu espacio para comenzar",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Profiles Grid/Flow
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val chunkedProfiles = profiles.chunked(2)
                chunkedProfiles.forEach { rowProfiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                    ) {
                        rowProfiles.forEach { profile ->
                            ProfileCard(
                                profile = profile,
                                onSelect = { onSelectProfile(profile) },
                                onEdit = { profileToEdit = profile },
                                onDelete = { profileToDelete = profile },
                                isDeletable = profiles.size > 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add Profile Card
                if (profiles.size < 6) {
                    Card(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .width(150.dp)
                            .height(145.dp)
                            .testTag("add_profile_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Crear Perfil",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nuevo Perfil",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CreateProfileDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, emoji, color ->
                onAddProfile(name, emoji, color)
                showAddDialog = false
            }
        )
    }

    profileToEdit?.let { profile ->
        CreateProfileDialog(
            profileToEdit = profile,
            onDismiss = { profileToEdit = null },
            onSave = { name, emoji, color ->
                onEditProfile(profile.id, name, emoji, color)
                profileToEdit = null
            }
        )
    }

    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Eliminar Perfil") },
            text = { Text("¿Estás seguro de que deseas eliminar el perfil de '${profile.name}'? Esto borrará también todas sus transacciones y préstamos registrados.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProfile(profile)
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ProfileCard(
    profile: com.example.data.Profile,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDeletable: Boolean
) {
    val profileColor = remember(profile.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(profile.colorHex))
        } catch (e: Exception) {
            Color(0xFF6750A4)
        }
    }
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(145.dp)
    ) {
        Card(
            onClick = onSelect,
            modifier = Modifier
                .fillMaxSize()
                .testTag("profile_card_${profile.name}"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, profileColor.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Beautiful Avatar Circle with profileColor ring
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(2.dp, profileColor, CircleShape)
                        .padding(4.dp)
                        .background(
                            color = profileColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profile.avatarEmoji, fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Edit button at TopStart
        IconButton(
            onClick = onEdit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    shape = CircleShape
                )
                .testTag("edit_profile_button_${profile.name}")
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(12.dp)
            )
        }

        if (isDeletable) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileDialog(
    profileToEdit: com.example.data.Profile? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(profileToEdit?.name ?: "") }
    var selectedEmoji by remember { mutableStateOf(profileToEdit?.avatarEmoji ?: "👤") }
    var selectedColor by remember { mutableStateOf(profileToEdit?.colorHex ?: "#6750A4") }

    val emojis = listOf("👤", "💑", "💼", "🏠", "💖", "🐕", "🚗", "🌴", "🍕", "🎓", "🎮", "💰")
    val colors = listOf(
        "#6750A4" to "Morado",
        "#0288D1" to "Azul",
        "#00796B" to "Verde Azulado",
        "#388E3C" to "Verde",
        "#F57C00" to "Naranja",
        "#C2185B" to "Rosa"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (profileToEdit != null) "Editar Perfil" else "Crear Nuevo Perfil", 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Perfil") },
                    placeholder = { Text("Ej: Mi Pareja, Papá, etc.") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                Column {
                    Text(
                        text = "Selecciona un Avatar",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        emojis.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (selectedEmoji == emoji) 2.dp else 1.dp,
                                        color = if (selectedEmoji == emoji) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Selecciona un Color",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { (colorHex, _) ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = color,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (selectedColor == colorHex) 3.dp else 0.dp,
                                        color = if (selectedColor == colorHex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorHex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, selectedEmoji, selectedColor)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TransactionSuccessOverlay(
    type: String,
    amount: Double,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    
    // Auto-dismiss after 2.5 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }

    // Determine visual style based on transaction type
    val title = when (type) {
        "INGRESO" -> "¡Ingreso Registrado!"
        "EGRESO" -> "¡Gasto Registrado!"
        "PRESTAMO_RECIBIDO" -> "¡Préstamo Recibido!"
        "PRESTAMO_OTORGADO" -> "¡Préstamo Otorgado!"
        "ABONO_PRESTAMO" -> "¡Abono Registrado!"
        else -> "¡Operación Exitosa!"
    }

    val amountPrefix = when (type) {
        "INGRESO", "PRESTAMO_RECIBIDO" -> "+"
        "EGRESO", "PRESTAMO_OTORGADO" -> "-"
        "ABONO_PRESTAMO" -> "+"
        else -> ""
    }

    val themeColor = when (type) {
        "INGRESO", "PRESTAMO_RECIBIDO" -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
        "EGRESO", "PRESTAMO_OTORGADO" -> if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
        "ABONO_PRESTAMO" -> if (isDark) Color(0xFF4FC3F7) else Color(0xFF0288D1)
        else -> MaterialTheme.colorScheme.primary
    }

    val icon = when (type) {
        "INGRESO" -> Icons.Default.TrendingUp
        "EGRESO" -> Icons.Default.TrendingDown
        "PRESTAMO_RECIBIDO" -> Icons.Default.Download
        "PRESTAMO_OTORGADO" -> Icons.Default.Upload
        "ABONO_PRESTAMO" -> Icons.Default.CheckCircle
        else -> Icons.Default.Check
    }

    // Animation States
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnim = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    val cardAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "card_alpha"
    )

    val bgAlpha by animateFloatAsState(
        targetValue = if (startAnim) 0.85f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "bg_alpha"
    )

    // Particle state for floating shapes
    val particleCount = 15
    val particles = remember {
        List(particleCount) {
            val randomX = kotlin.random.Random.nextInt(5, 96) / 100f // percentage across screen width
            val randomDelay = kotlin.random.Random.nextInt(0, 601)
            val speed = kotlin.random.Random.nextInt(1400, 2401)
            val startY = if (type == "EGRESO" || type == "PRESTAMO_OTORGADO") -0.05f else 1.05f
            val endY = if (type == "EGRESO" || type == "PRESTAMO_OTORGADO") 1.05f else -0.05f
            val size = kotlin.random.Random.nextInt(12, 33).dp
            val alpha = kotlin.random.Random.nextFloat() * (0.85f - 0.35f) + 0.35f
            ParticleData(randomX, randomDelay, speed, startY, endY, size, alpha)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha))
            .clickable(enabled = false) {}, // Intercept clicks
        contentAlignment = Alignment.Center
    ) {
        val width = maxWidth
        val height = maxHeight

        // Floating particles
        particles.forEach { p ->
            var pStart by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(p.delay.toLong())
                pStart = true
            }

            val pProgress by animateFloatAsState(
                targetValue = if (pStart) 1f else 0f,
                animationSpec = tween(durationMillis = p.speed, easing = LinearOutSlowInEasing),
                label = "particle_progress"
            )

            val pAlpha by animateFloatAsState(
                targetValue = if (pStart) (1f - pProgress) * p.alpha else 0f,
                animationSpec = tween(durationMillis = p.speed),
                label = "particle_alpha"
            )

            if (pStart && pProgress < 1f) {
                val currentY = p.startY + (p.endY - p.startY) * pProgress
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = width * p.pctX,
                            y = height * currentY
                        )
                        .size(p.size)
                        .background(
                            color = themeColor.copy(alpha = pAlpha),
                            shape = CircleShape
                        )
                )
            }
        }

        // Animated Card
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 400.dp)
                .graphicsLayer(
                    scaleX = cardScale,
                    scaleY = cardScale,
                    alpha = cardAlpha
                ),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFFFFFF)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Success Badge with rotating/pulsing icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(themeColor.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$amountPrefix${formatCurrency(amount)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = themeColor
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Small decorative success indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Actualizado Correctamente",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class ParticleData(
    val pctX: Float,
    val delay: Int,
    val speed: Int,
    val startY: Float,
    val endY: Float,
    val size: androidx.compose.ui.unit.Dp,
    val alpha: Float
)

data class MonthlyStats(
    val monthStr: String,
    val monthLabel: String,
    val income: Double,
    val expenses: Double
)

@Composable
fun MonthlyIncomeExpensesChart(
    transactions: List<Transaction>,
    onExportCsv: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    
    // Group and aggregate transactions for the last 5 months
    val statsList = remember(transactions) {
        val statsMap = mutableMapOf<String, Pair<Double, Double>>() // monthStr -> (income, expenses)
        val sdfMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        
        // 1. Pre-populate with the last 5 months chronologically so the chart is never empty
        val cal = java.util.Calendar.getInstance()
        val currentMonths = mutableListOf<String>()
        for (i in 0 until 5) {
            val mStr = sdfMonth.format(cal.time)
            currentMonths.add(mStr)
            statsMap[mStr] = Pair(0.0, 0.0)
            cal.add(java.util.Calendar.MONTH, -1)
        }
        
        // 2. Sum the real transactions
        for (tx in transactions) {
            val mStr = sdfMonth.format(java.util.Date(tx.dateMillis))
            val current = statsMap[mStr] ?: Pair(0.0, 0.0)
            var addInc = 0.0
            var addExp = 0.0
            if (tx.type == "INGRESO") {
                addInc = tx.amount
            } else if (tx.type == "EGRESO") {
                addExp = tx.amount
            }
            statsMap[mStr] = Pair(current.first + addInc, current.second + addExp)
        }
        
        // 3. Keep only the last 5 months we populated (sorted chronologically)
        currentMonths.sorted().map { monthStr ->
            val pair = statsMap[monthStr] ?: Pair(0.0, 0.0)
            val label = try {
                val date = sdfMonth.parse(monthStr)
                val sdfLabel = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                date?.let { sdfLabel.format(it).replaceFirstChar { c -> c.titlecase() } } ?: monthStr
            } catch (e: Exception) {
                monthStr
            }
            MonthlyStats(
                monthStr = monthStr,
                monthLabel = label.replace(".", ""), // strip trailing dot for abbreviated months if any
                income = pair.first,
                expenses = pair.second
            )
        }
    }
    
    // Max value to scale heights proportionally
    val maxVal = remember(statsList) {
        val highest = statsList.maxOfOrNull { maxOf(it.income, it.expenses) } ?: 0.0
        if (highest == 0.0) 1.0 else highest
    }
    
    // Interaction State: which bar is currently tapped
    var selectedMonthStats by remember { mutableStateOf<MonthlyStats?>(null) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("monthly_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Title and CSV Export Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Balance Histórico",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Últimos 5 meses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Export Button
                Button(
                    onClick = onExportCsv,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Exportar CSV",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Exportar CSV",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ingresos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Egresos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bar Chart Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                statsList.forEach { stats ->
                    // Proportional heights (0f to 1f)
                    val incRatio = (stats.income / maxVal).toFloat().coerceIn(0.01f, 1f)
                    val expRatio = (stats.expenses / maxVal).toFloat().coerceIn(0.01f, 1f)
                    
                    // Animations for bars
                    var triggerAnimation by remember { mutableStateOf(false) }
                    LaunchedEffect(stats) {
                        triggerAnimation = true
                    }
                    
                    val animatedIncRatio by animateFloatAsState(
                        targetValue = if (triggerAnimation) incRatio else 0f,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "income_bar_height"
                    )
                    val animatedExpRatio by animateFloatAsState(
                        targetValue = if (triggerAnimation) expRatio else 0f,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "expense_bar_height"
                    )
                    
                    // Column containing both bars and label for this month
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedMonthStats = if (selectedMonthStats?.monthStr == stats.monthStr) null else stats
                            }
                    ) {
                        // Vertical Bars container
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Income Bar
                            val isSelectedInc = selectedMonthStats?.monthStr == stats.monthStr
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(animatedIncRatio)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        color = if (isDark) {
                                            if (isSelectedInc) Color(0xFFA5D6A7) else Color(0xFF81C784)
                                        } else {
                                            if (isSelectedInc) Color(0xFF1B5E20) else Color(0xFF2E7D32)
                                        }
                                    )
                                    .border(
                                        width = if (isSelectedInc) 1.5.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            
                            // Expense Bar
                            val isSelectedExp = selectedMonthStats?.monthStr == stats.monthStr
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(animatedExpRatio)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        color = if (isDark) {
                                            if (isSelectedExp) Color(0xFFFFAB91) else Color(0xFFFF8A80)
                                        } else {
                                            if (isSelectedExp) Color(0xFFB71C1C) else Color(0xFFC62828)
                                        }
                                    )
                                    .border(
                                        width = if (isSelectedExp) 1.5.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Month Label
                        Text(
                            text = stats.monthLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedMonthStats?.monthStr == stats.monthStr) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedMonthStats?.monthStr == stats.monthStr) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            
            // Detail / Tooltip panel when a bar is tapped
            AnimatedVisibility(
                visible = selectedMonthStats != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedMonthStats?.let { stats ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Detalle de ${stats.monthLabel}:",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ingresos:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(stats.income),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Egresos:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(stats.expenses),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val netBalance = stats.income - stats.expenses
                                Text(
                                    text = "Balance Neto:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatCurrency(netBalance),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (netBalance >= 0) {
                                        if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                    } else {
                                        if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun exportTransactionsToCsv(context: android.content.Context, transactions: List<Transaction>) {
    try {
        val cacheDir = context.cacheDir
        val file = java.io.File(cacheDir, "historial_transacciones.csv")
        val writer = java.io.BufferedWriter(java.io.FileWriter(file))
        
        // Write standard headers
        writer.write("ID,Fecha,Tipo,Categoria,Monto,Notas,Origen,Destino,Direccion_Prestamo,Monto_Pagado_Prestamo,Prestamo_Liquidado\n")
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        for (tx in transactions) {
            val dateStr = sdf.format(java.util.Date(tx.dateMillis))
            val notesEscaped = tx.notes.replace("\"", "\"\"")
            val sourceEscaped = (tx.source ?: "").replace("\"", "\"\"")
            val destEscaped = (tx.destination ?: "").replace("\"", "\"\"")
            writer.write(
                "${tx.id}," +
                "\"$dateStr\"," +
                "\"${tx.type}\"," +
                "\"${tx.category}\"," +
                "${tx.amount}," +
                "\"$notesEscaped\"," +
                "\"$sourceEscaped\"," +
                "\"$destEscaped\"," +
                "\"${tx.loanDirection}\"," +
                "${tx.loanPaidAmount}," +
                "${tx.isLoanSettled}\n"
            )
        }
        writer.close()
        
        val authority = "com.example.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Historial de Transacciones")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Compartir Reporte de Transacciones"))
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
