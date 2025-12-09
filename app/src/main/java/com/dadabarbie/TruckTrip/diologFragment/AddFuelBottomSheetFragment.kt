package com.dadabarbie.TruckTrip.diologFragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private var position: Int = -1
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetAddFuelBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        
        setupDatePicker()
        populateFields()
        setupSaveButton()
        
        // Set dialog width
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(width, ActionBar.LayoutParams.WRAP_CONTENT)
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
            Toast.makeText(requireContext(), "Please enter diesel liters", Toast.LENGTH_SHORT).show()
            binding.etLiters.requestFocus()
            return
        }

        if (amount.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show()
            binding.etAmount.requestFocus()
            return
        }

        // Create description for fuel entry
        var desc = "Fuel - ${liters}L"
        if (km.isNotEmpty()) {
            desc += " - ${km}KM"
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
                        km = km
                    )
                )
            )
            dialog?.dismiss()
        }
    }
}

