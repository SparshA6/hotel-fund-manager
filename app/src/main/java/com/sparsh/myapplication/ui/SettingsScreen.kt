package com.sparsh.myapplication.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sparsh.myapplication.SettingsManager
import com.sparsh.myapplication.BackupInfo
import com.sparsh.myapplication.Booking
import com.sparsh.myapplication.BookingRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bookingRepository: BookingRepository,
    isStaffMode: Boolean,
    onRoleChanged: (Boolean) -> Unit,
    onRestored: (List<Booking>) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var taxRateStr by remember { mutableStateOf(SettingsManager.getTaxRate(context).toString()) }
    var tdsRateStr by remember { mutableStateOf(SettingsManager.getTdsRate(context).toString()) }
    var tcsRateStr by remember { mutableStateOf(SettingsManager.getTcsRate(context).toString()) }

    var backups by remember { mutableStateOf(listOf<BackupInfo>()) }
    var isLoadingBackups by remember { mutableStateOf(false) }
    var isOperating by remember { mutableStateOf(false) }
    var operationMessage by remember { mutableStateOf("") }

    var showRestoreConfirmDialog by remember { mutableStateOf<BackupInfo?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<BackupInfo?>(null) }

    LaunchedEffect(Unit) {
        isLoadingBackups = true
        try {
            backups = bookingRepository.getBackups()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to load backups: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isLoadingBackups = false
        }
    }

    val platforms = listOf("MMT", "Booking.com", "Agoda", "Goibibo", "Cleartrip")
    val commissionRates = remember {
        mutableStateMapOf<String, String>().apply {
            platforms.forEach { platform ->
                put(platform, SettingsManager.getCommissionRate(context, platform).toString())
            }
        }
    }

    var showSaveSuccessAlert by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(backgroundGradient)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Operation Progress
            if (isOperating) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = operationMessage,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Success alert
            if (showSaveSuccessAlert) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Settings saved successfully!",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // User Role Management Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStaffMode) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "User Role Management",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isStaffMode) {
                                "Current Role: STAFF (Restricted to current day's bookings only)"
                            } else {
                                "Current Role: ADMIN (Full access to all bookings and system tools)"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Button(
                            onClick = {
                                if (isStaffMode) {
                                    showPinDialog = true
                                } else {
                                    onRoleChanged(true)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStaffMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = if (isStaffMode) "Switch to Admin Mode" else "Switch to Staff Mode",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(
                                text = "Log Out Account",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (!isStaffMode) {
                // General Rates Card
                item {
                    Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tax & Government Deductions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = taxRateStr,
                            onValueChange = { taxRateStr = it },
                            label = { Text("Online Portal Tax Rate") },
                            suffix = { Text("%") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tdsRateStr,
                            onValueChange = { tdsRateStr = it },
                            label = { Text("TDS Rate") },
                            suffix = { Text("%") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tcsRateStr,
                            onValueChange = { tcsRateStr = it },
                            label = { Text("TCS Rate") },
                            suffix = { Text("%") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Platform Commission Rates Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Platform Commission Rates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        platforms.forEach { platform ->
                            OutlinedTextField(
                                value = commissionRates[platform] ?: "",
                                onValueChange = { commissionRates[platform] = it },
                                label = { Text("$platform Commission") },
                                suffix = { Text("%") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Database Backups Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Database Backups (Cloud)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoadingBackups = true
                                        try {
                                            backups = bookingRepository.getBackups()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Failed to load backups: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isLoadingBackups = false
                                        }
                                    }
                                },
                                enabled = !isLoadingBackups && !isOperating
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh backups list",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "Save a frozen copy of the database. You can restore older copies at any time. Restoring will overwrite the current live database state.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isOperating = true
                                    operationMessage = "Creating cloud backup..."
                                    try {
                                        bookingRepository.createBackup()
                                        Toast.makeText(context, "Backup created successfully!", Toast.LENGTH_SHORT).show()
                                        backups = bookingRepository.getBackups()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isOperating = false
                                    }
                                }
                            },
                            enabled = !isOperating && !isLoadingBackups,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Create Cloud Backup", fontWeight = FontWeight.Bold)
                        }

                        if (isLoadingBackups) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else if (backups.isEmpty()) {
                            Text(
                                text = "No backups available in the cloud.",
                                fontSize = 12.sp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                backups.forEach { backup ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = backup.displayDate,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "${backup.bookingCount} bookings",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }

                                        TextButton(
                                            onClick = { showRestoreConfirmDialog = backup },
                                            enabled = !isOperating,
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        val tax = taxRateStr.toFloatOrNull() ?: 5.0f
                        val tds = tdsRateStr.toFloatOrNull() ?: 0.5f
                        val tcs = tcsRateStr.toFloatOrNull() ?: 0.1f

                        SettingsManager.setTaxRate(context, tax)
                        SettingsManager.setTdsRate(context, tds)
                        SettingsManager.setTcsRate(context, tcs)

                        platforms.forEach { platform ->
                            val comm = commissionRates[platform]?.toFloatOrNull() ?: 0.0f
                            SettingsManager.setCommissionRate(context, platform, comm)
                        }

                        // Re-sync UI text fields with saved values
                        taxRateStr = SettingsManager.getTaxRate(context).toString()
                        tdsRateStr = SettingsManager.getTdsRate(context).toString()
                        tcsRateStr = SettingsManager.getTcsRate(context).toString()
                        platforms.forEach { p ->
                            commissionRates[p] = SettingsManager.getCommissionRate(context, p).toString()
                        }

                        showSaveSuccessAlert = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            }
        }
    }

    // PIN Confirmation Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPinDialog = false
                pinText = ""
                pinError = null
            },
            title = { Text("Enter Admin PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Please enter the 4-digit admin PIN to switch back to Admin Mode.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { 
                            pinText = it
                            if (it.length <= 4) pinError = null
                        },
                        label = { Text("Admin PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = pinError != null,
                        supportingText = pinError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinText == "1234") {
                            onRoleChanged(false)
                            showPinDialog = false
                            pinText = ""
                            pinError = null
                        } else {
                            pinError = "Incorrect PIN. Try again."
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        pinText = ""
                        pinError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog != null) {
        val backup = showRestoreConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = null },
            title = { Text("Restore Backup?") },
            text = { Text("Are you sure you want to restore the backup from ${backup.displayDate}? This will completely replace the current live database and local cache bookings with the ${backup.bookingCount} bookings in this backup.") },
            confirmButton = {
                Button(
                    onClick = {
                        val backupId = backup.id
                        showRestoreConfirmDialog = null
                        coroutineScope.launch {
                            isOperating = true
                            operationMessage = "Restoring database to backup state..."
                            try {
                                val restoredList = bookingRepository.restoreBackupAndSync(backupId)
                                onRestored(restoredList)
                                Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isOperating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog != null) {
        val backup = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Backup?") },
            text = { Text("Are you sure you want to delete this backup from ${backup.displayDate}? This action is permanent and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val backupId = backup.id
                        showDeleteConfirmDialog = null
                        coroutineScope.launch {
                            isOperating = true
                            operationMessage = "Deleting cloud backup..."
                            try {
                                bookingRepository.deleteBackupFromServer(backupId)
                                Toast.makeText(context, "Backup deleted successfully!", Toast.LENGTH_SHORT).show()
                                backups = bookingRepository.getBackups()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isOperating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
