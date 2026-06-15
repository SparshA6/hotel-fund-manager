package com.sparsh.myapplication

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BookingItem(
    val id: String = UUID.randomUUID().toString(),
    val category: String, // "Room" or "Dorm"
    val roomNumber: String,
    val amount: Double
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("category", category)
        json.put("roomNumber", roomNumber)
        json.put("amount", amount)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): BookingItem {
            return BookingItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                category = json.getString("category"),
                roomNumber = json.getString("roomNumber"),
                amount = json.getDouble("amount")
            )
        }
    }
}

data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val checkInDate: String,
    val platform: String,
    val guestName: String,
    val items: List<BookingItem>, // Now stores only Room category items
    val dormBedsSelected: Int = 0,
    val dormRoomABeds: String = "",
    val dormRoomBBeds: String = "",
    val dormTotalAmount: Double = 0.0,
    val isBillOn: Boolean,
    val billAmount: Double,
    val expenses: Double,
    val paymentStatus: String,
    val paymentMethod: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val amountCharged: Double
        get() = if (isBillOn) billAmount else (items.filter { it.category == "Room" }.sumOf { it.amount } + dormTotalAmount)

    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("checkInDate", checkInDate)
        json.put("platform", platform)
        json.put("guestName", guestName)
        
        val itemsArray = JSONArray()
        items.forEach { itemsArray.put(it.toJsonObject()) }
        json.put("items", itemsArray)
        
        json.put("dormBedsSelected", dormBedsSelected)
        json.put("dormRoomABeds", dormRoomABeds)
        json.put("dormRoomBBeds", dormRoomBBeds)
        json.put("dormTotalAmount", dormTotalAmount)
        
        json.put("isBillOn", isBillOn)
        json.put("billAmount", billAmount)
        json.put("amountCharged", amountCharged)
        
        json.put("expenses", expenses)
        json.put("paymentStatus", paymentStatus)
        json.put("paymentMethod", paymentMethod)
        json.put("notes", notes)
        json.put("timestamp", timestamp)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Booking {
            val itemsList = mutableListOf<BookingItem>()
            if (json.has("items")) {
                val itemsArray = json.getJSONArray("items")
                for (i in 0 until itemsArray.length()) {
                    itemsList.add(BookingItem.fromJsonObject(itemsArray.getJSONObject(i)))
                }
            } else if (json.has("roomNumbers")) {
                // Map old schema objects for backward compatibility
                val roomsArray = json.getJSONArray("roomNumbers")
                val amountCharged = json.optDouble("amountCharged", 0.0)
                val individualRate = if (roomsArray.length() > 0) amountCharged / roomsArray.length() else 0.0
                for (i in 0 until roomsArray.length()) {
                    itemsList.add(
                        BookingItem(
                            category = "Room",
                            roomNumber = roomsArray.getString(i),
                            amount = individualRate
                        )
                    )
                }
            }

            val dormBedsSelected = json.optInt("dormBedsSelected", 0)
            val dormRoomABeds = json.optString("dormRoomABeds", "")
            val dormRoomBBeds = json.optString("dormRoomBBeds", "")
            val dormTotalAmount = json.optDouble("dormTotalAmount", 0.0)

            val isBillOn = json.optBoolean("isBillOn", false)
            val defaultBillAmount = json.optDouble("amountCharged", 0.0)

            return Booking(
                id = json.optString("id", UUID.randomUUID().toString()),
                checkInDate = json.getString("checkInDate"),
                platform = json.getString("platform"),
                guestName = json.optString("guestName", "Direct Guest"),
                items = itemsList,
                dormBedsSelected = dormBedsSelected,
                dormRoomABeds = dormRoomABeds,
                dormRoomBBeds = dormRoomBBeds,
                dormTotalAmount = dormTotalAmount,
                isBillOn = isBillOn,
                billAmount = json.optDouble("billAmount", defaultBillAmount),
                expenses = json.optDouble("expenses", 0.0),
                paymentStatus = json.getString("paymentStatus"),
                paymentMethod = json.getString("paymentMethod"),
                notes = json.optString("notes", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}
