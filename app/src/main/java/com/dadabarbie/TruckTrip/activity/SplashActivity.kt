package com.dadabarbie.TruckTrip.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.languageLocale
import com.dadabarbie.TruckTrip.Utils.LocaleHelper
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.auth.activity.LoginScreenActivity
import com.dadabarbie.TruckTrip.databinding.ActivitySplashBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SplashActivity : AppCompatActivity() {
    private val binding: ActivitySplashBinding by lazy {
        ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Constants.setStatusBar(this, isLight = true, colorRes = com.dadabarbie.TruckTrip.R.color.white)
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            if (Prefs[Constants.isLogin]) {
                startActivity(Intent(this@SplashActivity, DashBoardActivity::class.java))
                finish()
            } else {
                 if(!Prefs[Constants.languageCode, ""].toString().isNullOrEmpty()){
                     setLanguage()
                     startActivity(Intent(this@SplashActivity, LoginScreenActivity::class.java))
                     finish()
                 }else{
                     startActivity(Intent(this@SplashActivity, LanguageSelction::class.java).putExtra("languageFlag",""))
                     finish()
                 }
            }

        }
    }
    private fun setLanguage() {
        val languageCode = LocaleHelper.getSavedLanguage(applicationContext)
        if (languageCode.isNotEmpty()) {
            LocaleHelper.setNewLocale(applicationContext, languageCode)
        }
    }

}
