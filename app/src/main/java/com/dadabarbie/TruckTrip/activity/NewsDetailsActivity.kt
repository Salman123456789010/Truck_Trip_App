package com.dadabarbie.TruckTrip.activity

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import com.bumptech.glide.Glide
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.databinding.ActivityNewsDetailsBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

class NewsDetailsActivity : AppCompatActivity(), View.OnClickListener {
    private val binding: ActivityNewsDetailsBinding by lazy {
        ActivityNewsDetailsBinding.inflate(layoutInflater)
    }
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        com.dadabarbie.TruckTrip.Utils.SystemUiUtils.setupStatusBar(this, R.color.green, false)
        setLanguage()
        setOnClickListner()
        binding.newsHeader.text = intent.getStringExtra("tittle")
        binding.newsDecreption.text = intent.getStringExtra("descreption")
        binding.newsTime.text = "Published : ${intent.getStringExtra("newsTime")}"

        binding.playerView.settings.javaScriptEnabled = true
        binding.playerView.webViewClient = WebViewClient()  // Ensures link opens in WebView

        // Load the video link (YouTube embed link or any video URL)
        val videoUrl = intent.getStringExtra("videoUrl")
        if (videoUrl != null) {
            binding.playerView.loadUrl(videoUrl)
        }
        GlobalScope.launch {
            MobileAds.initialize(applicationContext)
            val adLoader =
                AdLoader.Builder(applicationContext, "ca-app-pub-8808039515208362/9048738516")
                    .forNativeAd { p0 ->
                        binding.myTemplate.setNativeAd(p0)
                    }
                    .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }

        Glide.with(this).load(intent.getStringExtra("image")).into(binding.imagNews)

    }

    private fun setOnClickListner() {
        binding.backBtn.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    override fun onClick(v: View?) {
      when(v){
          binding.backBtn->{onBackPressed()}
      }
    }
    fun setLanguage(){
        val locale = Locale(Prefs[Constants.languageCode,""])
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }

}
