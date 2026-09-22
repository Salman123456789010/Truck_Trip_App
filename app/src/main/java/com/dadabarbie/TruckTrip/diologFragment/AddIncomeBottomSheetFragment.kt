package com.dadabarbie.TruckTrip.diologFragment

import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
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
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
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
) : BottomSheetDialogFragment(), TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private lateinit var binding: BottomSheetAddIncomeBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val incomeCategories = ArrayList<String>()
    private var selectedCategory = "Loading Bhada"

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

        // Set soft input mode to resize and expand bottom sheet
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        // Expand bottom sheet to show full content
        dialog?.setOnShowListener { dialogInterface ->
//            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = dialog?.findViewById<FrameLayout>(R.id.design_bottom_sheet)
//            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
//                com.google.android.material.R.id.design_bottom_sheet
//            )

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.peekHeight = 0

                // Set proper height for bottom sheet
                val layoutParams = it.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                it.layoutParams = layoutParams
            }
        }
        textToSpeech = TextToSpeech(requireActivity(), this)
        incomeCategories.add(getString(R.string.loading_bhada))
        incomeCategories.add(getString(R.string.unloading_bhada))
        incomeCategories.add(getString(R.string.advance))
        incomeCategories.add(getString(R.string.balance_slip))
        incomeCategories.add(getString(R.string.waiting_charges_rukne_ka_charge))
        incomeCategories.add(getString(R.string.extra_income))

        setupCategorySpinner()
        setupBalanceCalculation()
        setupFieldNavigation()
        setupSpeechToText()
        setupDatePicker()
        populateFields()
        setupSaveButton()

        // Set dialog width
        val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(width, ActionBar.LayoutParams.MATCH_PARENT)
    }

    private fun setupFieldNavigation() {
        // Setup IME actions for smooth field navigation
        binding.etTotalIncome.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.etNote.requestFocus()
                true
            } else false
        }

        binding.etNote.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.etPlace.requestFocus()
                true
            } else false
        }

        binding.etPlace.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.etDate.performClick()
                true
            } else false
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            incomeCategories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerIncomeCategory.adapter = adapter

        binding.spinnerIncomeCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = incomeCategories[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategory = "Loading Bhada"
            }
        }
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
        binding.micTitleInstruction.setOnClickListener {
            speakFeedbackMessage()
        }
    }

    private fun speakFeedbackMessage() {
        if (!isTtsInitialized) {
            return
        }
        textToSpeech?.stop()
        val message = getString(R.string.add_income_info)
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "feedback_message")
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
            binding.etTotalIncome.setText(incomeAmount ?: "")
            binding.etAdvanceTaken.setText(advanceTaken ?: "")
            binding.etBalance.setText(balance ?: "")
            binding.etNote.setText(if (!note.isNullOrEmpty()) note else incomeDesc ?: "")
            binding.etPlace.setText(place ?: "")

            val dateToUse = date?.takeIf { it.isNotEmpty() } ?: dateFormat.format(calendar.time)
            binding.etDate.setText(dateToUse)

            try {
                val parsedDate = dateFormat.parse(dateToUse)
                if (parsedDate != null) {
                    calendar.time = parsedDate
                }
            } catch (e: Exception) {
                Log.e("AddIncome", "Error parsing date: ${e.message}")
            }

            incomeDesc?.let { desc ->
                val categoryIndex = incomeCategories.indexOfFirst { it.equals(desc, ignoreCase = true) }
                if (categoryIndex != -1) {
                    binding.spinnerIncomeCategory.setSelection(categoryIndex)
                }
            }
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

                        val calculatedAmount = extractAndCalculateMath(spokenText)
                        if (calculatedAmount != null) {
                            val currentAmount = binding.etTotalIncome.text.toString().toIntOrNull() ?: 0
                            val newAmount = currentAmount + calculatedAmount
                            binding.etTotalIncome.setText(newAmount.toString())

                            val noteWithoutAmount = removeAmountFromText(spokenText)
                            binding.etNote.setText(noteWithoutAmount)

                            Toast.makeText(
                                requireContext(),
                                "Added ₹$calculatedAmount to total income",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            binding.etNote.setText(spokenText)
                        }
                    }
                    REQUEST_CODE_PLACE -> binding.etPlace.setText(result[0])
                }
            }
        }
    }

    private fun extractAndCalculateMath(text: String): Int? {
        try {
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

    private fun removeAmountFromText(text: String): String {
        var cleanedText = text
            .replace(Regex("\\b(plus|add|minus|subtract|times|multiply|multiplied by|into|divide|divided by|by)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d+"), "")
            .replace(Regex("[+\\-*/]"), "")
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
        val noteValue = binding.etNote.text.toString().trim()
        val placeValue = binding.etPlace.text.toString().trim()
        val dateValue = binding.etDate.text.toString().trim()

        if (totalIncomeValue.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.please_enter_amount), Toast.LENGTH_SHORT).show()
            binding.etTotalIncome.requestFocus()
            return
        }

        try {
            if (position != -1) {
                Log.d("AddIncome", "Editing income at position: $position")

                if (position < 0 || position >= creditList.size) {
                    Toast.makeText(requireContext(), "Invalid position", Toast.LENGTH_SHORT).show()
                    return
                }

                val finalNote = if (noteValue.isNotEmpty()) noteValue else selectedCategory

                val updatedIncome = Income(
                    desc = selectedCategory,
                    amount = totalIncomeValue,
                    note = finalNote,
                    place = placeValue,
                    date = dateValue
                )

                (requireActivity() as MainActivity)
                    .onIncomeEdited(position, updatedIncome)

                Log.d("AddIncome", "Income edited successfully")
                dialog?.dismiss()

            } else {
                Log.d("AddIncome", "Adding new income: $selectedCategory, amount: $totalIncomeValue")

                val finalNote = if (noteValue.isNotEmpty()) noteValue else selectedCategory

                val creditModel = CreditModel(
                    desc = selectedCategory,
                    amount = totalIncomeValue,
                    note = finalNote,
                    place = placeValue,
                    date = dateValue
                )

                Constants.emitEvent(Event(creditModel))

                Log.d("AddIncome", "Income event posted")
                dialog?.dismiss()
            }

        } catch (e: Exception) {
            Log.e("AddIncome", "Error saving income: ${e.message}", e)
            Toast.makeText(requireContext(), "Error saving income: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("hi", "IN"))

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                textToSpeech?.setLanguage(Locale.US)
            }

            isTtsInitialized = true
            textToSpeech?.setSpeechRate(0.85f)
            textToSpeech?.setPitch(1.0f)
        }
    }

    override fun onDestroy() {
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        super.onDestroy()
    }
}