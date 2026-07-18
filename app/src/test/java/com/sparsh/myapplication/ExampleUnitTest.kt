package com.sparsh.myapplication

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testBookingSerialization() {
        val booking = Booking(
            checkInDate = "2026-06-17",
            platform = "Direct",
            guestName = "John Doe",
            items = emptyList(),
            isBillOn = false,
            billAmount = 0.0,
            expenses = 0.0,
            payments = emptyList()
        )
        val gson = com.google.gson.GsonBuilder()
            .registerTypeAdapter(Booking::class.java, com.google.gson.JsonSerializer<Booking> { src, _, _ ->
                val obj = com.google.gson.JsonObject()
                obj.addProperty("id", src.id)
                obj.addProperty("checkInDate", src.checkInDate)
                obj.addProperty("platform", src.platform)
                obj.addProperty("guestName", src.guestName)
                obj.addProperty("paymentStatus", src.paymentStatus)
                obj.addProperty("paymentMethod", src.paymentMethod)
                obj
            })
            .create()
        val json = gson.toJson(booking)
        println("SERIALIZED BOOKING JSON: $json")
        assertTrue(json.contains("paymentStatus"))
        assertTrue(json.contains("paymentMethod"))
    }

    /*
    @Test
    fun testMockBookingsCloudSync() {
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
            expenses = 975.0,
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
            expenses = 570.0,
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
            billAmount = 3000.0,
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

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://hotel-fund-manager.onrender.com/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        val api = retrofit.create(BookingApi::class.java)

        val mocks = listOf(mock1, mock2, mock3, mock4)
        for ((idx, mock) in mocks.withIndex()) {
            try {
                println("Posting mock${idx + 1}...")
                val result = kotlinx.coroutines.runBlocking { api.saveBooking(mock) }
                println("Successfully saved mock${idx + 1}: ${result.id}")
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                System.err.println("Failed to save mock${idx + 1}: HTTP ${e.code()} - $errorBody")
                fail("Failed to save mock${idx + 1} due to server error")
            } catch (e: Exception) {
                System.err.println("Failed to save mock${idx + 1}: ${e.message}")
                fail("Failed to save mock${idx + 1} due to network error")
            }
        }
    }
    */
}