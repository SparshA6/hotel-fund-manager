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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.sparsh.myapplication.OtherPayment
import com.sparsh.myapplication.BookingRepository
import kotlinx.coroutines.launch

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
    onDeleteBooking: (String, Boolean) -> Unit,
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { BookingRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var otherPayments by remember { mutableStateOf<List<OtherPayment>>(emptyList()) }
    var showOtherPaymentDialog by remember { mutableStateOf(false) }
    var editingOtherPayment by remember { mutableStateOf<OtherPayment?>(null) }

    fun loadOtherPayments() {
        coroutineScope.launch {
            otherPayments = repository.getOtherPayments()
        }
    }

    LaunchedEffect(Unit) {
        otherPayments = repository.getLocalOtherPayments()
        loadOtherPayments()
    }

    val pullToRefreshState = rememberPullToRefreshState()
    var selectedBookingForAssignment by remember { mutableStateOf<Booking?>(null) }
    var showPendingPaymentsReport by remember { mutableStateOf(false) }

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.TODAY) }
    var calendarOffset by remember(selectedPeriod) { mutableStateOf(0) }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
            loadOtherPayments()
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

    val currentDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date()) }
    val defaultOtherPaymentDate = remember(selectedPeriod, targetDateStr, currentDateStr) {
        if (selectedPeriod == ReportPeriod.TODAY) {
            targetDateStr
        } else {
            currentDateStr
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
        if (b.platform.equals("Blocked", ignoreCase = true)) {
            return Triple(0.0, 0.0, 0.0)
        }
        var periodGross = 0.0
        var periodExpense = 0.0

        val bookingExpense = if (b.expenses > 0.0) {
            b.expenses
        } else if (!b.platform.equals("Direct", ignoreCase = true)) {
            val commBase = (b.amountCharged).coerceAtLeast(0.0)
            com.sparsh.myapplication.SettingsManager.calculateBreakdown(context, b.platform, commBase).totalDeductions
        } else {
            0.0
        }

        if (b.items.isEmpty()) {
            // Fallback for bookings without items (dorm legacy)
            if (datePredicate(b.checkInDate)) {
                periodGross = b.amountCharged
                periodExpense = bookingExpense
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
                        
                        // Scale gross by total booking amount ratio
                        val scaledGross = dailyRate * (b.amountCharged / sumOfItemAmounts)
                        
                        // Scale expense by rate proportion
                        val scaledExpense = bookingExpense * (dailyRate / sumOfItemAmounts)
                        
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

    val periodFinancials = remember(bookings, otherPayments, datePredicate) {
        var gross = 0.0
        var expense = 0.0
        bookings.forEach { b ->
            val financials = getBookingFinancialsForPeriod(b)
            gross += financials.first
            expense += financials.second
        }
        otherPayments.forEach { op ->
            if (datePredicate(op.date)) {
                gross += op.amount
            }
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
    val platforms = listOf("Direct", "MMT", "Booking.com", "Agoda", "Goibibo", "Yatra", "Cleartrip")
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

    // Account summary breakdown based on payments in period
    val standardAccounts = listOf(
        "UPI (Hotel Acc - GPay)",
        "UPI (Sparsh Acc - GPay)",
        "UPI (Meenu - PhonePe)",
        "UPI (Shop - HDFC)",
        "Cash",
        "Card",
        "Bank Transfer"
    )

    data class AccountSummaryItem(
        val name: String,
        val amount: Double,
        val count: Int
    )

    val accountSummaries = remember(bookings, otherPayments, datePredicate) {
        val summaryMap = mutableMapOf<String, Pair<Double, Int>>()
        
        standardAccounts.forEach { acc ->
            summaryMap[acc] = Pair(0.0, 0)
        }

        bookings.forEach { b ->
            b.payments.forEach { p ->
                val pDateStr = if (p.timestamp > 0) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(p.timestamp))
                } else {
                    b.checkInDate
                }
                if (datePredicate(pDateStr)) {
                    val method = if (p.method.isNotBlank()) p.method else "Other"
                    val current = summaryMap.getOrDefault(method, Pair(0.0, 0))
                    summaryMap[method] = Pair(current.first + p.amount, current.second + 1)
                }
            }
        }

        otherPayments.forEach { op ->
            if (datePredicate(op.date)) {
                val method = if (op.method.isNotBlank()) op.method else "Other"
                val current = summaryMap.getOrDefault(method, Pair(0.0, 0))
                summaryMap[method] = Pair(current.first + op.amount, current.second + 1)
            }
        }

        summaryMap.map { (name, data) -> AccountSummaryItem(name, data.first, data.second) }
            .sortedWith(compareByDescending<AccountSummaryItem> { it.amount }.thenBy { standardAccounts.indexOf(it.name).let { idx -> if (idx == -1) 999 else idx } })
    }

    val totalAccountPaymentsAmount = remember(accountSummaries) {
        accountSummaries.sumOf { it.amount }
    }

    val maxAccountAmount = remember(accountSummaries) {
        accountSummaries.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
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
                                    "Yatra" -> Color(0xFFE11D48) // Crimson Red
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

                // Account Summary Breakdown Card
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Account Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Payments received per account in period",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = currencyFormatter.format(totalAccountPaymentsAmount),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (accountSummaries.all { it.amount == 0.0 }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No payments recorded in this period",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            } else {
                                accountSummaries.filter { it.amount > 0.0 }.forEach { item ->
                                    val fraction = (item.amount / maxAccountAmount).toFloat().coerceIn(0f, 1f)

                                    val accountColor = when {
                                        item.name.contains("Hotel Acc", ignoreCase = true) -> Color(0xFF1D4ED8) // Deep Blue
                                        item.name.contains("Sparsh", ignoreCase = true) -> Color(0xFF0288D1) // Light Blue
                                        item.name.contains("Meenu", ignoreCase = true) -> Color(0xFF7E22CE) // Purple
                                        item.name.contains("Shop", ignoreCase = true) || item.name.contains("HDFC", ignoreCase = true) -> Color(0xFF0D9488) // Teal
                                        item.name.equals("Cash", ignoreCase = true) -> Color(0xFF15803D) // Green
                                        item.name.equals("Card", ignoreCase = true) -> Color(0xFFC2410C) // Orange
                                        item.name.contains("Bank", ignoreCase = true) -> Color(0xFF4338CA) // Indigo
                                        item.name.contains("Portal", ignoreCase = true) -> Color(0xFFD97706) // Amber
                                        else -> MaterialTheme.colorScheme.secondary
                                    }

                                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(accountColor, shape = RoundedCornerShape(5.dp))
                                                )
                                                Text(
                                                    text = item.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            Text(
                                                text = "${item.count} pmt${if (item.count > 1) "s" else ""} • ${currencyFormatter.format(item.amount)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction)
                                                    .fillMaxHeight()
                                                    .background(
                                                        accountColor,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Other Payments Card
                item {
                    val activeOtherPayments = remember(otherPayments, datePredicate) {
                        otherPayments.filter { datePredicate(it.date) }
                    }

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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Other Payments",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Non-booking payments & income",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        editingOtherPayment = null
                                        showOtherPaymentDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (activeOtherPayments.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No other payments recorded in this period",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            } else {
                                activeOtherPayments.forEachIndexed { index, payment ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = payment.method,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = payment.date,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (payment.reason.isNotBlank()) {
                                                Text(
                                                    text = payment.reason,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = currencyFormatter.format(payment.amount),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF10B981)
                                            )
                                            IconButton(
                                                onClick = {
                                                    editingOtherPayment = payment
                                                    showOtherPaymentDialog = true
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        repository.deleteOtherPayment(payment.id)
                                                        loadOtherPayments()
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
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
                            onDelete = { deleteIds -> onDeleteBooking(booking.id, deleteIds) },
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

            if (showOtherPaymentDialog) {
                OtherPaymentDialog(
                    initialPayment = editingOtherPayment,
                    defaultDate = defaultOtherPaymentDate,
                    standardAccounts = standardAccounts,
                    onDismiss = {
                        showOtherPaymentDialog = false
                        editingOtherPayment = null
                    },
                    onSave = { paymentToSave ->
                        coroutineScope.launch {
                            repository.saveOtherPayment(paymentToSave)
                            loadOtherPayments()
                            showOtherPaymentDialog = false
                            editingOtherPayment = null
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherPaymentDialog(
    initialPayment: OtherPayment?,
    defaultDate: String,
    standardAccounts: List<String>,
    onDismiss: () -> Unit,
    onSave: (OtherPayment) -> Unit
) {
    var amountStr by remember { mutableStateOf(initialPayment?.amount?.let { if (it == 0.0) "" else if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var selectedMethod by remember { mutableStateOf(initialPayment?.method ?: standardAccounts.firstOrNull() ?: "UPI (Hotel Acc - GPay)") }
    var selectedDate by remember { mutableStateOf(initialPayment?.date?.ifBlank { defaultDate } ?: defaultDate) }
    var reason by remember { mutableStateOf(initialPayment?.reason ?: "") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        if (selectedDate.isNotBlank()) {
            try {
                val parts = selectedDate.split("-")
                if (parts.size == 3) {
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialPayment == null) "Add Other Payment" else "Edit Other Payment",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Price Input
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        amountError = null
                    },
                    label = { Text("Amount / Price (₹)*") },
                    placeholder = { Text("e.g. 1500") },
                    prefix = { Text("₹ ") },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Date Picker Input
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Date*") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Account / CC Dropdown
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = !methodExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account / CC*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        standardAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc) },
                                onClick = {
                                    selectedMethod = acc
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                // Reason / Notes Input (can be empty)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Remarks (Optional)") },
                    placeholder = { Text("e.g. Laundry sales, Shop item, etc.") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountStr.toDoubleOrNull()
                    if (amountVal == null || amountVal <= 0.0) {
                        amountError = "Please enter a valid positive amount"
                        return@Button
                    }
                    val paymentToSave = (initialPayment ?: OtherPayment()).copy(
                        amount = amountVal,
                        method = selectedMethod,
                        date = selectedDate,
                        reason = reason.trim()
                    )
                    onSave(paymentToSave)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (initialPayment == null) "Add Payment" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
