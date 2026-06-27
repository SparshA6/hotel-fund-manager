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
import androidx.compose.material.icons.filled.Close
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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import android.net.Uri
import android.content.Intent
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import com.sparsh.myapplication.Booking
import com.sparsh.myapplication.BookingRepository
import com.sparsh.myapplication.BookingItem
import com.sparsh.myapplication.PaymentDetail
import com.sparsh.myapplication.GuestIdCard
import com.sparsh.myapplication.GuestIdImage
import com.sparsh.myapplication.getRoomCategory
import com.sparsh.myapplication.getRoomNumberForNight
import com.sparsh.myapplication.getRoomsForCategory
import com.sparsh.myapplication.getStayDate
import com.sparsh.myapplication.datesOverlap
import com.sparsh.myapplication.adjustRatesList
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch
import coil.compose.SubcomposeAsyncImage

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
    onDeleteBooking: (String, Boolean) -> Unit = { _, _ -> },
    bookingRepository: BookingRepository,
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
            bookingRepository = bookingRepository,
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
    onDeleteBooking: (String, Boolean) -> Unit,
    bookingRepository: BookingRepository,
    isDormMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
 
    val bookingsLookup = remember(bookings) {
        val map = mutableMapOf<String, MutableMap<String, MutableList<Booking>>>()
        bookings.filter { it.isAssigned }.forEach { booking ->
            booking.items.forEach { item ->
                val normalizedCat = if (isDormCategory(item.category)) "Dorm" else "Room"
                val nightsCount = if (item.nights > 0) item.nights else 1
                val itemStartDate = item.startDate.takeIf { !it.isNullOrBlank() } ?: booking.checkInDate
                for (offset in 0 until nightsCount) {
                    val stayDate = getStayDate(itemStartDate, offset)
                    val roomNo = com.sparsh.myapplication.getRoomNumberForNight(item.roomNumber, offset)
                    val key = "$normalizedCat:$roomNo"
                    map.getOrPut(stayDate) { mutableMapOf() }
                       .getOrPut(key) { mutableListOf() }
                       .add(booking)
                }
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
                                                        val itemStartDate = it.startDate.takeIf { !it.isNullOrBlank() } ?: booking.checkInDate
                                                        val offset = com.sparsh.myapplication.getDateOffset(itemStartDate, dateString)
                                                        val inStay = offset in 0 until it.nights
                                                        val itemRoom = if (inStay) com.sparsh.myapplication.getRoomNumberForNight(it.roomNumber, offset) else ""
                                                        itemRoom == col && isItemDorm == isDormMode
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
                                                            val itemStartDate = bookingItem.startDate.takeIf { !it.isNullOrBlank() } ?: booking.checkInDate
                                                            val offset = com.sparsh.myapplication.getDateOffset(itemStartDate, dateString)
                                                            val displayRate = if (bookingItem.rates.isNotEmpty()) {
                                                                bookingItem.rates.getOrNull(offset) ?: (bookingItem.amount / (if (bookingItem.nights > 0) bookingItem.nights else 1))
                                                            } else {
                                                                bookingItem.amount / (if (bookingItem.nights > 0) bookingItem.nights else 1)
                                                            }
                                                            Text(
                                                                text = "₹${formatDouble(displayRate)}",
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
                selectedDateForBooking = booking.checkInDate
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
            bookingRepository = bookingRepository,
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
            onDelete = onDeleteBooking,
            onSaveWithoutDismiss = { updatedBooking ->
                onAddBooking(updatedBooking)
                bookingToEditInDialog = updatedBooking
            }
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
    bookingRepository: BookingRepository,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit,
    onDelete: ((String, Boolean) -> Unit)? = null,
    onSaveWithoutDismiss: ((Booking) -> Unit)? = null
) {
    var guestName by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("Direct") }
    var selectedTab by remember { mutableStateOf(0) }
    var isBillOn by remember { mutableStateOf(false) }
    var billAmountStr by remember { mutableStateOf("") }
    var expensesStr by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf("Paid") }
    var paymentMethod by remember { mutableStateOf("UPI (Hotel Acc - GPay)") }
    var notes by remember { mutableStateOf("") }
    var discountStr by remember { mutableStateOf("") }
    var extraPriceStr by remember { mutableStateOf("") }
 
    var dialogPayments by remember { mutableStateOf(listOf<PaymentDetail>()) }
    var advancePaymentStr by remember { mutableStateOf("") }
    var advancePaymentMethod by remember { mutableStateOf("UPI (Hotel Acc - GPay)") }
    var advancePaymentDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var newPaymentAmountStr by remember { mutableStateOf("") }
    var newPaymentMethod by remember { mutableStateOf("UPI (Hotel Acc - GPay)") }
    var newPaymentDate by remember { mutableStateOf(System.currentTimeMillis()) }

    var bookingNights by remember { mutableStateOf(1) }
    var samePriceForAllNights by remember { mutableStateOf(true) }

    var itemNightsMap by remember { mutableStateOf(mapOf<String, Int>()) }
    var itemSamePriceMap by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var itemRatesMap by remember { mutableStateOf(mapOf<String, List<String>>()) }

    var dormNights by remember { mutableStateOf(1) }
    var dormSamePrice by remember { mutableStateOf(true) }
    var dormRates by remember { mutableStateOf(listOf("")) }
    var dormStartDate by remember { mutableStateOf<String?>(null) }
    var advancePaymentIsUnknown by remember { mutableStateOf(false) }
    var newPaymentIsUnknown by remember { mutableStateOf(false) }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var dialogRoomItems by remember { mutableStateOf(listOf<BookingItem>()) }
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
    var lastLoadedBookingId by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val dialogLazyListState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(bookingToEdit, roomNumber) {
        if (bookingToEdit != null) {
            if (bookingToEdit.id != lastLoadedBookingId) {
                lastLoadedBookingId = bookingToEdit.id
            guestName = bookingToEdit.guestName
            platform = bookingToEdit.platform
            isBillOn = bookingToEdit.isBillOn
            billAmountStr = if (bookingToEdit.billAmount == 0.0) "" else formatDouble(bookingToEdit.billAmount)
            expensesStr = if (bookingToEdit.expenses == 0.0) "" else formatDouble(bookingToEdit.expenses)
            paymentStatus = bookingToEdit.paymentStatus
            paymentMethod = bookingToEdit.paymentMethod
            notes = bookingToEdit.notes
            discountStr = if (bookingToEdit.discount == 0.0) "" else formatDouble(bookingToEdit.discount)
            extraPriceStr = if (bookingToEdit.extraPrice == 0.0) "" else formatDouble(bookingToEdit.extraPrice)
            dialogPayments = bookingToEdit.payments.filter { it.id != "portal_base" && it.method != "Portal (Auto)" }
 
            advancePaymentStr = ""
            advancePaymentMethod = "UPI (Hotel Acc - GPay)"
            advancePaymentDate = System.currentTimeMillis()
            advancePaymentIsUnknown = false
            newPaymentAmountStr = ""
            newPaymentMethod = "UPI (Hotel Acc - GPay)"
            newPaymentDate = System.currentTimeMillis()
            newPaymentIsUnknown = false

            val maxNights = bookingToEdit.items.map { it.nights }.maxOrNull() ?: 1
            bookingNights = maxNights
            samePriceForAllNights = bookingToEdit.items.all { item ->
                item.rates.distinct().size <= 1
            }

            // Populate Room category items
            dialogRoomItems = bookingToEdit.items.filter { isStandardRoomCategory(it.category) }
            
            val newNightsMap = mutableMapOf<String, Int>()
            val newSamePriceMap = mutableMapOf<String, Boolean>()
            val newRatesMap = mutableMapOf<String, List<String>>()
            dialogRoomItems.forEach { item ->
                val itNights = if (item.nights > 0) item.nights else 1
                newNightsMap[item.id] = itNights
                val allRatesEqual = item.rates.distinct().size <= 1
                newSamePriceMap[item.id] = allRatesEqual
                newRatesMap[item.id] = if (allRatesEqual) {
                    listOf(if (item.rates.isNotEmpty()) formatDouble(item.rates.first()) else (if (item.amount == 0.0) "" else formatDouble(item.amount / itNights)))
                } else {
                    item.rates.map { formatDouble(it) }
                }
            }
            itemNightsMap = newNightsMap
            itemSamePriceMap = newSamePriceMap
            itemRatesMap = newRatesMap

            // Initialize total dorm price if there are dorm items
            val dormItems = bookingToEdit.items.filter { isDormCategory(it.category) }
            if (dormItems.isNotEmpty()) {
                hasDormBooking = true
                val firstDormItem = dormItems.firstOrNull { it.roomNumber.isNotBlank() } ?: dormItems.first()
                dormRoom = if (firstDormItem.roomNumber.startsWith("B", ignoreCase = true)) "B" else "A"
                dormBedCount = dormItems.size
                dormBedCountStr = dormItems.size.toString()
                manualBedNoToggle = dormItems.any { it.roomNumber.isNotBlank() }
                manualBedNoText = dormItems.map { it.roomNumber }
                    .filter { it.startsWith(dormRoom, ignoreCase = true) && it.length > dormRoom.length }
                    .map { it.substring(dormRoom.length) }
                    .sorted()
                    .joinToString(", ")
                
                val firstDorm = dormItems.first()
                dormStartDate = firstDorm.startDate
                dormNights = if (firstDorm.nights > 0) firstDorm.nights else 1
                val allDormRatesEqual = firstDorm.rates.distinct().size <= 1
                dormSamePrice = allDormRatesEqual
                
                val dormSum = dormItems.sumOf { it.amount }
                dormRates = if (allDormRatesEqual) {
                    val dormAvgRate = if (firstDorm.rates.isNotEmpty()) {
                        firstDorm.rates.first()
                    } else {
                        val beds = dormItems.size.coerceAtLeast(1)
                        val nights = dormNights.coerceAtLeast(1)
                        dormSum / beds / nights
                    }
                    val totalDormVal = dormAvgRate * dormNights * dormItems.size
                    listOf(if (totalDormVal == 0.0) "" else formatDouble(totalDormVal))
                } else {
                    firstDorm.rates.map { formatDouble(it * dormItems.size) }
                }
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
                
                dormStartDate = null
                dormNights = 1
                dormSamePrice = true
                dormRates = listOf("")
            }
            } else {
                // Same booking, only update payments
                dialogPayments = bookingToEdit.payments.filter { it.id != "portal_base" && it.method != "Portal (Auto)" }
                paymentStatus = bookingToEdit.paymentStatus
                paymentMethod = bookingToEdit.paymentMethod
            }
        } else {
            lastLoadedBookingId = null
            // New booking
            guestName = ""
            platform = "Direct"
            isBillOn = false
            billAmountStr = ""
            expensesStr = ""
            paymentStatus = "Paid"
            paymentMethod = "UPI (Hotel Acc - GPay)"
            notes = ""
            discountStr = ""
            extraPriceStr = ""
 
            dialogPayments = emptyList()
            advancePaymentStr = ""
            advancePaymentMethod = "UPI (Hotel Acc - GPay)"
            advancePaymentIsUnknown = false
            newPaymentAmountStr = ""
            newPaymentMethod = "UPI (Hotel Acc - GPay)"
            newPaymentIsUnknown = false

            bookingNights = 1
            samePriceForAllNights = true

            // Initialize with the clicked room/bed
            if (isDormMode) {
                hasDormBooking = true
                dormRoom = if (roomNumber.startsWith("B", ignoreCase = true)) "B" else "A"
                dormBedCount = 1
                dormBedCountStr = "1"
                manualBedNoToggle = false
                manualBedNoText = roomNumber.substring(1)
                dialogRoomItems = emptyList()
                itemNightsMap = emptyMap()
                itemSamePriceMap = emptyMap()
                itemRatesMap = emptyMap()
                
                dormNights = 1
                dormSamePrice = true
                dormRates = listOf("")
            } else {
                hasDormBooking = false
                dormRoom = "A"
                dormBedCount = 1
                dormBedCountStr = "1"
                manualBedNoToggle = false
                manualBedNoText = ""
                
                dormNights = 1
                dormSamePrice = true
                dormRates = listOf("")

                val initialItem = BookingItem(
                    id = UUID.randomUUID().toString(),
                    category = "Room",
                    roomNumber = roomNumber,
                    amount = 0.0,
                    startDate = date
                )
                dialogRoomItems = listOf(initialItem)
                itemNightsMap = mapOf(initialItem.id to 1)
                itemSamePriceMap = mapOf(initialItem.id to true)
                itemRatesMap = mapOf(initialItem.id to listOf(""))
            }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title
                Text(
                    text = if (bookingToEdit != null) {
                        if (isDormMode) "Edit Booking (Bed $roomNumber)" else "Edit Booking ($roomNumber)"
                    } else {
                        if (isDormMode) "Book Bed $roomNumber on $displayDate" else "Book $roomNumber on $displayDate"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
                )
                // Content area (scrollable)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                val activeTabs = remember(platform, extraPriceStr, bookingToEdit) {
                    val list = mutableListOf("Booking Info")
                    if (platform == "Direct" || (extraPriceStr.toDoubleOrNull() ?: 0.0) > 0.0) {
                        list.add("Payment Details")
                    }
                    if (bookingToEdit != null) {
                        list.add("Guest IDs")
                    }
                    list
                }
                
                val safeSelectedTab = remember(selectedTab, activeTabs) {
                    if (selectedTab >= activeTabs.size) 0 else selectedTab
                }
                val tabTitle = activeTabs.getOrNull(safeSelectedTab) ?: "Booking Info"

                TabRow(
                    selectedTabIndex = safeSelectedTab,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    activeTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = safeSelectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                    }
                }

                if (showError) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                val actualDormSamePrice = if (platform == "Direct") dormSamePrice else samePriceForAllNights
                val dormRatesList = dormRates
                val totalDormVal = if (hasDormBooking) {
                    val rl = dormRatesList.map { it.toDoubleOrNull() ?: 0.0 }
                    if (actualDormSamePrice) rl.firstOrNull() ?: 0.0 else rl.sum()
                } else 0.0
                val dormShareRate = if (dormBedCount > 0) totalDormVal / dormBedCount else 0.0

                LazyColumn(
                    state = dialogLazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (tabTitle == "Booking Info") {

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

                // Global Nights & Same Price Toggle
                if (platform != "Direct") {
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
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (bookingNights > 1) {
                                                bookingNights--
                                                dialogRoomItems.forEach { item ->
                                                    val curR = itemRatesMap[item.id] ?: listOf("")
                                                    itemRatesMap = itemRatesMap + (item.id to adjustRatesList(curR, if (samePriceForAllNights) 1 else bookingNights))
                                                }
                                                dormRates = adjustRatesList(dormRates, if (samePriceForAllNights) 1 else bookingNights)
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
                                            dialogRoomItems.forEach { item ->
                                                val curR = itemRatesMap[item.id] ?: listOf("")
                                                itemRatesMap = itemRatesMap + (item.id to adjustRatesList(curR, if (samePriceForAllNights) 1 else bookingNights))
                                            }
                                            dormRates = adjustRatesList(dormRates, if (samePriceForAllNights) 1 else bookingNights)
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
                                }
                                Switch(
                                    checked = samePriceForAllNights,
                                    onCheckedChange = { checked ->
                                        samePriceForAllNights = checked
                                        dialogRoomItems.forEach { item ->
                                            val curR = itemRatesMap[item.id] ?: listOf("")
                                            itemRatesMap = itemRatesMap + (item.id to adjustRatesList(curR, if (checked) 1 else bookingNights))
                                        }
                                        dormRates = adjustRatesList(dormRates, if (checked) 1 else bookingNights)
                                    }
                                )
                            }
                        }
                    }
                }



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
                                            amount = 0.0,
                                            startDate = date
                                        )
                                        dialogRoomItems = dialogRoomItems + newItem
                                        itemNightsMap = itemNightsMap + (newItem.id to bookingNights)
                                        itemSamePriceMap = itemSamePriceMap + (newItem.id to samePriceForAllNights)
                                        itemRatesMap = itemRatesMap + (newItem.id to adjustRatesList(emptyList(), if (samePriceForAllNights) 1 else bookingNights))
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
                                            dormNights = bookingNights
                                            dormSamePrice = samePriceForAllNights
                                            dormRates = adjustRatesList(emptyList(), if (samePriceForAllNights) 1 else bookingNights)
                                            dormStartDate = date
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("+ Add Dorm Beds", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        dialogRoomItems.forEach { item ->
                            val itemNights = if (platform == "Direct") (itemNightsMap[item.id] ?: bookingNights) else bookingNights
                            val itemSamePrice = if (platform == "Direct") (itemSamePriceMap[item.id] ?: samePriceForAllNights) else samePriceForAllNights
                            val ratesList = itemRatesMap[item.id] ?: listOf("")
                            val itemCategory = if (item.roomNumber.isNotBlank()) {
                                val firstRoom = item.roomNumber.split(",").firstOrNull { it.isNotBlank() } ?: ""
                                if (firstRoom.isNotBlank()) getRoomCategory(firstRoom) else (if (item.category.isNotBlank()) item.category else "Room")
                            } else {
                                if (item.category.isNotBlank()) item.category else "Room"
                            }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 1. Header row (Category Badge + Start Date + Delete button)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = itemCategory,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                                )
                                            }

                                            val itemStartDate = item.startDate.takeIf { !it.isNullOrBlank() } ?: date
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
                                                modifier = Modifier.clickable {
                                                    val cal = Calendar.getInstance().apply {
                                                        try {
                                                            time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(itemStartDate) ?: Date()
                                                        } catch (e: Exception) {}
                                                    }
                                                    android.app.DatePickerDialog(
                                                        context,
                                                        { _, y, m, dOfMonth ->
                                                            val selectedCal = Calendar.getInstance()
                                                            selectedCal.set(y, m, dOfMonth)
                                                            val newDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedCal.time)
                                                            
                                                            dialogRoomItems = dialogRoomItems.map {
                                                                if (it.id == item.id) it.copy(startDate = newDateStr) else it
                                                            }
                                                        },
                                                        cal.get(Calendar.YEAR),
                                                        cal.get(Calendar.MONTH),
                                                        cal.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                }
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
                                        }

                                        // Delete button (show if there's more than 1 item combined or has dorm booking)
                                        val canDeleteRoom = (dialogRoomItems.size > 1) || hasDormBooking
                                        if (canDeleteRoom) {
                                            IconButton(
                                                onClick = {
                                                    dialogRoomItems = dialogRoomItems.filter { it.id != item.id }
                                                    itemRatesMap = itemRatesMap - item.id
                                                    itemNightsMap = itemNightsMap - item.id
                                                    itemSamePriceMap = itemSamePriceMap - item.id
                                                },
                                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Room", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    // 2. Direct-only settings (Nights stepper & Same Price Switch in a separate row to avoid crowding)
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
                                                Text("Nights:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                IconButton(
                                                    onClick = {
                                                        val currentNights = itemNightsMap[item.id] ?: bookingNights
                                                        if (currentNights > 1) {
                                                            val nextNights = currentNights - 1
                                                            itemNightsMap = itemNightsMap + (item.id to nextNights)
                                                            val currentRates = itemRatesMap[item.id] ?: listOf("")
                                                            val same = itemSamePriceMap[item.id] ?: samePriceForAllNights
                                                            itemRatesMap = itemRatesMap + (item.id to adjustRatesList(currentRates, if (same) 1 else nextNights))
                                                        }
                                                    },
                                                    enabled = (itemNightsMap[item.id] ?: bookingNights) > 1,
                                                    modifier = Modifier.size(28.dp),
                                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                                Text((itemNightsMap[item.id] ?: bookingNights).toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                IconButton(
                                                    onClick = {
                                                        val currentNights = itemNightsMap[item.id] ?: bookingNights
                                                        val nextNights = currentNights + 1
                                                        itemNightsMap = itemNightsMap + (item.id to nextNights)
                                                        val currentRates = itemRatesMap[item.id] ?: listOf("")
                                                        val same = itemSamePriceMap[item.id] ?: samePriceForAllNights
                                                        itemRatesMap = itemRatesMap + (item.id to adjustRatesList(currentRates, if (same) 1 else nextNights))
                                                    },
                                                    modifier = Modifier.size(28.dp),
                                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("Same price:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Switch(
                                                    checked = itemSamePriceMap[item.id] ?: samePriceForAllNights,
                                                    onCheckedChange = { checked ->
                                                        itemSamePriceMap = itemSamePriceMap + (item.id to checked)
                                                        val currentNights = itemNightsMap[item.id] ?: bookingNights
                                                        val currentRates = itemRatesMap[item.id] ?: listOf("")
                                                        itemRatesMap = itemRatesMap + (item.id to adjustRatesList(currentRates, if (checked) 1 else currentNights))
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    // Per-night rows: price/rate field on the left, room selection dropdown on the right side-by-side
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        for (nightIndex in 0 until itemNights) {
                                            key(item.id, nightIndex) {
                                                val itemStartDate = item.startDate.takeIf { !it.isNullOrBlank() } ?: date
                                                val stayDate = getStayDate(itemStartDate, nightIndex)
                                                val assignedRoom = getRoomNumberForNight(item.roomNumber, nightIndex)
                                                val displayRate = ratesList.getOrNull(if (itemSamePrice) 0 else nightIndex) ?: ""
                                                val allRoomsForCat = getRoomsForCategory(itemCategory)
                                                val otherItemsRoomSelection = remember(dialogRoomItems, item.id, nightIndex) {
                                                    dialogRoomItems.filter { it.id != item.id }
                                                        .map { getRoomNumberForNight(it.roomNumber, nightIndex) }
                                                }
                                                val availableRooms = remember(allRoomsForCat, bookings, stayDate, otherItemsRoomSelection, bookingToEdit?.id) {
                                                    allRoomsForCat.filter { room ->
                                                        val isBookedElsewhere = bookings.any { b ->
                                                            b.id != (bookingToEdit?.id ?: "") && 
                                                            b.items.any { bi -> 
                                                                val biStartDate = bi.startDate.takeIf { !it.isNullOrBlank() } ?: b.checkInDate
                                                                (0 until bi.nights).any { biNightIdx ->
                                                                    getStayDate(biStartDate, biNightIdx) == stayDate &&
                                                                    getRoomNumberForNight(bi.roomNumber, biNightIdx) == room
                                                                }
                                                            }
                                                        }
                                                        val isSelectedByOtherItemThisNight = otherItemsRoomSelection.contains(room)
                                                        !isBookedElsewhere && !isSelectedByOtherItemThisNight
                                                    }
                                                }

                                                var dropdownExpanded by remember { mutableStateOf(false) }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val displayStayDate = try {
                                                        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                                        val formatter = SimpleDateFormat("dd/MM", Locale.US)
                                                        val d = parser.parse(stayDate)
                                                        if (d != null) formatter.format(d) else stayDate
                                                    } catch (e: Exception) {
                                                        stayDate
                                                    }
                                                    Text(
                                                        text = "N${nightIndex + 1} ($displayStayDate):",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.width(60.dp)
                                                    )

                                                    // Price Input Field
                                                    OutlinedTextField(
                                                        value = displayRate,
                                                        onValueChange = { newRate ->
                                                            val updatedList = adjustRatesList(ratesList, itemNights).toMutableList()
                                                            if (itemSamePrice) {
                                                                for (k in 0 until itemNights) {
                                                                    updatedList[k] = newRate
                                                                }
                                                            } else {
                                                                updatedList[nightIndex] = newRate
                                                            }
                                                            itemRatesMap = itemRatesMap + (item.id to updatedList)

                                                            val index = dialogRoomItems.indexOf(item)
                                                            if (index != -1) {
                                                                val newList = dialogRoomItems.toMutableList()
                                                                newList[index] = item.copy(amount = updatedList.map { it.toDoubleOrNull() ?: 0.0 }.sum())
                                                                dialogRoomItems = newList
                                                            }
                                                            if ((newRate.toDoubleOrNull() ?: 0.0) > 0.0) {
                                                                roomRateErrors = roomRateErrors - item.id
                                                            }
                                                        },
                                                        placeholder = { Text("Rate") },
                                                        prefix = { Text("₹") },
                                                        isError = roomRateErrors[item.id] != null,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).height(50.dp),
                                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                                                    )

                                                    // Room Dropdown
                                                    Box(modifier = Modifier.width(130.dp)) {
                                                        val hasNoErr = roomNoErrors[item.id]
                                                        OutlinedCard(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(50.dp)
                                                                .clickable { dropdownExpanded = true },
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                            border = BorderStroke(1.dp, if (hasNoErr != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .padding(horizontal = 8.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = if (assignedRoom.isNotBlank()) "Room $assignedRoom" else "Assign...",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = if (assignedRoom.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (assignedRoom.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                        DropdownMenu(
                                                            expanded = dropdownExpanded,
                                                            onDismissRequest = { dropdownExpanded = false }
                                                        ) {
                                                            DropdownMenuItem(
                                                                text = { Text("Unassigned", fontSize = 12.sp) },
                                                                onClick = {
                                                                    val roomsList = (0 until itemNights).map { getRoomNumberForNight(item.roomNumber, it) }.toMutableList()
                                                                    while (roomsList.size < itemNights) {
                                                                        roomsList.add(getRoomNumberForNight(item.roomNumber, 0))
                                                                    }
                                                                    roomsList[nightIndex] = ""
                                                                    val newRoomNo = roomsList.joinToString(",")

                                                                    val index = dialogRoomItems.indexOf(item)
                                                                    if (index != -1) {
                                                                        val newList = dialogRoomItems.toMutableList()
                                                                        newList[index] = item.copy(roomNumber = newRoomNo)
                                                                        dialogRoomItems = newList
                                                                    }
                                                                    dropdownExpanded = false
                                                                }
                                                            )
                                                            val distinctRooms = if (assignedRoom.isNotBlank() && !allRoomsForCat.contains(assignedRoom)) {
                                                                listOf(assignedRoom) + allRoomsForCat
                                                            } else {
                                                                allRoomsForCat
                                                            }
                                                            if (distinctRooms.isEmpty()) {
                                                                DropdownMenuItem(
                                                                    text = { Text("No rooms in category", fontSize = 12.sp) },
                                                                    onClick = {},
                                                                    enabled = false
                                                                )
                                                            } else {
                                                                distinctRooms.forEach { room ->
                                                                    val isAvailable = availableRooms.contains(room) || room == assignedRoom
                                                                    DropdownMenuItem(
                                                                        text = {
                                                                            Text(
                                                                                text = if (isAvailable) "Room $room" else "Room $room (Occupied)",
                                                                                fontSize = 12.sp,
                                                                                color = if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                                                            )
                                                                        },
                                                                        onClick = {
                                                                            val roomsList = (0 until itemNights).map { getRoomNumberForNight(item.roomNumber, it) }.toMutableList()
                                                                            while (roomsList.size < itemNights) {
                                                                                roomsList.add(getRoomNumberForNight(item.roomNumber, 0))
                                                                            }
                                                                            roomsList[nightIndex] = room
                                                                            val newRoomNo = roomsList.joinToString(",")

                                                                            val index = dialogRoomItems.indexOf(item)
                                                                            if (index != -1) {
                                                                                val newList = dialogRoomItems.toMutableList()
                                                                                newList[index] = item.copy(roomNumber = newRoomNo)
                                                                                dialogRoomItems = newList
                                                                            }
                                                                            roomNoErrors = roomNoErrors - item.id
                                                                            dropdownExpanded = false
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Dorm Beds Booking",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        val itemStartDate = dormStartDate.takeIf { !it.isNullOrBlank() } ?: date
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
                                            modifier = Modifier.clickable {
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
                                                        dormStartDate = newDateStr
                                                    },
                                                    cal.get(Calendar.YEAR),
                                                    cal.get(Calendar.MONTH),
                                                    cal.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            }
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
                                    }
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

                                // Direct-only Dorm Nights stepper & Same Price Switch
                                if (platform == "Direct") {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("Dorm Nights:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            IconButton(
                                                onClick = {
                                                    if (dormNights > 1) {
                                                        dormNights--
                                                        dormRates = adjustRatesList(dormRates, if (dormSamePrice) 1 else dormNights)
                                                    }
                                                },
                                                enabled = dormNights > 1,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            Text(dormNights.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            IconButton(
                                                onClick = {
                                                    dormNights++
                                                    dormRates = adjustRatesList(dormRates, if (dormSamePrice) 1 else dormNights)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("Same price:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Switch(
                                                checked = dormSamePrice,
                                                onCheckedChange = { checked ->
                                                    dormSamePrice = checked
                                                    dormRates = adjustRatesList(dormRates, if (checked) 1 else dormNights)
                                                }
                                            )
                                        }
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }

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
                        val actualDormNights = if (platform == "Direct") dormNights else bookingNights
                        val actualDormSamePrice = if (platform == "Direct") dormSamePrice else samePriceForAllNights
                        val dormRatesList = dormRates
                        
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (actualDormSamePrice) {
                                OutlinedTextField(
                                    value = dormRatesList.firstOrNull() ?: "",
                                    onValueChange = { 
                                        val updated = dormRatesList.toMutableList()
                                        if (updated.isEmpty()) updated.add(it) else updated[0] = it
                                        dormRates = updated
                                        if ((it.toDoubleOrNull() ?: 0.0) > 0.0) dormTotalAmountError = null
                                    },
                                    isError = dormTotalAmountError != null,
                                    supportingText = dormTotalAmountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                                    label = { Text("Total Dorm Beds Price (All Nights & Beds)") },
                                    placeholder = { Text("e.g. 3000") },
                                    prefix = { Text("₹ ") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            } else {
                                Text("Enter total dorm price (all beds) for each night:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                for (nightIndex in 0 until actualDormNights) {
                                    val rateValue = dormRatesList.getOrNull(nightIndex) ?: ""
                                    OutlinedTextField(
                                        value = rateValue,
                                        onValueChange = { newRate ->
                                            val updated = adjustRatesList(dormRatesList, actualDormNights).toMutableList()
                                            updated[nightIndex] = newRate
                                            dormRates = updated
                                            if ((newRate.toDoubleOrNull() ?: 0.0) > 0.0) dormTotalAmountError = null
                                        },
                                        isError = dormTotalAmountError != null,
                                        placeholder = { Text("Night ${nightIndex + 1} Total Dorm Price (All Beds)") },
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
                        val calculatedSum = (dialogRoomItems.map { it.amount }.sum() + totalDormVal)
                        val totalBillValue = if (platform == "Direct" && isBillOn) {
                            billAmountStr.toDoubleOrNull() ?: calculatedSum
                        } else {
                            calculatedSum
                        }
                        val discountVal = discountStr.toDoubleOrNull() ?: 0.0
                        val commBase = (totalBillValue - discountVal).coerceAtLeast(0.0)
                        val breakdown = com.sparsh.myapplication.SettingsManager.calculateBreakdown(context, platform, commBase)
                        
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

                // Discount and Extra Price fields
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (platform == "Direct") {
                            OutlinedTextField(
                                value = discountStr,
                                onValueChange = { discountStr = it },
                                label = { Text("Discount (₹)") },
                                placeholder = { Text("0") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = extraPriceStr,
                            onValueChange = { extraPriceStr = it },
                            label = { Text("Extra Charge (₹)") },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(if (platform == "Direct") 1f else 2f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
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

                } // end of tabTitle == "Booking Info"
                else if (tabTitle == "Payment Details") {
                    // Payments Details Section
                    if (platform == "Direct" || (extraPriceStr.toDoubleOrNull() ?: 0.0) > 0.0) {
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

                                val calculatedSum = (dialogRoomItems.map { it.amount }.sum() + totalDormVal)
                                
                                val totalBillValue = if (platform == "Direct" && isBillOn) {
                                    billAmountStr.toDoubleOrNull() ?: calculatedSum
                                } else {
                                    calculatedSum
                                }

                                val extraPriceVal = extraPriceStr.toDoubleOrNull() ?: 0.0
                                val discountVal = discountStr.toDoubleOrNull() ?: 0.0
                                
                                val targetAmount = if (platform == "Direct") {
                                    totalBillValue + extraPriceVal - discountVal
                                } else {
                                    extraPriceVal
                                }

                                if (bookingToEdit == null) {
                                    // New Booking: Show Advance Payment
                                    val labelText = if (platform != "Direct") "Advance for Extra Charge" else "Advance Payment Amount"
                                    OutlinedTextField(
                                        value = advancePaymentStr,
                                        onValueChange = { advancePaymentStr = it },
                                        label = { Text(labelText) },
                                        placeholder = { Text("e.g. 1000 (0 for none)") },
                                        prefix = { Text("₹ ") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
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

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Advance Payment Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val advDateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(advancePaymentDate))
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = advDateFormatted,
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date", modifier = Modifier.size(18.dp)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .clickable {
                                                        val cal = Calendar.getInstance().apply { timeInMillis = advancePaymentDate }
                                                        DatePickerDialog(
                                                            context,
                                                            { _, y, m, dOfMonth ->
                                                                val selectedCal = Calendar.getInstance()
                                                                selectedCal.set(y, m, dOfMonth)
                                                                advancePaymentDate = selectedCal.timeInMillis
                                                            },
                                                            cal.get(Calendar.YEAR),
                                                            cal.get(Calendar.MONTH),
                                                            cal.get(Calendar.DAY_OF_MONTH)
                                                        ).show()
                                                    }
                                            )
                                        }
                                    }
                                } else {
                                    // Edit Booking: Show list of payments and allow adding new ones
                                    val totalPaidVal = dialogPayments.sumOf { it.amount }
                                    val balanceVal = targetAmount - totalPaidVal
                                    android.util.Log.e("QuickBookDialog", "RECOMPOSING PAYMENTS: targetAmount=$targetAmount, totalPaidVal=$totalPaidVal, balanceVal=$balanceVal, isBillOn=$isBillOn, billAmountStr=$billAmountStr, dialogRoomItemsSize=${dialogRoomItems.size}, dialogRoomItems=${dialogRoomItems.map { "${it.roomNumber}:${it.amount}" }}")
 
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
                                                            val finalUpdatedPayments = if (platform != "Direct") {
                                                                val portalBase = bookingToEdit.payments.firstOrNull { it.id == "portal_base" }
                                                                    ?: PaymentDetail(id = "portal_base", amount = bookingToEdit.baseAmountCharged - bookingToEdit.discount, method = "Portal (Auto)")
                                                                listOf(portalBase) + updatedPayments
                                                            } else {
                                                                updatedPayments
                                                            }
                                                            val updatedBooking = bookingToEdit.copy(
                                                                payments = finalUpdatedPayments
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
                                        if (platform == "Direct") {
                                            Text("Total: ₹${formatDouble(targetAmount)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Paid: ₹${formatDouble(totalPaidVal)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = "Balance: ₹${formatDouble(balanceVal)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (balanceVal > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text("Extra Charge: ₹${formatDouble(extraPriceVal)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("Paid at Prop: ₹${formatDouble(totalPaidVal)}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = "Prop Balance: ₹${formatDouble(balanceVal)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (balanceVal > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                     if (true) {
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
                                                Box(modifier = Modifier.weight(1f)) {
                                                    OutlinedTextField(
                                                        value = newDateFormatted,
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date", modifier = Modifier.size(16.dp)) },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .clickable {
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
                                                            }
                                                    )
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
                                                            val finalUpdatedPayments = if (platform != "Direct") {
                                                                val portalBase = bookingToEdit.payments.firstOrNull { it.id == "portal_base" }
                                                                    ?: PaymentDetail(id = "portal_base", amount = bookingToEdit.baseAmountCharged - bookingToEdit.discount, method = "Portal (Auto)")
                                                                listOf(portalBase) + updatedPayments
                                                            } else {
                                                                updatedPayments
                                                            }
                                                            val updatedBooking = bookingToEdit.copy(
                                                                payments = finalUpdatedPayments
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
                    } else {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Payments for OTA bookings ($platform) are managed by the platform directly.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                else if (tabTitle == "Guest IDs") {
                    if (bookingToEdit != null) {
                        item {
                            IdDocumentUploadSection(
                                booking = bookingToEdit,
                                bookingRepository = bookingRepository,
                                onBookingUpdated = { updatedBooking ->
                                    if (onSaveWithoutDismiss != null) {
                                        onSaveWithoutDismiss(updatedBooking)
                                    }
                                }
                            )
                        }
                    }
                }
            } // end LazyColumn
        } // end weight(1f) Column

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 56.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                            val hasIds = bookingToEdit.guestIds.isNotEmpty()
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                if (hasIds) {
                                    Button(
                                        onClick = {
                                            onDelete(bookingToEdit.id, true)
                                            showDeleteConfirm = false
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Delete Booking & IDs", fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onDelete(bookingToEdit.id, false)
                                            showDeleteConfirm = false
                                            onDismiss()
                                        },
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Delete Booking Only", fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            onDelete(bookingToEdit.id, false)
                                            showDeleteConfirm = false
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Delete")
                                    }
                                }
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
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
                    Spacer(modifier = Modifier.width(8.dp))

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
                            dialogRoomItems.forEach { item ->
                                val itemNights = if (platform == "Direct") (itemNightsMap[item.id] ?: bookingNights) else bookingNights
                                val itemSamePrice = if (platform == "Direct") (itemSamePriceMap[item.id] ?: samePriceForAllNights) else samePriceForAllNights
                                
                                // Check if at least one room is assigned across the stay
                                val assignedRooms = (0 until itemNights).map { getRoomNumberForNight(item.roomNumber, it) }
                                if (assignedRooms.all { it.isBlank() }) {
                                    newRoomNoErrors[item.id] = "At least one room assignment is required."
                                }

                                val ratesList = itemRatesMap[item.id] ?: listOf("")
                                if (itemSamePrice) {
                                    val rateVal = ratesList.firstOrNull()?.toDoubleOrNull() ?: 0.0
                                    if (rateVal <= 0.0) {
                                        newRoomRateErrors[item.id] = "Rate must be greater than 0."
                                    }
                                } else {
                                    val hasInvalidRate = ratesList.take(itemNights).any { (it.toDoubleOrNull() ?: 0.0) <= 0.0 }
                                    if (hasInvalidRate || ratesList.size < itemNights) {
                                        newRoomRateErrors[item.id] = "All night rates must be greater than 0."
                                    }
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

                                val actualDormNights = if (platform == "Direct") dormNights else bookingNights
                                val actualDormSamePrice = if (platform == "Direct") dormSamePrice else samePriceForAllNights
                                val dormRatesList = dormRates
                                
                                if (actualDormSamePrice) {
                                    val totalDormVal = dormRatesList.firstOrNull()?.toDoubleOrNull()
                                    if (totalDormVal == null || totalDormVal <= 0.0) {
                                        dormTotalAmountError = "Please enter a valid total dorm price."
                                        errorMessage = "Please enter a valid total dorm price."
                                        showError = true
                                        scrollToField("dormTotalAmount")
                                        return@Button
                                    }
                                } else {
                                    val hasInvalidRate = dormRatesList.take(actualDormNights).any { (it.toDoubleOrNull() ?: 0.0) <= 0.0 }
                                    if (hasInvalidRate || dormRatesList.size < actualDormNights) {
                                        dormTotalAmountError = "All night rates must be greater than 0."
                                        errorMessage = "All night rates must be greater than 0."
                                        showError = true
                                        scrollToField("dormTotalAmount")
                                        return@Button
                                    }
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
                                    val counts = getDormBedBookingCounts(date, actualDormNights, dormRoom, bookings, bookingToEdit?.id)
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
                            val actualDormNights = if (platform == "Direct") dormNights else bookingNights
                            val actualDormSamePrice = if (platform == "Direct") dormSamePrice else samePriceForAllNights
                            val dormRatesList = dormRates.take(actualDormNights).map { it.toDoubleOrNull() ?: 0.0 }
                            
                            val dormShareRatesList = if (actualDormSamePrice) {
                                val totalEntered = dormRatesList.firstOrNull() ?: 0.0
                                val ratePerBedPerNight = totalEntered / (actualDormNights.coerceAtLeast(1) * dormBedCount.coerceAtLeast(1))
                                List(actualDormNights) { ratePerBedPerNight }
                            } else {
                                dormRatesList.map { it / dormBedCount.coerceAtLeast(1) }
                            }
                            
                            val totalDormVal = dormShareRatesList.sum() * dormBedCount

                            // Build Dorm booking items list
                            val dormBookingItems = if (hasDormBooking) {
                                parsedDormBeds.map { bedNo ->
                                    BookingItem(
                                        id = UUID.randomUUID().toString(),
                                        category = getRoomCategory(bedNo),
                                        roomNumber = bedNo,
                                        amount = dormShareRatesList.sum(),
                                        nights = actualDormNights,
                                        rates = dormShareRatesList,
                                        startDate = dormStartDate
                                    )
                                }
                            } else {
                                emptyList()
                            }

                            val roomBookingItems = dialogRoomItems.map { item ->
                                val itemNights = if (platform == "Direct") (itemNightsMap[item.id] ?: bookingNights) else bookingNights
                                val itemSamePrice = if (platform == "Direct") (itemSamePriceMap[item.id] ?: samePriceForAllNights) else samePriceForAllNights
                                val ratesList = (itemRatesMap[item.id] ?: listOf("")).take(itemNights).map { it.toDoubleOrNull() ?: 0.0 }
                                
                                val ratesPerNight = if (itemSamePrice) {
                                    val ratePerNight = ratesList.firstOrNull() ?: 0.0
                                    List(itemNights) { ratePerNight }
                                } else {
                                    ratesList
                                }
                                
                                item.copy(
                                    category = if (item.roomNumber.isNotBlank()) {
                                        val firstRoom = item.roomNumber.split(",").firstOrNull { it.isNotBlank() } ?: ""
                                        if (firstRoom.isNotBlank()) getRoomCategory(firstRoom) else item.category
                                    } else item.category,
                                    amount = ratesPerNight.sum(),
                                    nights = itemNights,
                                    rates = ratesPerNight
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

                            val discountVal = if (platform == "Direct") (discountStr.toDoubleOrNull() ?: 0.0) else 0.0
                            val extraPriceVal = extraPriceStr.toDoubleOrNull() ?: 0.0

                            val commissionVal = if (platform != "Direct") {
                                val commBase = (finalBillAmount - discountVal).coerceAtLeast(0.0)
                                com.sparsh.myapplication.SettingsManager.calculateBreakdown(context, platform, commBase).totalDeductions
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
                                val basePayment = PaymentDetail(
                                    id = "portal_base",
                                    amount = finalBillAmount - discountVal,
                                    method = "Portal (Auto)",
                                    timestamp = bookingToEdit?.payments?.firstOrNull { it.id == "portal_base" }?.timestamp ?: System.currentTimeMillis()
                                )
                                val extraPayments = if (bookingToEdit == null) {
                                    val advVal = advancePaymentStr.toDoubleOrNull() ?: 0.0
                                    if (advVal > 0.0) {
                                        listOf(
                                            PaymentDetail(
                                                amount = advVal,
                                                method = if (advancePaymentIsUnknown) "Unknown" else advancePaymentMethod,
                                                timestamp = if (advancePaymentIsUnknown) 0L else advancePaymentDate
                                            )
                                        )
                                    } else {
                                        emptyList()
                                    }
                                } else {
                                    dialogPayments
                                }
                                listOf(basePayment) + extraPayments
                            } else if (bookingToEdit == null) {
                                val advVal = advancePaymentStr.toDoubleOrNull() ?: 0.0
                                if (advVal > 0.0) {
                                    listOf(
                                        PaymentDetail(
                                            amount = advVal,
                                            method = if (advancePaymentIsUnknown) "Unknown" else advancePaymentMethod,
                                            timestamp = if (advancePaymentIsUnknown) 0L else advancePaymentDate
                                        )
                                    )
                                } else {
                                    emptyList()
                                }
                            } else {
                                dialogPayments
                            }

                            val finalCheckInDate = if (platform == "Direct") {
                                updatedItems.map { it.startDate.takeIf { !it.isNullOrBlank() } ?: date }.minOrNull() ?: date
                            } else {
                                date
                            }

                            val newBooking = Booking(
                                id = bookingToEdit?.id ?: com.sparsh.myapplication.generateBookingId(),
                                guestIds = bookingToEdit?.guestIds ?: emptyList(),
                                checkInDate = finalCheckInDate,
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
                                discount = discountVal,
                                extraPrice = extraPriceVal,
                                timestamp = bookingToEdit?.timestamp ?: System.currentTimeMillis()
                            )
                            onConfirm(newBooking)
                        }
                    ) {
                        Text("Confirm")
                    }
                } // end buttons Row
            } // end outer Column
        } // end Surface
    } // end Dialog
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
                            val offset = com.sparsh.myapplication.getDateOffset(booking.checkInDate, date)
                            val bookingItem = booking.items.find { 
                                val isItemDorm = isDormCategory(it.category)
                                val itemRoom = com.sparsh.myapplication.getRoomNumberForNight(it.roomNumber, offset)
                                itemRoom == roomNumber && isItemDorm == isDormMode
                            }
                            val displayRate = if (bookingItem != null) {
                                if (bookingItem.rates.isNotEmpty()) {
                                    bookingItem.rates.getOrNull(offset) ?: (bookingItem.amount / (if (bookingItem.nights > 0) bookingItem.nights else 1))
                                } else {
                                    bookingItem.amount / (if (bookingItem.nights > 0) bookingItem.nights else 1)
                                }
                            } else {
                                0.0
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
                                        val nightsSuffix = if (bookingItem != null && bookingItem.nights > 1) " • ${bookingItem.nights} nights" else ""
                                        Text(
                                            text = "${booking.platform} • ${booking.paymentMethod}$nightsSuffix",
                                            fontSize = 11.sp,
                                            color = colors.second.copy(alpha = 0.7f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${formatDouble(displayRate)}",
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
    nights: Int,
    dormRoom: String,
    bookings: List<Booking>,
    excludeBookingId: String?
): Map<Int, Int> {
    val counts = mutableMapOf<Int, Int>()
    for (i in 1..8) {
        counts[i] = 0
    }
    
    val requestedDates = (0 until (if (nights > 0) nights else 1)).map { getStayDate(date, it) }.toSet()
    
    for (b in bookings) {
        if (excludeBookingId != null && b.id == excludeBookingId) continue
        for (item in b.items) {
            if (isDormCategory(item.category)) {
                val itemNights = if (item.nights > 0) item.nights else 1
                val itemStartDate = item.startDate.takeIf { !it.isNullOrBlank() } ?: b.checkInDate
                val itemDates = (0 until itemNights).map { getStayDate(itemStartDate, it) }.toSet()
                if (requestedDates.intersect(itemDates).isNotEmpty()) {
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
    }
    return counts
}

@Composable
fun IdDocumentUploadSection(
    booking: Booking,
    bookingRepository: BookingRepository,
    onBookingUpdated: (Booking) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    
    var activeUploadCardId by remember { mutableStateOf<String?>(null) }
    var activeUploadImageId by remember { mutableStateOf<String?>(null) }
    var activeUploadLabel by remember { mutableStateOf("") }
    var activeUploadIndex by remember { mutableStateOf(1) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var previewImageTitle by remember { mutableStateOf("") }
    
    // Create uri for temporary file to save camera capture
    val file = remember {
        java.io.File(context.cacheDir, "temp_id_capture.jpg")
    }
    val uri = remember {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    // Helper to read bytes from Uri and convert to Base64
    fun uriToBase64(imageUri: android.net.Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                var bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val maxDimension = 1600
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val newWidth = if (ratio > 1) {
                        if (bitmap.width > maxDimension) maxDimension else bitmap.width
                    } else {
                        if (bitmap.height > maxDimension) (maxDimension * ratio).toInt() else bitmap.width
                    }
                    val newHeight = if (ratio > 1) {
                        if (bitmap.width > maxDimension) (maxDimension / ratio).toInt() else bitmap.height
                    } else {
                        if (bitmap.height > maxDimension) maxDimension else bitmap.height
                    }
                    bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                    
                    val outputStream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val compressedBytes = outputStream.toByteArray()
                    android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT)
                } else {
                    android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val cardId = activeUploadCardId ?: return@rememberLauncherForActivityResult
            val imageId = activeUploadImageId ?: return@rememberLauncherForActivityResult
            val label = activeUploadLabel
            val idx = activeUploadIndex
            
            isUploading = true
            uploadError = null
            
            coroutineScope.launch {
                val base64 = uriToBase64(uri)
                if (base64 != null) {
                    val card = booking.guestIds.find { it.id == cardId }
                    val idType = card?.idType ?: "Aadhaar Card"
                    val guestName = card?.guestName ?: ""
                    
                    val updated = bookingRepository.uploadGuestId(
                        bookingId = booking.id,
                        cardId = cardId,
                        imageId = imageId,
                        idType = idType,
                        guestName = guestName,
                        imageBase64 = base64,
                        label = label,
                        index = idx
                    )
                    isUploading = false
                    if (updated != null) {
                        onBookingUpdated(updated)
                    } else {
                        uploadError = "Upload failed. Please check connection."
                    }
                } else {
                    isUploading = false
                    uploadError = "Error reading captured image."
                }
            }
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { selectedUri ->
        if (selectedUri != null) {
            val cardId = activeUploadCardId ?: return@rememberLauncherForActivityResult
            val imageId = activeUploadImageId ?: return@rememberLauncherForActivityResult
            val label = activeUploadLabel
            val idx = activeUploadIndex
            
            isUploading = true
            uploadError = null
            
            coroutineScope.launch {
                val base64 = uriToBase64(selectedUri)
                if (base64 != null) {
                    val card = booking.guestIds.find { it.id == cardId }
                    val idType = card?.idType ?: "Aadhaar Card"
                    val guestName = card?.guestName ?: ""
                    
                    val updated = bookingRepository.uploadGuestId(
                        bookingId = booking.id,
                        cardId = cardId,
                        imageId = imageId,
                        idType = idType,
                        guestName = guestName,
                        imageBase64 = base64,
                        label = label,
                        index = idx
                    )
                    isUploading = false
                    if (updated != null) {
                        onBookingUpdated(updated)
                    } else {
                        uploadError = "Upload failed. Please check connection."
                    }
                } else {
                    isUploading = false
                    uploadError = "Error reading selected image."
                }
            }
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to scan IDs", Toast.LENGTH_SHORT).show()
        }
    }
    
    var newGuestName by remember { mutableStateOf("") }
    var newIdType by remember { mutableStateOf("Aadhaar Card") }
    var showNewIdDropdown by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Guest ID Cards Management",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary
        )
        
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Uploading document to Google Drive...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        uploadError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Loop through existing cards
        booking.guestIds.forEach { card ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = card.guestName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = card.idType, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    card.images.forEach { img ->
                                        bookingRepository.deleteGuestIdImage(booking.id, card.id, img.id)
                                    }
                                    val updated = booking.copy(
                                        guestIds = booking.guestIds.filter { it.id != card.id }
                                    )
                                    bookingRepository.saveBooking(updated)
                                    onBookingUpdated(updated)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    
                    // Determine slots
                    val slots = if (card.idType == "Aadhaar Card") {
                        listOf(
                            Pair("Front Side", 1),
                            Pair("Back Side", 2)
                        )
                    } else {
                        val list = mutableListOf(Pair("Front Side", 1))
                        card.images.forEachIndexed { idx, img ->
                            if (idx >= 1) {
                                list.add(Pair(img.label.ifBlank { "Page ${idx + 1}" }, idx + 1))
                            }
                        }
                        // Always offer next empty slot if we want to add page
                        val nextIdx = card.images.size + 1
                        list.add(Pair("Page $nextIdx", nextIdx))
                        list
                    }
                    
                    slots.forEach { (label, index) ->
                        val img = card.images.find { it.label == label || (label.startsWith("Page") && it.label == label) } ?: card.images.getOrNull(index - 1)
                        if (img != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        previewImageUrl = img.url
                                        previewImageTitle = "${card.guestName} (${card.idType}) - $label"
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Preview",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "VIEW PHOTO",
                                            color = Color(0xFF2E7D32),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val updated = bookingRepository.deleteGuestIdImage(booking.id, card.id, img.id)
                                            if (updated != null) {
                                                onBookingUpdated(updated)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete image",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            activeUploadCardId = card.id
                                            activeUploadImageId = java.util.UUID.randomUUID().toString()
                                            activeUploadLabel = label
                                            activeUploadIndex = index

                                            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                                            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                cameraLauncher.launch(uri)
                                            } else {
                                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Camera", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            activeUploadCardId = card.id
                                            activeUploadImageId = java.util.UUID.randomUUID().toString()
                                            activeUploadLabel = label
                                            activeUploadIndex = index
                                            galleryLauncher.launch("image/*")
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Gallery", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Guest ID Card Creator Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add New Guest ID", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = newGuestName,
                    onValueChange = { newGuestName = it },
                    label = { Text("Guest Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showNewIdDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ID Type: $newIdType", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                    DropdownMenu(
                        expanded = showNewIdDropdown,
                        onDismissRequest = { showNewIdDropdown = false }
                    ) {
                        val idTypes = listOf("Aadhaar Card", "Passport", "Driving License", "PAN Card", "Voter ID", "Other")
                        idTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    newIdType = type
                                    showNewIdDropdown = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (newGuestName.isBlank()) {
                            Toast.makeText(context, "Please enter Guest Name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            val newCard = GuestIdCard(
                                id = java.util.UUID.randomUUID().toString(),
                                idType = newIdType,
                                guestName = newGuestName,
                                images = emptyList()
                            )
                            val updated = booking.copy(
                                guestIds = booking.guestIds + newCard
                            )
                            bookingRepository.saveBooking(updated)
                            onBookingUpdated(updated)
                            newGuestName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Guest ID Card", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (previewImageUrl != null) {
        val directUrl = if (previewImageUrl!!.contains("drive.google.com/file/d/")) {
            val parts = previewImageUrl!!.split("/d/")
            if (parts.size > 1) {
                val fileId = parts[1].split("/")[0]
                "https://drive.google.com/uc?export=download&id=$fileId"
            } else {
                previewImageUrl!!
            }
        } else {
            previewImageUrl!!
        }

        AlertDialog(
            onDismissRequest = { previewImageUrl = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = previewImageTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { previewImageUrl = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.SubcomposeAsyncImage(
                        model = directUrl,
                        loading = {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        },
                        error = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Could not load image directly", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                Button(
                                    onClick = {
                                        val browserIntent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(previewImageUrl)
                                        )
                                        context.startActivity(browserIntent)
                                    }
                                ) {
                                    Text("Open in Browser", fontSize = 11.sp)
                                }
                            }
                        },
                        contentDescription = "ID Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { previewImageUrl = null }) {
                    Text("Close")
                }
            }
        )
    }
}
