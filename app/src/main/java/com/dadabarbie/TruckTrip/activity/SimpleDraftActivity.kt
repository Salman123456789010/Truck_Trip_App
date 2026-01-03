package com.dadabarbie.TruckTrip.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.LocaleHelper
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
import com.google.android.gms.ads.MobileAds
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vasyerp.cafvd.room.model.Products
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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
            // 35 (android - 15)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        }
        MobileAds.initialize(this)
        setLanguage()
        setOnClickListner()

        Constants.refreshApi.observe(this) {
            it.getContentIfNotHandled()?.let { event ->
                event.let {
                    if (it < 0) {
                        // Do nothing
                    } else {
                        Constants.refreshApiGet(Event(1))
                        finish()
                    }
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
        if (tripList.isNullOrEmpty()) {
            onBackPressed()
        } else {
            draftList = tripList as ArrayList<TripDataTestModel>
            draftAdapter = NewDraftAdapter(this, this, this)
            draftAdapter.submitList(tripList)
            binding.draftList.adapter = draftAdapter
            draftAdapter.notifyDataSetChanged()
        }
    }

    private suspend fun getAllData(context: Context): List<TripDataTestModel> {
        val database = AppDatabase.getDatabase(applicationContext)
        val tripDataDao = database.productsDao()
        val entities = tripDataDao.getAllProducts()
        return entities.map {
            convertToModel(it)
        }
    }

    private suspend fun getDelete(context: Context, position: Int) {
        val database = AppDatabase.getDatabase(applicationContext)
        val tripDataDao = database.productsDao()
        tripDataDao.deleteTruckById(position)
    }

    // ✅ FIXED: Properly parse route from routeJson
    private fun convertToModel(entity: Products): TripDataTestModel {
        val gson = Gson()

        // ✅ Parse route from JSON string
        val parsedRoute = try {
            if (entity.routeJson.isNullOrEmpty()) {
                Log.w("DraftRoute1", "Empty routeJson for draft ${entity.randomNumber}")
                arrayListOf()
            } else {
                val route = RouteUtils.parseRouteFromJson(entity.routeJson)
                Log.d("DraftRoute1", "Parsed route for ${entity.randomNumber}: $route")
                ArrayList(route)
            }
        } catch (e: Exception) {
            Log.e("DraftRoute", "Error parsing route from JSON: ${e.message}", e)
            Log.e("DraftRoute", "RouteJson was: ${entity.routeJson}")
            // Fallback: try to create simple route
            try {
                arrayListOf(entity.srcPlace, entity.destPlace)
            } catch (e2: Exception) {
                arrayListOf()
            }
        }

        return TripDataTestModel(
            truckNumber = entity.truckNumber,
            srcPlace = entity.srcPlace,
            destPlace = entity.destPlace,
            srcDate = entity.srcDate,
            destDate = entity.destDate,
            avg = entity.avg,
            randomNumber = entity.randomNumber,
            modelList1 = gson.fromJson(
                entity.modelList1,
                object : TypeToken<List<Income>>() {}.type
            ),
            modelList2 = gson.fromJson(
                entity.modelList2,
                object : TypeToken<List<Expense>>() {}.type
            ),
            id = entity.id.toString(),
            routeList = entity.routeJson ?: "",  // Keep original JSON
            route = parsedRoute,  // ✅ Use properly parsed route ArrayList
            driverIncome = "",
            startOdometer = "",
            endOdometer = "",
            endTripKm = ""
        )
    }

    override fun editMethod(position: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val draft = draftList[position]

            // ✅ Log route before deletion
            Log.d("DraftRoute", "=== EDIT DRAFT START ===")
            Log.d("DraftRoute", "Draft position: $position")
            Log.d("DraftRoute", "Draft ID: ${draft.id}")
            Log.d("DraftRoute", "Draft randomNumber: ${draft.randomNumber}")
            Log.d("DraftRoute", "Route ArrayList: ${draft.route}")
            Log.d("DraftRoute", "Route size: ${draft.route?.size ?: 0}")
            Log.d("DraftRoute", "RouteJson string: ${draft.routeList}")

            // Delete draft from database
//            getDelete(applicationContext, draft.id.toInt())

            // Clear and set lists
            Constants.creditList.clear()
            Constants.debitList.clear()
            Constants.creditList = draft.modelList1 as ArrayList<Income>
            Constants.debitList = draft.modelList2 as ArrayList<Expense>

            withContext(Dispatchers.Main) {
                // ✅ Ensure route is not null before passing
                val routeToPass = draft.route ?: arrayListOf()

                Log.d("DraftRoute", "Route to pass to intent: $routeToPass")

                val intent = Intent(applicationContext, TruckNumberSpeechActivity::class.java).apply {
                    putExtra("EDIT_MODE", true)
                    putExtra("TRIP_ID", draft.randomNumber)
                    putExtra("TRUCK_NUMBER", draft.truckNumber)
                    putExtra("START_DATE", draft.srcDate)
                    putExtra("END_DATE", draft.destDate)
                    putExtra("START_PLACE", draft.srcPlace)
                    putExtra("END_PLACE", draft.destPlace)
                    putExtra("DRIVER_INCOME", draft.driverIncome)
                    putStringArrayListExtra("ROUTE_ARRAY", routeToPass)  // ✅ Pass the route
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)

                Log.d("DraftRoute", "=== EDIT DRAFT END - Activity Started ===")
                finish()
            }
        }
    }

    override fun deleteMethod(position: Int) {
        deleteDraftDialogFragment = DeleteDraftDialogFragment(position)
        deleteDraftDialogFragment.show(supportFragmentManager, "")
    }

    fun deleteFromDatabse(position: Int) {
        GlobalScope.launch {
            getDelete(applicationContext, draftList[position].id.toInt())
            withContext(Dispatchers.Main) {
                // Update your UI here
                initAdapter(getAllData(applicationContext))
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.backBtn -> {
                onBackPressed()
            }
        }
    }

    private fun setLanguage() {
        val savedLang = Prefs[Constants.languageCode, "en"]

        if (savedLang.isNotEmpty()) {
            val locale = Locale(savedLang)
            Locale.setDefault(locale)

            val config = resources.configuration
            config.setLocale(locale)

            resources.updateConfiguration(
                config,
                resources.displayMetrics
            )
        }
    }

}