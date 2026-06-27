package com.sparsh.myapplication

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

fun getStayDate(checkInDate: String, offsetDays: Int): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val date = parser.parse(checkInDate)
        if (date != null) {
            cal.time = date
            cal.add(Calendar.DAY_OF_MONTH, offsetDays)
            parser.format(cal.time)
        } else {
            checkInDate
        }
    } catch (e: Exception) {
        checkInDate
    }
}

fun datesOverlap(date1: String, nights1: Int, date2: String, nights2: Int): Boolean {
    val len1 = if (nights1 > 0) nights1 else 1
    val len2 = if (nights2 > 0) nights2 else 1
    val dates1 = (0 until len1).map { getStayDate(date1, it) }.toSet()
    val dates2 = (0 until len2).map { getStayDate(date2, it) }.toSet()
    return dates1.intersect(dates2).isNotEmpty()
}

fun getDateOffset(startDate: String, currentDate: String): Int {
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val start = parser.parse(startDate)
        val current = parser.parse(currentDate)
        if (start != null && current != null) {
            val diffMs = current.time - start.time
            val diffDays = diffMs.toDouble() / (1000 * 60 * 60 * 24)
            kotlin.math.round(diffDays).toInt()
        } else {
            0
        }
    } catch (e: Exception) {
        0
    }
}

fun getRoomNumberForNight(roomNumber: String, offset: Int): String {
    if (roomNumber.isBlank()) return ""
    val parts = roomNumber.split(",")
    return parts.getOrNull(offset) ?: parts.firstOrNull() ?: ""
}

fun roomConflictExists(checkIn1: String, nights1: Int, roomNumber1: String, checkIn2: String, nights2: Int, roomNumber2: String): Boolean {
    val len1 = if (nights1 > 0) nights1 else 1
    val len2 = if (nights2 > 0) nights2 else 1
    for (i in 0 until len1) {
        val date1 = getStayDate(checkIn1, i)
        val r1 = getRoomNumberForNight(roomNumber1, i)
        if (r1.isBlank()) continue
        
        for (j in 0 until len2) {
            val date2 = getStayDate(checkIn2, j)
            val r2 = getRoomNumberForNight(roomNumber2, j)
            if (r2.isBlank()) continue
            
            if (date1 == date2 && r1 == r2) {
                return true
            }
        }
    }
    return false
}

data class BookingItem(
    val id: String = UUID.randomUUID().toString(),
    val category: String, // E.g., "Room", "Dorm", or room categories "Standard", "Deluxe", "Double", "Family", "Deluxe Family", "Dorm Bed"
    val roomNumber: String = "", // empty if unassigned, e.g., "101"
    val amount: Double,
    val nights: Int = 1,
    val rates: List<Double> = emptyList(),
    val startDate: String? = null
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        val ratesStr = rates.joinToString(",") { it.toString() }
        val encodedCategory = if (startDate != null && startDate.isNotBlank()) {
            "$category|$nights|$ratesStr|$startDate"
        } else {
            "$category|$nights|$ratesStr"
        }
        json.put("category", encodedCategory)
        json.put("roomNumber", if (roomNumber.isBlank()) " " else roomNumber)
        json.put("amount", amount)
        json.put("nights", nights)
        val ratesArray = JSONArray()
        rates.forEach { ratesArray.put(it) }
        json.put("rates", ratesArray)
        json.put("startDate", startDate ?: "")
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): BookingItem {
            val rawCategory = json.getString("category")
            var categoryVal = rawCategory
            var nightsVal = json.optInt("nights", 1)
            val ratesList = mutableListOf<Double>()
            var startDateVal: String? = json.optString("startDate", "").takeIf { it.isNotBlank() }

            if (rawCategory.contains("|")) {
                val parts = rawCategory.split("|")
                categoryVal = parts[0]
                if (parts.size > 1) {
                    nightsVal = parts[1].toIntOrNull() ?: nightsVal
                }
                if (parts.size > 2 && parts[2].isNotEmpty()) {
                    val rateParts = parts[2].split(",")
                    ratesList.clear()
                    rateParts.forEach {
                        it.toDoubleOrNull()?.let { r -> ratesList.add(r) }
                    }
                }
                if (parts.size > 3 && parts[3].isNotEmpty()) {
                    startDateVal = parts[3]
                }
            }

            if (ratesList.isEmpty()) {
                if (json.has("rates")) {
                    val arr = json.getJSONArray("rates")
                    for (i in 0 until arr.length()) {
                        ratesList.add(arr.getDouble(i))
                    }
                } else {
                    val baseAmount = json.optDouble("amount", 0.0)
                    if (nightsVal > 0) {
                        val ratePerNight = baseAmount / nightsVal
                        repeat(nightsVal) {
                            ratesList.add(ratePerNight)
                        }
                    }
                }
            }

            return BookingItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                category = categoryVal,
                roomNumber = json.optString("roomNumber", "").let { if (it.isBlank()) "" else it },
                amount = json.getDouble("amount"),
                nights = nightsVal,
                rates = ratesList,
                startDate = startDateVal
            )
        }
    }
}

data class PaymentDetail(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val method: String, // "UPI", "Cash", "Card", "Bank Transfer", etc.
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("amount", amount)
        json.put("method", method)
        json.put("timestamp", timestamp)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): PaymentDetail {
            return PaymentDetail(
                id = json.optString("id", UUID.randomUUID().toString()),
                amount = json.getDouble("amount"),
                method = json.getString("method"),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

data class GuestIdImage(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val fileId: String = "",
    val label: String = ""
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("url", url)
        json.put("fileId", fileId)
        json.put("label", label)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): GuestIdImage {
            return GuestIdImage(
                id = json.optString("id", UUID.randomUUID().toString()),
                url = json.getString("url"),
                fileId = json.optString("fileId", ""),
                label = json.optString("label", "")
            )
        }
    }
}

data class GuestIdCard(
    val id: String = UUID.randomUUID().toString(),
    val idType: String,
    val guestName: String = "",
    val images: List<GuestIdImage> = emptyList()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("idType", idType)
        json.put("guestName", guestName)
        val imagesArray = JSONArray()
        images.forEach { imagesArray.put(it.toJsonObject()) }
        json.put("images", imagesArray)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): GuestIdCard {
            val imgList = mutableListOf<GuestIdImage>()
            if (json.has("images")) {
                val array = json.getJSONArray("images")
                for (i in 0 until array.length()) {
                    imgList.add(GuestIdImage.fromJsonObject(array.getJSONObject(i)))
                }
            }
            return GuestIdCard(
                id = json.optString("id", UUID.randomUUID().toString()),
                idType = json.getString("idType"),
                guestName = json.optString("guestName", ""),
                images = imgList
            )
        }
    }
}

fun generateBookingId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val randomPart = (1..5).map { chars.random() }.joinToString("")
    val dateStr = java.text.SimpleDateFormat("yyMMdd", java.util.Locale.US).format(java.util.Date())
    return "BK-$dateStr-$randomPart"
}

data class Booking(
    val id: String = generateBookingId(),
    val checkInDate: String,
    val platform: String,
    val guestName: String,
    val items: List<BookingItem>,
    val dormBedsSelected: Int = 0,
    val dormRoomABeds: String = "",
    val dormRoomBBeds: String = "",
    val dormTotalAmount: Double = 0.0,
    val isBillOn: Boolean,
    val billAmount: Double,
    val expenses: Double,
    val payments: List<PaymentDetail> = emptyList(),
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val discount: Double = 0.0,
    val extraPrice: Double = 0.0,
    val idDocumentType: String = "",
    val idDocumentUrl: String = "",
    val idDocumentFileId: String = "",
    val guestIds: List<GuestIdCard> = emptyList()
) {
    fun spansDate(targetDate: String): Boolean {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val checkIn = sdf.parse(checkInDate)
            val target = sdf.parse(targetDate)
            if (checkIn == null || target == null) return false
            
            val totalNights = items.maxOfOrNull { it.nights } ?: if (dormBedsSelected > 0) 1 else 1
            
            val calendar = java.util.Calendar.getInstance()
            calendar.time = checkIn
            calendar.add(java.util.Calendar.DAY_OF_YEAR, totalNights)
            val checkOut = calendar.time
            
            !target.before(checkIn) && !target.after(checkOut)
        } catch (e: Exception) {
            checkInDate == targetDate
        }
    }

    val checkOutDate: String
        get() {
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val checkIn = sdf.parse(checkInDate)
                val totalNights = items.maxOfOrNull { it.nights } ?: if (dormBedsSelected > 0) 1 else 1
                val calendar = java.util.Calendar.getInstance()
                calendar.time = checkIn!!
                calendar.add(java.util.Calendar.DAY_OF_YEAR, totalNights)
                sdf.format(calendar.time)
            } catch (e: Exception) {
                checkInDate
            }
        }

    val baseAmountCharged: Double
        get() = if (isBillOn) billAmount else (items.filter { it.category != "Dorm" && it.category != "Dorm Bed" }.sumOf { it.amount } + dormTotalAmount)


    val amountCharged: Double
        get() = baseAmountCharged + extraPrice - discount

    val totalPaid: Double
        get() = payments.sumOf { it.amount }

    val balance: Double
        get() = amountCharged - totalPaid

    val paymentStatus: String
        get() = if (balance <= 0.0) "Paid" else "Pending"

    val paymentMethod: String
        get() = if (payments.isNotEmpty()) payments.last().method else "UPI"

    val isAssigned: Boolean
        get() {
            if (items.isEmpty()) {
                return dormBedsSelected > 0 && (dormRoomABeds.isNotBlank() || dormRoomBBeds.isNotBlank())
            }
            return items.all { it.roomNumber.isNotBlank() }
        }

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
        
        val paymentsArray = JSONArray()
        payments.forEach { paymentsArray.put(it.toJsonObject()) }
        json.put("payments", paymentsArray)
        
        json.put("notes", notes)
        json.put("timestamp", timestamp)
        json.put("discount", discount)
        json.put("extraPrice", extraPrice)
        json.put("idDocumentType", idDocumentType)
        json.put("idDocumentUrl", idDocumentUrl)
        json.put("idDocumentFileId", idDocumentFileId)
        
        val guestIdsArray = JSONArray()
        guestIds.forEach { guestIdsArray.put(it.toJsonObject()) }
        json.put("guestIds", guestIdsArray)
        
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
            val billAmountValue = json.optDouble("billAmount", defaultBillAmount)

            val paymentsList = mutableListOf<PaymentDetail>()
            var hasPayments = false
            if (json.has("payments")) {
                val paymentsArray = json.getJSONArray("payments")
                if (paymentsArray.length() > 0) {
                    hasPayments = true
                    for (i in 0 until paymentsArray.length()) {
                        paymentsList.add(PaymentDetail.fromJsonObject(paymentsArray.getJSONObject(i)))
                    }
                }
            }
            if (!hasPayments) {
                // Synthesize payment list for backward compatibility
                val status = json.optString("paymentStatus", "Pending")
                val method = json.optString("paymentMethod", "UPI")
                val amountChargedVal = if (isBillOn) billAmountValue else {
                    val itemsSum = itemsList.sumOf { it.amount }
                    itemsSum + dormTotalAmount
                }
                if (status.equals("Paid", ignoreCase = true) && amountChargedVal > 0.0) {
                    paymentsList.add(
                        PaymentDetail(
                            amount = amountChargedVal,
                            method = method
                        )
                    )
                }
            }


            val discountVal = json.optDouble("discount", 0.0)
            val extraPriceVal = json.optDouble("extraPrice", 0.0)

            val guestIdsList = mutableListOf<GuestIdCard>()
            if (json.has("guestIds")) {
                val array = json.getJSONArray("guestIds")
                for (i in 0 until array.length()) {
                    guestIdsList.add(GuestIdCard.fromJsonObject(array.getJSONObject(i)))
                }
            } else {
                val oldUrl = json.optString("idDocumentUrl", "")
                val oldType = json.optString("idDocumentType", "")
                val oldFileId = json.optString("idDocumentFileId", "")
                if (oldUrl.isNotBlank() || oldType.isNotBlank()) {
                    guestIdsList.add(
                        GuestIdCard(
                            idType = if (oldType.isNotBlank()) oldType else "Aadhaar Card",
                            images = listOf(GuestIdImage(url = oldUrl, fileId = oldFileId, label = "Document"))
                        )
                    )
                }
            }

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
                billAmount = billAmountValue,
                expenses = json.optDouble("expenses", 0.0),
                payments = paymentsList,
                notes = json.optString("notes", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                discount = discountVal,
                extraPrice = extraPriceVal,
                idDocumentType = json.optString("idDocumentType", ""),
                idDocumentUrl = json.optString("idDocumentUrl", ""),
                idDocumentFileId = json.optString("idDocumentFileId", ""),
                guestIds = guestIdsList
            )
        }
    }
}

// Room Category Mappings Helpers
fun getRoomCategory(roomNumber: String): String {
    return when {
        roomNumber.isBlank() -> "Standard"
        roomNumber.startsWith("A") || roomNumber.startsWith("B") -> "Dorm Bed"
        roomNumber == "103" || roomNumber == "104I" -> "Deluxe Family"
        roomNumber in listOf("203", "204I", "303", "304I") -> "Family"
        roomNumber.endsWith("5") || roomNumber.endsWith("4II") -> "Standard"
        roomNumber.endsWith("1") || roomNumber.endsWith("2") -> "Deluxe"
        roomNumber.endsWith("6") -> "Double"
        else -> "Standard"
    }
}

fun getRoomsForCategory(category: String): List<String> {
    val allRooms = listOf(
        "101", "102", "103", "104I", "104II", "105", "106",
        "201", "202", "203", "204I", "204II", "205", "206",
        "301", "302", "303", "304I", "304II", "305", "306"
    )
    return when (category) {
        "Dorm Bed" -> {
            val beds = mutableListOf<String>()
            for (i in 1..8) beds.add("A$i")
            for (i in 1..8) beds.add("B$i")
            beds
        }
        "Deluxe Family" -> listOf("103", "104I")
        "Family" -> listOf("203", "204I", "303", "304I")
        "Standard" -> allRooms.filter { it.endsWith("5") || it.endsWith("4II") }
        "Deluxe" -> allRooms.filter { it.endsWith("1") || it.endsWith("2") }
        "Double" -> allRooms.filter { it.endsWith("6") }
        "Room", "Standard Room" -> allRooms
        else -> emptyList()
    }
}

fun adjustRatesList(rates: List<String>, targetSize: Int): List<String> {
    val result = rates.toMutableList()
    if (result.size < targetSize) {
        repeat(targetSize - result.size) { result.add("") }
    } else if (result.size > targetSize) {
        return result.take(targetSize)
    }
    return result
}
