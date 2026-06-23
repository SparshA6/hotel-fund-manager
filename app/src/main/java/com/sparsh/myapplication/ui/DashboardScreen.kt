package com.sparsh.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sparsh.myapplication.Booking
import com.sparsh.myapplication.getStayDate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.DialogProperties

enum class ReportPeriod {
    TODAY,
    MONTHLY,
    YEARLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    bookings: List<Booking>,
    onEditBooking: (Booking) -> Unit,
    onDeleteBooking: (String) -> Unit,
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()
    var selectedBookingForAssignment by remember { mutableStateOf<Booking?>(null) }
    var showPendingPaymentsReport by remember { mutableStateOf(false) }

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.TODAY) }
    var calendarOffset by remember(selectedPeriod) { mutableStateOf(0) }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
            pullToRefreshState.endRefresh()
        }
    }

    // Currency formatter
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
    }

    val targetCalendar = remember(selectedPeriod, calendarOffset) {
        Calendar.getInstance().apply {
            when (selectedPeriod) {
                ReportPeriod.TODAY -> add(Calendar.DAY_OF_MONTH, calendarOffset)
                ReportPeriod.MONTHLY -> add(Calendar.MONTH, calendarOffset)
                ReportPeriod.YEARLY -> add(Calendar.YEAR, calendarOffset)
            }
        }
    }

    val periodLabel = remember(selectedPeriod, targetCalendar) {
        when (selectedPeriod) {
            ReportPeriod.TODAY -> SimpleDateFormat("dd MMM yyyy", Locale.US).format(targetCalendar.time)
            ReportPeriod.MONTHLY -> SimpleDateFormat("MMMM yyyy", Locale.US).format(targetCalendar.time)
            ReportPeriod.YEARLY -> SimpleDateFormat("yyyy", Locale.US).format(targetCalendar.time)
        }
    }

    val targetDateStr = remember(selectedPeriod, targetCalendar) {
        when (selectedPeriod) {
            ReportPeriod.TODAY -> SimpleDateFormat("yyyy-MM-dd", Locale.US).format(targetCalendar.time)
            ReportPeriod.MONTHLY -> SimpleDateFormat("yyyy-MM", Locale.US).format(targetCalendar.time)
            ReportPeriod.YEARLY -> SimpleDateFormat("yyyy", Locale.US).format(targetCalendar.time)
        }
    }

    val datePredicate: (String) -> Boolean = remember(selectedPeriod, targetDateStr) {
        { stayDate ->
            when (selectedPeriod) {
                ReportPeriod.TODAY -> stayDate == targetDateStr
                ReportPeriod.MONTHLY -> stayDate.startsWith(targetDateStr)
                ReportPeriod.YEARLY -> stayDate.startsWith(targetDateStr)
            }
        }
    }

    // Helper to compute a booking's financial contribution to the target period
    fun getBookingFinancialsForPeriod(b: Booking): Triple<Double, Double, Double> {
        var periodGross = 0.0
        var periodExpense = 0.0

        if (b.items.isEmpty()) {
            // Fallback for bookings without items (dorm legacy)
            if (datePredicate(b.checkInDate)) {
                periodGross = b.amountCharged
                periodExpense = b.expenses
            }
        } else {
            val sumOfItemAmounts = b.items.sumOf { it.amount }.coerceAtLeast(1.0)
            
            b.items.forEach { item ->
                val itemStartDate = item.startDate ?: b.checkInDate
                val nights = if (item.nights > 0) item.nights else 1
                for (offset in 0 until nights) {
                    val stayDate = getStayDate(itemStartDate, offset)
                    if (datePredicate(stayDate)) {
                        val dailyRate = item.rates.getOrNull(offset) ?: (item.amount / nights)
                        
                        // Scale gross by custom bill ratio if applicable
                        val scaledGross = if (b.isBillOn) {
                            dailyRate * (b.billAmount / sumOfItemAmounts)
                        } else {
                            dailyRate
                        }
                        
                        // Scale expense by rate proportion
                        val scaledExpense = b.expenses * (dailyRate / sumOfItemAmounts)
                        
                        periodGross += scaledGross
                        periodExpense += scaledExpense
                    }
                }
            }
        }
        return Triple(periodGross, periodExpense, periodGross - periodExpense)
    }

    // Filtered bookings active in this period
    val activeBookings = remember(bookings, datePredicate) {
        bookings.filter { b ->
            if (b.items.isEmpty()) {
                datePredicate(b.checkInDate)
            } else {
                b.items.any { item ->
                    val itemStartDate = item.startDate ?: b.checkInDate
                    val nights = if (item.nights > 0) item.nights else 1
                    (0 until nights).any { offset ->
                        datePredicate(getStayDate(itemStartDate, offset))
                    }
                }
            }
        }
    }

    val periodFinancials = remember(bookings, datePredicate) {
        var gross = 0.0
        var expense = 0.0
        bookings.forEach { b ->
            val financials = getBookingFinancialsForPeriod(b)
            gross += financials.first
            expense += financials.second
        }
        Pair(gross, expense)
    }
    val periodGross = periodFinancials.first
    val periodExpense = periodFinancials.second
    val periodNet = periodGross - periodExpense
    val pendingPaymentsCount = remember(activeBookings) {
        activeBookings.count { it.balance > 0.0 }
    }

    // Platform distributions based on period allocations
    val platforms = listOf("Direct", "MMT", "Booking.com", "Agoda", "Goibibo", "Cleartrip")
    val platformCounts = remember(activeBookings) {
        platforms.associateWith { platform ->
            activeBookings.count { it.platform.equals(platform, ignoreCase = true) }
        }
    }
    val platformRevenues = remember(bookings, datePredicate) {
        platforms.associateWith { platform ->
            bookings.sumOf { b ->
                if (b.platform.equals(platform, ignoreCase = true)) {
                    getBookingFinancialsForPeriod(b).first
                } else {
                    0.0
                }
            }
        }
    }
    val maxPlatformRevenue = remember(platformRevenues) {
        platformRevenues.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    }


    // Theme Gradients
    val dashboardGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.surface
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(dashboardGradient)
    ) {
        // Top Header
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Hotel Fund Manager",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Period Switcher (Segmented Control Pill Bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ReportPeriod.values().forEach { period ->
                val selected = selectedPeriod == period
                val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(backgroundColor)
                        .clickable { selectedPeriod = period }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (period) {
                            ReportPeriod.TODAY -> "Today"
                            ReportPeriod.MONTHLY -> "Monthly"
                            ReportPeriod.YEARLY -> "Yearly"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor
                    )
                }
            }
        }

        // Time Period Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { calendarOffset-- },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
            }
            
            Text(
                text = periodLabel,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            IconButton(
                onClick = { calendarOffset++ },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Main Fund Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            val periodTitle = when (selectedPeriod) {
                                ReportPeriod.TODAY -> "Today's Net Income"
                                ReportPeriod.MONTHLY -> "Monthly Net Income"
                                ReportPeriod.YEARLY -> "Yearly Net Income"
                            }
                            Text(
                                text = periodTitle,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormatter.format(periodNet),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Gross Revenue",
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = currencyFormatter.format(periodGross),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Commissions",
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = currencyFormatter.format(periodExpense),
                                        color = Color(0xFFFFCC80), // Warm amber
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }

                // Stats Quick Overview Grid Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Bookings Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Bookings in Period",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${activeBookings.size}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Pending Payments Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (pendingPaymentsCount > 0) {
                                        showPendingPaymentsReport = true
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Pending Payments",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (pendingPaymentsCount == 1) "1 Booking" else "$pendingPaymentsCount Bookings",
                                    color = if (pendingPaymentsCount > 0) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Platform-wise Breakdown
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Platform Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            platforms.forEach { platform ->
                                val count = platformCounts[platform] ?: 0
                                val revenue = platformRevenues[platform] ?: 0.0
                                val fraction = (revenue / maxPlatformRevenue).toFloat().coerceIn(0f, 1f)

                                val platformColor = when (platform) {
                                    "Direct" -> Color(0xFF10B981) // Emerald Green
                                    "MMT" -> Color(0xFFF97316) // Orange
                                    "Booking.com" -> Color(0xFF2563EB) // Royal Blue
                                    "Agoda" -> Color(0xFF8B5CF6) // Purple
                                    "Goibibo" -> Color(0xFFEF4444) // Red
                                    "Cleartrip" -> Color(0xFFF59E0B) // Amber
                                    else -> MaterialTheme.colorScheme.secondary
                                }

                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = platform,
                                            fontWeight = FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "$count bkgs • ${currencyFormatter.format(revenue)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    // Custom visual bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .background(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                                shape = RoundedCornerShape(5.dp)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction)
                                                .fillMaxHeight()
                                                .background(
                                                    platformColor,
                                                    shape = RoundedCornerShape(5.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Bookings Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bookings in Period",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total: ${activeBookings.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Booking Empty State or Feed
                if (activeBookings.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No active bookings in this period",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Use the arrows above to browse other dates or add new check-ins.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(activeBookings, key = { it.id }) { booking ->
                        BookingItem(
                            booking = booking,
                            currencyFormatter = currencyFormatter,
                            onEdit = { onEditBooking(booking) },
                            onDelete = { onDeleteBooking(booking.id) },
                            onAssignClick = if (!booking.isAssigned) {
                                { selectedBookingForAssignment = booking }
                            } else null
                        )
                    }
                }
            }

            if (pullToRefreshState.progress > 0f || pullToRefreshState.isRefreshing) {
                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            if (selectedBookingForAssignment != null) {
                AssignRoomsDialog(
                    booking = selectedBookingForAssignment!!,
                    bookings = bookings,
                    onDismiss = { selectedBookingForAssignment = null },
                    onConfirm = { updatedBooking ->
                        onEditBooking(updatedBooking)
                        selectedBookingForAssignment = null
                    }
                )
            }

            if (showPendingPaymentsReport) {
                PendingPaymentsReportDialog(
                    periodLabel = periodLabel,
                    pendingBookings = activeBookings.filter { it.balance > 0.0 },
                    currencyFormatter = currencyFormatter,
                    onDismiss = { showPendingPaymentsReport = false },
                    onEditBooking = { booking ->
                        showPendingPaymentsReport = false
                        onEditBooking(booking)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingPaymentsReportDialog(
    periodLabel: String,
    pendingBookings: List<Booking>,
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit,
    onEditBooking: (Booking) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .navigationBarsPadding()
            .imePadding(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pending Payments",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Period: $periodLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "The following bookings active during this period have pending balances. Tap any booking to record payment or edit details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pendingBookings, key = { it.id }) { booking ->
                        val platformColors = when (booking.platform.lowercase()) {
                            "direct" -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
                            "mmt" -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
                            "booking.com" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                            "agoda" -> Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A))
                            "goibibo" -> Pair(Color(0xFFFFF3E0), Color(0xFFEF6C00))
                            else -> Pair(Color(0xFFECEFF1), Color(0xFF263238))
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditBooking(booking) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (booking.guestName.isBlank()) "Direct Booking" else booking.guestName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = platformColors.first,
                                        contentColor = platformColors.second
                                    ) {
                                        Text(
                                            text = booking.platform,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "In: ${booking.checkInDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Out: ${getBookingCheckoutDate(booking)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "To Collect",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = currencyFormatter.format(booking.balance),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Total: ${currencyFormatter.format(booking.amountCharged)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close")
            }
        }
    )
}


private fun getBookingCheckoutDate(booking: Booking): String {
    if (booking.items.isEmpty()) {
        return getStayDate(booking.checkInDate, 1)
    }
    return booking.items.map { item ->
        val itemStart = item.startDate.takeIf { !it.isNullOrBlank() } ?: booking.checkInDate
        val nights = if (item.nights > 0) item.nights else 1
        getStayDate(itemStart, nights)
    }.maxOrNull() ?: getStayDate(booking.checkInDate, 1)
}
