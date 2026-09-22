package com.dadabarbie.TruckTrip.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.ads.AdMobManager
import com.dadabarbie.TruckTrip.databinding.ActivityTripEstimatorBinding
import java.util.Locale

class TripEstimatorActivity : BaseActivity() {

    private val binding: ActivityTripEstimatorBinding by lazy {
        ActivityTripEstimatorBinding.inflate(layoutInflater)
    }

    private var calculateClickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        setupListeners()
        populateDataFromIntent()
    }

    private fun populateDataFromIntent() {
        val distance = intent.getStringExtra("DISTANCE") ?: intent.getStringExtra("DISTANCE_ONE_WAY") ?: ""
        val mileage = intent.getStringExtra("MILEAGE") ?: ""
        val fuelPrice = intent.getStringExtra("FUEL_PRICE") ?: ""

        if (distance.isNotEmpty()) binding.etDistance.setText(distance)
        if (mileage.isNotEmpty()) binding.etMileage.setText(mileage)
        if (fuelPrice.isNotEmpty()) binding.etFuelPrice.setText(fuelPrice)

        if (distance.isNotEmpty() && mileage.isNotEmpty() && fuelPrice.isNotEmpty()) {
            calculateEstimate()
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCalculate.setOnClickListener {
            if (calculateEstimate()) {
                calculateClickCount++
                if (calculateClickCount % 2 == 0) {
                    AdMobManager.showInterstitialIfReady(this)
                }
            }
        }
    }

    private fun calculateEstimate(): Boolean {
        val distanceOneWayStr = binding.etDistance.text.toString().trim()
        val distanceReturnStr = binding.etReturnDistance.text.toString().trim()
        val mileageStr = binding.etMileage.text.toString().trim()
        val fuelPriceStr = binding.etFuelPrice.text.toString().trim()
        val otherExpStr = binding.etOtherExpenses.text.toString().trim()
        val freightOfferedStr = binding.etFreightOffered.text.toString().trim()
        val returnFreightStr = binding.etReturnFreight.text.toString().trim()

        if (distanceOneWayStr.isEmpty() || mileageStr.isEmpty() || fuelPriceStr.isEmpty()) {
            Toast.makeText(this, "Please enter Distance, Mileage, and Fuel Price", Toast.LENGTH_SHORT).show()
            return false
        }

        val distanceOneWay = distanceOneWayStr.toDoubleOrNull() ?: 0.0
        val distanceReturn = distanceReturnStr.toDoubleOrNull() ?: 0.0
        val totalDistance = distanceOneWay + distanceReturn

        val mileage = mileageStr.toDoubleOrNull() ?: 0.0
        val fuelPrice = fuelPriceStr.toDoubleOrNull() ?: 0.0
        val otherExpenses = otherExpStr.toDoubleOrNull() ?: 0.0

        val freightOffered = freightOfferedStr.toDoubleOrNull() ?: 0.0
        val returnFreight = returnFreightStr.toDoubleOrNull() ?: 0.0
        val totalFreight = freightOffered + returnFreight

        if (totalDistance <= 0.0) {
            Toast.makeText(this, "Total Distance must be greater than 0", Toast.LENGTH_SHORT).show()
            return false
        }

        if (mileage <= 0.0) {
            Toast.makeText(this, "Mileage must be greater than 0", Toast.LENGTH_SHORT).show()
            return false
        }

        val fuelNeededLiters = totalDistance / mileage
        val fuelCost = fuelNeededLiters * fuelPrice
        val totalExpenses = fuelCost + otherExpenses
        val suggestedMinFreight = totalExpenses * 1.20 // 20% margin
        val netProfit = totalFreight - totalExpenses

        binding.tvTotalDistance.text = String.format(Locale.getDefault(), "%,.1f km", totalDistance)
        binding.tvTotalFreight.text = String.format(Locale.getDefault(), "₹ %,.2f", totalFreight)
        binding.tvFuelNeeded.text = String.format(Locale.getDefault(), "%.1f Liters", fuelNeededLiters)
        binding.tvFuelCost.text = String.format(Locale.getDefault(), "₹ %,.2f", fuelCost)
        binding.tvTotalExpenses.text = String.format(Locale.getDefault(), "₹ %,.2f", totalExpenses)
        binding.tvMinFreight.text = String.format(Locale.getDefault(), "₹ %,.2f", suggestedMinFreight)

        if (netProfit >= 0) {
            binding.tvNetProfit.text = String.format(Locale.getDefault(), "+ ₹ %,.2f", netProfit)
            binding.tvNetProfit.setTextColor(resources.getColor(R.color.green, theme))
        } else {
            binding.tvNetProfit.text = String.format(Locale.getDefault(), "- ₹ %,.2f", Math.abs(netProfit))
            binding.tvNetProfit.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
        }

        binding.resultCard.visibility = View.VISIBLE
        return true
    }
}
