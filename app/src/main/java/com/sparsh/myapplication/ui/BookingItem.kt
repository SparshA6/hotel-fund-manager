package com.sparsh.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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

@Composable
fun BookingItem(
    booking: Booking,
    currencyFormatter: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Booking", fontWeight = FontWeight.Bold) },
            text = {
                val roomsList = booking.items.map { it.roomNumber }.filter { it.isNotBlank() }
                if (roomsList.size > 1) {
                    Text("This booking contains multiple rooms: ${roomsList.joinToString(", ")}. Deleting it will remove the entire booking across all these rooms. Are you sure you want to delete the booking for ${if (booking.guestName.isBlank()) "Direct Guest" else booking.guestName}?")
                } else {
                    Text("Are you sure you want to delete the booking for ${if (booking.guestName.isBlank()) "Direct Guest" else booking.guestName}?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
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

    val statusColor = if (booking.paymentStatus.equals("Paid", ignoreCase = true)) {
        Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    } else {
        Color(0xFFFFF8E1) to Color(0xFFF57F17)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() }, // Opens edit menu when card is clicked
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Row for Guest Name & Status
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
                            text = if (booking.guestName.isBlank()) "Direct Booking" else booking.guestName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (booking.isBillOn) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "BILL",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    // Platform tag has a UNIFIED color style across all platforms
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = booking.platform,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date and Room Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Check-in: ${booking.checkInDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Payment status badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = statusColor.first),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = booking.paymentStatus,
                            color = statusColor.second,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val roomDesc = booking.items.filter { it.category == "Room" }.joinToString(", ") { "${it.category} ${it.roomNumber}" }
                        val dormList = mutableListOf<String>()
                        if (booking.dormBedsSelected > 0) {
                            if (booking.dormRoomABeds.isNotBlank()) dormList.add("Dorm A: (${booking.dormRoomABeds})")
                            if (booking.dormRoomBBeds.isNotBlank()) dormList.add("Dorm B: (${booking.dormRoomBBeds})")
                            if (dormList.isEmpty()) dormList.add("Dorm (${booking.dormBedsSelected} beds)")
                        }
                        val dormDesc = dormList.joinToString(", ")
                        val combinedDesc = listOf(roomDesc, dormDesc).filter { it.isNotBlank() }.joinToString(" | ")

                        Text(
                            text = if (combinedDesc.isEmpty()) "No allocations" else combinedDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        if (booking.expenses > 0) {
                            Text(
                                text = "Platform fee: ${currencyFormatter.format(booking.expenses)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Text(
                        text = currencyFormatter.format(booking.amountCharged),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete Booking Button (clicks are isolated from the card click)
            IconButton(
                onClick = { showDeleteConfirm = true },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Booking",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
