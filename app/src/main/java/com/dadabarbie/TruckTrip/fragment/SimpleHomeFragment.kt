package com.dadabarbie.TruckTrip.fragment

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.util.Pair
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
import com.dadabarbie.TruckTrip.activity.NormalUserDashBoard
import com.dadabarbie.TruckTrip.activity.TruckNumberSpeechActivity
import com.dadabarbie.TruckTrip.adapter.TripListAdapter
import com.dadabarbie.TruckTrip.adapter.TripNewListAdapter
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.FragmentHomeBinding
import com.dadabarbie.TruckTrip.databinding.FragmentSimpleHomeBinding
import com.dadabarbie.TruckTrip.diologFragment.DeleteDialogFragment
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.dadabarbie.TruckTrip.model.getTrip.Record
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.google.android.gms.ads.MobileAds
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vasyerp.cafvd.room.model.TripRecordEntity
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.logging.Handler
import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SimpleHomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class SimpleHomeFragment : Fragment(),View.OnClickListener, TripNewListAdapter.EditTripDataListner,
    TripNewListAdapter.ShareTripDataListner, TripNewListAdapter.DeleteTripListner,
    TripNewListAdapter.DowanloadListner  {
   lateinit var binding: FragmentSimpleHomeBinding
    lateinit var tripListAdapter: TripNewListAdapter
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
    private var arrowAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentSimpleHomeBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as NormalUserDashBoard).textChanges(1)
        MobileAds.initialize(requireActivity())
        setupAddTripButton()
        initDatePicker()
        tripList.clear()
        setOnClickListner()
        initViews()
        initAdapter()
        setObserver()
        (requireActivity() as NormalUserDashBoard).setData()
    }

    private fun setupAddTripButton() {
        // Start arrow animation
        startArrowAnimation()

        binding.addTrip.setOnClickListener {
            // Stop animation when clicked
            stopArrowAnimation()

            // Navigate to add trip
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }
    }

// FIXED: Working Animation Code for ExtendedFloatingActionButton


    private fun startArrowAnimation() {
        arrowAnimator = ValueAnimator.ofFloat(0f, 10f, 0f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                binding.addTrip.translationX = value
            }

            start()
        }
        Log.d("Animation", "✅ Button animation started")
    }

    private fun stopArrowAnimation() {
        arrowAnimator?.cancel()
        arrowAnimator = null
        binding.addTrip.translationX = 0f
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
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
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
                        (requireActivity() as NormalUserDashBoard).setData()
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
        tripListAdapter = TripNewListAdapter(requireActivity(), this, this, this, this)
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
            (requireActivity() as NormalUserDashBoard).setData()
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.addTrip -> {
                Constants.creditList.clear()
                Constants.debitList.clear()
                startActivity(
                    Intent(requireActivity(), TruckNumberSpeechActivity::class.java).putExtra("flag", 1)
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
        Constants.creditList.clear()
        Constants.debitList.clear()

        val trip = tripList[position]

        // Parse income and expense
        try {
            val incomeJson = Gson().toJson(trip.income)
            val expenseJson = Gson().toJson(trip.expense)

            val incomeList: List<Income> = Gson().fromJson(
                incomeJson,
                object : TypeToken<List<Income>>() {}.type
            )
            Constants.creditList.addAll(incomeList)

            val expenseList: List<Expense> = Gson().fromJson(
                expenseJson,
                object : TypeToken<List<Expense>>() {}.type
            )
            Constants.debitList.addAll(expenseList)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error parsing income/expense: ${e.message}")
        }

        // Navigate to TruckNumberSpeechActivity with edit flag
        val intent = Intent(requireContext(), TruckNumberSpeechActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("TRIP_ID", trip._id)
            putExtra("TRUCK_NUMBER", trip.truck_no)
            putExtra("START_DATE", trip.start_date)
            putExtra("END_DATE", trip.end_date)
            putExtra("START_PLACE", trip.source)
            putExtra("END_PLACE", trip.destination)
            putExtra("DRIVER_INCOME", trip.driver_income)
            putExtra("TOTAL_INCOME", trip.total_income)
            putExtra("TOTAL_EXPENSE", trip.total_expense)

            // Pass original values for change detection
            putExtra("ORIGINAL_TRUCK_NUMBER", trip.truck_no)
            putExtra("ORIGINAL_START_DATE", trip.start_date)
            putExtra("ORIGINAL_END_DATE", trip.end_date)
            putExtra("ORIGINAL_START_PLACE", trip.source)
            putExtra("ORIGINAL_END_PLACE", trip.destination)
        }

        startActivity(intent)

        Log.d("HomeFragment", "Starting edit for trip: ${trip._id}")
        Log.d("HomeFragment", "Income: ${Constants.creditList.size}, Expense: ${Constants.debitList.size}")
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