package com.sparsh.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BookingRepository(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("hotel_fund_prefs", Context.MODE_PRIVATE)

    private val KEY_BOOKINGS = "key_bookings"

    // Base Retrofit initialization
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(BookingApi::class.java)

    /**
     * Fetches bookings. Tries the cloud first, saves response to local cache,
     * and falls back to local cache if offline or server is unavailable.
     */
    suspend fun getBookings(): List<Booking> = withContext(Dispatchers.IO) {
        try {
            Log.d("BookingRepository", "Fetching bookings from cloud...")
            val remoteBookings = api.getBookings()
            Log.d("BookingRepository", "Fetched ${remoteBookings.size} bookings from cloud. Updating cache.")
            saveAllLocalBookings(remoteBookings)
            remoteBookings.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e("BookingRepository", "Cloud fetch failed: ${e.message}. Falling back to cache.")
            getLocalBookings()
        }
    }

    /**
     * Saves a booking. Updates local cache immediately, then syncs to cloud.
     */
    suspend fun saveBooking(booking: Booking) = withContext(Dispatchers.IO) {
        // Update local cache first
        val localBookings = getLocalBookings().toMutableList()
        localBookings.removeAll { it.id == booking.id }
        localBookings.add(booking)
        saveAllLocalBookings(localBookings)

        try {
            Log.d("BookingRepository", "Saving booking ${booking.id} to cloud...")
            api.saveBooking(booking)
            Log.d("BookingRepository", "Saved booking to cloud successfully.")
        } catch (e: Exception) {
            Log.e("BookingRepository", "Failed to save booking to cloud: ${e.message}. Saved locally.")
        }
    }

    /**
     * Deletes a booking. Updates local cache immediately, then syncs deletion to cloud.
     */
    suspend fun deleteBooking(bookingId: String) = withContext(Dispatchers.IO) {
        // Delete from local cache first
        val localBookings = getLocalBookings().toMutableList()
        localBookings.removeAll { it.id == bookingId }
        saveAllLocalBookings(localBookings)

        try {
            Log.d("BookingRepository", "Deleting booking $bookingId from cloud...")
            val response = api.deleteBooking(bookingId)
            if (response.isSuccessful) {
                Log.d("BookingRepository", "Deleted booking from cloud successfully.")
            } else {
                Log.e("BookingRepository", "Failed to delete booking from cloud. Code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("BookingRepository", "Failed to delete booking from cloud: ${e.message}. Deleted locally.")
        }
    }

    // --- Local Caching Helpers ---

    private fun getLocalBookings(): List<Booking> {
        val bookingsJsonString = sharedPreferences.getString(KEY_BOOKINGS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(bookingsJsonString)
            val bookings = mutableListOf<Booking>()
            for (i in 0 until jsonArray.length()) {
                bookings.add(Booking.fromJsonObject(jsonArray.getJSONObject(i)))
            }
            bookings.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveAllLocalBookings(bookings: List<Booking>) {
        val jsonArray = JSONArray()
        bookings.forEach { jsonArray.put(it.toJsonObject()) }
        sharedPreferences.edit().putString(KEY_BOOKINGS, jsonArray.toString()).apply()
    }

    companion object {
        // Connected phone needs the host computer's IP address on the Wi-Fi network.
        private const val BASE_URL = "http://10.72.25.41:5000/"
    }
}
