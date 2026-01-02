package com.dadabarbie.TruckTrip.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dadabarbie.TruckTrip.R
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager

import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.adapter.PreviewIncomeAdapter
import com.dadabarbie.TruckTrip.adapter.PreviewExpenseAdapter
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityTripPreviewBinding
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class TripPreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityTripPreviewBinding
    private val authViewModel: AuthViewModel by viewModels()
    private var tripId: String = ""
    private var tripName: String = ""

    private lateinit var incomeAdapter: PreviewIncomeAdapter
    private lateinit var expenseAdapter: PreviewExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            // 35 (android - 15)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        }
        initAdapters()
        // Get data from intent
        getTripDataFromIntent()

        // Setup UI
        setupToolbar()

        setClickListeners()
        setObservers()
    }

    private fun getTripDataFromIntent() {
        tripId = intent.getStringExtra("TRIP_ID") ?: ""

        val source = intent.getStringExtra("SOURCE") ?: ""
        val destination = intent.getStringExtra("DESTINATION") ?: ""
        val startDate = intent.getStringExtra("START_DATE") ?: ""
        val endDate = intent.getStringExtra("END_DATE") ?: ""
        val truckNo = intent.getStringExtra("TRUCK_NO") ?: ""
        val driverIncome = intent.getStringExtra("DRIVER_INCOME") ?: "0"
        val totalIncome = intent.getStringExtra("TOTAL_INCOME") ?: "0"
        val totalExpense = intent.getStringExtra("TOTAL_EXPENSE") ?: "0"
        val ownerProfit = intent.getStringExtra("OWNER_PROFIT") ?: "0"
        val truckAverage = intent.getStringExtra("TRUCK_AVERAGE") ?: "0"
        val totalDays = intent.getStringExtra("TOTAL_DAYS") ?: "0"

        val routeJson = intent.getStringExtra("ROUTE_JSON")
        val incomeJson = intent.getStringExtra("INCOME_JSON")
        val expenseJson = intent.getStringExtra("EXPENSE_JSON")

        tripName = "$source TO $destination"

        // Set basic info
        binding.apply {
            tvTripTitle.text = tripName
            tvTruckNumber.text = truckNo
            tvStartDate.text = formatDate(startDate)
            tvEndDate.text = formatDate(endDate)
            tvTotalDays.text = "$totalDays Days"
            tvTruckAverage.text = "$truckAverage km/l"

            // Route
            if (!routeJson.isNullOrEmpty()) {
                try {
                    val route: ArrayList<String> = Gson().fromJson(
                        routeJson,
                        object : TypeToken<ArrayList<String>>() {}.type
                    )
                    if (route.size > 2) {
                        tvRoute.visibility = View.VISIBLE
                        tvRoute.text = route.joinToString(" → ")
                    } else {
                        tvRoute.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    tvRoute.visibility = View.GONE
                }
            } else {
                tvRoute.visibility = View.GONE
            }

            // Financial summary
            tvTotalIncome.text = "₹$totalIncome"
            tvTotalExpense.text = "₹$totalExpense"
            tvDriverIncome.text = "₹$driverIncome"
            tvOwnerProfit.text = "₹$ownerProfit"

            // Set profit color
            val profit = ownerProfit.toDoubleOrNull() ?: 0.0
//            if (profit >= 0) {
//                tvOwnerProfit.setTextColor(getColor(R.color.green))
//            } else {
//                tvOwnerProfit.setTextColor(getColor(R.color.tamil_txt))
//            }
        }
        Log.d("IncomeJson", "getTripDataFromIntent: ${incomeJson}")

        // Parse and set income list
        if (!incomeJson.isNullOrEmpty()) {
            try {
                val incomeList: List<Income> = Gson().fromJson(
                    incomeJson,
                    object : TypeToken<List<Income>>() {}.type
                )

                incomeAdapter.submitList(incomeList)

                if (incomeList.isEmpty()) {
                    binding.tvNoIncome.visibility = View.VISIBLE
                    binding.rvIncome.visibility = View.GONE
                } else {
                    binding.tvNoIncome.visibility = View.GONE
                    binding.rvIncome.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("TripPreview", "Error parsing income: ${e.message}")
            }
        }

        // Parse and set expense list
        if (!expenseJson.isNullOrEmpty()) {
            try {
                val expenseList: List<Expense> = Gson().fromJson(
                    expenseJson,
                    object : TypeToken<List<Expense>>() {}.type
                )
                expenseAdapter.submitList(expenseList)

                if (expenseList.isEmpty()) {
                    binding.tvNoExpense.visibility = View.VISIBLE
                    binding.rvExpense.visibility = View.GONE
                } else {
                    binding.tvNoExpense.visibility = View.GONE
                    binding.rvExpense.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("TripPreview", "Error parsing expense: ${e.message}")
            }
        }
    }

    private fun setupToolbar() {
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun initAdapters() {
        incomeAdapter = PreviewIncomeAdapter()
        binding.rvIncome.apply {
            layoutManager = LinearLayoutManager(this@TripPreviewActivity)
            adapter = incomeAdapter
            isNestedScrollingEnabled = false
        }

        expenseAdapter = PreviewExpenseAdapter()
        binding.rvExpense.apply {
            layoutManager = LinearLayoutManager(this@TripPreviewActivity)
            adapter = expenseAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setClickListeners() {
        binding.btnDownload.setOnClickListener {
            downloadPdf()
        }
    }

    private fun setObservers() {
        authViewModel.downloadCompleted.observe(this, Observer { filePath ->
            Constants.dismissProgress()

            // Open the downloaded PDF
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    val file = java.io.File(filePath)
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this@TripPreviewActivity,
                        packageName,
                        file
                    )
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("TripPreview", "Error opening PDF: ${e.message}")
            }
        })
    }

    private fun downloadPdf() {
      showProgress()
        Constants.tripName = tripName
        authViewModel.getParticualrPdf(tripId)
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
}