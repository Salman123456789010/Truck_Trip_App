package com.dadabarbie.TruckTrip

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.languageLocale
import com.dadabarbie.TruckTrip.Utils.LocaleHelper
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class TruckTripApplication:Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    companion object{
        /*val gson = GsonBuilder().setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();*/
        val gson = GsonBuilder().setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();

        lateinit var appContext : Context
        // var dataStoreManager : DataStoreManager? = null
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        Prefs.init(this)

        appContext = applicationContext
    }






}