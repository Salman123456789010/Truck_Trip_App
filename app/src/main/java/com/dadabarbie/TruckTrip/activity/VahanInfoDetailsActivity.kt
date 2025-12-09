package com.dadabarbie.TruckTrip.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.databinding.ActivityVahanInfoDetailsBinding
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VahanInfoDetailsActivity : AppCompatActivity() {
     val bindding:ActivityVahanInfoDetailsBinding by lazy {
        ActivityVahanInfoDetailsBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bindding.root)
        MobileAds.initialize(this)
        GlobalScope.launch {
            val adLoader =
                AdLoader.Builder(applicationContext, "ca-app-pub-8808039515208362/9048738516")
                    .forNativeAd { p0 ->
                        bindding.myTemplate.setNativeAd(p0)
                    }
                    .build()

            adLoader.loadAd(AdRequest.Builder().build())

            val adLoader1 =
                AdLoader.Builder(applicationContext, "ca-app-pub-8808039515208362/9048738516")
                    .forNativeAd { p0 ->
                        bindding.myTemplate1.setNativeAd(p0)
                    }
                    .build()

            adLoader1.loadAd(AdRequest.Builder().build())
        }
    }
}