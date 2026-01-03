package com.dadabarbie.TruckTrip.diologFragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.debitList
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.databinding.BottomSheetAddFuelBinding
import com.dadabarbie.TruckTrip.model.DebitModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddFuelBottomSheetFragment(
    private var fuelDesc: String = "",
    private var fuelAmount: String = "",
    private var fuelLiters: String = "",
    private var fuelKm: String = "",

    private var fuelPlace: String = "",
    private var fuelDate: String = "",
    private var isOdometerMode: Boolean = true,  // This is passed from caller
    private var position: Int = -1
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetAddFuelBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var lastOdometerReading = 0.0
    private var currentIsOdometerMode = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetAddFuelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)


        // Get last odometer reading from database/list
        getLastOdometerReading()

        // ✅ FIX: Initialize mode BEFORE setting up toggle
        initializeMode()

        setupKmInputTypeToggle()
        setupDatePicker()
        populateFields()
        setupCalculateAverage()
        setupSaveButton()

        // Set dialog width
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(width, ActionBar.LayoutParams.WRAP_CONTENT)
    }

    private fun getLastOdometerReading() {
        // Get the last odometer reading from your fuel entries
        // Filter only entries where isOdometerMode = true and get the latest
        try {
            val lastFuelEntry = debitList
                .filter { it.type == "Fuel" && it.isOdometerMode && it.km.isNotEmpty() }
                .maxByOrNull {
                    try {
                        dateFormat.parse(it.date)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }

            lastOdometerReading = lastFuelEntry?.km?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            lastOdometerReading = 0.0
        }
    }

    // ✅ NEW METHOD: Initialize mode based on whether editing or adding new
    private fun initializeMode() {
        currentIsOdometerMode = if (position != -1) {
            // Editing: Use the saved mode from the existing entry
            debitList[position].isOdometerMode
        } else {
            // Adding new: Use the passed parameter (default true)
            isOdometerMode
        }
    }

    private fun setupKmInputTypeToggle() {
        // ✅ FIX: Set radio buttons based on initialized mode
        updateModeUI()

        // Set up radio button listeners
        binding.rgKmInputType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbOdometer -> {
                    if (!currentIsOdometerMode) {  // Only update if actually changed
                        currentIsOdometerMode = true
                        updateModeUI()
                        if (position == -1) { // Only clear when adding new entry
                            binding.etKm.setText("")
                        }
                        // Hide average when mode changes
                        binding.llAverageDisplay.visibility = View.GONE
                        // Recalculate with new mode
                        calculateAndDisplayAverage()
                    }
                }
                R.id.rbDistance -> {
                    if (currentIsOdometerMode) {  // Only update if actually changed
                        currentIsOdometerMode = false
                        updateModeUI()
                        if (position == -1) { // Only clear when adding new entry
                            binding.etKm.setText("")
                        }
                        // Hide average when mode changes
                        binding.llAverageDisplay.visibility = View.GONE
                        // Recalculate with new mode
                        calculateAndDisplayAverage()
                    }
                }
            }
        }
    }

    // ✅ NEW METHOD: Update UI based on current mode
    private fun updateModeUI() {
        if (currentIsOdometerMode) {
            binding.rbOdometer.isChecked = true
            binding.rbDistance.isChecked = false
            binding.tilKm.hint = getString(R.string.km_reading_odometer)
        } else {
            binding.rbDistance.isChecked = true
            binding.rbOdometer.isChecked = false
            binding.tilKm.hint = getString(R.string.distance_traveled_km)
        }
    }

    private fun setupDatePicker() {
        // Set default date to today if adding new fuel (not editing)
        if (position == -1 && fuelDate.isEmpty()) {
            val today = dateFormat.format(calendar.time)
            binding.etDate.setText(today)
        }

        binding.etDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val selectedDate = dateFormat.format(calendar.time)
                binding.etDate.setText(selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun populateFields() {
        if (position != -1) {
            // Editing existing fuel entry
            binding.etLiters.setText(fuelLiters)
            binding.etAmount.setText(fuelAmount)
            binding.etKm.setText(fuelKm)
            binding.etPlace.setText(fuelPlace)

            // Set date if available, otherwise use today
            if (fuelDate.isNotEmpty()) {
                binding.etDate.setText(fuelDate)
                // Parse the date to set calendar for date picker
                try {
                    val parsedDate = dateFormat.parse(fuelDate)
                    if (parsedDate != null) {
                        calendar.time = parsedDate
                    }
                } catch (e: Exception) {
                    // If parsing fails, keep current date
                }
            } else {
                val today = dateFormat.format(calendar.time)
                binding.etDate.setText(today)
            }

            // ✅ FIX: Trigger average calculation for editing mode
            calculateAndDisplayAverage()
        }
    }

    private fun setupCalculateAverage() {
        // Calculate average when user enters KM and Liters
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateAndDisplayAverage()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        binding.etKm.addTextChangedListener(textWatcher)
        binding.etLiters.addTextChangedListener(textWatcher)
    }

    private fun calculateAndDisplayAverage() {
        val kmText = binding.etKm.text.toString().trim()
        val litersText = binding.etLiters.text.toString().trim()

        if (kmText.isNotEmpty() && litersText.isNotEmpty()) {
            val liters = litersText.toDoubleOrNull() ?: return
            val kmValue = kmText.toDoubleOrNull() ?: return

            if (liters <= 0) {
                binding.llAverageDisplay.visibility = View.GONE
                return
            }

            // Calculate distance based on mode
            val distance = if (currentIsOdometerMode) {
                // Distance = Current Odometer - Last Odometer
                if (lastOdometerReading > 0 && kmValue > lastOdometerReading) {
                    kmValue - lastOdometerReading
                } else {
                    // First entry or invalid reading, can't calculate
                    binding.llAverageDisplay.visibility = View.GONE
                    return
                }
            } else {
                // Distance is directly entered
                kmValue
            }

            if (distance > 0) {
                val average = distance / liters
                binding.tvAverageValue.text = String.format("%.2f km/L", average)
                binding.llAverageDisplay.visibility = View.VISIBLE
            } else {
                binding.llAverageDisplay.visibility = View.GONE
            }
        } else {
            binding.llAverageDisplay.visibility = View.GONE
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveFuel()
        }
    }

    private fun saveFuel() {
        val liters = binding.etLiters.text.toString().trim()
        val amount = binding.etAmount.text.toString().trim()
        val km = binding.etKm.text.toString().trim()
        val place = binding.etPlace.text.toString().trim()
        val date = binding.etDate.text.toString().trim()

        // Validate required fields
        if (liters.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.please_enter_diesel_liters), Toast.LENGTH_SHORT).show()
            binding.etLiters.requestFocus()
            return
        }

        if (amount.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.please_enter_amount), Toast.LENGTH_SHORT).show()
            binding.etAmount.requestFocus()
            return
        }

        // Validate KM if entered
        if (km.isNotEmpty()) {
            val kmValue = km.toDoubleOrNull()
            if (kmValue == null || kmValue <= 0) {
                Toast.makeText(requireContext(),
                    getString(R.string.please_enter_valid_km), Toast.LENGTH_SHORT).show()
                binding.etKm.requestFocus()
                return
            }

            // Additional validation for odometer mode
            if (currentIsOdometerMode && lastOdometerReading > 0 && kmValue <= lastOdometerReading) {
                Toast.makeText(
                    requireContext(),
                    "Odometer reading must be greater than last reading (${lastOdometerReading.toInt()} KM)",
                    Toast.LENGTH_LONG
                ).show()
                binding.etKm.requestFocus()
                return
            }
        }

        // Calculate distance and average for description
        var desc = "Fuel - ${liters}L"
        var calculatedAverage = ""

        if (km.isNotEmpty()) {
            val kmValue = km.toDoubleOrNull() ?: 0.0
            val litersValue = liters.toDoubleOrNull() ?: 0.0

            val distance = if (currentIsOdometerMode && lastOdometerReading > 0) {
                kmValue - lastOdometerReading
            } else if (!currentIsOdometerMode) {
                kmValue
            } else {
                0.0
            }

            if (distance > 0 && litersValue > 0) {
                val avg = distance / litersValue
                calculatedAverage = String.format("%.2f", avg)
                desc += " - ${calculatedAverage} km/L"
            }

            if (currentIsOdometerMode) {
                desc += " (${kmValue.toInt()} KM)"
            } else {
                desc += " (${kmValue.toInt()} KM traveled)"
            }
        }

        // If editing
        if (position != -1) {
            val expense = debitList[position]
            expense.desc = desc
            expense.amount = amount
            expense.liters = liters
            expense.km = km
            expense.place = place
            expense.date = date
            expense.type = "Fuel"
            expense.note = "Fuel"
            expense.isOdometerMode = currentIsOdometerMode // Store the mode

            (requireActivity() as MainActivity).debitDataUpdate()

            dialog?.dismiss()
        } else {
            // Adding new fuel entry
            Constants.emitDebitEvent(
                Event(
                    DebitModel(
                        desc = desc,
                        amount = amount,
                        note = "Fuel",
                        place = place,
                        date = date,
                        type = "Fuel",
                        liters = liters,
                        km = km,
                        isOdometerMode = currentIsOdometerMode // Store the mode
                    )
                )
            )
            dialog?.dismiss()
        }
    }
}

// ============================================
// CALLER CODE FIX (MainActivity or wherever you call this dialog)
// ============================================

/*
When opening the dialog for EDITING, make sure to pass the correct isOdometerMode:

// ❌ WRONG - Always passes true
val dialog = AddFuelBottomSheetFragment(
    fuelDesc = expense.desc,
    fuelAmount = expense.amount,
    fuelLiters = expense.liters,
    fuelKm = expense.km,
    fuelPlace = expense.place,
    fuelDate = expense.date,
    isOdometerMode = true,  // ❌ Always true!
    position = position
)

// ✅ CORRECT - Pass the actual saved mode
val dialog = AddFuelBottomSheetFragment(
    fuelDesc = expense.desc,
    fuelAmount = expense.amount,
    fuelLiters = expense.liters,
    fuelKm = expense.km,
    fuelPlace = expense.place,
    fuelDate = expense.date,
    isOdometerMode = expense.isOdometerMode,  // ✅ Use saved mode
    position = position
)

// When adding NEW entry, you can omit it (defaults to true) or explicitly set:
val dialog = AddFuelBottomSheetFragment(
    isOdometerMode = true  // Or false if you want distance mode as default
)
*/