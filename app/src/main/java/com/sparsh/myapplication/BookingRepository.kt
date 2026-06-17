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

    private val client = okhttp3.OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val buffer = okio.Buffer()
            request.body()?.writeTo(buffer)
            val bodyStr = buffer.readUtf8()
            Log.e("BookingRepository", "API REQUEST: ${request.method()} ${request.url()} - Body: $bodyStr")
            chain.proceed(request)
        }
        .build()

    private val gson = com.google.gson.GsonBuilder()
        .registerTypeAdapter(Booking::class.java, BookingTypeAdapter())
        .create()

    // Base Retrofit initialization
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
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
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("BookingRepository", "Failed to save booking to cloud: HTTP ${e.code()} - $errorBody. Saved locally.")
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

    fun getLocalBookings(): List<Booking> {
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
        private const val BASE_URL = "https://hotel-fund-manager.onrender.com/"
    }
}

class BookingTypeAdapter : com.google.gson.JsonSerializer<Booking>, com.google.gson.JsonDeserializer<Booking> {
    override fun serialize(
        src: Booking,
        typeOfSrc: java.lang.reflect.Type,
        context: com.google.gson.JsonSerializationContext
    ): com.google.gson.JsonElement {
        val jsonStr = src.toJsonObject().toString()
        return com.google.gson.JsonParser.parseString(jsonStr)
    }

    override fun deserialize(
        json: com.google.gson.JsonElement,
        typeOfT: java.lang.reflect.Type,
        context: com.google.gson.JsonDeserializationContext
    ): Booking {
        val jsonStr = json.toString()
        val jsonObject = org.json.JSONObject(jsonStr)
        return Booking.fromJsonObject(jsonObject)
    }
}
