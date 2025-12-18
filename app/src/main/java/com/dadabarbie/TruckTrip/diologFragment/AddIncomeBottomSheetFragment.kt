package com.dadabarbie.TruckTrip.diologFragment

import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
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
import com.dadabarbie.TruckTrip.Utils.Constants.creditList
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.databinding.BottomSheetAddIncomeBinding
import com.dadabarbie.TruckTrip.model.CreditModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddIncomeBottomSheetFragment(
    private var incomeDesc: String? = "",
    private var incomeAmount: String? = "",
    private var totalIncome: String? = "",
    private var advanceTaken: String? = "",
    private var balance: String? = "",
    private var note: String? = "",
    private var place: String? = "",
    private var date: String? = "",
    private var position: Int = -1
) : BottomSheetDialogFragment()  {

    private lateinit var binding: BottomSheetAddIncomeBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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

        // Make BottomSheet adjust for keyboard
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setupBalanceCalculation()
        setupSpeechToText()
        setupDatePicker()
        populateFields()
        setupSaveButton()

        // Set dialog width
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(width, ActionBar.LayoutParams.WRAP_CONTENT)
    }

    private fun setupDatePicker() {
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
            binding.etTotalIncome.setText(incomeAmount ?: "")
            binding.etAdvanceTaken.setText(advanceTaken ?: "")
            binding.etBalance.setText(balance ?: "")
            binding.etNote.setText(if (!note.isNullOrEmpty()) note else incomeDesc ?: "")
            binding.etPlace.setText(place ?: "")

            // Set date if available, otherwise use today
            val dateToUse = date?.takeIf { it.isNotEmpty() } ?: dateFormat.format(calendar.time)
            binding.etDate.setText(dateToUse)

            // Parse the date to set calendar for date picker
            try {
                val parsedDate = dateFormat.parse(dateToUse)
                if (parsedDate != null) {
                    calendar.time = parsedDate
                }
            } catch (e: Exception) {
                // If parsing fails, keep current date
            }
        } else {
            // For new income, set today's date by default
            val today = dateFormat.format(calendar.time)
            binding.etDate.setText(today)
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
            startSpeechToText(REQUEST_CODE_NOTE)
        }

        binding.micPlace.setOnClickListener {
            startSpeechToText(REQUEST_CODE_PLACE)
        }
    }

    private fun startSpeechToText(requestCode: Int) {
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
            startActivityForResult(intent, requestCode)
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
        if (resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (result != null && result.isNotEmpty()) {
                when (requestCode) {
                    REQUEST_CODE_NOTE -> {
                        val spokenText = result[0]

                        // Check if spoken text contains mathematical expression or number
                        val calculatedAmount = extractAndCalculateMath(spokenText)
                        if (calculatedAmount != null) {
                            // Get current amount or 0
                            val currentAmount = binding.etTotalIncome.text.toString().toIntOrNull() ?: 0
                            // Add the calculated amount to current amount
                            val newAmount = currentAmount + calculatedAmount
                            binding.etTotalIncome.setText(newAmount.toString())

                            // Remove the amount from spoken text and set only note
                            val noteWithoutAmount = removeAmountFromText(spokenText)
                            binding.etNote.setText(noteWithoutAmount)

                            Toast.makeText(
                                requireContext(),
                                "Added ₹$calculatedAmount to total income",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // No amount found, just set the note as is
                            binding.etNote.setText(spokenText)
                        }
                    }
                    REQUEST_CODE_PLACE -> binding.etPlace.setText(result[0])
                }
            }
        }
    }

    /**
     * Extracts and calculates mathematical expressions from spoken text
     * Supports basic operations: addition, subtraction, multiplication, division
     */
    private fun extractAndCalculateMath(text: String): Int? {
        try {
            // Convert spoken words to mathematical symbols
            var mathExpression = text.lowercase()
                .replace("plus", "+")
                .replace("add", "+")
                .replace("minus", "-")
                .replace("subtract", "-")
                .replace("times", "*")
                .replace("multiply", "*")
                .replace("multiplied by", "*")
                .replace("into", "*")
                .replace("divide", "/")
                .replace("divided by", "/")
                .replace("by", "/")

            // Extract numbers and operators
            val pattern = Regex("(\\d+)\\s*([+\\-*/])\\s*(\\d+)")
            val matchResult = pattern.find(mathExpression)

            if (matchResult != null) {
                val num1 = matchResult.groupValues[1].toInt()
                val operator = matchResult.groupValues[2]
                val num2 = matchResult.groupValues[3].toInt()

                val result = when (operator) {
                    "+" -> num1 + num2
                    "-" -> num1 - num2
                    "*" -> num1 * num2
                    "/" -> if (num2 != 0) num1 / num2 else null
                    else -> null
                }

                return result
            }

            // If no mathematical expression found, try to extract single number
            val singleNumberPattern = Regex("\\b(\\d+)\\b")
            val singleMatch = singleNumberPattern.find(mathExpression)
            if (singleMatch != null) {
                return singleMatch.value.toInt()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * Removes numbers and mathematical operators from text to keep only the description/note
     */
    private fun removeAmountFromText(text: String): String {
        var cleanedText = text
            // Remove mathematical words
            .replace(Regex("\\b(plus|add|minus|subtract|times|multiply|multiplied by|into|divide|divided by|by)\\b", RegexOption.IGNORE_CASE), "")
            // Remove numbers
            .replace(Regex("\\d+"), "")
            // Remove mathematical operators
            .replace(Regex("[+\\-*/]"), "")
            // Clean up extra spaces
            .replace(Regex("\\s+"), " ")
            .trim()

        return cleanedText
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveIncome()
        }
    }

    companion object {
        private const val REQUEST_CODE_NOTE = 200
        private const val REQUEST_CODE_PLACE = 201
    }

    private fun saveIncome() {
        val totalIncomeValue = binding.etTotalIncome.text.toString().trim()
        val advanceTakenValue = binding.etAdvanceTaken.text.toString().trim()
        val balanceValue = binding.etBalance.text.toString().trim()
        val noteValue = binding.etNote.text.toString().trim()
        val placeValue = binding.etPlace.text.toString().trim()
        val dateValue = binding.etDate.text.toString().trim()

        // Validate total income
        if (totalIncomeValue.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.please_enter_amount), Toast.LENGTH_SHORT).show()
            binding.etTotalIncome.requestFocus()
            return
        }

        // If editing
        if (position != -1) {
            val income = creditList[position]
            // Use note as desc if note is provided, otherwise keep existing desc
            income.desc = if (noteValue.isNotEmpty()) noteValue else (incomeDesc ?: "")
            income.amount = totalIncomeValue  // Update amount with totalIncome
            income.note = noteValue
            income.place = placeValue
            income.date = dateValue

            (requireActivity() as MainActivity).creditDataUpdate()
            dialog?.dismiss()
        } else {
            // Adding new income
            if (noteValue.isEmpty()) {
                Toast.makeText(requireContext(),
                    getString(R.string.please_enter_note), Toast.LENGTH_SHORT).show()
                binding.etNote.requestFocus()
                return
            }

            Constants.emitEvent(
                Event(
                    CreditModel(
                        desc = noteValue,
                        amount = totalIncomeValue,  // Use totalIncome as amount
                        totalIncome = totalIncomeValue,
                        advanceTaken = advanceTakenValue,
                        balance = balanceValue,
                        note = noteValue,
                        place = placeValue,
                        date = dateValue
                    )
                )
            )
            dialog?.dismiss()
        }
    }

}