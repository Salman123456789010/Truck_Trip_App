package com.dadabarbie.TruckTrip.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.Utils.RouteUtils
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.databinding.ActivitySecondSpeechScreenBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SecondSpeechScreen : BaseActivity(), TextToSpeech.OnInitListener {
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

    // Multiple stops list
    private val middleStops = mutableListOf<String>()

    // Complete route array for API
    private fun getCompleteRouteArray(): ArrayList<String> {
        val routeArray = ArrayList<String>()

        // Add start place
        if (startPlace.isNotEmpty()) {
            routeArray.add(startPlace)
        }

        // Add all middle stops
        routeArray.addAll(middleStops)

        // Add end place
        if (endPlace.isNotEmpty()) {
            routeArray.add(endPlace)
        }

        return routeArray
    }

    // Dialog field type
    private var dialogFieldType = FieldType.START_PLACE
    private var editingStopIndex = -1

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
        START_DATE, START_PLACE, END_PLACE, MIDDLE_STOP
    }

    var langCode = ""
    private var currentDialog: Dialog? = null
    private var dialogEditText: EditText? = null

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

        // ✅ Set today's date by default
        setTodayDateByDefault()

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

    // ✅ NEW: Set today's date by default
    private fun setTodayDateByDefault() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("hi"))
        selectedDate = dateFormat.format(calendar.time)
        binding.tvStartDateValue.text = selectedDate
        binding.cardStartDate.setCardBackgroundColor(getColor(R.color.white))
        Log.d("DateSetup", "Today's date set: $selectedDate")
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

            // Get route array from intent
            val routeArray = intent.getStringArrayListExtra("ROUTE_ARRAY")

            if (routeArray != null && routeArray.isNotEmpty()) {
                // First element is always start place
                startPlace = routeArray[0]
                binding.tvStartPlaceValue.text = startPlace
                binding.cardStartPlace.setCardBackgroundColor(getColor(R.color.white))

                // Last element is always end place
                if (routeArray.size > 1) {
                    endPlace = routeArray[routeArray.size - 1]
                    binding.tvEndPlaceValue.text = endPlace
                    binding.cardEndPlace.setCardBackgroundColor(getColor(R.color.white))
                }

                // Middle elements are stops (if more than 2 elements)
                if (routeArray.size > 2) {
                    middleStops.clear()
                    for (i in 1 until routeArray.size - 1) {
                        middleStops.add(routeArray[i])
                    }
                    // Refresh UI to show middle stops
                    refreshMiddleStopsUI()
                }
            } else {
                // Fallback to old method if ROUTE_ARRAY not available
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
            }

            // Handle start date
            val startDate = intent.getStringExtra("START_DATE") ?: ""
            if (startDate.isNotEmpty()) {
                selectedDate = startDate
                binding.tvStartDateValue.text = startDate
                binding.cardStartDate.setCardBackgroundColor(getColor(R.color.white))
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
            showDatePicker()
//            if (currentField == FieldType.START_DATE) {
//
//            } else {
//                startVoiceRecognition()
//            }
        }

//        binding.cardStartDate.setOnClickListener {
//            currentField = FieldType.START_DATE
//            updateUI()
//            speakCurrentInstruction()
//        }

        binding.cardStartPlace.setOnClickListener {
            showDesiPlaceDialog(FieldType.START_PLACE)
        }

        binding.cardEndPlace.setOnClickListener {
            showDesiPlaceDialog(FieldType.END_PLACE)
        }

        // ✅ UPDATED: Add stop button - adds BEFORE end place
        binding.btnAddStop?.setOnClickListener {
            if (startPlace.isEmpty()) {
                Toast.makeText(this, getString(R.string.pehle_start_place_dalo), Toast.LENGTH_SHORT)
                    .show()
                speakText(getString(R.string.pehle_start_place_dalo))
                return@setOnClickListener
            }
            if (endPlace.isEmpty()) {
                Toast.makeText(this, getString(R.string.pehle_end_place_dalo), Toast.LENGTH_SHORT)
                    .show()
                speakText(getString(R.string.pehle_end_place_dalo))
                return@setOnClickListener
            }
            showDesiPlaceDialog(FieldType.MIDDLE_STOP)
        }

        binding.btnSubmit.setOnClickListener {
            if (validateInputs()) {
                // Get complete route array
                val completeRoute = getCompleteRouteArray()

                val intent = Intent(this, ThirdExpenseScreen::class.java).apply {
                    putExtra("TRUCK_NUMBER", getIntent().getStringExtra("TRUCK_NUMBER"))
                    putExtra("START_DATE", selectedDate)
                    putExtra("START_PLACE", startPlace)
                    putExtra("END_PLACE", endPlace)

                    // Pass complete route array
                    putStringArrayListExtra("ROUTE_ARRAY", completeRoute)

                    // Also pass as JSON string for easy API usage
                    putExtra("ROUTE_JSON", com.google.gson.Gson().toJson(completeRoute))

                    putExtra("EDIT_MODE", isEditMode)
                    putExtra("TRIP_ID", tripId)
                    putExtra("id", tripId)

                    if (isEditMode) {
                        putExtra("END_DATE", getIntent().getStringExtra("END_DATE"))
                        putExtra("DRIVER_INCOME", getIntent().getStringExtra("DRIVER_INCOME"))
                        putExtra("TOTAL_INCOME", getIntent().getStringExtra("TOTAL_INCOME"))
                        putExtra("TOTAL_EXPENSE", getIntent().getStringExtra("TOTAL_EXPENSE"))
                        putExtra(
                            "ORIGINAL_TRUCK_NUMBER",
                            getIntent().getStringExtra("ORIGINAL_TRUCK_NUMBER")
                        )
                        putExtra(
                            "ORIGINAL_START_DATE",
                            getIntent().getStringExtra("ORIGINAL_START_DATE")
                        )
                        putExtra(
                            "ORIGINAL_END_DATE",
                            getIntent().getStringExtra("ORIGINAL_END_DATE")
                        )
                        putExtra(
                            "ORIGINAL_START_PLACE",
                            getIntent().getStringExtra("ORIGINAL_START_PLACE")
                        )
                        putExtra(
                            "ORIGINAL_END_PLACE",
                            getIntent().getStringExtra("ORIGINAL_END_PLACE")
                        )
                    }
                }

                // Log for debugging
                Log.d("RouteArray", "Complete Route: ${completeRoute.joinToString(" -> ")}")

                startActivity(intent)
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
        tvTitle.text = when (fieldType) {
            FieldType.START_PLACE -> getString(R.string.sarvat_no_place_nakho)
            FieldType.END_PLACE -> getString(R.string.end_no_place_nakho)
            FieldType.MIDDLE_STOP -> getString(R.string.end_no_place_nakho)
            else -> ""
        }

        // Pre-fill existing value
        etPlace.setText(
            when (fieldType) {
                FieldType.START_PLACE -> startPlace
                FieldType.END_PLACE -> endPlace
                FieldType.MIDDLE_STOP -> if (editingStopIndex >= 0) middleStops[editingStopIndex] else ""
                else -> ""
            }
        )

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
                when (fieldType) {
                    FieldType.START_PLACE -> {
                        startPlace = inputText
                        binding.tvStartPlaceValue.text = startPlace
                        binding.cardStartPlace.setCardBackgroundColor(getColor(R.color.white))
                        Toast.makeText(
                            this,
                            getString(R.string.shuru_ki_jagah_save_ho_gayi),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    FieldType.END_PLACE -> {
                        endPlace = inputText
                        binding.tvEndPlaceValue.text = endPlace
                        binding.cardEndPlace.setCardBackgroundColor(getColor(R.color.white))
                        Toast.makeText(
                            this,
                            getString(R.string.akhri_jagah_save_ho_gayi),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    FieldType.MIDDLE_STOP -> {
                        if (editingStopIndex >= 0) {
                            // Editing existing stop
                            middleStops[editingStopIndex] = inputText
                            editingStopIndex = -1
                            Toast.makeText(
                                this,
                                getString(R.string.stop_update_ho_gaya),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // ✅ NEW LOGIC: Adding new stop - it becomes new dest, old dest becomes middle
                            if (endPlace.isNotEmpty()) {
                                // Move current end place to middle stops
                                middleStops.add(endPlace)
                            }
                            // Set new input as end place
                            endPlace = inputText
                            binding.tvEndPlaceValue.text = endPlace
                            binding.cardEndPlace.setCardBackgroundColor(getColor(R.color.white))

                            Toast.makeText(
                                this,
                                getString(R.string.raste_ka_stop_add_ho_gaya),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        refreshMiddleStopsUI()
                    }

                    else -> {}
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, getString(R.string.jagah_ka_naam_daalo), Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Cancel button
        btnCancel.setOnClickListener {
            editingStopIndex = -1
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun refreshMiddleStopsUI() {
        binding.llMiddleStops?.removeAllViews()

        middleStops.forEachIndexed { index, stop ->
            val stopCard = createMiddleStopCard(stop, index)
            binding.llMiddleStops?.addView(stopCard)
        }
    }

    private fun createMiddleStopCard(stopName: String, index: Int): MaterialCardView {
        val cardView = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16.dpToPx()
            }
            radius = 16f.dpToPx()
            cardElevation = 2f.dpToPx()
            setCardBackgroundColor(Color.WHITE)
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20.dpToPx(), 20.dpToPx(), 20.dpToPx(), 20.dpToPx())
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Icon circle
        val iconFrame = android.widget.FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(56.dpToPx(), 56.dpToPx())
            setBackgroundResource(R.drawable.bg_icon_circle_blue)
        }

        val iconView = ImageView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(32.dpToPx(), 32.dpToPx()).apply {
                gravity = android.view.Gravity.CENTER
            }
            setImageResource(R.drawable.compass)
            setColorFilter(ContextCompat.getColor(this@SecondSpeechScreen, R.color.white))
        }
        iconFrame.addView(iconView)

        // Text content
        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 16.dpToPx()
                }
        }

        val labelText = TextView(this).apply {
            text = context.getString(R.string.beech_ka_rasta, index + 1)
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
        }

        val valueText = TextView(this).apply {
            text = stopName
            textSize = 18f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        textLayout.addView(labelText)
        textLayout.addView(valueText)

        // Edit icon
        val editIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(24.dpToPx(), 24.dpToPx()).apply {
                marginEnd = 8.dpToPx()
            }
            setImageResource(R.drawable.baseline_edit_24)
            setColorFilter(ContextCompat.getColor(this@SecondSpeechScreen, R.color.color_primary))
            setOnClickListener {
                editingStopIndex = index
                showDesiPlaceDialog(FieldType.MIDDLE_STOP)
            }
        }

        // Delete icon
        val deleteIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(24.dpToPx(), 24.dpToPx())
            setImageResource(android.R.drawable.ic_menu_delete)
            setColorFilter(Color.RED)
            setOnClickListener {
                middleStops.removeAt(index)
                refreshMiddleStopsUI()
                Toast.makeText(
                    this@SecondSpeechScreen,
                    getString(R.string.stop_hata_diya),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        mainLayout.addView(iconFrame)
        mainLayout.addView(textLayout)
        mainLayout.addView(editIcon)
        mainLayout.addView(deleteIcon)
        cardView.addView(mainLayout)

        return cardView
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun Float.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }

    private fun startDialogVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                when (dialogFieldType) {
                    FieldType.START_PLACE -> getString(R.string.kaha_se_shuru_ho_rahe_ho_bolo)
                    FieldType.END_PLACE -> getString(R.string.kaha_jaana_hai_bolo)
                    FieldType.MIDDLE_STOP -> getString(R.string.kaha_jaana_hai_bolo)
                    else -> getString(R.string.jagah_ka_naam_bolo)
                }
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
            val validRoute = if (RouteUtils.isValidRoute(getCompleteRouteArray())) {
                Log.d("DraftSave", "✅ routeArray is valid")
                getCompleteRouteArray()
            } else {
                Log.w("DraftSave", "⚠️ Invalid route detected, creating simple route")
                getCompleteRouteArray()
            }

            val routeJsonString = RouteUtils.routeToJson(validRoute)
            GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val db =
                        com.dadabarbie.TruckTrip.room.AppDatabase.getDatabase(applicationContext)
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
                            existing.modelList2,
                            routeJson = routeJsonString,
                            updatedAt = System.currentTimeMillis()
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
            FieldType.START_DATE -> if (isEditMode) getString(R.string.tareekh_badal_sakte_ho) else getString(
                R.string.trip_shuru_hone_ki_tareekh_chuno
            )

            FieldType.START_PLACE -> if (isEditMode) getString(R.string.start_place_badal_sakte_ho) else getString(
                R.string.kaha_se_shuru_ho_rahe_ho_bolo
            )

            FieldType.END_PLACE -> if (isEditMode) getString(R.string.end_place_badal_sakte_ho) else getString(
                R.string.kaha_jaana_hai_bolo
            )

            FieldType.MIDDLE_STOP -> getString(R.string.beech_ka_place_he_to_bolo)
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
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
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
            else -> {}
        }
    }

    private fun resetCardColors() {
        binding.cardStartDate.strokeWidth = 0
        binding.cardStartPlace.strokeWidth = 0
        binding.cardEndPlace.strokeWidth = 0
    }

    // ✅ UPDATED: Enhanced validation with specific error messages
    private fun validateInputs(): Boolean {
        when {
            selectedDate.isEmpty() -> {
                Toast.makeText(this, getString(R.string.pehle_date_select_karo), Toast.LENGTH_SHORT)
                    .show()
                speakText(getString(R.string.pehle_date_select_karo))
                return false
            }

            startPlace.isEmpty() -> {
                Toast.makeText(this, getString(R.string.pehle_start_place_dalo), Toast.LENGTH_SHORT)
                    .show()
                speakText(getString(R.string.pehle_start_place_dalo))
                return false
            }

            endPlace.isEmpty() -> {
                Toast.makeText(this, getString(R.string.pehle_end_place_dalo), Toast.LENGTH_SHORT)
                    .show()
                speakText(getString(R.string.pehle_end_place_dalo))
                return false
            }

            else -> return true
        }
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