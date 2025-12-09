package com.dadabarbie.TruckTrip.Utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.*

object LocaleHelper {

    fun setLocale(context: Context) {
        val languageCode = getSavedLanguage(context)
        return updateResources(context, languageCode)
    }

    fun setNewLocale(context: Context, languageCode: String) {
        saveLanguage(context, languageCode)
        return updateResources(context, languageCode)
    }

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("language_code", "en") ?: "en"  // Default is English
    }

    private fun saveLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
        prefs.putString("language_code", languageCode)
        prefs.apply()
    }

    private fun updateResources(context: Context, languageCode: String) {
        val locale = Locale(languageCode)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as android.app.LocaleManager
            localeManager.applicationLocales = LocaleList(locale)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)

            val config = Configuration()
            config.setLocales(localeList)
//        context.createConfigurationContext(config)

            // Optionally, update the context's resources
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } else {

            Locale.setDefault(locale)

            val config = Configuration()
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }

        // Update the configuration for the application context as well if needed
        val applicationContext = context.applicationContext
        val config = applicationContext.resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            applicationContext.createConfigurationContext(config)
        } else {
            config.locale = locale
            applicationContext.resources.updateConfiguration(config, applicationContext.resources.displayMetrics)
        }
    }
}



