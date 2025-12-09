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
    var startTripDate: String? =""
    var truckNumberGiven: String? =""
    var driverIncome = "0"
    var totalDaysTrip = "0"
    var id = "0"

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
        Constants.setStatusBar(this, isLight = true, colorRes = R.color.white)
        setLanguage()
        initViews()
        setOnclickListner()
        setObserver()
        MobileAds.initialize(this) {}

        // 2. Pehle se ek ad load kar lo
        loadRewardedAd()
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
            randomNumberGenerate()
            callDialog("", "", "", "", "", "", "", "")
        } else {
            randomNumberGenerate()
            val truckNumber = intent.getStringExtra("truckNumber")
            val srcPlace = intent.getStringExtra("sourceName")
            val driverAvak = intent.getStringExtra("driverAvak")
            val destPlace = intent.getStringExtra("destinationName")
            val startDate = intent.getStringExtra("startingDate")
            startTripDate=startDate
            truckNumberGiven=truckNumber
            val endDate = intent.getStringExtra("endingDate")
            id = intent.getStringExtra("id").toString()
            Log.d("TAG1233", "initViews: $id")
            binding.truckNumber.text =truckNumber.toString()
            binding.srcName.text = srcPlace.toString()
//            binding.driverIncome.text = driverAvak.toString()
            binding.dest.text = destPlace.toString()
            binding.srcDate.text = startDate.toString()
//            binding.destDate.text = endDate.toString()
            if (creditList.size > 0) {
                creditIncomeUpdate()

            }
            Log.d("debit", "initViews: ${Constants.debitList}")
            if (debitList.size > 0) {
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
        tripDialogFragment = TripDialogFragment(date, startingPlace, endingPlace, driverIncome, truckNumber, truckAvg, totalDays, update)
        tripDialogFragment.show(supportFragmentManager, "")
        tripDialogFragment.isCancelable = false
    }

    fun getDataFill(
        truckNumber: String, srcPlaceValue: String, destPlaceValue: String, startDate: String,
        endDate: String, truckAvg: String = "", driverTripAvak: String, totalDays: String = "0"
    ) {
        binding.truckNumber.text = truckNumber.toString()
        binding.srcName.text = srcPlaceValue
        binding.dest.text = destPlaceValue
        binding.srcDate.text = startDate
        startTripDate=startDate
        truckNumberGiven=truckNumber
//        binding.destDate.text = endDate
//        binding.truckAvg.text = truckAvg.toString()
//        binding.driverIncome.text= driverTripAvak
        totalDaysTrip = totalDays
    }

    private fun setObserver() {
        Constants.events.observe(this) { it ->
            it.getContentIfNotHandled()?.let { event ->
                event.let {
                    if (it.amount.toInt() >= 0 && !it.desc.toString().isNullOrEmpty()) {
                        val income = Income(
                            desc = it.desc,
                            amount = it.amount,
                            totalIncome = it.totalIncome,
                            advanceTaken = it.advanceTaken,
                            balance = it.balance,
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
        binding.add.setOnClickListener(this)
        binding.backBtn.setOnClickListener(this)
        binding.done.setOnClickListener(this)
        binding.editLayout.setOnClickListener(this)
        binding.cardFood.setOnClickListener(this)
        binding.cardToll.setOnClickListener(this)
        binding.cardDiesel.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.add -> {
                addTaskDialogFragment = AddTaskDialogFragment("", "", "", 0)
                addTaskDialogFragment.show(supportFragmentManager, "")
            }

            binding.cardFood -> {
                addExpenseBottomSheetFragment = AddExpenseBottomSheetFragment()
                addExpenseBottomSheetFragment.show(supportFragmentManager, "AddExpenseBottomSheet")
            }

            binding.cardToll -> {
                addIncomeBottomSheetFragment = AddIncomeBottomSheetFragment()
                addIncomeBottomSheetFragment.show(supportFragmentManager, "AddIncomeBottomSheet")
            }

            binding.cardDiesel -> {
                addFuelBottomSheetFragment = AddFuelBottomSheetFragment()
                addFuelBottomSheetFragment.show(supportFragmentManager, "AddFuelBottomSheet")
            }

            binding.editLayout -> {
                callDialog(
                    "${startTripDate} to ${startTripDate}",
                    startingPlace = binding.srcName.text.toString(),
                    endingPlace = binding.dest.text.toString(),
                    driverIncome = "",
                    truckNumber = truckNumberGiven.toString(),
                    truckAvg = "",
                    totalDays = totalDaysTrip, "yes"
                )
            }

            binding.backBtn -> {
                onBackPressed()
            }

            binding.done -> {
                tripName = binding.srcName.text.toString() + " TO " + binding.dest.text.toString()
                // Show dialog to collect Trip End Date and Driver Income
                showTripEndDialog()
            }

        }
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
                            it.randomNumber.toInt(),
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
            incomeDesc = income.desc,
            incomeAmount = income.amount,
            totalIncome = income.totalIncome,
            advanceTaken = income.advanceTaken,
            balance = income.balance,
            note = income.note,
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
                fuelLiters = expense.liters,
                fuelKm = expense.km,
                fuelPlace = expense.place,
                fuelDate = expense.date,
                position = position
            )
            addFuelBottomSheetFragment.show(supportFragmentManager, "AddFuelBottomSheet")
        } else {
            // Regular expense entry
            addExpenseBottomSheetFragment = AddExpenseBottomSheetFragment(
                expenseDesc = expense.desc,
                expenseAmount = expense.amount,
                expenseNote = expense.note,
                expensePlace = expense.place,
                expenseDate = expense.date,
                expenseType = expense.type,
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
    }

    fun creditDeleteHissab(position: Int) {
        creditList.removeAt(position)
        creditAdapter.submitList(creditList)
        creditAdapter.notifyDataSetChanged()
        creditIncomeUpdate()
    }

    fun debitDataUpdate() {
        debitAdapter.submitList(debitList)
        binding.debitAmount.adapter = debitAdapter
        debitAdapter.notifyDataSetChanged()
        debitIncomeUpdate()
        // Recalculate average when fuel entries are updated
        calculateTripAverage()
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

    /**
     * Calculate trip average from fuel entries
     * Average = Total KM / Total Liters
     * For odometer readings: Total KM = (Max KM - Min KM) if multiple entries, or use KM if single entry
     * Returns average as string, or empty string if no valid fuel entries
     */
    private fun calculateTripAverage(): String {
        // Get all fuel entries with valid liters
        val fuelEntries = debitList.filter { 
            it.type == "Fuel" && it.liters.isNotEmpty() && it.liters.toDoubleOrNull() ?: 0.0 > 0
        }

        if (fuelEntries.isEmpty()) {
            return ""
        }

        // Calculate total liters
        var totalLiters = 0.0
        val kmReadings = mutableListOf<Double>()

        for (fuel in fuelEntries) {
            val liters = fuel.liters.toDoubleOrNull() ?: 0.0
            if (liters > 0) {
                totalLiters += liters
            }
            
            // Collect KM readings
            val km = fuel.km.toDoubleOrNull()
            if (km != null && km > 0) {
                kmReadings.add(km)
            }
        }

        if (totalLiters <= 0) {
            return ""
        }

        // Calculate total KM
        var totalKm = 0.0
        if (kmReadings.isNotEmpty()) {
            if (kmReadings.size == 1) {
                // Single entry: use the KM reading as total distance
                totalKm = kmReadings[0]
            } else {
                // Multiple entries: use difference between max and min (trip distance)
                val minKm = kmReadings.minOrNull() ?: 0.0
                val maxKm = kmReadings.maxOrNull() ?: 0.0
                totalKm = maxKm - minKm
            }
        }

        // Calculate average: KM per Liter
        if (totalKm > 0 && totalLiters > 0) {
            val average = totalKm / totalLiters
            // Format to 2 decimal places
            val formattedAverage = String.format("%.2f", average)
            return formattedAverage
        }

        return ""
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
        tripEndDialogFragment = TripEndDialogFragment { endDate, driverIncome ->
            // Data collected, now show ads
            showAdThen {
                // After ads are closed, generate PDF with collected data
                generateTripPdf(endDate, driverIncome)
            }
        }
        tripEndDialogFragment.show(supportFragmentManager, "TripEndDialog")
        tripEndDialogFragment.isCancelable = false
    }

    /**
     * Generate PDF with collected end date and driver income
     */
    private fun generateTripPdf(endDate: String, driverIncome: String) {
        // Calculate average from fuel entries
        val calculatedAverage = calculateTripAverage()
        
        // Generate PDF with collected data
        authViewModel.getTripPdf(
            AddTripRequestModel(
                id,
                binding.dest.text.toString(),
                driverIncome, // Use collected driver income
                endDate, // Use collected end date
                debitList as List<Expense>,
                creditList as List<Income>,
                binding.totalProfit.text.toString(),
                binding.srcName.text.toString(),
                startTripDate.toString(),
                totalDaysTrip,
                binding.totalExpanseNew.text.toString(),
                binding.totalIncomeNew.text.toString(),
                calculatedAverage, // Use calculated average from fuel entries
                truckNumberGiven.toString()
            )
        )
        showProgress()
    }
}
