package com.dadabarbie.TruckTrip.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.util.Pair
import androidx.lifecycle.Observer
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
import com.dadabarbie.TruckTrip.model.CreditModel
import com.dadabarbie.TruckTrip.model.DebitModel
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
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vasyerp.cafvd.room.model.Products
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.toString
import android.view.ViewConfiguration
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.views.CoachMarkHelper
import com.dadabarbie.TruckTrip.views.CoachMarkView

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
class MainActivity : AppCompatActivity(), View.OnClickListener,
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
    private val authViewModel: AuthViewModel by viewModels()
    lateinit var debitAdapter: DebitAdapter
    private var totalamount = 0
    var draftFlag = false
    var databaseAddFlag = false
    private lateinit var materialDateBuilder: MaterialDatePicker.Builder<Pair<Long, Long>>
    private lateinit var materialDatePicker: MaterialDatePicker<*>
    private val testList: ArrayList<TripDataTestModel> = arrayListOf()
    private var courseList: ArrayList<TripDataTestModel> = arrayListOf()
    lateinit var deleteDialogFragment: DeleteDialogFragment
    var totalDebitAmount = 0
    var randomNumber = 0
    var showingFlag = 0
    var startTripDate: String? = ""
    var truckNumberGiven: String? = ""
    var driverIncome = "0"
    var totalDaysTrip = "0"
    var id = "0"

    var startOdometerReading: String = ""  // NEW: Start odometer from trip dialog
    var endOdometerReading: String = ""    // NEW: End odometer from trip end dialog
    var endManualKm: String = ""           // NEW: Manual KM from trip end dialog
    var endKmIsOdometerMode: Boolean = true

// In MainActivity.kt, add these variables at the top with other declarations:

    var endTripDate: String? = ""           // Add this
    var driverIncomeAmount: String? = "0"
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
        setLanguage()
        initViews()
        setOnclickListner()
        setObserver()
        MobileAds.initialize(this) {}
        loadRewardedAd()

        // Show coach marks after a short delay to ensure all views are ready
        binding.root.postDelayed({
            showCoachMarksIfNeeded()
        }, 500)
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
            callDialog("", "", "", "", "", "", "", "")
        } else {
            // Edit existing trip
            randomNumberGenerate()

            val truckNumber = intent.getStringExtra("truckNumber")
            val srcPlace = intent.getStringExtra("sourceName")
            val driverAvak = intent.getStringExtra("driverAvak")
            val destPlace = intent.getStringExtra("destinationName")
            val startDate = intent.getStringExtra("startingDate")
            startTripDate = startDate
            truckNumberGiven = truckNumber
            val endDate = intent.getStringExtra("endingDate")
            endTripDate = endDate
            driverIncomeAmount = driverAvak
            id = intent.getStringExtra("id").toString()

            Log.d("MainActivity", "Editing trip ID: $id")

            if (showingFlag == 2 && id.isNotEmpty()) {
                randomNumber = id.toIntOrNull() ?: randomNumber
                databaseAddFlag = true

                // CRITICAL FIX: Only parse from Intent if Constants lists are empty
                // This prevents overwriting the lists that were already populated in HomeFragment
                if (Constants.creditList.isEmpty() && Constants.debitList.isEmpty()) {
                    Log.d("MainActivity", "Lists empty, parsing from Intent JSON")

                    intent.getStringExtra("incomeJson")?.let { json ->
                        try {
                            val parsed: List<Income> = Gson().fromJson(
                                json,
                                object : TypeToken<List<Income>>() {}.type
                            )
                            Constants.creditList.clear()
                            Constants.creditList.addAll(parsed)
                            Log.d("MainActivity", "Parsed ${parsed.size} income entries from JSON")
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error parsing income JSON: ${e.message}")
                        }
                    }

                    intent.getStringExtra("expenseJson")?.let { json ->
                        try {
                            val parsed: List<Expense> = Gson().fromJson(
                                json,
                                object : TypeToken<List<Expense>>() {}.type
                            )
                            Constants.debitList.clear()
                            Constants.debitList.addAll(parsed)
                            Log.d("MainActivity", "Parsed ${parsed.size} expense entries from JSON")
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error parsing expense JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.d("MainActivity", "Using existing lists - Income: ${Constants.creditList.size}, Expense: ${Constants.debitList.size}")
                }
            }

            // Set UI values
            binding.truckNumber.text = truckNumber.toString()
            binding.srcName.text = srcPlace.toString()
            binding.dest.text = destPlace.toString()
            binding.srcDate.text = startDate.toString()

            // Update adapters with loaded data
            if (creditList.isNotEmpty()) {
                Log.d("MainActivity", "Updating credit adapter with ${creditList.size} items")
                creditIncomeUpdate()
            }

            if (debitList.isNotEmpty()) {
                Log.d("MainActivity", "Updating debit adapter with ${debitList.size} items")
                debitIncomeUpdate()
            }
        }

        initAdapter()
    }

    private fun initAdapter() {
        creditAdapter = CreditAdapter(this, this, this)
        binding.creditAmount.adapter = creditAdapter
        creditAdapter.submitList(creditList)
        creditAdapter.notifyItemRangeChanged(0, creditList.size)

        debitAdapter = DebitAdapter(this, this, this)
        binding.debitAmount.adapter = debitAdapter
        debitAdapter.submitList(debitList)
        debitAdapter.notifyItemRangeChanged(0, debitList.size)

        binding.totalIncome.text = getString(R.string.ruppe) + totalamount.toString()
        binding.totalIncomeNew.text = totalamount.toString()
        binding.totalExpanse.text = getString(R.string.ruppe) + totalDebitAmount.toString()
        binding.totalExpanseNew.text = totalDebitAmount.toString()
    }

    private fun randomNumberGenerate() {
        val rand = Random()
        randomNumber = rand.nextInt(1000)
    }

    private fun callDialog(
        date: String, startingPlace: String, endingPlace: String,
        driverIncome: String, truckNumber: String, truckAvg: String,
        totalDays: String, update: String
    ) {
        databaseAddFlag = true
        tripDialogFragment = TripDialogFragment(
            date, startingPlace, endingPlace, driverIncome,
            truckNumber, truckAvg, totalDays, update,
            startOdometerReading  // NEW: Pass start odometer
        )
        tripDialogFragment.show(supportFragmentManager, "")
        tripDialogFragment.isCancelable = false
    }
    fun getDataFill(
        truckNumber: String,
        srcPlaceValue: String,
        destPlaceValue: String,
        startDate: String,
        endDate: String,
        truckAvg: String = "",
        driverTripAvak: String,
        totalDays: String = "0",
        startOdometer: String = ""  // NEW: Add start odometer parameter
    ) {
        binding.truckNumber.text = truckNumber.toString()
        binding.srcName.text = srcPlaceValue
        binding.dest.text = destPlaceValue
        binding.srcDate.text = startDate
        startTripDate = startDate
        truckNumberGiven = truckNumber
        totalDaysTrip = totalDays
        startOdometerReading = startOdometer  // NEW: Store start odometer
    }

    private fun setObserver() {
        Constants.events.observe(this) { it ->
            it.getContentIfNotHandled()?.let { event ->
                event.let {
                    if (it.amount.toInt() >= 0 && !it.desc.toString().isNullOrEmpty()) {
                        val income = Income(
                            desc = it.desc,
                            amount = it.amount,
                            note = it.note,
                            place = it.place,
                            date = it.date
                        )
                        if (creditList.size > 0) {
                            if (!creditList.contains(income)) {
                                creditList.add(income)
                            }
                        } else {
                            creditList.add(income)
                        }
                        binding.creditAmount.adapter = creditAdapter
                        creditAdapter.submitList(creditList)
                        creditAdapter.notifyDataSetChanged()
                        creditIncomeUpdate()
                        databaseAddFlag = false
                    }
                }
            }
        }
        Constants.debitevents.observe(this) {
            it.getContentIfNotHandled()?.let { event ->
                event.let {
                    if (it.amount.toInt() >= 0 && !it.desc.toString().isNullOrEmpty()) {
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
                        if (debitList.size > 0) {
                            if (!debitList.contains(expense)) {
                                debitList.add(expense)
                            }
                        } else {
                            debitList.add(expense)
                        }
                        binding.debitAmount.adapter = debitAdapter
                        debitAdapter.submitList(debitList)
                        debitAdapter.notifyDataSetChanged()
                        debitIncomeUpdate()
                        databaseAddFlag = false
                    }
                }

            }
        }

        authViewModel.downloadProgress.observe(this) {

            dismissProgress()
        }


        authViewModel.downloadCompleted.observe(this, Observer { data ->
            // Update your UI with the streamed data
            databaseAddFlag = true
            dismissProgress()
            try {
                openFile(data)
                Constants.refreshApiGet(Event(1))
                finish()
            } catch (e: Exception) {
            }


        })
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
                update = "yes"
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

    override fun onStop() {
        super.onStop()
        if (!databaseAddFlag)
            setAllData()
    }

    private fun setAllData() {
        var flag = false
        GlobalScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val draftDao = db.productsDao()
            val currentModel = TripDataTestModel(
                truckNumberGiven.toString(),
                binding.srcName.text.toString(),
                binding.dest.text.toString(),
                startTripDate.toString(),
                "",
                "",
                randomNumber.toString(),
                "",
                creditList as List<Income>,
                debitList as List<Expense>
            )
            val existing = draftDao.getDraftById(randomNumber.toString())
            if (existing != null) {
                val existingModel = convertToModel(existing)
                if (existingModel == currentModel) {
                    return@launch
                }
            }
            if (!getAllData(applicationContext).contains(
                    TripDataTestModel(
                        truckNumberGiven.toString(),
                        binding.srcName.text.toString(),
                        binding.dest.text.toString(),
                        startTripDate.toString(),
                        /*binding.destDate.text.toString()*/"",
                        /*binding.truckAvg.text.toString()*/"",
                        /*binding.driverIncome.text.toString()*/"",
                        randomNumber.toString(),
                        creditList as List<Income>,
                        debitList as List<Expense>
                    )
                )
            ) {
                if (getAllData(applicationContext).size == 0) {
                    flag = false
                } else {
                    for (item in getAllData(applicationContext)) {
                        if (item.randomNumber == randomNumber.toString()) {
                            flag = true
                            break
                        } else {
                            flag = false
                        }
                    }
                }
                if (flag) {
                    testList.add(
                        TripDataTestModel(
                            truckNumberGiven.toString(),
                            binding.srcName.text.toString(),
                            binding.dest.text.toString(),
                            startTripDate.toString(),
                            /*binding.destDate.text.toString()*/"",
                            /*binding.truckAvg.text.toString()*/"",
                            randomNumber.toString(),
                            /*binding.driverIncome.text.toString()*/"",
                            creditList as List<Income>,
                            debitList as List<Expense>
                        )
                    )

                    val tripDataList: ArrayList<TripDataTestModel> = testList

                    val tripDataEntities = tripDataList.map {
                        Products(
                            truckNumber = it.truckNumber,
                            srcPlace = it.srcPlace,
                            destPlace = it.destPlace,
                            srcDate = it.srcDate,
                            destDate = it.destDate,
                            avg = it.avg,
                            randomNumber = randomNumber.toString(),
                            modelList1 = Gson().toJson(it.modelList1),
                            modelList2 = Gson().toJson(it.modelList2)
                        )
                    }
                    Log.d("flagVal", "setAllData: update")
                    val db = AppDatabase.getDatabase(applicationContext)
                    val tripDataDao = db.productsDao()
                    tripDataEntities.forEach {
                        tripDataDao.update(
                            it.randomNumber,
                            it.truckNumber,
                            it.srcPlace,
                            it.destPlace,
                            it.srcDate,
                            it.destDate,
                            it.avg,
                            it.modelList1,
                            it.modelList2
                        )
                    }
                    Constants.refreshApiGet(Event(1))
                } else {
                    testList.add(
                        TripDataTestModel(
                            truckNumberGiven.toString(),
                            binding.srcName.text.toString(),
                            binding.dest.text.toString(),
                            startTripDate.toString(),
                            /*binding.destDate.text.toString()*/"",
                            /*binding.truckAvg.text.toString()*/"",
                            randomNumber.toString(),
                            /*binding.driverIncome.text.toString()*/"",
                            creditList as List<Income>,
                            debitList as List<Expense>
                        )
                    )

                    val tripDataList: ArrayList<TripDataTestModel> = testList

                    val tripDataEntities = tripDataList.map {
                        Products(
                            truckNumber = it.truckNumber,
                            srcPlace = it.srcPlace,
                            destPlace = it.destPlace,
                            srcDate = it.srcDate,
                            destDate = it.destDate,
                            avg = it.avg,
                            randomNumber = randomNumber.toString(),
                            modelList1 = Gson().toJson(it.modelList1),
                            modelList2 = Gson().toJson(it.modelList2)
                        )
                    }

                    val db = AppDatabase.getDatabase(applicationContext)
                    val tripDataDao = db.productsDao()
                    tripDataEntities.forEach { tripDataDao.insert(it) }
                    Constants.refreshApiGet(Event(1))
                }
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
            modelList1 = gson.fromJson(
                entity.modelList1,
                object : TypeToken<List<CreditModel>>() {}.type
            ),
            modelList2 = gson.fromJson(
                entity.modelList2,
                object : TypeToken<List<DebitModel>>() {}.type
            )
        )
    }

    private suspend fun getAllData(context: Context): List<TripDataTestModel> {
        val database = AppDatabase.getDatabase(context)
        val tripDataDao = database.productsDao()
        val entities = tripDataDao.getAllProducts()
        return entities.map {
            convertToModel(it)
        }
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

    override fun clickCreditEditMethod(position: Int) {
        val income = creditList[position]
        addIncomeBottomSheetFragment = AddIncomeBottomSheetFragment(
            incomeDesc = income.desc ?: "",
            incomeAmount = income.amount ?: "",
            note = income.note ?: "",
            place = income.place ?: "",
            date = income.date ?: "",
            position = position
        )
        addIncomeBottomSheetFragment.show(supportFragmentManager, "AddIncomeBottomSheet")
    }

    override fun clickCreditDeleteMethod(position: Int) {
        deleteDialogFragment = DeleteDialogFragment(position, "Credit")
        deleteDialogFragment.show(supportFragmentManager, "")
    }

    override fun clickDebitDeleteMethod(position: Int) {
        deleteDialogFragment = DeleteDialogFragment(position, "Debit")
        deleteDialogFragment.show(supportFragmentManager, "")
    }

    override fun clickDebitEditMethod(position: Int) {
        val expense = debitList[position]
        // Check if it's a fuel entry
        if (expense.type == "Fuel") {
            addFuelBottomSheetFragment = AddFuelBottomSheetFragment(
                fuelDesc = expense.desc,
                fuelAmount = expense.amount,
                fuelLiters = expense.liters ?: "",
                fuelKm = expense.km ?: "",
                fuelPlace = expense.place ?: "",
                fuelDate = expense.date ?: "",
                position = position
            )
            addFuelBottomSheetFragment.show(supportFragmentManager, "AddFuelBottomSheet")
        } else {
            // Regular expense entry
            addExpenseBottomSheetFragment = AddExpenseBottomSheetFragment(
                expenseDesc = expense.desc ?: "",
                expenseAmount = expense.amount,
                expenseNote = expense.note ?: "",  // Handle null
                expensePlace = expense.place ?: "",  // Handle null
                expenseDate = expense.date ?: "",  // Handle null
                expenseType = expense.type ?: "",  // Handle null
                position = position
            )
            addExpenseBottomSheetFragment.show(supportFragmentManager, "AddExpenseBottomSheet")
        }
    }

    fun debitDeleteHissab(position: Int) {
        debitList.removeAt(position)
        debitAdapter.submitList(debitList)
        debitAdapter.notifyDataSetChanged()
        debitIncomeUpdate()
        databaseAddFlag = false
    }

    fun creditDeleteHissab(position: Int) {
        creditList.removeAt(position)
        creditAdapter.submitList(creditList)
        creditAdapter.notifyDataSetChanged()
        creditIncomeUpdate()
        databaseAddFlag = false
    }

    fun debitDataUpdate() {
        debitAdapter.submitList(debitList)
        binding.debitAmount.adapter = debitAdapter
        debitAdapter.notifyDataSetChanged()
        debitIncomeUpdate()
        // Recalculate average when fuel entries are updated
        calculateFinalTripAverage()
    }

    fun creditDataUpdate() {
        creditAdapter.submitList(creditList)
        binding.creditAmount.adapter = creditAdapter
        creditAdapter.notifyDataSetChanged()
        creditIncomeUpdate()
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
            if (startOdometerReading.isNotEmpty() && endOdometerReading.isNotEmpty()) {
                val startOdo = startOdometerReading.toDoubleOrNull() ?: 0.0
                val endOdo = endOdometerReading.toDoubleOrNull() ?: 0.0

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
            if (startOdometerReading.isEmpty()) {
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
                    val lastOdometer = if (endOdometerReading.isNotEmpty()) {
                        endOdometerReading.toDoubleOrNull() ?: fuelEntriesWithOdometer.last().km.toDoubleOrNull() ?: 0.0
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
        totalamount = 0
        for (item in creditList) {
            totalamount += item.amount.toInt()
        }
        binding.totalIncome.text = getString(R.string.ruppe) + totalamount.toString()
        binding.totalIncomeNew.text = totalamount.toString()

        val total = binding.totalIncomeNew.text.toString()
            .toDouble() - if (binding.totalExpanseNew.text.toString()
                .isNullOrEmpty()
        ) 0.0 else binding.totalExpanseNew.text.toString().toDouble()
        binding.totalProfit.text = "$total"
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

        tripEndDialogFragment = TripEndDialogFragment(
            startDate = startTripDate ?: "",
            existingEndDate = endTripDate ?: "",
            existingDriverIncome = driverIncomeAmount ?: "",
            existingEndOdometer = endOdometerReading,  // NEW: Pass existing end odometer
            existingEndKm = endManualKm,               // NEW: Pass existing manual KM
        ) { endDate, driverIncome, totalDays, endOdometer, endKm, isOdometerMode ->
            // Update values
            totalDaysTrip = totalDays
            endTripDate = endDate
            driverIncomeAmount = driverIncome
            endOdometerReading = endOdometer  // NEW: Store end odometer
            endManualKm = endKm               // NEW: Store manual KM
            endKmIsOdometerMode = isOdometerMode  // NEW: Store mode

            generateTripPdf(endDate, driverIncome)
        }
        tripEndDialogFragment.show(supportFragmentManager, "TripEndDialog")
        tripEndDialogFragment.isCancelable = false
    }

    /**
     * Generate PDF with collected end date and driver income
     */
    private fun generateTripPdf(endDate: String, driverIncome: String) {
        // Calculate final comprehensive average
        val calculatedAverage = calculateFinalTripAverage()

        // Generate PDF with collected data
        authViewModel.getTripPdf(
            AddTripRequestModel(
                id,
                binding.dest.text.toString(),
                driverIncome,
                endDate,
                debitList as List<Expense>,
                creditList as List<Income>,
                binding.totalProfit.text.toString(),
                binding.srcName.text.toString(),
                startTripDate.toString(),
                totalDaysTrip,
                binding.totalExpanseNew.text.toString(),
                binding.totalIncomeNew.text.toString(),
                calculatedAverage,  // Use calculated average
                truckNumberGiven.toString()
            )
        )
        showProgress()
    }



}
