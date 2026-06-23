package com.sparsh.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sparsh.myapplication.Booking
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    bookings: List<Booking>,
    onEditBooking: (Booking) -> Unit,
    onDeleteBooking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatformFilter by remember { mutableStateOf<String?>(null) }

    val filteredBookings = remember(bookings, searchQuery, selectedPlatformFilter) {
        bookings.filter { booking ->
            val matchesSearch = searchQuery.isBlank() || 
                    booking.guestName.contains(searchQuery, ignoreCase = true) ||
                    booking.items.any { it.roomNumber.contains(searchQuery, ignoreCase = true) } ||
                    booking.dormRoomABeds.contains(searchQuery, ignoreCase = true) ||
                    booking.dormRoomBBeds.contains(searchQuery, ignoreCase = true)
            
            val matchesPlatform = selectedPlatformFilter == null || 
                    booking.platform.equals(selectedPlatformFilter, ignoreCase = true)
                    
            matchesSearch && matchesPlatform
        }
    }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Search & Filter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Guest Name, Room or Beds") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Platform Filter Row
            Text(
                "Filter by Platform",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val platforms = listOf("Direct", "MMT", "Booking.com", "Agoda", "Goibibo", "Cleartrip")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                platforms.forEach { platformName ->
                    val isSelected = selectedPlatformFilter == platformName
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedPlatformFilter = if (isSelected) null else platformName
                        },
                        label = { Text(platformName) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Search Results (${filteredBookings.size})", 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (filteredBookings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching bookings found.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(filteredBookings, key = { it.id }) { booking ->
                    BookingItem(
                        booking = booking,
                        currencyFormatter = currencyFormatter,
                        onEdit = { onEditBooking(booking) },
                        onDelete = { onDeleteBooking(booking.id) }
                    )
                }
            }
        }
    }
}
