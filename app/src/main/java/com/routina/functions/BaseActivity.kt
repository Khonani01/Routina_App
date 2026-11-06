package com.routina.functions

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    private var activeLanguage: String? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("RoutinaSettings", MODE_PRIVATE)
        activeLanguage = prefs.getString("language", "English")
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("RoutinaSettings", MODE_PRIVATE)
        val latestLanguage = prefs.getString("language", "English")
        if (activeLanguage != null && activeLanguage != latestLanguage) {
            activeLanguage = latestLanguage
            recreate()
        }
    }
}
