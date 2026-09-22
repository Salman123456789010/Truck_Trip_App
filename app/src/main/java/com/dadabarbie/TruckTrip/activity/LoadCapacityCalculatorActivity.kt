package com.dadabarbie.TruckTrip.activity

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.adapter.CargoAdapter
import com.dadabarbie.TruckTrip.ads.AdMobManager
import com.dadabarbie.TruckTrip.databinding.ActivityLoadCapacityCalculatorBinding
import com.dadabarbie.TruckTrip.databinding.DialogAddCargoBinding
import com.dadabarbie.TruckTrip.model.CargoItem
import java.util.Locale
import kotlin.math.min

class LoadCapacityCalculatorActivity : BaseActivity() {

    private val binding: ActivityLoadCapacityCalculatorBinding by lazy {
        ActivityLoadCapacityCalculatorBinding.inflate(layoutInflater)
    }

    private val cargoList = mutableListOf<CargoItem>()
    private lateinit var cargoAdapter: CargoAdapter

    private var isTruckUnitTon = false
    private var isTruckDimMeters = false
    private var userActionCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        setupUnitChips()
        setupDimensionsToggle()
        setupRecyclerView()
        setupListeners()
        calculateAndUpdateResults()
    }

    private fun setupUnitChips() {
        // Truck Weight Unit Chips
        binding.btnUnitKg.setOnClickListener {
            setTruckWeightUnit(isTon = false)
        }
        binding.btnUnitTon.setOnClickListener {
            setTruckWeightUnit(isTon = true)
        }

        // Truck Dimension Unit Chips
        binding.btnDimFt.setOnClickListener {
            setTruckDimUnit(isMeters = false)
        }
        binding.btnDimM.setOnClickListener {
            setTruckDimUnit(isMeters = true)
        }
    }

    private fun setTruckWeightUnit(isTon: Boolean) {
        isTruckUnitTon = isTon
        if (isTon) {
            binding.btnUnitTon.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.btnUnitTon.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnUnitKg.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.btnUnitKg.setTextColor(0xFF4B5563.toInt())
        } else {
            binding.btnUnitKg.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.btnUnitKg.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnUnitTon.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.btnUnitTon.setTextColor(0xFF4B5563.toInt())
        }
        calculateAndUpdateResults()
    }

    private fun setTruckDimUnit(isMeters: Boolean) {
        isTruckDimMeters = isMeters
        if (isMeters) {
            binding.btnDimM.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.btnDimM.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnDimFt.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.btnDimFt.setTextColor(0xFF4B5563.toInt())
        } else {
            binding.btnDimFt.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.btnDimFt.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnDimM.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.btnDimM.setTextColor(0xFF4B5563.toInt())
        }
        calculateAndUpdateResults()
    }

    private fun setupDimensionsToggle() {
        binding.switchEnableDimensions.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.layoutTruckDimensionsContainer.visibility = View.VISIBLE
            } else {
                binding.layoutTruckDimensionsContainer.visibility = View.GONE
            }
            calculateAndUpdateResults()
        }
    }

    private fun setupRecyclerView() {
        cargoAdapter = CargoAdapter(
            cargoList,
            onEditClick = { item, position ->
                showAddOrEditCargoDialog(item, position)
            },
            onDeleteClick = { item, position ->
                cargoList.removeAt(position)
                cargoAdapter.updateList(cargoList)
                updateCargoEmptyState()
                calculateAndUpdateResults()
            }
        )
        binding.rvCargoItems.layoutManager = LinearLayoutManager(this)
        binding.rvCargoItems.adapter = cargoAdapter
        updateCargoEmptyState()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnResetHeader.setOnClickListener {
            confirmAndReset()
        }

        binding.btnResetBottom.setOnClickListener {
            confirmAndReset()
        }

        binding.btnAddCargo.setOnClickListener {
            showAddOrEditCargoDialog(null, -1)
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateAndUpdateResults()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etTruckCapacity.addTextChangedListener(textWatcher)
        binding.etTruckLength.addTextChangedListener(textWatcher)
        binding.etTruckWidth.addTextChangedListener(textWatcher)
        binding.etTruckHeight.addTextChangedListener(textWatcher)
    }

    private fun updateCargoEmptyState() {
        if (cargoList.isEmpty()) {
            binding.tvEmptyCargoHint.visibility = View.VISIBLE
            binding.rvCargoItems.visibility = View.GONE
        } else {
            binding.tvEmptyCargoHint.visibility = View.GONE
            binding.rvCargoItems.visibility = View.VISIBLE
        }
    }

    private fun triggerAdOnUserAction() {
        userActionCount++
        // Show interstitial at a natural completion milestone (every 3rd saved cargo)
        if (userActionCount % 3 == 0) {
            AdMobManager.showInterstitialIfReady(this)
        }
    }

    private fun showAddOrEditCargoDialog(cargoItemToEdit: CargoItem?, editPosition: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_cargo, null)
        val dialogBinding = DialogAddCargoBinding.bind(dialogView)

        var dialogIsWeightUnitTon = false
        var dialogIsDimUnitMeters = false

        fun updateDialogWeightChips(isTon: Boolean) {
            dialogIsWeightUnitTon = isTon
            if (isTon) {
                dialogBinding.btnDialogUnitTon.setBackgroundResource(R.drawable.bg_chip_selected)
                dialogBinding.btnDialogUnitTon.setTextColor(ContextCompat.getColor(this, R.color.white))
                dialogBinding.btnDialogUnitKg.setBackgroundResource(R.drawable.bg_chip_unselected)
                dialogBinding.btnDialogUnitKg.setTextColor(0xFF4B5563.toInt())
            } else {
                dialogBinding.btnDialogUnitKg.setBackgroundResource(R.drawable.bg_chip_selected)
                dialogBinding.btnDialogUnitKg.setTextColor(ContextCompat.getColor(this, R.color.white))
                dialogBinding.btnDialogUnitTon.setBackgroundResource(R.drawable.bg_chip_unselected)
                dialogBinding.btnDialogUnitTon.setTextColor(0xFF4B5563.toInt())
            }
        }

        fun updateDialogDimChips(isMeters: Boolean) {
            dialogIsDimUnitMeters = isMeters
            if (isMeters) {
                dialogBinding.btnDialogDimM.setBackgroundResource(R.drawable.bg_chip_selected)
                dialogBinding.btnDialogDimM.setTextColor(ContextCompat.getColor(this, R.color.white))
                dialogBinding.btnDialogDimFt.setBackgroundResource(R.drawable.bg_chip_unselected)
                dialogBinding.btnDialogDimFt.setTextColor(0xFF4B5563.toInt())
            } else {
                dialogBinding.btnDialogDimFt.setBackgroundResource(R.drawable.bg_chip_selected)
                dialogBinding.btnDialogDimFt.setTextColor(ContextCompat.getColor(this, R.color.white))
                dialogBinding.btnDialogDimM.setBackgroundResource(R.drawable.bg_chip_unselected)
                dialogBinding.btnDialogDimM.setTextColor(0xFF4B5563.toInt())
            }
        }

        dialogBinding.btnDialogUnitKg.setOnClickListener { updateDialogWeightChips(false) }
        dialogBinding.btnDialogUnitTon.setOnClickListener { updateDialogWeightChips(true) }
        dialogBinding.btnDialogDimFt.setOnClickListener { updateDialogDimChips(false) }
        dialogBinding.btnDialogDimM.setOnClickListener { updateDialogDimChips(true) }

        dialogBinding.switchDialogDimensions.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.layoutDialogDimensionsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        if (cargoItemToEdit != null) {
            dialogBinding.tvDialogTitle.text = getString(R.string.edit_cargo)
            dialogBinding.etCargoName.setText(cargoItemToEdit.name)
            dialogBinding.etWeightPerItem.setText(formatNumber(cargoItemToEdit.weightPerItem))
            dialogBinding.etQuantity.setText(cargoItemToEdit.quantity.toString())

            updateDialogWeightChips(cargoItemToEdit.weightUnit.equals("ton", ignoreCase = true))

            if (cargoItemToEdit.hasDimensions()) {
                dialogBinding.switchDialogDimensions.isChecked = true
                dialogBinding.layoutDialogDimensionsContainer.visibility = View.VISIBLE
                dialogBinding.etLength.setText(formatNumber(cargoItemToEdit.length ?: 0.0))
                dialogBinding.etWidth.setText(formatNumber(cargoItemToEdit.width ?: 0.0))
                dialogBinding.etHeight.setText(formatNumber(cargoItemToEdit.height ?: 0.0))
                updateDialogDimChips(cargoItemToEdit.dimensionUnit.equals("m", ignoreCase = true))
            } else {
                dialogBinding.switchDialogDimensions.isChecked = false
                dialogBinding.layoutDialogDimensionsContainer.visibility = View.GONE
                updateDialogDimChips(false)
            }
        } else {
            dialogBinding.tvDialogTitle.text = getString(R.string.add_cargo)
            dialogBinding.etQuantity.setText("1")
            updateDialogWeightChips(false)
            updateDialogDimChips(false)
        }

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogBinding.btnCancelCargo.setOnClickListener {
            alertDialog.dismiss()
        }

        dialogBinding.btnSaveCargo.setOnClickListener {
            val name = dialogBinding.etCargoName.text.toString().trim()
            val weightStr = dialogBinding.etWeightPerItem.text.toString().trim()
            val quantityStr = dialogBinding.etQuantity.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_enter_cargo_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weight = weightStr.toDoubleOrNull() ?: 0.0
            if (weight <= 0.0) {
                Toast.makeText(this, getString(R.string.please_enter_valid_cargo_weight), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = quantityStr.toIntOrNull() ?: 1
            if (quantity <= 0) {
                Toast.makeText(this, getString(R.string.quantity_greater_zero), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weightUnit = if (dialogIsWeightUnitTon) "ton" else "kg"
            val dimUnit = if (dialogIsDimUnitMeters) "m" else "ft"

            val length: Double?
            val width: Double?
            val height: Double?

            if (dialogBinding.switchDialogDimensions.isChecked) {
                length = dialogBinding.etLength.text.toString().trim().toDoubleOrNull()
                width = dialogBinding.etWidth.text.toString().trim().toDoubleOrNull()
                height = dialogBinding.etHeight.text.toString().trim().toDoubleOrNull()
            } else {
                length = null
                width = null
                height = null
            }

            if (cargoItemToEdit != null && editPosition >= 0) {
                cargoItemToEdit.name = name
                cargoItemToEdit.weightPerItem = weight
                cargoItemToEdit.weightUnit = weightUnit
                cargoItemToEdit.quantity = quantity
                cargoItemToEdit.length = length
                cargoItemToEdit.width = width
                cargoItemToEdit.height = height
                cargoItemToEdit.dimensionUnit = dimUnit
            } else {
                val newItem = CargoItem(
                    name = name,
                    weightPerItem = weight,
                    weightUnit = weightUnit,
                    quantity = quantity,
                    length = length,
                    width = width,
                    height = height,
                    dimensionUnit = dimUnit
                )
                cargoList.add(newItem)
            }

            cargoAdapter.updateList(cargoList)
            updateCargoEmptyState()
            calculateAndUpdateResults()
            alertDialog.dismiss()

            triggerAdOnUserAction()
        }

        alertDialog.show()
    }

    private fun calculateAndUpdateResults() {
        val capacityInputStr = binding.etTruckCapacity.text.toString().trim()
        val capacityInput = capacityInputStr.toDoubleOrNull() ?: 0.0

        val maxCapacityInKg = if (isTruckUnitTon) capacityInput * 1000.0 else capacityInput

        val totalLoadedInKg = cargoList.sumOf { it.totalWeightInKg() }
        val remainingInKg = maxCapacityInKg - totalLoadedInKg

        val capacityUsedPercent = if (maxCapacityInKg > 0.0) {
            (totalLoadedInKg / maxCapacityInKg) * 100.0
        } else {
            0.0
        }

        val displayUnitStr = if (isTruckUnitTon) getString(R.string.unit_ton) else getString(R.string.unit_kg)
        val truckCapDisplayVal = if (isTruckUnitTon) capacityInput else maxCapacityInKg
        val loadedDisplayVal = if (isTruckUnitTon) totalLoadedInKg / 1000.0 else totalLoadedInKg
        val remainingDisplayVal = if (isTruckUnitTon) remainingInKg / 1000.0 else remainingInKg

        binding.tvSummaryTruckCapacity.text = "${formatNumber(truckCapDisplayVal)} $displayUnitStr"
        binding.tvSummaryTotalLoaded.text = "${formatNumber(loadedDisplayVal)} $displayUnitStr"

        val formattedRemainingStr = if (remainingDisplayVal < 0) {
            "0 $displayUnitStr"
        } else {
            "${formatNumber(remainingDisplayVal)} $displayUnitStr"
        }
        binding.tvSummaryRemainingCapacity.text = formattedRemainingStr
        binding.tvSummaryCapacityUsed.text = String.format(Locale.getDefault(), "%.2f%%", capacityUsedPercent)

        val progressInt = min(capacityUsedPercent.toInt(), 100)
        binding.pbCapacityUsed.progress = progressInt

        val isWeightOverCapacity = maxCapacityInKg > 0.0 && totalLoadedInKg > maxCapacityInKg

        if (isWeightOverCapacity) {
            val exceededInKg = totalLoadedInKg - maxCapacityInKg
            val exceededDisplayVal = if (isTruckUnitTon) exceededInKg / 1000.0 else exceededInKg
            val exceededTextStr = "${formatNumber(exceededDisplayVal)} $displayUnitStr"

            binding.tvWeightStatus.text = "🔴 ${getString(R.string.over_capacity)} • ${getString(R.string.exceeded_by, exceededTextStr)}"
            binding.tvWeightStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            binding.layoutWeightStatus.setBackgroundColor(0xFFFEF2F2.toInt())
            binding.pbCapacityUsed.progressTintList = ColorStateList.valueOf(0xFFDC2626.toInt())
        } else {
            binding.tvWeightStatus.text = "🟢 ${getString(R.string.load_capacity_ok)}"
            binding.tvWeightStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
            binding.layoutWeightStatus.setBackgroundColor(0xFFF0FDF4.toInt())
            binding.pbCapacityUsed.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green))
        }

        // Volume Calculation (only if dimensions switch is ON and valid truck dimensions entered)
        var isVolumeExceeded = false
        var volumeExceededReasonStr = ""

        if (binding.switchEnableDimensions.isChecked) {
            val truckL = binding.etTruckLength.text.toString().trim().toDoubleOrNull() ?: 0.0
            val truckW = binding.etTruckWidth.text.toString().trim().toDoubleOrNull() ?: 0.0
            val truckH = binding.etTruckHeight.text.toString().trim().toDoubleOrNull() ?: 0.0

            val hasTruckDimensions = truckL > 0.0 && truckW > 0.0 && truckH > 0.0
            val hasCargoDimensions = cargoList.any { it.hasDimensions() }

            if (hasTruckDimensions && hasCargoDimensions) {
                binding.cardVolumeSummary.visibility = View.VISIBLE

                val rawTruckVol = truckL * truckW * truckH
                val truckVolInCuFt = if (isTruckDimMeters) rawTruckVol * 35.3147 else rawTruckVol

                val totalCargoVolInCuFt = cargoList.sumOf { it.totalVolumeInCubicFeet() }

                val displayDimUnitStr = if (isTruckDimMeters) "m³" else "ft³"
                val displayTruckVol = if (isTruckDimMeters) rawTruckVol else truckVolInCuFt
                val displayCargoVol = if (isTruckDimMeters) totalCargoVolInCuFt / 35.3147 else totalCargoVolInCuFt

                val spaceUsedPercent = if (displayTruckVol > 0.0) (displayCargoVol / displayTruckVol) * 100.0 else 0.0

                binding.tvSummaryTruckVolume.text = "${formatNumber(displayTruckVol)} $displayDimUnitStr"
                binding.tvSummaryCargoVolume.text = "${formatNumber(displayCargoVol)} $displayDimUnitStr"
                binding.tvSummarySpaceUsed.text = String.format(Locale.getDefault(), "%.2f%%", spaceUsedPercent)

                if (totalCargoVolInCuFt > truckVolInCuFt) {
                    isVolumeExceeded = true
                    val exceededVolInCuFt = totalCargoVolInCuFt - truckVolInCuFt
                    val exceededVolDisplay = if (isTruckDimMeters) exceededVolInCuFt / 35.3147 else exceededVolInCuFt
                    val exceededVolStr = "${formatNumber(exceededVolDisplay)} $displayDimUnitStr"
                    volumeExceededReasonStr = getString(R.string.volume_exceeds_by, exceededVolStr)

                    binding.tvVolumeStatus.text = "🔴 ${getString(R.string.estimated_volume_exceeded)}"
                    binding.tvVolumeStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                    binding.layoutVolumeStatus.setBackgroundColor(0xFFFEF2F2.toInt())
                } else {
                    binding.tvVolumeStatus.text = "🟢 ${getString(R.string.estimated_volume_ok)}"
                    binding.tvVolumeStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
                    binding.layoutVolumeStatus.setBackgroundColor(0xFFF0FDF4.toInt())
                }
            } else {
                binding.cardVolumeSummary.visibility = View.GONE
            }
        } else {
            binding.cardVolumeSummary.visibility = View.GONE
        }

        // Overall Result Banner Calculation
        val isOverallLimitExceeded = isWeightOverCapacity || isVolumeExceeded

        if (isOverallLimitExceeded) {
            binding.layoutOverallResult.setBackgroundColor(0xFFFEF2F2.toInt())
            binding.tvOverallResultText.text = "🔴 ${getString(R.string.load_exceeds_limits)}"
            binding.tvOverallResultText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            binding.tvOverallResultReason.visibility = View.VISIBLE

            val weightExceededReasonStr = if (isWeightOverCapacity) {
                val exceededInKg = totalLoadedInKg - maxCapacityInKg
                val exceededDisplayVal = if (isTruckUnitTon) exceededInKg / 1000.0 else exceededInKg
                val exceededTextStr = "${formatNumber(exceededDisplayVal)} $displayUnitStr"
                getString(R.string.weight_exceeds_by, exceededTextStr)
            } else ""

            val combinedReason = when {
                isWeightOverCapacity && isVolumeExceeded -> "$weightExceededReasonStr\n$volumeExceededReasonStr"
                isWeightOverCapacity -> weightExceededReasonStr
                else -> volumeExceededReasonStr
            }
            binding.tvOverallResultReason.text = combinedReason
        } else {
            binding.layoutOverallResult.setBackgroundColor(0xFFF0FDF4.toInt())
            binding.tvOverallResultText.text = "🟢 ${getString(R.string.load_within_limits)}"
            binding.tvOverallResultText.setTextColor(ContextCompat.getColor(this, R.color.green))
            binding.tvOverallResultReason.visibility = View.GONE
        }
    }

    private fun confirmAndReset() {
        if (binding.etTruckCapacity.text.toString().isNotEmpty() || cargoList.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_confirm_title))
                .setMessage(getString(R.string.reset_confirm_msg))
                .setPositiveButton(getString(R.string.reset)) { dialog, _ ->
                    dialog.dismiss()
                    resetAllInputs()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            resetAllInputs()
        }
    }

    private fun resetAllInputs() {
        binding.etTruckCapacity.setText("")
        binding.etTruckLength.setText("")
        binding.etTruckWidth.setText("")
        binding.etTruckHeight.setText("")

        setTruckWeightUnit(isTon = false)
        setTruckDimUnit(isMeters = false)

        binding.switchEnableDimensions.isChecked = false
        binding.layoutTruckDimensionsContainer.visibility = View.GONE

        cargoList.clear()
        cargoAdapter.updateList(cargoList)
        updateCargoEmptyState()
        calculateAndUpdateResults()

        Toast.makeText(this, getString(R.string.cleared_all_data), Toast.LENGTH_SHORT).show()
        triggerAdOnUserAction()
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(Locale.getDefault(), "%,d", value.toLong())
        } else {
            String.format(Locale.getDefault(), "%,.2f", value)
        }
    }
}
