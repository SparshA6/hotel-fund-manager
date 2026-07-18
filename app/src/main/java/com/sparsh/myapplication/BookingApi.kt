package com.sparsh.myapplication

import retrofit2.Response
import retrofit2.http.*

data class UploadIdRequest(
    val idDocumentType: String,
    val imageContentBase64: String,
    val imageName: String
)

data class UploadGuestIdRequest(
    val cardId: String,
    val imageId: String,
    val idType: String,
    val guestName: String,
    val imageContentBase64: String,
    val label: String,
    val index: Int
)

interface BookingApi {
    @GET("api/bookings")
    suspend fun getBookings(): List<Booking>

    @POST("api/bookings")
    suspend fun saveBooking(@Body booking: Booking): Booking

    @DELETE("api/bookings/{id}")
    suspend fun deleteBooking(
        @Path("id") id: String,
        @Query("deleteIds") deleteIds: Boolean
    ): Response<Unit>

    @POST("api/bookings/{id}/upload-id")
    suspend fun uploadIdDocument(
        @Path("id") id: String,
        @Body body: UploadIdRequest
    ): Booking

    @POST("api/bookings/{id}/guest-ids/upload")
    suspend fun uploadGuestId(
        @Path("id") id: String,
        @Body body: UploadGuestIdRequest
    ): Booking

    @DELETE("api/bookings/{id}/guest-ids/{cardId}/images/{imageId}")
    suspend fun deleteGuestIdImage(
        @Path("id") id: String,
        @Path("cardId") cardId: String,
        @Path("imageId") imageId: String
    ): Booking

    @GET("api/portal-settings")
    suspend fun getPortalSettings(): List<PortalSettings>

    @POST("api/portal-settings")
    suspend fun savePortalSettings(@Body settings: PortalSettings): PortalSettings

    @GET("api/backups")
    suspend fun getBackups(): List<BackupInfo>

    @POST("api/backups")
    suspend fun createBackup(): BackupInfo

    @POST("api/backups/{id}/restore")
    suspend fun restoreBackup(@Path("id") id: String): Response<Unit>

    @DELETE("api/backups/{id}")
    suspend fun deleteBackup(@Path("id") id: String): Response<Unit>
}
