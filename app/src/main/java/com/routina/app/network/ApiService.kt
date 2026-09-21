package com.routina.app.network

import com.routina.app.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Maps directly onto the routes exposed by /api (Node/Express + Firestore).
 * See the /api/routes folder for the server-side implementation.
 */
interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET("habits")
    suspend fun getHabits(
        @Header("Authorization") bearer: String
    ): Response<HabitsResponse>

    @POST("habits")
    suspend fun createHabit(
        @Header("Authorization") bearer: String,
        @Body body: Habit
    ): Response<Habit>

    @PUT("habits/{id}")
    suspend fun updateHabit(
        @Header("Authorization") bearer: String,
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Habit>

    @DELETE("habits/{id}")
    suspend fun deleteHabit(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): Response<Unit>

    @GET("settings")
    suspend fun getSettings(
        @Header("Authorization") bearer: String
    ): Response<SettingsResponse>

    @PUT("settings")
    suspend fun updateSettings(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<SettingsResponse>
}
