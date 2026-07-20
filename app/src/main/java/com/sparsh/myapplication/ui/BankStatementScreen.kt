package com.sparsh.myapplication.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sparsh.myapplication.BookingRepository
import com.sparsh.myapplication.StatementRecord
import com.sparsh.myapplication.UploadedFileInfo
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BankStatementScreen(
    bookingRepository: BookingRepository,
    onBack: () -> Unit,
    onSelectBookingId: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var statementList by remember { mutableStateOf<List<StatementRecord>>(emptyList()) }
    var uploadedFiles by remember { mutableStateOf<List<UploadedFileInfo>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Unmatched, 1: Matched
    var isLoading by remember { mutableStateOf(false) }
    var showFilesDialog by remember { mutableStateOf(false) }
    
    // Load existing statements on startup
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            statementList = bookingRepository.getStatements()
            uploadedFiles = bookingRepository.getUploadedFiles()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load statements: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }
    
    // Filtration State
    var showFilters by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("") }
    var filterAmount by remember { mutableStateOf("") }
    var filterMonth by remember { mutableStateOf("All Months") }

    // Filter statement records based on filters
    val filteredStatementList = remember(statementList, filterText, filterAmount, filterMonth) {
        statementList.filter { record ->
            val matchesText = filterText.isBlank() || record.description.contains(filterText, ignoreCase = true)
            
            val matchesAmount = filterAmount.isBlank() || run {
                val amtQuery = filterAmount.toDoubleOrNull()
                if (amtQuery != null) {
                    Math.abs(record.amount - amtQuery) < 1.0
                } else {
                    record.amount.toString().contains(filterAmount)
                }
            }
            
            val matchesMonth = filterMonth == "All Months" || run {
                val parts = record.date.split("-")
                if (parts.size >= 2) {
                    val mNum = parts[1].toIntOrNull() ?: -1
                    val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    if (mNum in 1..12) monthNames[mNum] == filterMonth else false
                } else false
            }
            
            matchesText && matchesAmount && matchesMonth
        }
    }

    val unmatchedList = remember(filteredStatementList) { filteredStatementList.filter { !it.isMatched } }
    val matchedList = remember(filteredStatementList) { filteredStatementList.filter { it.isMatched } }

    val totalUnmatchedCount = remember(statementList) { statementList.count { !it.isMatched } }
    val totalMatchedCount = remember(statementList) { statementList.count { it.isMatched } }

    val availableMonths = remember(statementList) {
        val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val foundMonths = mutableSetOf<String>()
        statementList.forEach { record ->
            val parts = record.date.split("-")
            if (parts.size >= 2) {
                val mNum = parts[1].toIntOrNull() ?: -1
                if (mNum in 1..12) {
                    foundMonths.add(monthNames[mNum])
                }
            }
        }
        listOf("All Months") + foundMonths.toList().sorted()
    }

    // File picker launcher for uploading bank statements
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                isLoading = true
                try {
                    val contentResolver = context.contentResolver
                    val fileName = run {
                        var name = "bank_statement.xlsx"
                        val cursor = contentResolver.query(selectedUri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    name = it.getString(nameIndex)
                                }
                            }
                        }
                        name
                    }

                    val inputStream = contentResolver.openInputStream(selectedUri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        val requestFile = bytes.toRequestBody("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("file", fileName, requestFile)

                        val records = bookingRepository.uploadStatement(body)
                        statementList = records
                        uploadedFiles = bookingRepository.getUploadedFiles()
                        Toast.makeText(context, "Statement file uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bank Reconciliation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        if (statementList.isNotEmpty()) {
                            val matchProgressPercent = ((totalMatchedCount.toFloat() / statementList.size.toFloat()) * 100).toInt()
                            Text(
                                text = "${statementList.size} entries • $totalMatchedCount matched ($matchProgressPercent%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Upload GPay statement to reconcile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (statementList.isNotEmpty()) {
                        // Reconcile Button
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    try {
                                        statementList = bookingRepository.matchStatements()
                                        Toast.makeText(context, "Matching completed successfully!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Match failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Reconcile Payments", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Upload New Button
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Upload Statement", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Toggle Search/Filter
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Filter",
                                tint = if (showFilters || filterText.isNotEmpty() || filterAmount.isNotEmpty() || filterMonth != "All Months") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Uploaded Files Dialog
                        if (uploadedFiles.isNotEmpty()) {
                            IconButton(onClick = { showFilesDialog = true }) {
                                Icon(imageVector = Icons.Default.List, contentDescription = "Files", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Clear All Statements
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    try {
                                        bookingRepository.clearStatements()
                                        statementList = emptyList()
                                        uploadedFiles = emptyList()
                                        Toast.makeText(context, "Statements cleared successfully", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Clear failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Statements", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Upload Statement", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (statementList.isNotEmpty()) {
                    // Tab Selector Sticky Header
                    stickyHeader {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Unmatched", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            ) {
                                                Text("$totalUnmatchedCount")
                                            }
                                        }
                                    }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Matched", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ) {
                                                Text("$totalMatchedCount")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Optional Filter Bar Item
                    if (showFilters) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = filterText,
                                        onValueChange = { filterText = it },
                                        placeholder = { Text("Search description...", fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = filterAmount,
                                            onValueChange = { filterAmount = it },
                                            placeholder = { Text("Amount ₹", fontSize = 13.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        TextButton(
                                            onClick = {
                                                filterText = ""
                                                filterAmount = ""
                                                filterMonth = "All Months"
                                            }
                                        ) {
                                            Text("Reset", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Statements List
                    val activeList = if (selectedTab == 0) unmatchedList else matchedList
                    
                    if (activeList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = "Empty",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = if (selectedTab == 0) "All statements reconciled!" else "No reconciled statements yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(activeList, key = { it.id }) { record ->
                            StatementItem(
                                record = record,
                                onClick = if (record.isMatched && record.matchedBookingId.isNotEmpty()) {
                                    { onSelectBookingId(record.matchedBookingId) }
                                } else null
                            )
                        }
                    }
                } else {
                    // Upload Empty State
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Upload Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "No bank statement uploaded yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Select an Excel statement file to parse deposit transactions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Button(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Upload Icon")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select Bank Statement file")
                                }
                            }
                        }
                    }
                }
            }
            
            // Loading Overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // Uploaded Files Dialog
            if (showFilesDialog) {
                AlertDialog(
                    onDismissRequest = { showFilesDialog = false },
                    title = {
                        Text(
                            text = "Uploaded Files (${uploadedFiles.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uploadedFiles.forEach { file ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        bookingRepository.deleteUploadedFile(file.id)
                                                        uploadedFiles = bookingRepository.getUploadedFiles()
                                                        statementList = bookingRepository.getStatements()
                                                        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete File",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { openExcelFile(context, file.id) }
                                                .padding(horizontal = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.List,
                                                contentDescription = "File",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = formatFileDisplayName(file),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = formatUploadDate(file.uploadDate),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFilesDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StatementItem(
    record: StatementRecord,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (onClick != null) it.clickable(onClick = onClick) else it
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = record.date,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (record.isMatched) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Matched",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "+ ₹ ${record.amount}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF2E7D32) // Forest green deposit color
            )
        }
    }
}

fun formatUploadDate(isoDateStr: String): String {
    return try {
        val parts = isoDateStr.split("T")
        if (parts.size == 2) {
            val datePart = parts[0]
            val timePart = parts[1]
            val dateSplit = datePart.split("-")
            val timeSplit = timePart.split(":")
            if (dateSplit.size == 3 && timeSplit.size >= 2) {
                "${dateSplit[2]}/${dateSplit[1]} ${timeSplit[0]}:${timeSplit[1]}"
            } else {
                isoDateStr.substring(0, Math.min(16, isoDateStr.length))
            }
        } else {
            isoDateStr
        }
    } catch (e: Exception) {
        isoDateStr
    }
}

fun formatFileDisplayName(file: UploadedFileInfo): String {
    val start = file.startDate
    val end = file.endDate
    if (!start.isNullOrEmpty() && !end.isNullOrEmpty()) {
        val formattedStart = formatIsoDateToDisplay(start)
        val formattedEnd = formatIsoDateToDisplay(end)
        return if (formattedStart == formattedEnd) formattedStart else "$formattedStart – $formattedEnd"
    }
    return file.originalName
}

fun formatIsoDateToDisplay(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthIdx = (parts[1].toIntOrNull() ?: 1) - 1
            val monthName = if (monthIdx in 0..11) months[monthIdx] else parts[1]
            "${parts[2]} $monthName ${parts[0]}"
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}

fun openExcelFile(context: android.content.Context, fileId: String) {
    try {
        val baseUrl = "https://hotel-fund-manager.onrender.com/"
        val url = "${baseUrl}api/statements/files/$fileId/view"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
