package com.fixare.studio.paytrack.ui.welcome

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fixare.studio.paytrack.ui.AppViewModelProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    viewModel: WelcomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var userName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("USD") }
    var expanded by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val currencies = listOf("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "INR", "CNY", "PKR")

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to PayTrack",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Let's set up your profile for invoices.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Your Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company / Agency Name (Optional)") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCurrency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Local Currency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    currencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency) },
                            onClick = {
                                selectedCurrency = currency
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val permissionsToRequest = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    
                    if (permissionsToRequest.isNotEmpty()) {
                        permissionsLauncher.launch(permissionsToRequest.toTypedArray())
                    } else {
                        // No runtime permissions needed for storage on Android 10+ (MediaStore used)
                        permissionsGranted = true
                    }
                    
                    if (userName.isNotBlank()) {
                        coroutineScope.launch {
                            viewModel.saveProfile(userName, companyName, selectedCurrency)
                            onContinue()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = userName.isNotBlank()
            ) {
                Text("Continue")
            }
        }
    }
}
