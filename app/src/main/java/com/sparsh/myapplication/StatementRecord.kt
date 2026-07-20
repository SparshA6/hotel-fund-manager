package com.sparsh.myapplication

import org.json.JSONArray
import org.json.JSONObject

data class StatementRecord(
    val id: String,
    val date: String,
    val description: String,
    val amount: Double,
    val isMatched: Boolean = false,
    val matchedBookingId: String = "",
    val matchedPaymentId: String = "",
    val fileIds: List<String> = emptyList()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("date", date)
        json.put("description", description)
        json.put("amount", amount)
        json.put("isMatched", isMatched)
        json.put("matchedBookingId", matchedBookingId)
        json.put("matchedPaymentId", matchedPaymentId)
        val fileIdsArray = JSONArray()
        fileIds.forEach { fileIdsArray.put(it) }
        json.put("fileIds", fileIdsArray)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): StatementRecord {
            val fileIdsList = mutableListOf<String>()
            val arr = json.optJSONArray("fileIds")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    fileIdsList.add(arr.getString(i))
                }
            }
            return StatementRecord(
                id = json.getString("id"),
                date = json.getString("date"),
                description = json.getString("description"),
                amount = json.getDouble("amount"),
                isMatched = json.optBoolean("isMatched", false),
                matchedBookingId = json.optString("matchedBookingId", ""),
                matchedPaymentId = json.optString("matchedPaymentId", ""),
                fileIds = fileIdsList
            )
        }
    }
}
