package com.dadabarbie.TruckTrip.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
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
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
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
import com.dadabarbie.TruckTrip.adapter.TripListAdapter
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.FragmentHomeBinding
import com.dadabarbie.TruckTrip.diologFragment.DeleteDialogFragment
import com.dadabarbie.TruckTrip.model.getTrip.Record
import com.google.android.gms.ads.MobileAds
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.vasyerp.freshvegetables.util.NetworkResult
import com.vasyerp.cafvd.room.model.TripRecordEntity
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@AndroidEntryPoint
class HomeFragment : Fragment(), View.OnClickListener, TripListAdapter.EditTripDataListner,
    TripListAdapter.ShareTripDataListner, TripListAdapter.DeleteTripListner,
    TripListAdapter.DowanloadListner {
    lateinit var binding: FragmentHomeBinding
    lateinit var tripListAdapter: TripListAdapter
    private val authViewModel: AuthViewModel by viewModels()
    private var tripList: ArrayList<Record> = arrayListOf()
    private var isLoading: Boolean = false
    private val pageSize: Int = 20
    private lateinit var materialDateBuilder: MaterialDatePicker.Builder<Pair<Long, Long>>
    private lateinit var materialDatePicker: MaterialDatePicker<*>
    private lateinit var deleteDialogFragment: DeleteDialogFragment
    var startDate = ""
    private val MIN_CLICK_INTERVAL: Long = 1000  // 1 second
    private var lastClickTime: Long = 0
    var endDate = ""
    var shareFlag = false
    var clickFlag = false
    private var pendingDeleteRecordId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as DashBoardActivity).textChanges(1)
        Log.d("onviewcall", "onViewCreated: ")
        MobileAds.initialize(requireActivity())
        initDatePicker()
        tripList.clear()
        setOnClickListner()
        initViews()
        initAdapter()
        setObserver()
        (requireActivity() as DashBoardActivity).setData()
    }

    private fun setObserver() {
        authViewModel.getAllTripData.observe(requireActivity()) {
            it.getContentIfNotHandled()?.let { event ->
                event.let { it ->
                    when (it) {
                        is NetworkResult.Error -> {
                            GlobalScope.launch {
                                val cached = loadCachedTripRecords(startDate, endDate)
                                if (cached.isNotEmpty()) {
                                    tripList.clear()
                                    tripList.addAll(cached)

                                    // Use safe parsing for decimals and null-like strings
                                    val driverTotalDouble = cached.sumOf { r ->
                                        r.driver_income.toDoubleOrNullSafe()
                                    }
                                    val ownerTotalDouble = cached.sumOf { r ->
                                        r.owner_profit.toDoubleOrNullSafe()
                                    }

                                    val driverTotalStr = driverTotalDouble.toLong().toString()
                                    val ownerTotalStr = ownerTotalDouble.toLong().toString()

                                    requireActivity().runOnUiThread {
                                        binding.drivertotalAavak.text = "₹" +driverTotalStr
                                        binding.totalMalikAavak.text =  "₹" +ownerTotalStr
                                        tripListAdapter.submitList(tripList.toList())
                                        binding.progressbar.gone()
                                        binding.recycleList.visible()
                                        binding.swipeToRefreshBasicDetails.isRefreshing = false
                                    }
                                } else {
                                    requireActivity().runOnUiThread {
//                                        Constants.showSnackBar(
//                                            binding.root, it.message.toString()
//                                        )
                                        binding.progressbar.gone()
                                        binding.recycleList.visible()
                                        binding.swipeToRefreshBasicDetails.isRefreshing = false
                                    }
                                }
                                isLoading = false
                            }
                        }

                        is NetworkResult.Loading -> {
                            isLoading = true
                            setProgressBarVisibility()
                        }

                        is NetworkResult.Success -> {
                            binding.recycleList.visible()
                            binding.progressbar.gone()
                            Log.d("data", "setObserver: ${authViewModel.page}")
                            binding.noProductFound.gone()
                            if (it.data?.status == 200) {
                                binding.swipeToRefreshBasicDetails.isRefreshing = false
                                if (!it.data.data.records.isNullOrEmpty()) {
                                    val newRecords = it.data.data.records
                                    if (tripList.isEmpty()) {
                                        tripList.addAll(newRecords)
                                    } else {
                                        // Append without duplicates (by _id)
                                        val existingIds = tripList.map { r -> r._id }.toHashSet()
                                        tripList.addAll(newRecords.filter { r -> !existingIds.contains(r._id) })
                                    }
                                    GlobalScope.launch { cacheTripRecords(newRecords) }
                                    binding.drivertotalAavak.text =
                                        "₹" +it.data.data.totalDriverIncome.toString()
                                    binding.totalMalikAavak.text =
                                        "₹" +it.data.data.totalOwnerIncome.toString()
                                    tripListAdapter.submitList(tripList.toList())
                                    isLoading = false
                                } else {
                                    GlobalScope.launch {
                                        val db = AppDatabase.getDatabase(requireContext())
                                        val dao = db.tripRecordDao()
                                        dao.clearAll()
                                    }
                                    authViewModel.resetTripPagination()
                                    binding.drivertotalAavak.text = "₹" +"0"
                                    binding.totalMalikAavak.text =  "₹" +"0"
                                    binding.noProductFound.visible()
                                    binding.recycleList.gone()
                                    isLoading = false
                                }

                            } else {
                                if (tripList.isEmpty()) {
                                    authViewModel.resetTripPagination()
                                    binding.noProductFound.visible()
                                    binding.recycleList.gone()
                                }
                                isLoading = false
                            }

                        }
                    }
                }
            }

        }

        authViewModel.getDeleteTripData.observe(requireActivity()) {
            when (it) {
                is NetworkResult.Error -> {
                    dismissProgress()
                    Toast.makeText(
                        requireActivity(),
                        "Failed to delete trip. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Reset pending state
                    pendingDeleteRecordId = null
                }

                is NetworkResult.Loading -> {
//                    context?.showProgress()
                }

                is NetworkResult.Success -> {

                    // Delete from cache in background
                    GlobalScope.launch {
                        try {
                            pendingDeleteRecordId?.let { id ->
                                val db = AppDatabase.getDatabase(requireContext())
                                val dao = db.tripRecordDao()
                                dao.deleteById(id)
                            }
                        } catch (e: Exception) {
                            Log.e("DeleteTrip", "Cache delete failed: ${e.message}")
                        } finally {
                            pendingDeleteRecordId = null
                        }
                    }

                    // Show success message
//                    Toast.makeText(
//                        requireActivity(),
//                        "Trip deleted successfully",
//                        Toast.LENGTH_SHORT
//                    ).show()

                    // Refresh the list with slight delay to ensure backend consistency
                    Handler(Looper.getMainLooper()).postDelayed({
                        tripList.clear()
                        authViewModel.resetTripPagination()
                        authViewModel.getAllTripData(
                            fromDate = startDate,
                            toDate = endDate,
                            size = pageSize
                        )
                    }, 300) // 300ms delay
                }
            }
        }


        Constants.deleteTrip.observe(requireActivity()) {
            it.getContentIfNotHandled()?.let { event ->
                event.let { position ->
                    if (position >= 0) {
                        val record = tripList.getOrNull(position)
                        if (record != null) {
                            // Store the ID for later cache deletion
                            pendingDeleteRecordId = record._id

                            // Optimistically remove from UI list
                            tripList.removeAt(position)
                            tripListAdapter.submitList(tripList.toList())

                            // Call delete API
                            authViewModel.deleteTrip(record._id)
                        } else {
                            Log.e("DeleteTrip", "Invalid position: $position")
                        }
                    }
                }
            }
        }

        Constants.refreshApi.observe(requireActivity()) {
            it.getContentIfNotHandled()?.let { event ->
                event.let {
                    if (it < 0) {

                    } else {
                        tripList.clear()
                        authViewModel.resetTripPagination()
                        authViewModel.getAllTripData(fromDate = startDate, toDate = endDate, size = pageSize)
                        (requireActivity() as DashBoardActivity).setData()
                    }
                }
            }

        }


        authViewModel.downloadProgress.observe(requireActivity()) {}


        authViewModel.downloadCompleted.observe(requireActivity(), Observer { data ->
            // Update your UI with the streamed data
            dismissProgress()
            if (clickFlag) {
                if (shareFlag) {
                    sharePdf(data)
                } else {
                    openFile(data)
                }
            }
            clickFlag = false
            try {

            } catch (e: Exception) {
            }
        })
    }

    // -------------------------
    // Safe conversions & helpers
    // -------------------------

    // Safe parse for numeric string values; returns 0.0 for null/empty/"null"/invalid
    private fun String?.toDoubleOrNullSafe(): Double {
        if (this == null) return 0.0
        val cleaned = this.trim().lowercase(Locale.ENGLISH)
        if (cleaned.isEmpty() || cleaned == "null" || cleaned == "nan") return 0.0
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun recordToEntity(record: Record): TripRecordEntity {
        val gson = Gson()
        val incomeJson = gson.toJson(record.income)
        val expenseJson = gson.toJson(record.expense)

        // safe defaults for numeric-string fields
        val safeDriverIncome = record.driver_income?.takeIf { it.isNotBlank() && it.lowercase(Locale.ENGLISH) != "null" } ?: "0"
        val safeOwnerProfit = record.owner_profit?.takeIf { it.isNotBlank() && it.lowercase(Locale.ENGLISH) != "null" } ?: "0"

        return TripRecordEntity(
            id = record._id ?: "",
            source = record.source ?: "",
            destination = record.destination ?: "",
            start_date = record.start_date ?: "",
            end_date = record.end_date ?: "",
            total_income = record.total_income ?: "0",
            total_expense = record.total_expense ?: "0",
            driver_income = safeDriverIncome,
            owner_profit = safeOwnerProfit,
            truck_average = record.truck_average ?: "0",
            truck_no = record.truck_no ?: "",
            createdDate = record.createdDate ?: "",
            updatedDate = record.updatedDate ?: "",
            incomeJson = incomeJson,
            expenseJson = expenseJson
        )
    }

    private fun entityToRecord(entity: TripRecordEntity): Record {
        val gson = Gson()
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
            _id = entity.id ?: "",
            createdDate = entity.createdDate ?: "",
            destination = entity.destination ?: "",
            driver_income = entity.driver_income ?: "0",
            end_date = entity.end_date ?: "",
            expense = expenseList,
            income = incomeList,
            mobile = "",
            owner_profit = entity.owner_profit ?: "0",
            source = entity.source ?: "",
            start_date = entity.start_date ?: "",
            total_days = "",
            total_expense = entity.total_expense ?: "0",
            total_income = entity.total_income ?: "0",
            truck_average = entity.truck_average ?: "0",
            truck_no = entity.truck_no ?: "",
            updatedDate = entity.updatedDate ?: ""
        )
    }

    private suspend fun cacheTripRecords(records: List<Record>) {
        val db = AppDatabase.getDatabase(requireContext())
        val dao = db.tripRecordDao()
        dao.upsertAll(records.map { recordToEntity(it) })
    }

    private suspend fun loadCachedTripRecords(from: String, to: String): List<Record> {
        val db = AppDatabase.getDatabase(requireContext())
        val dao = db.tripRecordDao()
        return dao.getTripsInRange(from, to).map { entityToRecord(it) }
    }

    // -------------------------
    // File open / share helpers
    // -------------------------
    private fun openFile(data: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val myMime = MimeTypeMap.getSingleton()
            val apkURI = requireActivity()?.let {
                FileProvider.getUriForFile(
                    requireActivity(), it.packageName, File(data)
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
            // handle if needed
        }
    }

    private fun setProgressBarVisibility() {
        if (tripList.isEmpty()) {
            binding.progressbar?.visible()
            binding.recycleList.gone()
        } else {
            binding.progressbar?.gone()
            binding.recycleList.visible()
        }
    }

    private fun setOnClickListner() {
        binding.addTrip.setOnClickListener(this)
        binding.datePickerLayout.setOnClickListener(this)

        materialDatePicker.addOnPositiveButtonClickListener { selection: Any ->
            val myDates = selection as Pair<Long, Long>

            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val simpleDateFormatExport = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val dateStart = simpleDateFormat.format(Date(myDates.first))
            val dateEnd = simpleDateFormat.format(Date(myDates.second))
            startDate = dateStart  // API format - unchanged
            endDate = dateEnd      // API format - unchanged
            val dateStartExport = simpleDateFormatExport.format(Date(myDates.first))
            val dateEndExport = simpleDateFormatExport.format(Date(myDates.second))

            // ✅ ONLY THIS LINE CHANGED - Display format only
            binding.date.text = formatDateForDisplay(dateStart, dateEnd)

            tripList.clear()
            authViewModel.resetTripPagination()
            authViewModel.getAllTripData(fromDate = startDate, toDate = endDate, size = pageSize)
        }
    }

    private fun initDatePicker() {

        materialDateBuilder =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.select_date))
                .setTheme(R.style.ThemeOverlay_MyDatePicker)
                .setCalendarConstraints(
                    CalendarConstraints.Builder().build()
                )

        materialDatePicker = materialDateBuilder.build()

        val now = Calendar.getInstance()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

        // 1 year ago
        val startCal = now.clone() as Calendar
        startCal.add(Calendar.YEAR, -1)

        // 1 year after
        val endCal = now.clone() as Calendar
        endCal.add(Calendar.YEAR, 1)

        startDate = sdf.format(startCal.time)
        endDate = sdf.format(endCal.time)

        binding.date.text = formatDateForDisplay(startDate, endDate)
    }


    private fun getOneYearFromNow(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        return calendar.time
    }

    fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    private fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }

    private fun formatDateForDisplay(dateStart: String, dateEnd: String): String {
        try {
            val langCode = Prefs[Constants.languageCode] ?: "hi"
            val locale = getLocaleFromLanguageCode(langCode)

            // Input format (API format - ALWAYS yyyy-MM-dd)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

            // Output format for display only
            val outputFormat = SimpleDateFormat("dd MMM yyyy", locale)

            val start = inputFormat.parse(dateStart)
            val end = inputFormat.parse(dateEnd)

            if (start != null && end != null) {
                val formattedStart = outputFormat.format(start)
                val formattedEnd = outputFormat.format(end)

                // Get "To" text in user's language from strings.xml
                val toText = getString(R.string.date_range_to)

                return "$formattedStart $toText $formattedEnd"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback
        return "From: $dateStart To: $dateEnd"
    }
    private fun getLocaleFromLanguageCode(code: String): Locale {
        return when (code) {
            "hi" -> Locale("hi", "IN")  // Hindi
            "mr" -> Locale("mr", "IN")  // Marathi
            "gu" -> Locale("gu", "IN")  // Gujarati
            "pa" -> Locale("pa", "IN")  // Punjabi
            "bn" -> Locale("bn", "IN")  // Bengali
            "or" -> Locale("or", "IN")  // Odia
            "ta" -> Locale("ta", "IN")  // Tamil
            "te" -> Locale("te", "IN")  // Telugu
            "kn" -> Locale("kn", "IN")  // Kannada
            "ml" -> Locale("ml", "IN")  // Malayalam
            "as" -> Locale("as", "IN")  // Assamese
            "ne" -> Locale("ne", "NP")  // Nepali
            "ur" -> Locale("ur", "IN")  // Urdu
            "en" -> Locale("en", "IN")  // English
            else -> Locale("hi", "IN")  // Default Hindi
        }
    }
    private fun initViews() {
        Constants.emitDeleteTrip(Event(-1))
        Constants.refreshApiGet(Event(-1))
        tripList.clear()
        authViewModel.resetTripPagination()
        authViewModel.getAllTripData(startDate, endDate, size = pageSize)
    }

    private fun initAdapter() {
        tripListAdapter = TripListAdapter(requireActivity(), this, this, this, this)
        tripListAdapter.submitList(tripList.toList())
        binding.recycleList.adapter = tripListAdapter

        binding.recycleList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!isLoading && authViewModel.page < authViewModel.maxPossiblePageCount) {
                        isLoading = true
                        authViewModel.getAllTripData(fromDate = startDate, toDate = endDate, size = pageSize)
                    }
                }
            }
        })

        binding.swipeToRefreshBasicDetails?.setOnRefreshListener {
            tripList.clear()
            authViewModel.resetTripPagination()
            authViewModel.getAllTripData(fromDate = startDate, toDate = endDate, size = pageSize)
            (requireActivity() as DashBoardActivity).setData()
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.addTrip -> {
                Constants.creditList.clear()
                Constants.debitList.clear()
                startActivity(
                    Intent(requireActivity(), MainActivity::class.java).putExtra("flag", 1)
                )
            }

            binding.datePickerLayout -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime
                    materialDatePicker.show(childFragmentManager, "MATERIAL_DATE_PICKER")
                }
            }
        }
    }

    override fun editTripDataMethod(position: Int) {
        Constants.clearTripData()
        // Clear existing lists to prevent duplicates
        Constants.creditList.clear()
        Constants.debitList.clear()

        // Convert current trip's income and expense to JSON
        val incomeJson = Gson().toJson(tripList[position].income)
        val expenseJson = Gson().toJson(tripList[position].expense)

        // Parse and add to Constants lists BEFORE starting activity
        try {
            val incomeList: List<Income> = Gson().fromJson(
                incomeJson,
                object : TypeToken<List<Income>>() {}.type
            )
            Constants.creditList.addAll(incomeList)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error parsing income JSON: ${e.message}")
        }

        try {
            val expenseList: List<Expense> = Gson().fromJson(
                expenseJson,
                object : TypeToken<List<Expense>>() {}.type
            )
            Constants.debitList.addAll(expenseList)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error parsing expense JSON: ${e.message}")
        }

        // Start MainActivity with trip details
        startActivity(
            Intent(requireActivity(), MainActivity::class.java)
                .putExtra("sourceName", tripList[position].source)
                .putExtra("destinationName", tripList[position].destination)
                .putExtra("startingDate", tripList[position].start_date)
                .putExtra("driverAvak", tripList[position].driver_income)
                .putExtra("endingDate", tripList[position].end_date)
                .putExtra("truckNumber", tripList[position].truck_no)
                .putExtra("flag", 2)
                .putExtra("id", tripList[position]._id)
                .putExtra("incomeJson", incomeJson)
                .putExtra("expenseJson", expenseJson)
        )

        Log.d("HomeFragment", "Editing trip at position: $position, ID: ${tripList[position]._id}")
        Log.d("HomeFragment", "Income count: ${Constants.creditList.size}, Expense count: ${Constants.debitList.size}")
    }

    fun sharePdf(filePdf: String) {
        val pdfFile = File(filePdf)
        val pdfUri = FileProvider.getUriForFile(
            requireContext(), requireContext().applicationContext.packageName, pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "application/pdf"
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant temporary permission to read the file

        context?.startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
    }

    override fun shareTripDataMethod(position: Int) {
        shareFlag = true
        clickFlag = true
        context?.showProgress()
        tripName = tripList[position].source + " TO " + tripList[position].destination
        authViewModel.getParticualrPdf(tripList[position]._id)
    }

    override fun deleteTripMethod(position: Int) {
        if (position >= 0 && position < tripList.size) {
            deleteDialogFragment = DeleteDialogFragment(position, "")
            deleteDialogFragment.show(childFragmentManager, "DeleteDialog")
        } else {
            Log.e("DeleteTrip", "Invalid delete position: $position")
            Toast.makeText(
                requireActivity(),
                "Unable to delete trip. Please refresh and try again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun deleteTripData(position: Int) {
        // implement if needed
    }

    override fun dowanloadMethod(position: Int) {
        shareFlag = false
        clickFlag = true
        context?.showProgress()
        tripName = tripList[position].source + " TO " + tripList[position].destination
        authViewModel.getParticualrPdf(tripList[position]._id)
    }
}
object DateFormatter {

    /**
     * Format date range for display in user's language
     * @param startDate: Start date string (e.g., "2024-01-28")
     * @param endDate: End date string (e.g., "2025-01-28")
     * @param context: Context for string resources
     * @return Formatted date range string
     */
    fun formatDateRange(startDate: String, endDate: String, context: Context): String {
        try {
            val langCode = Prefs[Constants.languageCode] ?: "hi"
            val locale = getLocaleFromCode(langCode)

            // Input format (assumes ISO format: yyyy-MM-dd)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

            // Output format based on language preference
            val outputFormat = SimpleDateFormat(getDatePattern(langCode), locale)

            val start = inputFormat.parse(startDate)
            val end = inputFormat.parse(endDate)

            if (start != null && end != null) {
                val formattedStart = outputFormat.format(start)
                val formattedEnd = outputFormat.format(end)

                // Get "To" text in user's language
                val toText = context.getString(R.string.date_range_to) // Add this to strings.xml

                return "$formattedStart $toText $formattedEnd"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to original format if parsing fails
        return "$startDate To: $endDate"
    }

    /**
     * Get locale from language code
     */
    private fun getLocaleFromCode(code: String): Locale {
        return when (code) {
            "hi" -> Locale("hi", "IN")  // Hindi
            "mr" -> Locale("mr", "IN")  // Marathi
            "gu" -> Locale("gu", "IN")  // Gujarati
            "pa" -> Locale("pa", "IN")  // Punjabi
            "bn" -> Locale("bn", "IN")  // Bengali
            "or" -> Locale("or", "IN")  // Odia
            "ta" -> Locale("ta", "IN")  // Tamil
            "te" -> Locale("te", "IN")  // Telugu
            "kn" -> Locale("kn", "IN")  // Kannada
            "ml" -> Locale("ml", "IN")  // Malayalam
            "as" -> Locale("as", "IN")  // Assamese
            "ne" -> Locale("ne", "NP")  // Nepali
            "ur" -> Locale("ur", "IN")  // Urdu
            "en" -> Locale("en", "IN")  // English
            else -> Locale("hi", "IN")  // Default Hindi
        }
    }

    /**
     * Get appropriate date pattern for each language
     * Adjust patterns based on what looks best in each language
     */
    private fun getDatePattern(code: String): String {
        return when (code) {
            "en" -> "dd MMM yyyy"        // 28 Jan 2024
            "hi" -> "dd MMM yyyy"        // 28 जन 2024
            "mr" -> "dd MMM yyyy"        // 28 जाने 2024
            "gu" -> "dd MMM yyyy"        // 28 જાન્યુ 2024
            "pa" -> "dd MMM yyyy"        // 28 ਜਨ 2024
            "bn" -> "dd MMM yyyy"        // 28 জানু 2024
            "ta" -> "dd MMM yyyy"        // 28 ஜன 2024
            "te" -> "dd MMM yyyy"        // 28 జన 2024
            "kn" -> "dd MMM yyyy"        // 28 ಜನ 2024
            "ml" -> "dd MMM yyyy"        // 28 ജനു 2024
            "or" -> "dd MMM yyyy"        // 28 ଜାନୁ 2024
            "as" -> "dd MMM yyyy"        // 28 জানু 2024
            "ne" -> "dd MMM yyyy"        // 28 जन 2024
            "ur" -> "dd MMM yyyy"        // 28 جنوری 2024
            else -> "dd MMM yyyy"        // Default
        }
    }

    /**
     * Format single date
     */
    fun formatSingleDate(date: String, context: Context): String {
        try {
            val langCode = Prefs[Constants.languageCode] ?: "hi"
            val locale = getLocaleFromCode(langCode)

            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat(getDatePattern(langCode), locale)

            val dateObj = inputFormat.parse(date)
            if (dateObj != null) {
                return outputFormat.format(dateObj)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return date
    }
}