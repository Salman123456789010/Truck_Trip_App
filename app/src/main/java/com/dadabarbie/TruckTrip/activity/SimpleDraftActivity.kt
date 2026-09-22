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
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.Utils.RouteUtils
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.adapter.NewDraftAdapter
import com.dadabarbie.TruckTrip.databinding.ActivitySimpleDraftBinding
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
import java.util.Locale

class SimpleDraftActivity : BaseActivity(), NewDraftAdapter.DraftEditListner, NewDraftAdapter.DeleteListner, View.OnClickListener {
    private val binding: ActivitySimpleDraftBinding by lazy {
        ActivitySimpleDraftBinding.inflate(layoutInflater)
    }
    private var draftList: ArrayList<TripDataTestModel> = arrayListOf()
    lateinit var deleteDraftDialogFragment: DeleteDraftDialogFragment
    lateinit var draftAdapter: NewDraftAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        setLanguage()
        setOnClickListner()

        Constants.refreshApi.observe(this) { eventWrapper ->
            eventWrapper.getContentIfNotHandled()?.let { event ->
                if (event >= 0) {
                    Constants.refreshApiGet(Event(1))
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDrafts()
    }

    private fun loadDrafts() {
        lifecycleScope.launch(Dispatchers.IO) {
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
            draftAdapter = NewDraftAdapter(this, this, this)
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
            Log.e("SimpleDraftActivity", "Error deleting draft: ${e.message}", e)
        }
    }

    private fun convertToModel(entity: Products): TripDataTestModel {
        val gson = Gson()
        val parsedRoute = try {
            if (entity.routeJson.isNullOrEmpty()) {
                arrayListOf()
            } else {
                val route = RouteUtils.parseRouteFromJson(entity.routeJson)
                ArrayList(route)
            }
        } catch (e: Exception) {
            try {
                arrayListOf(entity.srcPlace, entity.destPlace)
            } catch (e2: Exception) {
                arrayListOf()
            }
        }

        val creditList: List<Income> = try {
            if (!entity.modelList1.isNullOrEmpty()) {
                gson.fromJson(entity.modelList1, object : TypeToken<List<Income>>() {}.type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("SimpleDraftActivity", "Error parsing modelList1: ${e.message}")
            emptyList()
        }

        val debitList: List<Expense> = try {
            if (!entity.modelList2.isNullOrEmpty()) {
                gson.fromJson(entity.modelList2, object : TypeToken<List<Expense>>() {}.type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("SimpleDraftActivity", "Error parsing modelList2: ${e.message}")
            emptyList()
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
            route = parsedRoute,
            driverIncome = "",
            startOdometer = "",
            endOdometer = "",
            endTripKm = ""
        )
    }

    override fun editMethod(position: Int) {
        if (position < 0 || position >= draftList.size) return
        val draft = draftList[position]

        Constants.creditList.clear()
        Constants.debitList.clear()
        Constants.creditList = draft.modelList1 as ArrayList<Income>
        Constants.debitList = draft.modelList2 as ArrayList<Expense>

        val routeToPass = draft.route ?: arrayListOf()

        val intent = Intent(applicationContext, CreateTripActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("TRIP_ID", draft.randomNumber)
            putExtra("TRUCK_NUMBER", draft.truckNumber)
            putExtra("START_DATE", draft.srcDate)
            putExtra("END_DATE", draft.destDate)
            putExtra("START_PLACE", draft.srcPlace)
            putExtra("END_PLACE", draft.destPlace)
            putExtra("DRIVER_INCOME", draft.driverIncome)
            putStringArrayListExtra("ROUTE_ARRAY", routeToPass)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
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

    private fun setLanguage() {
        val savedLang = Prefs[Constants.languageCode, "en"]
        if (savedLang.isNotEmpty()) {
            val locale = Locale(savedLang)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}