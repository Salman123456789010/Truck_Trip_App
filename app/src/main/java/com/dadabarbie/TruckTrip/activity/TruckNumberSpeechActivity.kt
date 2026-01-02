package com.dadabarbie.TruckTrip.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.databinding.ActivityTruckNumberSpeechBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

class TruckNumberSpeechActivity : BaseActivity(), TextToSpeech.OnInitListener {
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

    // All valid Indian state codes
    private val validStateCodes = setOf(
        "AP", "AR", "AS", "BR", "CG", "GA", "GJ", "HR", "HP", "JH", "JK", "KA", "KL",
        "MP", "MH", "MN", "ML", "MZ", "NL", "OD", "OR", "PB", "RJ", "SK", "TN", "TS",
        "TR", "UP", "UK", "WB", "AN", "CH", "DD", "DL", "LD", "PY", "LA"
    )

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

                    // Try all results to find the best match
                    var bestMatch: String? = null
                    for (i in results.indices) {
                        val spokenText = results[i]
                        val corrected = correctCommonMistakes(spokenText)
                        val formatted = formatTruckNumber(corrected)

                        // Check if this is a valid truck number format
                        if (isValidTruckNumber(formatted)) {
                            bestMatch = formatted
                            Log.d("VoiceRecognition", "Found valid match: $formatted from '$spokenText'")
                            break
                        }
                    }

                    if (bestMatch != null) {
                        binding.tvTruckNumber.text = bestMatch
                        binding.cardTruckNumber.visibility = View.VISIBLE
                        speakText("Truck number $bestMatch save ho gaya")
                        Toast.makeText(this, "✓ $bestMatch", Toast.LENGTH_SHORT).show()
                    } else {
                        // Use first result as fallback
                        val fallback = formatTruckNumber(correctCommonMistakes(results[0]))
                        binding.tvTruckNumber.text = fallback
                        binding.cardTruckNumber.visibility = View.VISIBLE
                        Toast.makeText(this,
                            getString(R.string.please_verify, fallback), Toast.LENGTH_LONG).show()
                        speakText(
                            getString(
                                R.string.truck_number_save_ho_gaya_ek_baar_check_kar_lijiye,
                                fallback
                            ))
                    }
                } else {
                    Toast.makeText(this,
                        getString(R.string.kuch_sunai_nahi_diya_dobara_try_karein), Toast.LENGTH_SHORT).show()
                    speakText(getString(R.string.kuch_sunai_nahi_diya_dobara_try_karein))
                }
            }

            RESULT_CANCELED -> {
                Log.d("VoiceRecognition", "Recognition cancelled")
                Toast.makeText(this,
                    getString(R.string.cancelled_please_try_again), Toast.LENGTH_SHORT).show()
            }

            else -> {
                Log.d("VoiceRecognition", "Unknown result code: ${result.resultCode}")
                Toast.makeText(this,
                    getString(R.string.kuch_galat_ho_gaya_phir_se_try_karein), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            // 35 (android - 15)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        }
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
                Toast.makeText(this, getString(R.string.pehle_se_sun_rahe_hain), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, getString(R.string.pehle_truck_number_bolo), Toast.LENGTH_SHORT).show()
                speakText(getString(R.string.pehle_truck_number_bolo))
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
        if (currentNumber.isNotEmpty() && currentNumber != "GJ-11-1234") {
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

        // Show dialog first
        dialog.show()

        // Configure dialog window AFTER showing
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

            val displayMetrics = resources.displayMetrics
            val dialogWidth = (displayMetrics.widthPixels * 0.88).toInt()

            setLayout(
                dialogWidth,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )

            setGravity(android.view.Gravity.CENTER)

            attributes = attributes?.apply {
                verticalMargin = 0.05f
            }
        }

        // Focus and show keyboard after dialog is properly sized
        etTruckNumber.postDelayed({
            etTruckNumber.requestFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etTruckNumber, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
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
                                existing.modelList2,
                                updatedAt = System.currentTimeMillis()
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
        if (isListening) {
            Log.d("VoiceRecognition", "Already listening, ignoring request")
            return
        }

        if (ttsInitialized && textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }

        handler.postDelayed({
            try {
                isListening = true

                val prompt = if (isEditMode) {
                    getString(R.string.naya_truck_number_bolo_ya_same_rakho)
                } else {
                    getString(R.string.truck_number_bolo)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
                    putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                voiceRecognitionLauncher.launch(intent)
                Toast.makeText(this, getString(R.string.bol_rahe_hain), Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                isListening = false
                Log.e("VoiceRecognition", "Error starting recognition", e)
                Toast.makeText(this,
                    getString(R.string.voice_recognition_nahi_khul_raha_please_check_permissions), Toast.LENGTH_LONG).show()
            }
        }, 300)
    }

    /**
     * Validates if a truck number is in correct format
     */
    private fun isValidTruckNumber(number: String): Boolean {
        val parts = number.split("-")

        // Must have at least 3 parts (state-district-number) or 4 parts (state-district-series-number)
        if (parts.size < 3 || parts.size > 4) return false

        // First part must be valid state code
        if (!validStateCodes.contains(parts[0])) return false

        // Second part must be 1-2 digit district code
        if (!parts[1].matches(Regex("\\d{1,2}"))) return false

        // Last part must be 3-4 digit number
        val lastPart = parts.last()
        if (!lastPart.matches(Regex("\\d{3,4}"))) return false

        // If 4 parts, third part must be 1-3 letter series
        if (parts.size == 4) {
            if (!parts[2].matches(Regex("[A-Z]{1,3}"))) return false
        }

        return true
    }

    /**
     * Corrects common speech recognition mistakes
     */
    private fun correctCommonMistakes(input: String): String {
        var corrected = input.uppercase().trim()

        // Remove common filler words
        corrected = corrected
            .replace(Regex("\\bTRUCK\\s+NUMBER\\b"), "")
            .replace(Regex("\\bNUMBER\\b"), "")
            .replace(Regex("\\bIS\\b"), "")
            .trim()

        // State code corrections (phonetic similarities)
        val stateCorrections = mapOf(
            // GJ corrections
            "DJ" to "GJ", "DG" to "GJ", "BJ" to "GJ", "ZJ" to "GJ", "JJ" to "GJ",
            "GEE JAY" to "GJ", "DEE JAY" to "GJ", "JEE JAY" to "GJ",
            "GUJARAT" to "GJ", "GUJRAT" to "GJ",

            // MH corrections
            "EM AITCH" to "MH", "EM EACH" to "MH", "EMMA" to "MH",
            "MAHARASHTRA" to "MH", "BOMBAY" to "MH", "MUMBAI" to "MH",

            // PB corrections
            "PEE BEE" to "PB", "PEE BE" to "PB", "PUNJAB" to "PB",

            // RJ corrections
            "AR JAY" to "RJ", "ARE JAY" to "RJ", "RAJASTHAN" to "RJ",

            // MP corrections
            "EM PEE" to "MP", "EMMA P" to "MP", "MADHYA PRADESH" to "MP",

            // UP corrections
            "YOU PEE" to "UP", "YOU P" to "UP", "UTTAR PRADESH" to "UP",

            // DL corrections
            "DEE ELL" to "DL", "DEE L" to "DL", "DELHI" to "DL",

            // HR corrections
            "AITCH AR" to "HR", "EACH ARE" to "HR", "HARYANA" to "HR",

            // KA corrections
            "KEY AY" to "KA", "K A" to "KA", "KARNATAKA" to "KA",

            // TN corrections
            "TEE EN" to "TN", "T N" to "TN", "TAMIL NADU" to "TN",

            // WB corrections
            "DOUBLE YOU BEE" to "WB", "W B" to "WB", "WEST BENGAL" to "WB",

            // OR/OD corrections
            "OH AR" to "OR", "O R" to "OR", "ORISSA" to "OR", "ODISHA" to "OD"
        )

        stateCorrections.forEach { (wrong, right) ->
            corrected = corrected.replace(Regex("\\b$wrong\\b"), right)
        }

        // Number word corrections
        val numberCorrections = mapOf(
            "ZERO" to "0", "OH" to "0", "O" to "0",
            "ONE" to "1", "WON" to "1",
            "TO" to "2", "TOO" to "2", "TWO" to "2", "TU" to "2",
            "THREE" to "3", "TREE" to "3",
            "FOUR" to "4", "FOR" to "4", "FORE" to "4",
            "FIVE" to "5", "FIFE" to "5",
            "SIX" to "6", "SICKS" to "6",
            "SEVEN" to "7",
            "EIGHT" to "8", "ATE" to "8",
            "NINE" to "9", "NEIN" to "9"
        )

        numberCorrections.forEach { (wrong, right) ->
            corrected = corrected.replace(Regex("\\b$wrong\\b"), right)
        }

        // Letter corrections (for series like TT, AB, etc.)
        val letterCorrections = mapOf(
            "TEE TEE" to "TT", "T T" to "TT", "DOUBLE T" to "TT",
            "AY BEE" to "AB", "A B" to "AB",
            "BEE AY" to "BA", "B A" to "BA",
            "CEE DEE" to "CD", "C D" to "CD",
            "ES ES" to "SS", "S S" to "SS", "DOUBLE S" to "SS"
        )

        letterCorrections.forEach { (wrong, right) ->
            corrected = corrected.replace(Regex("\\b$wrong\\b"), right)
        }

        Log.d("VoiceCorrection", "Original: '$input' → Corrected: '$corrected'")
        return corrected
    }

    /**
     * Enhanced truck number formatting with support for all Indian formats
     */
    private fun formatTruckNumber(input: String): String {
        val normalized = input.uppercase()
            .replace("-", " ")
            .replace("_", " ")
            .replace(",", " ")
            .replace(".", " ")
            .trim()

        Log.d("Formatting", "Normalized input: '$normalized'")

        // Split by whitespace
        val tokens = normalized.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        Log.d("Formatting", "Tokens: $tokens")

        var state: String? = null
        var district: String? = null
        var series: String? = null
        var number: String? = null

        for (token in tokens) {
            when {
                // Compact format: GJ114567 or MH12AB1234
                token.matches(Regex("[A-Z]{2}\\d{5,6}")) -> {
                    state = token.substring(0, 2)
                    district = token.substring(2, 4)
                    number = token.substring(4)
                }

                // Full compact: MH12AB1234
                token.matches(Regex("[A-Z]{2}\\d{1,2}[A-Z]{1,3}\\d{3,4}")) -> {
                    val match = Regex("([A-Z]{2})(\\d{1,2})([A-Z]{1,3})(\\d{3,4})").find(token)
                    match?.let {
                        state = it.groupValues[1]
                        district = it.groupValues[2]
                        series = it.groupValues[3]
                        number = it.groupValues[4]
                    }
                }

                // State code (2 letters)
                state == null && token.matches(Regex("[A-Z]{2}")) && validStateCodes.contains(token) -> {
                    state = token
                }

                // District (1-2 digits)
                district == null && token.matches(Regex("\\d{1,2}")) -> {
                    district = token
                }

                // Series (1-3 letters)
                series == null && token.matches(Regex("[A-Z]{1,3}")) && state != null -> {
                    series = token
                }

                // Number (3-4 digits)
                number == null && token.matches(Regex("\\d{3,4}")) -> {
                    number = token
                }

                // Mixed formats
                token.matches(Regex("[A-Z]{2}\\d{1,2}")) -> {
                    state = state ?: token.substring(0, 2)
                    district = district ?: token.substring(2)
                }
            }
        }

        // Build the final truck number
        val result = mutableListOf<String>()

        state?.let {
            if (validStateCodes.contains(it)) {
                result.add(it)
            }
        }
        district?.let { result.add(it) }
        series?.let { result.add(it) }
        number?.let { result.add(it) }

        val formatted = if (result.size >= 3) {
            result.joinToString("-")
        } else {
            // Fallback: try to extract from original input
            input.uppercase().replace(Regex("[^A-Z0-9]"), "-").replace(Regex("-+"), "-").trim('-')
        }

        Log.d("Formatting", "Final result: '$formatted'")
        return formatted
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