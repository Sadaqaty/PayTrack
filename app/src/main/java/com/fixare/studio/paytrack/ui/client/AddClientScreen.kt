package com.fixare.studio.paytrack.ui.client

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fixare.studio.paytrack.data.PaymentCycle
import com.fixare.studio.paytrack.ui.AppViewModelProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClientViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var name by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var notes by remember { mutableStateOf("") }
    var paymentCycle by remember { mutableStateOf(PaymentCycle.MONTHLY) }
    var contractStartDate by remember { mutableStateOf(System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var cycleExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    
    val currencies = listOf("USD", "EUR", "GBP", "PKR", "INR", "CAD", "AUD", "JPY", "CNY")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Client") },
                navigationIcon = {
                    // Back button could be here if we had an icon, 
                    // but standard usually is handled by system back or a specific icon
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Client Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("Project Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = rate,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) rate = it },
                    label = { Text("Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Currency Dropdown
                Box(modifier = Modifier.width(100.dp)) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cur") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, "dropdown")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { currencyExpanded = true }
                    )
                    DropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        currencies.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text(cur) },
                                onClick = {
                                    currency = cur
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                // Payment Cycle Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = paymentCycle.name.lowercase().capitalize(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cycle") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, "dropdown")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Overlay clickable box to catch clicks since OutlinedTextField might consume them if enabled
                    Box(
                         modifier = Modifier
                             .matchParentSize()
                             .clickable { cycleExpanded = true }
                    )
                    
                    DropdownMenu(
                        expanded = cycleExpanded,
                        onDismissRequest = { cycleExpanded = false }
                    ) {
                        PaymentCycle.values().forEach { cycle ->
                            DropdownMenuItem(
                                text = { Text(cycle.name.lowercase().capitalize()) },
                                onClick = {
                                    paymentCycle = cycle
                                    cycleExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Date Picker
            // Use a Box to stack the OutlinedTextField and the clickable area
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(contractStartDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = contractStartDate)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { contractStartDate = it }
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    val rateValue = rate.toDoubleOrNull()
                    if (name.isNotBlank() && projectName.isNotBlank() && rateValue != null) {
                        viewModel.addClient(
                            name = name,
                            projectName = projectName,
                            contractStartDate = contractStartDate,
                            paymentCycle = paymentCycle,
                            rate = rateValue,
                            currency = currency,
                            notes = notes
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Save Client")
            }
            
            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
