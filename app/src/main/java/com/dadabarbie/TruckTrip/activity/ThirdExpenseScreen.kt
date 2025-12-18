package com.dadabarbie.TruckTrip.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.adapter.ExpenseAdapter
import com.dadabarbie.TruckTrip.databinding.ActivityThirdExpenseScreenBinding
import com.dadabarbie.TruckTrip.databinding.BottomSheetExpenseBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class ThirdExpenseScreen : AppCompatActivity(),TextToSpeech.OnInitListener {
    data class ExpenseItem(
        var id: String = UUID.randomUUID().toString(),
        var note: String,
        var amount: Int,
        var type: ExpenseType
    )

    enum class ExpenseType {
        KHARCHA, AAVAK
    }
    private val binding: ActivityThirdExpenseScreenBinding by lazy {
        ActivityThirdExpenseScreenBinding.inflate(layoutInflater)
    }
    private lateinit var textToSpeech: TextToSpeech
    private var ttsInitialized = false
    private var isEditBottomSheetOpen = false
    private val expenseList = mutableListOf<ExpenseItem>()
    private val incomeList = mutableListOf<ExpenseItem>()

    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var incomeAdapter: ExpenseAdapter

    private var currentBottomSheetType: ExpenseType? = null
    private var currentBottomSheetDialog: BottomSheetDialog? = null
    private val tempBottomSheetList = mutableListOf<ExpenseItem>()

    private val voiceRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                parseAndAddExpense(spokenText)
            }
        }
    }
    private var endDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        textToSpeech = TextToSpeech(this, this)
        setupViews()
        updateMunafa()
    }

    private fun setupViews() {
        // Back button
        binding.ivBack.setOnClickListener {
            finish()
        }

        // Complete button - Top right icon
        binding.ivComplete.setOnClickListener {
            checkAndCompleteTrip()
        }

        // Expense adapters
        expenseAdapter = ExpenseAdapter(expenseList) { item, action ->
            android.util.Log.d("ExpenseAdapter", "Expense action: $action for ${item.note}")
            handleExpenseAction(item, action, ExpenseType.KHARCHA)
        }
        incomeAdapter = ExpenseAdapter(incomeList) { item, action ->
            android.util.Log.d("IncomeAdapter", "Income action: $action for ${item.note}")
            handleExpenseAction(item, action, ExpenseType.AAVAK)
        }

        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = expenseAdapter

        binding.rvIncome.layoutManager = LinearLayoutManager(this)
        binding.rvIncome.adapter = incomeAdapter

        // Kharcha Card Click
        binding.cardKharcha.setOnClickListener {
            speakText("Yaha Kharcha Dalo")
            showExpenseBottomSheet(ExpenseType.KHARCHA)
        }

        // Aavak Card Click
        binding.cardAavak.setOnClickListener {
            speakText("Yaha Aavak Dalo")
            showExpenseBottomSheet(ExpenseType.AAVAK)
        }

        // Speaker icons
        binding.ivSpeakerKharcha.setOnClickListener {
            speakText("Yaha Kharcha Dalo")
        }

        binding.ivSpeakerAavak.setOnClickListener {
            speakText("Yaha Aavak Dalo")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Hindi language not supported", Toast.LENGTH_SHORT).show()
            } else {
                ttsInitialized = true
                binding.root.postDelayed({
                    speakText("Kharcha ya Aavak dalo")
                }, 500)
            }
        }
    }

    private fun speakText(text: String) {
        if (ttsInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private var currentBottomSheetAdapter: ExpenseAdapter? = null

    private fun showExpenseBottomSheet(type: ExpenseType) {
        currentBottomSheetType = type
        tempBottomSheetList.clear()

        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetBinding = BottomSheetExpenseBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        currentBottomSheetDialog = bottomSheetDialog

        // Set title
        bottomSheetBinding.tvTitle.text = if (type == ExpenseType.KHARCHA) {
            "Kharcha Dalo"
        } else {
            "Aavak Dalo"
        }

        // Setup RecyclerView
        currentBottomSheetAdapter = ExpenseAdapter(tempBottomSheetList) { item, action ->
            handleBottomSheetAction(item, action)
        }
        bottomSheetBinding.rvBottomSheetExpenses.layoutManager = LinearLayoutManager(this)
        bottomSheetBinding.rvBottomSheetExpenses.adapter = currentBottomSheetAdapter

        // Mic button
        bottomSheetBinding.fabMic.setOnClickListener {
            startVoiceRecognitionForBottomSheet()
        }

        // Save button
        bottomSheetBinding.btnSave.setOnClickListener {
            saveBottomSheetExpenses()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun startVoiceRecognitionForBottomSheet() {
        val instruction = if (currentBottomSheetType == ExpenseType.KHARCHA) {
            "Kharcha bolo, jaise Khane ke 500"
        } else {
            "Aavak bolo, jaise Kiraye ke 5000"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, instruction)
        }

        try {
            voiceRecognitionLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseAndAddExpense(spokenText: String) {
        // Parse patterns like "Khane ke 500", "Diesel 1200", "500 rupaye khana"
        val patterns = listOf(
            Regex("(.+?)\\s*(\\d+)\\s*(?:rupaye|rupees)?", RegexOption.IGNORE_CASE),
            Regex("(\\d+)\\s*(?:rupaye|rupees)?\\s*(.+)", RegexOption.IGNORE_CASE)
        )

        var note = ""
        var amount = 0

        for (pattern in patterns) {
            val match = pattern.find(spokenText)
            if (match != null) {
                val groups = match.groupValues
                if (groups.size >= 3) {
                    // Try to find which group is the number
                    val firstGroup = groups[1].trim()
                    val secondGroup = groups[2].trim()

                    if (firstGroup.toIntOrNull() != null) {
                        amount = firstGroup.toInt()
                        note = secondGroup
                    } else {
                        note = firstGroup.replace(Regex("ke|ka|ki"), "").trim()
                        amount = secondGroup.toIntOrNull() ?: 0
                    }
                    break
                }
            }
        }

        if (amount > 0 && note.isNotEmpty()) {
            val item = ExpenseItem(
                note = note,
                amount = amount,
                type = currentBottomSheetType ?: ExpenseType.KHARCHA
            )
            tempBottomSheetList.add(item)
            currentBottomSheetAdapter?.notifyItemInserted(tempBottomSheetList.size - 1)

            speakText("$note ka $amount rupaye jod diya")
        } else {
            Toast.makeText(this, "Samajh nahi aaya, dubara bolo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseAndUpdateItem(spokenText: String, item: ExpenseItem) {
        val patterns = listOf(
            Regex("(.+?)\\s*(\\d+)\\s*(?:rupaye|rupees)?", RegexOption.IGNORE_CASE),
            Regex("(\\d+)\\s*(?:rupaye|rupees)?\\s*(.+)", RegexOption.IGNORE_CASE)
        )

        var note = ""
        var amount = 0

        for (pattern in patterns) {
            val match = pattern.find(spokenText)
            if (match != null) {
                val groups = match.groupValues
                if (groups.size >= 3) {
                    val firstGroup = groups[1].trim()
                    val secondGroup = groups[2].trim()

                    if (firstGroup.toIntOrNull() != null) {
                        amount = firstGroup.toInt()
                        note = secondGroup
                    } else {
                        note = firstGroup.replace(Regex("ke|ka|ki"), "").trim()
                        amount = secondGroup.toIntOrNull() ?: 0
                    }
                    break
                }
            }
        }

        if (amount > 0 && note.isNotEmpty()) {
            item.note = note
            item.amount = amount
            currentBottomSheetAdapter?.notifyDataSetChanged()
            speakText("$note ka $amount rupaye update ho gaya")
            Toast.makeText(this, "Update ho gaya!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Samajh nahi aaya, dubara bolo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleBottomSheetAction(item: ExpenseItem, action: String) {
        when (action) {
            "EDIT" -> showEditDialogForBottomSheet(item)
            "DELETE" -> {
                tempBottomSheetList.remove(item)
                currentBottomSheetAdapter?.notifyDataSetChanged()
                speakText("Delete ho gaya")
                Toast.makeText(this, "Delete ho gaya", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialogForBottomSheet(item: ExpenseItem) {
        isEditBottomSheetOpen = true
        speakText("Edit karne ke liye bolo")
        Log.d("EditBottomSheet", "Opening edit bottom sheet")

        // Create custom bottom sheet for voice edit
        val editBottomSheet = BottomSheetDialog(this)
        val editView = layoutInflater.inflate(R.layout.bottom_sheet_edit_expense, null)
        editBottomSheet.setContentView(editView)

        // Prevent dismiss on outside touch while editing
        editBottomSheet.setCanceledOnTouchOutside(false)

        val tvCurrentNote = editView.findViewById<TextView>(R.id.tvCurrentNote)
        val tvCurrentAmount = editView.findViewById<TextView>(R.id.tvCurrentAmount)
        val fabEditMic = editView.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabEditMic)
        val btnCancel = editView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)

        tvCurrentNote.text = "Current: ${item.note}"
        tvCurrentAmount.text = "Current: ₹${item.amount}"

        fabEditMic.setOnClickListener {
            Log.d("EditBottomSheet", "Mic button clicked")
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Naya bolo, jaise 'Diesel 3000'")
            }

            try {
                // Store current item for editing
                currentEditItem = item

                editItemLauncher.launch(intent)
                editBottomSheet.dismiss()
                isEditBottomSheetOpen = false
            } catch (e: Exception) {
                Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            Log.d("EditBottomSheet", "Cancel clicked")
            editBottomSheet.dismiss()
            isEditBottomSheetOpen = false
        }

        editBottomSheet.setOnDismissListener {
            isEditBottomSheetOpen = false
        }

        editBottomSheet.show()
        Log.d("EditBottomSheet", "Bottom sheet shown")
    }

    private var currentEditItem: ExpenseItem? = null

    // Add new voice recognition launcher for edit
    private val editItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty() && currentEditItem != null) {
                val spokenText = results[0]
                parseAndUpdateItem(spokenText, currentEditItem!!)
            }
        }
    }

    private fun saveBottomSheetExpenses() {
        if (tempBottomSheetList.isEmpty()) {
            Toast.makeText(this, "Kuch to dalo pehle", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentBottomSheetType == ExpenseType.KHARCHA) {
            expenseList.addAll(tempBottomSheetList)
            expenseAdapter.notifyDataSetChanged()
            binding.layoutExpenseEmpty.visibility = if (expenseList.isEmpty()) View.VISIBLE else View.GONE
        } else {
            incomeList.addAll(tempBottomSheetList)
            incomeAdapter.notifyDataSetChanged()
            binding.layoutIncomeEmpty.visibility = if (incomeList.isEmpty()) View.VISIBLE else View.GONE
        }

        updateMunafa()
        Toast.makeText(this, "Sab save ho gaya!", Toast.LENGTH_SHORT).show()
    }

    private fun handleExpenseAction(item: ExpenseItem, action: String, type: ExpenseType) {
        when (action) {
            "DELETE" -> {
                if (type == ExpenseType.KHARCHA) {
                    expenseList.remove(item)
                    expenseAdapter.notifyDataSetChanged()
                    binding.layoutExpenseEmpty.visibility = if (expenseList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    incomeList.remove(item)
                    incomeAdapter.notifyDataSetChanged()
                    binding.layoutIncomeEmpty.visibility = if (incomeList.isEmpty()) View.VISIBLE else View.GONE
                }
                updateMunafa()
                Toast.makeText(this, "Delete ho gaya", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateMunafa() {
        val totalExpense = expenseList.sumOf { it.amount }
        val totalIncome = incomeList.sumOf { it.amount }
        val profit = totalIncome - totalExpense

        binding.tvTotalExpense.text = "₹$totalExpense"
        binding.tvTotalIncome.text = "₹$totalIncome"
        binding.tvProfit.text = "₹$profit"

        // Color coding for profit
        if (profit >= 0) {
            binding.tvProfit.setTextColor(getColor(android.R.color.holo_green_dark))
            binding.tvProfitLabel.text = "Munafa (Profit)"
        } else {
            binding.tvProfit.setTextColor(getColor(android.R.color.holo_red_dark))
            binding.tvProfitLabel.text = "Nuksan (Loss)"
        }
    }

    private fun checkAndCompleteTrip() {
        // Check if there's any data
        val hasExpenses = expenseList.isNotEmpty()
        val hasIncome = incomeList.isNotEmpty()

        if (!hasExpenses && !hasIncome) {
            speakText("Abhi kuch entry nahi hai, pehle kharcha ya aavak dalo")
            Toast.makeText(this, "Pehle kharcha ya aavak dalo", Toast.LENGTH_SHORT).show()
            return
        }

        // All good, ask for end date
        speakText("Sab done hai, ab trip khatam hone ki tareekh chuniye")
        showEndDatePicker()
    }

    private fun showEndDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN"))
                endDate = dateFormat.format(calendar.time)

                speakText("Trip khatam, ab data save kar rahe hain")

                // Call API to generate report
                generateTripReport()
            },
            year, month, day
        )

        datePickerDialog.setTitle("Trip Khatam Hone Ki Tareekh")
        datePickerDialog.datePicker.calendarViewShown = true
        datePickerDialog.show()
    }

    private fun generateTripReport() {
        // Prepare data for API
        val tripData = hashMapOf(
            "truck_number" to getTruckNumber(),
            "start_date" to getStartDate(),
            "end_date" to endDate,
            "start_place" to getStartPlace(),
            "end_place" to getEndPlace(),
            "expenses" to expenseList.map {
                hashMapOf("note" to it.note, "amount" to it.amount)
            },
            "income" to incomeList.map {
                hashMapOf("note" to it.note, "amount" to it.amount)
            },
            "total_expense" to expenseList.sumOf { it.amount },
            "total_income" to incomeList.sumOf { it.amount },
            "profit" to (incomeList.sumOf { it.amount } - expenseList.sumOf { it.amount })
        )

        // Show loading
        Toast.makeText(this, "Data save ho raha hai...", Toast.LENGTH_SHORT).show()

        // TODO: Call your API here
        // Example:
        // RetrofitClient.api.createTripReport(tripData).enqueue(object : Callback<Response> {
        //     override fun onResponse(call: Call<Response>, response: retrofit2.Response<Response>) {
        //         if (response.isSuccessful) {
        //             speakText("Trip ka report tayar ho gaya")
        //             Toast.makeText(this@ThirdExpenseScreen, "Report tayar ho gaya!", Toast.LENGTH_LONG).show()
        //             // Navigate to report screen or finish
        //             finish()
        //         }
        //     }
        //     override fun onFailure(call: Call<Response>, t: Throwable) {
        //         Toast.makeText(this@ThirdExpenseScreen, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
        //     }
        // })

        // For now, just show success message
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            speakText("Trip ka report tayar ho gaya")

            AlertDialog.Builder(this)
                .setTitle("Trip Complete!")
                .setMessage("Trip Details:\n\n" +
                        "Truck: ${getTruckNumber()}\n" +
                        "Start: ${getStartDate()} - ${getStartPlace()}\n" +
                        "End: $endDate - ${getEndPlace()}\n\n" +
                        "Total Kharcha: ₹${expenseList.sumOf { it.amount }}\n" +
                        "Total Aavak: ₹${incomeList.sumOf { it.amount }}\n" +
                        "Munafa: ₹${incomeList.sumOf { it.amount } - expenseList.sumOf { it.amount }}")
                .setPositiveButton("OK") { _, _ ->
                    finish()
                }
                .show()
        }, 1500)
    }

    // Helper functions to get data from previous screens
    // You should pass these via Intent extras
    private fun getTruckNumber(): String {
        return intent.getStringExtra("truck_number") ?: "GJ-11-1234"
    }

    private fun getStartDate(): String {
        return intent.getStringExtra("start_date") ?: "01 Jan 2025"
    }

    private fun getStartPlace(): String {
        return intent.getStringExtra("start_place") ?: "Mumbai"
    }

    private fun getEndPlace(): String {
        return intent.getStringExtra("end_place") ?: "Delhi"
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}