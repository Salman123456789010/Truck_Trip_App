package com.dadabarbie.TruckTrip.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.TripEditManager
import com.dadabarbie.TruckTrip.Utils.TripEditManager.isEditMode
import com.dadabarbie.TruckTrip.adapter.ExpenseAdapter
import com.dadabarbie.TruckTrip.databinding.ActivityThirdExpenseScreenBinding
import com.dadabarbie.TruckTrip.databinding.BottomSheetExpenseBinding
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.dadabarbie.TruckTrip.room.model.TripDataTestModel
import com.vasyerp.cafvd.room.model.Products
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Random
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.dadabarbie.TruckTrip.Utils.Event
import com.google.gson.reflect.TypeToken
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import com.dadabarbie.TruckTrip.model.addTrip.AddTripRequestModel
import dagger.hilt.android.AndroidEntryPoint
import android.content.res.ColorStateList
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import java.io.File


@AndroidEntryPoint
class ThirdExpenseScreen : AppCompatActivity(),TextToSpeech.OnInitListener {
    private val authViewModel: AuthViewModel by viewModels()
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

    private var pdfFilePath: String = ""
    private var selectedRating: Int = 0
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var incomeAdapter: ExpenseAdapter

    private var currentBottomSheetType: ExpenseType? = null
    private var currentBottomSheetDialog: BottomSheetDialog? = null
    private val tempBottomSheetList = mutableListOf<ExpenseItem>()
    private var tripId = ""
    private var originalTruckNumber: String? = null
    private var originalStartDate: String? = null
    private var originalStartPlace: String? = null
    private var originalEndPlace: String? = null
    private var isTripCompleted = false
    private val testList: ArrayList<TripDataTestModel> = arrayListOf()

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

        val idString = intent.getStringExtra("id")
        if (!idString.isNullOrEmpty()) {
             tripId = idString
        }
        if (tripId.isEmpty()) {
            generateTripId()
        }

        // Capture original values if in edit mode
        if (isEditMode) {
            originalTruckNumber = intent.getStringExtra("ORIGINAL_TRUCK_NUMBER")
            originalStartDate = intent.getStringExtra("ORIGINAL_START_DATE")
            originalStartPlace = intent.getStringExtra("ORIGINAL_START_PLACE")
            originalEndPlace = intent.getStringExtra("ORIGINAL_END_PLACE")
        }

        textToSpeech = TextToSpeech(this, this)
        setupViews()
        updateMunafa()
        setupEditMode()
        setObserver()
    }

// Replace setObserver() and related methods in ThirdExpenseScreen.kt


    private fun setObserver() {
        authViewModel.downloadCompleted.observe(this, Observer { data ->
            dismissProgress()

            if (data.isNullOrEmpty()) {
                Toast.makeText(this, "PDF generation failed", Toast.LENGTH_SHORT).show()
                navigateToDashboard("")
                return@Observer
            }

            try {
                // Store PDF path
                pdfFilePath = data

                openFile(pdfFilePath)

                // Navigate after PDF opens
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToDashboard(pdfFilePath)
                }, 1000)
                // Show feedback dialog
//                showFeedbackDialog(data)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                navigateToDashboard("")
            }
        })
    }

    private fun showFeedbackDialog(pdfPath: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)

        // Find views
        val star1 = dialogView.findViewById<ImageView>(R.id.star1)
        val star2 = dialogView.findViewById<ImageView>(R.id.star2)
        val star3 = dialogView.findViewById<ImageView>(R.id.star3)
        val star4 = dialogView.findViewById<ImageView>(R.id.star4)
        val star5 = dialogView.findViewById<ImageView>(R.id.star5)
        val etFeedback = dialogView.findViewById<EditText>(R.id.etFeedback)
        val btnSkip = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSkip)
        val btnSubmit = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmit)
        val btnViewPdf = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewPdf)

        val stars = listOf(star1, star2, star3, star4, star5)
        selectedRating = 0

        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Star rating logic
        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                selectedRating = index + 1
                updateStars(stars, selectedRating)
            }
        }

        // Skip button
        btnSkip.setOnClickListener {
            dialog.dismiss()
            openFile(pdfPath)

            // Navigate after PDF opens
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToDashboard(pdfPath)
            }, 2000)
        }

        // Submit button
        btnSubmit.setOnClickListener {
            val feedback = etFeedback.text?.toString()?.trim() ?: ""

            if (selectedRating == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Log feedback (or send to server)
            Log.d("Feedback", "Rating: $selectedRating, Comment: $feedback")
            Toast.makeText(this, "Thank you for your feedback! ⭐", Toast.LENGTH_SHORT).show()

            // TODO: Send feedback to server
            // sendFeedbackToServer(selectedRating, feedback)

            dialog.dismiss()
            openFile(pdfPath)

            // Navigate after PDF opens
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToDashboard(pdfPath)
            }, 2000)
        }

        // View PDF button
        btnViewPdf.setOnClickListener {
            openFile(pdfPath)
            Toast.makeText(this, "Opening PDF...", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun updateStars(stars: List<ImageView>, rating: Int) {
        stars.forEachIndexed { index, star ->
            if (index < rating) {
                // Filled star
                star.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                // Empty star
                star.setImageResource(android.R.drawable.btn_star_big_off)
            }
        }
    }

    private fun navigateToDashboard(data: String) {
        isTripCompleted = true
        TripEditManager.clearEditMode()
        Constants.creditList.clear()
        Constants.debitList.clear()
        Constants.refreshApiGet(Event(1))

//        val intent1 = Intent(this, NormalUserDashBoard::class.java)
//        intent1.putExtra("tripData", data)
//        intent1.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//        startActivity(intent1)
        finish()
    }

    private fun openFile(data: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val myMime = MimeTypeMap.getSingleton()
            val apkURI = this?.let {
                FileProvider.getUriForFile(
                    this, it.packageName, File(data)
                )
            }
            val mimeType = myMime.getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(
                    apkURI.toString()
                )
            )
            intent.setDataAndType(apkURI, mimeType)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
//            Toast(applicationContext, "Its not open Something issue in your file Please try again",Toast.LENGTH_SHORT).show()
        }

    }

    // Optional: Send feedback to server
    private fun sendFeedbackToServer(rating: Int, comment: String) {
        // TODO: Implement API call to send feedback
        val feedbackData = hashMapOf(
            "trip_id" to tripId,
            "truck_number" to getTruckNumber(),
            "rating" to rating,
            "comment" to comment,
            "timestamp" to System.currentTimeMillis()
        )

        Log.d("FeedbackData", feedbackData.toString())

        // Example API call:
        // authViewModel.submitFeedback(feedbackData)
    }


//    private fun navigateToDashboard(pdfPath: String) {
//        isTripCompleted = true
//        TripEditManager.clearEditMode()
//        Constants.creditList.clear()
//        Constants.debitList.clear()
//        Constants.refreshApiGet(Event(1))
//
//        val intent = Intent(this, NormalUserDashBoard::class.java)
//
//        // ✅ CRITICAL: Use SINGLE_TOP to reuse existing instance and trigger onNewIntent()
//        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//
//        // Pass PDF path
//        if (pdfPath.isNotEmpty()) {
//            intent.putExtra("tripData", pdfPath)
//            Log.d("ThirdExpenseScreen", "✅ Passing PDF path to Dashboard: $pdfPath")
//        }
//
//        startActivity(intent)
//        finish()
//    }
//
//
//
//    private fun openFile(data: String) {
//        try {
//            val intent = Intent(Intent.ACTION_VIEW)
//            val myMime = MimeTypeMap.getSingleton()
//            val apkURI = this?.let {
//                FileProvider.getUriForFile(
//                    this, it.packageName, File(data)
//                )
//            }
//            val mimeType = myMime.getMimeTypeFromExtension(
//                MimeTypeMap.getFileExtensionFromUrl(
//                    apkURI.toString()
//                )
//            )
//            intent.setDataAndType(apkURI, mimeType)
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            startActivity(intent)
//
//
//        } catch (e: Exception) {
//
////            Toast(applicationContext, "Its not open Something issue in your file Please try again",Toast.LENGTH_SHORT).show()
//        }
//
//    }

    // Optional: Add error observer for API failures


// Call this in onCreate() after setObserver()
// setupErrorObserver()

    private fun setupEditMode() {
        // Load existing expense and income from Constants
        if (Constants.debitList.isNotEmpty()) {
            expenseList.addAll(Constants.debitList.map { expense ->
                ExpenseItem(
                    note = expense.note ?: "",
                    amount = expense.amount?.toIntOrNull() ?: 0,
                    type = ExpenseType.KHARCHA
                )
            })
            expenseAdapter.notifyDataSetChanged()
            binding.layoutExpenseEmpty.visibility = View.GONE
        }

        if (Constants.creditList.isNotEmpty()) {
            incomeList.addAll(Constants.creditList.map { income ->
                ExpenseItem(
                    note = income.note ?: "",
                    amount = income.amount?.toIntOrNull() ?: 0,
                    type = ExpenseType.AAVAK
                )
            })
            incomeAdapter.notifyDataSetChanged()
            binding.layoutIncomeEmpty.visibility = View.GONE
        }

        updateMunafa()

        // Change header
//        binding.tvTitle.text = "Kharcha-Aavak Edit Karein"

        // Change complete button icon/text
//        binding.ivComplete.setImageResource(R.drawable.baseline_done_24)

        speakText(getString(R.string.kharcha_aavak_edit_karein_chahiye_to_add_ya_delete_karo))
    }
    private fun setupViews() {
        // Back button
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Complete button - Top right icon
        binding.done.setOnClickListener {
            checkAndCompleteTrip()
        }

        // Expense adapters
        expenseAdapter = ExpenseAdapter(expenseList) { item, action ->
//            android.util.Log.d("ExpenseAdapter", "Expense action: $action for ${item.note}")
            handleExpenseAction(item, action, ExpenseType.KHARCHA)
        }
        incomeAdapter = ExpenseAdapter(incomeList) { item, action ->
//            android.util.Log.d("IncomeAdapter", "Income action: $action for ${item.note}")
            handleExpenseAction(item, action, ExpenseType.AAVAK)
        }

        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = expenseAdapter

        binding.rvIncome.layoutManager = LinearLayoutManager(this)
        binding.rvIncome.adapter = incomeAdapter

        // Kharcha Card Click
        binding.cardKharcha.setOnClickListener {
            speakText(getString(R.string.yaha_kharcha_dalo))
            showExpenseBottomSheet(ExpenseType.KHARCHA)
        }

        // Aavak Card Click
        binding.cardAavak.setOnClickListener {
            speakText(getString(R.string.yaha_aavak_dalo))
            showExpenseBottomSheet(ExpenseType.AAVAK)
        }

        // Speaker icons
        binding.ivSpeakerKharcha.setOnClickListener {
            speakText(getString(R.string.yaha_kharcha_dalo))
        }

        binding.ivSpeakerAavak.setOnClickListener {
            speakText(getString(R.string.yaha_aavak_dalo))
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale("hi", "IN"))

            if (isEditMode) {
                speakText(getString(R.string.kharcha_aavak_edit_kar_sakte_ho))
            } else {
                speakText(getString(R.string.kharcha_ya_aavak_dalo))
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
        when (type) {
            ExpenseType.KHARCHA -> {
                bottomSheetBinding.tvTitle.text = getString(R.string.kharcha_dalo)
                bottomSheetBinding.fabMic.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.tamil_txt)
                    )
            }

            ExpenseType.AAVAK -> {
                bottomSheetBinding.tvTitle.text = getString(R.string.aavak_dalo)
                bottomSheetBinding.fabMic.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.greencolor)
                    )
            }
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
            getString(R.string.kharcha_bolo_jaise_khane_ke_500)
        } else {
            getString(R.string.aavak_bolo_jaise_kiraye_ke_5000)
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
        val (amount, note) = extractAmountAndNote(spokenText)

        if (amount > 0 && note.isNotEmpty()) {
            val item = ExpenseItem(
                note = note,
                amount = amount,
                type = currentBottomSheetType ?: ExpenseType.KHARCHA
            )
            tempBottomSheetList.add(item)
            currentBottomSheetAdapter?.notifyItemInserted(tempBottomSheetList.size - 1)

            speakText(getString(R.string.ka_rupaye_jod_diya, note, amount))
        } else {
            Toast.makeText(this,
                getString(R.string.samajh_nahi_aaya_dubara_bolo), Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseAndUpdateItem(spokenText: String, item: ExpenseItem) {
        val (amount, note) = extractAmountAndNote(spokenText)

        if (amount > 0 && note.isNotEmpty()) {
            item.note = note
            item.amount = amount
            currentBottomSheetAdapter?.notifyDataSetChanged()
            speakText(getString(R.string.ka_rupaye_update_ho_gaya, note, amount))
            Toast.makeText(this, getString(R.string.update_ho_gaya), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.samajh_nahi_aaya_dubara_bolo), Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractAmountAndNote(spokenText: String): Pair<Int, String> {
        try {
            // Find all numbers in the string
            val numberRegex = Regex("\\d+")
            val matches = numberRegex.findAll(spokenText).toList()

            if (matches.isEmpty()) return Pair(0, "")

            // Strategy: The largest number is likely the amount (cost), 
            // others are likely quantity (liters, kg, etc.)
            val maxMatch = matches.maxByOrNull { it.value.toLongOrNull() ?: 0L }

            if (maxMatch != null) {
                val amount = maxMatch.value.toIntOrNull() ?: 0

                // Remove ONLY the amount number from the text to get the note
                val range = maxMatch.range
                val prefix = spokenText.substring(0, range.first)
                val suffix = spokenText.substring(range.last + 1)

                var note = (prefix + suffix).replace(Regex("\\s+"), " ").trim()

                return Pair(amount, note)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(0, "")
    }

    private fun handleBottomSheetAction(item: ExpenseItem, action: String) {
        when (action) {
            "EDIT" -> showEditDialogForBottomSheet(item)
            "DELETE" -> {
                tempBottomSheetList.remove(item)
                currentBottomSheetAdapter?.notifyDataSetChanged()
                speakText(getString(R.string.delete_ho_gaya))
                Toast.makeText(this, getString(R.string.delete_ho_gaya), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Replace these methods in ThirdExpenseScreen.kt

    private fun showEditDialogForBottomSheet(item: ExpenseItem) {
        isEditBottomSheetOpen = true
        speakText(getString(R.string.edit_karne_ke_liye_bolo))
        Log.d("EditBottomSheet", "Opening edit bottom sheet")

        val editBottomSheet = BottomSheetDialog(this)
        val editView = layoutInflater.inflate(R.layout.bottom_sheet_edit_expense, null)
        editBottomSheet.setContentView(editView)
        editBottomSheet.setCanceledOnTouchOutside(false)

        val tvCurrentNote = editView.findViewById<TextView>(R.id.tvCurrentNote)
        val tvCurrentAmount = editView.findViewById<TextView>(R.id.tvCurrentAmount)
        val etNote = editView.findViewById<EditText>(R.id.etNote)
        val etAmount = editView.findViewById<EditText>(R.id.etAmount)
        val fabEditMic = editView.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabEditMic)
        val btnCancel = editView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnUpdate = editView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpdate)

        tvCurrentNote.text = "Current: ${item.note}"
        tvCurrentAmount.text = "Current: ₹${item.amount}"

        // Pre-fill input fields with current values
        etNote.setText(item.note)
        etAmount.setText(item.amount.toString())

        // Voice input button
        fabEditMic.setOnClickListener {
            Log.d("EditBottomSheet", "Mic button clicked")
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Naya bolo, jaise 'Diesel 3000'")
            }

            try {
                currentEditItem = item
                editItemLauncher.launch(intent)
                editBottomSheet.dismiss()
                isEditBottomSheetOpen = false
            } catch (e: Exception) {
                Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Update button for text input
        btnUpdate.setOnClickListener {
            val newNote = etNote.text?.toString()?.trim() ?: ""
            val newAmount = etAmount.text?.toString()?.trim()?.toIntOrNull() ?: 0

            if (newNote.isEmpty()) {
                Toast.makeText(this, "Note khali nahi ho sakta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newAmount <= 0) {
                Toast.makeText(this, "Amount sahi nahi hai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update item
            item.note = newNote
            item.amount = newAmount
            currentBottomSheetAdapter?.notifyDataSetChanged()

            speakText("$newNote ka $newAmount rupaye update ho gaya")
            Toast.makeText(this, getString(R.string.update_ho_gaya), Toast.LENGTH_SHORT).show()

            editBottomSheet.dismiss()
            isEditBottomSheetOpen = false
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

    private fun showEditDialogForMainList(item: ExpenseItem, type: ExpenseType) {
        android.util.Log.d("ThirdExpenseScreen", "=== showEditDialogForMainList === Opening edit bottom sheet for main list")

        isEditBottomSheetOpen = true
        speakText(getString(R.string.edit_karne_ke_liye_bolo))

        val editBottomSheet = BottomSheetDialog(this)
        val editView = layoutInflater.inflate(R.layout.bottom_sheet_edit_expense, null)
        editBottomSheet.setContentView(editView)
        editBottomSheet.setCanceledOnTouchOutside(false)

        val tvCurrentNote = editView.findViewById<TextView>(R.id.tvCurrentNote)
        val tvCurrentAmount = editView.findViewById<TextView>(R.id.tvCurrentAmount)
        val etNote = editView.findViewById<EditText>(R.id.etNote)
        val etAmount = editView.findViewById<EditText>(R.id.etAmount)
        val fabEditMic = editView.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabEditMic)
        val btnCancel = editView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnUpdate = editView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpdate)

        tvCurrentNote.text = "Current: ${item.note}"
        tvCurrentAmount.text = "Current: ₹${item.amount}"

        // Pre-fill input fields with current values
        etNote.setText(item.note)
        etAmount.setText(item.amount.toString())

        // Voice input button
        fabEditMic.setOnClickListener {
            android.util.Log.d("ThirdExpenseScreen", "Mic button clicked for main list edit")

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.naya_bolo_jaise_diesel_3000))
            }

            try {
                currentEditItem = item
                currentEditType = type
                editMainItemLauncher.launch(intent)
                editBottomSheet.dismiss()
                isEditBottomSheetOpen = false
            } catch (e: Exception) {
                Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Update button for text input
        btnUpdate.setOnClickListener {
            val newNote = etNote.text?.toString()?.trim() ?: ""
            val newAmount = etAmount.text?.toString()?.trim()?.toIntOrNull() ?: 0

            if (newNote.isEmpty()) {
                Toast.makeText(this, "Note khali nahi ho sakta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newAmount <= 0) {
                Toast.makeText(this, "Amount sahi nahi hai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update item
            item.note = newNote
            item.amount = newAmount

            // Update appropriate adapter
            if (type == ExpenseType.KHARCHA) {
                expenseAdapter.notifyDataSetChanged()
            } else {
                incomeAdapter.notifyDataSetChanged()
            }

            updateMunafa()
            speakText("$newNote ka $newAmount rupaye update ho gaya")
            Toast.makeText(this, getString(R.string.update_ho_gaya), Toast.LENGTH_SHORT).show()

            editBottomSheet.dismiss()
            isEditBottomSheetOpen = false
        }

        btnCancel.setOnClickListener {
            android.util.Log.d("ThirdExpenseScreen", "Cancel clicked")
            editBottomSheet.dismiss()
            isEditBottomSheetOpen = false
        }

        editBottomSheet.setOnDismissListener {
            isEditBottomSheetOpen = false
        }

        editBottomSheet.show()
        android.util.Log.d("ThirdExpenseScreen", "Edit bottom sheet shown for main list")
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
            Toast.makeText(this, getString(R.string.kuch_to_dalo_pehle), Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, getString(R.string.sab_save_ho_gaya), Toast.LENGTH_SHORT).show()
    }

    private fun handleExpenseAction(item: ExpenseItem, action: String, type: ExpenseType) {
        android.util.Log.d("ThirdExpenseScreen", "=== handleExpenseAction called === Action: $action, Item: ${item.note}, Type: $type")

        when (action) {
            "EDIT" -> {
                android.util.Log.d("ThirdExpenseScreen", "=== EDIT CASE === Opening edit dialog")

                // Yeh zaruri hai - thoda wait karo dialog dismiss hone ke baad
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showEditDialogForMainList(item, type)
                }, 100)
            }
            "DELETE" -> {
                android.util.Log.d("ThirdExpenseScreen", "=== DELETE CASE === Deleting item")

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
                speakText(getString(R.string.delete_ho_gaya))
                Toast.makeText(this, getString(R.string.delete_ho_gaya), Toast.LENGTH_SHORT).show()
            }
        }
    }


//    private fun showEditDialogForMainList(item: ExpenseItem, type: ExpenseType) {
//        android.util.Log.d("ThirdExpenseScreen", "=== showEditDialogForMainList === Opening edit bottom sheet for main list")
//
//        isEditBottomSheetOpen = true
//        speakText(getString(R.string.edit_karne_ke_liye_bolo))
//
//        // Create custom bottom sheet for voice edit
//        val editBottomSheet = BottomSheetDialog(this)
//        val editView = layoutInflater.inflate(R.layout.bottom_sheet_edit_expense, null)
//        editBottomSheet.setContentView(editView)
//
//        // Prevent dismiss on outside touch while editing
//        editBottomSheet.setCanceledOnTouchOutside(false)
//
//        val tvCurrentNote = editView.findViewById<TextView>(R.id.tvCurrentNote)
//        val tvCurrentAmount = editView.findViewById<TextView>(R.id.tvCurrentAmount)
//        val fabEditMic = editView.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabEditMic)
//        val btnCancel = editView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
//
//        tvCurrentNote.text = "Current: ${item.note}"
//        tvCurrentAmount.text = "Current: ₹${item.amount}"
//
//        fabEditMic.setOnClickListener {
//            android.util.Log.d("ThirdExpenseScreen", "Mic button clicked for main list edit")
//
//            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
//                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
//                putExtra(RecognizerIntent.EXTRA_PROMPT,
//                    getString(R.string.naya_bolo_jaise_diesel_3000))
//            }
//
//            try {
//                // Store current item and type for editing
//                currentEditItem = item
//                currentEditType = type
//
//                editMainItemLauncher.launch(intent)
//                editBottomSheet.dismiss()
//                isEditBottomSheetOpen = false
//            } catch (e: Exception) {
//                Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        btnCancel.setOnClickListener {
//            android.util.Log.d("ThirdExpenseScreen", "Cancel clicked")
//            editBottomSheet.dismiss()
//            isEditBottomSheetOpen = false
//        }
//
//        editBottomSheet.setOnDismissListener {
//            isEditBottomSheetOpen = false
//        }
//
//        editBottomSheet.show()
//        android.util.Log.d("ThirdExpenseScreen", "Edit bottom sheet shown for main list")
//    }

    // Current edit item aur type store karne ke liye
    private var currentEditType: ExpenseType? = null

    // Main list items edit karne ke liye naya launcher
    private val editMainItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty() && currentEditItem != null && currentEditType != null) {
                val spokenText = results[0]
                parseAndUpdateMainItem(spokenText, currentEditItem!!, currentEditType!!)
            }
        }
    }
    private fun parseAndUpdateMainItem(spokenText: String, item: ExpenseItem, type: ExpenseType) {
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

            // Update appropriate adapter
            if (type == ExpenseType.KHARCHA) {
                expenseAdapter.notifyDataSetChanged()
            } else {
                incomeAdapter.notifyDataSetChanged()
            }

            updateMunafa()
            speakText("$note ka $amount rupaye update ho gaya")
            Toast.makeText(this, getString(R.string.update_ho_gaya), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.samajh_nahi_aaya_dubara_bolo), Toast.LENGTH_SHORT).show()
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
            binding.tvProfit.setTextColor(getColor(R.color.color_primary))
            binding.tvProfitLabel.text = getString(R.string.total_profit)
        } else {
            binding.tvProfit.setTextColor(getColor(R.color.tamil_txt))
            binding.tvProfitLabel.text = getString(R.string.nuksan)
        }
    }

// Replace these methods in ThirdExpenseScreen.kt

    private fun checkAndCompleteTrip() {
        // Step 1: First ask for end date
        showEndDatePicker()
    }

//    private fun showEndDatePicker() {
//        val calendar = Calendar.getInstance()
//        val year = calendar.get(Calendar.YEAR)
//        val month = calendar.get(Calendar.MONTH)
//        val day = calendar.get(Calendar.DAY_OF_MONTH)
//
//        val datePickerDialog = DatePickerDialog(
//            this,
//            { _, selectedYear, selectedMonth, selectedDay ->
//                calendar.set(selectedYear, selectedMonth, selectedDay)
//                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN"))
//                endDate = dateFormat.format(calendar.time)
//
//                speakText(getString(R.string.trip_khatam_ab_data_save_kar_rahe_hain))
//
//                // Step 2: After date selected, show confirmation dialog
//                showCompleteDialog()
//            },
//            year, month, day
//        )
//
//        datePickerDialog.setTitle(getString(R.string.trip_khatam_hone_ki_tareekh))
//        datePickerDialog.datePicker.calendarViewShown = true
//        datePickerDialog.show()
//    }

    private fun showCompleteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_trip_complete, null)

        // Find views
        val tvTruckNumber = dialogView.findViewById<TextView>(R.id.tvTruckNumber)
        val tvStartInfo = dialogView.findViewById<TextView>(R.id.tvStartInfo)
        val tvEndInfo = dialogView.findViewById<TextView>(R.id.tvEndInfo)
        val tvTotalExpense = dialogView.findViewById<TextView>(R.id.tvTotalExpense)
        val tvTotalIncome = dialogView.findViewById<TextView>(R.id.tvTotalIncome)
        val tvProfit = dialogView.findViewById<TextView>(R.id.tvProfit)
        val etDriverIncome = dialogView.findViewById<EditText>(R.id.etDriverIncome)

        // Calculate values
        val totalExpense = expenseList.sumOf { it.amount }
        val totalIncome = incomeList.sumOf { it.amount }
        val profit = totalIncome - totalExpense

        // Set values
        tvTruckNumber.text = getTruckNumber()
        tvStartInfo.text = "${getStartDate()} - ${getStartPlace()}"
        tvEndInfo.text = "$endDate - ${getEndPlace()}"
        tvTotalExpense.text = "₹$totalExpense"
        tvTotalIncome.text = "₹$totalIncome"
        tvProfit.text = "₹$profit"

        // Set profit color
        if (profit >= 0) {
            tvProfit.setTextColor(ContextCompat.getColor(this, R.color.greencolor))
        } else {
            tvProfit.setTextColor(ContextCompat.getColor(this, R.color.tamil_txt))
        }

        // Create and show dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Handle buttons
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirm)

        btnCancel.setOnClickListener {
            dialog.dismiss()
            endDate = "" // Reset end date
        }

        btnConfirm.setOnClickListener {
            val driverIncome = etDriverIncome.text?.toString()?.trim().takeIf { !it.isNullOrEmpty() } ?: "0"

            if (endDate.isEmpty()) {
                Toast.makeText(this, getString(R.string.end_date_chuno), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            callApiAndNavigate(endDate, driverIncome)
        }

        dialog.show()
    }

//    private fun callApiAndNavigate(endDate: String, driverIncome: String) {
//        val incomeModels = incomeList.map {
//            Income(
//                desc = it.note,
//                amount = it.amount.toString(),
//                note = it.note,
//                place = "",
//                date = ""
//            )
//        }
//        val expenseModels = expenseList.map {
//            Expense(
//                desc = it.note,
//                amount = it.amount.toString(),
//                note = it.note,
//                place = "",
//                date = "",
//                type = "Expense",
//                liters = "",
//                km = ""
//            )
//        }
//        val totalIncome = incomeList.sumOf { it.amount }.toString()
//        val totalExpense = expenseList.sumOf { it.amount }.toString()
//        val ownerProfit = (incomeList.sumOf { it.amount } - expenseList.sumOf { it.amount }).toString()
//        val totalDays = calculateTotalDays(getStartDate(), endDate)
//
//        showProgress()
//
//        authViewModel.getTripPdf(
//            AddTripRequestModel(
//                if (isEditMode) (intent.getStringExtra("TRIP_ID") ?: "") else "",
//                getEndPlace(),
//                driverIncome,
//                endDate,
//                expenseModels,
//                incomeModels,
//                ownerProfit,
//                getStartPlace(),
//                getStartDate(),
//                totalDays,
//                totalExpense,
//                totalIncome,
//                "0",
//                getTruckNumber()
//            )
//        )
//    }
//
//    private fun calculateTotalDays(start: String, end: String): String {
//        return try {
//            val fmt = SimpleDateFormat("dd MMM yyyy", Locale("en", "IN"))
//            val s = fmt.parse(start)
//            val e = fmt.parse(end)
//            if (s != null && e != null) {
//                val diff = (e.time - s.time) / (1000 * 60 * 60 * 24)
//                diff.toString()
//            } else "0"
//        } catch (e: Exception) {
//            "0"
//        }
//    }




    // Replace these methods in ThirdExpenseScreen.kt

    private fun showEndDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)

                // Format for display (Hindi locale)
                val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN"))
                endDate = displayFormat.format(calendar.time)

                speakText(getString(R.string.trip_khatam_ab_data_save_kar_rahe_hain))

                // Step 2: After date selected, show confirmation dialog
                showCompleteDialog()
            },
            year, month, day
        )

        datePickerDialog.setTitle(getString(R.string.trip_khatam_hone_ki_tareekh))
        datePickerDialog.datePicker.calendarViewShown = true
        datePickerDialog.show()
    }

    private fun callApiAndNavigate(endDate: String, driverIncome: String) {
        val incomeModels = incomeList.map {
            Income(
                desc = it.note,
                amount = it.amount.toString(),
                note = it.note,
                place = "",
                date = ""
            )
        }
        val expenseModels = expenseList.map {
            Expense(
                desc = it.note,
                amount = it.amount.toString(),
                note = it.note,
                place = "",
                date = "",
                type = "Expense",
                liters = "",
                km = ""
            )
        }
        val totalIncome = incomeList.sumOf { it.amount }.toString()
        val totalExpense = expenseList.sumOf { it.amount }.toString()
        val ownerProfit = (incomeList.sumOf { it.amount } - expenseList.sumOf { it.amount }).toString()
        val totalDays = calculateTotalDays(getStartDate(), endDate)

        // ✅ Convert dates to "yyyy-MM-dd" format for API
        val apiStartDate = convertToApiDateFormat(getStartDate())
        val apiEndDate = convertToApiDateFormat(endDate)

        showProgress()

        authViewModel.getTripPdf(
            AddTripRequestModel(
                if (isEditMode) (intent.getStringExtra("TRIP_ID") ?: "") else "",
                getEndPlace(),
                driverIncome,
                apiEndDate,  // ✅ Using converted format
                expenseModels,
                incomeModels,
                ownerProfit,
                getStartPlace(),
                apiStartDate,  // ✅ Using converted format
                totalDays,
                totalExpense,
                totalIncome,
                "0",
                getTruckNumber()
            )
        )
    }

    // ✅ NEW METHOD: Convert "dd MMM yyyy" to "yyyy-MM-dd"
    private fun convertToApiDateFormat(dateString: String): String {
        return try {
            // Parse from display format
            val inputFormat = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN"))
            val date = inputFormat.parse(dateString)

            // Convert to API format "yyyy-MM-dd"
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            outputFormat.format(date ?: Calendar.getInstance().time)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: return current date in correct format
            val fallbackFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            fallbackFormat.format(Calendar.getInstance().time)
        }
    }

    private fun calculateTotalDays(start: String, end: String): String {
        return try {
            val fmt = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN"))
            val s = fmt.parse(start)
            val e = fmt.parse(end)
            if (s != null && e != null) {
                val diff = (e.time - s.time) / (1000 * 60 * 60 * 24)
                diff.toString()
            } else "0"
        } catch (e: Exception) {
            "0"
        }
    }

// Remove old generateTripReport() method - not needed anymore

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
        Toast.makeText(this, getString(R.string.data_save_ho_raha_hai), Toast.LENGTH_SHORT).show()

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
            speakText(getString(R.string.trip_ka_report_tayar_ho_gaya))

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.trip_complete))
                .setMessage(
                    getString(R.string.trip_details) +
                            getString(R.string.truck, getTruckNumber()) +
                            getString(R.string.start, getStartDate(), getStartPlace()) +
                            getString(R.string.end, endDate, getEndPlace()) +
                            getString(R.string.total_kharcha, expenseList.sumOf { it.amount }) +
                            getString(R.string.total_aavak, incomeList.sumOf { it.amount }) +
                            getString(
                                R.string.munafa,
                                incomeList.sumOf { it.amount } - expenseList.sumOf { it.amount }))
                .setPositiveButton("OK") { _, _ ->
                    isTripCompleted = true
                    finish()
                }
                .show()
        }, 1500)
    }

    // Helper functions to get data from previous screens
    // You should pass these via Intent extras
    private fun getTruckNumber(): String {
        return intent.getStringExtra("TRUCK_NUMBER") ?: intent.getStringExtra("truck_number") ?: "GJ-11-1234"
    }

    private fun getStartDate(): String {
        return intent.getStringExtra("START_DATE") ?: intent.getStringExtra("start_date") ?: "01 Jan 2025"
    }

    private fun getStartPlace(): String {
        return intent.getStringExtra("START_PLACE") ?: intent.getStringExtra("start_place") ?: "Mumbai"
    }

    private fun getEndPlace(): String {
        return intent.getStringExtra("END_PLACE") ?: intent.getStringExtra("end_place") ?: "Delhi"
    }
    private fun generateTripId() {
        tripId = UUID.randomUUID().toString()
    }

    private fun setAllData() {
        if (isTripCompleted) return

        GlobalScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val draftDao = db.productsDao()
            
            // Prepare lists for checking/saving
            val currentCreditList = incomeList.map { 
                Income(
                    desc = it.note,
                    amount = it.amount.toString(),
                    note = it.note,
                    place = "",
                    date = ""
                ) 
            }
            
            val currentDebitList = expenseList.map { 
                Expense(
                    desc = it.note,
                    amount = it.amount.toString(),
                    note = it.note,
                    place = "",
                    date = "",
                    type = "Expense",
                    liters = "",
                    km = ""
                ) 
            }

            val currentModel = TripDataTestModel(
                getTruckNumber(),
                getStartPlace(),
                getEndPlace(),
                getStartDate(),
                "",
                "",
                tripId,
                "",
                currentCreditList,
                currentDebitList
            )
            
            val existing = draftDao.getDraftById(tripId)
            if (existing != null) {
                val existingModel = convertToModel(existing)
                if (existingModel == currentModel) {
                    return@launch
                }
            } else if (isEditMode) {
                 // Check against Original Data (Completed Trips)
                 // Normalize Constants lists to match setAllData format for comparison
                 val normalizedOriginalCreditList = Constants.creditList.map {
                    Income(
                        desc = it.note ?: "",
                        amount = (it.amount?.toIntOrNull() ?: 0).toString(),
                        note = it.note ?: "",
                        place = "",
                        date = ""
                    )
                 }
                 
                 val normalizedOriginalDebitList = Constants.debitList.map {
                    Expense(
                        desc = it.note ?: "",
                        amount = (it.amount?.toIntOrNull() ?: 0).toString(),
                        note = it.note ?: "",
                        place = "",
                        date = "",
                        type = "Expense",
                        liters = "",
                        km = ""
                    )
                 }
                 
                 val originalModelToCheck = TripDataTestModel(
                    originalTruckNumber ?: getTruckNumber(),
                    originalStartPlace ?: getStartPlace(),
                    originalEndPlace ?: getEndPlace(),
                    originalStartDate ?: getStartDate(),
                    "",
                    "",
                    tripId,
                    "",
                    normalizedOriginalCreditList,
                    normalizedOriginalDebitList
                 )
                 
                 if (currentModel == originalModelToCheck) {
                     return@launch
                 }
            }
            
            if (!getAllData(applicationContext).contains(currentModel)) {
                testList.clear()
                testList.add(currentModel)

                val tripDataList: ArrayList<TripDataTestModel> = testList

                val tripDataEntities = tripDataList.map {
                    Products(
                        truckNumber = it.truckNumber,
                        srcPlace = it.srcPlace,
                        destPlace = it.destPlace,
                        srcDate = it.srcDate,
                        destDate = it.destDate,
                        avg = it.avg,
                        randomNumber = tripId,
                        modelList1 = Gson().toJson(it.modelList1),
                        modelList2 = Gson().toJson(it.modelList2)
                    )
                }

                tripDataEntities.forEach { draftDao.insert(it) }
                Constants.refreshApiGet(Event(1))
            }
        }
    }

    private suspend fun getAllData(context: Context): List<TripDataTestModel> {
        val database = AppDatabase.getDatabase(context)
        val tripDataDao = database.productsDao()
        val entities = tripDataDao.getAllProducts()
        return entities.map {
            convertToModel(it)
        }
    }

    private fun convertToModel(entity: Products): TripDataTestModel {
        val gson = Gson()
        return TripDataTestModel(
            truckNumber = entity.truckNumber,
            srcPlace = entity.srcPlace,
            destPlace = entity.destPlace,
            srcDate = entity.srcDate,
            destDate = entity.destDate,
            avg = entity.avg,
            randomNumber = entity.randomNumber,
            modelList1 = gson.fromJson(
                entity.modelList1,
                object : TypeToken<List<Income>>() {}.type
            ),
            modelList2 = gson.fromJson(
                entity.modelList2,
                object : TypeToken<List<Expense>>() {}.type
            ),
            id = entity.id.toString()
        )
    }

    override fun onStop() {
        super.onStop()
        setAllData()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}
