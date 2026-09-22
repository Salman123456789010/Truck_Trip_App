package com.dadabarbie.TruckTrip.diologFragment

import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.debitList
import com.dadabarbie.TruckTrip.Utils.Constants.getExpenseKeyFromText
import com.dadabarbie.TruckTrip.Utils.Constants.getExpenseText
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.databinding.BottomSheetAddExpenseBinding
import com.dadabarbie.TruckTrip.model.DebitModel
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseBottomSheetFragment(
    private var expenseDesc: String = "",
    private var expenseAmount: String = "",
    private var expenseNote: String = "",
    private var expensePlace: String = "",
    private var expenseDate: String = "",
    private var expenseType: String = "",
    private var position: Int = -1
) : BottomSheetDialogFragment(), TextToSpeech.OnInitListener {

    private lateinit var binding: BottomSheetAddExpenseBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    // Comprehensive Expense type options covering all truck-related expenses


    private val expenseTypes = listOf(

        ExpenseTypeUi("Food" ,"🍽️ Food/Meals"),
        ExpenseTypeUi("Toll/Highway Charges", "🛣️ Toll/Highway Charges"),
        ExpenseTypeUi("Puncture/Tire Repair", "⚙️ Puncture/Tire Repair"),
        ExpenseTypeUi("Driver Expanse(Kharcha)", "👨‍✈️Driver Payment (Trip Wise)"),
        ExpenseTypeUi("Truck Maintenance", "🔧 Truck Maintenance"),

        ExpenseTypeUi("Loading Charges", "📦 Loading Charges (Bharne ka)"),
        ExpenseTypeUi("Unloading Charges", "📤 Unloading Charges (Khali karne ka)"),
        ExpenseTypeUi("Parking Charges", "🅿️ Parking Charges"),

        ExpenseTypeUi("Police/RTO Fine", "👮 Police/RTO Fine"),
        ExpenseTypeUi("Documents/Papers", "📄 Documents/Papers"),
        ExpenseTypeUi("Broker/Commission", "💰 Broker/Commission"),

        ExpenseTypeUi("Engine Oil/Lubricants", "🛢️ Engine Oil/Lubricants"),
        ExpenseTypeUi("Truck Cleaning/Washing", "🧼 Truck Cleaning/Washing (Service)"),
        ExpenseTypeUi("Phone/Communication", "📱 Phone/Communication (Recharge)"),
        ExpenseTypeUi("Tea", "☕ Tea/Snacks/Chai"),

        ExpenseTypeUi("Hotel/Accommodation", "🏨 Hotel/Accommodation"),
        ExpenseTypeUi("Permit/Entry Tax", "🎫 Permit/Entry Tax (Border Entry)"),
        ExpenseTypeUi("Weighbridge Charges", "⚖️ Weighbridge Charges"),

        ExpenseTypeUi("Spare Parts", "🔩 Spare Parts"),
        ExpenseTypeUi("Driver Bath/Facilities", "🚿 Driver Bath/Facilities"),
        ExpenseTypeUi("Bulb/Electrical Items", "💡 Bulb/Electrical Items"),
        ExpenseTypeUi("Mechanic Charges", "🪛 Mechanic Charges"),

        ExpenseTypeUi("Road Repair Contribution", "🚧 Road Repair Contribution"),
        ExpenseTypeUi("Tools/Equipment", "🧰 Tools/Equipment"),
        ExpenseTypeUi("Medicine/First Aid", "💊 Medicine/First Aid"),

        ExpenseTypeUi("Challan/Fine", "📋 Challan/Fine"),
        ExpenseTypeUi("Truck Insurance", "🚛 Truck Insurance"),
        ExpenseTypeUi("Registration/Fitness", "📝 Registration/Fitness"),
        ExpenseTypeUi("Market Fee/Mandi Charges", "🏪 Market Fee/Mandi Charges"),

        ExpenseTypeUi("OTHER", "🎯 Other Expenses")
    )

    private val expenseTypesnewTypes = listOf(

        ExpenseTypeUi("🍽️ Food / Meals", "Food"),
        ExpenseTypeUi("🛣️ Toll / Highway Charges", "Toll/Highway Charges"),
        ExpenseTypeUi("⚙️ Puncture / Tire Repair", "Puncture/Tire Repair"),
        ExpenseTypeUi("👨‍✈️ Driver Payment (Trip Wise)", "Driver Expanse(Kharcha)"),
        ExpenseTypeUi("🔧 Truck Maintenance", "Truck Maintenance"),

        ExpenseTypeUi("📦 Loading Charges (Bharne ka)", "Loading Charges"),
        ExpenseTypeUi("📤 Unloading Charges (Khali karne ka)", "Unloading Charges"),
        ExpenseTypeUi("🅿️ Parking Charges", "Parking Charges"),

        ExpenseTypeUi("👮 Police / RTO Fine", "Police/RTO Fine"),
        ExpenseTypeUi("📄 Documents / Papers", "Documents/Papers"),
        ExpenseTypeUi("💰 Broker / Commission", "Broker/Commission"),

        ExpenseTypeUi("🛢️ Engine Oil / Lubricants", "Engine Oil/Lubricants"),
        ExpenseTypeUi("🧼 Truck Cleaning / Washing (Service)", "Truck Cleaning/Washing"),
        ExpenseTypeUi("📱 Phone / Communication (Recharge)", "Phone/Communication"),
        ExpenseTypeUi("☕ Tea / Snacks / Chai", "Tea"),

        ExpenseTypeUi("🏨 Hotel / Accommodation", "Hotel/Accommodation"),
        ExpenseTypeUi("🎫 Permit / Entry Tax (Border Entry)", "Permit/Entry Tax"),
        ExpenseTypeUi("⚖️ Weighbridge Charges", "Weighbridge Charges"),

        ExpenseTypeUi("🔩 Spare Parts", "Spare Parts"),
        ExpenseTypeUi("🚿 Driver Bath / Facilities", "Driver Bath/Facilities"),
        ExpenseTypeUi("💡 Bulb / Electrical Items", "Bulb/Electrical Items"),
        ExpenseTypeUi("🪛 Mechanic Charges", "Mechanic Charges"),

        ExpenseTypeUi("🚧 Road Repair Contribution", "Road Repair Contribution"),
        ExpenseTypeUi("🧰 Tools / Equipment", "Tools/Equipment"),
        ExpenseTypeUi("💊 Medicine / First Aid", "Medicine/First Aid"),

        ExpenseTypeUi("📋 Challan / Fine", "Challan/Fine"),
        ExpenseTypeUi("🚛 Truck Insurance", "Truck Insurance"),
        ExpenseTypeUi("📝 Registration / Fitness", "Registration/Fitness"),
        ExpenseTypeUi("🏪 Market Fee / Mandi Charges", "Market Fee/Mandi Charges"),

        ExpenseTypeUi("🎯 Other Expenses", "OTHER")
    )



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetAddExpenseBinding.inflate(inflater, container, false)
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

        setupExpenseTypeDropdown()
        setupFieldNavigation()
        setupDatePicker()
        setupSpeechToText()
        populateFields()
        setupButtons()

        // Set dialog width
        val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(width, ActionBar.LayoutParams.MATCH_PARENT)
    }

    private fun setupFieldNavigation() {
        // Setup IME actions for smooth field navigation
        binding.etAmount.setOnEditorActionListener { _, actionId, _ ->
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

    private fun speakFeedbackMessage() {
        if (!isTtsInitialized) {
            return
        }

        // Stop any ongoing speech
        textToSpeech?.stop()

        // The message to speak
        val message = getString(R.string.add_expanse_info)

        // Speak the message
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "feedback_message")
    }

    private fun setupExpenseTypeDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            expenseTypes.map { it.label }   // 👈 ONLY UI TEXT
        )
        binding.actvExpenseType.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        // Set default date to today if adding new expense (not editing)
        if (position == -1 && expenseDate.isEmpty()) {
//            val today = dateFormat.format(calendar.time)
//            binding.etDate.setText(today)
        }

        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        binding.dateLayout.setOnClickListener {
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
            // Editing existing expense
            binding.etAmount.setText(expenseAmount)
            // Note field remains separate - populate with actual note
            binding.etNote.setText(expenseNote ?: "")
            binding.etPlace.setText(expensePlace ?: "")

            // Set date if available, otherwise use today
            val dateToUse = expenseDate?.takeIf { it.isNotEmpty() } ?: dateFormat.format(calendar.time)
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

            // Set expense type from dropdown
            Log.d("DataName", "populateFields:1 ${expenseType} ${expenseType}")
            val selectedLabel = context?.getExpenseKeyFromText(expenseType) ?: ""
            Log.d("DataName", "populateFields:2 ${selectedLabel} ${selectedLabel}")

            val expenseKey = expenseTypesnewTypes
                .firstOrNull { it.label == selectedLabel }
                ?.key ?: "OTHER"
            val type = expenseKey
            if (expenseTypesnewTypes.isNotEmpty()) {
                binding.actvExpenseType.setText(type ?: "", false)
            }

            // Update title for editing mode
            binding.title.text = getString(R.string.edit_expense)
        }
    }

    private fun setupSpeechToText() {
        binding.micAmount.setOnClickListener {
            startSpeechToText(REQUEST_CODE_AMOUNT)
        }
        binding.micNote.setOnClickListener {
            startSpeechToText(REQUEST_CODE_NOTE)
        }
        binding.btnClose.setOnClickListener {
            speakFeedbackMessage()
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
                    REQUEST_CODE_AMOUNT -> {
                        // Extract number from speech
                        val extractedAmount = processExtractedText(result[0])
                        binding.etAmount.setText(extractedAmount.toString())
                    }
                    REQUEST_CODE_NOTE -> {
                        val spokenText = result[0]

                        // Check if spoken text contains mathematical expression or number
                        val calculatedAmount = extractAndCalculateMath(spokenText)
                        if (calculatedAmount != null) {
                            // Get current amount or 0
                            val currentAmount = binding.etAmount.text.toString().toIntOrNull() ?: 0
                            // Add the calculated amount to current amount
                            val newAmount = currentAmount + calculatedAmount
                            binding.etAmount.setText(newAmount.toString())

                            // Remove the amount from spoken text and set only note
                            val noteWithoutAmount = removeAmountFromText(spokenText)
                            binding.etNote.setText(noteWithoutAmount)

                            Toast.makeText(
                                requireContext(),
                                "Added ₹$calculatedAmount to amount",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // No amount found, just set the note as is
                            binding.etNote.setText(spokenText)
                        }
                    }
                    REQUEST_CODE_PLACE -> {
                        binding.etPlace.setText(result[0])
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveExpense()
        }

        binding.btnCancel.setOnClickListener {
            dialog?.dismiss()
        }
    }

    private fun processExtractedText(text: String): Int {
        val regex = Regex("\\b\\d+\\b")
        val numbers = regex.findAll(text).map { it.value.toInt() }.toList()
        return if (numbers.isNotEmpty()) numbers[0] else 0
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

    companion object {
        private const val REQUEST_CODE_AMOUNT = 99
        private const val REQUEST_CODE_NOTE = 100
        private const val REQUEST_CODE_PLACE = 101
    }

    private fun saveExpense() {
        val amount = binding.etAmount.text.toString().trim()
        val note = binding.etNote.text.toString().trim()
        val place = binding.etPlace.text.toString().trim()
        val date = binding.etDate.text.toString().trim()

        val selectedLabel = binding.actvExpenseType.text.toString()
        Log.d("NewData", "saveExpense:1 ${selectedLabel}")
        val expenseKey = expenseTypes
            .firstOrNull { it.label == selectedLabel }
            ?.key ?: "OTHER"
        val type = context?.getExpenseText(expenseKey) ?: ""
        Log.d("NewData", "saveExpense:2 ${type}")
        // Validate amount
        if (amount.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.enter_income_amount), Toast.LENGTH_SHORT).show()
            binding.etAmount.requestFocus()
            return
        }
        Log.d("keyDAta", "saveExpense: ${type}")

        // Validate expense type
        if (type.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.please_select_expense_type), Toast.LENGTH_SHORT).show()
            binding.actvExpenseType.requestFocus()
            return
        }

        // If editing
        if (position != -1) {
            val updatedExpense = Expense(
                id = debitList[position].id,
                desc = type,  // Use type from dropdown as description
                amount = amount,
                note = note,  // Keep note separate
                place = place,
                date = date,
                type = type,  // Type from dropdown
                liters = debitList[position].liters,
                km = debitList[position].km,
                isOdometerMode = debitList[position].isOdometerMode
            )

            (requireActivity() as MainActivity)
                .onExpenseEdited(position, updatedExpense)

            Toast.makeText(requireContext(),
                getString(R.string.expense_updated_successfully), Toast.LENGTH_SHORT).show()
            dialog?.dismiss()

        } else {
            // Adding new expense
            Constants.emitDebitEvent(
                Event(
                    DebitModel(
                        desc = type,  // Use type from dropdown as description
                        amount = amount,
                        note = note,  // Keep note separate
                        place = place,
                        date = date,
                        type = type   // Type from dropdown
                    )
                )
            )

            Toast.makeText(requireContext(),
                getString(R.string.expense_added_successfully), Toast.LENGTH_SHORT).show()
            dialog?.dismiss()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to Hindi
            val result = textToSpeech?.setLanguage(Locale("hi", "IN"))

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                // Fallback to English
                textToSpeech?.setLanguage(Locale.US)
            }

            isTtsInitialized = true

            // Set speech rate (0.5 to 2.0, 1.0 is normal)
            textToSpeech?.setSpeechRate(0.85f)

            // Set pitch (0.5 to 2.0, 1.0 is normal)
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