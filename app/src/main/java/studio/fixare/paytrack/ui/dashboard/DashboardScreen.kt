package studio.fixare.paytrack.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.fixare.paytrack.ui.AppViewModelProvider
import studio.fixare.paytrack.ui.components.IncomeExpenseBarChart
import studio.fixare.paytrack.ui.theme.ErrorRed
import studio.fixare.paytrack.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onAddClient: () -> Unit,
    onAddExpense: () -> Unit,
    onMarkPayment: () -> Unit
) {
    val uiState by viewModel.dashboardUiState.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(400) // Simulate loading for animation demo
        isLoading = false
    }

    Scaffold { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bento Grid Layout for Key Metrics
                    item {
                        BentoGridMetrics(uiState)
                    }

                    // Quick Actions
                    item {
                        QuickActionsRow(
                            onAddClient = onAddClient,
                            onAddExpense = onAddExpense,
                            onMarkPayment = onMarkPayment
                        )
                    }

                    // Chart Section
                    item {
                        ChartSection(uiState)
                    }

                    // Overdue Payments
                    if (uiState.overduePayments.isNotEmpty()) {
                        item {
                            Text(
                                text = "Overdue Payments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ErrorRed,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(uiState.overduePayments) { item ->
                            PaymentItemCard(
                                clientName = item.clientName,
                                clientCurrency = item.clientCurrency,
                                amount = item.log.amount,
                                date = item.log.date,
                                statusColor = ErrorRed,
                                currencySymbol = uiState.currencySymbol
                            )
                        }
                    }

                    // Pending Payments
                    if (uiState.pendingPayments.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pending Payments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(uiState.pendingPayments) { item ->
                            PaymentItemCard(
                                clientName = item.clientName,
                                clientCurrency = item.clientCurrency,
                                amount = item.log.amount,
                                date = item.log.date,
                                statusColor = MaterialTheme.colorScheme.primary,
                                currencySymbol = uiState.currencySymbol
                            )
                        }
                    }

                    if (uiState.pendingPayments.isEmpty() && uiState.overduePayments.isEmpty()) {
                        item {
                            EmptyStateCard(message = "No pending payments")
                        }
                    }

                    // Recent Activity
                    item {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    if (uiState.recentActivity.isEmpty()) {
                         item {
                            EmptyStateCard(message = "No recent activity")
                        }
                    } else {
                        items(uiState.recentActivity) { activity ->
                            when(activity) {
                                is ActivityItem.Payment -> {
                                    val title = if(activity.log.isManualIncome) "Manual Income" else "Payment Received"
                                    ActivityItemRow(
                                        title = title,
                                        amount = activity.log.amount,
                                        date = activity.log.date,
                                        isIncome = true,
                                        currencySymbol = uiState.currencySymbol
                                    )
                                }
                                is ActivityItem.ExpenseItem -> {
                                    ActivityItemRow(
                                        title = activity.expense.category,
                                        amount = activity.expense.amount,
                                        date = activity.expense.date,
                                        isIncome = false,
                                        currencySymbol = uiState.currencySymbol
                                    )
                                }
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun BentoGridMetrics(uiState: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Top Row: Big Total Earned Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "Total Earnings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.currencySymbol}${String.format(Locale.US, "%.2f", uiState.totalEarnedAllTime)}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }
        }

        // Middle Row: Two smaller cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // This Month
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "This Month",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${uiState.currencySymbol}${String.format(Locale.US, "%.2f", uiState.totalEarnedThisMonth)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expenses
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp, // Using trending up to signify expense flow if desired, or money off
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Column {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${uiState.currencySymbol}${String.format(Locale.US, "%.2f", uiState.totalExpenseThisMonth)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(onAddClient: () -> Unit, onAddExpense: () -> Unit, onMarkPayment: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(icon = Icons.Default.PersonAdd, label = "New Client", onClick = onAddClient)
        QuickActionButton(icon = Icons.Default.AttachMoney, label = "Add Funds", onClick = onAddExpense) // Reusing add expense as generic add funds logic for now
        QuickActionButton(icon = Icons.Default.CreditCard, label = "Expense", onClick = onAddExpense)
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun ChartSection(uiState: DashboardUiState) {
    var chartTab by remember { mutableStateOf(0) } // 0: Monthly, 1: Yearly
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TabRow(
                selectedTabIndex = chartTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = {}
            ) {
                Tab(
                    selected = chartTab == 0,
                    onClick = { chartTab = 0 },
                    text = { 
                        Text(
                            "Monthly", 
                            fontWeight = if (chartTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (chartTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    }
                )
                Tab(
                    selected = chartTab == 1,
                    onClick = { chartTab = 1 },
                    text = { 
                        Text(
                            "Yearly", 
                            fontWeight = if (chartTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (chartTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            val income = if (chartTab == 0) uiState.totalEarnedThisMonth else uiState.totalEarnedThisYear
            val expense = if (chartTab == 0) uiState.totalExpenseThisMonth else uiState.totalExpenseThisYear
            
            IncomeExpenseBarChart(
                income = income,
                expense = expense,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
    }
}

@Composable
fun PaymentItemCard(
    clientName: String,
    clientCurrency: String,
    amount: Double,
    date: Long,
    statusColor: Color,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = statusColor
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = clientName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (clientCurrency.isNotBlank()) {
                        Text(
                            text = "Currency: $clientCurrency",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Text(
                text = "$currencySymbol${String.format(Locale.US, "%.2f", amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}

@Composable
fun ActivityItemRow(title: String, amount: Double, date: Long, isIncome: Boolean, currencySymbol: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
             Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isIncome) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if(isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (isIncome) SuccessGreen else ErrorRed
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "${if (isIncome) "+" else "-"}$currencySymbol${String.format(Locale.US, "%.2f", amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) SuccessGreen else ErrorRed
        )
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Helper to access IconButtonDefaults
object IconButtonDefaults {
    @Composable
    fun filledIconButtonColors(
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ) = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )
}
