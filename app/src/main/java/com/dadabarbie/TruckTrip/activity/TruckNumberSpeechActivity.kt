package com.dadabarbie.TruckTrip.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.databinding.ActivityOtpverificationScreenBinding
import com.dadabarbie.TruckTrip.databinding.ActivityTruckNumberSpeechBinding
import java.util.Locale

class TruckNumberSpeechActivity : AppCompatActivity(),TextToSpeech.OnInitListener {
    private val binding: ActivityTruckNumberSpeechBinding by lazy {
        ActivityTruckNumberSpeechBinding.inflate(layoutInflater)
    }
    private var isEditMode = false
    private var tripId = ""
    private lateinit var textToSpeech: TextToSpeech
    private var ttsInitialized = false
    private var hasSpokenOnce = false
    private var arrowAnimator: ValueAnimator? = null
    private var instructionText = ""

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
        binding.lottieMic.playAnimation()
        instructionText=getString(R.string.yaha_pe_truck_number_bolo)
        binding.lottieMic.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                // Hide animation
                binding.lottieMic.visibility = View.GONE

                // Show mic button
                binding.ivMicrophone.visibility = View.VISIBLE
                binding.lottieMic.alpha = 0f
                binding.lottieMic.animate().alpha(1f).setDuration(300).start()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        checkEditMode()
        setupViews()
        startArrowAnimation()

        Constants.refreshApi.observe(this) {
            it.getContentIfNotHandled()?.let { event ->
                event.let {
                    if (it < 0) {

                    } else {
                        Constants.refreshApiGet(Event(1))
                        finish()
                    }
                }
            }

        }
    }
    private fun checkEditMode() {
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        if (isEditMode) {
            tripId = intent.getStringExtra("TRIP_ID") ?: ""
            val truckNumber = intent.getStringExtra("TRUCK_NUMBER") ?: ""

            // Pre-fill truck number
            if (truckNumber.isNotEmpty()) {
                binding.tvTruckNumber.text = truckNumber
                binding.cardTruckNumber.visibility = android.view.View.VISIBLE
            }

            // Change instruction text
            instructionText = getString(R.string.trip_edit_kar_rahe_hain)
            binding.tvInstruction.text = instructionText

            // Change button text
            binding.btnNextScreen.text = getString(R.string.aage_badho_edit_karein)

            // Show edit indicator (optional - add ImageView in XML)
            binding.ivEditIndicator?.visibility = android.view.View.VISIBLE

            // Change status text
            binding.tvStatus.text = getString(R.string.edit_mode_truck_number_badal_sakte_ho)
        }
    }

    private fun startArrowAnimation() {
        arrowAnimator = ValueAnimator.ofFloat(0f, 10f, 0f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                binding.btnNextScreen.translationX = value
            }

            start()
        }
        Log.d("Animation", "✅ Button animation started")
    }

    private fun setupViews() {
        // Speaker icon click - speak instruction
        binding.ivSpeaker.setOnClickListener {
            speakText(instructionText)
        }

        // Big microphone click - start voice recognition
        binding.ivMicrophone.setOnClickListener {
            startVoiceRecognition()
        }

        // Bottom button click
        binding.btnNextScreen.setOnClickListener {
            val truckNumber = binding.tvTruckNumber.text.toString()

            if (truckNumber.isEmpty()) {
                Toast.makeText(this, "Pehle truck number bolo", Toast.LENGTH_SHORT).show()
                speakText("Pehle truck number bolo")
                return@setOnClickListener
            }

            // Navigate to Screen 2
            val intent = Intent(this, SecondSpeechScreen::class.java).apply {
                putExtra("TRUCK_NUMBER", truckNumber)
                putExtra("EDIT_MODE", isEditMode)
                putExtra("TRIP_ID", tripId)

                // Pass other edit data if in edit mode
                if (isEditMode) {
                    putExtra("START_DATE", getIntent().getStringExtra("START_DATE"))
                    putExtra("END_DATE", getIntent().getStringExtra("END_DATE"))
                    putExtra("START_PLACE", getIntent().getStringExtra("START_PLACE"))
                    putExtra("END_PLACE", getIntent().getStringExtra("END_PLACE"))
                    putExtra("DRIVER_INCOME", getIntent().getStringExtra("DRIVER_INCOME"))
                    putExtra("TOTAL_INCOME", getIntent().getStringExtra("TOTAL_INCOME"))
                    putExtra("TOTAL_EXPENSE", getIntent().getStringExtra("TOTAL_EXPENSE"))

                    // Forward ORIGINAL values
                    putExtra("ORIGINAL_TRUCK_NUMBER", getIntent().getStringExtra("ORIGINAL_TRUCK_NUMBER"))
                    putExtra("ORIGINAL_START_DATE", getIntent().getStringExtra("ORIGINAL_START_DATE"))
                    putExtra("ORIGINAL_END_DATE", getIntent().getStringExtra("ORIGINAL_END_DATE"))
                    putExtra("ORIGINAL_START_PLACE", getIntent().getStringExtra("ORIGINAL_START_PLACE"))
                    putExtra("ORIGINAL_END_PLACE", getIntent().getStringExtra("ORIGINAL_END_PLACE"))
                }
            }

            startActivity(intent)

            // Close this screen in edit mode to prevent back navigation issues
            if (isEditMode) {
                finish()
            }
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()

        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Constants.refreshApiGet(Event(1))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("en", "IN"))
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
        val prompt = if (isEditMode) {
            getString(R.string.naya_truck_number_bolo_ya_same_rakho)
        } else {
            getString(R.string.truck_number_bolo)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }

        try {
            voiceRecognitionLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTruckNumber(input: String): String {

        val normalized = input.uppercase()
            .replace("-", " ")
            .replace("_", " ")
            .trim()

        val tokens = normalized.split("\\s+".toRegex())

        var state: String? = null
        var district: String? = null
        var series: String? = null
        var number: String? = null

        for (token in tokens) {

            when {

                // Case 1: PB114567 → PB-11-4567
                token.matches(Regex("[A-Z]{2}\\d{5,6}")) -> {
                    state = state ?: token.substring(0, 2)
                    district = district ?: token.substring(2, 4)
                    number = number ?: token.substring(4)
                }

                // Case 2: MH12AB1234
                token.matches(Regex("[A-Z]{2}\\d{1,2}[A-Z]{1,3}\\d{3,4}")) -> {
                    val match = Regex("([A-Z]{2})(\\d{1,2})([A-Z]{1,3})(\\d{3,4})").find(token)
                    match?.let {
                        state = state ?: it.groupValues[1]
                        district = district ?: it.groupValues[2]
                        series = series ?: it.groupValues[3]
                        number = number ?: it.groupValues[4]
                    }
                }

                // Case 3: RJ14
                token.matches(Regex("[A-Z]{2}\\d{1,2}")) -> {
                    state = state ?: token.substring(0, 2)
                    district = district ?: token.substring(2)
                }

                // Case 4: Letters only (RJ / GB)
                token.matches(Regex("[A-Z]{1,3}")) -> {
                    if (state == null) state = token
                    else if (series == null) series = token
                }

                // Case 5: Numbers only
                token.matches(Regex("\\d{1,4}")) -> {
                    when {
                        district == null && token.length <= 2 -> district = token
                        number == null && token.length >= 3 -> number = token
                    }
                }
            }
        }

        // ✅ STRICT INDIAN ORDER
        val result = mutableListOf<String>()
        state?.let { result.add(it) }
        district?.let { result.add(it) }
        series?.let { result.add(it) }
        number?.let { result.add(it) }

        return if (result.isNotEmpty()) result.joinToString("-") else input
    }




    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}