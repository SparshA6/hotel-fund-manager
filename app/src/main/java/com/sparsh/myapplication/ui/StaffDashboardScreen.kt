package com.sparsh.myapplication.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sparsh.myapplication.Booking
import com.sparsh.myapplication.BookingRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDashboardScreen(
    bookings: List<Booking>,
    bookingRepository: BookingRepository,
    onSaveBooking: (Booking) -> Unit,
    onDeleteBooking: (String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val displayDate = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(Date()) }

    var bookingToEditInDialog by remember { mutableStateOf<Booking?>(null) }
    var showQuickBookForNew by remember { mutableStateOf(false) }
    var bookingForIdUpload by remember { mutableStateOf<Booking?>(null) }

    // Calculate metrics
    val checkInsToday = bookings.count { it.checkInDate == todayStr }
    val checkOutsToday = bookings.count { it.checkOutDate == todayStr }
    val missingIds = bookings.count { it.idDocumentUrl.isBlank() }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Staff Desk Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            text = displayDate,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickBookForNew = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Walk-in")
                    Text("Walk-in", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(backgroundGradient)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Stats summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Check-ins stats
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = checkInsToday.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Check-ins Today",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        // Check-outs stats
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = checkOutsToday.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Check-outs Today",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        // Missing IDs stats
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = missingIds.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (missingIds > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            )
                            Text(
                                text = "Missing Guest IDs",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bookings title
            item {
                Text(
                    text = "Active Today's Bookings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (bookings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active bookings for today.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(bookings) { booking ->
                    val hasId = booking.idDocumentUrl.isNotBlank()
                    val balance = booking.balance
                    val isPaid = balance <= 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Booking Title and Room number
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = booking.guestName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val allocs = booking.items.map {
                                        if (it.category.startsWith("Dorm")) "Bed ${it.roomNumber}" else "Room ${it.roomNumber}"
                                    }.joinToString(", ")
                                    Text(
                                        text = allocs,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Badges
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // ID Badge
                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                text = if (hasId) "ID Verified" else "Missing ID",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            labelColor = if (hasId) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                            containerColor = if (hasId) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer
                                        ),
                                        border = BorderStroke(0.dp, Color.Transparent)
                                    )
                                    
                                    // Payment Badge
                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                text = if (isPaid) "Paid" else "Due: ₹${balance.toInt()}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            labelColor = if (isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                            containerColor = if (isPaid) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer
                                        ),
                                        border = BorderStroke(0.dp, Color.Transparent)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Check-in and out info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Check-in", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(booking.checkInDate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Check-out", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(booking.checkOutDate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            // Quick Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Direct ID Upload button
                                OutlinedButton(
                                    onClick = { bookingForIdUpload = booking },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (hasId) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (hasId) Icons.Default.CheckCircle else Icons.Default.AccountBox,
                                            contentDescription = "Scan ID",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(if (hasId) "Update ID" else "Scan ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Quick Edit/Record Payment Button
                                Button(
                                    onClick = { bookingToEditInDialog = booking },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit details",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Edit Desk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ID verification dialog
    if (bookingForIdUpload != null) {
        Dialog(onDismissRequest = { bookingForIdUpload = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                        Text(
                            text = "Guest ID Verification",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { bookingForIdUpload = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    Text(
                        text = "Scan or upload an official ID document for guest ${bookingForIdUpload!!.guestName}.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IdDocumentUploadSection(
                        booking = bookingForIdUpload!!,
                        bookingRepository = bookingRepository,
                        onBookingUpdated = { updated ->
                            bookingForIdUpload = updated
                            onRefresh() // Refresh dashboard state
                        }
                    )
                }
            }
        }
    }

    // Walk-in booking creation dialog
    if (showQuickBookForNew) {
        QuickBookDialog(
            date = todayStr,
            roomNumber = "",
            bookings = bookings,
            bookingToEdit = null,
            isDormMode = false,
            bookingRepository = bookingRepository,
            onDismiss = { showQuickBookForNew = false },
            onConfirm = { newBooking ->
                onSaveBooking(newBooking)
                showQuickBookForNew = false
            }
        )
    }

    // Full booking edit dialog
    if (bookingToEditInDialog != null) {
        QuickBookDialog(
            date = bookingToEditInDialog!!.checkInDate,
            roomNumber = bookingToEditInDialog!!.items.firstOrNull()?.roomNumber ?: "",
            bookings = bookings,
            bookingToEdit = bookingToEditInDialog,
            isDormMode = bookingToEditInDialog!!.items.any { it.category == "Dorm" || it.category == "Dorm Bed" },
            bookingRepository = bookingRepository,
            onDismiss = { bookingToEditInDialog = null },
            onConfirm = { updatedBooking ->
                onSaveBooking(updatedBooking)
                bookingToEditInDialog = null
            },
            onDelete = { id ->
                onDeleteBooking(id)
                bookingToEditInDialog = null
            },
            onSaveWithoutDismiss = { updatedBooking ->
                onSaveBooking(updatedBooking)
                bookingToEditInDialog = updatedBooking
            }
        )
    }
}
