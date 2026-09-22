package com.dadabarbie.TruckTrip.activity

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.DateUtils
import com.dadabarbie.TruckTrip.Utils.DraftIdManager
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.Utils.RouteUtils
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.Utils.TripEditManager
import com.dadabarbie.TruckTrip.Utils.TripEditManager.isEditMode
import com.dadabarbie.TruckTrip.Utils.VoiceExpenseParser
import com.dadabarbie.TruckTrip.adapter.ExpenseAdapter
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityThirdExpenseScreenBinding
import com.dadabarbie.TruckTrip.databinding.BottomSheetExpenseBinding
import com.dadabarbie.TruckTrip.model.addTrip.AddTripRequestModel
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.dadabarbie.TruckTrip.room.model.TripDataTestModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vasyerp.cafvd.room.model.Products
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ThirdExpenseScreen : BaseActivity(), TextToSpeech.OnInitListener {
    private val authViewModel: AuthViewModel by viewModels()

    data class ExpenseItem(
        var id: String = UUID.randomUUID().toString(),
        var note: String,
        var amount: Int,
        var type: ExpenseType
    )

    private var originalRoute: ArrayList<String> = arrayListOf()
    enum class ExpenseType { KHARCHA, AAVAK }

    private val binding by lazy { ActivityThirdExpenseScreenBinding.inflate(layoutInflater) }
    private lateinit var textToSpeech: TextToSpeech
    private var ttsInitialized = false
    private var isEditBottomSheetOpen = false
    private val expenseList = mutableListOf<ExpenseItem>()
    private val incomeList = mutableListOf<ExpenseItem>()
    private var pdfFilePath: String = ""
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
    private var isVoiceListening = false
    private var currentEditItem: ExpenseItem? = null
    private var currentEditType: ExpenseType? = null
    @Volatile
    private var isCompletingTrip = false

    // 🆕 NEW: Variables for quick add feature
    private var selectedAmount = 0
    private var selectedCategory = ""

    private var endDate = ""
    var langCode = ""
    var routeArray: ArrayList<String> = arrayListOf()

    private val voiceRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isVoiceListening = false
        when (result.resultCode) {
            RESULT_OK -> {
                val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    Log.d("VoiceExpense", "Results: $results")
                    var parsed = false
                    for (spokenText in results) {
                        val (amount, note) = VoiceExpenseParser.extractAmountAndNote(spokenText, langCode)
                        if (amount > 0 && note.isNotEmpty()) {
                            parseAndAddExpense(amount, note)
                            parsed = true
                            break
                        }
                    }
                    if (!parsed) {
                        Toast.makeText(this, getString(R.string.samajh_nahi_aaya_dubara_bolo), Toast.LENGTH_SHORT).show()
                        speakText(getString(R.string.samajh_nahi_aaya_dubara_bolo))
                    }
                }
            }
            RESULT_CANCELED -> Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this, getString(R.string.samajh_nahi_aaya_dubara_bolo), Toast.LENGTH_SHORT).show()
        }
    }

    private val editItemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isVoiceListening = false
        if (result.resultCode == RESULT_OK && currentEditItem != null) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                for (spokenText in results) {
                    val (amount, note) = VoiceExpenseParser.extractAmountAndNote(spokenText, langCode)
                    if (amount > 0 && note.isNotEmpty()) {
                        parseAndUpdateItem(amount, note, currentEditItem!!)
                        break
                    }
                }
            }
        }
        currentEditItem = null
    }

    private val editMainItemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isVoiceListening = false
        if (result.resultCode == RESULT_OK && currentEditItem != null && currentEditType != null) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                for (spokenText in results) {
                    val (amount, note) = VoiceExpenseParser.extractAmountAndNote(spokenText, langCode)
                    if (amount > 0 && note.isNotEmpty()) {
                        parseAndUpdateMainItem(amount, note, currentEditItem!!, currentEditType!!)
                        break
                    }
                }
            }
        }
        currentEditItem = null
        currentEditType = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        langCode = Prefs[Constants.languageCode] ?: "hi"
        intent.getStringExtra("TRIP_ID")?.let { tripId = it }
        routeArray = intent.getStringArrayListExtra("ROUTE_ARRAY") ?: arrayListOf()
        if (tripId.isEmpty()) {
            tripId = DraftIdManager.generate()
        }
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        if (isEditMode) {
            originalTruckNumber = intent.getStringExtra("ORIGINAL_TRUCK_NUMBER")
            originalStartDate = intent.getStringExtra("ORIGINAL_START_DATE")
            originalStartPlace = intent.getStringExtra("ORIGINAL_START_PLACE")
            originalEndPlace = intent.getStringExtra("ORIGINAL_END_PLACE")
            originalRoute = ArrayList(routeArray)
            intent.getStringExtra("END_DATE")?.let { if (it.isNotEmpty()) endDate = it }
        }

        val retrievedEndDate = intent.getStringExtra("END_DATE")
        if (!retrievedEndDate.isNullOrEmpty()) {
            endDate = retrievedEndDate
        }
        textToSpeech = TextToSpeech(this, this)
        setupViews()
        updateMunafa()
        setupEditMode()
        setObserver()
    }

    private fun setObserver() {
        authViewModel.downloadCompleted.observe(this, Observer { data ->
            dismissProgress()
            if (data.isNullOrEmpty()) {
                Toast.makeText(this, "PDF generation failed", Toast.LENGTH_SHORT).show()
                navigateToDashboard("")
            } else {
                try {
                    pdfFilePath = data
                    lifecycleScope.launch(Dispatchers.IO) {
                        AppDatabase
                            .getDatabase(applicationContext)
                            .productsDao()
                            .deleteByRandomNumber(tripId)
                    }
                    openFile(pdfFilePath)
                    Handler(Looper.getMainLooper()).postDelayed({ navigateToDashboard(pdfFilePath) }, 1000)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    navigateToDashboard("")
                }
            }
        })
    }

    private fun navigateToDashboard(data: String) {
        isTripCompleted = true
        TripEditManager.clearEditMode()
        Constants.creditList.clear()
        Constants.debitList.clear()
        Constants.refreshApiGet(Event(1))

        com.dadabarbie.TruckTrip.ads.AdMobManager.onTripSavedSuccessfully(this) {
            if (!isFinishing && !isDestroyed) {
                finish()
            }
        }
    }

    private fun openFile(data: String) {
        try {
            val uri = FileProvider.getUriForFile(this, packageName, File(data))
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            )
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (e: Exception) {
            Log.e("OpenFile", "Error", e)
        }
    }

    private fun setupEditMode() {
        if (Constants.debitList.isNotEmpty()) {
            expenseList.addAll(Constants.debitList.map {
                ExpenseItem(note = it.desc ?: "", amount = it.amount?.toIntOrNull() ?: 0, type = ExpenseType.KHARCHA)
            })
            expenseAdapter.notifyDataSetChanged()
            binding.layoutExpenseEmpty.visibility = View.GONE
        }
        if (Constants.creditList.isNotEmpty()) {
            incomeList.addAll(Constants.creditList.map {
                ExpenseItem(note = it.desc ?: "", amount = it.amount?.toIntOrNull() ?: 0, type = ExpenseType.AAVAK)
            })
            incomeAdapter.notifyDataSetChanged()
            binding.layoutIncomeEmpty.visibility = View.GONE
        }
        updateMunafa()
        speakText(getString(R.string.kharcha_aavak_edit_karein_chahiye_to_add_ya_delete_karo))
    }

    private fun setupViews() {
        binding.backBtn.setOnClickListener { finish() }
        binding.done.setOnClickListener { checkAndCompleteTrip() }
        expenseAdapter = ExpenseAdapter(expenseList) { item, action -> handleExpenseAction(item, action, ExpenseType.KHARCHA) }
        incomeAdapter = ExpenseAdapter(incomeList) { item, action -> handleExpenseAction(item, action, ExpenseType.AAVAK) }
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = expenseAdapter
        binding.rvIncome.layoutManager = LinearLayoutManager(this)
        binding.rvIncome.adapter = incomeAdapter
        binding.cardKharcha.setOnClickListener {
            speakText(getString(R.string.yaha_kharcha_dalo) + getString(R.string.voice_all_expense_add_and_save))
            showExpenseBottomSheet(ExpenseType.KHARCHA)
        }
        binding.cardAavak.setOnClickListener {
            speakText(getString(R.string.yaha_aavak_dalo) + getString(R.string.voice_all_income_add_and_save))
            showExpenseBottomSheet(ExpenseType.AAVAK)
        }
        binding.ivSpeakerKharcha.setOnClickListener { speakText(getString(R.string.yaha_kharcha_dalo)) }
        binding.ivSpeakerAavak.setOnClickListener { speakText(getString(R.string.yaha_aavak_dalo)) }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale(langCode))
            ttsInitialized = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
            if (ttsInitialized) {
                speakText(if (isEditMode) getString(R.string.kharcha_aavak_edit_kar_sakte_ho)
                else getString(R.string.kharcha_ya_aavak_dalo))
            }
        }
    }

    private fun speakText(text: String) {
        if (ttsInitialized) textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private var currentBottomSheetAdapter: ExpenseAdapter? = null

    // 🆕 UPDATED: showExpenseBottomSheet with DYNAMIC chips based on type
    // 🆕 UPDATED: showExpenseBottomSheet with FULL SCREEN support
    private fun showExpenseBottomSheet(type: ExpenseType) {
        currentBottomSheetType = type
        tempBottomSheetList.clear()
        selectedAmount = 0
        selectedCategory = ""

        val bottomSheetBinding = BottomSheetExpenseBinding.inflate(layoutInflater)
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme).apply {
            setContentView(bottomSheetBinding.root)

            // ✅ Make bottom sheet full screen
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isDraggable = true

            // ✅ Set peek height to full screen
            behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels

            // ✅ Prevent dismiss on outside touch
            setCanceledOnTouchOutside(false)
        }

        currentBottomSheetDialog = bottomSheetDialog

        when (type) {
            ExpenseType.KHARCHA -> {
                bottomSheetBinding.tvTitle.text = getString(R.string.kharcha_dalo)
                bottomSheetBinding.fabMic.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.tamil_txt)
                )
            }
            ExpenseType.AAVAK -> {
                bottomSheetBinding.tvTitle.text = getString(R.string.aavak_dalo)
                bottomSheetBinding.fabMic.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.greencolor)
                )
            }
        }

        // ✅ Setup RecyclerView with proper layout manager
        currentBottomSheetAdapter = ExpenseAdapter(tempBottomSheetList) { item, action ->
            handleBottomSheetAction(item, action)
        }
        bottomSheetBinding.rvBottomSheetExpenses.apply {
            layoutManager = LinearLayoutManager(this@ThirdExpenseScreen)
            adapter = currentBottomSheetAdapter
            // ✅ Enable nested scrolling
            isNestedScrollingEnabled = true
            setHasFixedSize(false)
        }

        // Setup dynamic chips
        setupDynamicQuickAddChips(bottomSheetBinding, type)

        bottomSheetBinding.btnCustomAmount.setOnClickListener {
            showCustomAmountDialog(type)
        }
        bottomSheetBinding.btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetBinding.fabMic.setOnClickListener {
            startVoiceRecognitionForBottomSheet()
        }

        bottomSheetBinding.btnSave.setOnClickListener {
            saveBottomSheetExpenses()
            bottomSheetDialog.dismiss()
        }

        // ✅ Handle back button in bottom sheet
        bottomSheetDialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                event.action == android.view.KeyEvent.ACTION_UP) {
                bottomSheetDialog.dismiss()
                true
            } else {
                false
            }
        }

        bottomSheetDialog.show()

        // ✅ Force expand after show
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // 🆕 NEW METHOD: Setup DYNAMIC chips based on Expense or Income type
    private fun setupDynamicQuickAddChips(bottomSheetBinding: BottomSheetExpenseBinding, type: ExpenseType) {
        val amountChipGroup = bottomSheetBinding.chipGroupAmounts
        val categoryChipGroup = bottomSheetBinding.chipGroupCategories

        // Clear existing chips
        amountChipGroup.removeAllViews()
        categoryChipGroup.removeAllViews()

        // 🔴 DIFFERENT AMOUNT CHIPS FOR EXPENSE vs INCOME
        val amounts = if (type == ExpenseType.KHARCHA) {
            // Expense amounts: smaller values
            listOf(50, 100, 200, 500, 1000, 2000, 5000)
        } else {
            // Income amounts: larger values
            listOf(10000, 15000, 20000, 25000, 30000, 40000, 50000)
        }

        // 🔴 DIFFERENT CATEGORY CHIPS FOR EXPENSE vs INCOME
        val categories = if (type == ExpenseType.KHARCHA) {
            // Expense categories
            mapOf(
                getString(R.string.diesel_) to getString(R.string.diesel),
                getString(R.string.khana_) to getString(R.string.food),
                getString(R.string.toll_) to getString(R.string.toll_tax),
                getString(R.string.repair_) to getString(R.string.repair),
                getString(R.string.chai_pani_) to getString(R.string.chai_pani),
                getString(R.string.other_) to getString(R.string.other)
            )
        } else {
            // Income categories
            mapOf(
                "📦 ${getString(R.string.loading_bhada)}" to getString(R.string.loading_bhada),
                "🚛 ${getString(R.string.unloading_bhada)}" to getString(R.string.unloading_bhada),
                "💰 ${getString(R.string.advance)}" to getString(R.string.advance),
                "🧾 ${getString(R.string.balance_slip)}" to getString(R.string.balance_slip),
                "⏱️ ${getString(R.string.waiting_charges_rukne_ka_charge)}" to getString(R.string.waiting_charges_rukne_ka_charge),
                "➕ ${getString(R.string.extra_income)}" to getString(R.string.extra_income)
            )
        }

        // 🆕 ADD AMOUNT CHIPS DYNAMICALLY
        amounts.forEach { amount ->
            val chip = Chip(this).apply {
                text = "₹$amount"
                textSize = 14f
                isCheckable = true
                chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                setChipStrokeColorResource(R.color.color_primary)
                chipStrokeWidth = 2f
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedAmount = amount
                        Log.d("QuickAdd", "Amount selected: $selectedAmount")
                        if (selectedCategory.isNotEmpty()) {
                            addQuickExpense(selectedAmount, selectedCategory)
                            amountChipGroup.clearCheck()
                            categoryChipGroup.clearCheck()
                        }
                    }
                }
            }
            amountChipGroup.addView(chip)
        }

        // 🆕 ADD CATEGORY CHIPS DYNAMICALLY
        categories.forEach { (displayText, value) ->
            val chip = Chip(this).apply {
                text = displayText
                textSize = 14f
                isCheckable = true
                chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                setChipStrokeColorResource(R.color.color_primary)
                chipStrokeWidth = 2f
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategory = value
                        Log.d("QuickAdd", "Category selected: $selectedCategory")
                        if (selectedAmount > 0) {
                            addQuickExpense(selectedAmount, selectedCategory)
                            amountChipGroup.clearCheck()
                            categoryChipGroup.clearCheck()
                        }
                    }
                }
            }
            categoryChipGroup.addView(chip)
        }
    }

    // 🆕 NEW METHOD: Add Quick Expense
    private fun addQuickExpense(amount: Int, category: String) {
        val item = ExpenseItem(
            note = category,
            amount = amount,
            type = currentBottomSheetType ?: ExpenseType.KHARCHA
        )
        tempBottomSheetList.add(item)
        currentBottomSheetAdapter?.notifyItemInserted(tempBottomSheetList.size - 1)

        speakText("$category ka $amount rupaye jod diya")
        Toast.makeText(this, "✓ $category - ₹$amount", Toast.LENGTH_SHORT).show()

        selectedAmount = 0
        selectedCategory = ""
    }

    // 🆕 UPDATED: Show Custom Amount Dialog with DYNAMIC categories
    private fun showCustomAmountDialog(type: ExpenseType) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_amount, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val tvAmountDisplay = dialogView.findViewById<TextView>(R.id.tvAmountDisplay)
        val spinnerCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerCategory)
        val btnAddExpense = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddExpense)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)

        var currentAmount = ""
        var selectedCat = ""

        // 🔴 DIFFERENT CATEGORIES FOR EXPENSE vs INCOME
        val categories = if (type == ExpenseType.KHARCHA) {
            arrayOf(getString(R.string.diesel), getString(R.string.food), getString(R.string.toll_tax), getString(R.string.puncture_tire), getString(R.string.driver_kharch),
                getString(R.string.maintenance), getString(R.string.unloading_charge),
                getString(R.string.loading_charge), getString(R.string.police_rto_fine),
                getString(R.string.parking_charge), getString(R.string.broker_commission),
                getString(R.string.document_paper_kharcha), getString(R.string.engine_oil_lubricants),
                getString(R.string.truck_service), getString(R.string.border_entry),
                getString(R.string.weighbridge_charges), getString(R.string.spare_parts),
                getString(R.string.driver_personal_kharcha), getString(R.string.truck_electrical_items),
                getString(R.string.mechanic_charges), getString(R.string.road_repair_contribution),
                getString(R.string.truck_tools_equipment), getString(R.string.medicine_first_aid),
                getString(R.string.challan_fine), getString(R.string.truck_insurance),
                getString(R.string.mandi_charges), getString(R.string.other))
        } else {
            arrayOf(
                getString(R.string.loading_bhada),
                getString(R.string.unloading_bhada),
                getString(R.string.advance),
                getString(R.string.balance_slip),
                getString(R.string.waiting_charges_rukne_ka_charge),
                getString(R.string.extra_income)
            )
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerCategory.setAdapter(adapter)
        spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCat = categories[position]
            updateAddButton(btnAddExpense, currentAmount, selectedCat)
        }

        // Setup number pad
        val numberButtons = listOf(
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn0),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn1),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn2),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn3),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn4),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn5),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn6),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn7),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn8),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn9),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn00)
        )

        numberButtons.forEach { button ->
            button?.setOnClickListener {
                currentAmount += button.text
                tvAmountDisplay.text = "₹$currentAmount"
                updateAddButton(btnAddExpense, currentAmount, selectedCat)
            }
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClear)?.setOnClickListener {
            currentAmount = ""
            tvAmountDisplay.text = "₹0"
            updateAddButton(btnAddExpense, currentAmount, selectedCat)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnAddExpense.setOnClickListener {
            val amount = currentAmount.toIntOrNull() ?: 0
            if (amount > 0 && selectedCat.isNotEmpty()) {
                addQuickExpense(amount, selectedCat)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateAddButton(
        button: com.google.android.material.button.MaterialButton,
        amount: String,
        category: String
    ) {
        val hasAmount = amount.isNotEmpty() && amount.toIntOrNull() ?: 0 > 0
        val hasCategory = category.isNotEmpty()
        button.isEnabled = hasAmount && hasCategory
    }

    private fun startVoiceRecognitionForBottomSheet() {
        if (isVoiceListening) return Toast.makeText(this,
            getString(R.string.pehle_se_sun_rahe_hain), Toast.LENGTH_SHORT).show()
        if (ttsInitialized && textToSpeech.isSpeaking) textToSpeech.stop()

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                isVoiceListening = true
                val speechLangCode = mapLangCode(langCode)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLangCode)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechLangCode)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN"))
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
                    putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_PROMPT,
                        if (currentBottomSheetType == ExpenseType.KHARCHA) getString(R.string.kharcha_bolo_jaise_khane_ke_500)
                        else getString(R.string.aavak_bolo_jaise_kiraye_ke_5000))
                }
                voiceRecognitionLauncher.launch(intent)
                Toast.makeText(this, getString(R.string.boliye), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isVoiceListening = false
                Toast.makeText(this, "Voice recognition error", Toast.LENGTH_SHORT).show()
            }
        }, 300)
    }

    private fun mapLangCode(code: String) = when (code) {
        "hi" -> "hi-IN"; "gu" -> "gu-IN"; "mr" -> "mr-IN"; "ta" -> "ta-IN"
        "te" -> "te-IN"; "kn" -> "kn-IN"; "ml" -> "ml-IN"; "bn" -> "bn-IN"; "pa" -> "pa-IN"
        else -> "en-IN"
    }

    private fun parseAndAddExpense(amount: Int, note: String) {
        val item = ExpenseItem(note = note, amount = amount, type = currentBottomSheetType ?: ExpenseType.KHARCHA)
        tempBottomSheetList.add(item)
        currentBottomSheetAdapter?.notifyItemInserted(tempBottomSheetList.size - 1)
        speakText(getString(R.string.ka_rupaye_jod_diya, note, amount))
        Toast.makeText(this, "✓ $note - ₹$amount", Toast.LENGTH_SHORT).show()
    }

    private fun parseAndUpdateItem(amount: Int, note: String, item: ExpenseItem) {
        item.note = note
        item.amount = amount
        currentBottomSheetAdapter?.notifyDataSetChanged()
        speakText(getString(R.string.ka_rupaye_update_ho_gaya, note, amount))
        Toast.makeText(this, "✓ ${getString(R.string.update_ho_gaya)}", Toast.LENGTH_SHORT).show()
    }

    private fun handleBottomSheetAction(item: ExpenseItem, action: String) {
        when (action) {
            "EDIT" -> showEditDialogForBottomSheet(item)
            "DELETE" -> {
                tempBottomSheetList.remove(item)
                currentBottomSheetAdapter?.notifyDataSetChanged()
                speakText(getString(R.string.delete_ho_gaya))
                Toast.makeText(this, "✓ ${getString(R.string.delete_ho_gaya)}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var currentBottomSheetExpenseType: ExpenseType? = null

    private fun showEditDialogForBottomSheet(item: ExpenseItem) {
        isEditBottomSheetOpen = true
        speakText(getString(R.string.edit_karne_ke_liye_bolo))
        val editBottomSheet = BottomSheetDialog(this)
        val editView = layoutInflater.inflate(R.layout.bottom_sheet_edit_expense, null)
        editBottomSheet.setContentView(editView)
        editBottomSheet.setCanceledOnTouchOutside(false)

        val tvCurrentNote = editView.findViewById<TextView>(R.id.tvCurrentNote)
        val tvCurrentAmount = editView.findViewById<TextView>(R.id.tvCurrentAmount)
        val btnUpdate = editView.findViewById<MaterialButton>(R.id.btnUpdate)
        val btnCancel = editView.findViewById<MaterialButton>(R.id.btnCancel)
        val fabEditMic = editView.findViewById<ImageView>(R.id.fabEditMic)
        val mainLayout = editView.findViewById<CardView>(R.id.mainLayout)
        tvCurrentNote.text = "Current: ${item.note}"
        tvCurrentAmount.text = "Current: ₹${item.amount}"
        tvCurrentNote.setText(item.note)
        tvCurrentAmount.setText(item.amount.toString())

        val itemType = currentBottomSheetType ?: item.type
        val primaryColor: Int
        val secondaryColor: Int

        if (itemType == ExpenseType.AAVAK) {
            primaryColor = ContextCompat.getColor(this, R.color.greencolor)
            secondaryColor = ContextCompat.getColor(this, R.color.green)
        } else {
            primaryColor = ContextCompat.getColor(this, R.color.tamil_txt)
            secondaryColor = ContextCompat.getColor(this, R.color.tamil_txt)
        }

        fabEditMic.backgroundTintList = ColorStateList.valueOf(primaryColor)
        btnUpdate.backgroundTintList = ColorStateList.valueOf(primaryColor)
        mainLayout.backgroundTintList = ColorStateList.valueOf(primaryColor)

        fabEditMic.setOnClickListener {
            if (isVoiceListening) return@setOnClickListener Toast.makeText(this, getString(R.string.pehle_se_sun_rahe_hain), Toast.LENGTH_SHORT).show()
            if (ttsInitialized && textToSpeech.isSpeaking) textToSpeech.stop()

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    currentEditItem = item
                    isVoiceListening = true
                    editItemLauncher.launch(createVoiceIntent(getString(R.string.naya_bolo)))
                    editBottomSheet.dismiss()
                    isEditBottomSheetOpen = false
                } catch (e: Exception) {
                    isVoiceListening = false
                    Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
                }
            }, 300)
        }

        btnUpdate.setOnClickListener {
            val newNote = tvCurrentNote.text?.toString()?.trim() ?: ""
            val newAmount = tvCurrentAmount.text?.toString()?.trim()?.toIntOrNull() ?: 0
            when {
                newNote.isEmpty() -> Toast.makeText(this, getString(R.string.note_khali_nahi_ho_sakta), Toast.LENGTH_SHORT).show()
                newAmount <= 0 -> Toast.makeText(this, getString(R.string.amount_sahi_nahi_hai), Toast.LENGTH_SHORT).show()
                else -> {
                    item.note = newNote
                    item.amount = newAmount
                    currentBottomSheetAdapter?.notifyDataSetChanged()
                    speakText("$newNote ka $newAmount rupaye update ho gaya")
                    Toast.makeText(this, "✓ ${getString(R.string.update_ho_gaya)}", Toast.LENGTH_SHORT).show()
                    editBottomSheet.dismiss()
                    isEditBottomSheetOpen = false
                }
            }
        }

        btnCancel.setOnClickListener {
            editBottomSheet.dismiss()
            isEditBottomSheetOpen = false
        }
        editBottomSheet.setOnDismissListener { isEditBottomSheetOpen = false }
        editBottomSheet.show()
    }

    // Update showEditDialogForMainList method
    private fun showEditDialogForMainList(item: ExpenseItem, type: ExpenseType) {
        isEditBottomSheetOpen = true
        speakText(getString(R.string.edit_karne_ke_liye_bolo))
        val editBottomSheet = BottomSheetDialog(this)
        val editView = layoutInflater.inflate(R.layout.bottom_sheet_edit_expense, null)
        editBottomSheet.setContentView(editView)
        editBottomSheet.setCanceledOnTouchOutside(false)

        val tvCurrentNote = editView.findViewById<TextView>(R.id.tvCurrentNote)
        val tvCurrentAmount = editView.findViewById<TextView>(R.id.tvCurrentAmount)
        val firstText = editView.findViewById<TextView>(R.id.firstText)
        val etNote = editView.findViewById<EditText>(R.id.etNote)
        val mainLayout = editView.findViewById<androidx.cardview.widget.CardView>(R.id.mainLayout)
        val etAmount = editView.findViewById<EditText>(R.id.etAmount)
        val fabEditMic = editView.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabEditMic)
        val btnCancel = editView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnUpdate = editView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpdate)

        // ✅ NEW: Find TextInputLayouts
//        val tilNote = editView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilNote)
//        val tilAmount = editView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilAmount)

        tvCurrentNote.text = "Current: ${item.note}"
        tvCurrentAmount.text = "Current: ₹${item.amount}"
        etNote.setText(item.note)
        etAmount.setText(item.amount.toString())

        // ✅ NEW: Set colors based on item type
        val primaryColor: Int
        val secondaryColor: Int

        if (type == ExpenseType.AAVAK) {
            // Green colors for Income
            primaryColor = ContextCompat.getColor(this, R.color.greencolor)
            secondaryColor = ContextCompat.getColor(this, R.color.green)
        } else {
            // Red/Tamil colors for Expense
            primaryColor = ContextCompat.getColor(this, R.color.tamil_txt)
            secondaryColor = ContextCompat.getColor(this, R.color.tamil_txt)
        }

        // Apply colors
        fabEditMic.backgroundTintList = ColorStateList.valueOf(primaryColor)
        btnUpdate.backgroundTintList = ColorStateList.valueOf(primaryColor)
        mainLayout.backgroundTintList = ColorStateList.valueOf(primaryColor)
//        tilNote.boxStrokeColor = primaryColor
//        tilNote.hintTextColor = ColorStateList.valueOf(primaryColor)
//        tilAmount.boxStrokeColor = primaryColor
//        tilAmount.hintTextColor = ColorStateList.valueOf(primaryColor)

        fabEditMic.setOnClickListener {
            if (isVoiceListening) return@setOnClickListener Toast.makeText(this, getString(R.string.pehle_se_sun_rahe_hain), Toast.LENGTH_SHORT).show()
            if (ttsInitialized && textToSpeech.isSpeaking) textToSpeech.stop()

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    currentEditItem = item
                    currentEditType = type
                    isVoiceListening = true
                    editMainItemLauncher.launch(createVoiceIntent(getString(R.string.naya_bolo_jaise_diesel_3000)))
                    editBottomSheet.dismiss()
                    isEditBottomSheetOpen = false
                } catch (e: Exception) {
                    isVoiceListening = false
                    Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
                }
            }, 300)
        }

        btnUpdate.setOnClickListener {
            val newNote = etNote.text?.toString()?.trim() ?: ""
            val newAmount = etAmount.text?.toString()?.trim()?.toIntOrNull() ?: 0
            when {
                newNote.isEmpty() -> Toast.makeText(this, getString(R.string.note_khali_nahi_ho_sakta), Toast.LENGTH_SHORT).show()
                newAmount <= 0 -> Toast.makeText(this, getString(R.string.amount_sahi_nahi_hai), Toast.LENGTH_SHORT).show()
                else -> {
                    item.note = newNote
                    item.amount = newAmount
                    if (type == ExpenseType.KHARCHA) expenseAdapter.notifyDataSetChanged()
                    else incomeAdapter.notifyDataSetChanged()
                    updateMunafa()
                    speakText("$newNote ka $newAmount rupaye update ho gaya")
                    Toast.makeText(this, "✓ ${getString(R.string.update_ho_gaya)}", Toast.LENGTH_SHORT).show()
                    editBottomSheet.dismiss()
                    isEditBottomSheetOpen = false
                }
            }
        }

        btnCancel.setOnClickListener {
            editBottomSheet.dismiss()
            isEditBottomSheetOpen = false
        }
        editBottomSheet.setOnDismissListener { isEditBottomSheetOpen = false }
        editBottomSheet.show()
    }

    private fun createVoiceIntent(prompt: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, mapLangCode(langCode))
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, mapLangCode(langCode))
        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN"))
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
        putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }

    private fun saveBottomSheetExpenses() {
        if (tempBottomSheetList.isEmpty()) return Toast.makeText(this, getString(R.string.kuch_to_dalo_pehle), Toast.LENGTH_SHORT).show()

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
        when (action) {
            "EDIT" -> Handler(Looper.getMainLooper()).postDelayed({ showEditDialogForMainList(item, type) }, 100)
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
                speakText(getString(R.string.delete_ho_gaya))
                Toast.makeText(this, getString(R.string.delete_ho_gaya), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun parseAndUpdateMainItem(amount: Int, note: String, item: ExpenseItem, type: ExpenseType) {
        item.note = note
        item.amount = amount
        if (type == ExpenseType.KHARCHA) expenseAdapter.notifyDataSetChanged() else incomeAdapter.notifyDataSetChanged()
        updateMunafa()
        speakText("$note ka $amount rupaye update ho gaya")
        Toast.makeText(this, getString(R.string.update_ho_gaya), Toast.LENGTH_SHORT).show()
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

        // ✅ FIX: Normalize BOTH dates to same format for display
        val displayStartDate = normalizeDisplayDate(getStartDate())
        val displayEndDate = normalizeDisplayDate(endDate)

        // Set values with normalized dates
        tvTruckNumber.text = getTruckNumber()
        tvStartInfo.text = "$displayStartDate - ${getStartPlace()}"
        tvEndInfo.text = "$displayEndDate - ${getEndPlace()}"
        tvTotalExpense.text = "₹$totalExpense"
        tvTotalIncome.text = "₹$totalIncome"
        tvProfit.text = "₹$profit"

        // Set profit color
        if (profit >= 0) {
            tvProfit.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            tvProfit.setTextColor(ContextCompat.getColor(this, R.color.white))
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

    // ✅ NEW: Normalize date format for consistent display
    private fun normalizeDisplayDate(dateString: String): String {
        return try {
            // If already in "dd MMM yyyy" format, return as is
            if (dateString.matches(Regex("\\d{2}\\s\\w{3}\\s\\d{4}"))) {
                return dateString
            }

            // If in "yyyy-MM-dd" format, convert to display format
            if (dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val displayFormat = SimpleDateFormat("yyyy-MM-dd", Locale(langCode))
                val date = apiFormat.parse(dateString)
                return if (date != null) displayFormat.format(date) else dateString
            }

            // Try parsing with current language locale
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale(langCode))
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale(langCode))
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                // Fallback to English locale
                val inputFormatEn = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val date = inputFormatEn.parse(dateString)
                if (date != null) {
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale(langCode))
                    return outputFormat.format(date)
                }
            }

            dateString // Return original if all parsing fails
        } catch (e: Exception) {
            Log.e("DateNormalization", "Error normalizing date: $dateString", e)
            dateString
        }
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
        var dateWasSet = false

        Log.d("EndDateDebug", "showEndDatePicker called - isEditMode: $isEditMode, endDate: '$endDate'")

        // ✅ Try to parse and set the existing end date if in edit mode
        if (isEditMode && endDate.isNotEmpty()) {
            // Try multiple date formats
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),      // API format first
                SimpleDateFormat("yyyy-MM-dd", Locale(langCode)),   // Display format
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            )

            for (format in formats) {
                try {
                    val existingDate = format.parse(endDate)
                    if (existingDate != null) {
                        calendar.time = existingDate
                        dateWasSet = true
                        Log.d("EndDateDebug", "Successfully parsed endDate '$endDate' with format: ${format.toPattern()}")
                        break
                    }
                } catch (e: Exception) {
                    // Continue to next format
                    Log.d("EndDateDebug", "Failed to parse with ${format.toPattern()}: ${e.message}")
                }
            }

            if (!dateWasSet) {
                Log.e("EndDateDebug", "Failed to parse endDate: '$endDate' with all formats")
            }
        } else {
            Log.d("EndDateDebug", "Not setting date - isEditMode: $isEditMode, endDate isEmpty: ${endDate.isEmpty()}")
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        Log.d("EndDateDebug", "DatePicker will show: $day/${month+1}/$year")

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)

                // Format for display
                val displayFormat = SimpleDateFormat("yyyy-MM-dd", Locale(langCode))
                endDate = displayFormat.format(calendar.time)

                Log.d("EndDateDebug", "User selected new date: $endDate")

                speakText(getString(R.string.trip_khatam_ab_data_save_kar_rahe_hain))
                showCompleteDialog()
            },
            year, month, day
        )

        datePickerDialog.setTitle(getString(R.string.trip_khatam_hone_ki_tareekh))
        datePickerDialog.datePicker.calendarViewShown = true

        // ✅ Additional: Set min date to start date if available
        try {
            val startDateStr = getStartDate()
            Log.d("EndDateDebug", "Attempting to set minDate from startDate: '$startDateStr'")

            // Try multiple formats for start date too
            val startFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),      // API format
                SimpleDateFormat("yyyy-MM-dd", Locale(langCode)),   // Display format
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            )

            var startDate: Date? = null
            for (format in startFormats) {
                try {
                    startDate = format.parse(startDateStr)
                    if (startDate != null) {
                        val cal = Calendar.getInstance().apply {
                            time = startDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        datePickerDialog.datePicker.minDate = cal.timeInMillis
                        Log.d("EndDateDebug", "Successfully set minDate using format: ${format.toPattern()}")
                        break
                    }
                } catch (e: Exception) {
                    // Continue to next format
                }
            }

            if (startDate == null) {
                Log.w("EndDateDebug", "Could not parse start date with any format: '$startDateStr'")
            }
        } catch (e: Exception) {
            Log.e("EndDateDebug", "Failed to set minDate", e)
        }

        datePickerDialog.show()
    }

    private fun callApiAndNavigate(endDate: String, driverIncome: String) {
        isCompletingTrip = true
        isTripCompleted = true
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

        // ✅ Convert BOTH start and end dates to "yyyy-MM-dd" format for API
        val apiStartDate = convertToApiDateFormat(getStartDate())
        val apiEndDate = convertToApiDateFormat(endDate)

        val totalDays = calculateTotalDays(getStartDate(), endDate)

        showProgress()

        authViewModel.getTripPdf(
            AddTripRequestModel(
                if (isEditMode) (intent.getStringExtra("TRIP_ID") ?: "") else "",
                getEndPlace(),
                driverIncome,
                apiEndDate,
                route = routeArray,// ✅ Using converted format
                expenseModels,
                incomeModels,
                ownerProfit,
                getStartPlace(),
                apiStartDate,  // ✅ Using converted format - THIS WAS THE FIX
                totalDays,
                totalExpense,
                totalIncome,
                "0",
                getTruckNumber(),
                "",
                "",
                false,
                "",
                lang = com.dadabarbie.TruckTrip.Utils.Prefs[Constants.languageCode, "en"]
            )
        )
    }

    // ✅ Enhanced convertToApiDateFormat with better error handling
    private fun convertToApiDateFormat(dateString: String): String {
        return try {
            // If already in yyyy-MM-dd format, return as is
            if (dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                return dateString
            }

            // Try parsing with current language locale first
            var date: java.util.Date? = null

            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale(langCode))
                date = inputFormat.parse(dateString)
            } catch (e: Exception) {
                // Fallback to English locale if language-specific parsing fails
                try {
                    val inputFormatEn = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                    date = inputFormatEn.parse(dateString)
                } catch (e2: Exception) {
                    Log.e("DateConversion", "Failed to parse date: $dateString", e2)
                }
            }

            // Convert to API format "yyyy-MM-dd"
            if (date != null) {
                val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                return outputFormat.format(date)
            }

            // If parsing completely failed, log error and return current date
            Log.e("DateConversion", "All parsing attempts failed for: $dateString")
            val fallbackFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            fallbackFormat.format(Calendar.getInstance().time)

        } catch (e: Exception) {
            Log.e("DateConversion", "Unexpected error converting date: $dateString", e)
            val fallbackFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            fallbackFormat.format(Calendar.getInstance().time)
        }
    }

    private fun calculateTotalDays(start: String, end: String): String {
        return try {
            // Try parsing with current language code
            var s: java.util.Date? = null
            var e: java.util.Date? = null

            try {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale(langCode))
                s = fmt.parse(start)
                e = fmt.parse(end)
            } catch (ex: Exception) {
                // Fallback to English if parsing fails
                val fmtEn = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                s = fmtEn.parse(start)
                e = fmtEn.parse(end)
            }

            if (s != null && e != null) {
                val diff = (e.time - s.time) / (1000 * 60 * 60 * 24)
                // Add 1 to include both start and end days
                (diff + 1).toString()
            } else {
                Log.e("DateCalculation", "Failed to parse dates - start: $start, end: $end")
                "1"
            }
        } catch (e: Exception) {
            Log.e("DateCalculation", "Error calculating days between $start and $end", e)
            "1"
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
                    // Delete draft from DB since trip is completed
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val db = AppDatabase.getDatabase(applicationContext)
                            db.productsDao().deleteDraftByTripId(tripId)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    isTripCompleted = true
                    com.dadabarbie.TruckTrip.ads.AdMobManager.showInterstitialIfReady(this@ThirdExpenseScreen) {
                        finish()
                    }
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

//    private fun setAllData() {
//        if (isTripCompleted) {
//            Log.d("DraftSave", "Trip completed, skipping draft save")
//            return
//        }
//
//        Log.d("DraftSave", "=== START setAllData ===")
//        Log.d("DraftSave", "tripId: $tripId")
//        Log.d("DraftSave", "isEditMode: $isEditMode")
//        Log.d("DraftSave", "routeArray: $routeArray")
//        Log.d("DraftSave", "routeArray size: ${routeArray.size}")
//
//        // ✅ ENSURE ROUTE IS VALID
//        val validRoute = if (RouteUtils.isValidRoute(routeArray)) {
//            routeArray
//        } else {
//            Log.w("DraftSave", "Invalid route detected, creating simple route")
//            RouteUtils.createSimpleRoute(getStartPlace(), getEndPlace())
//        }
//
//        val routeJsonString = RouteUtils.routeToJson(validRoute)
//        Log.d("DraftSave", "Valid route: $validRoute")
//        Log.d("DraftSave", "Route JSON to save: $routeJsonString")
//
//        GlobalScope.launch(Dispatchers.IO) {
//            try {
//                val db = AppDatabase.getDatabase(applicationContext)
//                val draftDao = db.productsDao()
//
//                // Prepare current lists
//                val currentCreditList = incomeList.map {
//                    Income(
//                        desc = it.note,
//                        amount = it.amount.toString(),
//                        note = it.note,
//                        place = "",
//                        date = ""
//                    )
//                }
//
//                val currentDebitList = expenseList.map {
//                    Expense(
//                        desc = it.note,
//                        amount = it.amount.toString(),
//                        note = it.note,
//                        place = "",
//                        date = "",
//                        type = "Expense",
//                        liters = "",
//                        km = ""
//                    )
//                }
//
//                Log.d("DraftSave", "Credit list size: ${currentCreditList.size}")
//                Log.d("DraftSave", "Debit list size: ${currentDebitList.size}")
//
//                val currentModel = TripDataTestModel(
//                    truckNumber = getTruckNumber(),
//                    srcPlace = getStartPlace(),
//                    destPlace = getEndPlace(),
//                    srcDate = getStartDate(),
//                    destDate = "",
//                    avg = "",
//                    randomNumber = tripId,
//                    driverIncome = "",
//                    modelList1 = currentCreditList,
//                    modelList2 = currentDebitList,
//                    startOdometer = "",
//                    endOdometer = "",
//                    endTripKm = "",
//                    id = "",
//                    route = validRoute,
//                    routeList = routeJsonString,
//                )
//
//                val existing = draftDao.getDraftById(tripId)
//
//                if (existing != null) {
//                    Log.d("DraftSave", "✅ Found existing draft, updating...")
//
//                    // Always update if draft exists
//                    draftDao.update(
//                        tripId,
//                        currentModel.truckNumber,
//                        currentModel.srcPlace,
//                        currentModel.destPlace,
//                        currentModel.srcDate,
//                        currentModel.destDate,
//                        currentModel.avg,
//                        Gson().toJson(currentModel.modelList1),
//                        Gson().toJson(currentModel.modelList2),
//                        routeJsonString,
//                        updatedAt = System.currentTimeMillis()
//                    )
//                    Log.d("DraftSave", "✅ Draft updated successfully")
//                } else {
//                    Log.d("DraftSave", "📝 No existing draft, creating new one...")
//
//                    // ✅ FIX: Only check if editing a COMPLETED trip
//                    var shouldInsert = true
//
//                    if (isEditMode) {
//                        Log.d("DraftSave", "Edit mode detected, checking if data changed from original...")
//
//                        // Get original data from Constants
//                        val originalCreditList = Constants.creditList.map {
//                            Income(
//                                desc = it.note ?: "",
//                                amount = (it.amount?.toIntOrNull() ?: 0).toString(),
//                                note = it.note ?: "",
//                                place = "",
//                                date = ""
//                            )
//                        }
//
//                        val originalDebitList = Constants.debitList.map {
//                            Expense(
//                                desc = it.note ?: "",
//                                amount = (it.amount?.toIntOrNull() ?: 0).toString(),
//                                note = it.note ?: "",
//                                place = "",
//                                date = "",
//                                type = "Expense",
//                                liters = "",
//                                km = ""
//                            )
//                        }
//
//                        Log.d("DraftSave", "Original credit size: ${originalCreditList.size}")
//                        Log.d("DraftSave", "Original debit size: ${originalDebitList.size}")
//                        Log.d("DraftSave", "Original route: $originalRoute")
//                        Log.d("DraftSave", "Current route: $validRoute")
//
//                        // ✅ Check each field for changes
//                        val truckChanged = currentModel.truckNumber != (originalTruckNumber ?: getTruckNumber())
//                        val srcChanged = currentModel.srcPlace != (originalStartPlace ?: getStartPlace())
//                        val destChanged = currentModel.destPlace != (originalEndPlace ?: getEndPlace())
//                        val dateChanged = currentModel.srcDate != (originalStartDate ?: getStartDate())
//                        val creditChanged = currentCreditList != originalCreditList
//                        val debitChanged = currentDebitList != originalDebitList
//                        val routeChanged = validRoute.toList() != originalRoute.toList()
//
//                        Log.d("DraftSave", "Change detection:")
//                        Log.d("DraftSave", "  Truck changed: $truckChanged")
//                        Log.d("DraftSave", "  Source changed: $srcChanged")
//                        Log.d("DraftSave", "  Dest changed: $destChanged")
//                        Log.d("DraftSave", "  Date changed: $dateChanged")
//                        Log.d("DraftSave", "  Credit changed: $creditChanged")
//                        Log.d("DraftSave", "  Debit changed: $debitChanged")
//                        Log.d("DraftSave", "  Route changed: $routeChanged")
//
//                        // ✅ Only skip insert if NOTHING changed
//                        if (!truckChanged && !srcChanged && !destChanged && !dateChanged &&
//                            !creditChanged && !debitChanged && !routeChanged) {
//                            shouldInsert = false
//                            Log.d("DraftSave", "⏭️ No changes detected, skipping insert")
//                        } else {
//                            Log.d("DraftSave", "✅ Changes detected, will insert draft")
//                        }
//                    } else {
//                        Log.d("DraftSave", "Not in edit mode, will insert draft")
//                    }
//
//                    if (shouldInsert) {
//                        Log.d("DraftSave", "📝 Inserting new draft...")
//
//                        val product = Products(
//                            truckNumber = currentModel.truckNumber,
//                            srcPlace = currentModel.srcPlace,
//                            destPlace = currentModel.destPlace,
//                            srcDate = currentModel.srcDate,
//                            destDate = currentModel.destDate,
//                            avg = currentModel.avg,
//                            randomNumber = tripId,
//                            modelList1 = Gson().toJson(currentModel.modelList1),
//                            modelList2 = Gson().toJson(currentModel.modelList2),
//                            routeJson = routeJsonString,
//                            updatedAt = System.currentTimeMillis()
//                        )
//
//                        draftDao.insert(product)
//                        Log.d("DraftSave", "✅ Draft inserted successfully")
//
//                        // ✅ Verify immediately after insert
//                        val inserted = draftDao.getDraftById(tripId)
//                        if (inserted != null) {
//                            Log.d("DraftSave", "✅ VERIFY: Draft confirmed in DB")
//                            Log.d("DraftSave", "  - ID: ${inserted.id}")
//                            Log.d("DraftSave", "  - Route JSON: ${inserted.routeJson}")
//                        } else {
//                            Log.e("DraftSave", "❌ VERIFY FAILED: Draft not found after insert!")
//                        }
//                    } else {
//                        Log.d("DraftSave", "⏭️ Skipped insert (no changes)")
//                    }
//                }
//
//                Log.d("DraftSave", "=== END setAllData ===")
//
//            } catch (e: Exception) {
//                Log.e("DraftSave", "❌ ERROR in setAllData: ${e.message}", e)
//                e.printStackTrace()
//            }
//        }
//    }

    private suspend fun getAllData(context: Context): List<TripDataTestModel> {
        val database = AppDatabase.getDatabase(context)
        val tripDataDao = database.productsDao()
        val entities = tripDataDao.getAllProducts()
        return entities.map {
            convertToModel(it)
        }
    }

    // ✅ COMPLETE FIX FOR ThirdExpenseScreen.kt - Replace setAllData() method

    private fun setAllData() {
        if (isCompletingTrip || isTripCompleted) {
            Log.d("DraftSave", "Draft save blocked (trip completing)")
            return
        }

        Log.d("DraftSave", "=== START setAllData ===")
        Log.d("DraftSave", "tripId: $tripId")
        Log.d("DraftSave", "isEditMode: $isEditMode")
        Log.d("DraftSave", "routeArray received: $routeArray")
        Log.d("DraftSave", "routeArray size: ${routeArray.size}")

        // ✅ ENSURE ROUTE IS VALID - with detailed logging
        val validRoute = if (RouteUtils.isValidRoute(routeArray)) {
            Log.d("DraftSave", "✅ routeArray is valid")
            routeArray
        } else {
            Log.w("DraftSave", "⚠️ Invalid route detected, creating simple route")
            RouteUtils.createSimpleRoute(getStartPlace(), getEndPlace())
        }

        val routeJsonString = RouteUtils.routeToJson(validRoute)
        Log.d("DraftSave", "Valid route to save: $validRoute")
        Log.d("DraftSave", "Route JSON string length: ${routeJsonString.length}")
        Log.d("DraftSave", "Route JSON content: $routeJsonString")

        // ✅ Verify JSON is not empty
        if (routeJsonString.isEmpty() || routeJsonString == "[]") {
            Log.e("DraftSave", "❌ ERROR: Route JSON is empty or just []")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val draftDao = db.productsDao()

                // Prepare current lists
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

                Log.d("DraftSave", "Credit list size: ${currentCreditList.size}")
                Log.d("DraftSave", "Debit list size: ${currentDebitList.size}")

                val existing = draftDao.getDraftById(tripId)
                Log.d("DraftSave", "Existing draft found: ${existing != null}")

                if (existing != null) {
                    Log.d("DraftSave", "📝 UPDATING existing draft...")
                    Log.d("DraftSave", "Old route JSON: ${existing.routeJson}")
                    Log.d("DraftSave", "New route JSON: $routeJsonString")

                    // ✅ CRITICAL FIX: Ensure parameters match DAO method signature EXACTLY
                    val updateResult = draftDao.update(
                        randomNumber = tripId,  // ✅ Named parameters for clarity
                        truckNumber = getTruckNumber(),
                        srcPlace = getStartPlace(),
                        destPlace = getEndPlace(),
                        srcDate = DateUtils.uiToDbDate(getStartDate(), langCode),
                        destDate = DateUtils.uiToDbDate(endDate, langCode),
                        avg = "",
                        modelList1 = Gson().toJson(currentCreditList),
                        modelList2 = Gson().toJson(currentDebitList),
                        routeJson = routeJsonString,  // ✅ This must NOT be empty
                        updatedAt = System.currentTimeMillis()
                    )

                    Log.d("DraftSave", "✅ Update executed")

                    // ✅ VERIFY immediately after update
                    withContext(Dispatchers.Main) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            lifecycleScope.launch(Dispatchers.IO) {
                                val verified = draftDao.getDraftById(tripId)
                                if (verified != null) {
                                    Log.d("DraftSave", "✅ VERIFICATION: Draft updated successfully")
                                    Log.d("DraftSave", "  - Saved route JSON: ${verified.routeJson}")
                                    Log.d("DraftSave", "  - Route JSON length: ${verified.routeJson?.length}")

                                    if (verified.routeJson == routeJsonString) {
                                        Log.d("DraftSave", "✅✅✅ Route JSON MATCHES!")
                                    } else {
                                        Log.e("DraftSave", "❌ Route JSON MISMATCH!")
                                        Log.e("DraftSave", "Expected: $routeJsonString")
                                        Log.e("DraftSave", "Got: ${verified.routeJson}")
                                    }
                                } else {
                                    Log.e("DraftSave", "❌ VERIFICATION FAILED: Draft not found!")
                                }
                            }
                        }, 200)
                    }
                } else {
                    Log.d("DraftSave", "📝 INSERTING new draft...")

                    var shouldInsert = true

                    if (isEditMode) {
                        Log.d("DraftSave", "Edit mode: checking for changes...")

                        val originalCreditList = Constants.creditList.map {
                            Income(
                                desc = it.note ?: "",
                                amount = (it.amount?.toIntOrNull() ?: 0).toString(),
                                note = it.note ?: "",
                                place = "",
                                date = ""
                            )
                        }

                        val originalDebitList = Constants.debitList.map {
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

                        val truckChanged = getTruckNumber() != (originalTruckNumber ?: getTruckNumber())
                        val srcChanged = getStartPlace() != (originalStartPlace ?: getStartPlace())
                        val destChanged = getEndPlace() != (originalEndPlace ?: getEndPlace())
                        val dateChanged = getStartDate() != (originalStartDate ?: getStartDate())
                        val creditChanged = currentCreditList != originalCreditList
                        val debitChanged = currentDebitList != originalDebitList
                        val routeChanged = validRoute.toList() != originalRoute.toList()

                        Log.d("DraftSave", "Changes: truck=$truckChanged, src=$srcChanged, dest=$destChanged, date=$dateChanged, credit=$creditChanged, debit=$debitChanged, route=$routeChanged")

                        shouldInsert = truckChanged || srcChanged || destChanged || dateChanged || creditChanged || debitChanged || routeChanged
                    }

                    if (shouldInsert) {
                        Log.d("DraftSave", "Inserting with route JSON: $routeJsonString")

                        val product = Products(
                            truckNumber = getTruckNumber(),
                            srcPlace = getStartPlace(),
                            destPlace = getEndPlace(),
                            srcDate = getStartDate(),
                            destDate = "",
                            avg = "",
                            randomNumber = tripId,
                            modelList1 = Gson().toJson(currentCreditList),
                            modelList2 = Gson().toJson(currentDebitList),
                            routeJson = routeJsonString,  // ✅ Must not be empty
                            updatedAt = System.currentTimeMillis()
                        )

                        draftDao.insert(product)
                        Log.d("DraftSave", "✅ Insert executed")

                        // ✅ Verify insert
                        withContext(Dispatchers.Main) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val inserted = draftDao.getDraftById(tripId)
                                    if (inserted != null) {
                                        Log.d("DraftSave", "✅ VERIFY: Insert successful")
                                        Log.d("DraftSave", "  - Route JSON: ${inserted.routeJson}")
                                    } else {
                                        Log.e("DraftSave", "❌ VERIFY: Insert failed!")
                                    }
                                }
                            }, 200)
                        }
                    } else {
                        Log.d("DraftSave", "⏭️ Skipping insert (no changes)")
                    }
                }

                Log.d("DraftSave", "=== END setAllData ===")

            } catch (e: Exception) {
                Log.e("DraftSave", "❌ EXCEPTION in setAllData", e)
                e.printStackTrace()
            }
        }
    }

    // ✅ Also update convertToModel to ensure proper parsing
    private fun convertToModel(entity: Products): TripDataTestModel {
        val gson = Gson()

        Log.d("DraftLoad", "=== convertToModel ===")
        Log.d("DraftLoad", "Entity ID: ${entity.id}")
        Log.d("DraftLoad", "Entity randomNumber: ${entity.randomNumber}")
        Log.d("DraftLoad", "Entity routeJson: ${entity.routeJson}")

        // ✅ PROPER ROUTE PARSING
        val parsedRoute = try {
            if (entity.routeJson.isNullOrEmpty()) {
                Log.w("DraftLoad", "routeJson is null or empty, using empty list")
                arrayListOf()
            } else {
                val parsed = RouteUtils.parseRouteFromJson(entity.routeJson)
                Log.d("DraftLoad", "Successfully parsed route: $parsed")
                ArrayList(parsed)
            }
        } catch (e: Exception) {
            Log.e("DraftLoad", "Error parsing route from JSON: ${e.message}", e)
            Log.e("DraftLoad", "RouteJson was: ${entity.routeJson}")
            // Fallback: create simple route
            arrayListOf(entity.srcPlace, entity.destPlace)
        }

        Log.d("DraftLoad", "Final parsed route: $parsedRoute")

        val creditList: List<Income> = try {
            if (!entity.modelList1.isNullOrEmpty()) {
                gson.fromJson(entity.modelList1, object : TypeToken<List<Income>>() {}.type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val debitList: List<Expense> = try {
            if (!entity.modelList2.isNullOrEmpty()) {
                gson.fromJson(entity.modelList2, object : TypeToken<List<Expense>>() {}.type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return TripDataTestModel(
            truckNumber = entity.truckNumber ?: "",
            srcPlace = entity.srcPlace ?: "",
            destPlace = entity.destPlace ?: "",
            srcDate = entity.srcDate ?: "",
            destDate = entity.destDate ?: "",
            avg = entity.avg ?: "",
            randomNumber = entity.randomNumber ?: "",
            modelList1 = creditList,
            modelList2 = debitList,
            id = entity.id.toString(),
            routeList = entity.routeJson ?: "",
            route = parsedRoute
        )
    }
    private fun verifyRouteSaved() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val draft = db.productsDao().getDraftById(tripId)

                withContext(Dispatchers.Main) {
                    Log.d("DraftVerify", "=== VERIFICATION ===")
                    if (draft != null) {
                        Log.d("DraftVerify", "✅ Draft found for tripId: $tripId")
                        Log.d("DraftVerify", "Database ID: ${draft.id}")
                        Log.d("DraftVerify", "Truck: ${draft.truckNumber}")
                        Log.d("DraftVerify", "Saved route JSON: ${draft.routeJson}")

                        val savedRoute = RouteUtils.parseRouteFromJson(draft.routeJson ?: "")
                        Log.d("DraftVerify", "Parsed saved route: $savedRoute")
                        Log.d("DraftVerify", "Current routeArray: $routeArray")

                        if (savedRoute == routeArray.toList()) {
                            Log.d("DraftVerify", "✅✅✅ Route saved CORRECTLY!")
                        } else {
                            Log.e("DraftVerify", "❌ Route MISMATCH!")
                            Log.e("DraftVerify", "Expected: $routeArray")
                            Log.e("DraftVerify", "Got: $savedRoute")
                        }
                    } else {
                        Log.e("DraftVerify", "❌❌❌ No draft found for tripId: $tripId")

                        // Check all drafts
                        val allDrafts = db.productsDao().getAllProducts()
                        Log.e("DraftVerify", "Total drafts in DB: ${allDrafts.size}")
                        allDrafts.forEach {
                            Log.d("DraftVerify", "  Draft: ${it.randomNumber} - ${it.truckNumber}")
                        }
                    }
                    Log.d("DraftVerify", "=== END VERIFICATION ===")
                }
            } catch (e: Exception) {
                Log.e("DraftVerify", "Error verifying route: ${e.message}", e)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        setAllData()

        // ✅ Verify route was saved (remove this after testing)
        Handler(Looper.getMainLooper()).postDelayed({
            verifyRouteSaved()
        }, 500)
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        Constants.refreshApiGet(Event(-1))
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}
