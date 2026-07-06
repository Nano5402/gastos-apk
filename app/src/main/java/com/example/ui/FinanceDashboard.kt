package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
    val transactions by viewModel.filteredTransactions.collectAsState()
    val summary by viewModel.summaryState.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showRepayDialog by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Transaction?>(null) }

    val currentMonthStr = remember(availableMonths) {
        if (availableMonths.isNotEmpty()) availableMonths.first() else "2026-07"
    }
    val activeMonth = selectedMonth ?: currentMonthStr

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Control de Gastos",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Tus finanzas personales organizadas",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                actions = {
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
                    containerColor = Color(0xCCFDF7FF) // Elegant translucent glass top bar
                )
            )
        },
        floatingActionButton = {
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
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Dashboard summary cards
            SummaryCardsSection(summary = summary)

            // Dynamic filter pills
            FilterPillsSection(
                selectedType = selectedType,
                selectedCategory = selectedCategory,
                onTypeSelected = { viewModel.selectType(it) },
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            // Header for transactions list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transacciones del Mes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${transactions.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Transactions list
            if (transactions.isEmpty()) {
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
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("transactions_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            onRepayClick = { showRepayDialog = tx },
                            onDeleteClick = { showDeleteConfirmDialog = tx }
                        )
                    }
                }
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
            }
        )
    }

    showDeleteConfirmDialog?.let { tx ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Confirmar eliminación") },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar la transacción por ${formatCurrency(tx.amount)} " +
                            "(${tx.category})?"
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
}

@Composable
fun SummaryCardsSection(summary: FinanceSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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

        Spacer(modifier = Modifier.height(14.dp))

        // Incomes, Expenses & Loans Row (Translucent card items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Incomes Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1ACAC4D0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFFC4EED0), shape = RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color(0xFF072711),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Ingresos",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatCurrency(summary.totalIngresos),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF2E7D32),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Expenses Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1ACAC4D0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFFFFD8E4), shape = RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFF31111D),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Gastos",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatCurrency(summary.totalEgresos),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Loans Card (Préstamos)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1ACAC4D0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFFD3E3FD), shape = RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF041E49),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Préstamos",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val pendingLoan = summary.totalPrestamos - summary.totalPrestamosPagados
                    Text(
                        text = formatCurrency(pendingLoan),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF1565C0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            types.forEach { (typeKey, typeValue) ->
                val isSelected = selectedType == typeKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelected(typeKey) },
                    label = { Text(typeValue) },
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
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categoría:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Box {
                    var expandedCat by remember { mutableStateOf(false) }
                    Button(
                        onClick = { expandedCat = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("category_filter_dropdown")
                    ) {
                        Text(
                            text = if (selectedCategory == "TODAS") "Todas" else selectedCategory,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                    }

                    DropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false }
                    ) {
                        categoriesForType.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (cat == "TODAS") "Todas" else cat) },
                                onClick = {
                                    onCategorySelected(cat)
                                    expandedCat = false
                                }
                            )
                        }
                    }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xB3FFFFFF) // Translucent glass background
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33CAC4D0)), // Glass border stroke
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
                                Triple(Icons.Default.Payments, Color(0xFF072711), Color(0xFFC4EED0))
                            } else {
                                Triple(Icons.Default.ArrowUpward, Color(0xFF2E7D32), Color(0xFFE8F5E9))
                            }
                        }
                        "EGRESO" -> {
                            when (transaction.category) {
                                "Comida", "Alimentos" -> Triple(Icons.Default.ShoppingBag, Color(0xFF31111D), Color(0xFFFFD8E4))
                                "Renta", "Servicios" -> Triple(Icons.Default.ReceiptLong, Color(0xFF21005D), Color(0xFFE8DEF8))
                                "Pago Préstamo" -> Triple(Icons.Default.CreditCard, Color(0xFF041E49), Color(0xFFD3E3FD))
                                else -> Triple(Icons.Default.ArrowDownward, Color(0xFFB3261E), Color(0xFFFFEBEE))
                            }
                        }
                        else -> { // PRESTAMO
                            Triple(Icons.Default.AccountBalance, Color(0xFF041E49), Color(0xFFD3E3FD))
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
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Right Side: Amount & Options
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val (amountText, amountColor) = when (transaction.type) {
                        "INGRESO" -> "+" + formatCurrency(transaction.amount) to Color(0xFF2E7D32)
                        "EGRESO" -> "-" + formatCurrency(transaction.amount) to MaterialTheme.colorScheme.error
                        else -> formatCurrency(transaction.amount) to Color(0xFFE65100)
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
                            if (remaining > 0.0) {
                                Button(
                                    onClick = onRepayClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE65100),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .height(24.dp)
                                        .testTag("repay_button")
                                ) {
                                    Text("Abonar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Saldado", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prestador: ${transaction.source ?: "Familiar"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Destinado a: ${transaction.destination ?: "Efectivo"}",
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
                            text = "Pagado: ${formatCurrency(transaction.loanPaidAmount)}",
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Restante: ${formatCurrency(transaction.amount - transaction.loanPaidAmount)}",
                            fontSize = 10.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF2E7D32),
                        trackColor = Color(0xFFFFCC80)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
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
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                        "INGRESO" to "Ingreso",
                        "PRESTAMO" to "Préstamo"
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

                Spacer(modifier = Modifier.weight(1f))

                // Save and Cancel buttons
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
                        .height(50.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar Transacción", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonar al Préstamo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Concepto: ${loan.category}")
                Text("Proveedor: ${loan.source ?: "Familiar"}")
                Text("Total del préstamo: ${formatCurrency(loan.amount)}")
                Text("Ya pagado: ${formatCurrency(loan.loanPaidAmount)}")
                Text("Restante pendiente: ${formatCurrency(remaining)}", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = payAmount,
                    onValueChange = { payAmount = it },
                    label = { Text("Monto a abonar") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repay_amount_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val payDouble = payAmount.toDoubleOrNull() ?: 0.0
                    if (payDouble > 0.0) {
                        onRepay(payDouble)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.testTag("submit_repay_button")
            ) {
                Text("Registrar Abono", fontWeight = FontWeight.Bold)
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
