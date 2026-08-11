package com.sparsh.myapplication

import org.json.JSONObject
import java.util.UUID

data class OtherPayment(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double = 0.0,
    val method: String = "UPI (Hotel Acc - GPay)",
    val date: String = "", // yyyy-MM-dd
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("amount", amount)
        json.put("method", method)
        json.put("date", date)
        json.put("reason", reason)
        json.put("timestamp", timestamp)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): OtherPayment {
            return OtherPayment(
                id = json.optString("id", UUID.randomUUID().toString()),
                amount = json.optDouble("amount", 0.0),
                method = json.optString("method", "UPI (Hotel Acc - GPay)"),
                date = json.optString("date", ""),
                reason = json.optString("reason", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}
