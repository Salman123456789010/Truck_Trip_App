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
import com.dadabarbie.TruckTrip.databinding.ActivityNormalUserDashBoardBinding
import com.dadabarbie.TruckTrip.databinding.ActivitySplashBinding
import com.example.driverhisaab.VerySimpleModeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

import com.dadabarbie.TruckTrip.Utils.ServerWarmupManager
import com.dadabarbie.TruckTrip.api.ApiService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    @Inject
    lateinit var apiService: ApiService

    private val binding: ActivitySplashBinding by lazy {
        ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ServerWarmupManager.warmupServer(apiService)

        CoroutineScope(Dispatchers.Main).launch {
            delay(1500)

            val isLogin = Prefs[Constants.isLogin, false]
            val langSelected = Prefs[Constants.languageCode, ""].isNotEmpty()
            val appModeSelected = Prefs[Constants.appMode, ""].isNotEmpty()
            applySavedLanguage()

            val nextIntent = when {
                // Language not selected
                !langSelected -> {
                    Intent(this@SplashActivity, LanguageSelction::class.java)
                }

                // Not logged in
                !isLogin  -> {
                    Intent(this@SplashActivity, LoginScreenActivity::class.java)
                }

                // Logged in → dashboard
                else -> {
                    if (Prefs[Constants.appMode, ""] == "A") {
                        Intent(this@SplashActivity, NormalUserDashBoard::class.java).putExtra("tripData", "")
                    } else {
                        Intent(this@SplashActivity, DashBoardActivity::class.java)
                    }
                }
            }


            nextIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(nextIntent)

            finish()
        }
    }
    private fun applySavedLanguage() {
        val savedLang = Prefs[Constants.languageCode, ""]
        if (savedLang.isNotEmpty()) {
            val locale = Locale(savedLang)
            Locale.setDefault(locale)
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }


}
