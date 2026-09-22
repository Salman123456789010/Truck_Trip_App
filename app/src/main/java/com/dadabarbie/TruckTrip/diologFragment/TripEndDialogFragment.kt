package com.dadabarbie.TruckTrip.diologFragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.DialogFragment
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.databinding.DialogTripEndBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TripEndDialogFragment(
    private val startDate: String,
    private val existingEndDate: String = "",
    private val existingDriverIncome: String = "",
    private val existingEndOdometer: String = "",     // NEW: existing end odometer
    private val existingEndKm: String = "",           // NEW: existing manual KM
    private var onDataCollected: (endDate: String, driverIncome: String, totalDays: String, endOdometer: String, endKm: String, isOdometerMode: Boolean) -> Unit
) : DialogFragment(), TextToSpeech.OnInitListener {

    private var _binding: DialogTripEndBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentIsOdometerMode = true  // NEW: Track mode

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTripEndBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // init TTS
        tts = TextToSpeech(requireContext(), this)

        setupKmInputTypeToggle()  // NEW: Setup toggle
        setupDatePicker()
        setupButtons()
        setupSpeakers()
        populateExistingData()

        // Set dialog width & transparent background
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(width, ActionBar.LayoutParams.WRAP_CONTENT)

        // Make endDate non-focusable so keyboard won't appear — datepicker only
        binding.etEndDate.isFocusable = false
        binding.etEndDate.isClickable = true
    }

    // NEW: Setup radio button toggle for Odometer vs Manual KM
    private fun setupKmInputTypeToggle() {
        // Update UI based on current mode
        updateModeUI()
        Log.d("TripEndDialogFragment", "setupKmInputTypeToggle: ${existingEndOdometer}")

        binding.rgEndKmInputType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbEndOdometer -> {
                    if (!currentIsOdometerMode) {
                        currentIsOdometerMode = true
                        updateModeUI()
                        binding.etEndKm.setText("")  // Clear when switching
                    }
                }
                R.id.rbEndKm -> {
                    if (currentIsOdometerMode) {
                        currentIsOdometerMode = false
                        updateModeUI()
                        binding.etEndKm.setText("")  // Clear when switching
                    }
                }
            }
        }
    }

    // NEW: Update UI based on mode
    private fun updateModeUI() {
        if (currentIsOdometerMode) {
            binding.rbEndOdometer.isChecked = true
            binding.rbEndKm.isChecked = false

            // Update hint for odometer mode
            binding.etEndKm.hint = getString(R.string.end_odometer_reading)
        } else {
            binding.rbEndKm.isChecked = true
            binding.rbEndOdometer.isChecked = false
            // Update hint for manual KM mode
            binding.etEndKm.hint = getString(R.string.manual_km_traveled)
        }
    }

    private fun populateExistingData() {
        // Populate end date
        if (existingEndDate.isNotEmpty()) {
            binding.etEndDate.setText(existingEndDate)
        } else {
            binding.etEndDate.setText("")
            binding.etEndDate.hint = getString(R.string.end_trip_date)
        }

        // Populate driver income
        if (existingDriverIncome.isNotEmpty()) {
            binding.etDriverIncome.setText(existingDriverIncome)
        }

        // NEW: Populate end odometer or KM based on what was saved
        if (existingEndOdometer.isNotEmpty()) {
            currentIsOdometerMode = true
            binding.etEndKm.setText(existingEndOdometer)
            updateModeUI()
        } else if (existingEndKm.isNotEmpty()) {
            currentIsOdometerMode = false
            binding.etEndKm.setText(existingEndKm)
            updateModeUI()
        }
    }

    // TTS init callback
    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }
        } else {
            ttsReady = false
        }
    }

    private fun setupSpeakers() {
        // speak end date hint or selected date
        binding.speaker.setOnClickListener {
            val textToSpeak = when {
                binding.etEndDate.text.toString().trim().isNotEmpty() ->
                    binding.etEndDate.text.toString().trim()
                binding.etEndDate.hint != null ->
                    binding.etEndDate.hint.toString()
                else -> getString(R.string.end_trip_date)
            }
            speak(textToSpeak)
        }

        // speak driver income hint or entered value
        binding.driverIncomeSpeaker.setOnClickListener {
            val textToSpeak = when {
                binding.etDriverIncome.text.toString().trim().isNotEmpty() ->
                    binding.etDriverIncome.text.toString().trim()
                binding.etDriverIncome.hint != null ->
                    binding.etDriverIncome.hint.toString()
                else -> getString(R.string.driver_income)
            }
            speak(textToSpeak)
        }

        // NEW: speak KM/Odometer hint or value
        binding.kmSpeaker.setOnClickListener {
            val textToSpeak = when {
                binding.etEndKm.text.toString().trim().isNotEmpty() ->
                    binding.etEndKm.text.toString().trim()
                binding.etEndKm.hint != null ->
                    binding.etEndKm.hint.toString()
                else -> if (currentIsOdometerMode) {
                    getString(R.string.end_odometer_reading)
                } else {
                    getString(R.string.manual_km_traveled)
                }
            }
            speak(textToSpeak)
        }
    }

    private fun speak(text: String) {
        if (!ttsReady) {
            Toast.makeText(requireContext(), "TTS not ready", Toast.LENGTH_SHORT).show()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, System.currentTimeMillis().toString())
    }

    private fun setupDatePicker() {
        binding.etEndDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun parseDateSafely(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        val formats = listOf(
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "yyyy/MM/dd",
            "dd MMM yyyy",
            "d MMM yyyy"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }
                val parsed = sdf.parse(dateStr.trim())
                if (parsed != null) return parsed
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                val selectedDate = dateFormat.format(calendar.time)
                binding.etEndDate.setText(selectedDate)
            },
            year,
            month,
            day
        )

        // Allow same date: set minDate to start date at midnight
        parseDateSafely(startDate)?.let { sDate ->
            val startCal = Calendar.getInstance().apply {
                time = sDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            datePickerDialog.datePicker.minDate = startCal.timeInMillis
        }

        datePickerDialog.show()
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dialog?.dismiss()
        }

        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun calculateTotalDays(startDateStr: String, endDateStr: String): String {
        return try {
            val start = parseDateSafely(startDateStr)
            val end = parseDateSafely(endDateStr)

            if (start != null && end != null) {
                val startCal = Calendar.getInstance().apply {
                    time = start
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    time = end
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffInMillis = endCal.timeInMillis - startCal.timeInMillis
                val days = TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1
                maxOf(1L, days).toString()
            } else {
                "1"
            }
        } catch (e: Exception) {
            "1"
        }
    }

    private fun validateAndSubmit() {
        val endDate = binding.etEndDate.text.toString().trim()
        val driverIncome = binding.etDriverIncome.text.toString().trim()
        val endKmValue = binding.etEndKm.text.toString().trim()  // NEW

        // Validate end date
        if (endDate.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.please_select_trip_end_date), Toast.LENGTH_SHORT).show()
            binding.etEndDate.requestFocus()
            return
        }

        // Validate driver income
        if (driverIncome.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.please_enter_driver_income), Toast.LENGTH_SHORT).show()
            binding.etDriverIncome.requestFocus()
            return
        }

        // Validate driver income is a valid number
        if (driverIncome.toDoubleOrNull() == null) {
            Toast.makeText(requireContext(),
                getString(R.string.please_enter_valid_driver_income_amount), Toast.LENGTH_SHORT).show()
            binding.etDriverIncome.requestFocus()
            return
        }

        // NEW: Validate end KM/Odometer if entered
        if (endKmValue.isNotEmpty()) {
            val kmVal = endKmValue.toDoubleOrNull()
            if (kmVal == null || kmVal <= 0) {
                Toast.makeText(
                    requireContext(),
                    if (currentIsOdometerMode) getString(R.string.please_enter_valid_odometer_reading) else "Please enter valid KM",
                    Toast.LENGTH_SHORT
                ).show()
                binding.etEndKm.requestFocus()
                return
            }
        }

        // Validate end date is not strictly before start date (same date IS allowed)
        val startParsed = parseDateSafely(startDate)
        val endParsed = parseDateSafely(endDate)
        if (startParsed != null && endParsed != null) {
            val startCal = Calendar.getInstance().apply {
                time = startParsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                time = endParsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (endCal.before(startCal)) {
                Toast.makeText(requireContext(),
                    getString(R.string.end_date_cannot_be_before_start_date), Toast.LENGTH_SHORT).show()
                binding.etEndDate.requestFocus()
                return
            }
        }

        // Calculate total days (same day = 1 day)
        val totalDays = calculateTotalDays(startDate, endDate)

        // NEW: Separate endOdometer and endKm based on mode
        val endOdometer = if (currentIsOdometerMode) endKmValue else ""
        val endKm = if (!currentIsOdometerMode) endKmValue else ""

        // Callback with collected data
        onDataCollected(endDate, driverIncome, totalDays, endOdometer, endKm, currentIsOdometerMode)
        dialog?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
        _binding = null
    }
}