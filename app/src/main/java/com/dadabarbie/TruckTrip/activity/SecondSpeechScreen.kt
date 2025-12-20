package com.dadabarbie.TruckTrip.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
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
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.databinding.ActivitySecondSpeechScreenBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import java.util.*



class SecondSpeechScreen : AppCompatActivity(), TextToSpeech.OnInitListener {
    private val binding: ActivitySecondSpeechScreenBinding by lazy {
        ActivitySecondSpeechScreenBinding.inflate(layoutInflater)
    }
    private lateinit var textToSpeech: TextToSpeech
    private var ttsInitialized = false
    private var hasSpokenOnce = false

    // EDIT MODE VARIABLES
    private var isEditMode = false
    private var tripId = ""

    private var currentField = FieldType.START_DATE
    private var selectedDate = ""
    private var startPlace = ""
    private var endPlace = ""
    private var arrowAnimator: ValueAnimator? = null
    private val voiceRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                handleVoiceInput(spokenText)
            }
        }
    }

    enum class FieldType {
        START_DATE, START_PLACE, END_PLACE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        textToSpeech = TextToSpeech(this, this)

        // CHECK EDIT MODE
        checkEditMode()

        setupViews()
        updateUI()
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

        binding.lottieMic.playAnimation()

        binding.lottieMic.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                // Hide animation
                binding.lottieMic.visibility = View.GONE

                // Show mic button
                binding.fabMicrophone.visibility = View.VISIBLE
                binding.lottieMic.alpha = 0f
                binding.lottieMic.animate().alpha(1f).setDuration(300).start()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    private fun startArrowAnimation() {
        arrowAnimator = ValueAnimator.ofFloat(0f, 10f, 0f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                binding.btnSubmit.translationX = value
            }

            start()
        }
        Log.d("Animation", "✅ Button animation started")
    }
    /**
     * Check if opened in edit mode and pre-fill data
     */
    private fun checkEditMode() {
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        if (isEditMode) {
            tripId = intent.getStringExtra("TRIP_ID") ?: ""

            // Pre-fill start date
            val startDate = intent.getStringExtra("START_DATE") ?: ""
            if (startDate.isNotEmpty()) {
                selectedDate = startDate
                binding.tvStartDateValue.text = startDate
                binding.cardStartDate.setCardBackgroundColor(getColor(R.color.white))
            }

            // Pre-fill start place
            val startPlaceValue = intent.getStringExtra("START_PLACE") ?: ""
            if (startPlaceValue.isNotEmpty()) {
                startPlace = startPlaceValue
                binding.tvStartPlaceValue.text = startPlace
                binding.cardStartPlace.setCardBackgroundColor(getColor(R.color.white))
            }

            // Pre-fill end place
            val endPlaceValue = intent.getStringExtra("END_PLACE") ?: ""
            if (endPlaceValue.isNotEmpty()) {
                endPlace = endPlaceValue
                binding.tvEndPlaceValue.text = endPlace
                binding.cardEndPlace.setCardBackgroundColor(getColor(R.color.white))
            }

            // Change button text
            binding.btnSubmit.text = getString(R.string.edit_continue_karein)

            // Change header text
//            binding.tvTitle?.text = "Trip Details Edit Karein"

            // Show edit indicator (if you have it in XML)
            binding.tvInstruction?.visibility = View.VISIBLE
        }
    }

    private fun setupViews() {
        // Back button
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Speaker icon - speak current instruction
        binding.ivSpeaker.setOnClickListener {
            speakCurrentInstruction()
        }

        // Big microphone - start voice recognition
        binding.fabMicrophone.setOnClickListener {
            if (currentField == FieldType.START_DATE) {
                showDatePicker()
            } else {
                startVoiceRecognition()
            }
        }

        // Start Date Card
        binding.cardStartDate.setOnClickListener {
            currentField = FieldType.START_DATE
            updateUI()
            speakCurrentInstruction()
        }

        // Start Place Card
        binding.cardStartPlace.setOnClickListener {
            currentField = FieldType.START_PLACE
            updateUI()
            speakCurrentInstruction()
        }

        // End Place Card
        binding.cardEndPlace.setOnClickListener {
            currentField = FieldType.END_PLACE
            updateUI()
            speakCurrentInstruction()
        }

        // Submit button
        binding.btnSubmit.setOnClickListener {
            if (validateInputs()) {
                val intent = Intent(this, ThirdExpenseScreen::class.java).apply {
                    putExtra("TRUCK_NUMBER", getIntent().getStringExtra("TRUCK_NUMBER"))
                    putExtra("START_DATE", selectedDate)
                    putExtra("START_PLACE", startPlace)
                    putExtra("END_PLACE", endPlace)
                    putExtra("EDIT_MODE", isEditMode)
                    putExtra("TRIP_ID", tripId)
                    putExtra("id", tripId) // Pass tripId as "id" for ThirdExpenseScreen to identify draft

                    // Pass additional edit data
                    if (isEditMode) {
                        putExtra("END_DATE", getIntent().getStringExtra("END_DATE"))
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

                // Close in edit mode
                if (isEditMode) {
                    finish()
                }
            } else {
                Toast.makeText(this, getString(R.string.sari_fields_bharo), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "language not supported", Toast.LENGTH_SHORT).show()
            } else {
                ttsInitialized = true
                // Auto-speak on first load
                if (!hasSpokenOnce) {
                    binding.root.postDelayed({
                        if (isEditMode) {
                            speakText(getString(R.string.trip_details_edit_karein_chahiye_to_change_karo))
                        } else {
                            speakCurrentInstruction()
                        }
                        hasSpokenOnce = true
                    }, 500)
                }
            }
        }
    }

    private fun getCurrentInstruction(): String {
        return when (currentField) {
            FieldType.START_DATE -> if (isEditMode) getString(R.string.tareekh_badal_sakte_ho) else getString(R.string.trip_shuru_hone_ki_tareekh_chuno)
            FieldType.START_PLACE -> if (isEditMode) getString(R.string.start_place_badal_sakte_ho) else getString(
                R.string.kaha_se_shuru_ho_rahe_ho_bolo
            )
            FieldType.END_PLACE -> if (isEditMode) getString(R.string.end_place_badal_sakte_ho) else getString(
                R.string.kaha_jaana_hai_bolo
            )
        }
    }

    private fun speakCurrentInstruction() {
        if (ttsInitialized) {
            textToSpeech.speak(getCurrentInstruction(), TextToSpeech.QUEUE_FLUSH, null, null)
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, getCurrentInstruction())
        }

        try {
            voiceRecognitionLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN"))
                selectedDate = dateFormat.format(calendar.time)
                binding.tvStartDateValue.text = selectedDate
                binding.cardStartDate.setCardBackgroundColor(getColor(R.color.white))

                // Move to next field
                currentField = FieldType.START_PLACE
                updateUI()
                speakCurrentInstruction()
            },
            year, month, day
        )

        // Make calendar dialog larger
        datePickerDialog.datePicker.calendarViewShown = true
        datePickerDialog.show()
    }

    private fun handleVoiceInput(spokenText: String) {
        when (currentField) {
            FieldType.START_PLACE -> {
                startPlace = spokenText
                binding.tvStartPlaceValue.text = startPlace
                binding.cardStartPlace.setCardBackgroundColor(getColor(R.color.white))

                // Move to next field
                currentField = FieldType.END_PLACE
                updateUI()
                speakCurrentInstruction()
            }
            FieldType.END_PLACE -> {
                endPlace = spokenText
                binding.tvEndPlaceValue.text = endPlace
                binding.cardEndPlace.setCardBackgroundColor(getColor(R.color.white))

//                Toast.makeText(this, "Sab details bhar gayi!", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    private fun updateUI() {
        // Update instruction text
        binding.tvInstruction.text = getCurrentInstruction()

        // Update microphone icon based on field
        if (currentField == FieldType.START_DATE) {
            binding.fabMicrophone.setImageResource(R.drawable.calendar)
        } else {
            binding.fabMicrophone.setImageResource(R.drawable.baseline_mic_24)
        }

        // Update status text
        binding.tvStatus.text = when (currentField) {
            FieldType.START_DATE -> getString(R.string.calendar_khulega)
            else -> getString(R.string.mic_ko_dabaye_aur_bolo)
        }

        // Highlight active card
        resetCardColors()
        when (currentField) {
            FieldType.START_DATE -> binding.cardStartDate.strokeWidth = 4
            FieldType.START_PLACE -> binding.cardStartPlace.strokeWidth = 4
            FieldType.END_PLACE -> binding.cardEndPlace.strokeWidth = 4
        }
    }

    private fun resetCardColors() {
        binding.cardStartDate.strokeWidth = 0
        binding.cardStartPlace.strokeWidth = 0
        binding.cardEndPlace.strokeWidth = 0
    }

    private fun validateInputs(): Boolean {
        return selectedDate.isNotEmpty() && startPlace.isNotEmpty() && endPlace.isNotEmpty()
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }}