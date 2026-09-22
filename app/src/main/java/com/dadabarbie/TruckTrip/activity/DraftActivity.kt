package com.dadabarbie.TruckTrip.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.RouteUtils
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.adapter.DraftAdapter
import com.dadabarbie.TruckTrip.databinding.ActivityDraftBinding
import com.dadabarbie.TruckTrip.diologFragment.DeleteDraftDialogFragment
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.dadabarbie.TruckTrip.room.model.TripDataTestModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vasyerp.cafvd.room.model.Products
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DraftActivity : BaseActivity(), DraftAdapter.DraftEditListner, DraftAdapter.DeleteListner, View.OnClickListener {
    private val binding: ActivityDraftBinding by lazy {
        ActivityDraftBinding.inflate(layoutInflater)
    }
    private var draftList: ArrayList<TripDataTestModel> = arrayListOf()
    lateinit var deleteDraftDialogFragment: DeleteDraftDialogFragment
    lateinit var draftAdapter: DraftAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        setOnClickListner()
    }

    override fun onResume() {
        super.onResume()
        loadDrafts()
    }

    private fun loadDrafts() {
        lifecycleScope.launch {
            val data = getAllData(applicationContext)
            withContext(Dispatchers.Main) {
                initAdapter(data)
            }
        }
    }

    private fun setOnClickListner() {
        binding.backBtn.setOnClickListener(this)
    }

    private fun initAdapter(tripList: List<TripDataTestModel>) {
        draftList = ArrayList(tripList)
        if (draftList.isEmpty()) {
            binding.draftList.visibility = View.GONE
            binding.noProductFound.visibility = View.VISIBLE
        } else {
            binding.draftList.visibility = View.VISIBLE
            binding.noProductFound.visibility = View.GONE
            draftAdapter = DraftAdapter(this, this, this)
            draftAdapter.submitList(draftList)
            binding.draftList.adapter = draftAdapter
            draftAdapter.notifyDataSetChanged()
        }
    }

    private suspend fun getAllData(context: Context): List<TripDataTestModel> {
        val database = AppDatabase.getDatabase(context)
        val tripDataDao = database.productsDao()
        val entities = tripDataDao.getAllProducts()
        return entities.map {
            convertToModel(it)
        }
    }

    private suspend fun deleteDraftFromDb(context: Context, item: TripDataTestModel) {
        val database = AppDatabase.getDatabase(context)
        val tripDataDao = database.productsDao()
        try {
            if (!item.randomNumber.isNullOrEmpty()) {
                tripDataDao.deleteByRandomNumber(item.randomNumber)
            }
            val idInt = item.id.toIntOrNull()
            if (idInt != null) {
                tripDataDao.deleteTruckById(idInt)
            }
        } catch (e: Exception) {
            Log.e("DraftActivity", "Error deleting draft: ${e.message}", e)
        }
    }

    private fun convertToModel(entity: Products): TripDataTestModel {
        val gson = Gson()
        val creditList: List<Income> = try {
            if (!entity.modelList1.isNullOrEmpty()) {
                gson.fromJson(entity.modelList1, object : TypeToken<List<Income>>() {}.type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("DraftActivity", "Error parsing modelList1: ${e.message}")
            emptyList()
        }

        val debitList: List<Expense> = try {
            if (!entity.modelList2.isNullOrEmpty()) {
                gson.fromJson(entity.modelList2, object : TypeToken<List<Expense>>() {}.type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("DraftActivity", "Error parsing modelList2: ${e.message}")
            emptyList()
        }

        val parsedRoute = try {
            RouteUtils.parseRouteFromJson(entity.routeJson)
        } catch (e: Exception) {
            arrayListOf()
        }

        return TripDataTestModel(
            truckNumber = entity.truckNumber ?: "",
            srcPlace = entity.srcPlace ?: "",
            destPlace = entity.destPlace ?: "",
            srcDate = entity.srcDate ?: "",
            destDate = entity.destDate ?: "",
            avg = entity.avg ?: "",
            randomNumber = entity.randomNumber ?: "",
            modelList1 = creditList,
            modelList2 = debitList,
            id = entity.id.toString(),
            routeList = entity.routeJson ?: "",
            driverIncome = "",
            startOdometer = "",
            endOdometer = "",
            endTripKm = "",
            route = parsedRoute
        )
    }

    override fun editMethod(position: Int) {
        if (position < 0 || position >= draftList.size) return
        val item = draftList[position]

        Constants.creditList.clear()
        Constants.debitList.clear()
        Constants.creditList.addAll(item.modelList1)
        Constants.debitList.addAll(item.modelList2)

        Log.d("DraftActivity", "Editing draft with ID: ${item.randomNumber}")

        startActivity(
            Intent(this, CreateTripActivity::class.java)
                .putExtra("EDIT_MODE", true)
                .putExtra("TRIP_ID", item.randomNumber)
                .putExtra("sourceName", item.srcPlace)
                .putExtra("destinationName", item.destPlace)
                .putExtra("startingDate", item.srcDate)
                .putExtra("endingDate", item.destDate)
                .putExtra("truckNumber", item.truckNumber)
                .putExtra("driverAvak", item.driverIncome)
                .putExtra("startOdometer", item.startOdometer)
                .putExtra("routeJson", item.routeList)
                .putStringArrayListExtra("ROUTE_ARRAY", item.route)
        )
        finish()
    }

    override fun deleteMethod(position: Int) {
        if (position < 0 || position >= draftList.size) return
        deleteDraftDialogFragment = DeleteDraftDialogFragment(position)
        deleteDraftDialogFragment.show(supportFragmentManager, "")
    }

    fun deleteFromDatabse(position: Int) {
        if (position < 0 || position >= draftList.size) return
        val item = draftList[position]
        lifecycleScope.launch {
            deleteDraftFromDb(applicationContext, item)
            val updatedData = getAllData(applicationContext)
            withContext(Dispatchers.Main) {
                initAdapter(updatedData)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.backBtn -> finish()
        }
    }
}
