package com.sparsh.myapplication.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.sparsh.myapplication.Booking
import com.sparsh.myapplication.BookingItem
import com.sparsh.myapplication.PaymentDetail
import com.sparsh.myapplication.getRoomsForCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BookingAllocationInput(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "Standard",
    val rate: String = "",
    val dormBedsCount: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    bookings: List<Booking>,
    onSaveBooking: (Booking) -> Unit,
    onDeleteBooking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        )
    )

    var showAddBookingDialog by remember { mutableStateOf(false) }
    var selectedBookingForAssignment by remember { mutableStateOf<Booking?>(null) }
    var selectedBookingForPayment by remember { mutableStateOf<Booking?>(null) }
    var selectedBookingForEdit by remember { mutableStateOf<Booking?>(null) }

    val unassignedBookings = remember(bookings) {
        bookings.filter { !it.isAssigned }.sortedBy { it.checkInDate }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Bookings (Yet to Check-In)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                actions = {
                    IconButton(onClick = { showAddBookingDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Unassigned Booking",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(formBackground)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (unassignedBookings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No unassigned bookings yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Button(
                            onClick = { showAddBookingDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Booking")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(unassignedBookings, key = { it.id }) { booking ->
                        UnassignedBookingCard(
                            booking = booking,
                            onAssignClick = { selectedBookingForAssignment = booking },
                            onAddPaymentClick = { selectedBookingForPayment = booking },
                            onDeleteClick = { onDeleteBooking(booking.id) },
                            onClick = { selectedBookingForEdit = booking }
                        )
                    }
                }
            }
        }
    }

    if (showAddBookingDialog) {
        AddUnassignedBookingDialog(
            bookingToEdit = null,
            onDismiss = { showAddBookingDialog = false },
            onConfirm = { newBooking ->
                onSaveBooking(newBooking)
                showAddBookingDialog = false
            }
        )
    }

    if (selectedBookingForEdit != null) {
        AddUnassignedBookingDialog(
            bookingToEdit = selectedBookingForEdit,
            onDismiss = { selectedBookingForEdit = null },
            onConfirm = { updatedBooking ->
                onSaveBooking(updatedBooking)
                selectedBookingForEdit = null
            }
        )
    }

    if (selectedBookingForAssignment != null) {
        AssignRoomsDialog(
            booking = selectedBookingForAssignment!!,
            bookings = bookings,
            onDismiss = { selectedBookingForAssignment = null },
            onConfirm = { updatedBooking ->
                onSaveBooking(updatedBooking)
                selectedBookingForAssignment = null
            }
        )
    }

    if (selectedBookingForPayment != null) {
        AddPaymentDialog(
            booking = selectedBookingForPayment!!,
            onDismiss = { selectedBookingForPayment = null },
            onConfirm = { updatedBooking ->
                onSaveBooking(updatedBooking)
                selectedBookingForPayment = null
            }
        )
    }
}

@Composable
fun UnassignedBookingCard(
    booking: Booking,
    onAssignClick: () -> Unit,
    onAddPaymentClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    val hasBalance = booking.balance > 0.0
    val borderStroke = if (hasBalance) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.error)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Name and Platform
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.guestName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Check-in: ${booking.checkInDate}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Platform Chip
                val platformColors = getPlatformColorsForUnassigned(booking.platform)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(platformColors.first)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = booking.platform,
                        color = platformColors.second,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Booking items detail list
            Text(
                text = "Requested Allocations:",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Group room allocations by category and price
                val roomsGrouped = booking.items.filter { it.category != "Dorm Bed" }.groupBy { Pair(it.category, it.amount) }
                roomsGrouped.forEach { (pair, items) ->
                    val (category, amount) = pair
                    Text(
                        text = "• ${items.size}x $category Rooms @ ₹${amount.toInt()} each",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val dormItems = booking.items.filter { it.category == "Dorm Bed" }
                if (dormItems.isNotEmpty()) {
                    Text(
                        text = "• ${dormItems.size}x Dorm Beds @ ₹${dormItems.first().amount.toInt()} each",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else if (booking.dormBedsSelected > 0) {
                    val rate = if (booking.dormBedsSelected > 0) booking.dormTotalAmount / booking.dormBedsSelected else 0.0
                    Text(
                        text = "• ${booking.dormBedsSelected}x Dorm Beds @ ₹${rate.toInt()} each",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (booking.notes.isNotBlank()) {
                Text(
                    text = "Notes: ${booking.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Pricing summary & balance highlight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total: ₹${booking.amountCharged.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paid: ₹${booking.totalPaid.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasBalance) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Balance: ₹${booking.balance.toInt()}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Paid in Full",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAssignClick,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Assign Room", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                if (hasBalance) {
                    OutlinedButton(
                        onClick = onAddPaymentClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Payment", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete booking",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun getPlatformColorsForUnassigned(platform: String): Pair<Color, Color> {
    return when (platform) {
        "Direct" -> Pair(Color(0xFFE8EAF6), Color(0xFF1A237E))
        "MMT" -> Pair(Color(0xFFE8F5E9), Color(0xFF1B5E20))
        "Booking.com" -> Pair(Color(0xFFE3F2FD), Color(0xFF0D47A1))
        "Agoda" -> Pair(Color(0xFFF3E5F5), Color(0xFF4A148C))
        "Goibibo" -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        "Cleartrip" -> Pair(Color(0xFFFFFDE7), Color(0xFFF57F17))
        else -> Pair(Color(0xFFECEFF1), Color(0xFF263238))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUnassignedBookingDialog(
    bookingToEdit: Booking? = null,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit
) {
    var guestName by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.guestName ?: "") }
    var checkInDate by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.checkInDate ?: "") }
    var platform by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.platform ?: "Direct") }
    var notes by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.notes ?: "") }

    var dialogPayments by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.payments ?: emptyList()) }
    var advancePaymentStr by remember(bookingToEdit) { mutableStateOf("") }
    var newPaymentAmountStr by remember { mutableStateOf("") }
    var newPaymentMethod by remember { mutableStateOf("UPI") }
    var advancePaymentMethod by remember(bookingToEdit) { mutableStateOf("UPI") }

    val initialAllocations = remember(bookingToEdit) {
        if (bookingToEdit != null) {
            val nonDorm = bookingToEdit.items.filter { it.category != "Dorm Bed" }.map { item ->
                BookingAllocationInput(
                    id = item.id,
                    category = item.category,
                    rate = item.amount.toInt().toString(),
                    dormBedsCount = 1
                )
            }
            val dormItems = bookingToEdit.items.filter { it.category == "Dorm Bed" }
            val dormAlloc = if (dormItems.isNotEmpty()) {
                listOf(
                    BookingAllocationInput(
                        id = UUID.randomUUID().toString(),
                        category = "Dorm Bed",
                        rate = dormItems.sumOf { it.amount }.toInt().toString(),
                        dormBedsCount = dormItems.size
                    )
                )
            } else {
                emptyList()
            }
            nonDorm + dormAlloc
        } else {
            listOf(BookingAllocationInput())
        }
    }

    var selectedAllocations by remember(initialAllocations) { mutableStateOf(initialAllocations) }

    var guestNameError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var allocationsError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            checkInDate = formatter.format(cal.time)
            dateError = null
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                text = if (bookingToEdit != null) "Edit Booking (Unassigned)" else "Add Booking (Unassigned)",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Guest Name
                item {
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { 
                            guestName = it 
                            if (it.isNotBlank()) guestNameError = null
                        },
                        label = { Text("Guest Name (Required)") },
                        isError = guestNameError != null,
                        supportingText = guestNameError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Check-in Date
                item {
                    OutlinedTextField(
                        value = checkInDate,
                        onValueChange = {},
                        label = { Text("Check-in Date (Required)") },
                        isError = dateError != null,
                        supportingText = dateError?.let { { Text(it) } },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Platform Selection
                item {
                    Column {
                        Text("Platform", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        val platformsList = listOf("Direct", "MMT", "Booking.com", "Agoda", "Goibibo", "Cleartrip")
                        platformsList.chunked(3).forEach { rowPlatforms ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPlatforms.forEach { plat ->
                                    val isSel = platform == plat
                                    ElevatedCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clickable { platform = plat },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = plat,
                                                fontSize = 11.sp,
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Room allocations list
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Allocate Room Types", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        
                        if (allocationsError != null) {
                            Text(allocationsError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }

                        // List of allocated room selections (Editable inline)
                        val categories = listOf("Standard", "Deluxe", "Double", "Family", "Deluxe Family", "Dorm Bed")
                        
                        selectedAllocations.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category dropdown card
                                var dropdownExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1.5f)) {
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clickable { dropdownExpanded = true },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(item.category, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false }
                                    ) {
                                        categories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat) },
                                                onClick = {
                                                    selectedAllocations = selectedAllocations.map {
                                                        if (it.id == item.id) it.copy(category = cat) else it
                                                    }
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Beds count input if category is Dorm Bed
                                if (item.category == "Dorm Bed") {
                                    OutlinedTextField(
                                        value = if (item.dormBedsCount > 0) item.dormBedsCount.toString() else "",
                                        onValueChange = { newVal ->
                                            val beds = newVal.toIntOrNull() ?: 0
                                            selectedAllocations = selectedAllocations.map {
                                                if (it.id == item.id) it.copy(dormBedsCount = beds) else it
                                            }
                                        },
                                        placeholder = { Text("Beds") },
                                        modifier = Modifier.weight(0.8f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )
                                }

                                // Rate input
                                OutlinedTextField(
                                    value = item.rate,
                                    onValueChange = { newVal ->
                                        selectedAllocations = selectedAllocations.map {
                                            if (it.id == item.id) it.copy(rate = newVal) else it
                                        }
                                    },
                                    placeholder = { Text(if (item.category == "Dorm Bed") "Total Rate" else "Rate") },
                                    prefix = { Text("₹", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                )

                                // Delete button
                                IconButton(
                                    onClick = {
                                        selectedAllocations = selectedAllocations.filter { it.id != item.id }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete allocation",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Button to add new item
                        Button(
                            onClick = {
                                selectedAllocations = selectedAllocations + BookingAllocationInput()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Allocation Item", fontSize = 11.sp)
                        }
                    }
                }

                // Payments Details Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Payments Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val totalBillValue = selectedAllocations.sumOf { it.rate.toDoubleOrNull() ?: 0.0 }

                        if (bookingToEdit == null) {
                            // New Booking: Show Advance Payment
                            OutlinedTextField(
                                value = advancePaymentStr,
                                onValueChange = { advancePaymentStr = it },
                                label = { Text("Advance Amount") },
                                placeholder = { Text("e.g. 1000 (0 for none)") },
                                prefix = { Text("₹ ") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        } else {
                            // Edit Booking: Show list of payments and allow adding new ones
                            val totalPaidVal = dialogPayments.sumOf { it.amount }
                            val balanceVal = totalBillValue - totalPaidVal

                            if (dialogPayments.isEmpty()) {
                                Text(
                                    text = "No payments recorded yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            } else {
                                dialogPayments.forEach { p ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "₹${p.amount.toInt()} via ${p.method}",
                                            fontSize = 12.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        IconButton(
                                            onClick = {
                                                dialogPayments = dialogPayments.filter { it.id != p.id }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove payment",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total: ₹${totalBillValue.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Paid: ₹${totalPaidVal.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Balance: ₹${balanceVal.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (balanceVal > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }

                            if (balanceVal > 0.0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Text("Record Additional Payment", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newPaymentAmountStr,
                                        onValueChange = { newPaymentAmountStr = it },
                                        placeholder = { Text("Amount") },
                                        prefix = { Text("₹ ") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )

                                    Button(
                                        onClick = {
                                            val amt = newPaymentAmountStr.toDoubleOrNull() ?: 0.0
                                            if (amt > 0.0) {
                                                dialogPayments = dialogPayments + PaymentDetail(amount = amt, method = newPaymentMethod)
                                                newPaymentAmountStr = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text("Record")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("UPI", "Cash", "Card", "Bank Transfer").forEach { method ->
                                        val isSel = newPaymentMethod == method
                                        ElevatedCard(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .clickable { newPaymentMethod = method },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = method,
                                                    fontSize = 9.sp,
                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic OTA Commission Calculation Display Label
                if (platform != "Direct") {
                    item {
                        val totalBillValue = selectedAllocations.sumOf { it.rate.toDoubleOrNull() ?: 0.0 }
                        val breakdown = com.sparsh.myapplication.SettingsManager.calculateBreakdown(context, platform, totalBillValue)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$platform Commission Breakdown",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                
                                val formatMoney = { amt: Double -> "₹${String.format(Locale.US, "%.2f", amt)}" }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Guest Paid (Total):", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(formatMoney(breakdown.guestAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val taxPct = com.sparsh.myapplication.SettingsManager.getTaxRate(context)
                                    Text("Tax ($taxPct%):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatMoney(breakdown.tax), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Base Amount (Total - Tax):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatMoney(breakdown.base), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val commPct = com.sparsh.myapplication.SettingsManager.getCommissionRate(context, platform)
                                    Text("Commission ($commPct% of Base):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatMoney(breakdown.commission), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val tdsPct = com.sparsh.myapplication.SettingsManager.getTdsRate(context)
                                    Text("TDS ($tdsPct% of Base):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatMoney(breakdown.tds), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val tcsPct = com.sparsh.myapplication.SettingsManager.getTcsRate(context)
                                    Text("TCS ($tcsPct% of Base):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatMoney(breakdown.tcs), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Deductions (Expense):", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    Text(formatMoney(breakdown.totalDeductions), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                                
                                val remainingBase = breakdown.base - breakdown.totalDeductions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Remaining (Base - Deductions):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatMoney(remainingBase), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Platform Pays Us (Remaining + Tax):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(formatMoney(breakdown.netPayout), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Notes / Remarks
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Remarks") },
                        placeholder = { Text("Add comments...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    guestNameError = null
                    dateError = null
                    allocationsError = null

                    var hasError = false
                    if (guestName.trim().isEmpty()) {
                        guestNameError = "Guest Name is required."
                        hasError = true
                    }
                    if (checkInDate.trim().isEmpty()) {
                        dateError = "Check-in date is required."
                        hasError = true
                    }
                    if (selectedAllocations.isEmpty()) {
                        allocationsError = "Please allocate at least one room type."
                        hasError = true
                    }
                    val validAllocations = selectedAllocations.all { 
                        val rateVal = it.rate.toDoubleOrNull() ?: 0.0
                        val bedsVal = if (it.category == "Dorm Bed") it.dormBedsCount else 1
                        rateVal > 0.0 && bedsVal > 0
                    }
                    if (!validAllocations && selectedAllocations.isNotEmpty()) {
                        allocationsError = "Please enter valid rates and bed counts (> 0) for all allocations."
                        hasError = true
                    }

                    if (hasError) return@Button

                    // Build list of BookingItems
                    val itemsList = selectedAllocations.flatMap { alloc ->
                        if (alloc.category == "Dorm Bed") {
                            val beds = alloc.dormBedsCount.coerceAtLeast(1)
                            val totalRate = alloc.rate.toDoubleOrNull() ?: 0.0
                            val ratePerBed = totalRate / beds
                            List(beds) {
                                BookingItem(
                                    id = UUID.randomUUID().toString(),
                                    category = "Dorm Bed",
                                    roomNumber = "",
                                    amount = ratePerBed
                                )
                            }
                        } else {
                            listOf(
                                BookingItem(
                                    id = if (bookingToEdit != null && alloc.id.isNotEmpty()) alloc.id else UUID.randomUUID().toString(),
                                    category = alloc.category,
                                    roomNumber = "", // Unassigned initially
                                    amount = alloc.rate.toDoubleOrNull() ?: 0.0
                                )
                            )
                        }
                    }

                    // Separate out dorm bed counts
                    val dormItems = itemsList.filter { it.category == "Dorm Bed" }
                    val dormTotalVal = dormItems.sumOf { it.amount }

                    // Construct payments
                    val finalPayments = if (bookingToEdit != null) {
                        dialogPayments
                    } else {
                        val initialPayments = mutableListOf<PaymentDetail>()
                        val advVal = advancePaymentStr.toDoubleOrNull() ?: 0.0
                        if (advVal > 0.0) {
                            initialPayments.add(
                                PaymentDetail(
                                    amount = advVal,
                                    method = "UPI"
                                )
                            )
                        }
                        initialPayments
                    }

                    val totalAmount = itemsList.sumOf { it.amount }

                    val newBooking = Booking(
                        id = bookingToEdit?.id ?: UUID.randomUUID().toString(),
                        checkInDate = checkInDate,
                        platform = platform,
                        guestName = guestName.trim(),
                        items = itemsList,
                        dormBedsSelected = dormItems.size,
                        dormTotalAmount = dormTotalVal,
                        isBillOn = bookingToEdit?.isBillOn ?: false,
                        billAmount = totalAmount,
                        expenses = if (platform != "Direct") {
                            com.sparsh.myapplication.SettingsManager.calculateBreakdown(context, platform, totalAmount).totalDeductions
                        } else {
                            0.0
                        },
                        payments = finalPayments,
                        notes = notes.trim(),
                        timestamp = bookingToEdit?.timestamp ?: System.currentTimeMillis()
                    )

                    onConfirm(newBooking)
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignRoomsDialog(
    booking: Booking,
    bookings: List<Booking>,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit
) {
    // Separate standard room items from dorm bed items
    val standardItems = remember(booking) { booking.items.filter { it.category != "Dorm Bed" } }
    val dormItems = remember(booking) { booking.items.filter { it.category == "Dorm Bed" } }

    var assignments by remember { mutableStateOf(mapOf<String, String>()) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var dormRoom by remember { mutableStateOf("A") }
    var manualBedNoToggle by remember { mutableStateOf(false) }
    var manualBedNoText by remember { mutableStateOf("") }

    val counts = remember(dormRoom, bookings, booking.checkInDate, booking.id) {
        getDormBedBookingCounts(booking.checkInDate, dormRoom, bookings, booking.id)
    }
    val freeBeds = remember(counts) {
        (1..8).filter { (counts[it] ?: 0) == 0 }.sorted()
    }

    val parsedDormBeds = remember(manualBedNoToggle, manualBedNoText, freeBeds, dormItems.size, dormRoom) {
        if (manualBedNoToggle) {
            parseBedNumbers(manualBedNoText, dormRoom)
        } else {
            freeBeds.take(dormItems.size).map { "$dormRoom$it" }
        }
    }

    LaunchedEffect(booking) {
        assignments = booking.items.filter { it.category != "Dorm Bed" }.associate { it.id to it.roomNumber }
        
        val firstDormItem = booking.items.firstOrNull { it.category == "Dorm Bed" && it.roomNumber.isNotBlank() }
        if (firstDormItem != null) {
            dormRoom = if (firstDormItem.roomNumber.startsWith("B", ignoreCase = true)) "B" else "A"
            manualBedNoToggle = true
            manualBedNoText = booking.items.filter { it.category == "Dorm Bed" && it.roomNumber.startsWith(dormRoom, ignoreCase = true) }
                .map { it.roomNumber.substring(dormRoom.length) }
                .sorted()
                .joinToString(", ")
        } else {
            dormRoom = "A"
            manualBedNoToggle = false
            manualBedNoText = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                text = "Assign Rooms (${booking.guestName})",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select available room numbers for each of the requested categories on ${booking.checkInDate}:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (showError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Standard Room Allocations List
                    items(standardItems, key = { it.id }) { item ->
                        val category = item.category
                        val assignedRoom = assignments[item.id] ?: ""

                        // Query available rooms of this category on this date
                        val allRoomsForCat = getRoomsForCategory(category)
                        val availableRooms = remember(allRoomsForCat, bookings, assignments) {
                            allRoomsForCat.filter { room ->
                                val isBookedElsewhere = bookings.any { b ->
                                    b.id != booking.id && 
                                    b.checkInDate == booking.checkInDate && 
                                    b.items.any { bi -> bi.roomNumber == room }
                                }
                                val isSelectedByOtherItem = assignments.any { (id, selectedRoom) ->
                                    id != item.id && selectedRoom == room
                                }
                                !isBookedElsewhere && !isSelectedByOtherItem
                            }
                        }

                        var dropdownExpanded by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Requested Room: $category",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clickable { dropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (assignedRoom.isNotBlank()) "Room $assignedRoom" else "Select Room...",
                                            fontSize = 13.sp,
                                            fontWeight = if (assignedRoom.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                            color = if (assignedRoom.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    if (availableRooms.isEmpty() && assignedRoom.isBlank()) {
                                        DropdownMenuItem(
                                            text = { Text("No available rooms!") },
                                            onClick = {},
                                            enabled = false
                                        )
                                    } else {
                                        availableRooms.forEach { room ->
                                            DropdownMenuItem(
                                                text = { Text("Room $room") },
                                                onClick = {
                                                    assignments = assignments + (item.id to room)
                                                    dropdownExpanded = false
                                                    showError = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Dorm Beds Allocation Group Section
                    if (dormItems.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Requested Dorm Beds: ${dormItems.size} beds",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Select Dorm Room
                                Column {
                                    Text("Select Dorm Room", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("A", "B").forEach { room ->
                                            val isSel = dormRoom == room
                                            ElevatedCard(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clickable { dormRoom = room },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "Dorm Room $room",
                                                        fontSize = 11.sp,
                                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Manual input switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Assign Bed Numbers Manually",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "e.g. 1,2,3 or 1-3",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Switch(
                                        checked = manualBedNoToggle,
                                        onCheckedChange = { manualBedNoToggle = it }
                                    )
                                }

                                if (manualBedNoToggle) {
                                    OutlinedTextField(
                                        value = manualBedNoText,
                                        onValueChange = { 
                                            manualBedNoText = it 
                                            showError = false
                                        },
                                        placeholder = { Text("e.g. 1-3, 5") },
                                        label = { Text("Bed Numbers") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                // Live parsed preview
                                if (parsedDormBeds.isNotEmpty()) {
                                    Text(
                                        text = "Beds assigned: ${parsedDormBeds.joinToString(", ") { it.substring(dormRoom.length) }} (${parsedDormBeds.size} of ${dormItems.size} beds)",
                                        fontSize = 11.sp,
                                        color = if (parsedDormBeds.size == dormItems.size) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "Please assign ${dormItems.size} beds.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showError = false
                    
                    // Validate standard items
                    val standardAllAssigned = standardItems.all { item ->
                        assignments[item.id]?.isNotBlank() == true
                    }
                    if (!standardAllAssigned) {
                        errorMessage = "Please select room numbers for all standard rooms."
                        showError = true
                        return@Button
                    }

                    // Validate dorm items
                    if (dormItems.isNotEmpty() && parsedDormBeds.size != dormItems.size) {
                        errorMessage = "Please assign exactly ${dormItems.size} beds. Currently assigned: ${parsedDormBeds.size}"
                        showError = true
                        return@Button
                    }

                    // Build updated items list
                    val updatedStandardItems = standardItems.map { item ->
                        item.copy(roomNumber = assignments[item.id] ?: "")
                    }
                    val updatedDormItems = dormItems.zip(parsedDormBeds) { item, bedNo ->
                        item.copy(roomNumber = bedNo)
                    }
                    val updatedItems = updatedStandardItems + updatedDormItems

                    // Construct bed assignment codes for dorm beds inside old schema properties
                    val dormItemsAssigned = updatedItems.filter { it.category == "Dorm Bed" }
                    val dormRoomABedsStr = dormItemsAssigned.filter { it.roomNumber.startsWith("A") }
                        .map { it.roomNumber.substring(1) }.sorted().joinToString(",")
                    val dormRoomBBedsStr = dormItemsAssigned.filter { it.roomNumber.startsWith("B") }
                        .map { it.roomNumber.substring(1) }.sorted().joinToString(",")

                    val updatedBooking = booking.copy(
                        items = updatedItems,
                        dormRoomABeds = dormRoomABedsStr,
                        dormRoomBBeds = dormRoomBBedsStr
                    )

                    onConfirm(updatedBooking)
                }
            ) {
                Text("Confirm Assignment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit
) {
    var paymentAmountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("UPI") }
    var paymentAmountError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                text = "Add Payment Details",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Record a payment detail for ${booking.guestName}. Outstanding Balance: ₹${booking.balance.toInt()}")

                OutlinedTextField(
                    value = paymentAmountStr,
                    onValueChange = { 
                        paymentAmountStr = it 
                        if (it.isNotBlank()) paymentAmountError = null
                    },
                    label = { Text("Payment Amount (Required)") },
                    prefix = { Text("₹ ") },
                    isError = paymentAmountError != null,
                    supportingText = paymentAmountError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI", "Cash", "Card", "Bank Transfer").forEach { method ->
                        val isSel = paymentMethod == method
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clickable { paymentMethod = method },
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = method,
                                    fontSize = 10.sp,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtVal = paymentAmountStr.toDoubleOrNull() ?: 0.0
                    if (amtVal <= 0.0) {
                        paymentAmountError = "Please enter a valid payment amount."
                        return@Button
                    }
                    if (amtVal > booking.balance) {
                        paymentAmountError = "Amount cannot exceed outstanding balance of ₹${booking.balance.toInt()}."
                        return@Button
                    }

                    // Create PaymentDetail and append
                    val newPayment = PaymentDetail(
                        amount = amtVal,
                        method = paymentMethod
                    )
                    val updatedPayments = booking.payments + newPayment
                    val updatedBooking = booking.copy(
                        payments = updatedPayments
                    )

                    onConfirm(updatedBooking)
                }
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
