package com.routina.functions.network

import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.http.*

interface RoutinaApiService {

    // AUTH SSO
    @POST("auth/sso")
    fun ssoAuth(@Body request: SsoRequest): Call<SsoResponse>

    // PUSH NOTIFICATIONS REGISTER TOKEN
    @POST("notifications/register-token")
    fun registerPushToken(@Body request: RegisterTokenRequest): Call<Void>

    // TASKS
    @GET("tasks")
    fun getTasks(): Call<JsonElement>

    @POST("tasks")
    fun createTask(@Body request: TaskCreateRequest): Call<JsonElement>

    @PUT("tasks/{id}")
    fun updateTask(@Path("id") id: String, @Body request: Map<String, @JvmSuppressWildcards Any>): Call<JsonElement>

    @PATCH("tasks/{id}")
    fun patchTaskStatus(@Path("id") id: String, @Body request: TaskStatusUpdateRequest): Call<JsonElement>

    @DELETE("tasks/{id}")
    fun deleteTask(@Path("id") id: String): Call<Void>

    // HABITS
    @GET("habits")
    fun getHabits(): Call<JsonElement>

    @POST("habits")
    fun createHabit(@Body request: HabitCreateRequest): Call<JsonElement>

    @PUT("habits/{id}")
    fun updateHabit(@Path("id") id: String, @Body request: HabitUpdateRequest): Call<JsonElement>

    @DELETE("habits/{id}")
    fun deleteHabit(@Path("id") id: String): Call<Void>

    // NOTES
    @GET("notes")
    fun getNotes(): Call<JsonElement>

    @POST("notes")
    fun createNote(@Body request: NoteCreateRequest): Call<JsonElement>

    @PUT("notes/{id}")
    fun updateNote(@Path("id") id: String, @Body request: NoteUpdateRequest): Call<JsonElement>

    @DELETE("notes/{id}")
    fun deleteNote(@Path("id") id: String): Call<Void>

    // USER PROFILE & DEVICE TOKEN
    @GET("users/profile")
    fun getUserProfile(): Call<UserProfileDto>

    @PUT("users/profile")
    fun updateUserProfile(@Body request: UserProfileUpdateRequest): Call<UserProfileDto>

    @POST("users/device-token")
    fun saveDeviceToken(@Body request: DeviceTokenRequest): Call<Void>
}
