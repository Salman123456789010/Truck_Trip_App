package com.dadabarbie.TruckTrip.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.LocaleHelper
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.Utils.RouteUtils
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.adapter.DraftAdapter
import com.dadabarbie.TruckTrip.databinding.ActivityDraftBinding
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DraftActivity : BaseActivity(), DraftAdapter.DraftEditListner,DraftAdapter.DeleteListner,View.OnClickListener {
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
            // 35 (android - 15)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        }
        MobileAds.initialize(this)
        setLanguage()
        setOnClickListner()

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                // Update your UI here
                initAdapter(getAllData(applicationContext))
            }

        }


    }

    private fun setOnClickListner() {
        binding.backBtn.setOnClickListener(this)
    }

    private fun initAdapter(tripList: List<TripDataTestModel>) {
        if(tripList.isNullOrEmpty()){
            onBackPressed()
        }else{
            draftList = tripList as ArrayList<TripDataTestModel>
            draftAdapter = DraftAdapter(this, this,this)
            draftAdapter.submitList(tripList)
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
    private suspend fun getDelete(context: Context,position: Int) {
        val database = AppDatabase.getDatabase(context)
        val tripDataDao = database.productsDao()
        val entities = tripDataDao.deleteTruckById(position)

    }

    private fun convertToModel(entity: Products): TripDataTestModel {
        val gson = Gson()
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
            routeList = entity.routeJson,
            driverIncome = "",
            startOdometer = "",
            endOdometer = "",
            endTripKm = "",
            route =  RouteUtils.parseRouteFromJson(entity.routeJson)
        )
    }

    override fun editMethod(position: Int) {
        val item = draftList[position]

        Constants.creditList.clear()
        Constants.debitList.clear()
        Constants.creditList.addAll(item.modelList1)
        Constants.debitList.addAll(item.modelList2)

        Log.d("DraftActivity", "Editing draft with ID: ${item.randomNumber}")

        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra("flag", 3) // EDIT MODE
                .putExtra("id", item.randomNumber) // This should be the draft ID
                .putExtra("sourceName", item.srcPlace)
                .putExtra("destinationName", item.destPlace)
                .putExtra("startingDate", item.srcDate)
                .putExtra("endingDate", item.destDate)
                .putExtra("truckNumber", item.truckNumber)
                .putExtra("driverAvak", item.driverIncome)
                .putExtra("startOdometer", item.startOdometer)
                .putExtra("routeJson", item.routeList) // Pass route as JSON
                .putStringArrayListExtra("ROUTE_ARRAY", item.route)
        )
        finish()
    }


    override fun deleteMethod(position: Int) {
        deleteDraftDialogFragment= DeleteDraftDialogFragment(position)
        deleteDraftDialogFragment.show(supportFragmentManager,"")
    }
    fun deleteFromDatabse(position: Int){
        lifecycleScope.launch {
            getDelete(applicationContext,draftList[position].id.toInt())
            withContext(Dispatchers.Main) {
                // Update your UI here
                initAdapter(getAllData(applicationContext))
            }
        }
    }
    fun amagram(name1:String,name2:String){
        var name1="abca"
        var removedup=""
        for(item in name1.indices){
            var flag=false
            for(item1 in item+1..name1.length-1){
                if(name1[item]==name1[item1]){

                }
            }
            if(!flag){
                removedup+=name1[item].toString()
            }
        }
        print(removedup)
    }

    override fun onClick(v: View?) {
        when(v){
            binding.backBtn->{
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
