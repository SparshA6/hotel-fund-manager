package com.sparsh.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.DateRange
import com.sparsh.myapplication.ui.AddBookingScreen
import com.sparsh.myapplication.ui.DashboardScreen
import com.sparsh.myapplication.ui.SearchScreen
import com.sparsh.myapplication.ui.QuickBookDialog
import com.sparsh.myapplication.ui.AddUnassignedBookingDialog

import com.sparsh.myapplication.ui.BookingsScreen
import com.sparsh.myapplication.ui.SettingsScreen
import com.sparsh.myapplication.ui.LoginScreen
import com.sparsh.myapplication.ui.StaffDashboardScreen
import com.sparsh.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var bookingRepository: BookingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookingRepository = BookingRepository(this)
        
        // Seeding disabled to start with a completely fresh, empty list of real bookings
        // seedMockDataIfNeeded()

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val prefs = remember { getSharedPreferences("hotel_fund_prefs", android.content.Context.MODE_PRIVATE) }
                var isLoggedIn by remember { mutableStateOf(prefs.getBoolean("is_logged_in", false)) }
                var isStaffMode by remember { mutableStateOf(prefs.getBoolean("is_staff_mode", false)) }
                val onRoleChanged = { isStaff: Boolean ->
                    isStaffMode = isStaff
                    prefs.edit().putBoolean("is_staff_mode", isStaff).apply()
                }
                val onLoginSuccess = { isStaff: Boolean ->
                    isStaffMode = isStaff
                    isLoggedIn = true
                    prefs.edit()
                        .putBoolean("is_staff_mode", isStaff)
                        .putBoolean("is_logged_in", true)
                        .apply()
                }
                val onLogout = {
                    isLoggedIn = false
                    prefs.edit().putBoolean("is_logged_in", false).apply()
                }

                val todayStr = remember {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                }

                var currentTab by remember { mutableStateOf(0) }
                var bookingToEdit by remember { mutableStateOf<Booking?>(null) }
                val bookings = remember { mutableStateOf<List<Booking>>(bookingRepository.getLocalBookings()) }
                val coroutineScope = rememberCoroutineScope()
                var isLoading by remember { mutableStateOf(true) }

                val displayedBookings = if (isStaffMode) {
                    bookings.value.filter { it.spansDate(todayStr) }
                } else {
                    bookings.value
                }
 
                LaunchedEffect(currentTab, isLoggedIn) {
                    if (isLoggedIn) {
                        bookings.value = bookingRepository.getLocalBookings()
                        isLoading = true
                        bookings.value = bookingRepository.getBookings()
                        isLoading = false
                    }
                }
 
                var isAddBookingInitialized by remember { mutableStateOf(true) }
                var isBookingsScreenInitialized by remember { mutableStateOf(true) }
                var isSearchInitialized by remember { mutableStateOf(true) }
                var showBankStatementScreen by remember { mutableStateOf(false) }

                if (!isLoggedIn) {
                    LoginScreen(onLoginSuccess = onLoginSuccess)
                } else if (isStaffMode) {
                    StaffDashboardScreen(
                        bookings = displayedBookings,
                        bookingRepository = bookingRepository,
                        onSaveBooking = { newBooking ->
                            coroutineScope.launch {
                                bookingRepository.saveBooking(newBooking)
                                bookings.value = bookingRepository.getBookings()
                            }
                        },
                        onDeleteBooking = { id, deleteIds ->
                            coroutineScope.launch {
                                bookingRepository.deleteBooking(id, deleteIds)
                                bookings.value = bookingRepository.getBookings()
                            }
                        },
                        onRefresh = {
                            coroutineScope.launch {
                                isLoading = true
                                bookings.value = bookingRepository.getBookings()
                                isLoading = false
                            }
                        },
                        onLogout = onLogout,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { 
                                    currentTab = 0 
                                    showBankStatementScreen = false
                                },
                                label = { Text("Dashboard") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Dashboard"
                                    )
                                }
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { 
                                    currentTab = 1 
                                    showBankStatementScreen = false
                                },
                                label = { Text("Chart") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Chart"
                                    )
                                }
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { 
                                    currentTab = 2 
                                    showBankStatementScreen = false
                                },
                                label = { Text("Bookings") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Bookings"
                                    )
                                }
                            )
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { 
                                    currentTab = 3 
                                    showBankStatementScreen = false
                                },
                                label = { Text("Search") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                }
                            )
                            NavigationBarItem(
                                selected = currentTab == 4,
                                onClick = { 
                                    currentTab = 4 
                                    showBankStatementScreen = false
                                },
                                label = { Text("Settings") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings"
                                    )
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Box(
                            modifier = if (currentTab == 0) Modifier.fillMaxSize() else Modifier.size(0.dp)
                        ) {
                            DashboardScreen(
                                bookings = displayedBookings,
                                onEditBooking = { booking ->
                                    bookingToEdit = booking
                                },
                                onDeleteBooking = { id, deleteIds ->
                                    coroutineScope.launch {
                                        bookingRepository.deleteBooking(id, deleteIds)
                                        bookings.value = bookingRepository.getBookings()
                                    }
                                },
                                onRefresh = {
                                    isLoading = true
                                    val delayJob = coroutineScope.launch { delay(1000) }
                                    bookings.value = bookingRepository.getBookings()
                                    delayJob.join()
                                    isLoading = false
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (isAddBookingInitialized) {
                            Box(
                                modifier = if (currentTab == 1) Modifier.fillMaxSize() else Modifier.size(0.dp)
                            ) {
                                AddBookingScreen(
                                    bookings = displayedBookings,
                                    bookingRepository = bookingRepository,
                                    onAddBooking = { newBooking ->
                                        coroutineScope.launch {
                                            bookingRepository.saveBooking(newBooking)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                    },
                                    onDeleteBooking = { id, deleteIds ->
                                        coroutineScope.launch {
                                            bookingRepository.deleteBooking(id, deleteIds)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (isBookingsScreenInitialized) {
                            Box(
                                modifier = if (currentTab == 2) Modifier.fillMaxSize() else Modifier.size(0.dp)
                            ) {
                                BookingsScreen(
                                    bookings = displayedBookings,
                                    onSaveBooking = { newBooking ->
                                        coroutineScope.launch {
                                            bookingRepository.saveBooking(newBooking)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                    },
                                    onDeleteBooking = { id, deleteIds ->
                                        coroutineScope.launch {
                                            bookingRepository.deleteBooking(id, deleteIds)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
 
                        if (isSearchInitialized) {
                            Box(
                                modifier = if (currentTab == 3) Modifier.fillMaxSize() else Modifier.size(0.dp)
                            ) {
                                SearchScreen(
                                    bookings = displayedBookings,
                                    onEditBooking = { booking ->
                                        bookingToEdit = booking
                                    },
                                    onDeleteBooking = { id, deleteIds ->
                                        coroutineScope.launch {
                                            bookingRepository.deleteBooking(id, deleteIds)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Box(
                            modifier = if (currentTab == 4) Modifier.fillMaxSize() else Modifier.size(0.dp)
                        ) {
                            SettingsScreen(
                                bookingRepository = bookingRepository,
                                isStaffMode = isStaffMode,
                                onRoleChanged = onRoleChanged,
                                onRestored = { restoredBookings ->
                                    bookings.value = restoredBookings
                                },
                                onLogout = onLogout,
                                onNavigateToBankReconciliation = {
                                    showBankStatementScreen = true
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // BankStatementScreen moved outside Scaffold innerPadding

                        if (bookingToEdit != null) {
                            if (bookingToEdit!!.isAssigned) {
                                QuickBookDialog(
                                    date = bookingToEdit!!.checkInDate,
                                    roomNumber = bookingToEdit!!.items.firstOrNull()?.roomNumber ?: "",
                                    bookings = displayedBookings,
                                    bookingToEdit = bookingToEdit,
                                    isDormMode = bookingToEdit!!.items.any { it.category == "Dorm" || it.category == "Dorm Bed" },
                                    bookingRepository = bookingRepository,
                                    onDismiss = { bookingToEdit = null },
                                    onConfirm = { updatedBooking ->
                                        coroutineScope.launch {
                                            bookingRepository.saveBooking(updatedBooking)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                        bookingToEdit = null
                                    },
                                    onSaveWithoutDismiss = { updatedBooking ->
                                        coroutineScope.launch {
                                            bookingRepository.saveBooking(updatedBooking)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                        bookingToEdit = updatedBooking
                                    },
                                    onDelete = { id, deleteIds ->
                                        coroutineScope.launch {
                                            bookingRepository.deleteBooking(id, deleteIds)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                        bookingToEdit = null
                                    }
                                )
                            } else {
                                AddUnassignedBookingDialog(
                                    bookingToEdit = bookingToEdit,
                                    bookingRepository = bookingRepository,
                                    onDismiss = { bookingToEdit = null },
                                    onConfirm = { updatedBooking ->
                                        coroutineScope.launch {
                                            bookingRepository.saveBooking(updatedBooking)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                        bookingToEdit = null
                                    },
                                    onSaveWithoutDismiss = { updatedBooking ->
                                        coroutineScope.launch {
                                            bookingRepository.saveBooking(updatedBooking)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                        bookingToEdit = updatedBooking
                                    }
                                )
                            }
                        }

                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                            )
                        }
                    }

                        // BankStatementScreen as full-screen overlay outside Scaffold
                        if (showBankStatementScreen) {
                            androidx.activity.compose.BackHandler(enabled = true) {
                                showBankStatementScreen = false
                            }
                            com.sparsh.myapplication.ui.BankStatementScreen(
                                bookingRepository = bookingRepository,
                                onBack = { showBankStatementScreen = false },
                                onSelectBookingId = { bookingId ->
                                    val matchedB = bookings.value.find { it.id == bookingId }
                                    if (matchedB != null) {
                                        bookingToEdit = matchedB
                                    } else {
                                        android.widget.Toast.makeText(this@MainActivity, "Booking not found", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun seedMockDataIfNeeded() {
        val prefs = getSharedPreferences("hotel_fund_prefs", android.content.Context.MODE_PRIVATE)
        val isSeeded = prefs.getBoolean("is_seeded", false)
        if (!isSeeded) {
            val mock1 = Booking(
                guestName = "Amit Patel",
                checkInDate = "2026-06-14",
                platform = "MMT",
                items = listOf(
                    BookingItem(category = "Room", roomNumber = "102", amount = 3250.0),
                    BookingItem(category = "Room", roomNumber = "103", amount = 3250.0)
                ),
                isBillOn = false,
                billAmount = 6500.0,
                expenses = 975.0, // 15% OTA commission
                payments = listOf(PaymentDetail(amount = 6500.0, method = "UPI (Hotel Acc - GPay)")),
                notes = "Pre-paid booking via MMT. Requested extra towels."
            )
            val mock2 = Booking(
                guestName = "Priya Sen",
                checkInDate = "2026-06-15",
                platform = "Booking.com",
                items = listOf(
                    BookingItem(category = "Room", roomNumber = "205", amount = 3800.0)
                ),
                isBillOn = false,
                billAmount = 3800.0,
                expenses = 570.0, // 15% commission
                payments = emptyList(),
                notes = "Will pay at counter during checkout."
            )
            val mock3 = Booking(
                guestName = "John Doe",
                checkInDate = "2026-06-12",
                platform = "Direct",
                items = listOf(
                    BookingItem(category = "Room", roomNumber = "110", amount = 3200.0)
                ),
                isBillOn = true,
                billAmount = 3000.0, // Custom bill total showing discount
                expenses = 0.0,
                payments = listOf(PaymentDetail(amount = 3000.0, method = "Cash")),
                notes = "Corporate discount applied. Custom bill amount of 3000.0."
            )
            val mock4 = Booking(
                guestName = "Direct Guest",
                checkInDate = "2026-06-18",
                platform = "Direct",
                items = emptyList(),
                dormBedsSelected = 4,
                dormRoomABeds = "1-3",
                dormRoomBBeds = "5",
                dormTotalAmount = 2400.0,
                isBillOn = false,
                billAmount = 2400.0,
                expenses = 0.0,
                payments = listOf(PaymentDetail(amount = 2400.0, method = "Bank Transfer")),
                notes = "Group check-in for dorm beds. 3 beds in Room A and 1 bed in Room B."
            )
            bookingRepository.saveBooking(mock1)
            bookingRepository.saveBooking(mock2)
            bookingRepository.saveBooking(mock3)
            bookingRepository.saveBooking(mock4)
            prefs.edit().putBoolean("is_seeded", true).apply()
        }
    }
}