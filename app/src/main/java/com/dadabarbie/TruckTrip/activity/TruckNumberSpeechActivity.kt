package com.dadabarbie.TruckTrip.activity

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.databinding.ActivityOtpverificationScreenBinding
import com.dadabarbie.TruckTrip.databinding.ActivityTruckNumberSpeechBinding
import java.util.Locale

class TruckNumberSpeechActivity : AppCompatActivity(),TextToSpeech.OnInitListener {
    private val binding: ActivityTruckNumberSpeechBinding by lazy {
        ActivityTruckNumberSpeechBinding.inflate(layoutInflater)
    }


    private lateinit var textToSpeech: TextToSpeech
    private var ttsInitialized = false
    private var hasSpokenOnce = false

    private val instructionText = "Yaha pe Truck Number Bolo"

    // Voice recognition launcher
    private val voiceRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                val formattedNumber = formatTruckNumber(spokenText)
                binding.tvTruckNumber.text = formattedNumber
                binding.cardTruckNumber.visibility = android.view.View.VISIBLE
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        textToSpeech = TextToSpeech(this, this)

        setupViews()
    }
    private fun setupViews() {
        // Speaker icon click - speak instruction
        binding.ivSpeaker.setOnClickListener {
            speakText(instructionText)
        }

        // Big microphone click - start voice recognition
        binding.fabMicrophone.setOnClickListener {
            startVoiceRecognition()
        }

        // Bottom button click
        binding.btnNextScreen.setOnClickListener {
            val truckNumber = binding.tvTruckNumber.text.toString()
            val intent = Intent(this, SecondSpeechScreen::class.java)
            intent.putExtra("truck_number", truckNumber)
            startActivity(intent)
            // Navigate to second screen
            Toast.makeText(this, "Dosri Screen par jaa rahe hain", Toast.LENGTH_SHORT).show()
            // val intent = Intent(this, SecondActivity::class.java)
            // startActivity(intent)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Hindi language not supported", Toast.LENGTH_SHORT).show()
            } else {
                ttsInitialized = true
                // Auto-speak on first load
                if (!hasSpokenOnce) {
                    binding.root.postDelayed({
                        speakText(instructionText)
                        hasSpokenOnce = true
                    }, 500)
                }
            }
        }
    }

    private fun speakText(text: String) {
        if (ttsInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Truck Number Bolo")
        }

        try {
            voiceRecognitionLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTruckNumber(input: String): String {
        // Remove spaces, hyphens and convert to uppercase
        val cleaned = input.replace(Regex("[\\s-]"), "").uppercase()

        // Match pattern: 2 letters, 2 digits, 4 digits
        val regex = Regex("([A-Z]{2})(\\d{2})(\\d{1,4})")
        val matchResult = regex.find(cleaned)

        return if (matchResult != null) {
            val (state, district, number) = matchResult.destructured
            "$state-$district-$number"
        } else {
            cleaned
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}