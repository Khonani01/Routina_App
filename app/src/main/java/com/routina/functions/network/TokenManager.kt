package com.routina.functions.network

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREF_NAME = "RoutinaApiPrefs"
    private const val KEY_API_TOKEN = "api_bearer_token"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveApiToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_API_TOKEN, token).apply()
    }

    fun getApiToken(context: Context): String? {
        return getPrefs(context).getString(KEY_API_TOKEN, null)
    }

    fun clearApiToken(context: Context) {
        getPrefs(context).edit().remove(KEY_API_TOKEN).apply()
    }
}
