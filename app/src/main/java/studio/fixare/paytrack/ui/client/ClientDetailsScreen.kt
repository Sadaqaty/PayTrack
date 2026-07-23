package studio.fixare.paytrack.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.fixare.paytrack.data.ClientStatus
import studio.fixare.paytrack.data.PaymentLog
import studio.fixare.paytrack.data.PaymentStatus
import studio.fixare.paytrack.ui.AppViewModelProvider
import studio.fixare.paytrack.ui.theme.ErrorRed
import studio.fixare.paytrack.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClientDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showPaymentConversionDialog by remember { mutableStateOf<ExpectedPayment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.client?.name ?: "Client Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.client?.status == ClientStatus.ACTIVE) {
                        IconButton(onClick = { showCompleteDialog = true }) {
                            Icon(Icons.Default.Archive, contentDescription = "Complete Project", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Client", tint = ErrorRed)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            uiState.client?.let { client ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Project: ${client.projectName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (client.status == ClientStatus.COMPLETED) {
                                Text(
                                    text = "COMPLETED",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                             Text(text = "Rate: ${client.currency} ${client.rate}", style = MaterialTheme.typography.bodyLarge)
                             Text(text = "Cycle: ${client.paymentCycle.name}", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (client.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Notes: ${client.notes}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.client?.status == ClientStatus.ACTIVE) {
                Text(
                    text = "Expected / Pending",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.expectedPayments.isEmpty()) {
                         item { Text("No pending payments found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(uiState.expectedPayments) { payment ->
                        ExpectedPaymentItem(
                            payment,
                            currencySymbol = uiState.client?.currency ?: "",
                            onMarkPaid = { viewModel.markAsPaid(payment) },
                            onDownloadInvoice = { viewModel.generatePendingInvoice(context, payment) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                 if (uiState.paymentHistory.isEmpty()) {
                     item { Text("No history yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(uiState.paymentHistory) { log ->
                    HistoryItem(
                        log = log,
                        onDownloadInvoice = { viewModel.generateInvoice(context, log) }
                    )
                }
            }
        }
        
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Client") },
                text = { Text("Are you sure you want to delete this client? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteClient()
                            showDeleteDialog = false
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showCompleteDialog) {
            AlertDialog(
                onDismissRequest = { showCompleteDialog = false },
                title = { Text("Complete Project") },
                text = { Text("Mark this project as completed? This will stop future payment reminders. You can still view history.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.markClientAsCompleted()
                            showCompleteDialog = false
                        }
                    ) {
                        Text("Mark Completed")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCompleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// Helper for conversion dialog removed as requested

@Composable
fun ExpectedPaymentItem(
    payment: ExpectedPayment,
    currencySymbol: String,
    onMarkPaid: () -> Unit,
    onDownloadInvoice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(payment.dueDate)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if(payment.status == PaymentStatus.OVERDUE) "Overdue" else "Pending",
                    color = if(payment.status == PaymentStatus.OVERDUE) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currencySymbol ${String.format(Locale.US, "%.2f", payment.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDownloadInvoice) {
                    Icon(Icons.Default.Download, contentDescription = "Invoice", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onMarkPaid) {
                    Icon(Icons.Default.Check, contentDescription = "Mark as Paid", tint = SuccessGreen)
                }
            }
        }
    }
}

@Composable
fun HistoryItem(log: PaymentLog, onDownloadInvoice: () -> Unit) {
     Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(log.date)),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (log.originalAmount != null && log.originalCurrency != null) {
                     Text(
                        text = "(${log.originalCurrency} ${log.originalAmount})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${String.format(Locale.US, "%.2f", log.amount)}", // Local currency value
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
                IconButton(onClick = onDownloadInvoice) {
                    Icon(Icons.Default.Download, contentDescription = "Download Invoice", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
