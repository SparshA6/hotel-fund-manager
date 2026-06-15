package com.sparsh.myapplication

import retrofit2.Response
import retrofit2.http.*

interface BookingApi {
    @GET("api/bookings")
    suspend fun getBookings(): List<Booking>

    @POST("api/bookings")
    suspend fun saveBooking(@Body booking: Booking): Booking

    @DELETE("api/bookings/{id}")
    suspend fun deleteBooking(@Path("id") id: String): Response<Unit>
}
