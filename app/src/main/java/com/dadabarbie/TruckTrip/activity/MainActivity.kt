package com.dadabarbie.TruckTrip.activity

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.util.Pair
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.creditList
import com.dadabarbie.TruckTrip.Utils.Constants.debitList
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.Constants.tripName
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.adapter.CreditAdapter
import com.dadabarbie.TruckTrip.adapter.DebitAdapter
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityMainBinding
import com.dadabarbie.TruckTrip.diologFragment.AddExpenseBottomSheetFragment
import com.dadabarbie.TruckTrip.diologFragment.AddFuelBottomSheetFragment
import com.dadabarbie.TruckTrip.diologFragment.AddIncomeBottomSheetFragment
import com.dadabarbie.TruckTrip.diologFragment.AddTaskDialogFragment
import com.dadabarbie.TruckTrip.diologFragment.AvgDialogFragment
import com.dadabarbie.TruckTrip.diologFragment.DeleteDialogFragment
import com.dadabarbie.TruckTrip.diologFragment.TripDialogFragment
import com.dadabarbie.TruckTrip.diologFragment.TripEndDialogFragment
import com.dadabarbie.TruckTrip.model.addTrip.AddTripRequestModel
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.dadabarbie.TruckTrip.room.model.TripDataTestModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vasyerp.cafvd.room.model.Products
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Random
import kotlin.toString
import com.dadabarbie.TruckTrip.Utils.RouteUtils
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils

import com.dadabarbie.TruckTrip.views.CoachMarkHelper
import com.dadabarbie.TruckTrip.views.CoachMarkView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

// ----------------------
// Extension: single-click to prevent rapid double taps
// ----------------------
// Place this top-level function in the file (already here).
fun View.setOnSingleClickListener(delay: Long = 800L, onClick: (View) -> Unit) {
    // disable view for short time to prevent multiple rapid clicks
    this.setOnClickListener { v ->
        if (!v.isEnabled) return@setOnClickListener
        v.isEnabled = false
        try {
            onClick(v)
        } finally {
            v.postDelayed({ v.isEnabled = true }, delay)
        }
    }
}

@AndroidEntryPoint
class MainActivity : BaseActivity(), View.OnClickListener,
    CreditAdapter.EditCreditClickListner, CreditAdapter.DeleteCreditClickLitsner,
    DebitAdapter.EditClickListner, DebitAdapter.DeleteClickListner {
    lateinit var binding: ActivityMainBinding
    lateinit var addTaskDialogFragment: AddTaskDialogFragment
    lateinit var addExpenseBottomSheetFragment: AddExpenseBottomSheetFragment
    lateinit var addFuelBottomSheetFragment: AddFuelBottomSheetFragment
    lateinit var addIncomeBottomSheetFragment: AddIncomeBottomSheetFragment
    lateinit var avgDialogFragment: AvgDialogFragment
    lateinit var tripDialogFragment: TripDialogFragment
    lateinit var tripEndDialogFragment: TripEndDialogFragment
    lateinit var creditAdapter: CreditAdapter
    private var draftFlowClosed = false
    private var initialCreditSnapshot = ""
    private var initialDebitSnapshot = ""
    private val authViewModel: AuthViewModel by viewModels()
    lateinit var debitAdapter: DebitAdapter
    private var totalamount = 0
    var draftFlag = false
    var databaseAddFlag = false
    var databaseAddFlagName = false
    private var shouldDeleteDraftOnComplete = false
    private lateinit var materialDateBuilder: MaterialDatePicker.Builder<Pair<Long, Long>>
    private lateinit var materialDatePicker: MaterialDatePicker<*>
    private val testList: ArrayList<TripDataTestModel> = arrayListOf()
    private var courseList: ArrayList<TripDataTestModel> = arrayListOf()
    lateinit var deleteDialogFragment: DeleteDialogFragment
    var totalDebitAmount = 0
    var randomNumber = "0"
    var showingFlag = 0
    var startTripDate: String? = ""
    var routeArray: ArrayList<String> =arrayListOf()
    var truckNumberGiven: String? = ""
    var driverIncome = "0"
    var totalDaysTrip = "0"
    private var isSavingDraft = false  // NEW: Prevent multiple saves
    private var saveJob: Job? = null
    var id = "0"
    private var isDraftDeletedAfterPdf = false
    var startOdometerReading: String = ""  // NEW: Start odometer from trip dialog
       // NEW: End odometer from trip end dialog
    var endManualKm: String = ""           // NEW: Manual KM from trip end dialog
    var endKmIsOdometerMode: Boolean = true

// In MainActivity.kt, add these variables at the top with other declarations:

    var endTripDate: String? = ""           // Add this
    var driverIncomeAmount: String? = "0"
    var startOdometer: String? = "0"
    var endOdometer: String? = "0"
    var endKM: String? = "0"
    var isOdometer: Boolean? = true
    companion object {
        // Yaha apna Rewarded Ad Unit ID daalo (second ID)
        private const val REWARDED_AD_UNIT_ID =
            "ca-app-pub-8808039515208362/3557131853"
    }

    private var rewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            // 35 (android - 15)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        }
//        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)
        setLanguage()
        initViews()
        setOnclickListner()
        setObserver()
        MobileAds.initialize(this) {}
        loadRewardedAd()

        // Show coach marks after a short delay to ensure all views are ready

    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }
    private fun checkListChanged(): Boolean {
        val currentCredit = Gson().toJson(creditList)
        val currentDebit = Gson().toJson(debitList)

        return currentCredit != initialCreditSnapshot ||
                currentDebit != initialDebitSnapshot
    }

    private fun showAdThen(onFinished: () -> Unit) {
        val ad = rewardedAd

        if (ad == null) {
            // Ad ready nahi hai → direct PDF open kar do
            onFinished()
            // background me next ad load kar lo
            loadRewardedAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // user ne cross/close kiya
                rewardedAd = null
                loadRewardedAd()      // agla ad ready karo
                onFinished()          // ab PDF open karo
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd()
                onFinished()          // fail ho gaya → fir bhi PDF dikha do
            }
        }

        ad.show(this) { rewardItem ->
            // Agar tum reward logic use karna chaho to yaha amount etc milega
            // val amount = rewardItem.amount
            // val type = rewardItem.type
        }
    }

    private fun initViews() {
        showingFlag = intent.getIntExtra("flag", 1)

        if (showingFlag == 1) {
            // New trip
            randomNumberGenerate()
            callDialog("", "", "", "", "", "", "", "","")
        } else {
            // Edit existing trip
            val truckNumber = intent.getStringExtra("truckNumber") ?: ""
            val srcPlace = intent.getStringExtra("sourceName") ?: ""
            val driverAvak = intent.getStringExtra("driverAvak") ?: "0"
            val destPlace = intent.getStringExtra("destinationName") ?: ""
            val startDate = intent.getStringExtra("startingDate") ?: ""
            val endDate = intent.getStringExtra("endingDate") ?: ""
            val start_Odometer = intent.getStringExtra("startOdometer") ?: ""
            val edn_Odometer = intent.getStringExtra("endOdometer") ?: ""
            val end_Km = intent.getStringExtra("endKm") ?: ""
            id = intent.getStringExtra("id") ?: "0"
            val is_Odometer = intent.getBooleanExtra("isOdometer",true) ?: true

            // Safely get route array from intent
            routeArray = intent.getStringArrayListExtra("ROUTE_ARRAY") ?: arrayListOf()

            Log.d("edn_Odometer", "initViews: ${edn_Odometer}")

            startTripDate = startDate
            truckNumberGiven = truckNumber
            endTripDate = endDate
            driverIncomeAmount = driverAvak
            startOdometer=start_Odometer
            endOdometer=edn_Odometer
            endManualKm=end_Km
            endKmIsOdometerMode=is_Odometer


            Log.d("randomNumberValue", "initViews: ${randomNumber}")
            Log.d("randomNumberValue", "initViews: ${id}")
            if (id.isNotEmpty() && id != "0") {
                Log.d("randomNumberValue", "initViews: ${id}")
                randomNumber = id
            } else {
                randomNumberGenerate()
            }
            Log.d("randomNumberValue", "initViews: ${randomNumber}")
            // NEW: Parse route from Intent using RouteUtils
            val routeJson = intent.getStringExtra("routeJson")
            routeList = if (!routeJson.isNullOrEmpty()) {
                RouteUtils.parseRouteFromJson(routeJson)
            } else {
                // Fallback: try ROUTE_ARRAY or create simple route
                if (routeArray.isNotEmpty()) {
                    ArrayList(routeArray)
                } else {
                    RouteUtils.createSimpleRoute(srcPlace, destPlace)
                }
            }

            Log.d("MainActivityList", "Editing trip ID: $id with ${routeList.size} route places")

            // Handle income and expense data
            if (showingFlag == 2 && id.isNotEmpty()) {
                randomNumber = id ?: randomNumber
                databaseAddFlag = true

                if (creditList.isEmpty() && debitList.isEmpty()) {
                    Log.d("MainActivity", "Lists empty, parsing from Intent JSON")

                    // Parse income
                    intent.getStringExtra("incomeJson")?.let { json ->
                        try {
                            val parsed: List<Income> = Gson().fromJson(
                                json,
                                object : TypeToken<List<Income>>() {}.type
                            )
                            creditList.clear()
                            creditList.addAll(parsed)
                            Log.d("MainActivity", "Parsed ${parsed.size} income entries")
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error parsing income JSON: ${e.message}", e)
                        }
                    }

                    // Parse expense
                    intent.getStringExtra("expenseJson")?.let { json ->
                        try {
                            val parsed: List<Expense> = Gson().fromJson(
                                json,
                                object : TypeToken<List<Expense>>() {}.type
                            )
                            debitList.clear()
                            debitList.addAll(parsed)
                            Log.d("MainActivity", "Parsed ${parsed.size} expense entries")
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error parsing expense JSON: ${e.message}", e)
                        }
                    }
                }
            }
            if (showingFlag == 3 && id.isNotEmpty()) {
                shouldDeleteDraftOnComplete = true // Mark for deletion after completion
                // ... rest of your code
            }

            // Set UI values with safe route handling
            binding.truckNumber.text = truckNumber

            // Display route information
            if (RouteUtils.isValidRoute(routeList)) {
                binding.srcName.text = RouteUtils.getSourcePlace(routeList, srcPlace)
                binding.dest.text = RouteUtils.getDestinationPlace(routeList, destPlace)
                if (routeList.isNotEmpty()) {
                   binding.tvRoutes.visibility = View.VISIBLE

                    val routeText = buildString {
                        append(routeList[0])

                       routeList.drop(1).forEach { routeItem ->
                            append(" → ")
                            append(routeItem)
                        }
                    }

                    binding.tvRoutes.text = routeText
                } else {
                    binding.tvRoutes.visibility = View.GONE
                }

            } else {
                // Fallback to simple source/destination
                binding.srcName.text = srcPlace
                binding.dest.text = destPlace
                routeList = RouteUtils.createSimpleRoute(srcPlace, destPlace)
            }

            binding.srcDate.text = startDate

            // Update adapters with loaded data
            if (creditList.isNotEmpty()) {
                creditIncomeUpdate()
            }

            if (debitList.isNotEmpty()) {
                debitIncomeUpdate()
            }
        }
        takeInitialListSnapshot();
        initAdapter()
    }

    private fun takeInitialListSnapshot() {
        initialCreditSnapshot = Gson().toJson(creditList)
        initialDebitSnapshot = Gson().toJson(debitList)
    }
//    private fun initAdapter() {
//        creditAdapter = CreditAdapter(this, this, this)
//        binding.creditAmount.adapter = creditAdapter
//        creditAdapter.submitList(creditList)
//        creditAdapter.notifyItemRangeChanged(0, creditList.size)
//
//        debitAdapter = DebitAdapter(this, this, this)
//        binding.debitAmount.adapter = debitAdapter
//        debitAdapter.submitList(debitList)
//        debitAdapter.notifyItemRangeChanged(0, debitList.size)
//
//        binding.totalIncome.text = getString(R.string.ruppe) + totalamount.toString()
//        binding.totalIncomeNew.text = totalamount.toString()
//        binding.totalExpanse.text = getString(R.string.ruppe) + totalDebitAmount.toString()
//        binding.totalExpanseNew.text = totalDebitAmount.toString()
//    }

    private fun randomNumberGenerate() {
        val rand = Random()
        randomNumber = rand.nextInt(1000).toString()
    }

    var routeList: ArrayList<String> = arrayListOf()  // NEW: Store complete route

    // Update the callDialog function to pass route:
    private fun callDialog(
        date: String, startingPlace: String, endingPlace: String,
        driverIncome: String, truckNumber: String, truckAvg: String,
        totalDays: String, update: String, startOdometer: String? = ""
    ) {
        databaseAddFlag = true

        // OLD CODE (remove this):
        // tripDialogFragment = TripDialogFragment(
        //     date, startingPlace, endingPlace, driverIncome,
        //     truckNumber, truckAvg, totalDays, update,
        //     startOdometer!!,
        //     routeList
        // )

        // NEW CODE (use this):
        tripDialogFragment = TripDialogFragment.newInstance(
            date = date,
            startingPlace = startingPlace,
            endingPlace = endingPlace,
            driverIncome = driverIncome,
            truckNumber = truckNumber,
            truckAvg = truckAvg,
            totalDays = totalDays,
            update = update,
            startOdometer = startOdometer ?: "",  // Safe null handling
            existingRoute = routeList
        )

        tripDialogFragment.show(supportFragmentManager, "")
        tripDialogFragment.isCancelable = false
    }

    // Update getDataFill function to accept route:
    fun getDataFill(
        truckNumber: String,
        srcPlaceValue: String,
        destPlaceValue: String,
        startDate: String,
        endDate: String,
        truckAvg: String = "",
        driverTripAvak: String,
        totalDays: String = "0",
        startOdometer_: String = "",
        route: ArrayList<String> = arrayListOf()
    ) {
        try {
            // Only mark as changed if data actually changed
            val hasChanges = binding.truckNumber.text.toString() != truckNumber ||
                    binding.srcName.text.toString() != srcPlaceValue ||
                    binding.dest.text.toString() != destPlaceValue ||
                    binding.srcDate.text.toString() != startDate ||
                    startOdometer != startOdometer_ ||
                    !RouteUtils.areRoutesEqual(routeList, route)

            if (hasChanges) {
                hasPendingChanges = true
                databaseAddFlag = false
                Log.d("MainActivity", "Data changed, marking for save")
            }

            binding.truckNumber.text = truckNumber

            if (RouteUtils.isValidRoute(route)) {
                routeList = ArrayList(route)
                binding.srcName.text = RouteUtils.getSourcePlace(route, srcPlaceValue)
                binding.dest.text = RouteUtils.getDestinationPlace(route, destPlaceValue)

                if (routeList.isNotEmpty()) {
                    binding.tvRoutes.visibility = View.VISIBLE
                    val routeText = buildString {
                        append(routeList[0])
                        routeList.drop(1).forEach { routeItem ->
                            append(" → ")
                            append(routeItem)
                        }
                    }
                    binding.tvRoutes.text = routeText
                } else {
                    binding.tvRoutes.visibility = View.GONE
                }
            } else {
                binding.srcName.text = srcPlaceValue
                binding.dest.text = destPlaceValue
                routeList = RouteUtils.createSimpleRoute(srcPlaceValue, destPlaceValue)
                binding.tvRoutes.visibility = View.GONE
            }

            binding.srcDate.text = startDate
            startTripDate = startDate
            truckNumberGiven = truckNumber
            totalDaysTrip = totalDays
            startOdometer = startOdometer_

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in getDataFill: ${e.message}", e)
        }
    }


    private var saveAttemptCount = 0

    private fun debugSaveAttempt() {
        saveAttemptCount++
        Log.d("MainActivity", "===== Save Attempt #$saveAttemptCount =====")
        Log.d("MainActivity", "isSavingDraft: $isSavingDraft")
        Log.d("MainActivity", "hasPendingChanges: $hasPendingChanges")
        Log.d("MainActivity", "draftFlowClosed: $draftFlowClosed")
        Log.d("MainActivity", "isDraftDeletedAfterPdf: $isDraftDeletedAfterPdf")
        Log.d("MainActivity", "randomNumber: $randomNumber")
        Log.d("MainActivity", "Stack trace:")
        Thread.currentThread().stackTrace.take(10).forEach {
            Log.d("MainActivity", "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})")
        }
    }

    // Update delete methods to mark changes
// ==========================================
// FIXED DELETE AND UPDATE METHODS
// ==========================================

    fun creditDeleteHissab(position: Int) {
        if (position !in creditList.indices) return

        creditList.removeAt(position)

        val newList = creditList.toList()
        creditAdapter.submitList(newList) {
            updateAllTotals()  // ✅ Use unified method
        }

        hasPendingChanges = true
    }


    fun debitDeleteHissab(position: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                if (position < 0 || position >= debitList.size) {
                    Log.e("MainActivity", "Invalid debit position: $position")
                    return@launch
                }

                val removedItem = debitList.removeAt(position)
                Log.d("MainActivity", "Removed debit: ${removedItem.desc}")

                val newList = debitList.toMutableList()

                debitAdapter.submitList(newList) {
                    updateAllTotals()  // ✅ Use unified method
                    calculateFinalTripAverage()
                }

                hasPendingChanges = true
                databaseAddFlag = false

            } catch (e: Exception) {
                Log.e("MainActivity", "Error deleting debit: ${e.message}", e)
            }
        }
    }


    fun creditDataUpdate() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                Log.d("MainActivity", "creditDataUpdate called, list size: ${creditList.size}")

                // Create new list reference for DiffUtil
                val newList = ArrayList(creditList)

                // Submit to adapter with callback
                creditAdapter.submitList(newList) {
                    Log.d("MainActivity", "Adapter list submitted")
                    updateAllTotals()  // Update all totals after adapter updates
                }

                hasPendingChanges = true
                databaseAddFlag = false

            } catch (e: Exception) {
                Log.e("MainActivity", "Error updating credit: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Error updating income list", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun debitDataUpdate() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val newList = debitList.toMutableList()
                debitAdapter.submitList(newList) {
                    updateAllTotals()  // ✅ Use unified method
                    calculateFinalTripAverage()
                }
                hasPendingChanges = true
            } catch (e: Exception) {
                Log.e("MainActivity", "Error updating debit: ${e.message}", e)
            }
        }
    }

// ==========================================
// 4. UPDATE setObserver() METHOD
// ==========================================

    private fun setObserver() {
        // In setObserver()
        Constants.events.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it.amount.toInt() >= 0 && !it.desc.isNullOrEmpty()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            val income = Income(
                                desc = it.desc,
                                amount = it.amount,
                                note = it.note,
                                place = it.place,
                                date = it.date
                            )

                            val isDuplicate = creditList.any { existing ->
                                existing.desc == income.desc &&
                                        existing.amount == income.amount &&
                                        existing.date == income.date &&
                                        existing.place == income.place
                            }

                            if (!isDuplicate) {
                                creditList.add(income)
                                Log.d("MainActivity", "Added income via observer: ${income.desc}")

                                val newList = creditList.toMutableList()
                                creditAdapter.submitList(newList) {
                                    updateAllTotals()  // ✅ This updates profit
                                }

                                hasPendingChanges = true
                                databaseAddFlag = false
                            }

                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error adding income: ${e.message}", e)
                        }
                    }
                }
            }
        }

// ==========================================
// 7. UPDATE setObserver() - Expense Section
// ==========================================
        Constants.debitevents.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it.amount.toInt() >= 0 && !it.desc.isNullOrEmpty()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            val expense = Expense(
                                desc = it.desc,
                                amount = it.amount,
                                note = it.note,
                                place = it.place,
                                date = it.date,
                                type = it.type,
                                liters = it.liters,
                                km = it.km
                            )

                            val isDuplicate = debitList.any { existing ->
                                existing.desc == expense.desc &&
                                        existing.amount == expense.amount &&
                                        existing.date == expense.date &&
                                        existing.place == expense.place
                            }

                            if (!isDuplicate) {
                                debitList.add(expense)
                                Log.d("MainActivity", "Added expense: ${expense.desc}")

                                val newList = debitList.toMutableList()
                                debitAdapter.submitList(newList) {
                                    updateAllTotals()  // ✅ Use unified method
                                    calculateFinalTripAverage()
                                }

                                hasPendingChanges = true
                                databaseAddFlag = false
                            }

                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error adding expense: ${e.message}", e)
                        }
                    }
                }
            }
        }

        authViewModel.downloadProgress.observe(this) {
            dismissProgress()
        }

        authViewModel.downloadCompleted.observe(this, Observer { data ->
            isPdfGenerating = false
            databaseAddFlagName = true
            databaseAddFlag = true
            dismissProgress()

            try {
                // Delete draft after successful PDF generation
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val draftDao = db.productsDao()

                        if (randomNumber != "0") {
                            val randomNumberStr = randomNumber.toString()
                            draftDao.deleteByRandomNumber(randomNumberStr)

                            val checkDraft = draftDao.getDraftById(randomNumberStr)
                            if (checkDraft == null) {
                                Log.d("MainActivity", "Draft deleted successfully")
                            }
                        }

                        withContext(Dispatchers.Main) {
                            Constants.refreshApiGet(Event(1))
                            openFile(data)
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error deleting draft: ${e.message}", e)
                    }
                }

                finish()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error opening file: ${e.message}")
            }
        })
    }

    // 5. IMPROVED DELETE DIALOG CALLBACKS
    override fun clickCreditDeleteMethod(position: Int) {
        try {
            if (position < 0 || position >= creditList.size) {
                Log.e("MainActivity", "Invalid credit position for delete dialog: $position")
                return
            }

            deleteDialogFragment = DeleteDialogFragment(position, "Credit")
            deleteDialogFragment.show(supportFragmentManager, "DeleteCreditDialog")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing credit delete dialog: ${e.message}", e)
        }
    }

    override fun clickDebitDeleteMethod(position: Int) {
        try {
            if (position < 0 || position >= debitList.size) {
                Log.e("MainActivity", "Invalid debit position for delete dialog: $position")
                return
            }

            deleteDialogFragment = DeleteDialogFragment(position, "Debit")
            deleteDialogFragment.show(supportFragmentManager, "DeleteDebitDialog")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing debit delete dialog: ${e.message}", e)
        }
    }

    // ==========================================
// ADAPTER INITIALIZATION - IMPROVED VERSION
// ==========================================
    private fun initAdapter() {
        try {
            // Credit Adapter
            creditAdapter = CreditAdapter(this, this, this)
            binding.creditAmount.adapter = creditAdapter
            creditAdapter.submitList(ArrayList(creditList)) {
                updateAllTotals()  // ✅ Use unified method after initial load
            }

            // Debit Adapter
            debitAdapter = DebitAdapter(this, this, this)
            binding.debitAmount.adapter = debitAdapter
            debitAdapter.submitList(ArrayList(debitList)) {
                updateAllTotals()  // ✅ Use unified method after initial load
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing adapters: ${e.message}", e)
        }
    }

    // Helper method to update credit totals
    private fun updateCreditTotals() {
        binding.totalIncome.text = getString(R.string.ruppe) + totalamount.toString()
        binding.totalIncomeNew.text = totalamount.toString()
        updateProfitDisplay()
    }

    private fun recalculateTotalsAndProfit() {
        val income = creditList.sumOf { it.amount.toIntOrNull() ?: 0 }
        val expense = debitList.sumOf { it.amount.toIntOrNull() ?: 0 }

        binding.totalIncome.text = getString(R.string.ruppe) + income
        binding.totalIncomeNew.text = income.toString()

        binding.totalExpanse.text = getString(R.string.ruppe) + expense
        binding.totalExpanseNew.text = expense.toString()

        val profit = income - expense
        binding.totalProfit.text = profit.toString()
    }


    // Helper method to update debit totals
    private fun updateDebitTotals() {
        binding.totalExpanse.text = getString(R.string.ruppe) + totalDebitAmount.toString()
        binding.totalExpanseNew.text = totalDebitAmount.toString()
        updateProfitDisplay()
    }

    // Helper method to update profit display
    private fun updateProfitDisplay() {
        try {
            val income = binding.totalIncomeNew.text.toString().toDoubleOrNull() ?: 0.0
            val expense = binding.totalExpanseNew.text.toString().toDoubleOrNull() ?: 0.0
            val profit = income - expense
            binding.totalProfit.text = String.format("%.2f", profit)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating profit: ${e.message}", e)
            binding.totalProfit.text = "0"
        }
    }




    // Add these variables at the top of MainActivity class
    private var hasPendingChanges = false  // Track if user made any changes
    private var isPdfGenerating = false     // Track if PDF is being generated

    // Update setObserver() method - Add change tracking
//    private fun setObserver() {
//        Constants.events.observe(this) { it ->
//            it.getContentIfNotHandled()?.let { event ->
//                event.let {
//                    if (it.amount.toInt() >= 0 && !it.desc.toString().isNullOrEmpty()) {
//                        val income = Income(
//                            desc = it.desc,
//                            amount = it.amount,
//                            note = it.note,
//                            place = it.place,
//                            date = it.date
//                        )
//                        if (creditList.size > 0) {
//                            if (!creditList.contains(income)) {
//                                creditList.add(income)
//                                hasPendingChanges = true  // Mark as changed
//                            }
//                        } else {
//                            creditList.add(income)
//                            hasPendingChanges = true  // Mark as changed
//                        }
//                        binding.creditAmount.adapter = creditAdapter
//                        creditAdapter.submitList(creditList) {
//                            // This callback runs after DiffUtil finishes
//                            creditAdapter.notifyDataSetChanged()
//                            creditIncomeUpdate()
//                        }
//                        creditAdapter.notifyDataSetChanged()
//                        creditIncomeUpdate()
//                        databaseAddFlag = false
//                    }
//                }
//            }
//        }
//
//        Constants.debitevents.observe(this) {
//            it.getContentIfNotHandled()?.let { event ->
//                event.let {
//                    if (it.amount.toInt() >= 0 && !it.desc.toString().isNullOrEmpty()) {
//                        val expense = Expense(
//                            desc = it.desc,
//                            amount = it.amount,
//                            note = it.note,
//                            place = it.place,
//                            date = it.date,
//                            type = it.type,
//                            liters = it.liters,
//                            km = it.km
//                        )
//                        if (debitList.size > 0) {
//                            if (!debitList.contains(expense)) {
//                                debitList.add(expense)
//                                hasPendingChanges = true  // Mark as changed
//                            }
//                        } else {
//                            debitList.add(expense)
//                            hasPendingChanges = true  // Mark as changed
//                        }
//                        binding.debitAmount.adapter = debitAdapter
//                        debitAdapter.submitList(debitList) {
//                            debitAdapter.notifyDataSetChanged()
//                            debitIncomeUpdate()
//                        }
//                        debitAdapter.notifyDataSetChanged()
//                        debitIncomeUpdate()
//                        databaseAddFlag = false
//                    }
//                }
//            }
//        }
//
//        authViewModel.downloadProgress.observe(this) {
//            dismissProgress()
//
//        }
//
//        authViewModel.downloadCompleted.observe(this, Observer { data ->
//            isPdfGenerating = false
//            databaseAddFlagName = true
//            databaseAddFlag = true
//            dismissProgress()
//
//            try {
//
//
//
//                // Delete draft after successful PDF generation
//                lifecycleScope.launch(Dispatchers.IO) {
//                    try {
//
//
//                        // Delete by randomNumber for draft trips
//                        val db = AppDatabase.getDatabase(applicationContext)
//                        val draftDao = db.productsDao()
//
//                        // Use deleteByRandomNumber since randomNumber is stored as String
//                        if (randomNumber != "0") {
//                            val randomNumberStr = randomNumber.toString()
//                            draftDao.deleteByRandomNumber(randomNumberStr)
//                            // Verify deletion
//                            val checkDraft = draftDao.getDraftById(randomNumberStr)
//                            if (checkDraft == null) {
//                                openFile(data)
//                            } else {
//                            }
//                        }
//                        // Refresh the draft list
//                        withContext(Dispatchers.Main) {
//                            Constants.refreshApiGet(Event(1))
//                        }
//                    } catch (e: Exception) {
//                        Log.e("MainActivity", "Error deleting draft: ${e.message}", e)
//                    }
//                }
//
//                finish()
//            } catch (e: Exception) {
//                Log.e("MainActivity", "Error opening file: ${e.message}")
//            }
//        })
//    }

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

    private fun setOnclickListner() {
        // Use single-click extension and also prevent opening the same dialog if already shown.
        binding.add.setOnSingleClickListener {
            if (::addTaskDialogFragment.isInitialized && addTaskDialogFragment.isAdded) return@setOnSingleClickListener
            addTaskDialogFragment = AddTaskDialogFragment("", "", "", 0)
            addTaskDialogFragment.show(supportFragmentManager, "")
        }

        binding.backBtn.setOnSingleClickListener {
            onBackPressed()
        }

        binding.done.setOnSingleClickListener {
            if (::tripEndDialogFragment.isInitialized && tripEndDialogFragment.isAdded) return@setOnSingleClickListener
            tripName = binding.srcName.text.toString() + " TO " + binding.dest.text.toString()
            showTripEndDialog()
        }

        binding.editLayout.setOnSingleClickListener {
            if (::tripDialogFragment.isInitialized && tripDialogFragment.isAdded) return@setOnSingleClickListener

            callDialog(
                "${startTripDate}",
                startingPlace = binding.srcName.text.toString(),
                endingPlace = binding.dest.text.toString(),
                driverIncome = "",
                truckNumber = truckNumberGiven.toString(),
                truckAvg = "",
                totalDays = totalDaysTrip,
                update = "yes",
                startOdometer =startOdometer
            )
        }

        binding.cardFood.setOnSingleClickListener {
            if (::addExpenseBottomSheetFragment.isInitialized && addExpenseBottomSheetFragment.isAdded) return@setOnSingleClickListener
            addExpenseBottomSheetFragment = AddExpenseBottomSheetFragment()
            addExpenseBottomSheetFragment.show(supportFragmentManager, "AddExpenseBottomSheet")
        }

        binding.cardToll.setOnSingleClickListener {
            if (::addIncomeBottomSheetFragment.isInitialized && addIncomeBottomSheetFragment.isAdded) return@setOnSingleClickListener
            addIncomeBottomSheetFragment = AddIncomeBottomSheetFragment()
            addIncomeBottomSheetFragment.show(supportFragmentManager, "AddIncomeBottomSheet")
        }

        binding.cardDiesel.setOnSingleClickListener {
            if (::addFuelBottomSheetFragment.isInitialized && addFuelBottomSheetFragment.isAdded) return@setOnSingleClickListener
            addFuelBottomSheetFragment = AddFuelBottomSheetFragment()
            addFuelBottomSheetFragment.show(supportFragmentManager, "AddFuelBottomSheet")
        }
    }

    override fun onClick(v: View?) {
        // kept for compatibility; main click handling moved to setOnclickListner()
    }

    private fun roundOffDecimal(number: Double): Double? {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }


    override fun onPause() {
        super.onPause()
        Log.d("whathappend", "onPause: ${databaseAddFlagName}   ${databaseAddFlag}")
        Log.d("MainActivity", "onPause called")

        // Check all skip conditions
        if (draftFlowClosed) {
            Log.d("MainActivity", "Draft flow closed – skipping save")
            return
        }

        if (isDraftDeletedAfterPdf) {
            Log.d("MainActivity", "Draft already deleted, skipping save")
            return
        }

        if (isSavingDraft) {
            Log.d("MainActivity", "Already saving draft, skipping")
            return
        }

        if (!hasPendingChanges) {
            Log.d("MainActivity", "No pending changes, skipping save")
            return
        }

        // Now save
        setAllData()


//        if(showingFlag==3 && !databaseAddFlag ||checkListChanged()){
//            getDelete(this,id.toInt())
//        }
//        if (hasPendingChanges && !isPdfGenerating && hasValidTripData() && !isFinishing) {
//            saveDraftSynchronously()
//            // Added !isFinishing check ✅
//        }

    }


    private fun setAllData() {
        // Prevent multiple simultaneous saves
        if (isSavingDraft) {
            Log.d("MainActivity", "Save already in progress, skipping")
            return
        }

        // Cancel any existing save job
        saveJob?.cancel()

        isSavingDraft = true

        saveJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainActivity", "Starting draft save...")

                val db = AppDatabase.getDatabase(applicationContext)
                val draftDao = db.productsDao()

                val validRoute = if (RouteUtils.isValidRoute(routeList)) {
                    routeList
                } else {
                    RouteUtils.createSimpleRoute(
                        binding.srcName.text.toString(),
                        binding.dest.text.toString()
                    )
                }

                val routeJsonString = RouteUtils.routeToJson(validRoute)

                val tripDataEntity = Products(
                    truckNumber = truckNumberGiven.toString(),
                    srcPlace = binding.srcName.text.toString(),
                    destPlace = binding.dest.text.toString(),
                    srcDate = startTripDate.toString(),
                    destDate = "",
                    avg = "",
                    randomNumber = randomNumber.toString(),
                    modelList1 = Gson().toJson(creditList),
                    modelList2 = Gson().toJson(debitList),
                    routeJson = routeJsonString,
                    updatedAt = System.currentTimeMillis()
                )

                // Check if draft exists
                val existing = draftDao.getDraftById(randomNumber.toString())

                if (existing != null || showingFlag == 3) {
                    // Update existing draft
                    draftDao.update(
                        tripDataEntity.randomNumber,
                        tripDataEntity.truckNumber,
                        tripDataEntity.srcPlace,
                        tripDataEntity.destPlace,
                        tripDataEntity.srcDate,
                        tripDataEntity.destDate,
                        tripDataEntity.avg,
                        tripDataEntity.modelList1,
                        tripDataEntity.modelList2,
                        tripDataEntity.routeJson,
                        updatedAt = System.currentTimeMillis()
                    )
                    Log.d("MainActivity", "Draft updated: $randomNumber")
                } else {
                    // Insert new draft
                    draftDao.insert(tripDataEntity)
                    Log.d("MainActivity", "New draft inserted: $randomNumber")
                }

                // Reset flags AFTER successful save
                hasPendingChanges = false

                withContext(Dispatchers.Main) {
                    // Only refresh if activity is not finishing
                    if (!isFinishing) {
                        Constants.refreshApiGet(Event(1))
                    }
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error saving draft: ${e.message}", e)
            } finally {
                isSavingDraft = false
            }
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
            driverIncome = "",
            modelList1 = try {
                gson.fromJson(
                    entity.modelList1,
                    object : TypeToken<List<Income>>() {}.type
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error parsing modelList1: ${e.message}")
                emptyList()
            },
            modelList2 = try {
                gson.fromJson(
                    entity.modelList2,
                    object : TypeToken<List<Expense>>() {}.type
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error parsing modelList2: ${e.message}")
                emptyList()
            },
            routeList = entity.routeJson,
            route = arrayListOf(),
            startOdometer = startOdometer,
            endOdometer = endOdometer!!,
            endTripKm = endKM!!,
            id = ""
        )
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v: View = currentFocus ?: return super.dispatchTouchEvent(event)
            if (v is EditText) {
                val outRect = Rect()// here rect means rectangle class
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
                    imm?.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

//    fun creditDataUpdate(position: Int = -1) {
//        lifecycleScope.launch(Dispatchers.Main) {
//            val newList = creditList.toList()
//            creditAdapter.submitList(newList) {
//                recalculateTotalsAndProfit()
//            }
//            hasPendingChanges = true
//            databaseAddFlag = false
//        }
//    }


    // Debit update with position
    fun debitDataUpdate(position: Int = -1) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                Log.d("MainActivity", "Updating debit list, position: $position")

                // Create new list with new reference
                val newList = debitList.toList()  // Immutable copy

                debitAdapter.submitList(newList) {
                    debitIncomeUpdate()
                    calculateFinalTripAverage()
                    Log.d("MainActivity", "Debit adapter updated")
                }
                recalculateTotalsAndProfit()
                hasPendingChanges = true
                databaseAddFlag = false

            } catch (e: Exception) {
                Log.e("MainActivity", "Error updating debit: ${e.message}", e)
            }
        }
    }
//    fun debitDeleteHissab(position: Int) {
//        debitList.removeAt(position)
//        debitAdapter.submitList(debitList)
//        debitAdapter.notifyDataSetChanged()
//        debitIncomeUpdate()
//        databaseAddFlag = false
//    }
//
//    fun creditDeleteHissab(position: Int) {
//        creditList.removeAt(position)
//        creditAdapter.submitList(creditList)
//        creditAdapter.notifyDataSetChanged()
//        creditIncomeUpdate()
//        databaseAddFlag = false
//    }
//
//    fun debitDataUpdate() {
//        debitAdapter.submitList(debitList)
//        binding.debitAmount.adapter = debitAdapter
//        debitAdapter.notifyDataSetChanged()
//        debitIncomeUpdate()
//        // Recalculate average when fuel entries are updated
//        calculateFinalTripAverage()
//    }
//
//    fun creditDataUpdate() {
//        creditAdapter.submitList(creditList)
//        binding.creditAmount.adapter = creditAdapter
//        creditAdapter.notifyDataSetChanged()
//        creditIncomeUpdate()
//    }


    override fun onBackPressed() {
        super.onBackPressed()
        if (hasPendingChanges && !isSavingDraft) {
            lifecycleScope.launch {
                setAllData()
                // Wait for save to complete
                saveJob?.join()
                // Then finish activity
                withContext(Dispatchers.Main) {
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    fun onExpenseEdited(position: Int, updatedExpense: Expense) {
        if (position !in debitList.indices) return

        val newList = debitList.toMutableList()
        newList[position] = updatedExpense

        debitList.clear()
        debitList.addAll(newList)

        debitAdapter.submitList(newList) {
            updateAllTotals()  // ✅ Use unified method
            calculateFinalTripAverage()
        }

        hasPendingChanges = true
    }

    // ==========================================
// 5. UPDATE onIncomeEdited METHOD
// ==========================================
    fun onIncomeEdited(position: Int, updatedIncome: Income) {
        if (position !in creditList.indices) return

        val newList = creditList.toMutableList()
        newList[position] = updatedIncome

        creditList.clear()
        creditList.addAll(newList)

        creditAdapter.submitList(newList) {
            updateAllTotals()  // ✅ This updates profit
        }

        hasPendingChanges = true
    }

    private fun debitIncomeUpdate() {
        totalDebitAmount = 0
        for (item in debitList) {
            totalDebitAmount += item.amount.toInt()
        }
        binding.totalExpanse.text = getString(R.string.ruppe) + totalDebitAmount.toString()
        binding.totalExpanseNew.text = totalDebitAmount.toString()

        val total = binding.totalIncomeNew.text.toString()
            .toDouble() - if (binding.totalExpanseNew.text.toString()
                .isNullOrEmpty()
        ) 0.0 else binding.totalExpanseNew.text.toString().toDouble()
        binding.totalProfit.text = "$total"
    }

    private fun calculateTotalLitersFromFuelEntries(): Double {
        return debitList
            .filter { it.type == "Fuel" && it.liters.isNotEmpty() }
            .sumOf { it.liters.toDoubleOrNull() ?: 0.0 }
    }
    /**
     * Calculate trip average from fuel entries
     * Average = Total KM / Total Liters
     * For odometer readings: Total KM = (Max KM - Min KM) if multiple entries, or use KM if single entry
     * Returns average as string, or empty string if no valid fuel entries
     */
    private fun calculateFinalTripAverage(): String {
        try {
            // SCENARIO 1: User provided both start and end odometer readings
            if (startOdometer!!.isNotEmpty() && endOdometer!!.isNotEmpty()) {
                val startOdo = startOdometer!!.toDoubleOrNull() ?: 0.0
                val endOdo = endOdometer?.toDoubleOrNull() ?: 0.0

                if (endOdo > startOdo) {
                    val totalKm = endOdo - startOdo
                    val totalLiters = calculateTotalLitersFromFuelEntries()

                    if (totalLiters > 0) {
                        val average = totalKm / totalLiters
                        return String.format("%.2f", average)
                    }
                }
            }

            // SCENARIO 2: User provided manual KM at trip end (not odometer mode)
            if (!endKmIsOdometerMode && endManualKm.isNotEmpty()) {
                val manualKm = endManualKm.toDoubleOrNull() ?: 0.0

                if (manualKm > 0) {
                    val totalLiters = calculateTotalLitersFromFuelEntries()

                    if (totalLiters > 0) {
                        val average = manualKm / totalLiters
                        return String.format("%.2f", average)
                    }
                }
            }

            // SCENARIO 3: No start odometer provided, but we have fuel entries
            // Calculate from fuel entry odometer readings
            if (startOdometer!!.isEmpty()) {
                // Get all fuel entries with valid odometer readings, sorted by date
                val fuelEntriesWithOdometer = debitList
                    .filter {
                        it.type == "Fuel" &&
                                it.isOdometerMode &&
                                it.km.isNotEmpty() &&
                                (it.km.toDoubleOrNull() ?: 0.0) > 0
                    }
                    .sortedBy { entry ->
                        try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.date)?.time ?: 0L
                        } catch (e: Exception) {
                            0L
                        }
                    }

                if (fuelEntriesWithOdometer.isNotEmpty()) {
                    // Get first and last odometer readings from fuel entries
                    val firstOdometer = fuelEntriesWithOdometer.first().km.toDoubleOrNull() ?: 0.0

                    // If user provided end odometer, use that; otherwise use last fuel entry
                    val lastOdometer = if (endOdometer!!.isNotEmpty()) {
                        endOdometer!!.toDoubleOrNull() ?: fuelEntriesWithOdometer.last().km.toDoubleOrNull() ?: 0.0
                    } else {
                        fuelEntriesWithOdometer.last().km.toDoubleOrNull() ?: 0.0
                    }

                    if (lastOdometer > firstOdometer) {
                        val totalKm = lastOdometer - firstOdometer
                        val totalLiters = calculateTotalLitersFromFuelEntries()

                        if (totalLiters > 0) {
                            val average = totalKm / totalLiters
                            return String.format("%.2f", average)
                        }
                    }
                }
            }

            // SCENARIO 4: Calculate from individual fuel entry distances (non-odometer mode entries)
            val fuelEntriesWithDistance = debitList
                .filter {
                    it.type == "Fuel" &&
                            !it.isOdometerMode &&
                            it.km.isNotEmpty() &&
                            (it.km.toDoubleOrNull() ?: 0.0) > 0
                }

            if (fuelEntriesWithDistance.isNotEmpty()) {
                val totalKm = fuelEntriesWithDistance.sumOf { it.km.toDoubleOrNull() ?: 0.0 }
                val totalLiters = calculateTotalLitersFromFuelEntries()

                if (totalKm > 0 && totalLiters > 0) {
                    val average = totalKm / totalLiters
                    return String.format("%.2f", average)
                }
            }

            // No valid data to calculate average
            return ""

        } catch (e: Exception) {
            Log.e("MainActivity", "Error calculating final trip average", e)
            return ""
        }
    }

    private fun showCoachMarksIfNeeded() {
        // Check if coach marks should be shown
        if (!CoachMarkHelper.shouldShowCoachMark(this)) {
            return
        }

        // Wait for views to be laid out
        binding.root.post {
            val coachMarkTargets = mutableListOf<CoachMarkHelper.CoachMarkTarget>()

            // Add Expense coach mark
            coachMarkTargets.add(
                CoachMarkHelper.CoachMarkTarget(
                    view = binding.cardFood,
                    text = getString(R.string.coach_mark_expense), // Add this string resource
                    direction = CoachMarkView.ArrowDirection.TOP
                )
            )

            // Add Income coach mark
            coachMarkTargets.add(
                CoachMarkHelper.CoachMarkTarget(
                    view = binding.cardToll,
                    text = getString(R.string.coach_mark_income), // Add this string resource
                    direction = CoachMarkView.ArrowDirection.TOP
                )
            )

            // Add Fuel coach mark
            coachMarkTargets.add(
                CoachMarkHelper.CoachMarkTarget(
                    view = binding.cardDiesel,
                    text = getString(R.string.coach_mark_fuel), // Add this string resource
                    direction = CoachMarkView.ArrowDirection.TOP
                )
            )

            // Show coach marks
            CoachMarkHelper.showCoachMarks(
                activity = this,
                targets = coachMarkTargets,
                onDismiss = {
                    // Optional: Do something after all coach marks are dismissed
                }
            )
        }
    }

    private fun creditIncomeUpdate() {
        try {
            Log.d("MainActivity", "creditIncomeUpdate called, list size: ${creditList.size}")

            // Calculate total
            totalamount = 0
            for (item in creditList) {
                val amount = item.amount.toIntOrNull() ?: 0
                totalamount += amount
            }

            Log.d("MainActivity", "Total income calculated: $totalamount")

            // Update UI
            binding.totalIncome.text = getString(R.string.ruppe) + totalamount.toString()
            binding.totalIncomeNew.text = totalamount.toString()

            // Update profit
            val income = totalamount.toDouble()
            val expense = binding.totalExpanseNew.text.toString().toDoubleOrNull() ?: 0.0
            val profit = income - expense
            binding.totalProfit.text = String.format("%.2f", profit)

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in creditIncomeUpdate: ${e.message}", e)
        }
    }

    fun setLanguage() {
        val locale = Locale(Prefs[Constants.languageCode, ""])
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Show dialog to collect Trip End Date and Driver Income
     * After collecting data, show ads, then generate PDF
     */
    private fun showTripEndDialog() {
        if (::tripEndDialogFragment.isInitialized && tripEndDialogFragment.isAdded) return

        Log.d("dataLoaded", "showTripEndDialog: ${endOdometer}")
        tripEndDialogFragment = TripEndDialogFragment(
            startDate = startTripDate ?: "",
            existingEndDate = endTripDate ?: "",
            existingDriverIncome = driverIncomeAmount ?: "",
            existingEndOdometer = endOdometer ?: "",  // NEW: Pass existing end odometer
            existingEndKm = endManualKm ?: "",               // NEW: Pass existing manual KM
        ) { endDate, driverIncome, totalDays, endOdometer_, endKm, isOdometerMode ->
            // Update values
            totalDaysTrip = totalDays
            endTripDate = endDate
            driverIncomeAmount = driverIncome
            endOdometer = endOdometer_  // NEW: Store end odometer
            endManualKm = endKm               // NEW: Store manual KM
            endKmIsOdometerMode = isOdometerMode  // NEW: Store mode

            generateTripPdf(endDate, driverIncome,endOdometer ?: "",isOdometerMode,endKm)
        }
        tripEndDialogFragment.show(supportFragmentManager, "TripEndDialog")
        tripEndDialogFragment.isCancelable = false
    }

    /**
     * Generate PDF with collected end date and driver income
     */
    private fun generateTripPdf(endDate: String, driverIncome: String,endOdometer: String,isOdometerMode: Boolean,
                endKm: String) {
        try {
            isPdfGenerating = true  // Mark PDF generation started

            val calculatedAverage = calculateFinalTripAverage()
            Log.d("calculatedAverage", "generateTripPdf: ${calculatedAverage}")

            val validRoute = if (RouteUtils.isValidRoute(routeList)) {
                routeList
            } else {
                RouteUtils.createSimpleRoute(
                    binding.srcName.text.toString(),
                    binding.dest.text.toString()
                )
            }
            isDraftDeletedAfterPdf = true
            draftFlowClosed = true
            authViewModel.getTripPdf(
                AddTripRequestModel(
                    id,
                    binding.dest.text.toString(),
                    driverIncome,
                    endDate,
                    route = validRoute,
                    debitList as List<Expense>,
                    creditList as List<Income>,
                    binding.totalProfit.text.toString(),
                    binding.srcName.text.toString(),
                    startTripDate.toString(),
                    totalDaysTrip,
                    binding.totalExpanseNew.text.toString(),
                    binding.totalIncomeNew.text.toString(),
                    calculatedAverage,
                    truckNumberGiven.toString(),
                    startOdometer ?: "",

                    endOdometer,
                    isOdometerMode,
                    endKm
                )
            )
            showProgress()

        } catch (e: Exception) {
            isPdfGenerating = false
            Log.e("MainActivity", "Error generating PDF: ${e.message}", e)
            dismissProgress()
            Toast.makeText(this, "Error generating PDF. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasValidTripData(): Boolean {
        return try {
            val hasTruckNumber = !truckNumberGiven.isNullOrEmpty()
            val hasSource = binding.srcName.text.isNotEmpty()
            val hasDestination = binding.dest.text.isNotEmpty()
            val hasDate = !startTripDate.isNullOrEmpty()
            val hasEntries = creditList.isNotEmpty() || debitList.isNotEmpty()

            val isValid = (hasTruckNumber || (hasSource && hasDestination)) && (hasDate || hasEntries)

            Log.d("MainActivity", "hasValidTripData: truck=$hasTruckNumber, src=$hasSource, " +
                    "dest=$hasDestination, date=$hasDate, entries=$hasEntries, result=$isValid")

            isValid
        } catch (e: Exception) {
            Log.e("MainActivity", "Error validating trip data: ${e.message}")
            false
        }
    }
    private fun updateAllTotals() {
        try {
            // Calculate credit total
            totalamount = creditList.sumOf { it.amount.toIntOrNull() ?: 0 }

            // Calculate debit total
            totalDebitAmount = debitList.sumOf { it.amount.toIntOrNull() ?: 0 }

            // Update Income UI
            binding.totalIncome.text = getString(R.string.ruppe) + totalamount.toString()
            binding.totalIncomeNew.text = totalamount.toString()

            // Update Expense UI
            binding.totalExpanse.text = getString(R.string.ruppe) + totalDebitAmount.toString()
            binding.totalExpanseNew.text = totalDebitAmount.toString()

            // Calculate and update Profit
            val profit = totalamount - totalDebitAmount
            binding.totalProfit.text = profit.toString()

            Log.d("MainActivity", "Totals updated - Income: $totalamount, Expense: $totalDebitAmount, Profit: $profit")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating totals: ${e.message}", e)
        }
    }
    override fun clickCreditEditMethod(position: Int) {
        try {
            if (position < 0 || position >= creditList.size) {
                Log.e("MainActivity", "Invalid credit position: $position")
                return
            }

            val income = creditList[position]
            Log.d("MainActivity", "Editing credit at position $position: ${income.desc}")

            addIncomeBottomSheetFragment = AddIncomeBottomSheetFragment(
                incomeDesc = income.desc ?: "",
                incomeAmount = income.amount ?: "",
                totalIncome = income.amount ?: "",  // Pass as totalIncome
                advanceTaken = "0",  // Default
                balance = income.amount ?: "",  // Default to amount
                note = income.note ?: income.desc ?: "",
                place = income.place ?: "",
                date = income.date ?: "",
                position = position
            )
            addIncomeBottomSheetFragment.show(supportFragmentManager, "AddIncomeBottomSheet")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in credit edit: ${e.message}", e)
        }
    }

    override fun clickDebitEditMethod(position: Int) {
        try {
            if (position < 0 || position >= debitList.size) {
                Log.e("MainActivity", "Invalid debit position: $position")
                return
            }

            val expense = debitList[position]
            Log.d("MainActivity", "Editing debit at position $position: ${expense.desc}")

            if (expense.type == "Fuel") {
                addFuelBottomSheetFragment = AddFuelBottomSheetFragment(
                    fuelDesc = expense.desc,
                    fuelAmount = expense.amount,
                    fuelLiters = expense.liters ?: "",
                    fuelKm = expense.km ?: "",
                    fuelPlace = expense.place ?: "",
                    fuelDate = expense.date ?: "",
                    isOdometerMode = expense.isOdometerMode,
                    position = position  // Pass position
                )
                addFuelBottomSheetFragment.show(supportFragmentManager, "AddFuelBottomSheet")
            } else {
                addExpenseBottomSheetFragment = AddExpenseBottomSheetFragment(
                    expenseDesc = expense.desc ?: "",
                    expenseAmount = expense.amount,
                    expenseNote = expense.note ?: "",
                    expensePlace = expense.place ?: "",
                    expenseDate = expense.date ?: "",
                    expenseType = expense.type ?: "",
                    position = position  // Pass position
                )
                addExpenseBottomSheetFragment.show(supportFragmentManager, "AddExpenseBottomSheet")
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in debit edit: ${e.message}", e)
        }
    }


}

