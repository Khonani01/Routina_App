package com.routina.functions

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    fun applyLanguage(context: Context): Context {
        val preferences = context.getSharedPreferences("RoutinaSettings", Context.MODE_PRIVATE)
        val savedLanguage = preferences.getString("language", "English") ?: "English"

        val langCode = getLanguageCode(savedLanguage)
        val locale = Locale.forLanguageTag(langCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))

        return context.createConfigurationContext(config)
    }

    fun getLanguageCode(languageName: String): String {
        return when (languageName) {
            "Afrikaans" -> "af"
            "Setswana" -> "tn"
            else -> "en"
        }
    }
}
