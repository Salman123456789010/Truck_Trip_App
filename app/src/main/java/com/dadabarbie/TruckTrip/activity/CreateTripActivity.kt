package com.dadabarbie.TruckTrip.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.RouteUtils
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityCreateTripBinding
import com.dadabarbie.TruckTrip.model.addTrip.AddTripRequestModel
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.dadabarbie.TruckTrip.ads.AdMobManager
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vasyerp.cafvd.room.dao.ProductsDao
import com.vasyerp.cafvd.room.model.Products
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class CreateTripActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateTripBinding
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var productsDao: ProductsDao

    private var randomNumber: String = ""
    private var startTripDateStr: String = ""
    private var endTripDateStr: String = ""
    private var paymentMode: String = "Cash"
    private var paymentStatus: String = "Pending"
    private var currentIsOdometerMode: Boolean = true
    private var isSavingDraft: Boolean = false
    private var isPdfGenerating: Boolean = false

    private val viaPlaceRows = mutableListOf<EditText>()
    private val extraIncomeRows = mutableListOf<Pair<EditText, EditText>>()
    private val extraExpenseRows = mutableListOf<Pair<EditText, EditText>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productsDao = AppDatabase.getDatabase(applicationContext).productsDao()
        randomNumber = "draft_${System.currentTimeMillis()}_${UUID.randomUUID()}"

        initDefaults()
        setupListeners()
        setupTextWatchers()
        observeViewModel()
        checkAndPopulateDraftOrEditData()
    }

    private fun initDefaults() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val today = sdf.format(Date())
        startTripDateStr = today
        endTripDateStr = today
        binding.tvTripDate.text = startTripDateStr
        binding.tvEndTripDate.text = endTripDateStr

        val passedTruck = intent.getStringExtra("truck_number") ?: intent.getStringExtra("TRUCK_NUMBER")
        if (!passedTruck.isNullOrEmpty()) {
            binding.etTruckNumber.setText(passedTruck)
        }

        updateKmModeUI()
    }

    private fun checkAndPopulateDraftOrEditData() {
        val draftId = intent.getStringExtra("TRIP_ID") ?: intent.getStringExtra("id")
        val passedTruck = intent.getStringExtra("TRUCK_NUMBER") ?: intent.getStringExtra("truckNumber") ?: intent.getStringExtra("truck_number")
        val passedSrc = intent.getStringExtra("START_PLACE") ?: intent.getStringExtra("sourceName") ?: intent.getStringExtra("SOURCE_PLACE")
        val passedDest = intent.getStringExtra("END_PLACE") ?: intent.getStringExtra("destinationName") ?: intent.getStringExtra("DESTINATION_PLACE")
        val passedStartDate = intent.getStringExtra("START_DATE") ?: intent.getStringExtra("startingDate")
        val passedEndDate = intent.getStringExtra("END_DATE") ?: intent.getStringExtra("endingDate")
        val passedDriverIncome = intent.getStringExtra("DRIVER_INCOME") ?: intent.getStringExtra("driverAvak")
        val passedStartOdo = intent.getStringExtra("START_ODOMETER") ?: intent.getStringExtra("startOdometer")
        val passedEndOdo = intent.getStringExtra("END_ODOMETER") ?: intent.getStringExtra("endOdometer")
        val passedEndKm = intent.getStringExtra("END_KM") ?: intent.getStringExtra("endTripKm")
        val passedRoute = intent.getStringArrayListExtra("ROUTE_ARRAY")

        if (!draftId.isNullOrEmpty()) {
            randomNumber = draftId
            lifecycleScope.launch(Dispatchers.IO) {
                val draftEntity = productsDao.getDraftById(draftId)
                withContext(Dispatchers.Main) {
                    if (draftEntity != null) {
                        populateFromProductEntity(draftEntity)
                    } else {
                        populateFromDirectValues(
                            passedTruck, passedSrc, passedDest, passedStartDate, passedEndDate,
                            passedDriverIncome, passedStartOdo, passedEndOdo, passedEndKm, passedRoute
                        )
                    }
                }
            }
        } else {
            populateFromDirectValues(
                passedTruck, passedSrc, passedDest, passedStartDate, passedEndDate,
                passedDriverIncome, passedStartOdo, passedEndOdo, passedEndKm, passedRoute
            )
        }
    }

    private fun populateFromProductEntity(entity: Products) {
        if (!entity.truckNumber.isNullOrEmpty()) binding.etTruckNumber.setText(entity.truckNumber)
        if (!entity.srcPlace.isNullOrEmpty()) binding.etFromPlace.setText(entity.srcPlace)
        if (!entity.destPlace.isNullOrEmpty()) binding.etToPlace.setText(entity.destPlace)
        if (!entity.srcDate.isNullOrEmpty()) {
            startTripDateStr = entity.srcDate
            binding.tvTripDate.text = startTripDateStr
        }
        if (!entity.destDate.isNullOrEmpty()) {
            endTripDateStr = entity.destDate
            binding.tvEndTripDate.text = endTripDateStr
        }

        // Parse route Json
        if (!entity.routeJson.isNullOrEmpty()) {
            val parsedRoute = RouteUtils.parseRouteFromJson(entity.routeJson)
            val viaPlaces = parsedRoute.filter { it != entity.srcPlace && it != entity.destPlace }
            for (via in viaPlaces) {
                addViaPlaceRowWithValue(via)
            }
        }

        // Parse credit list
        if (!entity.modelList1.isNullOrEmpty()) {
            try {
                val incomes: List<Income> = Gson().fromJson(entity.modelList1, object : TypeToken<List<Income>>() {}.type) ?: emptyList()
                for (inc in incomes) {
                    when (inc.desc) {
                        "Freight" -> binding.etTotalFreight.setText(inc.amount)
                        "Advance Received" -> binding.etAdvanceAmount.setText(inc.amount)
                        else -> addExtraIncomeRowWithValue(inc.amount, inc.desc)
                    }
                }
            } catch (e: Exception) {
                Log.e("CreateTripActivity", "Error parsing income list: ${e.message}")
            }
        }

        // Parse debit list
        if (!entity.modelList2.isNullOrEmpty()) {
            try {
                val expenses: List<Expense> = Gson().fromJson(entity.modelList2, object : TypeToken<List<Expense>>() {}.type) ?: emptyList()
                for (exp in expenses) {
                    when (exp.desc) {
                        "Diesel" -> {
                            binding.etDieselAmount.setText(exp.amount)
                            if (!exp.liters.isNullOrEmpty()) binding.etDieselLiters.setText(exp.liters)
                            if (!exp.km.isNullOrEmpty()) binding.etStartOdometer.setText(exp.km)
                        }
                        "Toll / FastTag", "Toll" -> binding.etTollAmount.setText(exp.amount)
                        "Driver Bhatta", "Bhatta" -> binding.etBhattaAmount.setText(exp.amount)
                        "Hamali / Loading", "Hamali" -> binding.etHamaliAmount.setText(exp.amount)
                        "Other Expense" -> {
                            binding.etOtherAmount.setText(exp.amount)
                            if (!exp.note.isNullOrEmpty()) binding.etOtherNote.setText(exp.note)
                        }
                        else -> addExtraExpenseRowWithValue(exp.amount, if (exp.note.isNullOrEmpty()) exp.desc else exp.note)
                    }
                }
            } catch (e: Exception) {
                Log.e("CreateTripActivity", "Error parsing expense list: ${e.message}")
            }
        }

        populateFromConstants()
        recalculateLive()
    }

    private fun populateFromDirectValues(
        truck: String?, src: String?, dest: String?, sDate: String?, eDate: String?,
        dIncome: String?, startOdo: String?, endOdo: String?, endKm: String?, routeArray: ArrayList<String>?
    ) {
        if (!truck.isNullOrEmpty()) binding.etTruckNumber.setText(truck)
        if (!src.isNullOrEmpty()) binding.etFromPlace.setText(src)
        if (!dest.isNullOrEmpty()) binding.etToPlace.setText(dest)
        if (!sDate.isNullOrEmpty()) {
            startTripDateStr = sDate
            binding.tvTripDate.text = startTripDateStr
        }
        if (!eDate.isNullOrEmpty()) {
            endTripDateStr = eDate
            binding.tvEndTripDate.text = endTripDateStr
        }
        if (!dIncome.isNullOrEmpty()) binding.etDriverIncome.setText(dIncome)
        if (!startOdo.isNullOrEmpty()) binding.etStartOdometer.setText(startOdo)
        if (!endOdo.isNullOrEmpty()) {
            currentIsOdometerMode = true
            binding.etEndKm.setText(endOdo)
            updateKmModeUI()
        } else if (!endKm.isNullOrEmpty()) {
            currentIsOdometerMode = false
            binding.etEndKm.setText(endKm)
            updateKmModeUI()
        }

        if (!routeArray.isNullOrEmpty()) {
            val viaPlaces = routeArray.filter { it != src && it != dest }
            for (via in viaPlaces) {
                addViaPlaceRowWithValue(via)
            }
        }

        populateFromConstants()
        recalculateLive()
    }

    private fun populateFromConstants() {
        if (Constants.creditList.isNotEmpty()) {
            for (inc in Constants.creditList) {
                when (inc.desc) {
                    "Freight" -> if (binding.etTotalFreight.text.isNullOrEmpty()) binding.etTotalFreight.setText(inc.amount)
                    "Advance Received" -> if (binding.etAdvanceAmount.text.isNullOrEmpty()) binding.etAdvanceAmount.setText(inc.amount)
                    else -> {
                        val exists = extraIncomeRows.any { it.first.text.toString() == inc.amount }
                        if (!exists) addExtraIncomeRowWithValue(inc.amount, inc.desc)
                    }
                }
            }
        }
        if (Constants.debitList.isNotEmpty()) {
            for (exp in Constants.debitList) {
                when (exp.desc) {
                    "Diesel" -> {
                        if (binding.etDieselAmount.text.isNullOrEmpty()) binding.etDieselAmount.setText(exp.amount)
                        if (binding.etDieselLiters.text.isNullOrEmpty() && !exp.liters.isNullOrEmpty()) binding.etDieselLiters.setText(exp.liters)
                    }
                    "Toll / FastTag", "Toll" -> if (binding.etTollAmount.text.isNullOrEmpty()) binding.etTollAmount.setText(exp.amount)
                    "Driver Bhatta", "Bhatta" -> if (binding.etBhattaAmount.text.isNullOrEmpty()) binding.etBhattaAmount.setText(exp.amount)
                    "Hamali / Loading", "Hamali" -> if (binding.etHamaliAmount.text.isNullOrEmpty()) binding.etHamaliAmount.setText(exp.amount)
                    "Other Expense" -> {
                        if (binding.etOtherAmount.text.isNullOrEmpty()) binding.etOtherAmount.setText(exp.amount)
                        if (binding.etOtherNote.text.isNullOrEmpty() && !exp.note.isNullOrEmpty()) binding.etOtherNote.setText(exp.note)
                    }
                    else -> {
                        val exists = extraExpenseRows.any { it.first.text.toString() == exp.amount }
                        if (!exists) addExtraExpenseRowWithValue(exp.amount, if (exp.note.isNullOrEmpty()) exp.desc else exp.note)
                    }
                }
            }
        }
    }

    private fun addViaPlaceRowWithValue(value: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4.dpToPx(), 0, 4.dpToPx())
        }

        val viaEdit = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 48.dpToPx(), 1f).apply {
                marginEnd = 8.dpToPx()
            }
            background = resources.getDrawable(R.drawable.bg_rounded_edittext, theme)
            hint = getString(R.string.via_place_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dpToPx(), 0, 12.dpToPx(), 0)
            setTextColor(resources.getColor(R.color.text_main, theme))
            textSize = 14f
            setText(value)
        }

        val deleteBtn = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(36.dpToPx(), 48.dpToPx())
            gravity = Gravity.CENTER
            text = "✕"
            setTextColor(resources.getColor(R.color.state_error, theme))
            textSize = 16f
            background = resources.getDrawable(R.drawable.bg_chip_unselected, theme)
            isClickable = true
            isFocusable = true
        }

        deleteBtn.setOnClickListener {
            binding.containerViaPlaces.removeView(layout)
            viaPlaceRows.remove(viaEdit)
        }

        layout.addView(viaEdit)
        layout.addView(deleteBtn)

        binding.containerViaPlaces.addView(layout)
        viaPlaceRows.add(viaEdit)
    }

    private fun addExtraIncomeRowWithValue(amount: String, note: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4.dpToPx(), 0, 4.dpToPx())
        }

        val amtEdit = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 48.dpToPx(), 1f).apply {
                marginEnd = 6.dpToPx()
            }
            background = resources.getDrawable(R.drawable.bg_rounded_edittext, theme)
            hint = getString(R.string.income_amount_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dpToPx(), 0, 12.dpToPx(), 0)
            setTextColor(resources.getColor(R.color.text_main, theme))
            textSize = 14f
            setText(amount)
        }

        val noteEdit = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 48.dpToPx(), 1f).apply {
                marginStart = 6.dpToPx()
                marginEnd = 6.dpToPx()
            }
            background = resources.getDrawable(R.drawable.bg_rounded_edittext, theme)
            hint = getString(R.string.income_note_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dpToPx(), 0, 12.dpToPx(), 0)
            setTextColor(resources.getColor(R.color.text_main, theme))
            textSize = 14f
            setText(note)
        }

        val deleteBtn = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(36.dpToPx(), 48.dpToPx())
            gravity = Gravity.CENTER
            text = "✕"
            setTextColor(resources.getColor(R.color.state_error, theme))
            textSize = 16f
            background = resources.getDrawable(R.drawable.bg_chip_unselected, theme)
            isClickable = true
            isFocusable = true
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                recalculateLive()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        amtEdit.addTextChangedListener(watcher)

        deleteBtn.setOnClickListener {
            binding.containerExtraIncome.removeView(layout)
            extraIncomeRows.remove(Pair(amtEdit, noteEdit))
            recalculateLive()
        }

        layout.addView(amtEdit)
        layout.addView(noteEdit)
        layout.addView(deleteBtn)

        binding.containerExtraIncome.addView(layout)
        extraIncomeRows.add(Pair(amtEdit, noteEdit))
    }

    private fun addExtraExpenseRowWithValue(amount: String, note: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4.dpToPx(), 0, 4.dpToPx())
        }

        val amtEdit = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 48.dpToPx(), 1f).apply {
                marginEnd = 6.dpToPx()
            }
            background = resources.getDrawable(R.drawable.bg_rounded_edittext, theme)
            hint = getString(R.string.other_amount_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dpToPx(), 0, 12.dpToPx(), 0)
            setTextColor(resources.getColor(R.color.text_main, theme))
            textSize = 14f
            setText(amount)
        }

        val noteEdit = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 48.dpToPx(), 1f).apply {
                marginStart = 6.dpToPx()
                marginEnd = 6.dpToPx()
            }
            background = resources.getDrawable(R.drawable.bg_rounded_edittext, theme)
            hint = getString(R.string.other_note_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12.dpToPx(), 0, 12.dpToPx(), 0)
            setTextColor(resources.getColor(R.color.text_main, theme))
            textSize = 14f
            setText(note)
        }

        val deleteBtn = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(36.dpToPx(), 48.dpToPx())
            gravity = Gravity.CENTER
            text = "✕"
            setTextColor(resources.getColor(R.color.state_error, theme))
            textSize = 16f
            background = resources.getDrawable(R.drawable.bg_chip_unselected, theme)
            isClickable = true
            isFocusable = true
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                recalculateLive()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        amtEdit.addTextChangedListener(watcher)

        deleteBtn.setOnClickListener {
            binding.containerExtraExpenses.removeView(layout)
            extraExpenseRows.remove(Pair(amtEdit, noteEdit))
            recalculateLive()
        }

        layout.addView(amtEdit)
        layout.addView(noteEdit)
        layout.addView(deleteBtn)

        binding.containerExtraExpenses.addView(layout)
        extraExpenseRows.add(Pair(amtEdit, noteEdit))
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSwapRoute.setOnClickListener {
            val from = binding.etFromPlace.text.toString()
            val to = binding.etToPlace.text.toString()
            binding.etFromPlace.setText(to)
            binding.etToPlace.setText(from)
        }

        binding.tvAddViaPlace.setOnClickListener {
            addViaPlaceRow()
        }

        binding.btnSelectDate.setOnClickListener {
            showStartDatePickerDialog()
        }

        binding.btnSelectEndDate.setOnClickListener {
            showEndDatePickerDialog()
        }

        binding.rgEndKmInputType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbEndOdometer -> {
                    currentIsOdometerMode = true
                    updateKmModeUI()
                }
                R.id.rbEndKm -> {
                    currentIsOdometerMode = false
                    updateKmModeUI()
                }
            }
        }

        binding.chipFreight10k.setOnClickListener { addFreightAmount(10000.0) }
        binding.chipFreight25k.setOnClickListener { addFreightAmount(25000.0) }
        binding.chipFreight50k.setOnClickListener { addFreightAmount(50000.0) }
        binding.chipFreight1L.setOnClickListener { addFreightAmount(100000.0) }
        binding.chipFreight2L.setOnClickListener { addFreightAmount(200000.0) }

        binding.chipCash.setOnClickListener {
            paymentMode = "Cash"
            binding.chipCash.background = resources.getDrawable(R.drawable.bg_chip_selected, theme)
            binding.chipCash.setTextColor(resources.getColor(R.color.white, theme))
            binding.chipOnline.background = resources.getDrawable(R.drawable.bg_chip_unselected, theme)
            binding.chipOnline.setTextColor(resources.getColor(R.color.text_muted, theme))
        }

        binding.chipOnline.setOnClickListener {
            paymentMode = "Online"
            binding.chipOnline.background = resources.getDrawable(R.drawable.bg_chip_selected, theme)
            binding.chipOnline.setTextColor(resources.getColor(R.color.white, theme))
            binding.chipCash.background = resources.getDrawable(R.drawable.bg_chip_unselected, theme)
            binding.chipCash.setTextColor(resources.getColor(R.color.text_muted, theme))
        }

        binding.chipPaid.setOnClickListener {
            paymentStatus = "Paid"
            binding.chipPaid.background = resources.getDrawable(R.drawable.bg_chip_selected, theme)
            binding.chipPaid.setTextColor(resources.getColor(R.color.white, theme))
            binding.chipPending.background = resources.getDrawable(R.drawable.bg_chip_unselected, theme)
            binding.chipPending.setTextColor(resources.getColor(R.color.text_muted, theme))
        }

        binding.chipPending.setOnClickListener {
            paymentStatus = "Pending"
            binding.chipPending.background = resources.getDrawable(R.drawable.bg_chip_selected, theme)
            binding.chipPending.setTextColor(resources.getColor(R.color.white, theme))
            binding.chipPaid.background = resources.getDrawable(R.drawable.bg_chip_unselected, theme)
            binding.chipPaid.setTextColor(resources.getColor(R.color.text_muted, theme))
        }

        binding.tvAddExtraIncome.setOnClickListener {
            addExtraIncomeRow()
        }

        binding.btnAddMoreExpense.setOnClickListener {
            addExtraExpenseRow()
        }

        AdMobManager.preloadInterstitial(this)

        binding.btnSaveTrip.setOnClickListener {
            if (validateInputs()) {
                showProgress(getString(R.string.data_save_ho_raha_hai))
                AdMobManager.showInterstitialIfReady(this, force = true) {
                    saveTripToRoom {
                        Toast.makeText(this, "Trip saved to drafts successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        binding.btnSaveAndPdf.setOnClickListener {
            if (validateInputs()) {
                showProgress(getString(R.string.data_save_ho_raha_hai))
                AdMobManager.showInterstitialIfReady(this, force = true) {
                    saveTripToRoom {
                        triggerDirectPdfGeneration()
                    }
                }
            }
        }
    }


    private fun updateKmModeUI() {
        if (currentIsOdometerMode) {
            binding.rbEndOdometer.isChecked = true
            binding.rbEndKm.isChecked = false
            binding.rbEndOdometer.background = resources.getDrawable(R.drawable.bg_chip_selected, theme)
            binding.rbEndOdometer.setTextColor(resources.getColor(R.color.white, theme))
            binding.rbEndKm.background = resources.getDrawable(R.drawable.bg_chip_unselected, theme)
            binding.rbEndKm.setTextColor(resources.getColor(R.color.text_muted, theme))
            binding.etEndKm.hint = getString(R.string.end_odometer_reading)
        } else {
            binding.rbEndKm.isChecked = true
            binding.rbEndOdometer.isChecked = false
            binding.rbEndKm.background = resources.getDrawable(R.drawable.bg_chip_selected, theme)
            binding.rbEndKm.setTextColor(resources.getColor(R.color.white, theme))
            binding.rbEndOdometer.background = resources.getDrawable(R.drawable.bg_chip_unselected, theme)
            binding.rbEndOdometer.setTextColor(resources.getColor(R.color.text_muted, theme))
            binding.etEndKm.hint = getString(R.string.manual_km_traveled)
        }
    }

    private fun addFreightAmount(amount: Double) {
        val currentStr = binding.etTotalFreight.text.toString().trim()
        val current = currentStr.toDoubleOrNull() ?: 0.0
        val updated = current + amount
        binding.etTotalFreight.setText(if (updated % 1.0 == 0.0) updated.toLong().toString() else updated.toString())
    }

    private fun showStartDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(y, m, d)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            startTripDateStr = sdf.format(selectedCalendar.time)
            binding.tvTripDate.text = startTripDateStr
            if (endTripDateStr < startTripDateStr) {
                endTripDateStr = startTripDateStr
                binding.tvEndTripDate.text = endTripDateStr
            }
        }, year, month, day).show()
    }

    private fun showEndDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(this, { _, y, m, d ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(y, m, d)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            endTripDateStr = sdf.format(selectedCalendar.time)
            binding.tvEndTripDate.text = endTripDateStr
        }, year, month, day)

        parseDateSafely(startTripDateStr)?.let { startDate ->
            dialog.datePicker.minDate = startDate.time
        }
        dialog.show()
    }

    private fun parseDateSafely(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(dateStr)
        } catch (_: Exception) {
            null
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                recalculateLive()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etTotalFreight.addTextChangedListener(watcher)
        binding.etAdvanceAmount.addTextChangedListener(watcher)
        binding.etDieselAmount.addTextChangedListener(watcher)
        binding.etTollAmount.addTextChangedListener(watcher)
        binding.etBhattaAmount.addTextChangedListener(watcher)
        binding.etHamaliAmount.addTextChangedListener(watcher)
        binding.etOtherAmount.addTextChangedListener(watcher)
    }

    private fun addViaPlaceRow() {
        addViaPlaceRowWithValue("")
    }

    private fun addExtraIncomeRow() {
        addExtraIncomeRowWithValue("", "")
    }

    private fun addExtraExpenseRow() {
        addExtraExpenseRowWithValue("", "")
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun recalculateLive() {
        val freight = binding.etTotalFreight.text.toString().toDoubleOrNull() ?: 0.0
        val advance = binding.etAdvanceAmount.text.toString().toDoubleOrNull() ?: 0.0

        var extraIncSum = 0.0
        for ((amtEdit, _) in extraIncomeRows) {
            extraIncSum += amtEdit.text.toString().toDoubleOrNull() ?: 0.0
        }

        val diesel = binding.etDieselAmount.text.toString().toDoubleOrNull() ?: 0.0
        val toll = binding.etTollAmount.text.toString().toDoubleOrNull() ?: 0.0
        val bhatta = binding.etBhattaAmount.text.toString().toDoubleOrNull() ?: 0.0
        val hamali = binding.etHamaliAmount.text.toString().toDoubleOrNull() ?: 0.0
        val other = binding.etOtherAmount.text.toString().toDoubleOrNull() ?: 0.0

        var extraExpSum = 0.0
        for ((amtEdit, _) in extraExpenseRows) {
            extraExpSum += amtEdit.text.toString().toDoubleOrNull() ?: 0.0
        }

        val totalExpense = diesel + toll + bhatta + hamali + other + extraExpSum
        val totalIncome = freight + extraIncSum
        val netProfit = totalIncome - totalExpense
        val balanceDue = totalIncome - advance

        binding.tvTotalIncome.text = "₹${totalIncome.toInt()}"
        binding.tvTotalExpense.text = "₹${totalExpense.toInt()}"
        binding.tvNetProfit.text = "₹${netProfit.toInt()}"
        binding.tvBalanceDue.text = "₹${balanceDue.toInt()}"
    }

    private fun validateInputs(): Boolean {
        val truck = binding.etTruckNumber.text.toString().trim()
        if (truck.isEmpty()) {
            Toast.makeText(this, "Please enter Truck Number", Toast.LENGTH_SHORT).show()
            binding.etTruckNumber.requestFocus()
            return false
        }
        return true
    }

    private fun calculateTotalDays(startDateStr: String, endDateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val start = sdf.parse(startDateStr)
            val end = sdf.parse(endDateStr)

            if (start != null && end != null) {
                val diffInMillis = end.time - start.time
                val days = TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1
                maxOf(1L, days).toString()
            } else {
                "1"
            }
        } catch (e: Exception) {
            "1"
        }
    }

    private fun triggerDirectPdfGeneration() {
        val endDate = if (endTripDateStr.isNotEmpty()) endTripDateStr else startTripDateStr

        val startParsed = parseDateSafely(startTripDateStr)
        val endParsed = parseDateSafely(endDate)
        if (startParsed != null && endParsed != null && endParsed.before(startParsed)) {
            Toast.makeText(this, getString(R.string.end_date_cannot_be_before_start_date), Toast.LENGTH_SHORT).show()
            binding.tvEndTripDate.requestFocus()
            return
        }

        val driverIncome = binding.etDriverIncome.text.toString().trim().ifEmpty { "0" }
        val endKmValue = binding.etEndKm.text.toString().trim()
        val totalDays = calculateTotalDays(startTripDateStr, endDate)

        val endOdometer = if (currentIsOdometerMode) endKmValue else ""
        val endKm = if (!currentIsOdometerMode) endKmValue else ""

        // Check 10-trip limit for free users
        lifecycleScope.launch(Dispatchers.IO) {
            val localCount = try { productsDao.getCount() } catch (_: Exception) { 0 }
            withContext(Dispatchers.Main) {
                if (!com.dadabarbie.TruckTrip.billing.SubscriptionManager.canSaveTrip(localCount)) {
                    com.dadabarbie.TruckTrip.dialog.PremiumPaywallDialog.show(
                        this@CreateTripActivity,
                        com.dadabarbie.TruckTrip.billing.SubscriptionManager.PremiumFeature.UNLIMITED_TRIPS
                    ) {
                        generatePdf(
                            endDate = endDate,
                            driverIncome = driverIncome,
                            totalDays = totalDays,
                            endOdometer = endOdometer,
                            endKm = endKm,
                            isOdometerMode = currentIsOdometerMode
                        )
                    }
                } else {
                    generatePdf(
                        endDate = endDate,
                        driverIncome = driverIncome,
                        totalDays = totalDays,
                        endOdometer = endOdometer,
                        endKm = endKm,
                        isOdometerMode = currentIsOdometerMode
                    )
                }
            }
        }
    }

    private fun buildRouteList(): ArrayList<String> {
        val src = binding.etFromPlace.text.toString().trim()
        val dest = binding.etToPlace.text.toString().trim()
        val viaPlaces = viaPlaceRows.map { it.text.toString().trim() }.filter { it.isNotEmpty() }

        val routeList = arrayListOf<String>()
        if (src.isNotEmpty()) routeList.add(src)
        routeList.addAll(viaPlaces)
        if (dest.isNotEmpty()) routeList.add(dest)

        return if (RouteUtils.isValidRoute(routeList)) {
            routeList
        } else {
            RouteUtils.createSimpleRoute(src, dest)
        }
    }

    private fun buildCreditList(): List<Income> {
        val creditList = mutableListOf<Income>()
        val party = binding.etPartyName.text.toString().trim()
        val eway = binding.etEwayBill.text.toString().trim()
        val src = binding.etFromPlace.text.toString().trim()

        val freight = binding.etTotalFreight.text.toString().trim()
        if (freight.isNotEmpty() && (freight.toDoubleOrNull() ?: 0.0) > 0) {
            val noteParts = mutableListOf<String>()
            if (party.isNotEmpty()) noteParts.add("${getString(R.string.party_label)}: $party")
            noteParts.add("${getString(R.string.payment_mode_label)}: $paymentMode")
            noteParts.add("${getString(R.string.payment_status_label)}: $paymentStatus")
            if (eway.isNotEmpty()) noteParts.add("POD/E-Way: $eway")

            creditList.add(
                Income(
                    id = System.currentTimeMillis(),
                    desc = getString(R.string.total_freight_label),
                    amount = freight,
                    note = noteParts.joinToString(" | "),
                    place = src,
                    date = startTripDateStr
                )
            )
        }

        val advance = binding.etAdvanceAmount.text.toString().trim()
        if (advance.isNotEmpty() && (advance.toDoubleOrNull() ?: 0.0) > 0) {
            creditList.add(
                Income(
                    id = System.currentTimeMillis() + 1,
                    desc = getString(R.string.advance_received),
                    amount = advance,
                    note = "${getString(R.string.payment_mode_label)}: $paymentMode",
                    place = src,
                    date = startTripDateStr
                )
            )
        }

        var idx = 2L
        for ((amtEdit, noteEdit) in extraIncomeRows) {
            val amt = amtEdit.text.toString().trim()
            val note = noteEdit.text.toString().trim()
            if (amt.isNotEmpty() && (amt.toDoubleOrNull() ?: 0.0) > 0) {
                creditList.add(
                    Income(
                        id = System.currentTimeMillis() + idx,
                        desc = if (note.isNotEmpty()) note else getString(R.string.income_label),
                        amount = amt,
                        note = "${getString(R.string.payment_mode_label)}: $paymentMode | ${getString(R.string.payment_status_label)}: $paymentStatus",
                        place = src,
                        date = startTripDateStr
                    )
                )
                idx++
            }
        }

        return creditList
    }

    private fun buildDebitList(): List<Expense> {
        val debitList = mutableListOf<Expense>()
        val dieselAmt = binding.etDieselAmount.text.toString().trim()
        val dieselLiters = binding.etDieselLiters.text.toString().trim()
        val dieselPump = binding.etDieselPump.text.toString().trim()

        if (dieselAmt.isNotEmpty() && (dieselAmt.toDoubleOrNull() ?: 0.0) > 0) {
            val noteParts = mutableListOf<String>()
            if (dieselLiters.isNotEmpty()) noteParts.add("$dieselLiters ${getString(R.string.liters_suffix)}")
            if (dieselPump.isNotEmpty()) noteParts.add("${getString(R.string.pump_label)}: $dieselPump")

            debitList.add(
                Expense(
                    id = System.currentTimeMillis(),
                    desc = getString(R.string.diesel_charges_label),
                    amount = dieselAmt,
                    note = noteParts.joinToString(" | "),
                    place = "",
                    date = startTripDateStr,
                    type = "Diesel",
                    liters = dieselLiters,
                    km = binding.etStartOdometer.text.toString().trim(),
                    isOdometerMode = true
                )
            )
        }

        val toll = binding.etTollAmount.text.toString().trim()
        if (toll.isNotEmpty() && (toll.toDoubleOrNull() ?: 0.0) > 0) {
            debitList.add(Expense(id = System.currentTimeMillis() + 2, desc = getString(R.string.toll_charges_label), amount = toll, note = "", place = "", date = startTripDateStr, type = "Toll"))
        }

        val bhatta = binding.etBhattaAmount.text.toString().trim()
        if (bhatta.isNotEmpty() && (bhatta.toDoubleOrNull() ?: 0.0) > 0) {
            debitList.add(Expense(id = System.currentTimeMillis() + 3, desc = getString(R.string.driver_bhatta_label), amount = bhatta, note = "", place = "", date = startTripDateStr, type = "Bhatta"))
        }

        val hamali = binding.etHamaliAmount.text.toString().trim()
        if (hamali.isNotEmpty() && (hamali.toDoubleOrNull() ?: 0.0) > 0) {
            debitList.add(Expense(id = System.currentTimeMillis() + 4, desc = getString(R.string.hamali_loading_label), amount = hamali, note = "", place = "", date = startTripDateStr, type = "Hamali"))
        }

        val other = binding.etOtherAmount.text.toString().trim()
        val otherNote = binding.etOtherNote.text.toString().trim()
        if (other.isNotEmpty() && (other.toDoubleOrNull() ?: 0.0) > 0) {
            debitList.add(Expense(id = System.currentTimeMillis() + 5, desc = getString(R.string.other_expenses_label), amount = other, note = otherNote, place = "", date = startTripDateStr, type = "Other"))
        }

        var idx = 6L
        for ((amtEdit, noteEdit) in extraExpenseRows) {
            val amt = amtEdit.text.toString().trim()
            val note = noteEdit.text.toString().trim()
            if (amt.isNotEmpty() && (amt.toDoubleOrNull() ?: 0.0) > 0) {
                debitList.add(Expense(id = System.currentTimeMillis() + idx, desc = if (note.isNotEmpty()) note else getString(R.string.expense_label), amount = amt, note = note, place = "", date = startTripDateStr, type = "Other"))
                idx++
            }
        }

        return debitList
    }

    private fun saveTripToRoom(onComplete: (() -> Unit)? = null) {
        if (isSavingDraft) return
        isSavingDraft = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val creditList = buildCreditList()
                val debitList = buildDebitList()
                val src = binding.etFromPlace.text.toString().trim()
                val dest = binding.etToPlace.text.toString().trim()
                val truck = binding.etTruckNumber.text.toString().trim()
                val validRoute = buildRouteList()

                val product = Products(
                    truckNumber = truck,
                    srcPlace = src,
                    destPlace = dest,
                    srcDate = startTripDateStr,
                    destDate = if (endTripDateStr.isNotEmpty()) endTripDateStr else startTripDateStr,
                    avg = "",
                    randomNumber = randomNumber,
                    modelList1 = Gson().toJson(creditList),
                    modelList2 = Gson().toJson(debitList),
                    routeJson = RouteUtils.routeToJson(validRoute),
                    updatedAt = System.currentTimeMillis()
                )

                val existing = productsDao.getDraftById(randomNumber)
                if (existing != null) {
                    productsDao.update(
                        randomNumber = product.randomNumber,
                        truckNumber = product.truckNumber,
                        srcPlace = product.srcPlace,
                        destPlace = product.destPlace,
                        srcDate = product.srcDate,
                        destDate = product.destDate,
                        avg = product.avg,
                        modelList1 = product.modelList1,
                        modelList2 = product.modelList2,
                        routeJson = product.routeJson,
                        updatedAt = product.updatedAt
                    )
                } else {
                    productsDao.insert(product)
                }

                withContext(Dispatchers.Main) {
                    isSavingDraft = false
                    Constants.refreshApiGet(Event(1))
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                Log.e("CreateTripActivity", "Error saving trip to Room", e)
                withContext(Dispatchers.Main) {
                    isSavingDraft = false
                    Toast.makeText(this@CreateTripActivity, "Failed to save draft: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generatePdf(
        endDate: String,
        driverIncome: String,
        totalDays: String,
        endOdometer: String,
        endKm: String,
        isOdometerMode: Boolean
    ) {
        val creditList = buildCreditList()
        val debitList = buildDebitList()
        val src = binding.etFromPlace.text.toString().trim()
        val dest = binding.etToPlace.text.toString().trim()
        val truck = binding.etTruckNumber.text.toString().trim()
        val startOdo = binding.etStartOdometer.text.toString().trim()
        val validRoute = buildRouteList()

        var totalInc = 0.0
        for (c in creditList) {
            totalInc += c.amount.toDoubleOrNull() ?: 0.0
        }
        var totalExp = 0.0
        for (d in debitList) {
            totalExp += d.amount.toDoubleOrNull() ?: 0.0
        }
        val totalProfit = totalInc - totalExp

        isPdfGenerating = true
        showProgress(getString(R.string.generating_pdf_progress))

        val currentLang = com.dadabarbie.TruckTrip.Utils.Prefs[Constants.languageCode, "en"]

        authViewModel.getTripPdf(
            AddTripRequestModel(
                id = randomNumber,
                destination = dest,
                driver_income = driverIncome,
                end_date = endDate,
                route = validRoute,
                expense = debitList,
                income = creditList,
                owner_profit = totalProfit.toInt().toString(),
                source = src,
                start_date = startTripDateStr,
                total_days = totalDays,
                total_expense = totalExp.toInt().toString(),
                total_income = totalInc.toInt().toString(),
                truck_average = "0",
                truck_no = truck,
                startOdometer = startOdo,
                endOdometer = endOdometer,
                isOdometer = isOdometerMode,
                endKm = endKm,
                lang = currentLang
            )
        )
    }

    private fun observeViewModel() {
        authViewModel.downloadCompleted.observe(this) { filePath ->
            Constants.dismissProgress()
            if (!filePath.isNullOrEmpty() && isPdfGenerating) {
                isPdfGenerating = false
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        productsDao.deleteByRandomNumber(randomNumber)
                    } catch (e: Exception) {
                        Log.e("CreateTripActivity", "Error deleting draft after PDF: ${e.message}")
                    }
                    withContext(Dispatchers.Main) {
                        openPdfFile(filePath)
                        Constants.refreshApiGet(Event(1))
                        finish()
                    }
                }
            }
        }

        authViewModel.pdfErrorEvent.observe(this) { event ->
            val errorMsg = event.getContentIfNotHandled()
            if (errorMsg != null && isPdfGenerating) {
                isPdfGenerating = false
                Constants.dismissProgress()
                showServerIssueDraftDialog()
            }
        }
    }

    private fun showServerIssueDraftDialog() {
        if (isFinishing || isDestroyed) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.server_issue_title))
            .setMessage(getString(R.string.saved_to_draft_server_issue_desc))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.go_to_drafts)) { dialog, _ ->
                dialog.dismiss()
                val isNormalUser = com.dadabarbie.TruckTrip.Utils.Prefs[Constants.appMode, ""] == "A"
                val intent = if (isNormalUser) {
                    Intent(this, SimpleDraftActivity::class.java)
                } else {
                    Intent(this, DraftActivity::class.java)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.close)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }


    private fun openPdfFile(data: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val file = File(data)
            val apkURI: Uri = FileProvider.getUriForFile(
                this,
                packageName,
                file
            )
            intent.setDataAndType(apkURI, "application/pdf")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("CreateTripActivity", "Error opening PDF file: ${e.message}")
            Toast.makeText(this, "Could not open PDF file", Toast.LENGTH_SHORT).show()
        }
    }
}
