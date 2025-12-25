package com.dadabarbie.TruckTrip.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.databinding.ActivityTruckNumberSpeechBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

class TruckNumberSpeechActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
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
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())

    // Voice recognition launcher with improved handling
    private val voiceRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false

        when (result.resultCode) {
            RESULT_OK -> {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val confidences = data?.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES)

                if (!results.isNullOrEmpty()) {
                    Log.d("VoiceRecognition", "Results: $results")
                    Log.d("VoiceRecognition", "Confidences: ${confidences?.contentToString()}")

                    // Get the best result
                    val spokenText = results[0]
                    val correctedText = correctCommonMistakes(spokenText)
                    val formattedNumber = formatTruckNumber(correctedText)

                    binding.tvTruckNumber.text = formattedNumber
                    binding.cardTruckNumber.visibility = View.VISIBLE

                    // Confirm with user
                    speakText("Truck number $formattedNumber save ho gaya")
                    Toast.makeText(this, "✓ $formattedNumber", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Kuch sunai nahi diya, dobara try karein", Toast.LENGTH_SHORT).show()
                    speakText("Kuch sunai nahi diya, dobara try karein")
                }
            }

            RESULT_CANCELED -> {
                Log.d("VoiceRecognition", "Recognition cancelled")
                Toast.makeText(this, "Cancelled - Please try again", Toast.LENGTH_SHORT).show()
            }

            else -> {
                Log.d("VoiceRecognition", "Unknown result code: ${result.resultCode}")
                Toast.makeText(this, "Kuch galat ho gaya, phir se try karein", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        textToSpeech = TextToSpeech(this, this)
        binding.lottieMic.playAnimation()
        instructionText = getString(R.string.yaha_pe_truck_number_bolo)


        binding.lottieMic.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                binding.lottieMic.visibility = View.GONE
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
                if (event >= 0) {
                    Constants.refreshApiGet(Event(1))
                    finish()
                }
            }
        }
    }

    private fun checkEditMode() {
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        if (isEditMode) {
            tripId = intent.getStringExtra("TRIP_ID") ?: ""
            val truckNumber = intent.getStringExtra("TRUCK_NUMBER") ?: ""

            if (truckNumber.isNotEmpty()) {
                binding.tvTruckNumber.text = truckNumber
                binding.cardTruckNumber.visibility = View.VISIBLE
            }

            instructionText = getString(R.string.trip_edit_kar_rahe_hain)
            binding.tvInstruction.text = instructionText
            binding.btnNextScreen.text = getString(R.string.aage_badho_edit_karein)
//            binding.ivEditIndicator?.visibility = View.VISIBLE
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
    }

    private fun setupViews() {
        binding.ivSpeaker.setOnClickListener {
            speakText(instructionText)
        }

        binding.ivMicrophone.setOnClickListener {
            if (!isListening) {
                startVoiceRecognition()
            } else {
                Toast.makeText(this, "Pehle se sun rahe hain...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardTruckNumber.setOnClickListener {
            showEditTruckNumberDialog()
        }

        binding.tvTruckNumber.setOnClickListener {
            showEditTruckNumberDialog()
        }

        binding.btnNextScreen.setOnClickListener {
            val truckNumber = binding.tvTruckNumber.text.toString()

            if (truckNumber.isEmpty()) {
                Toast.makeText(this, "Pehle truck number bolo", Toast.LENGTH_SHORT).show()
                speakText("Pehle truck number bolo")
                return@setOnClickListener
            }

            val intent = Intent(this, SecondSpeechScreen::class.java).apply {
                putExtra("TRUCK_NUMBER", truckNumber)
                putExtra("EDIT_MODE", isEditMode)
                putExtra("TRIP_ID", tripId)

                if (isEditMode) {
                    putExtra("START_DATE", getIntent().getStringExtra("START_DATE"))
                    putExtra("END_DATE", getIntent().getStringExtra("END_DATE"))
                    putExtra("START_PLACE", getIntent().getStringExtra("START_PLACE"))
                    putExtra("END_PLACE", getIntent().getStringExtra("END_PLACE"))
                    putExtra("DRIVER_INCOME", getIntent().getStringExtra("DRIVER_INCOME"))
                    putExtra("TOTAL_INCOME", getIntent().getStringExtra("TOTAL_INCOME"))
                    putExtra("TOTAL_EXPENSE", getIntent().getStringExtra("TOTAL_EXPENSE"))

                    // FIXED: Safely handle ROUTE_ARRAY
                    val routeArray = getIntent().getStringArrayListExtra("ROUTE_ARRAY")
                    if (routeArray != null) {
                        putStringArrayListExtra("ROUTE_ARRAY", routeArray)
                    }
                    Log.d("ROUTE_ARRAY", "Complete Route: ${routeArray}")

                    putExtra("ORIGINAL_TRUCK_NUMBER", getIntent().getStringExtra("ORIGINAL_TRUCK_NUMBER"))
                    putExtra("ORIGINAL_START_DATE", getIntent().getStringExtra("ORIGINAL_START_DATE"))
                    putExtra("ORIGINAL_END_DATE", getIntent().getStringExtra("ORIGINAL_END_DATE"))
                    putExtra("ORIGINAL_START_PLACE", getIntent().getStringExtra("ORIGINAL_START_PLACE"))
                    putExtra("ORIGINAL_END_PLACE", getIntent().getStringExtra("ORIGINAL_END_PLACE"))
                }
            }

            startActivity(intent)
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        saveDraft()
    }

    private fun showEditTruckNumberDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_truck_number, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.RoundedDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val etTruckNumber = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTruckNumber)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val ivClose = dialogView.findViewById<ImageView>(R.id.ivClose)

        val currentNumber = binding.tvTruckNumber.text.toString()
        if (currentNumber != "GJ-11-1234") {
            etTruckNumber.setText(currentNumber)
            etTruckNumber.setSelection(currentNumber.length)
        }

        btnSave.setOnClickListener {
            val newNumber = etTruckNumber.text.toString().trim()

            if (newNumber.isEmpty()) {
                Toast.makeText(this, getString(R.string.truck_number_khali_nahi_ho_sakta), Toast.LENGTH_SHORT).show()
                speakText(getString(R.string.truck_number_khali_nahi_ho_sakta))
                return@setOnClickListener
            }

            if (newNumber.length < 5) {
                Toast.makeText(this, getString(R.string.sahi_truck_number_daliye), Toast.LENGTH_SHORT).show()
                speakText(getString(R.string.sahi_truck_number_daliye))
                return@setOnClickListener
            }

            val formattedNumber = formatTruckNumber(newNumber)
            binding.tvTruckNumber.text = formattedNumber
            binding.cardTruckNumber.visibility = View.VISIBLE

            Toast.makeText(this, getString(R.string.truck_number_badal_diya_gaya), Toast.LENGTH_SHORT).show()
            speakText(getString(R.string.truck_number_badal_diya_gaya))
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        ivClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            etTruckNumber.requestFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etTruckNumber, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        dialog.show()
    }

    private fun saveDraft() {
        if (tripId.isNotEmpty()) {
            val truckNumber = binding.tvTruckNumber.text.toString()
            if (truckNumber.isNotEmpty()) {
                GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val db = com.dadabarbie.TruckTrip.room.AppDatabase.getDatabase(applicationContext)
                        val dao = db.productsDao()
                        val existing = dao.getDraftById(tripId)
                        if (existing != null) {
                            dao.update(
                                tripId,
                                truckNumber,
                                existing.srcPlace,
                                existing.destPlace,
                                existing.srcDate,
                                existing.destDate,
                                existing.avg,
                                existing.modelList1,
                                existing.modelList2
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
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
        // Prevent multiple simultaneous listeners
        if (isListening) {
            Log.d("VoiceRecognition", "Already listening, ignoring request")
            return
        }

        // Stop TTS if speaking
        if (ttsInitialized && textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }

        // Wait a moment for TTS to fully stop
        handler.postDelayed({
            try {
                isListening = true

                val prompt = if (isEditMode) {
                    getString(R.string.naya_truck_number_bolo_ya_same_rakho)
                } else {
                    getString(R.string.truck_number_bolo)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    // Use multiple language models for better accuracy
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")

                    // Request multiple results for better accuracy
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

                    // Get confidence scores
                    putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)

                    // Better audio settings
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)

                    putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                voiceRecognitionLauncher.launch(intent)

                // Visual feedback
                Toast.makeText(this, "🎤 Bol rahe hain...", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                isListening = false
                Log.e("VoiceRecognition", "Error starting recognition", e)
                Toast.makeText(this, "Voice recognition nahi khul raha, please check permissions", Toast.LENGTH_LONG).show()
            }
        }, 300) // 300ms delay to ensure TTS has stopped
    }

    /**
     * Corrects common speech recognition mistakes
     */
    private fun correctCommonMistakes(input: String): String {
        var corrected = input.uppercase()

        // Common misrecognitions - DJ -> GJ, BJ -> GJ, etc.
        val corrections = mapOf(
            "DJ" to "GJ",
            "DG" to "GJ",
            "BJ" to "GJ",
            "ZJ" to "GJ",
            "JJ" to "GJ",
            "ZERO" to "0",
            "OH" to "0",
            "ONE" to "1",
            "TO" to "2",
            "TOO" to "2",
            "TWO" to "2",
            "THREE" to "3",
            "FOUR" to "4",
            "FOR" to "4",
            "FIVE" to "5",
            "SIX" to "6",
            "SEVEN" to "7",
            "EIGHT" to "8",
            "ATE" to "8",
            "NINE" to "9"
        )

        // Apply corrections
        corrections.forEach { (wrong, right) ->
            corrected = corrected.replace(Regex("\\b$wrong\\b"), right)
        }

        // Handle phonetic similarities
        corrected = corrected
            .replace(Regex("\\bDEE\\s+JAY\\b"), "GJ")
            .replace(Regex("\\bJEE\\s+JAY\\b"), "GJ")
            .replace(Regex("\\bEM\\s+AITCH\\b"), "MH")
            .replace(Regex("\\bKEY\\s+AY\\b"), "KA")
            .replace(Regex("\\bAR\\s+JAY\\b"), "RJ")

        Log.d("VoiceCorrection", "Original: $input → Corrected: $corrected")
        return corrected
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
                // PB114567 → PB-11-4567
                token.matches(Regex("[A-Z]{2}\\d{5,6}")) -> {
                    state = state ?: token.substring(0, 2)
                    district = district ?: token.substring(2, 4)
                    number = number ?: token.substring(4)
                }

                // MH12AB1234
                token.matches(Regex("[A-Z]{2}\\d{1,2}[A-Z]{1,3}\\d{3,4}")) -> {
                    val match = Regex("([A-Z]{2})(\\d{1,2})([A-Z]{1,3})(\\d{3,4})").find(token)
                    match?.let {
                        state = state ?: it.groupValues[1]
                        district = district ?: it.groupValues[2]
                        series = series ?: it.groupValues[3]
                        number = number ?: it.groupValues[4]
                    }
                }

                // RJ14
                token.matches(Regex("[A-Z]{2}\\d{1,2}")) -> {
                    state = state ?: token.substring(0, 2)
                    district = district ?: token.substring(2)
                }

                // Letters only
                token.matches(Regex("[A-Z]{1,3}")) -> {
                    if (state == null) state = token
                    else if (series == null) series = token
                }

                // Numbers only
                token.matches(Regex("\\d{1,4}")) -> {
                    when {
                        district == null && token.length <= 2 -> district = token
                        number == null && token.length >= 3 -> number = token
                    }
                }
            }
        }

        val result = mutableListOf<String>()
        state?.let { result.add(it) }
        district?.let { result.add(it) }
        series?.let { result.add(it) }
        number?.let { result.add(it) }

        return if (result.isNotEmpty()) result.joinToString("-") else input
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        arrowAnimator?.cancel()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}