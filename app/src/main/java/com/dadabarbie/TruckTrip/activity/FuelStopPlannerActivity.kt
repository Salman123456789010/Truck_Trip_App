package com.dadabarbie.TruckTrip.activity

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.adapter.FuelStopAdapter
import com.dadabarbie.TruckTrip.ads.AdMobManager
import com.dadabarbie.TruckTrip.databinding.ActivityFuelStopPlannerBinding
import com.dadabarbie.TruckTrip.fuelplanner.model.FuelPlan
import com.dadabarbie.TruckTrip.fuelplanner.model.LoadCondition
import com.dadabarbie.TruckTrip.fuelplanner.model.ReachabilityStatus
import com.dadabarbie.TruckTrip.fuelplanner.service.FuelCalculationService
import com.dadabarbie.TruckTrip.fuelplanner.service.FuelStopPlanningService
import java.util.Locale

class FuelStopPlannerActivity : BaseActivity() {

    private val binding: ActivityFuelStopPlannerBinding by lazy {
        ActivityFuelStopPlannerBinding.inflate(layoutInflater)
    }

    private lateinit var fuelStopAdapter: FuelStopAdapter
    private var calculateActionCount = 0
    private var selectedSafetyReservePercent = 20.0
    private var selectedLoadCondition = LoadCondition.FULLY_LOADED
    private var currentPlan: FuelPlan? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        setupRecyclerView()
        setupListeners()
        setupChips()
        populateDataFromIntent()
    }

    private fun setupRecyclerView() {
        val currencySymbol = getString(R.string.ruppe)
        fuelStopAdapter = FuelStopAdapter(currencySymbol = currencySymbol)
        binding.rvFuelStops.apply {
            layoutManager = LinearLayoutManager(this@FuelStopPlannerActivity)
            adapter = fuelStopAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupChips() {
        // Safety Reserve Chips
        binding.chipGroupReserve.setOnCheckedChangeListener { _, checkedId ->
            selectedSafetyReservePercent = when (checkedId) {
                R.id.chipReserve10 -> 10.0
                R.id.chipReserve15 -> 15.0
                else -> 20.0
            }
        }

        // Load Condition Chips
        binding.chipGroupLoad.setOnCheckedChangeListener { _, checkedId ->
            selectedLoadCondition = when (checkedId) {
                R.id.chipEmpty -> LoadCondition.EMPTY
                R.id.chipHalfLoaded -> LoadCondition.HALF_LOADED
                else -> LoadCondition.FULLY_LOADED
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnInfoDialog.setOnClickListener {
            showSafetyReserveInfoDialog()
        }

        binding.btnExplainReserve.setOnClickListener {
            showSafetyReserveInfoDialog()
        }

        binding.btnFillTank.setOnClickListener {
            val tankCapacityStr = binding.etTankCapacity.text.toString().trim()
            if (tankCapacityStr.isNotEmpty()) {
                binding.etCurrentFuel.setText(tankCapacityStr)
                Toast.makeText(this, getString(R.string.toast_tank_full, tankCapacityStr), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.toast_enter_tank_first), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCalculate.setOnClickListener {
            if (calculateAndDisplayPlan()) {
                calculateActionCount++
                if (calculateActionCount % 2 == 0) {
                    AdMobManager.showInterstitialIfReady(this)
                }
            }
        }

        binding.btnSendToEstimator.setOnClickListener {
            val distance = binding.etDistance.text.toString().trim()
            val mileage = binding.etMileage.text.toString().trim()
            val fuelPrice = binding.etFuelPrice.text.toString().trim()

            val intent = Intent(this, TripEstimatorActivity::class.java).apply {
                putExtra("DISTANCE", distance)
                putExtra("MILEAGE", mileage)
                putExtra("FUEL_PRICE", fuelPrice)
            }
            startActivity(intent)
        }
    }

    private fun populateDataFromIntent() {
        val startLocation = intent.getStringExtra("SOURCE")
            ?: intent.getStringExtra("START_LOCATION")
            ?: intent.getStringExtra("source")
            ?: ""
        val destinationLocation = intent.getStringExtra("DESTINATION")
            ?: intent.getStringExtra("destination")
            ?: ""
        val distance = intent.getStringExtra("DISTANCE")
            ?: intent.getStringExtra("total_distance")
            ?: ""
        val truckNumber = intent.getStringExtra("TRUCK_NO")
            ?: intent.getStringExtra("truck_no")
            ?: intent.getStringExtra("TRUCK_NUMBER")
            ?: ""
        val truckAverage = intent.getStringExtra("TRUCK_AVERAGE")
            ?: intent.getStringExtra("truck_average")
            ?: ""
        val fuelPrice = intent.getStringExtra("FUEL_PRICE") ?: ""

        if (startLocation.isNotEmpty()) binding.etStartLocation.setText(startLocation)
        if (destinationLocation.isNotEmpty()) binding.etDestinationLocation.setText(destinationLocation)
        if (distance.isNotEmpty()) binding.etDistance.setText(distance)
        if (truckNumber.isNotEmpty()) binding.etTruckNumber.setText(truckNumber)
        if (truckAverage.isNotEmpty()) binding.etMileage.setText(truckAverage)
        if (fuelPrice.isNotEmpty()) binding.etFuelPrice.setText(fuelPrice)

        if (startLocation.isNotEmpty() && destinationLocation.isNotEmpty()) {
            binding.tvHeaderRoute.text = "$startLocation → $destinationLocation"
        }

        // Default tank capacity suggestion if empty
        if (binding.etTankCapacity.text.isNullOrEmpty()) {
            binding.etTankCapacity.setText("300")
        }

        // If distance and mileage exist, auto-calculate immediately
        if (distance.isNotEmpty() && truckAverage.isNotEmpty()) {
            binding.etCurrentFuel.setText("150")
            calculateAndDisplayPlan()
        }
    }

    private fun calculateAndDisplayPlan(): Boolean {
        val startLocation = binding.etStartLocation.text.toString().trim().ifEmpty { "Trip Start" }
        val destinationLocation = binding.etDestinationLocation.text.toString().trim().ifEmpty { "Destination" }
        val distanceStr = binding.etDistance.text.toString().trim()
        val truckNumber = binding.etTruckNumber.text.toString().trim()
        val currentFuelStr = binding.etCurrentFuel.text.toString().trim()
        val tankCapacityStr = binding.etTankCapacity.text.toString().trim()
        val mileageStr = binding.etMileage.text.toString().trim()
        val fuelPriceStr = binding.etFuelPrice.text.toString().trim()

        val distance = distanceStr.toDoubleOrNull()
        val currentFuel = currentFuelStr.toDoubleOrNull()
        val tankCapacity = tankCapacityStr.toDoubleOrNull()
        val mileage = mileageStr.toDoubleOrNull()
        val fuelPrice = fuelPriceStr.toDoubleOrNull()

        // Validation using FuelCalculationService
        val validation = FuelCalculationService.validateInputs(
            distanceKm = distance,
            averageMileageKmPerL = mileage,
            currentFuelLiters = currentFuel,
            tankCapacityLiters = tankCapacity,
            fuelPricePerLiter = fuelPrice,
            safetyReservePercent = selectedSafetyReservePercent
        )

        if (!validation.isValid) {
            val msg = if (validation.errorRes != 0) getString(validation.errorRes) else (validation.errorMessage ?: "Invalid input")
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return false
        }

        val plan = FuelStopPlanningService.planFuelTrip(
            startLocation = startLocation,
            destinationLocation = destinationLocation,
            totalDistanceKm = distance!!,
            currentFuelLiters = currentFuel!!,
            tankCapacityLiters = tankCapacity!!,
            averageMileageKmPerL = mileage!!,
            fuelPricePerLiter = fuelPrice,
            safetyReservePercent = selectedSafetyReservePercent,
            loadCondition = selectedLoadCondition,
            truckNumber = truckNumber.ifEmpty { null }
        )
        currentPlan = plan

        displayPlanResults(plan)
        return true
    }

    private fun displayPlanResults(plan: FuelPlan) {
        val currencySymbol = getString(R.string.ruppe)

        // Route header text
        binding.tvHeaderRoute.text = "${plan.startLocation} → ${plan.destinationLocation} (${String.format(Locale.getDefault(), "%,.1f km", plan.totalDistanceKm)})"

        // 1. Reachability Status
        renderReachabilityCard(plan.reachabilityStatus, plan)

        // 2. Recommendation
        if (plan.reachabilityStatus == ReachabilityStatus.SAFE) {
            binding.tvRecommendedRefuelDistance.text = getString(R.string.no_stops_needed_msg)
            binding.tvRecommendedFuelQuantity.text = getString(R.string.enough_fuel_margin_msg, plan.safetyReservePercent.toInt())
        } else {
            val distMin = plan.recommendedRefuelDistanceMinKm ?: 0.0
            val distMax = plan.recommendedRefuelDistanceMaxKm ?: 0.0
            binding.tvRecommendedRefuelDistance.text = String.format(
                Locale.getDefault(),
                "⛽ %s",
                getString(R.string.refuel_around_format, distMin, distMax)
            )
            binding.tvRecommendedFuelQuantity.text = getString(
                R.string.recommended_fuel_to_add_qty,
                plan.recommendedFuelQuantityLiters
            )
        }

        // 3. Key Metrics
        binding.tvSafeRange.text = String.format(Locale.getDefault(), "%,.1f km", plan.safeRangeKm)
        binding.tvTheoreticalRange.text = String.format(Locale.getDefault(), "%,.1f km", plan.theoreticalRangeKm)
        binding.tvEstimatedFuel.text = String.format(Locale.getDefault(), "%,.1f L", plan.estimatedFuelRequiredLiters)
        binding.tvFuelWithSafety.text = String.format(Locale.getDefault(), "%,.1f L", plan.fuelWithSafetyLiters)

        val remaining = plan.fuelRemainingAtDestinationLiters
        if (remaining >= 0) {
            binding.tvFuelRemainingAtDest.text = String.format(Locale.getDefault(), "%,.1f L", remaining)
            binding.tvFuelRemainingAtDest.setTextColor(
                if (plan.reachabilityStatus == ReachabilityStatus.SAFE) Color.parseColor("#0E8A37")
                else Color.parseColor("#D35400")
            )
        } else {
            binding.tvFuelRemainingAtDest.text = String.format(Locale.getDefault(), "- %,.1f L (%s)", Math.abs(remaining), getString(R.string.fuel_shortage_label))
            binding.tvFuelRemainingAtDest.setTextColor(Color.parseColor("#C0392B"))
        }

        // 4. Cost Card
        if (plan.estimatedFuelCost != null && plan.estimatedFuelCost > 0.0) {
            binding.cardCost.visibility = View.VISIBLE
            binding.tvTotalFuelCost.text = String.format(
                Locale.getDefault(),
                "%s %,.2f",
                currencySymbol,
                plan.estimatedFuelCost
            )
            val costPerKm = plan.fuelCostPerKm ?: 0.0
            binding.tvCostPerKm.text = String.format(
                Locale.getDefault(),
                "%s %,.2f / km",
                currencySymbol,
                costPerKm
            )
        } else {
            binding.cardCost.visibility = View.VISIBLE
            binding.tvTotalFuelCost.text = getString(R.string.fuel_price_not_provided)
            binding.tvCostPerKm.text = "N/A"
        }

        // 5. Stops Itinerary
        if (plan.fuelStops.isNotEmpty()) {
            binding.tvStopsHeader.text = getString(R.string.recommended_fuel_stops_count, plan.fuelStops.size)
            binding.tvNoStopsMessage.visibility = View.GONE
            binding.rvFuelStops.visibility = View.VISIBLE
            fuelStopAdapter.updateData(plan.fuelStops, currencySymbol)
        } else {
            binding.tvStopsHeader.text = getString(R.string.fuel_stop_timeline)
            binding.tvNoStopsMessage.visibility = View.VISIBLE
            binding.rvFuelStops.visibility = View.GONE
        }

        binding.resultContainer.visibility = View.VISIBLE
    }

    private fun renderReachabilityCard(status: ReachabilityStatus, plan: FuelPlan) {
        val title = if (status.titleRes != 0) getString(status.titleRes) else status.title
        val desc = if (status.descriptionRes != 0) getString(status.descriptionRes) else status.description
        when (status) {
            ReachabilityStatus.SAFE -> {
                binding.cardReachabilityStatus.setCardBackgroundColor(Color.parseColor("#EAF7EE"))
                binding.cardReachabilityStatus.strokeColor = Color.parseColor("#27AE60")
                binding.tvStatusBadge.text = "🟢 $title"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#1E824C"))
                binding.tvStatusDescription.text = desc
                binding.tvStatusDescription.setTextColor(Color.parseColor("#1E824C"))
            }
            ReachabilityStatus.REFUEL_RECOMMENDED -> {
                binding.cardReachabilityStatus.setCardBackgroundColor(Color.parseColor("#FFF8E7"))
                binding.cardReachabilityStatus.strokeColor = Color.parseColor("#F39C12")
                binding.tvStatusBadge.text = "🟠 $title"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#D35400"))
                binding.tvStatusDescription.text = desc
                binding.tvStatusDescription.setTextColor(Color.parseColor("#8E44AD"))
            }
            ReachabilityStatus.REFUEL_REQUIRED -> {
                binding.cardReachabilityStatus.setCardBackgroundColor(Color.parseColor("#FDEAEA"))
                binding.cardReachabilityStatus.strokeColor = Color.parseColor("#E74C3C")
                binding.tvStatusBadge.text = "🔴 $title"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#C0392B"))
                binding.tvStatusDescription.text = desc
                binding.tvStatusDescription.setTextColor(Color.parseColor("#C0392B"))
            }
        }
    }

    private fun showSafetyReserveInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.fuel_safety_reserve_dialog_title)
            .setMessage(R.string.fuel_safety_reserve_dialog_msg)
            .setPositiveButton(R.string.btn_got_it) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
