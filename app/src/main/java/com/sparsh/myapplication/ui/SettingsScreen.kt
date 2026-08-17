package com.sparsh.myapplication.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.sparsh.myapplication.SettingsManager
import com.sparsh.myapplication.BackupInfo
import com.sparsh.myapplication.Booking
import com.sparsh.myapplication.PortalSettings
import com.sparsh.myapplication.EmailSettings
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
    onNavigateToBankReconciliation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var backups by remember { mutableStateOf(listOf<BackupInfo>()) }
    var isLoadingBackups by remember { mutableStateOf(false) }
    var showBackupsDialog by remember { mutableStateOf(false) }
    var isOperating by remember { mutableStateOf(false) }
    var operationMessage by remember { mutableStateOf("") }

    var showRestoreConfirmDialog by remember { mutableStateOf<BackupInfo?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<BackupInfo?>(null) }

    val platforms = listOf("MMT", "Goibibo", "Yatra", "Booking.com", "Agoda", "Cleartrip")
    var selectedPlatform by remember { mutableStateOf("MMT") }
    var portalSettingsList by remember { mutableStateOf(bookingRepository.getLocalPortalSettings()) }

    val formatBackupDate = remember {
        { timestamp: Long, fallback: String ->
            try {
                if (timestamp == 0L) fallback
                else SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(timestamp))
            } catch (e: Exception) {
                fallback
            }
        }
    }

    var emailAddressStr by remember { mutableStateOf("hotelorangeclassic@gmail.com") }
    var appPasswordStr by remember { mutableStateOf("woar uums ramq dkku") }
    var imapHostStr by remember { mutableStateOf("imap.gmail.com") }
    var imapPortStr by remember { mutableStateOf("993") }
    var autoSyncEnabled by remember { mutableStateOf(true) }
    var isSyncingEmails by remember { mutableStateOf(false) }
    var emailSyncLog by remember { mutableStateOf("") }
    var showRawEmailDialog by remember { mutableStateOf(false) }
    var rawEmailSubject by remember { mutableStateOf("") }
    var rawEmailBody by remember { mutableStateOf("") }

    var biometricAdminEnabled by remember { mutableStateOf(SettingsManager.isBiometricNetIncomeEnabled(context, "admin")) }
    var biometricStaffEnabled by remember { mutableStateOf(SettingsManager.isBiometricNetIncomeEnabled(context, "staff")) }
    var screenProtectionEnabled by remember { mutableStateOf(SettingsManager.isScreenProtectionEnabled(context)) }
    var autoLockBackgroundEnabled by remember { mutableStateOf(SettingsManager.isAutoLockOnBackgroundEnabled(context)) }
    var biometricLoginEnabled by remember { mutableStateOf(SettingsManager.isBiometricLoginEnabled(context)) }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var changePinError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoadingBackups = true
        try {
            backups = bookingRepository.getBackups()
            val remoteSettings = bookingRepository.getPortalSettings()
            portalSettingsList = remoteSettings
            val fetchedEmailSettings = bookingRepository.getEmailSettings()
            emailAddressStr = fetchedEmailSettings.email
            appPasswordStr = fetchedEmailSettings.appPassword
            imapHostStr = fetchedEmailSettings.host
            imapPortStr = fetchedEmailSettings.port.toString()
            autoSyncEnabled = fetchedEmailSettings.enabled
            emailSyncLog = fetchedEmailSettings.lastSyncLog
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to load backups or settings: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isLoadingBackups = false
        }
    }

    var commissionRateStr by remember { mutableStateOf("") }
    var propertyGstRateStr by remember { mutableStateOf("") }
    var gstOnCommissionRateStr by remember { mutableStateOf("") }
    var tdsRateStr by remember { mutableStateOf("") }
    var tcsRateStr by remember { mutableStateOf("") }
    var paymentProcessingFeeRateStr by remember { mutableStateOf("") }
    var serviceChargeStr by remember { mutableStateOf("") }

    LaunchedEffect(selectedPlatform, portalSettingsList) {
        val settings = portalSettingsList.find { it.platform.equals(selectedPlatform, ignoreCase = true) } ?: PortalSettings(selectedPlatform)
        commissionRateStr = if (settings.commissionRate == 0f) "" else settings.commissionRate.toString()
        propertyGstRateStr = if (settings.propertyGstRate == 0f) "" else settings.propertyGstRate.toString()
        gstOnCommissionRateStr = if (settings.gstOnCommissionRate == 0f) "" else settings.gstOnCommissionRate.toString()
        tdsRateStr = if (settings.tdsRate == 0f) "" else settings.tdsRate.toString()
        tcsRateStr = if (settings.tcsRate == 0f) "" else settings.tcsRate.toString()
        paymentProcessingFeeRateStr = if (settings.paymentProcessingFeeRate == 0f) "" else settings.paymentProcessingFeeRate.toString()
        serviceChargeStr = if (settings.serviceCharge == 0f) "" else settings.serviceCharge.toString()
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

            // Biometric & Security Options Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Biometric Lock",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Biometric & Net Income Privacy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Require biometric authentication (fingerprint, face, or PIN) to reveal Monthly and Yearly Net Income stats. Today's stats remain visible at all times.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        if (!isStaffMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Biometric Lock for Admin User",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (biometricAdminEnabled) "Enabled (Default: On)" else "Disabled (Default: On)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = biometricAdminEnabled,
                                    onCheckedChange = { isChecked ->
                                        biometricAdminEnabled = isChecked
                                        SettingsManager.setBiometricNetIncomeEnabled(context, "admin", isChecked)
                                        Toast.makeText(context, "Admin Biometric Lock ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Biometric Lock for Staff User",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (biometricStaffEnabled) "Enabled (Default: On)" else "Disabled (Default: On)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = biometricStaffEnabled,
                                onCheckedChange = { isChecked ->
                                    biometricStaffEnabled = isChecked
                                    SettingsManager.setBiometricNetIncomeEnabled(context, "staff", isChecked)
                                    Toast.makeText(context, "Staff Biometric Lock ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Biometric Admin Login Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Biometric Login for Admin",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Allow unlocking Admin mode with fingerprint/face ID",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = biometricLoginEnabled,
                                onCheckedChange = { isChecked ->
                                    biometricLoginEnabled = isChecked
                                    SettingsManager.setBiometricLoginEnabled(context, isChecked)
                                    Toast.makeText(context, "Biometric Admin Login ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // Background Auto-Lock Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Lock Stats on Background",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Re-lock Net Income stats whenever app is minimized",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoLockBackgroundEnabled,
                                onCheckedChange = { isChecked ->
                                    autoLockBackgroundEnabled = isChecked
                                    SettingsManager.setAutoLockOnBackgroundEnabled(context, isChecked)
                                    Toast.makeText(context, "Auto-Lock ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // Screenshot & Recents Protection Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Screenshot & Recording Protection",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Block screenshots & previews in recent apps screen (FLAG_SECURE)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = screenProtectionEnabled,
                                onCheckedChange = { isChecked ->
                                    screenProtectionEnabled = isChecked
                                    SettingsManager.setScreenProtectionEnabled(context, isChecked)
                                    (context as? com.sparsh.myapplication.MainActivity)?.updateScreenProtection()
                                    Toast.makeText(context, "Screenshot Protection ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (!isStaffMode) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            OutlinedButton(
                                onClick = {
                                    currentPinInput = ""
                                    newPinInput = ""
                                    confirmPinInput = ""
                                    changePinError = null
                                    showChangePinDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Admin PIN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (!isStaffMode) {
                // Portal Selection & Settings Card
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Portal Settings Configuration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        ScrollableTabRow(
                            selectedTabIndex = platforms.indexOf(selectedPlatform),
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            divider = {}
                        ) {
                            platforms.forEach { p ->
                                Tab(
                                    selected = selectedPlatform == p,
                                    onClick = { selectedPlatform = p },
                                    text = { Text(p, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

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
                                    text = "$selectedPlatform Settings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = commissionRateStr,
                                    onValueChange = { commissionRateStr = it },
                                    label = { Text("Commission Rate") },
                                    suffix = { Text("%") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = propertyGstRateStr,
                                    onValueChange = { propertyGstRateStr = it },
                                    label = { Text("Property GST Rate") },
                                    suffix = { Text("%") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = gstOnCommissionRateStr,
                                    onValueChange = { gstOnCommissionRateStr = it },
                                    label = { Text("GST on Commission Rate") },
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

                                OutlinedTextField(
                                    value = paymentProcessingFeeRateStr,
                                    onValueChange = { paymentProcessingFeeRateStr = it },
                                    label = { Text("Payment Processing Fee Rate") },
                                    suffix = { Text("%") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = serviceChargeStr,
                                    onValueChange = { serviceChargeStr = it },
                                    label = { Text("Service Charge") },
                                    prefix = { Text("₹") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Bank Statement Reconciliation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Bank Statement Reconciliation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Upload GPay/UPI bank statement Excel sheets to reconcile deposit records with recorded booking payments.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Button(
                            onClick = onNavigateToBankReconciliation,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reconcile"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reconcile Bank Statements", fontWeight = FontWeight.Bold)
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
                        Text(
                            text = "Database Backups (Cloud)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Save a frozen copy of the database. You can restore older copies at any time. Restoring will overwrite the current live database state.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Create Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoadingBackups = true
                                        try {
                                            backups = bookingRepository.getBackups()
                                            showBackupsDialog = true
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Failed to load backups: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isLoadingBackups = false
                                        }
                                    }
                                },
                                enabled = !isOperating && !isLoadingBackups,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Restore Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. Automated Portal Email Monitoring Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Sync",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Portal Email Automation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Monitors incoming OTA emails (MakeMyTrip, Goibibo, Booking.com, Agoda, Yatra, Cleartrip) to automatically add new upcoming bookings and remove cancellations.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Email Monitoring", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = autoSyncEnabled,
                                onCheckedChange = { autoSyncEnabled = it }
                            )
                        }

                        OutlinedTextField(
                            value = emailAddressStr,
                            onValueChange = { emailAddressStr = it },
                            label = { Text("Reservation Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = appPasswordStr,
                            onValueChange = { appPasswordStr = it },
                            label = { Text("App Password / Passcode") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = imapHostStr,
                                onValueChange = { imapHostStr = it },
                                label = { Text("IMAP Host") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = imapPortStr,
                                onValueChange = { imapPortStr = it },
                                label = { Text("Port") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        if (emailSyncLog.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "Status: $emailSyncLog",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSyncingEmails = true
                                        try {
                                            val settingsToSave = EmailSettings(
                                                email = emailAddressStr.trim(),
                                                appPassword = appPasswordStr.trim(),
                                                host = imapHostStr.trim(),
                                                port = imapPortStr.toIntOrNull() ?: 993,
                                                enabled = autoSyncEnabled
                                            )
                                            bookingRepository.saveEmailSettings(settingsToSave)
                                            val syncRes = bookingRepository.syncEmails()
                                            emailSyncLog = syncRes.message
                                            if (syncRes.addedCount > 0 || syncRes.cancelledCount > 0) {
                                                val updatedList = bookingRepository.getBookings()
                                                onRestored(updatedList)
                                            }
                                            Toast.makeText(context, syncRes.message, Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Email sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isSyncingEmails = false
                                        }
                                    }
                                },
                                enabled = !isSyncingEmails,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isSyncingEmails) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Sync Mails Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = { showRawEmailDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Test Email Text", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        val updated = PortalSettings(
                            platform = selectedPlatform,
                            commissionRate = commissionRateStr.toFloatOrNull() ?: 0f,
                            propertyGstRate = propertyGstRateStr.toFloatOrNull() ?: 0f,
                            gstOnCommissionRate = gstOnCommissionRateStr.toFloatOrNull() ?: 0f,
                            tdsRate = tdsRateStr.toFloatOrNull() ?: 0f,
                            tcsRate = tcsRateStr.toFloatOrNull() ?: 0f,
                            paymentProcessingFeeRate = paymentProcessingFeeRateStr.toFloatOrNull() ?: 0f,
                            serviceCharge = serviceChargeStr.toFloatOrNull() ?: 0f
                        )
                        coroutineScope.launch {
                            isOperating = true
                            operationMessage = "Saving portal settings..."
                            try {
                                bookingRepository.savePortalSettings(updated)
                                val list = bookingRepository.getPortalSettings()
                                portalSettingsList = list
                                showSaveSuccessAlert = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isOperating = false
                            }
                        }
                    },
                    enabled = !isOperating,
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
            text = { Text("Are you sure you want to restore the backup from ${formatBackupDate(backup.timestamp, backup.displayDate)}? This will completely replace the current live database and local cache bookings with the ${backup.bookingCount} bookings in this backup.") },
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
            text = { Text("Are you sure you want to delete this backup from ${formatBackupDate(backup.timestamp, backup.displayDate)}? This action is permanent and cannot be undone.") },
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

    if (showBackupsDialog) {
        AlertDialog(
            onDismissRequest = { showBackupsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available Backups", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                            contentDescription = "Refresh list",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (isLoadingBackups) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (backups.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No backups found on server.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(backups) { backup ->
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
                                            text = formatBackupDate(backup.timestamp, backup.displayDate),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${backup.bookingCount} bookings",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { 
                                                showRestoreConfirmDialog = backup
                                            },
                                            enabled = !isOperating,
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(
                                            onClick = { 
                                                showDeleteConfirmDialog = backup
                                            },
                                            enabled = !isOperating,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete backup",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showRawEmailDialog) {
        var rawParseResult by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showRawEmailDialog = false
                rawEmailSubject = ""
                rawEmailBody = ""
                rawParseResult = ""
            },
            title = { Text("Test Email Text Parser", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Paste an email subject & body text below to test how the OTA parser handles it:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = rawEmailSubject,
                        onValueChange = { rawEmailSubject = it },
                        label = { Text("Email Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rawEmailBody,
                        onValueChange = { rawEmailBody = it },
                        label = { Text("Email Body Text") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 6
                    )
                    if (rawParseResult.isNotEmpty()) {
                        Text(
                            text = rawParseResult,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val res = bookingRepository.parseRawEmail(rawEmailSubject, rawEmailBody)
                                rawParseResult = "Result: ${res["parsed"] ?: res["result"] ?: res}"
                            } catch (e: Exception) {
                                rawParseResult = "Error: ${e.message}"
                            }
                        }
                    }
                ) {
                    Text("Parse Text")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRawEmailDialog = false
                        rawEmailSubject = ""
                        rawEmailBody = ""
                        rawParseResult = ""
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Admin PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                currentPinInput = it
                                changePinError = null
                            }
                        },
                        label = { Text("Current Admin PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                newPinInput = it
                                changePinError = null
                            }
                        },
                        label = { Text("New 4-Digit PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                confirmPinInput = it
                                changePinError = null
                            }
                        },
                        label = { Text("Confirm New PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (changePinError != null) {
                        Text(
                            text = changePinError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedPin = SettingsManager.getAdminPin(context)
                        if (currentPinInput != expectedPin) {
                            changePinError = "Current PIN is incorrect."
                        } else if (newPinInput.length != 4) {
                            changePinError = "New PIN must be 4 digits."
                        } else if (newPinInput != confirmPinInput) {
                            changePinError = "New PINs do not match."
                        } else {
                            SettingsManager.setAdminPin(context, newPinInput)
                            Toast.makeText(context, "Admin PIN changed successfully!", Toast.LENGTH_SHORT).show()
                            showChangePinDialog = false
                        }
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
