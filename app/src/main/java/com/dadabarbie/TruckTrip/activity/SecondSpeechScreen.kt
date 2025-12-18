package com.dadabarbie.TruckTrip.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dadabarbie.TruckTrip.R
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

    private var currentField = FieldType.START_DATE
    private var selectedDate = ""
    private var startPlace = ""
    private var endPlace = ""


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

        setupViews()
        updateUI()
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
                if (validateInputs()) {
                    val intent = Intent(this, ThirdExpenseScreen::class.java)
                    intent.putExtra("truck_number", getIntent().getStringExtra("truck_number"))
                    intent.putExtra("start_date", selectedDate)
                    intent.putExtra("start_place", startPlace)
                    intent.putExtra("end_place", endPlace)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Sabhi fields bharo", Toast.LENGTH_SHORT).show()
                }
                Toast.makeText(this, "Trip details saved!", Toast.LENGTH_SHORT).show()
                // Navigate to next screen or save data
            } else {
                Toast.makeText(this, "Sabhi fields bharo", Toast.LENGTH_SHORT).show()
            }
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
                        speakCurrentInstruction()
                        hasSpokenOnce = true
                    }, 500)
                }
            }
        }
    }

    private fun getCurrentInstruction(): String {
        return when (currentField) {
            FieldType.START_DATE -> "Trip Shuru Hone Ki Tareekh Chuno"
            FieldType.START_PLACE -> "Kaha Se Shuru Ho Rahe Ho, Bolo"
            FieldType.END_PLACE -> "Kaha Jaana Hai, Bolo"
        }
    }

    private fun speakCurrentInstruction() {
        if (ttsInitialized) {
            textToSpeech.speak(getCurrentInstruction(), TextToSpeech.QUEUE_FLUSH, null, null)
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
                binding.cardStartDate.setCardBackgroundColor(getColor(android.R.color.holo_green_light))

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
                binding.cardStartPlace.setCardBackgroundColor(getColor(android.R.color.holo_green_light))

                // Move to next field
                currentField = FieldType.END_PLACE
                updateUI()
                speakCurrentInstruction()
            }
            FieldType.END_PLACE -> {
                endPlace = spokenText
                binding.tvEndPlaceValue.text = endPlace
                binding.cardEndPlace.setCardBackgroundColor(getColor(android.R.color.holo_green_light))

                Toast.makeText(this, "Sab details bhar gayi!", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    private fun updateUI() {
        // Update instruction text
        binding.tvInstruction.text = getCurrentInstruction()

        // Update microphone icon based on field
        if (currentField == FieldType.START_DATE) {
            binding.fabMicrophone.setImageResource(android.R.drawable.ic_menu_my_calendar)
        } else {
            binding.fabMicrophone.setImageResource(android.R.drawable.ic_btn_speak_now)
        }

        // Update status text
        binding.tvStatus.text = when (currentField) {
            FieldType.START_DATE -> "Calendar khulega"
            else -> "Mic ko dabaye aur bolo"
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
    }
}