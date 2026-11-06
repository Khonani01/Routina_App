package com.routina.functions

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class RoutinaApp : Application() {

    companion object {
        lateinit var instance: RoutinaApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Load user settings on application start
        val preferences = getSharedPreferences("RoutinaSettings", MODE_PRIVATE)
        val darkMode = preferences.getBoolean("darkMode", false)

        // Apply dark mode preference
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }
}
