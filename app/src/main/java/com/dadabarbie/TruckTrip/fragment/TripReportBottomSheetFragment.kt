package com.dadabarbie.TruckTrip.fragment

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.ads.AdMobManager
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.DialogTripReportBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class TripReportBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: DialogTripReportBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    private val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    private var fromCalendar: Calendar = Calendar.getInstance()
    private var toCalendar: Calendar = Calendar.getInstance()

    private var selectedReportType: String = "daily"

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onStart() {
        super.onStart()
        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTripReportBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AdMobManager.preloadRewardedAd(requireContext())

        binding.btnClose.setOnClickListener { dismiss() }

        setupPeriodChips()
        setupDatePickers()
        setupSubmitButton()
        observeViewModel()

        // Default to Daily period
        selectPeriod("daily")
    }

    private fun setupPeriodChips() {
        binding.chipGroupPeriod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipDaily -> selectPeriod("daily")
                R.id.chipWeekly -> selectPeriod("weekly")
                R.id.chipMonthly -> selectPeriod("monthly")
            }
        }
    }

    private fun selectPeriod(period: String) {
        selectedReportType = period
        toCalendar = Calendar.getInstance()

        when (period) {
            "daily" -> {
                binding.chipDaily.isChecked = true
                fromCalendar = Calendar.getInstance()
            }
            "weekly" -> {
                binding.chipWeekly.isChecked = true
                fromCalendar = Calendar.getInstance()
                fromCalendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            "monthly" -> {
                binding.chipMonthly.isChecked = true
                fromCalendar = Calendar.getInstance()
                fromCalendar.set(Calendar.DAY_OF_MONTH, 1)
            }
        }

        updateDateDisplays()
    }

    private fun updateDateDisplays() {
        binding.etFromDate.text = sdfApi.format(fromCalendar.time)
        binding.etToDate.text = sdfApi.format(toCalendar.time)
        updateRangeSummary()
    }

    private fun updateRangeSummary() {
        val diffMs = toCalendar.timeInMillis - fromCalendar.timeInMillis
        val days = (diffMs / (1000 * 60 * 60 * 24)).toInt() + 1
        val daysText = if (days <= 1) getString(R.string.day_single) else getString(R.string.days_plural, days)
        binding.tvRangeSummary.text = getString(R.string.report_timeframe_summary, daysText, binding.etFromDate.text, binding.etToDate.text)
    }

    private fun setupDatePickers() {
        val fromListener = View.OnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    fromCalendar.set(year, month, dayOfMonth)
                    updateDateDisplays()
                },
                fromCalendar.get(Calendar.YEAR),
                fromCalendar.get(Calendar.MONTH),
                fromCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        binding.cardFromDate.setOnClickListener(fromListener)
        binding.etFromDate.setOnClickListener(fromListener)

        val toListener = View.OnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    toCalendar.set(year, month, dayOfMonth)
                    updateDateDisplays()
                },
                toCalendar.get(Calendar.YEAR),
                toCalendar.get(Calendar.MONTH),
                toCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        binding.cardToDate.setOnClickListener(toListener)
        binding.etToDate.setOnClickListener(toListener)
    }

    private fun setupSubmitButton() {
        binding.btnDownloadReport.setOnClickListener {
            val truckNo = binding.etTruckNo.text?.toString()?.trim().orEmpty()
            binding.tilTruckNo.error = null

            val fromDate = sdfApi.format(fromCalendar.time)
            val toDate = sdfApi.format(toCalendar.time)

            if (fromCalendar.after(toCalendar)) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_select_valid_date_range),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Show rewarded ad to unlock report download
            AdMobManager.showRewardedAd(
                requireActivity(),
                onRewardEarned = {
                    // Start download API only after user watched and closed the ad
                    startDownloadReport(fromDate, toDate, truckNo)
                },
                onAdFailedOrClosed = { reason ->
                    if (reason.contains("before earning", ignoreCase = true) || reason.contains("closed", ignoreCase = true)) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.ad_not_finished_warning),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // If ad is not ready / unavailable, proceed to download directly
                        startDownloadReport(fromDate, toDate, truckNo)
                    }
                }
            )
        }

        binding.btnExportReport.setOnClickListener {
            val truckNo = binding.etTruckNo.text?.toString()?.trim().orEmpty()
            binding.tilTruckNo.error = null

            val fromDate = sdfApi.format(fromCalendar.time)
            val toDate = sdfApi.format(toCalendar.time)

            if (fromCalendar.after(toCalendar)) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_select_valid_date_range),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Show rewarded ad to unlock report export
            AdMobManager.showRewardedAd(
                requireActivity(),
                onRewardEarned = {
                    // Start export API only after user watched and closed the ad
                    startExportReport(fromDate, toDate, truckNo)
                },
                onAdFailedOrClosed = { reason ->
                    if (reason.contains("before earning", ignoreCase = true) || reason.contains("closed", ignoreCase = true)) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.ad_not_finished_warning),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // If ad is not ready / unavailable, proceed to export directly
                        startExportReport(fromDate, toDate, truckNo)
                    }
                }
            )
        }
    }

    private fun startDownloadReport(
        fromDate: String,
        toDate: String,
        truckNo: String
    ) {
        binding.tvLoadingText.text = getString(R.string.generating_downloading_pdf)
        binding.layoutLoading.visibility = View.VISIBLE
        binding.btnDownloadReport.isEnabled = false
        binding.btnExportReport.isEnabled = false

        authViewModel.downloadTripsReport(fromDate, toDate, truckNo)
    }

    private fun startExportReport(
        fromDate: String,
        toDate: String,
        truckNo: String
    ) {
        binding.tvLoadingText.text = getString(R.string.exporting_report_data)
        binding.layoutLoading.visibility = View.VISIBLE
        binding.btnDownloadReport.isEnabled = false
        binding.btnExportReport.isEnabled = false

        authViewModel.exportTripsReport(fromDate, toDate, truckNo)
    }

    private fun observeViewModel() {
        authViewModel.tripsReportDownloadCompleted.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { filePath ->
                binding.layoutLoading.visibility = View.GONE
                binding.btnDownloadReport.isEnabled = true
                binding.btnExportReport.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    getString(R.string.report_downloaded_success),
                    Toast.LENGTH_SHORT
                ).show()

                openReportFile(filePath)
                dismiss()
            }
        }

        authViewModel.tripsReportErrorEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorMsg ->
                binding.layoutLoading.visibility = View.GONE
                binding.btnDownloadReport.isEnabled = true
                binding.btnExportReport.isEnabled = true

                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }

        authViewModel.tripsExportDownloadCompleted.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { filePath ->
                binding.layoutLoading.visibility = View.GONE
                binding.btnDownloadReport.isEnabled = true
                binding.btnExportReport.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    getString(R.string.report_exported_success),
                    Toast.LENGTH_SHORT
                ).show()

                openReportFile(filePath)
                dismiss()
            }
        }

        authViewModel.tripsExportErrorEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { errorMsg ->
                binding.layoutLoading.visibility = View.GONE
                binding.btnDownloadReport.isEnabled = true
                binding.btnExportReport.isEnabled = true

                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openReportFile(filePath: String) {
        try {
            val context = context ?: return
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, getString(R.string.file_not_found), Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(context, context.packageName, file)
            val mimeType = when {
                file.name.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                file.name.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
                file.name.endsWith(".csv", ignoreCase = true) -> "text/csv"
                file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.open_trip_report)))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TripReportBottomSheetFragment"

        fun newInstance(): TripReportBottomSheetFragment {
            return TripReportBottomSheetFragment()
        }
    }
}
