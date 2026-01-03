package com.dadabarbie.TruckTrip.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.gone
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.Constants.tripName
import com.dadabarbie.TruckTrip.Utils.Constants.visible
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.DashBoardActivity
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.activity.TripPreviewActivity
import com.dadabarbie.TruckTrip.adapter.TripListAdapter
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.FragmentHomeBinding
import com.dadabarbie.TruckTrip.diologFragment.DeleteDialogFragment
import com.dadabarbie.TruckTrip.model.getTrip.Record
import com.google.android.gms.ads.MobileAds
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.vasyerp.freshvegetables.util.NetworkResult
import com.vasyerp.cafvd.room.model.TripRecordEntity
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.getTrip.TripGetResponseModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment(), View.OnClickListener, TripListAdapter.EditTripDataListner,
    TripListAdapter.ShareTripDataListner, TripListAdapter.DeleteTripListner,
    TripListAdapter.DowanloadListner, TripListAdapter.ViewTripDataListner {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var tripListAdapter: TripListAdapter? = null
    private val authViewModel: AuthViewModel by viewModels()

    // 🔥 CRITICAL FIX: Use LinkedHashMap to maintain order and allow ID-based operations
    private val tripMap: LinkedHashMap<String, Record> = LinkedHashMap()

    private var isLoading: Boolean = false
    private val pageSize: Int = 20

    private var materialDatePicker: MaterialDatePicker<*>? = null
    private var deleteDialogFragment: DeleteDialogFragment? = null

    private var startDate = ""
    private var endDate = ""
    private var shareFlag = false
    private var clickFlag = false

    // 🔥 CRITICAL: Track which trip is being deleted
    private var pendingDeleteTripId: String? = null
    private var isDeleting = false  // Prevent multiple simultaneous deletes

    private val MIN_CLICK_INTERVAL: Long = 1000
    private var lastClickTime: Long = 0

    private val apiDateFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    private val gson by lazy { Gson() }

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? DashBoardActivity)?.textChanges(1)

        if (savedInstanceState == null) {
            MobileAds.initialize(requireActivity())
        }

        initDatePicker()
        setOnClickListner()
        initAdapter()
        setObserver()

        if (savedInstanceState == null) {
            loadInitialData()
        }

        (requireActivity() as? DashBoardActivity)?.setData()
    }

    private fun loadInitialData() {
        tripMap.clear()
        authViewModel.resetTripPagination()
        authViewModel.getAllTripData(startDate, endDate, size = pageSize)
    }

    private fun setObserver() {
        authViewModel.getAllTripData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { result ->
                handleTripDataResult(result)
            }
        }

        authViewModel.getDeleteTripData.observe(viewLifecycleOwner) { result ->
            handleDeleteResult(result)
        }

        // 🔥 CRITICAL FIX: Get trip by current list position, then delete by ID
        Constants.deleteTrip.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { position ->
                handleDeleteRequest(position)
            }
        }

        Constants.refreshApi.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { flag ->
                if (flag >= 0) refreshData()
            }
        }

        authViewModel.downloadCompleted.observe(viewLifecycleOwner) { data ->
            handleDownloadComplete(data)
        }
    }

    private fun handleTripDataResult(result: NetworkResult<*>) {
        when (result) {
            is NetworkResult.Error -> handleError()
            is NetworkResult.Loading -> handleLoading()
            is NetworkResult.Success -> handleSuccess(result)
        }
    }

    private fun handleError() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val cached = loadCachedTripRecords(startDate, endDate)
            withContext(Dispatchers.Main) {
                if (cached.isNotEmpty()) {
                    updateUIWithCachedData(cached)
                } else {
                    hideProgressBar()
                }
                isLoading = false
            }
        }
    }

    private fun handleLoading() {
        isLoading = true
        setProgressBarVisibility()
    }

    private fun handleSuccess(result: NetworkResult.Success<*>) {
        binding.recycleList.visible()
        binding.progressbar.gone()
        binding.noProductFound.gone()

        val data = result.data as? TripGetResponseModel

        if (data?.status == 200) {
            binding.swipeToRefreshBasicDetails.isRefreshing = false

            data.data.records?.let { newRecords ->
                if (newRecords.isNotEmpty()) {
                    // 🔥 FIX: Add to map with ID as key
                    newRecords.forEach { record ->
                        if (!record._id.isNullOrEmpty()) {
                            tripMap[record._id] = record
                        }
                    }

                    // Cache in background
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        cacheTripRecords(newRecords)
                    }

                    // Update UI
                    binding.drivertotalAavak.text = "₹${data.data.totalDriverIncome}"
                    binding.totalMalikAavak.text = "₹${data.data.totalOwnerIncome}"

                    // 🔥 CRITICAL FIX: Always create NEW list instance
                    updateAdapterList()
                } else {
                    handleEmptyData()
                }
            } ?: handleEmptyData()
        } else {
            if (tripMap.isEmpty()) {
                showNoDataView()
            }
        }

        isLoading = false
    }

    // 🔥 NEW METHOD: Update adapter with fresh list
    private fun updateAdapterList() {
        val newList = ArrayList(tripMap.values)
        Log.d(TAG, "Updating adapter with ${newList.size} items")

        // Force adapter to recognize this as a completely new list
        tripListAdapter?.submitList(null)
        tripListAdapter?.submitList(newList)
    }

    private fun handleEmptyData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(requireContext()).tripRecordDao().clearAll()
            withContext(Dispatchers.Main) {
                authViewModel.resetTripPagination()
                binding.drivertotalAavak.text = "₹0"
                binding.totalMalikAavak.text = "₹0"
                showNoDataView()
            }
        }
    }

    private fun showNoDataView() {
        binding.noProductFound.visible()
        binding.recycleList.gone()
        authViewModel.resetTripPagination()
        isLoading = false
    }

    private fun updateUIWithCachedData(cached: List<Record>) {
        tripMap.clear()
        cached.forEach { record ->
            if (!record._id.isNullOrEmpty()) {
                tripMap[record._id] = record
            }
        }

        val driverTotal = cached.sumOf { it.driver_income.toDoubleOrNullSafe() }.toLong()
        val ownerTotal = cached.sumOf { it.owner_profit.toDoubleOrNullSafe() }.toLong()

        binding.drivertotalAavak.text = "₹$driverTotal"
        binding.totalMalikAavak.text = "₹$ownerTotal"

        updateAdapterList()
        hideProgressBar()
    }

    private fun hideProgressBar() {
        binding.progressbar.gone()
        binding.recycleList.visible()
        binding.swipeToRefreshBasicDetails.isRefreshing = false
    }

    private fun handleDeleteResult(result: NetworkResult<*>) {
        when (result) {
            is NetworkResult.Error -> {
                dismissProgress()
                isDeleting = false
                Toast.makeText(requireContext(), "Failed to delete trip", Toast.LENGTH_SHORT).show()

                // 🔥 FIX: Restore item on failure by refreshing
                refreshData()
                pendingDeleteTripId = null
            }

            is NetworkResult.Loading -> {
                // Keep showing progress
            }

            is NetworkResult.Success -> {
                handleDeleteSuccess()
            }
        }
    }

    private fun handleDeleteSuccess() {
        val deletedId = pendingDeleteTripId

        if (deletedId != null) {
            Log.d(TAG, "Delete success for trip: $deletedId")

            // Delete from cache
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    AppDatabase.getDatabase(requireContext()).tripRecordDao().deleteById(deletedId)
                    Log.d(TAG, "Trip deleted from cache: $deletedId")
                } catch (e: Exception) {
                    Log.e(TAG, "Cache delete failed: ${e.message}")
                }
            }

            // Clear pending state
            pendingDeleteTripId = null
            isDeleting = false

            dismissProgress()
            Toast.makeText(requireContext(), "Trip deleted successfully", Toast.LENGTH_SHORT).show()

            // 🔥 CRITICAL: Refresh to get clean state from server
            Handler(Looper.getMainLooper()).postDelayed({
                refreshData()
            }, 300)
        } else {
            dismissProgress()
            isDeleting = false
        }
    }

    // 🔥 CRITICAL FIX: Proper delete flow
    private fun handleDeleteRequest(position: Int) {
        // Prevent multiple simultaneous deletes
        if (isDeleting) {
            Toast.makeText(requireContext(), "Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current snapshot of list
        val currentList = ArrayList(tripMap.values)

        // Validate position
        if (position < 0 || position >= currentList.size) {
            Log.e(TAG, "Invalid delete position: $position, list size: ${currentList.size}")
            Toast.makeText(
                requireContext(),
                "Invalid position. Please refresh.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val trip = currentList[position]
        val tripId = trip._id

        if (tripId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid trip ID", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(
            TAG,
            "DELETE REQUEST - Position: $position, ID: $tripId, Source: ${trip.source}, Dest: ${trip.destination}"
        )

        // Set delete state
        isDeleting = true
        pendingDeleteTripId = tripId

        // 🔥 CRITICAL: Remove from map IMMEDIATELY
        tripMap.remove(tripId)

        // 🔥 CRITICAL: Update adapter with new list immediately
        updateAdapterList()

        // Show progress and call API
        context?.showProgress()
        authViewModel.deleteTrip(tripId)
    }

    private fun refreshData() {
        Log.d(TAG, "Refreshing trip data")
        tripMap.clear()
        authViewModel.resetTripPagination()
        authViewModel.getAllTripData(fromDate = startDate, toDate = endDate, size = pageSize)
        (requireActivity() as? DashBoardActivity)?.setData()
    }

    private fun handleDownloadComplete(data: String) {
        dismissProgress()
        if (clickFlag) {
            if (shareFlag) {
                sharePdf(data)
            } else {
                openFile(data)
            }
        }
        clickFlag = false
        shareFlag = false
    }

    private fun String?.toDoubleOrNullSafe(): Double {
        if (this == null) return 0.0
        val cleaned = this.trim().lowercase(Locale.ENGLISH)
        if (cleaned.isEmpty() || cleaned == "null" || cleaned == "nan") return 0.0
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun recordToEntity(record: Record): TripRecordEntity {
        return TripRecordEntity(
            id = record._id ?: "",
            source = record.source ?: "",
            destination = record.destination ?: "",
            start_date = record.start_date ?: "",
            end_date = record.end_date ?: "",
            total_income = record.total_income ?: "0",
            total_expense = record.total_expense ?: "0",
            driver_income = record.driver_income?.takeIf {
                it.isNotBlank() && it.lowercase() != "null"
            } ?: "0",
            owner_profit = record.owner_profit?.takeIf {
                it.isNotBlank() && it.lowercase() != "null"
            } ?: "0",
            truck_average = record.truck_average ?: "0",
            truck_no = record.truck_no ?: "",
            createdDate = record.createdDate ?: "",
            updatedDate = record.updatedDate ?: "",
            incomeJson = gson.toJson(record.income),
            expenseJson = gson.toJson(record.expense)
        )
    }

    private fun entityToRecord(entity: TripRecordEntity): Record {
        val incomeList: List<Income> = try {
            gson.fromJson(entity.incomeJson, object : TypeToken<List<Income>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }

        val expenseList: List<Expense> = try {
            gson.fromJson(entity.expenseJson, object : TypeToken<List<Expense>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }

        return Record(
            __v = 0,
            _id = entity.id,
            createdDate = entity.createdDate,
            destination = entity.destination,
            driver_income = entity.driver_income,
            end_date = entity.end_date,
            expense = expenseList,
            income = incomeList,
            mobile = "",
            owner_profit = entity.owner_profit,
            source = entity.source,
            start_date = entity.start_date,
            total_days = "",
            total_expense = entity.total_expense,
            total_income = entity.total_income,
            truck_average = entity.truck_average,
            truck_no = entity.truck_no,
            updatedDate = entity.updatedDate
        )
    }

    private suspend fun cacheTripRecords(records: List<Record>) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(requireContext()).tripRecordDao()
        dao.upsertAll(records.map { recordToEntity(it) })
    }

    private suspend fun loadCachedTripRecords(from: String, to: String): List<Record> =
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(requireContext()).tripRecordDao()
            dao.getTripsInRange(from, to).map { entityToRecord(it) }
        }

    private fun openFile(data: String) {
        try {
            val file = File(data)
            val uri =
                FileProvider.getUriForFile(requireContext(), requireContext().packageName, file)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file: ${e.message}")
            Toast.makeText(requireContext(), "Cannot open file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setProgressBarVisibility() {
        if (tripMap.isEmpty()) {
            binding.progressbar.visible()
            binding.recycleList.gone()
        } else {
            binding.progressbar.gone()
            binding.recycleList.visible()
        }
    }

    private fun setOnClickListner() {
        binding.addTrip.setOnClickListener(this)
        binding.datePickerLayout.setOnClickListener(this)

        getMaterialDatePicker().addOnPositiveButtonClickListener { selection: Any ->
            val myDates = selection as Pair<Long, Long>

            val dateStart = apiDateFormat.format(Date(myDates.first))
            val dateEnd = apiDateFormat.format(Date(myDates.second))

            startDate = dateStart
            endDate = dateEnd

            binding.date.text = formatDateForDisplay(dateStart, dateEnd)

            tripMap.clear()
            authViewModel.resetTripPagination()
            authViewModel.getAllTripData(fromDate = startDate, toDate = endDate, size = pageSize)
        }
    }

    private fun getMaterialDatePicker(): MaterialDatePicker<*> {
        if (materialDatePicker == null) {
            materialDatePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.select_date))
                .setTheme(R.style.ThemeOverlay_MyDatePicker)
                .setCalendarConstraints(CalendarConstraints.Builder().build())
                .build()
        }
        return materialDatePicker!!
    }

    private fun initDatePicker() {
        val now = Calendar.getInstance()

        val startCal = (now.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        val endCal = (now.clone() as Calendar).apply { add(Calendar.YEAR, 1) }

        startDate = apiDateFormat.format(startCal.time)
        endDate = apiDateFormat.format(endCal.time)

        binding.date.text = formatDateForDisplay(startDate, endDate)
    }

    private fun formatDateForDisplay(dateStart: String, dateEnd: String): String {
        return try {
            val langCode = Prefs[Constants.languageCode] ?: "hi"
            val locale = getLocaleFromLanguageCode(langCode)
            val outputFormat = SimpleDateFormat("dd MMM yyyy", locale)

            val start = apiDateFormat.parse(dateStart)
            val end = apiDateFormat.parse(dateEnd)

            if (start != null && end != null) {
                val formattedStart = outputFormat.format(start)
                val formattedEnd = outputFormat.format(end)
                val toText = getString(R.string.date_range_to)
                "$formattedStart $toText $formattedEnd"
            } else {
                "From: $dateStart To: $dateEnd"
            }
        } catch (e: Exception) {
            "From: $dateStart To: $dateEnd"
        }
    }

    private fun getLocaleFromLanguageCode(code: String): Locale {
        return when (code) {
            "hi" -> Locale("hi", "IN")
            "mr" -> Locale("mr", "IN")
            "gu" -> Locale("gu", "IN")
            "pa" -> Locale("pa", "IN")
            "bn" -> Locale("bn", "IN")
            "or" -> Locale("or", "IN")
            "ta" -> Locale("ta", "IN")
            "te" -> Locale("te", "IN")
            "kn" -> Locale("kn", "IN")
            "ml" -> Locale("ml", "IN")
            "as" -> Locale("as", "IN")
            "ne" -> Locale("ne", "NP")
            "ur" -> Locale("ur", "IN")
            "en" -> Locale("en", "IN")
            else -> Locale("hi", "IN")
        }
    }

    private fun initAdapter() {
        tripListAdapter = TripListAdapter(requireActivity(), this, this, this, this, this)
        binding.recycleList.adapter = tripListAdapter

        binding.recycleList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) &&
                    newState == RecyclerView.SCROLL_STATE_IDLE &&
                    !isLoading &&
                    authViewModel.page < authViewModel.maxPossiblePageCount
                ) {
                    isLoading = true
                    authViewModel.getAllTripData(
                        fromDate = startDate,
                        toDate = endDate,
                        size = pageSize
                    )
                }
            }
        })

        binding.swipeToRefreshBasicDetails.setOnRefreshListener {
            refreshData()
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.addTrip -> {
                Constants.creditList.clear()
                Constants.debitList.clear()
                startActivity(
                    Intent(requireActivity(), MainActivity::class.java).putExtra(
                        "flag",
                        1
                    )
                )
            }

            binding.datePickerLayout -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime
                    getMaterialDatePicker().show(childFragmentManager, "MATERIAL_DATE_PICKER")
                }
            }
        }
    }

    private fun sharePdf(filePdf: String) {
        val pdfFile = File(filePdf)
        val pdfUri =
            FileProvider.getUriForFile(requireContext(), requireContext().packageName, pdfFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
    }

    override fun shareTripDataMethod(position: Int) {
        val currentList = ArrayList(tripMap.values)

        if (position < 0 || position >= currentList.size) {
            Toast.makeText(requireContext(), "Unable to share. Please refresh.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val trip = currentList[position]

        if (trip._id.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid trip", Toast.LENGTH_SHORT).show()
            return
        }

        shareFlag = true
        clickFlag = true
        requireContext().showProgress()

        tripName = "${trip.source} TO ${trip.destination}"
        authViewModel.getParticualrPdf(trip._id)
    }

    override fun deleteTripMethod(position: Int) {
        val currentList = ArrayList(tripMap.values)

        if (position < 0 || position >= currentList.size) {
            Toast.makeText(
                requireContext(),
                "Unable to delete. Please refresh.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        deleteDialogFragment = DeleteDialogFragment(position, "")
        if (!childFragmentManager.isStateSaved) {
            deleteDialogFragment?.show(childFragmentManager, "DeleteDialog")
        }
    }

    override fun dowanloadMethod(position: Int) {
        val currentList = ArrayList(tripMap.values)

        if (position < 0 || position >= currentList.size) {
            Toast.makeText(
                requireContext(),
                "Unable to download. Please refresh.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val trip = currentList[position]

        if (trip._id.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid trip", Toast.LENGTH_SHORT).show()
            return
        }

        shareFlag = false
        clickFlag = true
        requireContext().showProgress()

        tripName = "${trip.source} TO ${trip.destination}"
        authViewModel.getParticualrPdf(trip._id)
    }

    override fun editTripData(trip: Record) {
        if (trip._id.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid trip ID", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Editing trip: ID=${trip._id}, Source=${trip.source}, Dest=${trip.destination}")

        Constants.clearTripData()
        Constants.creditList.clear()
        Constants.debitList.clear()

        val incomeJson = gson.toJson(trip.income)
        val expenseJson = gson.toJson(trip.expense)

        startActivity(
            Intent(requireActivity(), MainActivity::class.java).apply {
                putExtra("sourceName", trip.source)
                putExtra("destinationName", trip.destination)
                putExtra("startingDate", trip.start_date)
                putExtra("endingDate", trip.end_date)
                putExtra("driverAvak", trip.driver_income)
                putExtra("truckNumber", trip.truck_no)
                putExtra("startOdometer", trip.startOdometer ?: "")
                putExtra("endOdometer", trip.endOdometer ?: "")
                putExtra("endKm", trip.endKm ?: "")
                putExtra("isOdometer", trip.isOdometer ?: true)
                putExtra("id", trip._id)
                putExtra("flag", 2)
                putExtra("incomeJson", incomeJson)
                putExtra("expenseJson", expenseJson)
                putStringArrayListExtra("ROUTE_ARRAY", ArrayList(trip.route ?: emptyList()))
            }
        )
    }

    override fun viewTripDataMethod(trip: Record) {
        if (trip._id.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid trip", Toast.LENGTH_SHORT).show()
            return
        }

        val totalDays = try {
            val startDate = apiDateFormat.parse(trip.start_date)
            val endDate = apiDateFormat.parse(trip.end_date)
            if (startDate != null && endDate != null) {
                val diffInMillis = endDate.time - startDate.time
                val days = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                days.toString()
            } else "0"
        } catch (e: Exception) {
            "0"
        }

        val intent = Intent(requireActivity(), TripPreviewActivity::class.java).apply {
            putExtra("TRIP_ID", trip._id)
            putExtra("SOURCE", trip.source)
            putExtra("DESTINATION", trip.destination)
            putExtra("START_DATE", trip.start_date)
            putExtra("END_DATE", trip.end_date)
            putExtra("TRUCK_NO", trip.truck_no)
            putExtra("DRIVER_INCOME", trip.driver_income)
            putExtra("TOTAL_INCOME", trip.total_income)
            putExtra("TOTAL_EXPENSE", trip.total_expense)
            putExtra("OWNER_PROFIT", trip.owner_profit)
            putExtra("TRUCK_AVERAGE", trip.truck_average)
            putExtra("TOTAL_DAYS", totalDays)

            val routeJson = if (trip.route != null && trip.route.isNotEmpty()) {
                gson.toJson(trip.route)
            } else ""
            putExtra("ROUTE_JSON", routeJson)

            putExtra("INCOME_JSON", gson.toJson(trip.income))
            putExtra("EXPENSE_JSON", gson.toJson(trip.expense))
        }

        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tripListAdapter = null
        materialDatePicker = null
    }
}