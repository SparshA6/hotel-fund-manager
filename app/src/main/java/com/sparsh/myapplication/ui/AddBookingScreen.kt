package com.sparsh.myapplication.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.sparsh.myapplication.getRoomCategory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

private fun formatDouble(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
    }
}

fun isStandardRoomCategory(category: String): Boolean {
    return category == "Room" || category == "Standard" || category == "Deluxe" || 
           category == "Double" || category == "Family" || category == "Deluxe Family"
}

fun isDormCategory(category: String): Boolean {
    return category == "Dorm" || category == "Dorm Bed"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookingScreen(
    bookings: List<Booking> = emptyList(),
    onAddBooking: (Booking) -> Unit,
    onDeleteBooking: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val formBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    )

    var selectedViewMode by remember { mutableStateOf(0) } // 0 for Rooms, 1 for Dorms

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(formBackground)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Chart",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Toggle Switcher for Rooms vs Dorms Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf("Rooms Grid View", "Dorms Grid View")
            modes.forEachIndexed { index, modeTitle ->
                val isSelected = selectedViewMode == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { selectedViewMode = index }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = modeTitle,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        AddBookingGridView(
            bookings = bookings,
            onAddBooking = onAddBooking,
            onDeleteBooking = onDeleteBooking,
            isDormMode = (selectedViewMode == 1),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun CategoryItemRow(
    item: BookingItem,
    rateStr: String,
    onRoomNoChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var roomNoText by remember { mutableStateOf(item.roomNumber) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = roomNoText,
            onValueChange = {
                roomNoText = it
                onRoomNoChange(it)
            },
            placeholder = { Text("Room No.") },
            modifier = Modifier.weight(1.2f),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        OutlinedTextField(
            value = rateStr,
            onValueChange = { onRateChange(it) },
            placeholder = { Text("Rate") },
            prefix = { Text("₹") },
            modifier = Modifier.weight(1.5f),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        IconButton(
            onClick = onDelete,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete item",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun getPlatformColors(platform: String, isPending: Boolean): Pair<Color, Color> {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        if (isPending) {
            Pair(Color(0xFF5D4037), Color(0xFFFFCC80)) // Dark brown/orange, light orange text
        } else {
            when (platform) {
                "Direct" -> Pair(Color(0xFF1A237E), Color(0xFFC5CAE9))      // Dark Indigo, light Indigo text
                "MMT" -> Pair(Color(0xFF1B5E20), Color(0xFFC8E6C9))         // Dark Green
                "Booking.com" -> Pair(Color(0xFF0D47A1), Color(0xFFBBDEFB)) // Dark Blue
                "Agoda" -> Pair(Color(0xFF4A148C), Color(0xFFE1BEE7))       // Dark Purple
                "Goibibo" -> Pair(Color(0xFFE65100), Color(0xFFFFCC80))     // Dark Orange
                "Cleartrip" -> Pair(Color(0xFFF57F17), Color(0xFFFFE082))   // Dark Yellow
                else -> Pair(Color(0xFF263238), Color(0xFFCFD8DC))          // Dark Grey
            }
        }
    } else {
        if (isPending) {
            Pair(Color(0xFFFFF3E0), Color(0xFFE65100)) // Light orange, dark orange text
        } else {
            when (platform) {
                "Direct" -> Pair(Color(0xFFE8EAF6), Color(0xFF1A237E))      // Light Indigo, dark Indigo text
                "MMT" -> Pair(Color(0xFFE8F5E9), Color(0xFF1B5E20))         // Light Green
                "Booking.com" -> Pair(Color(0xFFE3F2FD), Color(0xFF0D47A1)) // Light Blue
                "Agoda" -> Pair(Color(0xFFF3E5F5), Color(0xFF4A148C))       // Light Purple
                "Goibibo" -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))     // Light Orange
                "Cleartrip" -> Pair(Color(0xFFFFFDE7), Color(0xFFF57F17))   // Light Yellow
                else -> Pair(Color(0xFFECEFF1), Color(0xFF263238))          // Light Grey
            }
        }
    }
}

@Composable
fun AddBookingGridView(
    bookings: List<Booking>,
    onAddBooking: (Booking) -> Unit,
    onDeleteBooking: (String) -> Unit,
    isDormMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
 
    val bookingsLookup = remember(bookings) {
        val map = mutableMapOf<String, MutableMap<String, MutableList<Booking>>>()
        bookings.filter { it.isAssigned }.forEach { booking ->
            val date = booking.checkInDate
            booking.items.forEach { item ->
                val normalizedCat = if (isDormCategory(item.category)) "Dorm" else "Room"
                val key = "$normalizedCat:${item.roomNumber}"
                map.getOrPut(date) { mutableMapOf() }
                   .getOrPut(key) { mutableListOf() }
                   .add(booking)
            }
        }
        map
    }

    val daysInMonth = remember(currentYear, currentMonth) {
        val tempCal = Calendar.getInstance()
        tempCal.set(Calendar.YEAR, currentYear)
        tempCal.set(Calendar.MONTH, currentMonth)
        tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val columns = remember(isDormMode) {
        val list = mutableListOf<String>()
        if (isDormMode) {
            // Beds A1 to A8
            for (i in 1..8) list.add("A$i")
            // Beds B1 to B8
            for (i in 1..8) list.add("B$i")
        } else {
            // 1st Floor: 101, 102, 103, 104I, 104II, 105, 106
            list.addAll(listOf("101", "102", "103", "104I", "104II", "105", "106"))
            // 2nd Floor: 201, 202, 203, 204I, 204II, 205, 206
            list.addAll(listOf("201", "202", "203", "204I", "204II", "205", "206"))
            // 3rd Floor: 301, 302, 303, 304I, 304II, 305, 306
            list.addAll(listOf("301", "302", "303", "304I", "304II", "305", "306"))
        }
        list
    }

    val dateColWidth = 65.dp
    val roomColWidth = 85.dp
    val rowHeight = 52.dp
    val headerHeight = 44.dp

    val horizontalScrollState = rememberScrollState()
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = currentDay - 1)

    var selectedCellDate by remember { mutableStateOf<String?>(null) }
    var selectedCellRoom by remember { mutableStateOf<String?>(null) }

    var selectedDateForBooking by remember { mutableStateOf<String?>(null) }
    var selectedRoomForBooking by remember { mutableStateOf<String?>(null) }
    var bookingToEditInDialog by remember { mutableStateOf<Booking?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Month Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val displayMonthStr = remember(currentYear, currentMonth) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, currentYear)
                cal.set(Calendar.MONTH, currentMonth)
                monthFormat.format(cal.time)
            }

            IconButton(onClick = {
                if (currentMonth == 0) {
                    currentMonth = 11
                    currentYear -= 1
                } else {
                    currentMonth -= 1
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = displayMonthStr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(onClick = {
                if (currentMonth == 11) {
                    currentMonth = 0
                    currentYear += 1
                } else {
                    currentMonth += 1
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. STICKY HEADER ROW (Room/Bed numbers)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .border(
                            BorderStroke(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Label Column Header (Top Left corner cell)
                    Box(
                        modifier = Modifier
                            .width(dateColWidth)
                            .height(headerHeight)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Date",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Horizontal scroll of Room/Bed numbers
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(headerHeight)
                            .horizontalScroll(horizontalScrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        columns.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .width(roomColWidth)
                                    .fillMaxHeight()
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isDormMode) "Bed $col" else col,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // 2. SCROLLABLE ROWS (Days)
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(daysInMonth, key = { dayIndex -> dayIndex }) { dayIndex ->
                        val dayNum = dayIndex + 1

                        val cellCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, currentYear)
                            set(Calendar.MONTH, currentMonth)
                            set(Calendar.DAY_OF_MONTH, dayNum)
                        }

                        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val dateString = dateFormatter.format(cellCalendar.time)

                        val dayOfWeekFormatter = SimpleDateFormat("EEE", Locale.US)
                        val dayOfWeek = dayOfWeekFormatter.format(cellCalendar.time)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .border(
                                    width = 0.25.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Fixed Date Cell on Left
                            val isWeekend = dayOfWeek == "Sat" || dayOfWeek == "Sun"
                            Box(
                                modifier = Modifier
                                    .width(dateColWidth)
                                    .fillMaxHeight()
                                    .background(
                                        if (isWeekend) MaterialTheme.colorScheme.secondaryContainer.copy(
                                            alpha = 0.3f
                                        ) else Color.Transparent
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNum",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isWeekend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = dayOfWeek,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            // Horizontally Scrollable Room/Bed Booking Cells on Right
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .horizontalScroll(horizontalScrollState),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                columns.forEach { col ->
                                    val cellKey = if (isDormMode) "Dorm:$col" else "Room:$col"
                                    val bookingsForCell = bookingsLookup[dateString]?.get(cellKey) ?: emptyList()

                                    Box(
                                        modifier = Modifier
                                            .width(roomColWidth)
                                            .fillMaxHeight()
                                            .border(
                                                width = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )
                                            .clickable {
                                                if (bookingsForCell.isEmpty()) {
                                                    selectedDateForBooking = dateString
                                                    selectedRoomForBooking = col
                                                    bookingToEditInDialog = null
                                                } else {
                                                    selectedCellDate = dateString
                                                    selectedCellRoom = col
                                                }
                                            }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bookingsForCell.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(1.dp)
                                            ) {
                bookingsForCell.forEach { booking ->
                                                    val bookingItem = booking.items.find { 
                                                        val isItemDorm = isDormCategory(it.category)
                                                        it.roomNumber == col && isItemDorm == isDormMode
                                                    }
                                                    if (bookingItem != null) {
                                                        val isPending = booking.paymentStatus == "Pending"
                                                        val colors = getPlatformColors(booking.platform, isPending)
                                                        val hasBalance = booking.balance > 0.0

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(colors.first)
                                                                .run {
                                                                    if (hasBalance) {
                                                                        border(
                                                                            width = 1.5.dp,
                                                                            color = MaterialTheme.colorScheme.error,
                                                                            shape = RoundedCornerShape(4.dp)
                                                                        )
                                                                    } else {
                                                                        this
                                                                    }
                                                                }
                                                                .padding(horizontal = 2.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "₹${formatDouble(bookingItem.amount)}",
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = if (bookingsForCell.size > 1) 9.sp else 11.sp,
                                                                color = colors.second,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Empty cell
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "+",
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
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
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Dialogs
    if (selectedCellDate != null && selectedCellRoom != null) {
        val cellKey = if (isDormMode) "Dorm:$selectedCellRoom" else "Room:$selectedCellRoom"
        val bookingsForCell = bookingsLookup[selectedCellDate]?.get(cellKey) ?: emptyList()
        CellBookingsDialog(
            date = selectedCellDate!!,
            roomNumber = selectedCellRoom!!,
            bookingsForCell = bookingsForCell,
            isDormMode = isDormMode,
            onDismiss = {
                selectedCellDate = null
                selectedCellRoom = null
            },
            onAddBookingClick = {
                selectedDateForBooking = selectedCellDate
                selectedRoomForBooking = selectedCellRoom
                bookingToEditInDialog = null
                selectedCellDate = null
                selectedCellRoom = null
            },
            onEditBookingClick = { booking ->
                selectedDateForBooking = selectedCellDate
                selectedRoomForBooking = selectedCellRoom
                bookingToEditInDialog = booking
                selectedCellDate = null
                selectedCellRoom = null
            }
        )
    }

    if (selectedDateForBooking != null && selectedRoomForBooking != null) {
        QuickBookDialog(
            date = selectedDateForBooking!!,
            roomNumber = selectedRoomForBooking!!,
            bookings = bookings,
            bookingToEdit = bookingToEditInDialog,
            isDormMode = isDormMode,
            onDismiss = {
                selectedDateForBooking = null
                selectedRoomForBooking = null
                bookingToEditInDialog = null
            },
            onConfirm = { newBooking ->
                onAddBooking(newBooking)
                selectedDateForBooking = null
                selectedRoomForBooking = null
                bookingToEditInDialog = null
            },
            onDelete = onDeleteBooking
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickBookDialog(
    date: String,
    roomNumber: String,
    bookings: List<Booking> = emptyList(),
    bookingToEdit: Booking? = null,
    isDormMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var guestName by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("Direct") }
    var isBillOn by remember { mutableStateOf(false) }
    var billAmountStr by remember { mutableStateOf("") }
    var expensesStr by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf("Paid") }
    var paymentMethod by remember { mutableStateOf("UPI (Hotel Acc - GPay)") }
    var notes by remember { mutableStateOf("") }
 
    var dialogPayments by remember { mutableStateOf(listOf<PaymentDetail>()) }
    var advancePaymentStr by remember { mutableStateOf("") }
    var advancePaymentMethod by remember { mutableStateOf("UPI (Hotel Acc - GPay)") }
    var newPaymentAmountStr by remember { mutableStateOf("") }
    var newPaymentMethod by remember { mutableStateOf("UPI (Hotel Acc - GPay)") }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var dialogRoomItems by remember { mutableStateOf(listOf<BookingItem>()) }
    var dialogItemRatesMap by remember { mutableStateOf(mapOf<String, String>()) }
    var totalDormPriceStr by remember { mutableStateOf("") }

    var hasDormBooking by remember { mutableStateOf(false) }
    var dormRoom by remember { mutableStateOf("A") }
    var dormBedCount by remember { mutableStateOf(1) }
    var dormBedCountStr by remember { mutableStateOf("1") }
    var manualBedNoToggle by remember { mutableStateOf(false) }
    var manualBedNoText by remember { mutableStateOf("") }

    var guestNameError by remember { mutableStateOf<String?>(null) }
    var roomNoErrors by remember { mutableStateOf(mapOf<String, String>()) }
    var roomRateErrors by remember { mutableStateOf(mapOf<String, String>()) }
    var dormBedCountError by remember { mutableStateOf<String?>(null) }
    var dormTotalAmountError by remember { mutableStateOf<String?>(null) }
    var dormBedsError by remember { mutableStateOf<String?>(null) }
    var customBillError by remember { mutableStateOf<String?>(null) }
    var expensesError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val dialogLazyListState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(bookingToEdit, roomNumber) {
        if (bookingToEdit != null) {
            guestName = bookingToEdit.guestName
            platform = bookingToEdit.platform
            isBillOn = bookingToEdit.isBillOn
            billAmountStr = if (bookingToEdit.billAmount == 0.0) "" else formatDouble(bookingToEdit.billAmount)
            expensesStr = if (bookingToEdit.expenses == 0.0) "" else formatDouble(bookingToEdit.expenses)
            paymentStatus = bookingToEdit.paymentStatus
            paymentMethod = bookingToEdit.paymentMethod
            notes = bookingToEdit.notes
 
            advancePaymentStr = ""
            advancePaymentMethod = "UPI (Hotel Acc - GPay)"
            newPaymentAmountStr = ""
            newPaymentMethod = "UPI (Hotel Acc - GPay)"

            // Populate Room category items
            dialogRoomItems = bookingToEdit.items.filter { isStandardRoomCategory(it.category) }
            dialogItemRatesMap = dialogRoomItems.associate { it.id to (if (it.amount == 0.0) "" else formatDouble(it.amount)) }

            // Initialize total dorm price if there are dorm items
            val dormItems = bookingToEdit.items.filter { isDormCategory(it.category) }
            val dormSum = dormItems.sumOf { it.amount }
            totalDormPriceStr = if (dormSum == 0.0) "" else formatDouble(dormSum)

            if (dormItems.isNotEmpty()) {
                hasDormBooking = true
                val firstDormItem = dormItems.firstOrNull { it.roomNumber.isNotBlank() }
                dormRoom = if (firstDormItem != null && firstDormItem.roomNumber.startsWith("B", ignoreCase = true)) "B" else "A"
                dormBedCount = dormItems.size
                dormBedCountStr = dormItems.size.toString()
                manualBedNoToggle = dormItems.any { it.roomNumber.isNotBlank() }
                manualBedNoText = dormItems.map { it.roomNumber }
                    .filter { it.startsWith(dormRoom, ignoreCase = true) && it.length > dormRoom.length }
                    .map { it.substring(dormRoom.length) }
                    .sorted()
                    .joinToString(", ")
            } else {
                hasDormBooking = false
                dormRoom = if (isDormMode && roomNumber.isNotBlank()) {
                    if (roomNumber.startsWith("B", ignoreCase = true)) "B" else "A"
                } else {
                    "A"
                }
                dormBedCount = 1
                dormBedCountStr = "1"
                manualBedNoToggle = false
                manualBedNoText = ""
            }
        } else {
            // New booking
            guestName = ""
            platform = "Direct"
            isBillOn = false
            billAmountStr = ""
            expensesStr = ""
            paymentStatus = "Paid"
            paymentMethod = "UPI (Hotel Acc - GPay)"
            notes = ""
 
            dialogPayments = emptyList()
            advancePaymentStr = ""
            advancePaymentMethod = "UPI (Hotel Acc - GPay)"
            newPaymentAmountStr = ""
            newPaymentMethod = "UPI (Hotel Acc - GPay)"

            // Initialize with the clicked room/bed
            if (isDormMode) {
                hasDormBooking = true
                dormRoom = if (roomNumber.startsWith("B", ignoreCase = true)) "B" else "A"
                dormBedCount = 1
                dormBedCountStr = "1"
                manualBedNoToggle = false
                manualBedNoText = roomNumber.substring(1)
                dialogRoomItems = emptyList()
                dialogItemRatesMap = emptyMap()
            } else {
                hasDormBooking = false
                dormRoom = "A"
                dormBedCount = 1
                dormBedCountStr = "1"
                manualBedNoToggle = false
                manualBedNoText = ""

                val initialItem = BookingItem(
                    id = UUID.randomUUID().toString(),
                    category = "Room",
                    roomNumber = roomNumber,
                    amount = 0.0
                )
                dialogRoomItems = listOf(initialItem)
                dialogItemRatesMap = mapOf(initialItem.id to "")
            }
            totalDormPriceStr = ""
        }
    }

    val displayDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("dd/MM", Locale.US)
        val d = parser.parse(date)
        if (d != null) formatter.format(d) else date
    } catch (e: Exception) {
        date
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.98f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                text = if (bookingToEdit != null) {
                    if (isDormMode) "Edit Booking (Bed $roomNumber)" else "Edit Booking ($roomNumber)"
                } else {
                    if (isDormMode) "Book Bed $roomNumber on $displayDate" else "Book $roomNumber on $displayDate"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            LazyColumn(
                state = dialogLazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 650.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showError) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                // Platform Selection
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Guest Name
                item {
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { 
                            guestName = it 
                            if (it.trim().isNotEmpty()) guestNameError = null
                        },
                        isError = guestNameError != null,
                        supportingText = guestNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        label = { Text("Guest Name" + if (platform != "Direct" || isBillOn) " (Required)" else "") },
                        placeholder = { Text("e.g. Amit Patel") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Total Dorm Price (if there is any Dorm item allocated)
                val totalDormVal = totalDormPriceStr.toDoubleOrNull() ?: 0.0
                val dormShareRate = if (dormBedCount > 0) totalDormVal / dormBedCount else 0.0

                // Rooms list allocation fields
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Allocated Rooms",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        val newItem = BookingItem(
                                            id = UUID.randomUUID().toString(),
                                            category = "Room",
                                            roomNumber = "",
                                            amount = 0.0
                                        )
                                        dialogRoomItems = dialogRoomItems + newItem
                                        dialogItemRatesMap = dialogItemRatesMap + (newItem.id to "")
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("+ Add Room", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (!hasDormBooking) {
                                    TextButton(
                                        onClick = {
                                            hasDormBooking = true
                                            dormBedCount = 1
                                            dormBedCountStr = "1"
                                            dormRoom = "A"
                                            manualBedNoToggle = false
                                            manualBedNoText = ""
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("+ Add Dorm Beds", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        dialogRoomItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Badge (Room)
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.width(60.dp)
                                ) {
                                    Text(
                                        text = "Room",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Room Number field
                                val hasNoErr = roomNoErrors[item.id]
                                OutlinedTextField(
                                    value = item.roomNumber,
                                    onValueChange = { newRoomNo ->
                                        val index = dialogRoomItems.indexOf(item)
                                        if (index != -1) {
                                            val newList = dialogRoomItems.toMutableList()
                                            newList[index] = item.copy(roomNumber = newRoomNo)
                                            dialogRoomItems = newList
                                        }
                                        if (newRoomNo.trim().isNotEmpty()) {
                                            roomNoErrors = roomNoErrors - item.id
                                        }
                                    },
                                    isError = hasNoErr != null,
                                    supportingText = hasNoErr?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                    placeholder = { Text("Room No", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                // Rate field
                                val currentRateStr = dialogItemRatesMap[item.id] ?: ""
                                val hasRateErr = roomRateErrors[item.id]
                                OutlinedTextField(
                                    value = currentRateStr,
                                    onValueChange = { newRate ->
                                        dialogItemRatesMap = dialogItemRatesMap + (item.id to newRate)
                                        val index = dialogRoomItems.indexOf(item)
                                        if (index != -1) {
                                            val newList = dialogRoomItems.toMutableList()
                                            newList[index] = item.copy(amount = newRate.toDoubleOrNull() ?: 0.0)
                                            dialogRoomItems = newList
                                        }
                                        if ((newRate.toDoubleOrNull() ?: 0.0) > 0.0) {
                                            roomRateErrors = roomRateErrors - item.id
                                        }
                                    },
                                    isError = hasRateErr != null,
                                    supportingText = hasRateErr?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                    placeholder = { Text("Rate", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    prefix = { Text("₹") },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                // Delete button (show if there's more than 1 item combined)
                                val canDeleteRoom = (dialogRoomItems.size > 1) || hasDormBooking
                                if (canDeleteRoom) {
                                    IconButton(
                                        onClick = {
                                            dialogRoomItems = dialogRoomItems.filter { it.id != item.id }
                                            dialogItemRatesMap = dialogItemRatesMap - item.id
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Room",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (hasDormBooking) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header: Dorm Beds Booking & Delete button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Dorm Beds Booking",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (dialogRoomItems.isNotEmpty()) {
                                        IconButton(
                                            onClick = { hasDormBooking = false },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Dorm Booking",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // 1. Dorm Room Option A or B
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Select Dorm Room",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("A", "B").forEach { room ->
                                            val isSelected = dormRoom == room
                                            ElevatedCard(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clickable { dormRoom = room },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "Dorm Room $room",
                                                        fontSize = 11.sp,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. Number of beds selector (OutlinedTextField with keyboard & increase/decrease arrows)
                                OutlinedTextField(
                                    value = dormBedCountStr,
                                    onValueChange = { newValue ->
                                        dormBedCountStr = newValue
                                        val num = newValue.toIntOrNull()
                                        if (num != null && num in 1..8) {
                                            dormBedCount = num
                                            dormBedCountError = null
                                        }
                                    },
                                    isError = dormBedCountError != null,
                                    supportingText = dormBedCountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                    label = { Text("Number of Beds") },
                                    placeholder = { Text("1-8") },
                                    prefix = { Text("₹${formatDouble(dormShareRate)} each | ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (dormBedCount > 1) {
                                                        dormBedCount--
                                                        dormBedCountStr = dormBedCount.toString()
                                                    }
                                                },
                                                enabled = dormBedCount > 1,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Decrease"
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (dormBedCount < 8) {
                                                        dormBedCount++
                                                        dormBedCountStr = dormBedCount.toString()
                                                    }
                                                },
                                                enabled = dormBedCount < 8,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Increase"
                                                )
                                            }
                                        }
                                    }
                                )

                                // 3. Toggle for Manual Bed Numbers
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Fill bed numbers manually",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "e.g. 1,2,3,4 or 1-4 or 1,2,4-7",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Switch(
                                        checked = manualBedNoToggle,
                                        onCheckedChange = { manualBedNoToggle = it }
                                    )
                                }

                                // 4. Manual Bed Numbers text field (if toggle active)
                                if (manualBedNoToggle) {
                                    OutlinedTextField(
                                        value = manualBedNoText,
                                        onValueChange = { 
                                            manualBedNoText = it 
                                            dormBedsError = null
                                        },
                                        isError = dormBedsError != null,
                                        supportingText = dormBedsError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                        label = { Text("Bed Numbers") },
                                        placeholder = { Text("e.g. 1-4, 6") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    
                                    // Live parsed count feedback
                                    val parsedBeds = remember(manualBedNoText, dormRoom) {
                                        parseBedNumbers(manualBedNoText, dormRoom)
                                    }
                                    if (parsedBeds.isNotEmpty()) {
                                        Text(
                                            text = "Parsed beds: ${parsedBeds.joinToString(", ") { it.substring(dormRoom.length) }} (${parsedBeds.size} beds)",
                                            fontSize = 11.sp,
                                            color = if (parsedBeds.size == dormBedCount) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (hasDormBooking) {
                    item {
                        OutlinedTextField(
                            value = totalDormPriceStr,
                            onValueChange = { 
                                totalDormPriceStr = it 
                                if ((it.toDoubleOrNull() ?: 0.0) > 0.0) dormTotalAmountError = null
                            },
                            isError = dormTotalAmountError != null,
                            supportingText = dormTotalAmountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            label = { Text("Total Dorm Price") },
                            placeholder = { Text("e.g. 1500") },
                            prefix = { Text("₹ ") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
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
                                    val hasBoth = dialogRoomItems.any { it.category == "Room" } && dialogRoomItems.any { it.category == "Dorm" }
                                    val descText = if (hasBoth) "Different from sum of room & bed rates" else if (isDormMode) "Different from sum of bed rates" else "Different from sum of room rates"
                                    Text(descText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                                Switch(checked = isBillOn, onCheckedChange = { isBillOn = it })
                            }
                            if (isBillOn) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = billAmountStr,
                                    onValueChange = { 
                                        billAmountStr = it 
                                        if ((it.toDoubleOrNull() ?: 0.0) > 0.0) customBillError = null
                                    },
                                    isError = customBillError != null,
                                    supportingText = customBillError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
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

                // Dynamic OTA Commission Calculation Display Label
                if (platform != "Direct") {
                    item {
                        val calculatedSum = (dialogRoomItems.map { it.amount }.sum() + (totalDormPriceStr.toDoubleOrNull() ?: 0.0))
                        val totalBillValue = if (platform == "Direct" && isBillOn) {
                            billAmountStr.toDoubleOrNull() ?: calculatedSum
                        } else {
                            calculatedSum
                        }
                        
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
                                
                                val formatMoney = { amt: Double -> "₹${String.format(java.util.Locale.US, "%.2f", amt)}" }
                                
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

                // Notes
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

                // Payments Details Section
                if (platform == "Direct") {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Payments Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val calculatedSum = (dialogRoomItems.map { it.amount }.sum() + (totalDormPriceStr.toDoubleOrNull() ?: 0.0))
                            
                            val totalBillValue = if (platform == "Direct" && isBillOn) {
                                billAmountStr.toDoubleOrNull() ?: calculatedSum
                            } else {
                                calculatedSum
                            }

                            if (bookingToEdit == null) {
                                // New Booking: Show Advance Payment
                                OutlinedTextField(
                                    value = advancePaymentStr,
                                    onValueChange = { advancePaymentStr = it },
                                    label = { Text("Advance Payment Amount") },
                                    placeholder = { Text("e.g. 1000 (0 for none)") },
                                    prefix = { Text("₹ ") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                
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
                                                text = "₹${formatDouble(p.amount)} via ${p.method}",
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
        },
        confirmButton = {
            Button(
                onClick = {
                    // Clear previous errors
                    guestNameError = null
                    val newRoomNoErrors = mutableMapOf<String, String>()
                    val newRoomRateErrors = mutableMapOf<String, String>()
                    roomNoErrors = emptyMap()
                    roomRateErrors = emptyMap()
                    dormBedCountError = null
                    dormBedsError = null
                    dormTotalAmountError = null
                    customBillError = null
                    expensesError = null
                    showError = false
                    errorMessage = ""

                    val scrollToField = { field: String ->
                        var idx = 0
                        if (showError) idx++ // Error Card (index 0)
                        
                        val targetIdx = when (field) {
                            "platform" -> idx
                            "guestName" -> idx + 1
                            "roomsList" -> idx + 2
                            "dormBeds" -> idx + 3
                            "dormTotalAmount" -> idx + 4
                            "customBill" -> {
                                var currentIdx = idx + 3
                                if (hasDormBooking) currentIdx += 2
                                currentIdx
                            }
                            else -> 0
                        }
                        coroutineScope.launch {
                            dialogLazyListState.animateScrollToItem(targetIdx)
                        }
                    }

                    // 1. Validate name if required
                    val nameRequired = (platform != "Direct") || (platform == "Direct" && isBillOn)
                    if (nameRequired && guestName.trim().isEmpty()) {
                        guestNameError = "Guest Name is required for this booking type."
                        errorMessage = "Guest Name is required for this booking type."
                        showError = true
                        scrollToField("guestName")
                        return@Button
                    }

                    // 2. Validate allocations (at least one room/bed)
                    if (dialogRoomItems.isEmpty() && !hasDormBooking) {
                        errorMessage = "Please allocate at least one room or bed."
                        showError = true
                        scrollToField("roomsList")
                        return@Button
                    }

                    // 3. Validate Room items details
                    dialogRoomItems.filter { it.category == "Room" }.forEach { item ->
                        if (item.roomNumber.trim().isEmpty()) {
                            newRoomNoErrors[item.id] = "Room number is required."
                        }
                        val rateVal = dialogItemRatesMap[item.id]?.toDoubleOrNull() ?: 0.0
                        if (rateVal <= 0.0) {
                            newRoomRateErrors[item.id] = "Rate must be greater than 0."
                        }
                    }
                    if (newRoomNoErrors.isNotEmpty() || newRoomRateErrors.isNotEmpty()) {
                        roomNoErrors = newRoomNoErrors
                        roomRateErrors = newRoomRateErrors
                        errorMessage = "Please enter valid room numbers and rates (> 0)."
                        showError = true
                        scrollToField("roomsList")
                        return@Button
                    }

                    // Validate Dorm items details
                    var parsedDormBeds = emptyList<String>()
                    if (hasDormBooking) {
                        val bedCountVal = dormBedCountStr.toIntOrNull()
                        if (bedCountVal == null || bedCountVal !in 1..8) {
                            dormBedCountError = "Please enter a valid number of beds (between 1 and 8)."
                            errorMessage = "Please enter a valid number of beds (between 1 and 8)."
                            showError = true
                            scrollToField("dormBeds")
                            return@Button
                        }
                        dormBedCount = bedCountVal

                        val totalDormVal = totalDormPriceStr.toDoubleOrNull()
                        if (totalDormVal == null || totalDormVal <= 0.0) {
                            dormTotalAmountError = "Please enter a valid total dorm price."
                            errorMessage = "Please enter a valid total dorm price."
                            showError = true
                            scrollToField("dormTotalAmount")
                            return@Button
                        }

                        if (manualBedNoToggle) {
                            if (manualBedNoText.trim().isEmpty()) {
                                dormBedsError = "Please enter bed numbers."
                                errorMessage = "Please enter bed numbers."
                                showError = true
                                scrollToField("dormBeds")
                                return@Button
                            }
                            parsedDormBeds = parseBedNumbers(manualBedNoText, dormRoom)
                            if (parsedDormBeds.isEmpty()) {
                                dormBedsError = "Please enter valid bed numbers (e.g. 1-4, 6)."
                                errorMessage = "Please enter valid bed numbers (e.g. 1-4, 6)."
                                showError = true
                                scrollToField("dormBeds")
                                return@Button
                            }
                            
                            // Validate indices are between 1 and 8
                            val invalidBed = parsedDormBeds.any { bed ->
                                val num = bed.substring(dormRoom.length).toIntOrNull()
                                num == null || num !in 1..8
                            }
                            if (invalidBed) {
                                dormBedsError = "Bed numbers must be between 1 and 8."
                                errorMessage = "Bed numbers must be between 1 and 8."
                                showError = true
                                scrollToField("dormBeds")
                                return@Button
                            }

                            if (parsedDormBeds.size != dormBedCount) {
                                dormBedsError = "Entered bed numbers count (${parsedDormBeds.size}) does not match selected bed count ($dormBedCount)."
                                errorMessage = "Entered bed numbers count (${parsedDormBeds.size}) does not match selected bed count ($dormBedCount)."
                                showError = true
                                scrollToField("dormBeds")
                                return@Button
                            }
                        } else {
                            // Automatic allocation
                            // Find free beds, or sort by bookings count
                            val counts = getDormBedBookingCounts(date, dormRoom, bookings, bookingToEdit?.id)
                            val freeBeds = (1..8).filter { (counts[it] ?: 0) == 0 }.sorted()
                            val bookedBeds = (1..8).filter { (counts[it] ?: 0) > 0 }.sortedWith(
                                compareBy( { counts[it] ?: 0 }, { it } )
                            )

                            val assignedBeds = mutableListOf<Int>()
                            if (freeBeds.size >= dormBedCount) {
                                assignedBeds.addAll(freeBeds.take(dormBedCount))
                            } else {
                                assignedBeds.addAll(freeBeds)
                                val remainingNeeded = dormBedCount - freeBeds.size
                                assignedBeds.addAll(bookedBeds.take(remainingNeeded))
                            }
                            parsedDormBeds = assignedBeds.sorted().map { "$dormRoom$it" }
                        }
                    }

                    // 4. Calculate final items allocation values
                    val totalDormVal = totalDormPriceStr.toDoubleOrNull() ?: 0.0
                    val dormShareRate = if (dormBedCount > 0) totalDormVal / dormBedCount else 0.0

                    // Build Dorm booking items list
                    val dormBookingItems = if (hasDormBooking) {
                        parsedDormBeds.map { bedNo ->
                            BookingItem(
                                id = UUID.randomUUID().toString(),
                                category = getRoomCategory(bedNo),
                                roomNumber = bedNo,
                                amount = dormShareRate
                            )
                        }
                    } else {
                        emptyList()
                    }

                    val roomBookingItems = dialogRoomItems.map { item ->
                        item.copy(
                            category = if (item.roomNumber.isNotBlank()) getRoomCategory(item.roomNumber) else item.category,
                            amount = dialogItemRatesMap[item.id]?.toDoubleOrNull() ?: 0.0
                        )
                    }

                    val updatedItems = roomBookingItems + dormBookingItems
                    val calculatedSum = updatedItems.sumOf { it.amount }

                    val finalBillAmount = if (platform == "Direct" && isBillOn) {
                        val customBill = billAmountStr.toDoubleOrNull()
                        if (customBill == null || customBill <= 0.0) {
                            customBillError = "Please enter a valid custom bill amount."
                            errorMessage = "Please enter a valid custom bill amount."
                            showError = true
                            scrollToField("customBill")
                            return@Button
                        }
                        customBill
                    } else {
                        calculatedSum
                    }

                    val commissionVal = if (platform != "Direct") {
                        com.sparsh.myapplication.SettingsManager.calculateBreakdown(context, platform, finalBillAmount).totalDeductions
                    } else {
                        0.0
                    }

                    // Create/Update Booking
                    val finalGuestName = if (platform == "Direct" && !isBillOn && guestName.trim().isEmpty()) "Direct Guest" else guestName.trim()

                    val dormRoomABedsStr = if (hasDormBooking && dormRoom == "A") {
                        if (manualBedNoToggle) manualBedNoText.trim() else parsedDormBeds.map { it.substring(1) }.joinToString(",")
                    } else ""

                    val dormRoomBBedsStr = if (hasDormBooking && dormRoom == "B") {
                        if (manualBedNoToggle) manualBedNoText.trim() else parsedDormBeds.map { it.substring(1) }.joinToString(",")
                    } else ""

                    val finalPayments = if (platform != "Direct") {
                        listOf(
                            PaymentDetail(
                                amount = finalBillAmount,
                                method = "UPI (Hotel Acc - GPay)"
                            )
                        )
                    } else if (bookingToEdit == null) {
                        val advVal = advancePaymentStr.toDoubleOrNull() ?: 0.0
                        if (advVal > 0.0) {
                            listOf(PaymentDetail(amount = advVal, method = advancePaymentMethod))
                        } else {
                            emptyList()
                        }
                    } else {
                        dialogPayments
                    }

                    val newBooking = Booking(
                        id = bookingToEdit?.id ?: UUID.randomUUID().toString(),
                        checkInDate = date,
                        platform = platform,
                        guestName = finalGuestName,
                        items = updatedItems,
                        dormBedsSelected = if (hasDormBooking) dormBedCount else 0,
                        dormRoomABeds = dormRoomABedsStr,
                        dormRoomBBeds = dormRoomBBedsStr,
                        dormTotalAmount = totalDormVal,
                        isBillOn = if (platform == "Direct") isBillOn else true,
                        billAmount = finalBillAmount,
                        expenses = commissionVal,
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bookingToEdit != null && onDelete != null) {
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete Booking", fontWeight = FontWeight.Bold) },
                            text = {
                                val itemsDescription = bookingToEdit.items.map { 
                                    if (isDormCategory(it.category)) "Bed ${it.roomNumber}" else it.roomNumber 
                                }.filter { it.isNotBlank() }
                                if (itemsDescription.size > 1) {
                                    Text("This booking contains multiple allocations: ${itemsDescription.joinToString(", ")}. Deleting it will remove the entire booking across all these allocations. Are you sure you want to delete this booking?")
                                } else {
                                    Text("Are you sure you want to delete this booking?")
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onDelete(bookingToEdit.id)
                                        showDeleteConfirm = false
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun CellBookingsDialog(
    date: String,
    roomNumber: String,
    bookingsForCell: List<Booking>,
    isDormMode: Boolean = false,
    onDismiss: () -> Unit,
    onAddBookingClick: () -> Unit,
    onEditBookingClick: (Booking) -> Unit
) {
    val displayDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("dd/MM", Locale.US)
        val d = parser.parse(date)
        if (d != null) formatter.format(d) else date
    } catch (e: Exception) {
        date
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.98f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                text = if (isDormMode) "Bookings for Bed $roomNumber on $displayDate" else "Bookings for $roomNumber on $displayDate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (bookingsForCell.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No bookings for this cell yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bookingsForCell) { booking ->
                            val bookingItem = booking.items.find { 
                                val isItemDorm = isDormCategory(it.category)
                                it.roomNumber == roomNumber && isItemDorm == isDormMode
                            }
                            val isPending = booking.paymentStatus == "Pending"
                            val colors = getPlatformColors(booking.platform, isPending)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditBookingClick(booking) },
                                colors = CardDefaults.cardColors(
                                    containerColor = colors.first
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, colors.second.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = booking.guestName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = colors.second
                                        )
                                        Text(
                                            text = "${booking.platform} • ${booking.paymentMethod}",
                                            fontSize = 11.sp,
                                            color = colors.second.copy(alpha = 0.7f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${bookingItem?.amount?.let { formatDouble(it) } ?: 0}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = colors.second
                                        )
                                        Text(
                                            text = booking.paymentStatus,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.second.copy(alpha = 0.8f)
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
            Button(
                onClick = onAddBookingClick
            ) {
                Text(if (isDormMode) "+ Add Bed Booking" else "+ Add Booking")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun parseBedNumbers(text: String, dormRoom: String): List<String> {
    val result = mutableListOf<String>()
    val parts = text.split(",")
    for (part in parts) {
        val trimmed = part.trim()
        if (trimmed.isEmpty()) continue
        if (trimmed.contains("-")) {
            val rangeParts = trimmed.split("-")
            if (rangeParts.size == 2) {
                val start = rangeParts[0].trim().toIntOrNull()
                val end = rangeParts[1].trim().toIntOrNull()
                if (start != null && end != null) {
                    val range = if (start <= end) start..end else end..start
                    for (num in range) {
                        result.add("$dormRoom$num")
                    }
                }
            }
        } else {
            val num = trimmed.toIntOrNull()
            if (num != null) {
                result.add("$dormRoom$num")
            }
        }
    }
    return result.distinct()
}

fun getDormBedBookingCounts(
    date: String,
    dormRoom: String,
    bookings: List<Booking>,
    excludeBookingId: String?
): Map<Int, Int> {
    val counts = mutableMapOf<Int, Int>()
    for (i in 1..8) {
        counts[i] = 0
    }
    
    val activeBookings = bookings.filter { b ->
        b.checkInDate == date && (excludeBookingId == null || b.id != excludeBookingId)
    }
    
    for (b in activeBookings) {
        for (item in b.items) {
            if (isDormCategory(item.category)) {
                val roomNum = item.roomNumber.trim()
                if (roomNum.startsWith(dormRoom, ignoreCase = true)) {
                    val bedIndexStr = roomNum.substring(dormRoom.length)
                    val bedIndex = bedIndexStr.toIntOrNull()
                    if (bedIndex != null && bedIndex in 1..8) {
                        counts[bedIndex] = (counts[bedIndex] ?: 0) + 1
                    }
                }
            }
        }
    }
    
    return counts
}

