package com.dadabarbie.TruckTrip.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.LocaleHelper
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.adapter.DraftAdapter
import com.dadabarbie.TruckTrip.adapter.NewDraftAdapter
import com.dadabarbie.TruckTrip.databinding.ActivityDraftBinding
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

class SimpleDraftActivity : AppCompatActivity(), NewDraftAdapter.DraftEditListner,NewDraftAdapter.DeleteListner,View.OnClickListener {
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
        MobileAds.initialize(this)
        setLanguage()
        setOnClickListner()

        Constants.refreshApi.observe(this) {
            it.getContentIfNotHandled()?.let { event ->
                event.let {
                    if (it < 0) {

                    } else {
                        Constants.refreshApiGet(Event(1))
                        finish()
                    }
                }
            }

        }

        GlobalScope.launch {
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
            draftAdapter = NewDraftAdapter(this, this,this)
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
    private suspend fun getDelete(context: Context, position: Int) {
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
            id = entity.id.toString()
        )
    }

    override fun editMethod(position: Int) {
        val draft = draftList[position]
        Constants.creditList.clear()
        Constants.debitList.clear()
        Constants.creditList = draft.modelList1 as ArrayList<Income>
        Constants.debitList = draft.modelList2 as ArrayList<Expense>

        val intent = Intent(applicationContext, TruckNumberSpeechActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("TRIP_ID", draft.randomNumber)
            putExtra("TRUCK_NUMBER", draft.truckNumber)
            putExtra("START_DATE", draft.srcDate)
            putExtra("END_DATE", draft.destDate)
            putExtra("START_PLACE", draft.srcPlace)
            putExtra("END_PLACE", draft.destPlace)
            putExtra("DRIVER_INCOME", draft.driverIncome)
            // Add any other fields if needed, but Products entity is limited
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun deleteMethod(position: Int) {
        deleteDraftDialogFragment= DeleteDraftDialogFragment(position)
        deleteDraftDialogFragment.show(supportFragmentManager,"")
    }
    fun deleteFromDatabse(position: Int){
        GlobalScope.launch {
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
    fun setLanguage(){
        LocaleHelper.setNewLocale(applicationContext, Prefs[Constants.languageCode,""])
    }
}