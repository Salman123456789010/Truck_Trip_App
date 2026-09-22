package com.dadabarbie.TruckTrip.activity

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.ads.AdMobManager
import com.dadabarbie.TruckTrip.backhaul.model.BackhaulCalculationResult
import com.dadabarbie.TruckTrip.backhaul.model.BackhaulEvaluationStatus
import com.dadabarbie.TruckTrip.backhaul.model.BackhaulPlan
import com.dadabarbie.TruckTrip.backhaul.service.BackhaulCalculatorService
import com.dadabarbie.TruckTrip.backhaul.util.BackhaulStorageHelper
import com.dadabarbie.TruckTrip.databinding.ActivityBackhaulCalculatorBinding
import com.dadabarbie.TruckTrip.fuelplanner.service.FuelCalculationService
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.vasyerp.cafvd.room.model.TripRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

class BackhaulCalculatorActivity : BaseActivity() {

    private val binding: ActivityBackhaulCalculatorBinding by lazy {
        ActivityBackhaulCalculatorBinding.inflate(layoutInflater)
    }

    private var isWithBackhaul: Boolean = false
    private var actionCount = 0
    private var lastResult: BackhaulCalculationResult? = null
    private var currentTripId: String? = null

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
        setupLiveExpenseCalculation()
        setupReturnDistanceSync()
        setupScenarioChips()
        populateDataFromIntent()

        // If intent had no data, load last plan if available or default sample
        if (binding.etOutboundDistance.text.isNullOrEmpty() && binding.etOutboundOrigin.text.isNullOrEmpty()) {
            loadInitialDefaults()
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnInfo.setOnClickListener {
            showInfoDialog()
        }

        binding.btnReset.setOnClickListener {
            resetToDefaults()
        }

        // Return type toggle chips
        binding.btnReturnEmpty.setOnClickListener {
            setReturnType(withBackhaul = false)
        }

        binding.btnReturnWithLoad.setOnClickListener {
            setReturnType(withBackhaul = true)
        }

        // Custom return expenses toggle
        binding.cbCustomReturnExpenses.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutCustomReturnExpenses.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateLiveTotalCost()
        }

        // Quick Fuel Calculator helper
        binding.btnQuickCalcFuel.setOnClickListener {
            showQuickFuelCalculatorDialog()
        }

        // Select from My Trips
        binding.btnSelectFromTrips.setOnClickListener {
            showSelectTripDialog()
        }

        // Calculate button
        binding.btnCalculate.setOnClickListener {
            if (calculateAndDisplayResults()) {
                actionCount++
                if (actionCount % 2 == 0) {
                    AdMobManager.showInterstitialIfReady(this)
                }
            }
        }

        // Preload Interstitial Ad for Save/Share actions
        AdMobManager.preloadInterstitial(this)

        // Save plan
        binding.btnSavePlan.setOnClickListener {
            if (lastResult == null) {
                Toast.makeText(this, "Please calculate the plan first before saving", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showProgress("Saving Plan... Please wait")
            AdMobManager.showInterstitialIfReady(this, force = true) {
                com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress()
                saveCurrentPlan()
            }
        }

        // Share summary
        binding.btnShareSummary.setOnClickListener {
            if (lastResult == null) {
                Toast.makeText(this, "Please calculate the plan first before sharing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showProgress("Preparing Summary... Please wait")
            AdMobManager.showInterstitialIfReady(this, force = true) {
                com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress()
                shareSummaryText()
            }
        }
    }



    private fun setReturnType(withBackhaul: Boolean) {
        isWithBackhaul = withBackhaul
        if (withBackhaul) {
            binding.btnReturnWithLoad.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.btnReturnWithLoad.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnReturnEmpty.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.btnReturnEmpty.setTextColor(0xFF4B5563.toInt())

            binding.layoutReturnRevenue.visibility = View.VISIBLE
            binding.layoutEmptyReturnNotice.visibility = View.GONE

            if (binding.etReturnRevenue.text.toString() == "0" || binding.etReturnRevenue.text.isNullOrEmpty()) {
                binding.etReturnRevenue.setText("25000")
            }
        } else {
            binding.btnReturnEmpty.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.btnReturnEmpty.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnReturnWithLoad.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.btnReturnWithLoad.setTextColor(0xFF4B5563.toInt())

            binding.layoutReturnRevenue.visibility = View.GONE
            binding.layoutEmptyReturnNotice.visibility = View.VISIBLE
            binding.etReturnRevenue.setText("0")
        }

        updateRouteSummaryBanner()
        if (binding.layoutResults.visibility == View.VISIBLE) {
            calculateAndDisplayResults()
        }
    }

    private fun setupReturnDistanceSync() {
        binding.etOutboundDistance.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // If return distance is empty or equals previous outbound, update it
                val outDist = s?.toString()?.trim().orEmpty()
                if (outDist.isNotEmpty() && (binding.etReturnDistance.text.isNullOrEmpty())) {
                    binding.etReturnDistance.setText(outDist)
                }
                updateRouteSummaryBanner()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etOutboundOrigin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                updateRouteSummaryBanner()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etOutboundDestination.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                updateRouteSummaryBanner()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupLiveExpenseCalculation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                updateLiveTotalCost()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etFuelCost.addTextChangedListener(watcher)
        binding.etTollCost.addTextChangedListener(watcher)
        binding.etDriverCost.addTextChangedListener(watcher)
        binding.etOtherCost.addTextChangedListener(watcher)
        binding.etReturnFuel.addTextChangedListener(watcher)
        binding.etReturnToll.addTextChangedListener(watcher)
    }

    private fun updateLiveTotalCost() {
        val fuel = binding.etFuelCost.text.toString().trim().toDoubleOrNull() ?: 0.0
        val toll = binding.etTollCost.text.toString().trim().toDoubleOrNull() ?: 0.0
        val driver = binding.etDriverCost.text.toString().trim().toDoubleOrNull() ?: 0.0
        val other = binding.etOtherCost.text.toString().trim().toDoubleOrNull() ?: 0.0

        val total = fuel + toll + driver + other
        binding.tvLiveTotalCost.text = String.format(Locale.getDefault(), "₹ %,.0f", total)
    }

    private fun updateRouteSummaryBanner() {
        val origin = binding.etOutboundOrigin.text.toString().trim().ifEmpty { "A" }
        val dest = binding.etOutboundDestination.text.toString().trim().ifEmpty { "B" }
        binding.tvRouteSummary.text = "$origin ➔ $dest ➔ $origin"
    }

    private fun populateDataFromIntent() {
        val source = intent.getStringExtra("SOURCE")
            ?: intent.getStringExtra("START_LOCATION")
            ?: intent.getStringExtra("source")
            ?: ""
        val destination = intent.getStringExtra("DESTINATION")
            ?: intent.getStringExtra("destination")
            ?: ""
        val distance = intent.getStringExtra("DISTANCE")
            ?: intent.getStringExtra("total_distance")
            ?: ""
        val revenue = intent.getStringExtra("REVENUE")
            ?: intent.getStringExtra("total_income")
            ?: ""
        val fuelCost = intent.getStringExtra("FUEL_COST") ?: intent.getStringExtra("fuel_cost") ?: ""
        val tollCost = intent.getStringExtra("TOLL_COST") ?: intent.getStringExtra("toll_cost") ?: ""
        val driverCost = intent.getStringExtra("DRIVER_COST") ?: intent.getStringExtra("driver_cost") ?: ""
        val otherCost = intent.getStringExtra("OTHER_COST") ?: intent.getStringExtra("other_cost") ?: ""
        currentTripId = intent.getStringExtra("TRIP_ID") ?: intent.getStringExtra("id")

        if (source.isNotEmpty()) binding.etOutboundOrigin.setText(source)
        if (destination.isNotEmpty()) binding.etOutboundDestination.setText(destination)
        if (distance.isNotEmpty()) {
            binding.etOutboundDistance.setText(distance)
            binding.etReturnDistance.setText(distance)
        }
        if (revenue.isNotEmpty()) binding.etOutboundRevenue.setText(revenue)
        if (fuelCost.isNotEmpty()) binding.etFuelCost.setText(fuelCost.toDoubleOrNull()?.let { "%.0f".format(it) } ?: fuelCost)
        if (tollCost.isNotEmpty()) binding.etTollCost.setText(tollCost.toDoubleOrNull()?.let { "%.0f".format(it) } ?: tollCost)
        if (driverCost.isNotEmpty()) binding.etDriverCost.setText(driverCost.toDoubleOrNull()?.let { "%.0f".format(it) } ?: driverCost)
        if (otherCost.isNotEmpty()) binding.etOtherCost.setText(otherCost.toDoubleOrNull()?.let { "%.0f".format(it) } ?: otherCost)

        updateRouteSummaryBanner()
        updateLiveTotalCost()

        if (distance.isNotEmpty() && revenue.isNotEmpty()) {
            // Auto calculate if valid initial values
            calculateAndDisplayResults()
        }
    }

    private fun loadInitialDefaults() {
        val lastPlan = BackhaulStorageHelper.getLastPlan(this)
        if (lastPlan != null) {
            binding.etOutboundOrigin.setText(lastPlan.outboundOrigin)
            binding.etOutboundDestination.setText(lastPlan.outboundDestination)
            binding.etOutboundDistance.setText(lastPlan.outboundDistanceKm.toString().removeSuffix(".0"))
            binding.etOutboundRevenue.setText(lastPlan.outboundRevenue.toString().removeSuffix(".0"))
            binding.etReturnDistance.setText(lastPlan.returnDistanceKm.toString().removeSuffix(".0"))
            setReturnType(lastPlan.isWithBackhaul)
            if (lastPlan.isWithBackhaul) {
                binding.etReturnRevenue.setText(lastPlan.returnRevenue.toString().removeSuffix(".0"))
            }
            if (lastPlan.fuelCost > 0) binding.etFuelCost.setText(lastPlan.fuelCost.toString().removeSuffix(".0"))
            if (lastPlan.tollCost > 0) binding.etTollCost.setText(lastPlan.tollCost.toString().removeSuffix(".0"))
            if (lastPlan.driverCost > 0) binding.etDriverCost.setText(lastPlan.driverCost.toString().removeSuffix(".0"))
            if (lastPlan.otherCost > 0) binding.etOtherCost.setText(lastPlan.otherCost.toString().removeSuffix(".0"))
            if (lastPlan.targetAdditionalProfit > 0) {
                binding.etDesiredAdditionalProfit.setText(lastPlan.targetAdditionalProfit.toString().removeSuffix(".0"))
            }
            calculateAndDisplayResults()
        } else {
            resetToDefaults()
        }
    }

    private fun resetToDefaults() {
        binding.etOutboundOrigin.setText("Ahmedabad")
        binding.etOutboundDestination.setText("Mumbai")
        binding.etOutboundDistance.setText("500")
        binding.etOutboundRevenue.setText("40000")
        binding.etReturnDistance.setText("500")
        setReturnType(withBackhaul = true)
        binding.etReturnRevenue.setText("25000")

        binding.etFuelCost.setText("22000")
        binding.etTollCost.setText("4000")
        binding.etDriverCost.setText("5000")
        binding.etOtherCost.setText("2000")
        binding.etDesiredAdditionalProfit.setText("10000")

        binding.cbCustomReturnExpenses.isChecked = false
        binding.etReturnFuel.setText("")
        binding.etReturnToll.setText("")

        updateRouteSummaryBanner()
        updateLiveTotalCost()
        calculateAndDisplayResults()
    }

    private fun calculateAndDisplayResults(): Boolean {
        val outDist = binding.etOutboundDistance.text.toString().trim().toDoubleOrNull()
        val retDist = binding.etReturnDistance.text.toString().trim().toDoubleOrNull()
            ?: outDist
            ?: 0.0
        val outRev = binding.etOutboundRevenue.text.toString().trim().toDoubleOrNull()
        val retRev = if (isWithBackhaul) binding.etReturnRevenue.text.toString().trim().toDoubleOrNull() ?: 0.0 else 0.0

        val fuel = binding.etFuelCost.text.toString().trim().toDoubleOrNull() ?: 0.0
        val toll = binding.etTollCost.text.toString().trim().toDoubleOrNull() ?: 0.0
        val driver = binding.etDriverCost.text.toString().trim().toDoubleOrNull() ?: 0.0
        val other = binding.etOtherCost.text.toString().trim().toDoubleOrNull() ?: 0.0
        val targetExtra = binding.etDesiredAdditionalProfit.text.toString().trim().toDoubleOrNull() ?: 0.0

        // Validation
        val validation = BackhaulCalculatorService.validateInputs(
            outboundDistanceKm = outDist,
            returnDistanceKm = retDist,
            outboundRevenue = outRev,
            returnRevenue = retRev,
            isWithBackhaul = isWithBackhaul,
            fuelCost = fuel,
            tollCost = toll,
            driverCost = driver,
            otherCost = other,
            desiredAdditionalProfit = targetExtra
        )

        if (!validation.isValid) {
            Toast.makeText(this, validation.errorMessage ?: "Please verify your input values", Toast.LENGTH_SHORT).show()
            return false
        }

        // Custom return cost if specified
        var explicitReturnCost: Double? = null
        if (binding.cbCustomReturnExpenses.isChecked) {
            val retFuel = binding.etReturnFuel.text.toString().trim().toDoubleOrNull() ?: 0.0
            val retToll = binding.etReturnToll.text.toString().trim().toDoubleOrNull() ?: 0.0
            if (retFuel > 0.0 || retToll > 0.0) {
                explicitReturnCost = retFuel + retToll
            }
        }

        // Perform calculation
        val result = BackhaulCalculatorService.computePlan(
            outboundDistanceKm = outDist!!,
            returnDistanceKm = retDist,
            outboundRevenue = outRev!!,
            isWithBackhaul = isWithBackhaul,
            returnRevenue = retRev,
            fuelCost = fuel,
            tollCost = toll,
            driverCost = driver,
            otherCost = other,
            explicitEmptyReturnCost = explicitReturnCost,
            targetAdditionalProfit = targetExtra
        )

        lastResult = result

        // 1. Hero Empty Return Cost
        binding.tvEmptyReturnCostHero.text = String.format(Locale.getDefault(), "₹ %,.0f", result.emptyReturnCost)
        binding.tvEmptyReturnCostDesc.text = getString(
            R.string.empty_return_cost_desc,
            String.format(Locale.getDefault(), "₹ %,.0f", result.emptyReturnCost)
        )

        // 2. Profit Improvement Hero
        val improvementSign = if (result.profitImprovement >= 0) "+" else "-"
        binding.tvProfitImprovementAmount.text = String.format(Locale.getDefault(), "%s₹ %,.0f", improvementSign, abs(result.profitImprovement))

        if (result.profitImprovementPercentage != null && result.profitImprovementPercentage > 0) {
            binding.tvProfitImprovementPercent.visibility = View.VISIBLE
            binding.tvProfitImprovementPercent.text = String.format(Locale.getDefault(), "+%.1f%%", result.profitImprovementPercentage)
        } else {
            binding.tvProfitImprovementPercent.visibility = View.GONE
        }

        // 3. Comparison Cards
        // Without Backhaul
        binding.tvEmptyRevenue.text = String.format(Locale.getDefault(), "₹ %,.0f", result.emptyReturnRevenue)
        binding.tvEmptyCost.text = String.format(Locale.getDefault(), "₹ %,.0f", result.totalTripCost)
        if (result.emptyReturnProfit >= 0) {
            binding.tvEmptyProfit.text = String.format(Locale.getDefault(), "₹ %,.0f", result.emptyReturnProfit)
            binding.tvEmptyProfit.setTextColor(ContextCompat.getColor(this, R.color.green))
        } else {
            binding.tvEmptyProfit.text = String.format(Locale.getDefault(), "-₹ %,.0f", abs(result.emptyReturnProfit))
            binding.tvEmptyProfit.setTextColor(Color.parseColor("#DC2626"))
        }
        binding.tvEmptyProfitPerKm.text = String.format(Locale.getDefault(), "₹ %,.2f/km", result.emptyReturnProfitPerKm)

        // With Backhaul
        binding.tvBackhaulTotalRev.text = String.format(Locale.getDefault(), "₹ %,.0f", result.backhaulTotalRevenue)
        binding.tvBackhaulCost.text = String.format(Locale.getDefault(), "₹ %,.0f", result.totalTripCost)
        if (result.backhaulProfit >= 0) {
            binding.tvBackhaulProfit.text = String.format(Locale.getDefault(), "₹ %,.0f", result.backhaulProfit)
            binding.tvBackhaulProfit.setTextColor(ContextCompat.getColor(this, R.color.green))
        } else {
            binding.tvBackhaulProfit.text = String.format(Locale.getDefault(), "-₹ %,.0f", abs(result.backhaulProfit))
            binding.tvBackhaulProfit.setTextColor(Color.parseColor("#DC2626"))
        }
        binding.tvBackhaulProfitPerKm.text = String.format(Locale.getDefault(), "₹ %,.2f/km", result.backhaulProfitPerKm)

        // 4. Decision Status Banner
        binding.tvStatusIcon.text = result.evaluation.iconEmoji
        binding.tvStatusTitle.text = if (result.evaluation.titleRes != 0) getString(result.evaluation.titleRes) else result.evaluation.title
        binding.tvStatusMessage.text = if (result.evaluation.messageRes != 0) getString(result.evaluation.messageRes) else result.evaluation.message

        when (result.evaluation.status) {
            BackhaulEvaluationStatus.EMPTY_RETURN -> {
                binding.cardStatusBanner.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
                binding.cardStatusBanner.strokeColor = Color.parseColor("#FCA5A5")
                binding.tvStatusTitle.setTextColor(Color.parseColor("#DC2626"))
                binding.tvStatusMessage.setTextColor(Color.parseColor("#991B1B"))
            }
            BackhaulEvaluationStatus.BELOW_RETURN_COST -> {
                binding.cardStatusBanner.setCardBackgroundColor(Color.parseColor("#FFFBEB"))
                binding.cardStatusBanner.strokeColor = Color.parseColor("#FCD34D")
                binding.tvStatusTitle.setTextColor(Color.parseColor("#D97706"))
                binding.tvStatusMessage.setTextColor(Color.parseColor("#92400E"))
            }
            BackhaulEvaluationStatus.BREAK_EVEN -> {
                binding.cardStatusBanner.setCardBackgroundColor(Color.parseColor("#FEFCE8"))
                binding.cardStatusBanner.strokeColor = Color.parseColor("#FDE047")
                binding.tvStatusTitle.setTextColor(Color.parseColor("#CA8A04"))
                binding.tvStatusMessage.setTextColor(Color.parseColor("#854D0E"))
            }
            BackhaulEvaluationStatus.STRONG_BACKHAUL -> {
                binding.cardStatusBanner.setCardBackgroundColor(Color.parseColor("#F0FDF4"))
                binding.cardStatusBanner.strokeColor = Color.parseColor("#86EFAC")
                binding.tvStatusTitle.setTextColor(Color.parseColor("#15803D"))
                binding.tvStatusMessage.setTextColor(Color.parseColor("#166534"))
            }
        }

        // 5. Pricing Targets
        binding.tvMinBackhaulPrice.text = String.format(Locale.getDefault(), "₹ %,.0f", result.minimumBackhaulPrice)
        binding.tvTargetBackhaulPrice.text = String.format(Locale.getDefault(), "₹ %,.0f", result.targetProfitBackhaulPrice)
        binding.tvOverallBreakEvenPrice.text = if (result.tripBreakEvenBackhaulPrice <= 0.0) {
            String.format(Locale.getDefault(), "₹ 0 (%s)", getString(R.string.covered_label))
        } else {
            String.format(Locale.getDefault(), "₹ %,.0f", result.tripBreakEvenBackhaulPrice)
        }
        val contribSign = if (result.additionalReturnContribution >= 0) "+" else "-"
        binding.tvReturnContribution.text = String.format(Locale.getDefault(), "%s₹ %,.0f", contribSign, abs(result.additionalReturnContribution))

        // 6. Update scenario preview with active return freight
        updateScenarioResultPreview(retRev)

        binding.layoutResults.visibility = View.VISIBLE
        return true
    }

    private fun setupScenarioChips() {
        val scenarioMap = mapOf(
            binding.chipScenario1 to 10000.0,
            binding.chipScenario2 to 15000.0,
            binding.chipScenario3 to 20000.0,
            binding.chipScenario4 to 25000.0,
            binding.chipScenario5 to 30000.0
        )

        scenarioMap.forEach { (chip, price) ->
            chip.setOnClickListener {
                // Highlight active chip
                scenarioMap.keys.forEach { otherChip ->
                    if (otherChip == chip) {
                        otherChip.setBackgroundResource(R.drawable.bg_chip_selected)
                        otherChip.setTextColor(ContextCompat.getColor(this, R.color.white))
                    } else {
                        otherChip.setBackgroundResource(R.drawable.bg_chip_unselected)
                        otherChip.setTextColor(Color.parseColor("#374151"))
                    }
                }

                // If user clicks scenario, update return revenue field and recalculate
                if (!isWithBackhaul) {
                    setReturnType(withBackhaul = true)
                }
                binding.etReturnRevenue.setText(price.toInt().toString())
                calculateAndDisplayResults()
                updateScenarioResultPreview(price)
            }
        }
    }

    private fun updateScenarioResultPreview(price: Double) {
        val outRev = binding.etOutboundRevenue.text.toString().trim().toDoubleOrNull() ?: 0.0
        val totalCost = binding.tvLiveTotalCost.text.toString().replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
        val outDist = binding.etOutboundDistance.text.toString().trim().toDoubleOrNull() ?: 500.0
        val retDist = binding.etReturnDistance.text.toString().trim().toDoubleOrNull() ?: outDist
        val totalDist = outDist + retDist

        val scenarios = BackhaulCalculatorService.simulateScenarios(
            outboundRevenue = outRev,
            totalTripCost = totalCost,
            totalDistanceKm = totalDist,
            emptyReturnCost = lastResult?.emptyReturnCost ?: 0.0,
            emptyReturnProfit = lastResult?.emptyReturnProfit ?: 0.0,
            prices = listOf(price)
        )

        if (scenarios.isNotEmpty()) {
            val sc = scenarios[0]
            val sign = if (sc.profitImprovement >= 0) "+" else "-"
            val profitFormatted = String.format(Locale.getDefault(), "₹ %,.0f", sc.totalProfit)
            val diffFormatted = String.format(Locale.getDefault(), "%s₹ %,.0f", sign, abs(sc.profitImprovement))
            binding.tvScenarioResultPreview.text = getString(
                R.string.scenario_preview_format,
                String.format(Locale.getDefault(), "₹ %,.0f", sc.returnPrice),
                profitFormatted,
                diffFormatted
            )
        }
    }

    private fun showQuickFuelCalculatorDialog() {
        val outDist = binding.etOutboundDistance.text.toString().trim().toDoubleOrNull() ?: 500.0
        val retDist = binding.etReturnDistance.text.toString().trim().toDoubleOrNull() ?: outDist
        val totalDist = outDist + retDist

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quick_fuel_calc, null, false)
        val etMileage = dialogView.findViewById<EditText>(R.id.etDialogMileage)
        val etFuelPrice = dialogView.findViewById<EditText>(R.id.etDialogFuelPrice)
        val tvTotalDistInfo = dialogView.findViewById<TextView>(R.id.tvDialogTotalDistance)

        tvTotalDistInfo.text = String.format(Locale.getDefault(), "%,.0f km", totalDist)
        etMileage.setText("3.8")
        etFuelPrice.setText("95")

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.quick_fuel_calc_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.apply_fuel_cost)) { dialog, _ ->
                val mileage = etMileage.text.toString().trim().toDoubleOrNull() ?: 3.8
                val price = etFuelPrice.text.toString().trim().toDoubleOrNull() ?: 95.0

                val fuelLiters = FuelCalculationService.calculateEstimatedFuel(totalDist, mileage)
                val fuelCost = FuelCalculationService.calculateFuelCost(fuelLiters, price)

                binding.etFuelCost.setText(String.format(Locale.getDefault(), "%.0f", fuelCost))
                updateLiveTotalCost()
                Toast.makeText(this, String.format(Locale.getDefault(), "%.1f L = ₹ %,.0f", fuelLiters, fuelCost), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSelectTripDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val trips = try {
                AppDatabase.getDatabase(applicationContext).tripRecordDao().getAll()
            } catch (e: Exception) {
                emptyList<TripRecordEntity>()
            }

            withContext(Dispatchers.Main) {
                if (trips.isEmpty()) {
                    Toast.makeText(this@BackhaulCalculatorActivity, getString(R.string.no_saved_trips_found), Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val tripTitles = trips.map { trip ->
                    "${trip.source} ➔ ${trip.destination} (${trip.truck_no}) - Rev: ₹${trip.total_income}"
                }.toTypedArray()

                AlertDialog.Builder(this@BackhaulCalculatorActivity)
                    .setTitle(getString(R.string.select_a_trip))
                    .setItems(tripTitles) { _, which ->
                        val selectedTrip = trips[which]
                        currentTripId = selectedTrip.id
                        binding.etOutboundOrigin.setText(selectedTrip.source)
                        binding.etOutboundDestination.setText(selectedTrip.destination)

                        val income = selectedTrip.total_income.toDoubleOrNull() ?: 0.0
                        if (income > 0) {
                            binding.etOutboundRevenue.setText(String.format(Locale.getDefault(), "%.0f", income))
                        }

                        val expense = selectedTrip.total_expense.toDoubleOrNull() ?: 0.0
                        if (expense > 0) {
                            binding.etFuelCost.setText(String.format(Locale.getDefault(), "%.0f", expense * 0.70))
                            binding.etTollCost.setText(String.format(Locale.getDefault(), "%.0f", expense * 0.15))
                            binding.etDriverCost.setText(String.format(Locale.getDefault(), "%.0f", expense * 0.15))
                        }

                        updateRouteSummaryBanner()
                        updateLiveTotalCost()
                        calculateAndDisplayResults()
                        Toast.makeText(this@BackhaulCalculatorActivity, "Loaded: ${selectedTrip.source} ➔ ${selectedTrip.destination}", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun saveCurrentPlan() {
        val result = lastResult
        if (result == null) {
            Toast.makeText(this, "Please calculate the plan first before saving", Toast.LENGTH_SHORT).show()
            return
        }

        val plan = BackhaulPlan(
            tripId = currentTripId,
            outboundOrigin = binding.etOutboundOrigin.text.toString().trim(),
            outboundDestination = binding.etOutboundDestination.text.toString().trim(),
            outboundDistanceKm = binding.etOutboundDistance.text.toString().trim().toDoubleOrNull() ?: 0.0,
            outboundRevenue = binding.etOutboundRevenue.text.toString().trim().toDoubleOrNull() ?: 0.0,
            returnOrigin = binding.etOutboundDestination.text.toString().trim(),
            returnDestination = binding.etOutboundOrigin.text.toString().trim(),
            returnDistanceKm = binding.etReturnDistance.text.toString().trim().toDoubleOrNull() ?: 0.0,
            isWithBackhaul = isWithBackhaul,
            returnRevenue = if (isWithBackhaul) binding.etReturnRevenue.text.toString().trim().toDoubleOrNull() ?: 0.0 else 0.0,
            fuelCost = binding.etFuelCost.text.toString().trim().toDoubleOrNull() ?: 0.0,
            tollCost = binding.etTollCost.text.toString().trim().toDoubleOrNull() ?: 0.0,
            driverCost = binding.etDriverCost.text.toString().trim().toDoubleOrNull() ?: 0.0,
            otherCost = binding.etOtherCost.text.toString().trim().toDoubleOrNull() ?: 0.0,
            totalCost = result.totalTripCost,
            emptyReturnCost = result.emptyReturnCost,
            targetAdditionalProfit = binding.etDesiredAdditionalProfit.text.toString().trim().toDoubleOrNull() ?: 0.0,
            calculationResult = result
        )

        BackhaulStorageHelper.saveLastPlan(this, plan)
        Toast.makeText(this, "✅ Backhaul plan saved successfully!", Toast.LENGTH_LONG).show()
    }

    private fun shareSummaryText() {
        val result = lastResult ?: return
        val origin = binding.etOutboundOrigin.text.toString().trim().ifEmpty { "A" }
        val dest = binding.etOutboundDestination.text.toString().trim().ifEmpty { "B" }

        val shareMessage = buildString {
            append("🔄 *TRIP BACKHAUL & RETURN ANALYSIS*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("📍 Route: $origin ➔ $dest ➔ $origin\n")
            append(String.format(Locale.getDefault(), "🛣️ Total Distance: %,.0f km\n", result.totalDistanceKm))
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("🔴 *EMPTY RETURN (DEADHEAD):*\n")
            append(String.format(Locale.getDefault(), "• Empty Return Cost: ₹ %,.0f\n", result.emptyReturnCost))
            append(String.format(Locale.getDefault(), "• Profit Without Load: ₹ %,.0f (₹ %,.2f/km)\n", result.emptyReturnProfit, result.emptyReturnProfitPerKm))
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("🟢 *WITH RETURN LOAD:*\n")
            append(String.format(Locale.getDefault(), "• Return Load Revenue: ₹ %,.0f\n", result.returnRevenue))
            append(String.format(Locale.getDefault(), "• Total Revenue: ₹ %,.0f\n", result.backhaulTotalRevenue))
            append(String.format(Locale.getDefault(), "• Profit With Load: ₹ %,.0f (₹ %,.2f/km)\n", result.backhaulProfit, result.backhaulProfitPerKm))
            append(String.format(Locale.getDefault(), "🚀 *Profit Boost: +₹ %,.0f*\n", result.profitImprovement))
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("💡 *DECISION TARGETS:*\n")
            append(String.format(Locale.getDefault(), "• Break-Even Return Rate: ₹ %,.0f\n", result.minimumBackhaulPrice))
            append(String.format(Locale.getDefault(), "• Target Profit Return Rate: ₹ %,.0f\n", result.targetProfitBackhaulPrice))
            append(String.format(Locale.getDefault(), "• Return Contribution: +₹ %,.0f\n", result.additionalReturnContribution))
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("Calculated with Truck Trip App")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share Trip Backhaul Analysis"))
    }

    private fun showInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.backhaul_info_title))
            .setMessage(getString(R.string.backhaul_info_message))
            .setPositiveButton(getString(R.string.got_it), null)
            .show()
    }
}
