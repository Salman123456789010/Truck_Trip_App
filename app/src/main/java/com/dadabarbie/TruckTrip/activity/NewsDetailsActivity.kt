package com.dadabarbie.TruckTrip.activity

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.ads.AdConfig
import com.dadabarbie.TruckTrip.billing.BillingManager
import com.dadabarbie.TruckTrip.databinding.ActivityNewsDetailsBinding
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import java.util.Locale

class NewsDetailsActivity : AppCompatActivity(), View.OnClickListener {

    private val binding: ActivityNewsDetailsBinding by lazy {
        ActivityNewsDetailsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setLanguage()
        setOnClickListner()

        binding.newsHeader.text = intent.getStringExtra("tittle")
        binding.newsDecreption.text = intent.getStringExtra("descreption")
        binding.newsTime.text = "Published : ${intent.getStringExtra("newsTime")}"

        binding.playerView.settings.javaScriptEnabled = true
        binding.playerView.webViewClient = WebViewClient()

        val videoUrl = intent.getStringExtra("videoUrl")
        if (videoUrl != null) {
            binding.playerView.loadUrl(videoUrl)
        }

        loadNativeAdIfAllowed()

        Glide.with(this).load(intent.getStringExtra("image")).into(binding.imagNews)
    }

    private fun loadNativeAdIfAllowed() {
        com.dadabarbie.TruckTrip.ads.AdMobManager.loadNativeAd(
            this,
            binding.myTemplate,
            com.dadabarbie.TruckTrip.ads.AdMobManager.NativePlacement.NEWS_DETAILS
        )
    }

    private fun setOnClickListner() {
        binding.backBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.backBtn -> onBackPressed()
        }
    }

    fun setLanguage() {
        val locale = Locale(Prefs[Constants.languageCode, ""])
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
