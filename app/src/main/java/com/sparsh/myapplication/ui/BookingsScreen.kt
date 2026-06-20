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
import com.sparsh.myapplication.getStayDate
import com.sparsh.myapplication.datesOverlap
import com.sparsh.myapplication.adjustRatesList
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private fun formatDouble(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
    }
}

data class BookingAllocationInput(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "Standard",
    val rate: String = "",
    val dormBedsCount: Int = 1,
    val nights: Int = 1,
    val samePrice: Boolean = true,
    val rates: List<String> = emptyList(),
    val startDate: String? = null
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
            },
            onSaveWithoutDismiss = { updatedBooking ->
                onSaveBooking(updatedBooking)
                selectedBookingForEdit = updatedBooking
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
                    val firstItem = items.firstOrNull()
                    val nights = firstItem?.nights ?: 1
                    val rateText = if (nights > 1) {
                        val avgRate = amount / nights
                        "₹${avgRate.toInt()}/night ($nights nights)"
                    } else {
                        "₹${amount.toInt()}"
                    }
                    Text(
                        text = "• ${items.size}x $category Rooms @ $rateText each",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val dormItems = booking.items.filter { it.category == "Dorm Bed" }
                if (dormItems.isNotEmpty()) {
                    val firstDorm = dormItems.first()
                    val nights = firstDorm.nights
                    val rateText = if (nights > 1) {
                        val avgRate = firstDorm.amount / nights
                        "₹${avgRate.toInt()}/night ($nights nights)"
                    } else {
                        "₹${firstDorm.amount.toInt()}"
                    }
                    Text(
                        text = "• ${dormItems.size}x Dorm Beds @ $rateText each",
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
                        text = "Total: ₹${formatDouble(booking.amountCharged)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paid: ₹${formatDouble(booking.totalPaid)}",
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
                            text = "Balance: ₹${formatDouble(booking.balance)}",
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
    onConfirm: (Booking) -> Unit,
    onSaveWithoutDismiss: ((Booking) -> Unit)? = null
) {
    var guestName by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.guestName ?: "") }
    val currentDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    }
    var checkInDate by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.checkInDate ?: currentDateStr) }
    var platform by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.platform ?: "Direct") }
    var notes by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.notes ?: "") }

    var dialogPayments by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.payments ?: emptyList()) }
    var advancePaymentStr by remember(bookingToEdit) { mutableStateOf("") }
    var newPaymentAmountStr by remember { mutableStateOf("") }
    var newPaymentMethod by remember { mutableStateOf("UPI (Hotel Acc - GPay)") }
    var advancePaymentMethod by remember(bookingToEdit) { mutableStateOf("UPI (Hotel Acc - GPay)") }
    var newPaymentDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var advancePaymentIsUnknown by remember(bookingToEdit) { mutableStateOf(false) }
    var newPaymentIsUnknown by remember(bookingToEdit) { mutableStateOf(false) }

    var bookingNights by remember(bookingToEdit) {
        mutableStateOf(bookingToEdit?.items?.map { it.nights }?.maxOrNull() ?: 1)
    }
    var samePriceForAllNights by remember(bookingToEdit) {
        mutableStateOf(bookingToEdit?.items?.all { it.rates.distinct().size <= 1 } ?: true)
    }
    var isBillOn by remember(bookingToEdit) { mutableStateOf(bookingToEdit?.isBillOn ?: false) }
    var billAmountStr by remember(bookingToEdit) {
        mutableStateOf(if (bookingToEdit == null || bookingToEdit.billAmount == 0.0 || !bookingToEdit.isBillOn) "" else formatDouble(bookingToEdit.billAmount))
    }

    val initialAllocations = remember(bookingToEdit) {
        if (bookingToEdit != null) {
            val nonDorm = bookingToEdit.items.filter { it.category != "Dorm Bed" }.map { item ->
                val allRatesEqual = item.rates.distinct().size <= 1
                BookingAllocationInput(
                    id = item.id,
                    category = item.category,
                    rate = if (allRatesEqual) formatDouble(item.amount) else "",
                    dormBedsCount = 1,
                    nights = if (item.nights > 0) item.nights else 1,
                    samePrice = allRatesEqual,
                    rates = item.rates.map { formatDouble(it) },
                    startDate = item.startDate
                )
            }
            val dormItems = bookingToEdit.items.filter { it.category == "Dorm Bed" }
            val dormAlloc = if (dormItems.isNotEmpty()) {
                val firstDorm = dormItems.first()
                val allDormRatesEqual = firstDorm.rates.distinct().size <= 1
                val dormSum = dormItems.sumOf { it.amount }
                val combinedRates = if (allDormRatesEqual) {
                    emptyList()
                } else {
                    val ratesCount = firstDorm.rates.size
                    val arr = DoubleArray(ratesCount)
                    dormItems.forEach { item ->
                        for (k in 0 until item.rates.size.coerceAtMost(ratesCount)) {
                            arr[k] += item.rates[k]
                        }
                    }
                    arr.map { formatDouble(it) }
                }
                listOf(
                    BookingAllocationInput(
                        id = UUID.randomUUID().toString(),
                        category = "Dorm Bed",
                        rate = if (allDormRatesEqual) formatDouble(dormSum) else "",
                        dormBedsCount = dormItems.size,
                        nights = if (firstDorm.nights > 0) firstDorm.nights else 1,
                        samePrice = allDormRatesEqual,
                        rates = combinedRates,
                        startDate = firstDorm.startDate
                    )
                )
            } else {
                emptyList()
            }
            nonDorm + dormAlloc
        } else {
            listOf(BookingAllocationInput(nights = bookingNights, samePrice = samePriceForAllNights, rates = adjustRatesList(emptyList(), if (samePriceForAllNights) 1 else bookingNights)))
        }
    }

    var selectedAllocations by remember(initialAllocations) { mutableStateOf(initialAllocations) }

    val totalBillValue = remember(selectedAllocations, platform, bookingNights, samePriceForAllNights) {
        selectedAllocations.sumOf { alloc ->
            val allocNights = if (platform == "Direct") alloc.nights else bookingNights
            val allocSamePrice = if (platform == "Direct") alloc.samePrice else samePriceForAllNights
            if (allocSamePrice) {
                alloc.rate.toDoubleOrNull() ?: 0.0
            } else {
                alloc.rates.take(allocNights).sumOf { it.toDoubleOrNull() ?: 0.0 }
            }
        }
    }

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
        modifier = Modifier
            .fillMaxWidth(0.98f)
            .navigationBarsPadding()
            .imePadding(),
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
                    .heightIn(max = 650.dp),
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
                    val displayCheckInDate = if (checkInDate.isNotBlank()) {
                        try {
                            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val formatter = SimpleDateFormat("dd/MM", Locale.US)
                            val d = parser.parse(checkInDate)
                            if (d != null) formatter.format(d) else checkInDate
                        } catch (e: Exception) {
                            checkInDate
                        }
                    } else ""

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = displayCheckInDate,
                            onValueChange = {},
                            label = { Text("Check-in Date (Required)") },
                            isError = dateError != null,
                            supportingText = dateError?.let { { Text(it) } },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { datePickerDialog.show() }
                        )
                    }
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
                                            .clickable {
                                                platform = plat
                                                if (platform != "Direct") {
                                                    isBillOn = false
                                                    selectedAllocations = selectedAllocations.map { alloc ->
                                                        val targetSize = if (samePriceForAllNights) 1 else bookingNights
                                                        alloc.copy(
                                                            nights = bookingNights,
                                                            samePrice = samePriceForAllNights,
                                                            rates = adjustRatesList(alloc.rates, targetSize),
                                                            rate = if (samePriceForAllNights) (alloc.rates.firstOrNull() ?: alloc.rate) else ""
                                                        )
                                                    }
                                                }
                                            },
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

                // Global Nights & Same Price Toggle
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nights",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (platform == "Direct") {
                                    Text(
                                        text = "Default for new allocations",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (bookingNights > 1) {
                                            bookingNights--
                                            if (platform != "Direct") {
                                                selectedAllocations = selectedAllocations.map { alloc ->
                                                    val targetSize = if (samePriceForAllNights) 1 else bookingNights
                                                    alloc.copy(
                                                        nights = bookingNights,
                                                        samePrice = samePriceForAllNights,
                                                        rates = adjustRatesList(alloc.rates, targetSize),
                                                        rate = if (samePriceForAllNights) (alloc.rates.firstOrNull() ?: alloc.rate) else ""
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    enabled = bookingNights > 1,
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrease nights")
                                }
                                
                                Text(
                                    text = bookingNights.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                
                                IconButton(
                                    onClick = {
                                        bookingNights++
                                        if (platform != "Direct") {
                                            selectedAllocations = selectedAllocations.map { alloc ->
                                                val targetSize = if (samePriceForAllNights) 1 else bookingNights
                                                alloc.copy(
                                                    nights = bookingNights,
                                                    samePrice = samePriceForAllNights,
                                                    rates = adjustRatesList(alloc.rates, targetSize),
                                                    rate = if (samePriceForAllNights) (alloc.rates.firstOrNull() ?: alloc.rate) else ""
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increase nights")
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Same price for all nights",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (platform == "Direct") {
                                    Text(
                                        text = "Default for new allocations",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Switch(
                                checked = samePriceForAllNights,
                                onCheckedChange = { checked ->
                                    samePriceForAllNights = checked
                                    if (platform != "Direct") {
                                        selectedAllocations = selectedAllocations.map { alloc ->
                                            val targetSize = if (checked) 1 else bookingNights
                                            alloc.copy(
                                                samePrice = checked,
                                                rates = adjustRatesList(alloc.rates, targetSize),
                                                rate = if (checked) (alloc.rates.firstOrNull() ?: alloc.rate) else ""
                                            )
                                        }
                                    }
                                }
                            )
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
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
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

                                    val itemStartDate = item.startDate.takeIf { !it.isNullOrBlank() } ?: checkInDate
                                    val itemDateFormatted = try {
                                        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                        val formatter = SimpleDateFormat("dd/MM", Locale.US)
                                        val d = parser.parse(itemStartDate)
                                        if (d != null) formatter.format(d) else itemStartDate
                                    } catch (e: Exception) {
                                        itemStartDate
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clickable {
                                                val cal = Calendar.getInstance().apply {
                                                    try {
                                                        time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(itemStartDate) ?: Date()
                                                    } catch (e: Exception) {}
                                                }
                                                DatePickerDialog(
                                                    context,
                                                    { _, y, m, dOfMonth ->
                                                        val selectedCal = Calendar.getInstance()
                                                        selectedCal.set(y, m, dOfMonth)
                                                        val newDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedCal.time)
                                                        selectedAllocations = selectedAllocations.map { alloc ->
                                                            if (alloc.id == item.id) alloc.copy(startDate = newDateStr) else alloc
                                                        }
                                                    },
                                                    cal.get(Calendar.YEAR),
                                                    cal.get(Calendar.MONTH),
                                                    cal.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "Change Start Date",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Starts: $itemDateFormatted",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // If Direct, show Nights stepper and Same Price Switch side by side
                                    val itemNights = if (platform == "Direct") item.nights else bookingNights
                                    val itemSamePrice = if (platform == "Direct") item.samePrice else samePriceForAllNights
                                    val ratesList = item.rates
                                    
                                    if (platform == "Direct") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("Nights:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                IconButton(
                                                    onClick = {
                                                        if (item.nights > 1) {
                                                            val newN = item.nights - 1
                                                            selectedAllocations = selectedAllocations.map {
                                                                if (it.id == item.id) {
                                                                    val newRates = adjustRatesList(it.rates, if (it.samePrice) 1 else newN)
                                                                    it.copy(nights = newN, rates = newRates)
                                                                } else it
                                                            }
                                                        }
                                                    },
                                                    enabled = item.nights > 1,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                                }
                                                Text(item.nights.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                IconButton(
                                                    onClick = {
                                                        val newN = item.nights + 1
                                                        selectedAllocations = selectedAllocations.map {
                                                            if (it.id == item.id) {
                                                                val newRates = adjustRatesList(it.rates, if (it.samePrice) 1 else newN)
                                                                it.copy(nights = newN, rates = newRates)
                                                            } else it
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("Same price:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Switch(
                                                    checked = item.samePrice,
                                                    onCheckedChange = { checked ->
                                                        selectedAllocations = selectedAllocations.map {
                                                            if (it.id == item.id) {
                                                                val newRates = adjustRatesList(it.rates, if (checked) 1 else it.nights)
                                                                it.copy(samePrice = checked, rates = newRates)
                                                            } else it
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Rate fields rendering
                                    if (itemSamePrice) {
                                        // 1 Rate field
                                        OutlinedTextField(
                                            value = item.rate,
                                            onValueChange = { newVal ->
                                                selectedAllocations = selectedAllocations.map {
                                                    if (it.id == item.id) {
                                                        val updatedRates = if (it.rates.isEmpty()) mutableListOf(newVal) else it.rates.toMutableList().apply { this[0] = newVal }
                                                        it.copy(rate = newVal, rates = updatedRates)
                                                    } else it
                                                }
                                            },
                                            placeholder = { Text(if (item.category == "Dorm Bed") "Total Dorm Beds Price (All Nights & Beds)" else "Room Rate per Night") },
                                            prefix = { Text("₹", fontSize = 12.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                        )
                                    } else {
                                        // N Rate fields
                                        Text(if (item.category == "Dorm Bed") "Enter total dorm price (all beds) for each night:" else "Enter rate for each night:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            for (nightIndex in 0 until itemNights) {
                                                val rateValue = ratesList.getOrNull(nightIndex) ?: ""
                                                OutlinedTextField(
                                                    value = rateValue,
                                                    onValueChange = { newVal ->
                                                        selectedAllocations = selectedAllocations.map {
                                                            if (it.id == item.id) {
                                                                val updatedRates = adjustRatesList(it.rates, itemNights).toMutableList()
                                                                updatedRates[nightIndex] = newVal
                                                                it.copy(rates = updatedRates)
                                                            } else it
                                                        }
                                                    },
                                                    placeholder = { Text(if (item.category == "Dorm Bed") "Night ${nightIndex + 1} Total Dorm Price (All Beds)" else "Night ${nightIndex + 1} Rate") },
                                                    prefix = { Text("₹", fontSize = 12.sp) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Button to add new item
                        Button(
                            onClick = {
                                val targetSize = if (samePriceForAllNights) 1 else bookingNights
                                selectedAllocations = selectedAllocations + BookingAllocationInput(
                                    nights = bookingNights,
                                    samePrice = samePriceForAllNights,
                                    rates = adjustRatesList(emptyList(), targetSize)
                                )
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
                if (platform == "Direct") {
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
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().clickable { advancePaymentIsUnknown = !advancePaymentIsUnknown }
                                ) {
                                    Checkbox(
                                        checked = advancePaymentIsUnknown,
                                        onCheckedChange = { advancePaymentIsUnknown = it }
                                    )
                                    Text(
                                        text = "Unknown Advance Payment (Date & Mode do not matter)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!advancePaymentIsUnknown) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Advance Payment Method", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    val methodList = listOf(
                                        "UPI (Hotel Acc - GPay)",
                                        "UPI (Sparsh Acc - GPay)",
                                        "UPI (Meenu - PhonePe)",
                                        "UPI (Shop - HDFC)",
                                        "Cash",
                                        "Card",
                                        "Bank Transfer"
                                    )
                                    methodList.chunked(3).forEach { rowMethods ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rowMethods.forEach { method ->
                                                val isSel = advancePaymentMethod == method
                                                ElevatedCard(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(28.dp)
                                                        .clickable { advancePaymentMethod = method },
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = method,
                                                            fontSize = 8.sp,
                                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                            if (rowMethods.size < 3) {
                                                repeat(3 - rowMethods.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
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
                                    dialogPayments.sortedBy { it.timestamp }.forEach { p ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val pDateFormatted = if (p.timestamp == 0L) "Unknown Date" else SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(p.timestamp))
                                            val pMethodStr = if (p.method == "Unknown") "Unknown Mode" else p.method
                                            Text(
                                                text = "₹${formatDouble(p.amount)} via $pMethodStr on $pDateFormatted",
                                                fontSize = 12.sp,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            IconButton(
                                                onClick = {
                                                    val updatedPayments = dialogPayments.filter { it.id != p.id }
                                                    dialogPayments = updatedPayments
                                                    if (bookingToEdit != null) {
                                                        val updatedBooking = bookingToEdit.copy(
                                                            payments = updatedPayments
                                                        )
                                                        if (onSaveWithoutDismiss != null) {
                                                            onSaveWithoutDismiss(updatedBooking)
                                                        } else {
                                                            onConfirm(updatedBooking)
                                                        }
                                                    }
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
                                    Text("Total: ₹${formatDouble(totalBillValue)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Paid: ₹${formatDouble(totalPaidVal)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "Balance: ₹${formatDouble(balanceVal)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (balanceVal > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (balanceVal > 0.0) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Text("Record Additional Payment", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().clickable { newPaymentIsUnknown = !newPaymentIsUnknown }
                                    ) {
                                        Checkbox(
                                            checked = newPaymentIsUnknown,
                                            onCheckedChange = { newPaymentIsUnknown = it }
                                        )
                                        Text(
                                            text = "Unknown Payment (Date & Mode do not matter)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))

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
                                            modifier = Modifier.weight(if (newPaymentIsUnknown) 2.2f else 1.2f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )

                                        if (!newPaymentIsUnknown) {
                                            // Payment Date Selector
                                            val newDateFormatted = SimpleDateFormat("dd/MM", Locale.US).format(Date(newPaymentDate))
                                            OutlinedCard(
                                                modifier = Modifier.weight(0.7f).height(52.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().clickable {
                                                        val cal = Calendar.getInstance().apply { timeInMillis = newPaymentDate }
                                                        DatePickerDialog(
                                                            context,
                                                            { _, y, m, dOfMonth ->
                                                                val selectedCal = Calendar.getInstance()
                                                                selectedCal.set(y, m, dOfMonth)
                                                                newPaymentDate = selectedCal.timeInMillis
                                                            },
                                                            cal.get(Calendar.YEAR),
                                                            cal.get(Calendar.MONTH),
                                                            cal.get(Calendar.DAY_OF_MONTH)
                                                        ).show()
                                                    },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(newDateFormatted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val amt = newPaymentAmountStr.toDoubleOrNull() ?: 0.0
                                                if (amt > 0.0) {
                                                    val newPayment = PaymentDetail(
                                                        amount = amt,
                                                        method = if (newPaymentIsUnknown) "Unknown" else newPaymentMethod,
                                                        timestamp = if (newPaymentIsUnknown) 0L else newPaymentDate
                                                    )
                                                    val updatedPayments = dialogPayments + newPayment
                                                    dialogPayments = updatedPayments
                                                    newPaymentAmountStr = ""
                                                    newPaymentDate = System.currentTimeMillis() // Reset to current date
                                                    newPaymentIsUnknown = false // Reset checkbox
                                                    if (bookingToEdit != null) {
                                                        val updatedBooking = bookingToEdit.copy(
                                                            payments = updatedPayments
                                                        )
                                                        if (onSaveWithoutDismiss != null) {
                                                            onSaveWithoutDismiss(updatedBooking)
                                                        } else {
                                                            onConfirm(updatedBooking)
                                                        }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(52.dp)
                                        ) {
                                            Text("Record", fontSize = 11.sp)
                                        }
                                    }

                                    if (!newPaymentIsUnknown) {
                                        val methodList = listOf(
                                            "UPI (Hotel Acc - GPay)",
                                            "UPI (Sparsh Acc - GPay)",
                                            "UPI (Meenu - PhonePe)",
                                            "UPI (Shop - HDFC)",
                                            "Cash",
                                            "Card",
                                            "Bank Transfer"
                                        )
                                        methodList.chunked(3).forEach { rowMethods ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                rowMethods.forEach { method ->
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
                                                                fontSize = 8.sp,
                                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                                if (rowMethods.size < 3) {
                                                    repeat(3 - rowMethods.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
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

                // Custom Bill (if Direct)
                if (platform == "Direct") {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Custom Bill Total", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Different from sum of rates", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                                Switch(checked = isBillOn, onCheckedChange = { isBillOn = it })
                            }
                            if (isBillOn) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = billAmountStr,
                                    onValueChange = { billAmountStr = it },
                                    label = { Text("Custom Bill Amount") },
                                    placeholder = { Text("e.g. 2800") },
                                    prefix = { Text("₹ ") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
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
                    val validAllocations = selectedAllocations.all { alloc ->
                        val allocNights = if (platform == "Direct") alloc.nights else bookingNights
                        val allocSamePrice = if (platform == "Direct") alloc.samePrice else samePriceForAllNights
                        val ratesList = alloc.rates.map { it.toDoubleOrNull() ?: 0.0 }
                        val bedsVal = if (alloc.category == "Dorm Bed") alloc.dormBedsCount else 1
                        
                        if (allocSamePrice) {
                            val rateVal = alloc.rate.toDoubleOrNull() ?: 0.0
                            rateVal > 0.0 && bedsVal > 0
                        } else {
                            val hasInvalidRate = ratesList.any { it <= 0.0 } || ratesList.size < allocNights
                            !hasInvalidRate && bedsVal > 0
                        }
                    }
                    if (!validAllocations && selectedAllocations.isNotEmpty()) {
                        allocationsError = "Please enter valid rates and bed counts (> 0) for all allocations."
                        hasError = true
                    }

                    if (hasError) return@Button

                    // Build list of BookingItems
                    val itemsList = selectedAllocations.flatMap { alloc ->
                        val allocNights = if (platform == "Direct") alloc.nights else bookingNights
                        val allocSamePrice = if (platform == "Direct") alloc.samePrice else samePriceForAllNights
                        val ratesList = alloc.rates.map { it.toDoubleOrNull() ?: 0.0 }
                        
                        if (alloc.category == "Dorm Bed") {
                            val beds = alloc.dormBedsCount.coerceAtLeast(1)
                            val dormShareRatesList = if (allocSamePrice) {
                                val totalEntered = alloc.rate.toDoubleOrNull() ?: 0.0
                                val ratePerBedPerNight = totalEntered / (allocNights.coerceAtLeast(1) * beds)
                                List(allocNights) { ratePerBedPerNight }
                            } else {
                                ratesList.take(allocNights).map { it / beds }
                            }
                            
                            List(beds) {
                                BookingItem(
                                    id = UUID.randomUUID().toString(),
                                    category = "Dorm Bed",
                                    roomNumber = "",
                                    amount = dormShareRatesList.sum(),
                                    nights = allocNights,
                                    rates = dormShareRatesList,
                                    startDate = alloc.startDate.takeIf { !it.isNullOrBlank() } ?: checkInDate
                                )
                            }
                        } else {
                            val ratesPerNight = if (allocSamePrice) {
                                val ratePerNight = alloc.rate.toDoubleOrNull() ?: 0.0
                                List(allocNights) { ratePerNight }
                            } else {
                                ratesList.take(allocNights)
                            }
                            
                            listOf(
                                BookingItem(
                                    id = if (bookingToEdit != null && alloc.id.isNotEmpty()) alloc.id else UUID.randomUUID().toString(),
                                    category = alloc.category,
                                    roomNumber = "", // Unassigned initially
                                    amount = ratesPerNight.sum(),
                                    nights = allocNights,
                                    rates = ratesPerNight,
                                    startDate = alloc.startDate.takeIf { !it.isNullOrBlank() } ?: checkInDate
                                )
                            )
                        }
                    }

                    // Separate out dorm bed counts
                    val dormItems = itemsList.filter { it.category == "Dorm Bed" }
                    val dormTotalVal = dormItems.sumOf { it.amount }

                    val totalAmount = itemsList.sumOf { it.amount }

                    val finalBillAmount = if (platform == "Direct" && isBillOn) {
                        billAmountStr.toDoubleOrNull() ?: totalAmount
                    } else {
                        totalAmount
                    }

                    // Construct payments
                    val finalPayments = if (platform != "Direct") {
                        listOf(
                            PaymentDetail(
                                amount = finalBillAmount,
                                method = "UPI (Hotel Acc - GPay)"
                            )
                        )
                    } else if (bookingToEdit != null) {
                        dialogPayments
                    } else {
                        val initialPayments = mutableListOf<PaymentDetail>()
                        val advVal = advancePaymentStr.toDoubleOrNull() ?: 0.0
                        if (advVal > 0.0) {
                            initialPayments.add(
                                PaymentDetail(
                                    amount = advVal,
                                    method = if (advancePaymentIsUnknown) "Unknown" else advancePaymentMethod,
                                    timestamp = if (advancePaymentIsUnknown) 0L else System.currentTimeMillis()
                                )
                            )
                        }
                        initialPayments
                    }

                    val newBooking = Booking(
                        id = bookingToEdit?.id ?: UUID.randomUUID().toString(),
                        checkInDate = checkInDate,
                        platform = platform,
                        guestName = guestName.trim(),
                        items = itemsList,
                        dormBedsSelected = dormItems.size,
                        dormTotalAmount = dormTotalVal,
                        isBillOn = if (platform == "Direct") isBillOn else true,
                        billAmount = finalBillAmount,
                        expenses = if (platform != "Direct") {
                            com.sparsh.myapplication.SettingsManager.calculateBreakdown(context, platform, finalBillAmount).totalDeductions
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

    val counts = remember(dormRoom, bookings, booking.checkInDate, booking.id, dormItems) {
        val dormItem = dormItems.firstOrNull()
        val dormStartDate = dormItem?.startDate.takeIf { !it.isNullOrBlank() } ?: booking.checkInDate
        val maxDormNights = dormItems.map { it.nights }.maxOrNull() ?: 1
        getDormBedBookingCounts(dormStartDate, maxDormNights, dormRoom, bookings, booking.id)
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
        val newAssignments = mutableMapOf<String, String>()
        booking.items.filter { it.category != "Dorm Bed" }.forEach { item ->
            for (nightIndex in 0 until item.nights) {
                newAssignments["${item.id}_$nightIndex"] = com.sparsh.myapplication.getRoomNumberForNight(item.roomNumber, nightIndex)
            }
        }
        assignments = newAssignments
        
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
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .navigationBarsPadding()
            .imePadding(),
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Requested Room: $category (${item.nights} nights)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (nightIndex in 0 until item.nights) {
                                    androidx.compose.runtime.key(item.id, nightIndex) {
                                        val itemStartDate = item.startDate.takeIf { !it.isNullOrBlank() } ?: booking.checkInDate
                                        val stayDate = com.sparsh.myapplication.getStayDate(itemStartDate, nightIndex)
                                        val assignedRoom = assignments["${item.id}_$nightIndex"] ?: ""

                                        val allRoomsForCat = getRoomsForCategory(category)
                                        val roomItems = remember(allRoomsForCat, bookings, assignments, stayDate) {
                                            allRoomsForCat.map { room ->
                                                val isBookedElsewhere = bookings.any { b ->
                                                    b.id != booking.id && b.items.any { bi ->
                                                        val otherItemStartDate = bi.startDate.takeIf { !it.isNullOrBlank() } ?: b.checkInDate
                                                        (0 until bi.nights).any { j ->
                                                            com.sparsh.myapplication.getStayDate(otherItemStartDate, j) == stayDate &&
                                                            com.sparsh.myapplication.getRoomNumberForNight(bi.roomNumber, j) == room
                                                        }
                                                    }
                                                }
                                                val isSelectedElsewhereThisBooking = assignments.any { (key, selectedRoom) ->
                                                    if (selectedRoom != room) return@any false
                                                    val parts = key.split("_")
                                                    if (parts.size < 2) return@any false
                                                    val otherItemId = parts[0]
                                                    val otherNightIndex = parts[1].toIntOrNull() ?: 0
                                                    val otherItem = booking.items.firstOrNull { it.id == otherItemId }
                                                    val otherItemStartDate = otherItem?.startDate.takeIf { !it.isNullOrBlank() } ?: booking.checkInDate
                                                    val otherStayDate = com.sparsh.myapplication.getStayDate(otherItemStartDate, otherNightIndex)

                                                    otherStayDate == stayDate && otherItemId != item.id
                                                }
                                                val isOccupied = isBookedElsewhere || isSelectedElsewhereThisBooking
                                                Pair(room, isOccupied)
                                            }
                                        }

                                        var dropdownExpanded by remember { mutableStateOf(false) }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Night ${nightIndex + 1} (${stayDate}):",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )

                                            Box(modifier = Modifier.width(140.dp)) {
                                                OutlinedCard(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(36.dp)
                                                        .clickable { dropdownExpanded = true },
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (assignedRoom.isNotBlank()) "Room $assignedRoom" else "Select Room...",
                                                            fontSize = 12.sp,
                                                            fontWeight = if (assignedRoom.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (assignedRoom.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                        )
                                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                DropdownMenu(
                                                    expanded = dropdownExpanded,
                                                    onDismissRequest = { dropdownExpanded = false }
                                                ) {
                                                    if (roomItems.isEmpty()) {
                                                        DropdownMenuItem(
                                                            text = { Text("No rooms in category!") },
                                                            onClick = {},
                                                            enabled = false
                                                        )
                                                    } else {
                                                        roomItems.forEach { (room, isOccupied) ->
                                                            DropdownMenuItem(
                                                                text = { 
                                                                    Text(
                                                                        text = if (isOccupied) "Room $room (Occupied)" else "Room $room",
                                                                        color = if (isOccupied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                },
                                                                onClick = {
                                                                    assignments = assignments + ("${item.id}_$nightIndex" to room)
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
                        (0 until item.nights).all { nightIndex ->
                            assignments["${item.id}_$nightIndex"]?.isNotBlank() == true
                        }
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
                        val rooms = (0 until item.nights).map { nightIndex ->
                            assignments["${item.id}_$nightIndex"] ?: ""
                        }
                        item.copy(roomNumber = rooms.joinToString(","))
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
    var paymentMethod by remember { mutableStateOf("UPI (Hotel Acc - GPay)") }
    var paymentAmountError by remember { mutableStateOf<String?>(null) }
    var paymentIsUnknown by remember { mutableStateOf(false) }

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
                Text("Record a payment detail for ${booking.guestName}. Outstanding Balance: ₹${formatDouble(booking.balance)}")

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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { paymentIsUnknown = !paymentIsUnknown }
                ) {
                    Checkbox(
                        checked = paymentIsUnknown,
                        onCheckedChange = { paymentIsUnknown = it }
                    )
                    Text(
                        text = "Unknown Payment (Date & Mode do not matter)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!paymentIsUnknown) {
                    Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val methodList = listOf(
                        "UPI (Hotel Acc - GPay)",
                        "UPI (Sparsh Acc - GPay)",
                        "UPI (Meenu - PhonePe)",
                        "UPI (Shop - HDFC)",
                        "Cash",
                        "Card",
                        "Bank Transfer"
                    )
                    methodList.chunked(3).forEach { rowMethods ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowMethods.forEach { method ->
                                val isSel = paymentMethod == method
                                ElevatedCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clickable { paymentMethod = method },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = method,
                                            fontSize = 8.sp,
                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (rowMethods.size < 3) {
                                repeat(3 - rowMethods.size) {
                                    Spacer(modifier = Modifier.weight(1f))
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
                    val amtVal = paymentAmountStr.toDoubleOrNull() ?: 0.0
                    if (amtVal <= 0.0) {
                        paymentAmountError = "Please enter a valid payment amount."
                        return@Button
                    }
                    if (amtVal > booking.balance) {
                        paymentAmountError = "Amount cannot exceed outstanding balance of ₹${formatDouble(booking.balance)}."
                        return@Button
                    }

                    // Create PaymentDetail and append
                    val newPayment = PaymentDetail(
                        amount = amtVal,
                        method = if (paymentIsUnknown) "Unknown" else paymentMethod,
                        timestamp = if (paymentIsUnknown) 0L else System.currentTimeMillis()
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
