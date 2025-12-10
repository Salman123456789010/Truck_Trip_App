package com.dadabarbie.TruckTrip.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.gone
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.Constants.tripName
import com.dadabarbie.TruckTrip.Utils.Constants.visible
import com.dadabarbie.TruckTrip.Utils.Event
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
        // Inflate the layout for this fragment
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
                                    val driverTotal = cached.sumOf { r -> r.driver_income.toIntOrNull() ?: 0 }
                                    val ownerTotal = cached.sumOf { r -> r.owner_profit.toIntOrNull() ?: 0 }
                                    requireActivity().runOnUiThread {
                                        binding.drivertotalAavak.text = driverTotal.toString()
                                        binding.totalMalikAavak.text = ownerTotal.toString()
                                        tripListAdapter.submitList(tripList.toList())
                                        binding.progressbar.gone()
                                        binding.recycleList.visible()
                                        binding.swipeToRefreshBasicDetails.isRefreshing = false
                                    }
                                } else {
                                    requireActivity().runOnUiThread {
                                        Constants.showSnackBar(
                                            binding.root, it.message.toString()
                                        )
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
                                        it.data.data.totalDriverIncome.toString()
                                    binding.totalMalikAavak.text =
                                        it.data.data.totalOwnerIncome.toString()
                                    tripListAdapter.submitList(tripList.toList())
                                    isLoading = false


                                } else {
                                    GlobalScope.launch {
                                        val db = AppDatabase.getDatabase(requireContext())
                                        val dao = db.tripRecordDao()
                                        dao.clearAll()
                                    }
                                    authViewModel.resetTripPagination()
                                    binding.drivertotalAavak.text = "0"
                                    binding.totalMalikAavak.text = "0"
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

                }

                is NetworkResult.Loading -> {
//                    setProgressBarVisibility()
//                    binding.recycleList.visible()
                }

                is NetworkResult.Success -> {
                    GlobalScope.launch {
                        pendingDeleteRecordId?.let { id ->
                            val db = AppDatabase.getDatabase(requireContext())
                            val dao = db.tripRecordDao()
                            dao.deleteById(id)
                            pendingDeleteRecordId = null
                        }
                    }
                    tripList.clear()
                    authViewModel.resetTripPagination()
                    authViewModel.getAllTripData(fromDate = startDate, toDate = endDate, size = pageSize)
                }
            }
        }

        Constants.deleteTrip.observe(requireActivity()) {
            it.getContentIfNotHandled()?.let { event ->
                event.let {
                    if (it < 0) {

                    } else {
                        val idx = it.toInt()
                        val record = tripList.getOrNull(idx)
                        if (record != null) {
                            pendingDeleteRecordId = record._id
                            authViewModel.deleteTrip(record._id)
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

    private fun recordToEntity(record: Record): TripRecordEntity {
        val gson = Gson()
        val incomeJson = gson.toJson(record.income)
        val expenseJson = gson.toJson(record.expense)
        return TripRecordEntity(
            id = record._id,
            source = record.source,
            destination = record.destination,
            start_date = record.start_date,
            end_date = record.end_date,
            total_income = record.total_income,
            total_expense = record.total_expense,
            driver_income = record.driver_income,
            owner_profit = record.owner_profit,
            truck_average = record.truck_average,
            truck_no = record.truck_no,
            createdDate = record.createdDate,
            updatedDate = record.updatedDate,
            incomeJson = incomeJson,
            expenseJson = expenseJson
        )
    }

    private fun entityToRecord(entity: TripRecordEntity): Record {
        val gson = Gson()
        val incomeList: List<Income> = gson.fromJson(entity.incomeJson, object : TypeToken<List<Income>>() {}.type)
        val expenseList: List<Expense> = gson.fromJson(entity.expenseJson, object : TypeToken<List<Expense>>() {}.type)
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
//            Toast(applicationContext, "Its not open Something issue in your file Please try again",Toast.LENGTH_SHORT).show()
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
            startDate = dateStart
            endDate = dateEnd
            val dateStartExport = simpleDateFormatExport.format(Date(myDates.first))
            val dateEndExport = simpleDateFormatExport.format(Date(myDates.second))
            binding.date.text = "From:$dateStart To:$dateEnd"
            tripList.clear()
            authViewModel.resetTripPagination()
            authViewModel.getAllTripData(fromDate = startDate, toDate = endDate, size = pageSize)
        }
    }

    private fun initDatePicker() {
        materialDateBuilder = MaterialDatePicker.Builder.dateRangePicker().setCalendarConstraints(
            CalendarConstraints.Builder().setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                .setValidator(DateValidatorPointBackward.before(MaterialDatePicker.todayInUtcMilliseconds()))
                .build()
        )
        materialDatePicker = materialDateBuilder.build()
        materialDateBuilder.setTitleText("Select Date")
        val now = getCurrentDateTime()

// 1 year ago
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

        val cal = Calendar.getInstance()

// 1 year ago
        cal.time = now
        cal.add(Calendar.YEAR, -1)
        val dateStart = sdf.format(cal.time)
        val dateStartExport = dateStart

// 1 year after
        cal.time = now
        cal.add(Calendar.YEAR, 1)
        val dateEnd = sdf.format(cal.time)
        val dateEndExport = dateEnd

        startDate = dateStart
        endDate = dateEnd
        binding.date.text = "From:$dateStart To:$dateEnd"

    }

    private fun getOneYearFromNow(): Date {
        // Get the current date
        val calendar = Calendar.getInstance()

        // Add 1 year to the current date
        calendar.add(Calendar.YEAR, -1)

        // Return the time in milliseconds (1 year from now)
        return calendar.time
    }

    fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }


    private fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
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
            //Event Observer call if change any data
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
        val incomeJson = Gson().toJson(tripList[position].income)
        val expenseJson = Gson().toJson(tripList[position].expense)
        startActivity(
            Intent(requireActivity(), MainActivity::class.java).putExtra(
                "sourceName", tripList[position].source
            ).putExtra("destinationName", tripList[position].destination)
                .putExtra("startingDate", tripList[position].start_date)
                .putExtra("driverAvak", tripList[position].driver_income)
                .putExtra("endingDate", tripList[position].end_date)
                .putExtra("truckNumber", tripList[position].truck_no).putExtra("flag", 2)
                .putExtra("id", tripList[position]._id)
                .putExtra("incomeJson", incomeJson)
                .putExtra("expenseJson", expenseJson)
        )
        Log.d("TAG1233", "editTripDataMethod: ${tripList[position].expense as ArrayList<Expense>}")
        Constants.creditList.clear()
        Constants.debitList.clear()
        Constants.creditList =
            tripList[position].income as ArrayList<com.dadabarbie.TruckTrip.model.addTrip.Income>
        Constants.debitList = tripList[position].expense as ArrayList<Expense>
    }

    fun sharePdf(filePdf: String) {
        val pdfFile = File(
            filePdf
        )
        val pdfUri = FileProvider.getUriForFile(
            requireContext(), requireContext().applicationContext.packageName, pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "application/pdf"
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant temporary permission to read the file

        context?.startActivity(Intent.createChooser(shareIntent, "Share PDF via"));
    }

    override fun shareTripDataMethod(position: Int) {
        shareFlag = true
        clickFlag = true
        context?.showProgress()
        tripName = tripList[position].source + " TO " + tripList[position].destination
        authViewModel.getParticualrPdf(tripList[position]._id)
    }

    override fun deleteTripMethod(position: Int) {
        deleteDialogFragment = DeleteDialogFragment(position, "")
        deleteDialogFragment.show(childFragmentManager, "")

    }

    fun deleteTripData(position: Int) {

    }

    override fun dowanloadMethod(position: Int) {
        shareFlag = false
        clickFlag = true
        context?.showProgress()
        tripName = tripList[position].source + " TO " + tripList[position].destination
        authViewModel.getParticualrPdf(tripList[position]._id)
    }

}
