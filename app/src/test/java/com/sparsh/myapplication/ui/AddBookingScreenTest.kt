package com.sparsh.myapplication.ui

import com.sparsh.myapplication.Booking
import com.sparsh.myapplication.BookingItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AddBookingScreenTest {

    @Test
    fun testParseBedNumbers_singleNumbers() {
        val result = parseBedNumbers("1, 2, 3, 5", "A")
        assertEquals(listOf("A1", "A2", "A3", "A5"), result)
    }

    @Test
    fun testParseBedNumbers_ranges() {
        val result = parseBedNumbers("1-4", "A")
        assertEquals(listOf("A1", "A2", "A3", "A4"), result)
    }

    @Test
    fun testParseBedNumbers_mixedRangesAndNumbers() {
        val result = parseBedNumbers("1,2, 4-7", "B")
        assertEquals(listOf("B1", "B2", "B4", "B5", "B6", "B7"), result)
    }

    @Test
    fun testParseBedNumbers_reversedAndSpaced() {
        val result = parseBedNumbers(" 8-6,   2 ", "A")
        // "8-6" parses as range of 6..8, so A6, A7, A8. Plus A2.
        assertEquals(listOf("A6", "A7", "A8", "A2"), result)
    }

    @Test
    fun testAutoAllocation_allFree() {
        val date = "2026-06-15"
        val bookings = emptyList<Booking>()
        
        // Let's allocate 4 beds in Dorm Room A
        val counts = getDormBedBookingCounts(date, "A", bookings, null)
        val freeBeds = (1..8).filter { (counts[it] ?: 0) == 0 }.sorted()
        val bookedBeds = (1..8).filter { (counts[it] ?: 0) > 0 }.sortedWith(
            compareBy({ counts[it] ?: 0 }, { it })
        )
        
        val dormBedCount = 4
        val assignedBeds = mutableListOf<Int>()
        if (freeBeds.size >= dormBedCount) {
            assignedBeds.addAll(freeBeds.take(dormBedCount))
        } else {
            assignedBeds.addAll(freeBeds)
            val remainingNeeded = dormBedCount - freeBeds.size
            assignedBeds.addAll(bookedBeds.take(remainingNeeded))
        }
        val result = assignedBeds.sorted().map { "A$it" }
        
        assertEquals(listOf("A1", "A2", "A3", "A4"), result)
    }

    @Test
    fun testAutoAllocation_partiallyBooked() {
        val date = "2026-06-15"
        
        // Existing bookings occupy A1 and A2
        val mockBookings = listOf(
            Booking(
                checkInDate = date,
                platform = "Direct",
                guestName = "Guest 1",
                items = listOf(
                    BookingItem(category = "Dorm", roomNumber = "A1", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A2", amount = 500.0)
                ),
                isBillOn = false,
                billAmount = 1000.0,
                expenses = 0.0
            )
        )
        
        // Let's allocate 4 beds in Dorm Room A
        val counts = getDormBedBookingCounts(date, "A", mockBookings, null)
        val freeBeds = (1..8).filter { (counts[it] ?: 0) == 0 }.sorted()
        val bookedBeds = (1..8).filter { (counts[it] ?: 0) > 0 }.sortedWith(
            compareBy({ counts[it] ?: 0 }, { it })
        )
        
        val dormBedCount = 4
        val assignedBeds = mutableListOf<Int>()
        if (freeBeds.size >= dormBedCount) {
            assignedBeds.addAll(freeBeds.take(dormBedCount))
        } else {
            assignedBeds.addAll(freeBeds)
            val remainingNeeded = dormBedCount - freeBeds.size
            assignedBeds.addAll(bookedBeds.take(remainingNeeded))
        }
        val result = assignedBeds.sorted().map { "A$it" }
        
        // A1, A2 are booked, so we expect A3, A4, A5, A6 (the free beds in ascending order)
        assertEquals(listOf("A3", "A4", "A5", "A6"), result)
    }

    @Test
    fun testAutoAllocation_noBedsCompletelyFree() {
        val date = "2026-06-15"
        
        // All beds A1-A8 have at least one booking
        // A1 has 1 booking
        // A2 has 2 bookings
        // A3 has 1 booking
        // A4 has 2 bookings
        // A5 has 3 bookings
        // A6 has 1 booking
        // A7 has 1 booking
        // A8 has 2 bookings
        val mockBookings = listOf(
            Booking(
                checkInDate = date,
                platform = "Direct",
                guestName = "Guest 1",
                items = listOf(
                    BookingItem(category = "Dorm", roomNumber = "A1", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A2", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A3", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A4", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A5", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A6", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A7", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A8", amount = 500.0)
                ),
                isBillOn = false,
                billAmount = 4000.0,
                expenses = 0.0
            ),
            Booking(
                checkInDate = date,
                platform = "Direct",
                guestName = "Guest 2",
                items = listOf(
                    BookingItem(category = "Dorm", roomNumber = "A2", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A4", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A5", amount = 500.0),
                    BookingItem(category = "Dorm", roomNumber = "A8", amount = 500.0)
                ),
                isBillOn = false,
                billAmount = 2000.0,
                expenses = 0.0
            ),
            Booking(
                checkInDate = date,
                platform = "Direct",
                guestName = "Guest 3",
                items = listOf(
                    BookingItem(category = "Dorm", roomNumber = "A5", amount = 500.0)
                ),
                isBillOn = false,
                billAmount = 500.0,
                expenses = 0.0
            )
        )
        
        // Let's allocate 3 beds.
        // Booking counts:
        // A1: 1
        // A2: 2
        // A3: 1
        // A4: 2
        // A5: 3
        // A6: 1
        // A7: 1
        // A8: 2
        // Beds with lowest bookings (count=1) are: A1, A3, A6, A7.
        // In ascending order: A1, A3, A6, A7.
        // We need 3 beds, so we expect A1, A3, A6.
        val counts = getDormBedBookingCounts(date, "A", mockBookings, null)
        val freeBeds = (1..8).filter { (counts[it] ?: 0) == 0 }.sorted()
        val bookedBeds = (1..8).filter { (counts[it] ?: 0) > 0 }.sortedWith(
            compareBy({ counts[it] ?: 0 }, { it })
        )
        
        val dormBedCount = 3
        val assignedBeds = mutableListOf<Int>()
        if (freeBeds.size >= dormBedCount) {
            assignedBeds.addAll(freeBeds.take(dormBedCount))
        } else {
            assignedBeds.addAll(freeBeds)
            val remainingNeeded = dormBedCount - freeBeds.size
            assignedBeds.addAll(bookedBeds.take(remainingNeeded))
        }
        val result = assignedBeds.sorted().map { "A$it" }
        
        assertEquals(listOf("A1", "A3", "A6"), result)
    }
}
