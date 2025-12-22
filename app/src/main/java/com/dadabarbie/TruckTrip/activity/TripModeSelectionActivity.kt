package com.dadabarbie.TruckTrip.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dadabarbie.TruckTrip.R



import android.animation.ValueAnimator
import android.content.Intent

import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.languageLocale
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.databinding.ActivityTripModeSelectionBinding
import java.util.Locale

class TripModeSelectionActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val binding: ActivityTripModeSelectionBinding by lazy {
        ActivityTripModeSelectionBinding.inflate(layoutInflater)
    }

    private lateinit var textToSpeech: TextToSpeech
    private var ttsInitialized = false
    private var hasSpokenOnce = false
    private var arrowAnimator: ValueAnimator? = null
    private var langCode = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        langCode = Prefs[Constants.languageCode] ?: "hi"
        textToSpeech = TextToSpeech(this, this)

        setupViews()
        startArrowAnimation()
        startArrowNewAnimation()
    }

    private fun setupViews() {
        // Back button
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Speaker button
        binding.ivSpeakerFull.setOnClickListener {
            speakInstructions()
        }

        // Simple Mode Card - Voice Only
        binding.addTrip.setOnClickListener {
            Prefs[Constants.appMode] = "B"
            speakText(getString(R.string.full_mode_selected))
            if(intent.getStringExtra("languageFlag").equals("")){
                startActivity(Intent(this, HowToUseActivity::class.java)
                    .putExtra("languageFlag",""))
            }else{
                navigateToFullMode()
            }

        }
        binding.addFullDetailsTrip.setOnClickListener {
            Prefs[Constants.appMode] = "A"
            speakText(getString(R.string.simple_mode_selected))
            if(intent.getStringExtra("languageFlag").equals("")){
                startActivity(Intent(this, HowToUseNormalActivity::class.java)
                    .putExtra("languageFlag",""))
            }else{
                navigateToSimpleMode()
            }

            finish()
        }

        // Full Mode Card - Complete Details
//        binding.cardFullMode.setOnClickListener {
//            speakText(getString(R.string.full_mode_selected))
//            navigateToFullMode()
//        }

        // Speaker icons on cards
        binding.ivSpeakerSimple.setOnClickListener {
            speakText(getString(R.string.simple_mode_description))
        }

        binding.ivSpeakerFull.setOnClickListener {
            speakText(getString(R.string.full_mode_description))
        }
    }

    private fun startArrowAnimation() {
        arrowAnimator = ValueAnimator.ofFloat(0f, -15f, 0f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                binding.ivArrowDown.translationY = value

            }
            start()
        }
    }
    private fun startArrowNewAnimation() {
        arrowAnimator = ValueAnimator.ofFloat(0f, 10f, 0f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                binding.addTrip.translationY = value
                binding.addFullDetailsTrip.translationY = value
            }

            start()
        }
        Log.d("Animation", "✅ Button animation started")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale(langCode))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
            } else {
                ttsInitialized = true
                if (!hasSpokenOnce) {
                    binding.root.postDelayed({
                        speakInstructions()
                        hasSpokenOnce = true
                    }, 500)
                }
            }
        }
    }

    private fun speakInstructions() {
        speakText(getString(R.string.trip_mode_instruction))
    }

    private fun speakText(text: String) {
        if (ttsInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun navigateToSimpleMode() {
        val intent = Intent(this, NormalUserDashBoard::class.java)
        intent.putExtra("tripData","")
        intent.putExtra("MODE", "SIMPLE")
        startActivity(intent)
    }

    private fun navigateToFullMode() {
        val intent = Intent(this, DashBoardActivity::class.java)

        intent.putExtra("MODE", "FULL")
        startActivity(intent)
    }

    override fun onDestroy() {
        arrowAnimator?.cancel()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}