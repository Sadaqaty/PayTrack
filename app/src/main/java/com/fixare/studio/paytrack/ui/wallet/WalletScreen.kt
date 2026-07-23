package com.fixare.studio.paytrack.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fixare.studio.paytrack.data.Expense
import com.fixare.studio.paytrack.ui.AppViewModelProvider
import com.fixare.studio.paytrack.ui.theme.ErrorRed
import com.fixare.studio.paytrack.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

private fun getCurrencySymbol(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).symbol
    } catch (e: Exception) {
        currencyCode
    }
}

@Composable
fun WalletScreen(
    viewModel: WalletViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.walletUiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var tabIndex by remember { mutableStateOf(0) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Wallet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            BalanceCard(uiState)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.recentExpenses) { expense ->
                    ExpenseItem(expense, uiState.localCurrency)
                }
            }
        }
        
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TabRow(selectedTabIndex = tabIndex) {
                            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Expense") })
                            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Income") })
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (tabIndex == 0) {
                            AddTransactionContent(
                                title = "Add Expense",
                                onAdd = { amount, category, note ->
                                    viewModel.addExpense(amount, category, System.currentTimeMillis(), note)
                                    showAddDialog = false
                                },
                                onCancel = { showAddDialog = false }
                            )
                        } else {
                            AddTransactionContent(
                                title = "Add Side Hustle",
                                onAdd = { amount, category, note ->
                                    viewModel.addManualIncome(amount, System.currentTimeMillis(), "$category: $note")
                                    showAddDialog = false
                                },
                                onCancel = { showAddDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceCard(uiState: WalletUiState) {
    val currencySymbol = getCurrencySymbol(uiState.localCurrency)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = "$currencySymbol${String.format(Locale.US, "%.2f", uiState.balance)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BalanceItem(
                    label = "Income",
                    amount = uiState.totalIncome,
                    icon = Icons.Default.ArrowUpward,
                    color = SuccessGreen,
                    currencySymbol = currencySymbol
                )
                BalanceItem(
                    label = "Expenses",
                    amount = uiState.totalExpenses,
                    icon = Icons.Default.ArrowDownward,
                    color = ErrorRed,
                    currencySymbol = currencySymbol
                )
            }
        }
    }
}

@Composable
fun BalanceItem(label: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, currencySymbol: String = "$") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(text = "$currencySymbol${String.format(Locale.US, "%.2f", amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun ExpenseItem(expense: Expense, currencyCode: String = "USD") {
    val currencySymbol = getCurrencySymbol(currencyCode)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = expense.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(expense.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "-$currencySymbol${String.format(Locale.US, "%.2f", expense.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ErrorRed
            )
        }
    }
}

@Composable
fun AddTransactionContent(
    title: String,
    onAdd: (Double, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Column {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    amount = newValue
                }
            },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category / Source") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = onCancel, colors = ButtonDefaults.textButtonColors()) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (amountVal != null && category.isNotBlank()) {
                        onAdd(amountVal, category, note)
                    }
                }
            ) {
                Text("Add")
            }
        }
    }
}
