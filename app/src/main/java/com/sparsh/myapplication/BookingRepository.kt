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
            request.body?.writeTo(buffer)
            val bodyStr = buffer.readUtf8()
            Log.e("BookingRepository", "API REQUEST: ${request.method} ${request.url} - Body: $bodyStr")
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
    suspend fun deleteBooking(bookingId: String, deleteIds: Boolean = false) = withContext(Dispatchers.IO) {
        // Delete from local cache first
        val localBookings = getLocalBookings().toMutableList()
        localBookings.removeAll { it.id == bookingId }
        saveAllLocalBookings(localBookings)

        try {
            Log.d("BookingRepository", "Deleting booking $bookingId from cloud (deleteIds=$deleteIds)...")
            val response = api.deleteBooking(bookingId, deleteIds)
            if (response.isSuccessful) {
                Log.d("BookingRepository", "Deleted booking from cloud successfully.")
            } else {
                Log.e("BookingRepository", "Failed to delete booking from cloud. Code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("BookingRepository", "Failed to delete booking from cloud: ${e.message}. Deleted locally.")
        }
    }

    suspend fun uploadGuestId(
        bookingId: String,
        cardId: String,
        imageId: String,
        idType: String,
        guestName: String,
        imageBase64: String,
        label: String,
        index: Int
    ): Booking? = withContext(Dispatchers.IO) {
        try {
            Log.d("BookingRepository", "Uploading guest ID for booking $bookingId, card $cardId...")
            val req = UploadGuestIdRequest(cardId, imageId, idType, guestName, imageBase64, label, index)
            val updatedBooking = api.uploadGuestId(bookingId, req)
            Log.d("BookingRepository", "Uploaded guest ID successfully. Updating local cache.")
            // Update local cache
            val localBookings = getLocalBookings().toMutableList()
            localBookings.removeAll { it.id == updatedBooking.id }
            localBookings.add(updatedBooking)
            saveAllLocalBookings(localBookings)
            updatedBooking
        } catch (e: Exception) {
            Log.e("BookingRepository", "Failed to upload guest ID: ${e.message}")
            null
        }
    }

    suspend fun deleteGuestIdImage(
        bookingId: String,
        cardId: String,
        imageId: String
    ): Booking? = withContext(Dispatchers.IO) {
        try {
            Log.d("BookingRepository", "Deleting guest ID image $imageId for booking $bookingId...")
            val updatedBooking = api.deleteGuestIdImage(bookingId, cardId, imageId)
            Log.d("BookingRepository", "Deleted guest ID image successfully. Updating local cache.")
            // Update local cache
            val localBookings = getLocalBookings().toMutableList()
            localBookings.removeAll { it.id == updatedBooking.id }
            localBookings.add(updatedBooking)
            saveAllLocalBookings(localBookings)
            updatedBooking
        } catch (e: Exception) {
            Log.e("BookingRepository", "Failed to delete guest ID image: ${e.message}")
            null
        }
    }

    /**
     * Uploads the ID document. Syncs to cloud and updates local cache.
     */
    suspend fun uploadIdDocument(bookingId: String, idType: String, imageBase64: String, fileName: String): Booking? = withContext(Dispatchers.IO) {
        try {
            Log.d("BookingRepository", "Uploading ID document for booking $bookingId...")
            val updatedBooking = api.uploadIdDocument(bookingId, UploadIdRequest(idType, imageBase64, fileName))
            Log.d("BookingRepository", "Uploaded ID document successfully. Updating local cache.")
            // Update local cache
            val localBookings = getLocalBookings().toMutableList()
            localBookings.removeAll { it.id == updatedBooking.id }
            localBookings.add(updatedBooking)
            saveAllLocalBookings(localBookings)
            updatedBooking
        } catch (e: Exception) {
            Log.e("BookingRepository", "Failed to upload ID document: ${e.message}")
            null
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

    suspend fun getBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        api.getBackups()
    }

    suspend fun createBackup(): BackupInfo = withContext(Dispatchers.IO) {
        api.createBackup()
    }

    suspend fun deleteBackupFromServer(id: String) = withContext(Dispatchers.IO) {
        api.deleteBackup(id)
    }

    suspend fun restoreBackupAndSync(id: String): List<Booking> = withContext(Dispatchers.IO) {
        api.restoreBackup(id)
        val remoteBookings = api.getBookings()
        saveAllLocalBookings(remoteBookings)
        remoteBookings.sortedByDescending { it.timestamp }
    }

    private val KEY_PORTAL_SETTINGS = "key_portal_settings"

    fun getDefaultPortalSettings(): List<PortalSettings> {
        return listOf(
            PortalSettings("MMT", commissionRate = 20.0f, propertyGstRate = 12.0f, gstOnCommissionRate = 18.0f, tdsRate = 1.0f, tcsRate = 1.0f, serviceCharge = 0.0f),
            PortalSettings("Goibibo", commissionRate = 15.0f, propertyGstRate = 12.0f, gstOnCommissionRate = 18.0f, tdsRate = 1.0f, tcsRate = 1.0f, serviceCharge = 0.0f),
            PortalSettings("Yatra", commissionRate = 15.0f, propertyGstRate = 0.0f, gstOnCommissionRate = 0.0f, tdsRate = 1.0f, tcsRate = 1.0f, serviceCharge = 0.0f),
            PortalSettings("Booking.com", commissionRate = 15.0f, propertyGstRate = 12.0f, gstOnCommissionRate = 0.0f, tdsRate = 0.0f, tcsRate = 0.0f, paymentProcessingFeeRate = 2.0f),
            PortalSettings("Agoda", commissionRate = 15.0f, propertyGstRate = 12.0f, gstOnCommissionRate = 18.0f, tdsRate = 1.0f, tcsRate = 1.0f, serviceCharge = 0.0f),
            PortalSettings("Cleartrip", commissionRate = 12.0f, propertyGstRate = 12.0f, gstOnCommissionRate = 18.0f, tdsRate = 1.0f, tcsRate = 1.0f, serviceCharge = 0.0f)
        )
    }

    private var cachedPortalSettings: List<PortalSettings>? = null

    fun getLocalPortalSettings(): List<PortalSettings> {
        val cached = cachedPortalSettings
        if (cached != null) return cached
        val settingsJson = sharedPreferences.getString(KEY_PORTAL_SETTINGS, null) ?: return getDefaultPortalSettings()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<PortalSettings>>() {}.type
            val result: List<PortalSettings> = gson.fromJson(settingsJson, type)
            cachedPortalSettings = result
            result
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultPortalSettings()
        }
    }

    fun saveLocalPortalSettings(settings: List<PortalSettings>) {
        cachedPortalSettings = settings
        val json = gson.toJson(settings)
        sharedPreferences.edit().putString(KEY_PORTAL_SETTINGS, json).apply()
    }

    suspend fun getPortalSettings(): List<PortalSettings> = withContext(Dispatchers.IO) {
        try {
            Log.d("BookingRepository", "Fetching portal settings from cloud...")
            val remoteSettings = api.getPortalSettings()
            Log.d("BookingRepository", "Fetched ${remoteSettings.size} portal settings from cloud. Updating cache.")
            saveLocalPortalSettings(remoteSettings)
            remoteSettings
        } catch (e: Exception) {
            Log.e("BookingRepository", "Cloud portal settings fetch failed: ${e.message}. Falling back to cache.")
            getLocalPortalSettings()
        }
    }

    suspend fun savePortalSettings(settings: PortalSettings) = withContext(Dispatchers.IO) {
        val localSettings = getLocalPortalSettings().toMutableList()
        localSettings.removeAll { it.platform.equals(settings.platform, ignoreCase = true) }
        localSettings.add(settings)
        saveLocalPortalSettings(localSettings)

        try {
            Log.d("BookingRepository", "Saving portal settings for ${settings.platform} to cloud...")
            api.savePortalSettings(settings)
            Log.d("BookingRepository", "Saved portal settings to cloud successfully.")
        } catch (e: Exception) {
            Log.e("BookingRepository", "Failed to save portal settings to cloud: ${e.message}. Saved locally.")
        }
    }

    fun getPortalSettingsForPlatform(platform: String): PortalSettings {
        return getLocalPortalSettings().find { it.platform.equals(platform, ignoreCase = true) }
            ?: PortalSettings(platform)
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
