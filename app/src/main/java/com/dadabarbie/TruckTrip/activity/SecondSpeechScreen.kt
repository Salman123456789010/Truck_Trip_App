package com.dadabarbie.TruckTrip.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.databinding.ActivitySecondSpeechScreenBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    // Dialog field type
    private var dialogFieldType = FieldType.START_PLACE

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

    private val dialogVoiceRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                handleDialogVoiceInput(spokenText)
            }
        }
    }

    enum class FieldType {
        START_DATE, START_PLACE, END_PLACE
    }

    var langCode = ""
    private var currentDialog: Dialog? = null
    private var dialogEditText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        textToSpeech = TextToSpeech(this, this)

        checkEditMode()
        setupViews()
        updateUI()
        startArrowAnimation()
        langCode = Prefs[Constants.languageCode] ?: "hi"

        Constants.refreshApi.observe(this) {
            it.getContentIfNotHandled()?.let { event ->
                event.let {
                    if (it < 0) {
                        Log.d("its here", "onCreate: 1")
                    } else {
                        Log.d("its here", "onCreate: 2")
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
                binding.lottieMic.visibility = View.GONE
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
    }

    private fun checkEditMode() {
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        if (isEditMode) {
            tripId = intent.getStringExtra("TRIP_ID") ?: ""

            val startDate = intent.getStringExtra("START_DATE") ?: ""
            if (startDate.isNotEmpty()) {
                selectedDate = startDate
                binding.tvStartDateValue.text = startDate
                binding.cardStartDate.setCardBackgroundColor(getColor(R.color.white))
            }

            val startPlaceValue = intent.getStringExtra("START_PLACE") ?: ""
            if (startPlaceValue.isNotEmpty()) {
                startPlace = startPlaceValue
                binding.tvStartPlaceValue.text = startPlace
                binding.cardStartPlace.setCardBackgroundColor(getColor(R.color.white))
            }

            val endPlaceValue = intent.getStringExtra("END_PLACE") ?: ""
            if (endPlaceValue.isNotEmpty()) {
                endPlace = endPlaceValue
                binding.tvEndPlaceValue.text = endPlace
                binding.cardEndPlace.setCardBackgroundColor(getColor(R.color.white))
            }

            binding.btnSubmit.text = getString(R.string.edit_continue_karein)
            binding.tvInstruction?.visibility = View.VISIBLE
        }
    }

    private fun setupViews() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.ivSpeaker.setOnClickListener {
            speakCurrentInstruction()
        }

        binding.fabMicrophone.setOnClickListener {
            if (currentField == FieldType.START_DATE) {
                showDatePicker()
            } else {
                startVoiceRecognition()
            }
        }

        binding.cardStartDate.setOnClickListener {
            currentField = FieldType.START_DATE
            updateUI()
            speakCurrentInstruction()
        }

        binding.cardStartPlace.setOnClickListener {
            // Show desi dialog for start place
            showDesiPlaceDialog(FieldType.START_PLACE)
        }

        binding.cardEndPlace.setOnClickListener {
            // Show desi dialog for end place
            showDesiPlaceDialog(FieldType.END_PLACE)
        }

        binding.btnSubmit.setOnClickListener {
            if (validateInputs()) {
                val intent = Intent(this, ThirdExpenseScreen::class.java).apply {
                    putExtra("TRUCK_NUMBER", getIntent().getStringExtra("TRUCK_NUMBER"))
                    putExtra("START_DATE", selectedDate)
                    putExtra("START_PLACE", startPlace)
                    putExtra("END_PLACE", endPlace)
                    putExtra("EDIT_MODE", isEditMode)
                    putExtra("TRIP_ID", tripId)
                    putExtra("id", tripId)

                    if (isEditMode) {
                        putExtra("END_DATE", getIntent().getStringExtra("END_DATE"))
                        putExtra("DRIVER_INCOME", getIntent().getStringExtra("DRIVER_INCOME"))
                        putExtra("TOTAL_INCOME", getIntent().getStringExtra("TOTAL_INCOME"))
                        putExtra("TOTAL_EXPENSE", getIntent().getStringExtra("TOTAL_EXPENSE"))
                        putExtra("ORIGINAL_TRUCK_NUMBER", getIntent().getStringExtra("ORIGINAL_TRUCK_NUMBER"))
                        putExtra("ORIGINAL_START_DATE", getIntent().getStringExtra("ORIGINAL_START_DATE"))
                        putExtra("ORIGINAL_END_DATE", getIntent().getStringExtra("ORIGINAL_END_DATE"))
                        putExtra("ORIGINAL_START_PLACE", getIntent().getStringExtra("ORIGINAL_START_PLACE"))
                        putExtra("ORIGINAL_END_PLACE", getIntent().getStringExtra("ORIGINAL_END_PLACE"))
                    }
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.sari_fields_bharo), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDesiPlaceDialog(fieldType: FieldType) {
        dialogFieldType = fieldType

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_desi_place_input)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val etPlace = dialog.findViewById<EditText>(R.id.etPlaceInput)
        val btnMic = dialog.findViewById<ImageView>(R.id.btnDialogMic)
        val btnSave = dialog.findViewById<MaterialButton>(R.id.btnDialogSave)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnDialogCancel)

        // Set title based on field type
        tvTitle.text = if (fieldType == FieldType.START_PLACE) {
            getString(R.string.sarvat_no_place_nakho)
        } else {
            getString(R.string.end_no_place_nakho)
        }

        // Pre-fill existing value
        etPlace.setText(if (fieldType == FieldType.START_PLACE) startPlace else endPlace)

        // Store reference for voice input
        dialogEditText = etPlace
        currentDialog = dialog

        // Mic button click
        btnMic.setOnClickListener {
            startDialogVoiceRecognition()
        }

        // Save button
        btnSave.setOnClickListener {
            val inputText = etPlace.text.toString().trim()
            if (inputText.isNotEmpty()) {
                if (fieldType == FieldType.START_PLACE) {
                    startPlace = inputText
                    binding.tvStartPlaceValue.text = startPlace
                    binding.cardStartPlace.setCardBackgroundColor(getColor(R.color.white))
                } else {
                    endPlace = inputText
                    binding.tvEndPlaceValue.text = endPlace
                    binding.cardEndPlace.setCardBackgroundColor(getColor(R.color.white))
                }
                dialog.dismiss()
                Toast.makeText(this, getString(R.string.jagah_save_ho_gayi), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.jagah_ka_naam_daalo), Toast.LENGTH_SHORT).show()
            }
        }

        // Cancel button
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startDialogVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(RecognizerIntent.EXTRA_PROMPT,
                if (dialogFieldType == FieldType.START_PLACE)
                    getString(R.string.kaha_se_shuru_ho_rahe_ho_bolo)
                else
                    getString(R.string.kaha_jaana_hai_bolo)
            )
        }

        try {
            dialogVoiceRecognitionLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDialogVoiceInput(spokenText: String) {
        dialogEditText?.setText(spokenText)
    }

    override fun onStop() {
        super.onStop()
        saveDraft()
    }

    private fun saveDraft() {
        if (tripId.isNotEmpty()) {
            GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val db = com.dadabarbie.TruckTrip.room.AppDatabase.getDatabase(applicationContext)
                    val dao = db.productsDao()
                    val existing = dao.getDraftById(tripId)
                    if (existing != null) {
                        dao.update(
                            tripId,
                            existing.truckNumber,
                            startPlace,
                            endPlace,
                            selectedDate,
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale(langCode))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "language not supported", Toast.LENGTH_SHORT).show()
            } else {
                ttsInitialized = true
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
            FieldType.START_PLACE -> if (isEditMode) getString(R.string.start_place_badal_sakte_ho) else getString(R.string.kaha_se_shuru_ho_rahe_ho_bolo)
            FieldType.END_PLACE -> if (isEditMode) getString(R.string.end_place_badal_sakte_ho) else getString(R.string.kaha_jaana_hai_bolo)
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
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
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale(langCode))
                selectedDate = dateFormat.format(calendar.time)
                binding.tvStartDateValue.text = selectedDate
                binding.cardStartDate.setCardBackgroundColor(getColor(R.color.white))

                currentField = FieldType.START_PLACE
                updateUI()
                speakCurrentInstruction()
            },
            year, month, day
        )

        datePickerDialog.datePicker.calendarViewShown = true
        datePickerDialog.show()
    }

    private fun handleVoiceInput(spokenText: String) {
        when (currentField) {
            FieldType.START_PLACE -> {
                startPlace = spokenText
                binding.tvStartPlaceValue.text = startPlace
                binding.cardStartPlace.setCardBackgroundColor(getColor(R.color.white))

                currentField = FieldType.END_PLACE
                updateUI()
                speakCurrentInstruction()
            }
            FieldType.END_PLACE -> {
                endPlace = spokenText
                binding.tvEndPlaceValue.text = endPlace
                binding.cardEndPlace.setCardBackgroundColor(getColor(R.color.white))
            }
            else -> {}
        }
    }

    private fun updateUI() {
        binding.tvInstruction.text = getCurrentInstruction()

        if (currentField == FieldType.START_DATE) {
            binding.fabMicrophone.setImageResource(R.drawable.calendar)
        } else {
            binding.fabMicrophone.setImageResource(R.drawable.baseline_mic_24)
        }

        binding.tvStatus.text = when (currentField) {
            FieldType.START_DATE -> getString(R.string.calendar_khulega)
            else -> getString(R.string.mic_ko_dabaye_aur_bolo)
        }

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

    override fun onBackPressed() {
        super.onBackPressed()
        Constants.refreshApiGet(Event(-1))
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        currentDialog?.dismiss()
        super.onDestroy()
    }
}