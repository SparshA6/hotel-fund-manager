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
import androidx.compose.material3.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sparsh.myapplication.ui.AddBookingScreen
import com.sparsh.myapplication.ui.DashboardScreen
import com.sparsh.myapplication.ui.SearchScreen
import com.sparsh.myapplication.ui.QuickBookDialog
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
                var currentTab by remember { mutableStateOf(0) }
                var bookingToEdit by remember { mutableStateOf<Booking?>(null) }
                val bookings = remember { mutableStateOf<List<Booking>>(emptyList()) }
                val coroutineScope = rememberCoroutineScope()
                var isLoading by remember { mutableStateOf(true) }

                LaunchedEffect(currentTab) {
                    if (currentTab == 0) {
                        isLoading = true
                        bookings.value = bookingRepository.getBookings()
                        isLoading = false
                    }
                }

                var isAddBookingInitialized by remember { mutableStateOf(false) }
                var isSearchInitialized by remember { mutableStateOf(false) }

                if (currentTab == 1) isAddBookingInitialized = true
                if (currentTab == 2) isSearchInitialized = true

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { 
                                    currentTab = 0 
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
                                },
                                label = { Text("Add Booking") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Booking"
                                    )
                                }
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { 
                                    currentTab = 2 
                                },
                                label = { Text("Search") },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
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
                                bookings = bookings.value,
                                onEditBooking = { booking ->
                                    bookingToEdit = booking
                                },
                                onDeleteBooking = { id ->
                                    coroutineScope.launch {
                                        bookingRepository.deleteBooking(id)
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
                                    bookings = bookings.value,
                                    onAddBooking = { newBooking ->
                                        coroutineScope.launch {
                                            bookingRepository.saveBooking(newBooking)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                    },
                                    onDeleteBooking = { id ->
                                        coroutineScope.launch {
                                            bookingRepository.deleteBooking(id)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (isSearchInitialized) {
                            Box(
                                modifier = if (currentTab == 2) Modifier.fillMaxSize() else Modifier.size(0.dp)
                            ) {
                                SearchScreen(
                                    bookings = bookings.value,
                                    onEditBooking = { booking ->
                                        bookingToEdit = booking
                                    },
                                    onDeleteBooking = { id ->
                                        coroutineScope.launch {
                                            bookingRepository.deleteBooking(id)
                                            bookings.value = bookingRepository.getBookings()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (bookingToEdit != null) {
                            QuickBookDialog(
                                date = bookingToEdit!!.checkInDate,
                                roomNumber = bookingToEdit!!.items.firstOrNull()?.roomNumber ?: "",
                                bookings = bookings.value,
                                bookingToEdit = bookingToEdit,
                                isDormMode = bookingToEdit!!.items.any { it.category == "Dorm" },
                                onDismiss = { bookingToEdit = null },
                                onConfirm = { updatedBooking ->
                                    coroutineScope.launch {
                                        bookingRepository.saveBooking(updatedBooking)
                                        bookings.value = bookingRepository.getBookings()
                                    }
                                    bookingToEdit = null
                                },
                                onDelete = { id ->
                                    coroutineScope.launch {
                                        bookingRepository.deleteBooking(id)
                                        bookings.value = bookingRepository.getBookings()
                                    }
                                    bookingToEdit = null
                                }
                            )
                        }

                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
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
                paymentStatus = "Paid",
                paymentMethod = "UPI",
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
                paymentStatus = "Pending",
                paymentMethod = "Card",
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
                paymentStatus = "Paid",
                paymentMethod = "Cash",
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
                paymentStatus = "Paid",
                paymentMethod = "Bank Transfer",
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