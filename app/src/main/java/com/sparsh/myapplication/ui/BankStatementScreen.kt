package com.sparsh.myapplication.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
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
                amtQuery == null || Math.abs(record.amount - amtQuery) < 1.0 || record.amount.toString().contains(filterAmount)
            }
            
            val matchesMonth = filterMonth == "All Months" || run {
                val monthIndex = when (filterMonth) {
                    "Jan" -> "-01-"
                    "Feb" -> "-02-"
                    "Mar" -> "-03-"
                    "Apr" -> "-04-"
                    "May" -> "-05-"
                    "Jun" -> "-06-"
                    "Jul" -> "-07-"
                    "Aug" -> "-08-"
                    "Sep" -> "-09-"
                    "Oct" -> "-10-"
                    "Nov" -> "-11-"
                    "Dec" -> "-12-"
                    else -> ""
                }
                if (monthIndex.isEmpty()) true else record.date.contains(monthIndex)
            }
            
            matchesText && matchesAmount && matchesMonth
        }
    }

    // Unfiltered counts for tabs
    val totalUnmatchedCount = remember(statementList) { statementList.count { !it.isMatched } }
    val totalMatchedCount = remember(statementList) { statementList.count { it.isMatched } }

    val unmatchedList = remember(filteredStatementList) { filteredStatementList.filter { !it.isMatched } }
    val matchedList = remember(filteredStatementList) { filteredStatementList.filter { it.isMatched } }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isLoading = true
                try {
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val requestFile = bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("statement", "statement.xlsx", requestFile)
                        statementList = bookingRepository.uploadStatement(body)
                        uploadedFiles = bookingRepository.getUploadedFiles()
                        Toast.makeText(context, "Statement uploaded & parsed successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
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
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Match GPay statement entries with booking receipts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary and Stats Card
                val latestStatementDate = remember(statementList) {
                    if (statementList.isEmpty()) null
                    else statementList.map { it.date }.maxOrNull()
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
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
                            Column {
                                Text(
                                    text = "Statement Overview",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (statementList.isEmpty()) "No statement uploaded" else "${statementList.size} deposit transactions found",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                latestStatementDate?.let { date ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Uploaded till: $date",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            if (statementList.isEmpty()) {
                                Button(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload Statement", fontSize = 13.sp)
                                }
                            } else {
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
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        if (statementList.isNotEmpty()) {
                            val matchProgress = if (statementList.isEmpty()) 0f else (matchedList.size.toFloat() / statementList.size.toFloat())
                            
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Reconciliation Progress",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "${matchedList.size}/${statementList.size} matched (${(matchProgress * 100).toInt()}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { matchProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
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
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Reconcile",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reconcile Payments", fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload New",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload New", fontSize = 13.sp)
                                }
                            }
                        }

                        // Collapsible Uploaded Files List
                        if (uploadedFiles.isNotEmpty()) {
                            var showFilesList by remember { mutableStateOf(false) }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFilesList = !showFilesList }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Uploaded Files (${uploadedFiles.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (showFilesList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle Files",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (showFilesList) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    uploadedFiles.forEach { file ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
                                                            Toast.makeText(context, "Statement file deleted", Toast.LENGTH_SHORT).show()
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Error deleting file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete File",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { openExcelFile(context, file.id) }
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.List,
                                                    contentDescription = "File",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = formatFileDisplayName(file),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = formatUploadDate(file.uploadDate),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Filtration Card
                if (statementList.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFilters = !showFilters },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Filter Transactions",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (filterText.isNotEmpty() || filterAmount.isNotEmpty() || filterMonth != "All Months") {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Active",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = if (showFilters) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (showFilters) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = filterText,
                                        onValueChange = { filterText = it },
                                        label = { Text("Desc/Remarks", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1.5f),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        singleLine = true,
                                        trailingIcon = {
                                            if (filterText.isNotEmpty()) {
                                                IconButton(onClick = { filterText = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Clear",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    OutlinedTextField(
                                        value = filterAmount,
                                        onValueChange = { filterAmount = it },
                                        label = { Text("Amount", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        singleLine = true,
                                        trailingIcon = {
                                            if (filterAmount.isNotEmpty()) {
                                                IconButton(onClick = { filterAmount = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Clear",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var monthMenuExpanded by remember { mutableStateOf(false) }
                                    val monthsList = listOf(
                                        "All Months", "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = filterMonth,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Month", fontSize = 11.sp) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { monthMenuExpanded = true },
                                            enabled = false,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            textStyle = MaterialTheme.typography.bodySmall,
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "Dropdown",
                                                    modifier = Modifier.clickable { monthMenuExpanded = true }
                                                )
                                            }
                                        )
                                        DropdownMenu(
                                            expanded = monthMenuExpanded,
                                            onDismissRequest = { monthMenuExpanded = false }
                                        ) {
                                            monthsList.forEach { m ->
                                                DropdownMenuItem(
                                                    text = { Text(m) },
                                                    onClick = {
                                                        filterMonth = m
                                                        monthMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (filterText.isNotEmpty() || filterAmount.isNotEmpty() || filterMonth != "All Months") {
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
                }

                if (statementList.isNotEmpty()) {
                    // Tab Selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
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

                    // Statements Table/List
                    val activeList = if (selectedTab == 0) unmatchedList else matchedList
                    
                    if (activeList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
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
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeList, key = { it.id }) { record ->
                                StatementItem(
                                    record = record,
                                    onClick = if (record.isMatched && record.matchedBookingId.isNotEmpty()) {
                                        { onSelectBookingId(record.matchedBookingId) }
                                    } else null
                                )
                            }
                        }
                    }
                } else {
                    // Upload Empty State
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Spreadsheet Icon",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Excel statement parser handles most GPay statement layouts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Upload Icon"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Bank Statement file")
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
