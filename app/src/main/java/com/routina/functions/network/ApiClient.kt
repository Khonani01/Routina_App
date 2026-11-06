package com.routina.functions.network

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.routina.functions.RoutinaApp
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val TAG = "RoutinaApiClient"
    private const val BASE_URL = "https://routina-app.onrender.com/"

    private val gson = Gson()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        // Get saved API bearer token first
        var apiToken = TokenManager.getApiToken(RoutinaApp.instance)

        // If no API token saved yet, attempt SSO exchange with Firebase ID token
        if (apiToken.isNullOrEmpty()) {
            apiToken = performSsoTokenExchange()
        }

        val requestBuilder = originalRequest.newBuilder()
        if (!apiToken.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $apiToken")
        }

        chain.proceed(requestBuilder.build())
    }

    // Auto-retry on 401 Unauthorized by exchanging fresh Firebase ID Token via POST /auth/sso
    private val tokenAuthenticator = Authenticator { _: Route?, response: Response ->
        if (responseCount(response) >= 2) {
            Log.e(TAG, "Failed to authenticate after 2 attempts.")
            return@Authenticator null
        }

        Log.d(TAG, "401 received. Exchanging fresh Firebase token via POST /auth/sso...")
        val freshApiToken = performSsoTokenExchange()

        if (!freshApiToken.isNullOrEmpty()) {
            return@Authenticator response.request.newBuilder()
                .header("Authorization", "Bearer $freshApiToken")
                .build()
        }

        null
    }

    private fun performSsoTokenExchange(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            val tokenTask = user.getIdToken(true)
            val tokenResult = Tasks.await(tokenTask, 20, TimeUnit.SECONDS)
            val firebaseIdToken = tokenResult.token ?: return null

            val jsonBody = gson.toJson(SsoRequest(idToken = firebaseIdToken))
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val ssoHttpRequest = Request.Builder()
                .url(BASE_URL + "auth/sso")
                .post(requestBody)
                .build()

            val rawHttpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val httpResponse = rawHttpClient.newCall(ssoHttpRequest).execute()
            if (httpResponse.isSuccessful) {
                val responseBodyStr = httpResponse.body?.string()
                val ssoResponse = gson.fromJson(responseBodyStr, SsoResponse::class.java)
                val newApiToken = ssoResponse?.token
                if (!newApiToken.isNullOrEmpty()) {
                    TokenManager.saveApiToken(RoutinaApp.instance, newApiToken)
                    return newApiToken
                }
            } else {
                Log.e(TAG, "SSO Exchange HTTP Error: ${httpResponse.code}")
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error performing SSO exchange: ${e.message}")
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: RoutinaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoutinaApiService::class.java)
    }
}
