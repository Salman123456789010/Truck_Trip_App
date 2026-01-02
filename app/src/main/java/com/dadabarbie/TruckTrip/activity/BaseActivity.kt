package com.dadabarbie.TruckTrip.activity

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.LocaleHelper
import com.dadabarbie.TruckTrip.Utils.Prefs
import java.util.Locale

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Get SharedPreferences directly (replace "MyPrefs" with your actual prefs name)
        val sp = newBase.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val savedLang = sp.getString(Constants.languageCode, null)

        Log.d("LANG_DEBUG", "attachBaseContext - Saved: $savedLang")

        // Use saved language or system default
        val lang = savedLang ?: newBase.resources.configuration.locales[0].language

        val context = LocaleHelper.wrap(newBase, lang)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply locale when activity is created
        applyLanguage()
    }

    private fun applyLanguage() {
        val sp = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val savedLang = sp.getString(Constants.languageCode, null)

        if (savedLang != null) {
            val locale = Locale(savedLang)
            Locale.setDefault(locale)

            val config = Configuration(resources.configuration)
            config.setLocale(locale)

            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}
