package com.dadabarbie.TruckTrip.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.languageLocale
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.LocaleHelper
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.adapter.DraftAdapter
import com.dadabarbie.TruckTrip.adapter.TripListAdapter
import com.dadabarbie.TruckTrip.databinding.ActivityDraftBinding
import com.dadabarbie.TruckTrip.diologFragment.DeleteDialogFragment
import com.dadabarbie.TruckTrip.diologFragment.DeleteDraftDialogFragment
import com.dadabarbie.TruckTrip.model.CreditModel
import com.dadabarbie.TruckTrip.model.DebitModel
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

class DraftActivity : AppCompatActivity(), DraftAdapter.DraftEditListner,DraftAdapter.DeleteListner,View.OnClickListener {
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
        MobileAds.initialize(this)
        setLanguage()
        setOnClickListner()

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
            id = entity.id.toString()
        )
    }

    override fun editMethod(position: Int) {
        lifecycleScope.launch(Dispatchers.IO){
            getDelete(applicationContext,draftList[position].id.toInt())
            withContext(Dispatchers.Main) {
                startActivity(
                    Intent(applicationContext, MainActivity::class.java)
                        .putExtra("sourceName", draftList[position].srcPlace)
                        .putExtra("destinationName", draftList[position].destPlace)
                        .putExtra("startingDate", draftList[position].srcDate)
                        .putExtra("endingDate", draftList[position].destDate)
                        .putExtra("truckNumber", draftList[position].truckNumber)
                        .putExtra("driverAvak", draftList[position].driverIncome)
                        .putExtra("flag", 3)
                        .putExtra("id", 0)
                )
                Log.d("listsize", "editMethod: ${ draftList[position].modelList1 as ArrayList<Income>}")
                Constants.creditList.clear()
                Constants.debitList.clear()
                Constants.creditList = draftList[position].modelList1 as ArrayList<Income>
                Constants.debitList = draftList[position].modelList2 as ArrayList<Expense>

                finish()
            }

        }


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
        LocaleHelper.setNewLocale(applicationContext,Prefs[Constants.languageCode,""])
    }

}
