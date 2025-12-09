package com.dadabarbie.TruckTrip.diologFragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.DialogFragment
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.databinding.DialogTripEndBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TripEndDialogFragment(
    private var onDataCollected: (endDate: String, driverIncome: String) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogTripEndBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogTripEndBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDatePicker()
        setupButtons()
        
        // Set dialog width
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(width, ActionBar.LayoutParams.WRAP_CONTENT)
        
        // Set default date to today
        val today = dateFormat.format(calendar.time)
        binding.etEndDate.setText(today)
    }

    private fun setupDatePicker() {
        binding.etEndDate.setOnClickListener {
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
                binding.etEndDate.setText(selectedDate)
            },
            year,
            month,
            day
        )
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

    private fun validateAndSubmit() {
        val endDate = binding.etEndDate.text.toString().trim()
        val driverIncome = binding.etDriverIncome.text.toString().trim()

        // Validate end date
        if (endDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please select trip end date", Toast.LENGTH_SHORT).show()
            binding.etEndDate.requestFocus()
            return
        }

        // Validate driver income
        if (driverIncome.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter driver income", Toast.LENGTH_SHORT).show()
            binding.etDriverIncome.requestFocus()
            return
        }

        // Validate driver income is a valid number
        if (driverIncome.toDoubleOrNull() == null) {
            Toast.makeText(requireContext(), "Please enter valid driver income amount", Toast.LENGTH_SHORT).show()
            binding.etDriverIncome.requestFocus()
            return
        }

        // Callback with collected data
        onDataCollected(endDate, driverIncome)
        dialog?.dismiss()
    }
}

