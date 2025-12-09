package com.dadabarbie.TruckTrip.diologFragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.creditList
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.databinding.BottomSheetAddIncomeBinding
import com.dadabarbie.TruckTrip.model.CreditModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddIncomeBottomSheetFragment(
    private var incomeDesc: String = "",
    private var incomeAmount: String = "",
    private var totalIncome: String = "",
    private var advanceTaken: String = "",
    private var balance: String = "",
    private var note: String = "",
    private var position: Int = -1
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetAddIncomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetAddIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupBalanceCalculation()
        setupSpeechToText()
        populateFields()
        setupSaveButton()
        
        // Set dialog width
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(width, ActionBar.LayoutParams.WRAP_CONTENT)
    }

    private fun setupBalanceCalculation() {
        // Add text watchers to auto-calculate balance
        binding.etTotalIncome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateBalance()
            }
        })

        binding.etAdvanceTaken.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateBalance()
            }
        })
    }

    private fun calculateBalance() {
        val totalIncomeValue = binding.etTotalIncome.text.toString().trim()
        val advanceTakenValue = binding.etAdvanceTaken.text.toString().trim()

        val totalIncome = if (totalIncomeValue.isEmpty()) 0.0 else totalIncomeValue.toDoubleOrNull() ?: 0.0
        val advance = if (advanceTakenValue.isEmpty()) 0.0 else advanceTakenValue.toDoubleOrNull() ?: 0.0

        val balanceValue = totalIncome - advance
        // Format balance to 2 decimal places if needed
        val formattedBalance = if (balanceValue >= 0) {
            if (balanceValue % 1 == 0.0) {
                balanceValue.toInt().toString()
            } else {
                String.format("%.2f", balanceValue)
            }
        } else {
            "0"
        }
        binding.etBalance.setText(formattedBalance)
    }

    private fun populateFields() {
        if (position != -1) {
            // Editing existing income
            // Use totalIncome if available, otherwise use amount
            val totalIncomeValue = if (totalIncome.isNotEmpty()) totalIncome else incomeAmount
            binding.etTotalIncome.setText(totalIncomeValue)
            binding.etAdvanceTaken.setText(advanceTaken)
            // Use note if available, otherwise use desc
            binding.etNote.setText(if (note.isNotEmpty()) note else incomeDesc)
            // Calculate balance after setting values
            calculateBalance()
        } else {
            // For new income, calculate balance if fields are pre-filled
            calculateBalance()
        }
    }

    private fun calculateBalanceFromFields(): String {
        val totalIncomeValue = binding.etTotalIncome.text.toString().trim()
        val advanceTakenValue = binding.etAdvanceTaken.text.toString().trim()

        val totalIncome = if (totalIncomeValue.isEmpty()) 0.0 else totalIncomeValue.toDoubleOrNull() ?: 0.0
        val advance = if (advanceTakenValue.isEmpty()) 0.0 else advanceTakenValue.toDoubleOrNull() ?: 0.0

        val balanceValue = totalIncome - advance
        // Format balance to 2 decimal places if needed
        return if (balanceValue >= 0) {
            if (balanceValue % 1 == 0.0) {
                balanceValue.toInt().toString()
            } else {
                String.format("%.2f", balanceValue)
            }
        } else {
            "0"
        }
    }

    private fun setupSpeechToText() {
        binding.micNote.setOnClickListener {
            startSpeechToText()
        }
    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Prefs[Constants.languageCode, ""].toString() + "-IN"
        )
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your language")

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, REQUEST_CODE_NOTE)
        } else {
            Toast.makeText(
                requireContext(),
                "Your Device Don't Support Speech Input",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null && requestCode == REQUEST_CODE_NOTE) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (result != null && result.isNotEmpty()) {
                binding.etNote.setText(result[0])
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveIncome()
        }
    }

    companion object {
        private const val REQUEST_CODE_NOTE = 200
    }

    private fun saveIncome() {
        val totalIncomeValue = binding.etTotalIncome.text.toString().trim()
        val advanceTakenValue = binding.etAdvanceTaken.text.toString().trim()
        val balanceValue = binding.etBalance.text.toString().trim()
        val noteValue = binding.etNote.text.toString().trim()

        // Validate total income
        if (totalIncomeValue.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter total income", Toast.LENGTH_SHORT).show()
            binding.etTotalIncome.requestFocus()
            return
        }

        // Always recalculate balance from current field values to ensure accuracy
        val calculatedBalance = calculateBalanceFromFields()

        // Use totalIncome as amount for backward compatibility
        val amount = totalIncomeValue

        // If editing
        if (position != -1) {
            val income = creditList[position]
            // Use note as desc if note is provided, otherwise keep existing desc
            income.desc = if (noteValue.isNotEmpty()) noteValue else incomeDesc
            income.amount = amount
            income.totalIncome = totalIncomeValue
            income.advanceTaken = advanceTakenValue
            income.balance = calculatedBalance
            income.note = noteValue
            
            (requireActivity() as MainActivity).creditDataUpdate()
            dialog?.dismiss()
        } else {
            // Adding new income
            if (noteValue.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter note (party name etc.)", Toast.LENGTH_SHORT).show()
                binding.etNote.requestFocus()
                return
            }

            Constants.emitEvent(
                Event(
                    CreditModel(
                        desc = noteValue, // Use note as desc for new income
                        amount = amount,
                        totalIncome = totalIncomeValue,
                        advanceTaken = advanceTakenValue,
                        balance = calculatedBalance,
                        note = noteValue
                    )
                )
            )
            dialog?.dismiss()
        }
    }
}

