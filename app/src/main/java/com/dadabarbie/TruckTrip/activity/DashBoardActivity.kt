package com.dadabarbie.TruckTrip.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.usermobileNumber
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.Utils.UpdateDialog
import com.dadabarbie.TruckTrip.adapter.TripListAdapter
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityDashBoardBinding
import com.dadabarbie.TruckTrip.model.CreditModel
import com.dadabarbie.TruckTrip.model.DebitModel
import com.dadabarbie.TruckTrip.model.TripListModel
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.dadabarbie.TruckTrip.room.model.TripDataTestModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vasyerp.cafvd.room.model.Products
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class DashBoardActivity : AppCompatActivity(), OnClickListener {
    lateinit var binding: ActivityDashBoardBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val authViewModel:AuthViewModel by viewModels()
    var totalCount=0
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.dadabarbie.TruckTrip.Utils.SystemUiUtils.setupStatusBar(this, R.color.green, false)
        appVersionNameCheck()
        setObserver()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setObserver() {
        authViewModel.appVersionName.observe(this){
            when(it){
                is NetworkResult.Error -> {
                    initViews()
                    usermobileNumber= Prefs[Constants.mobileNumber,""].toString()
                    setOnClickListner()
                    getPermission()
                    askNotificationPermission()
                }
                is NetworkResult.Loading -> {
                    initViews()
                    usermobileNumber= Prefs[Constants.mobileNumber,""].toString()
                    setOnClickListner()
                    getPermission()
                    askNotificationPermission()
                }
                is NetworkResult.Success -> {
                    if(getAppVersionName(applicationContext)!="Unknown"){
                        if(getAppVersionName(applicationContext)== it.data?.data?.version ?: ""){
                            initViews()
                            usermobileNumber= Prefs[Constants.mobileNumber,""].toString()
                            setOnClickListner()
                            getPermission()
                            askNotificationPermission()
                        }else{
                            UpdateDialog(this).show()
                        }
                    } else {
                        initViews()
                        usermobileNumber= Prefs[Constants.mobileNumber,""].toString()
                        setOnClickListner()
                        getPermission()
                        askNotificationPermission()
                    }
                }
            }
        }

    }

    private fun appVersionNameCheck() {
        authViewModel.getVersionName()
    }
    fun getAppVersionName(context: Context): String {
        return try {
            val packageManager = context.packageManager
            val packageName = context.packageName

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33 and above
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                // Below API 33
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }

            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        setData()
    }
    fun setData(){
        totalCount=0
        GlobalScope.launch {
            totalCount=getAllData(applicationContext).size
            Log.d("call1", "onResume: ${totalCount}")
            binding.notificationLayout.notificationCount.text=""+totalCount.toString()
        }
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
            modelList1 = gson.fromJson(entity.modelList1, object : TypeToken<List<CreditModel>>() {}.type),
            modelList2 = gson.fromJson(entity.modelList2, object : TypeToken<List<DebitModel>>() {}.type)
        )
    }
    private suspend fun getAllData(context: Context): List<TripDataTestModel> {
        val database = AppDatabase.getDatabase(context)
        val tripDataDao = database.productsDao()
        val entities = tripDataDao.getAllProducts()
        return entities.map {
            convertToModel(it)
        }
    }
    private fun initViews() {
        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.nav_host_fragment
        ) as NavHostFragment

        navController = navHostFragment.navController
        val bottomNavigationView = binding.bottomNavView
        bottomNavigationView.setupWithNavController(navController)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.vahanInfoFragment,
                R.id.newsFragment,
                R.id.profileFragment
            )
        )
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
    }

    private fun setOnClickListner() {
        binding.notificationLayout.notificataionList.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.notificationLayout.notificataionList->{
                if(totalCount>0){
                    startActivity(Intent(this, DraftActivity::class.java))
                }else{
                    Toast.makeText(applicationContext,"You Have No Any Draft",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }


    @RequiresApi(Build.VERSION_CODES.R)
    private fun getPermission(){
        hasManageExternalStoragePermission()

    }
    @RequiresApi(Build.VERSION_CODES.R)
    private fun hasManageExternalStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Environment.isExternalStorageManager()) {

                true
            } else {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        } else {
                            TODO("VERSION.SDK_INT < R")
                        }
                    }
                    false
                } catch (e: Exception) {
                    true //if anything needs adjusting it would be this
                }
            }
        } else {
            if (checkPermissions()) {
                return true
            } else {
                requestPermissions()
                return false
            }
        }
        // assumed storage permissions granted
    }

    private fun checkPermissions(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val result1 = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 100
        )
    }
    fun textChanges(type: Int) {
        val locale = Locale(Prefs[Constants.languageCode,""])
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
        totalCount=0
        GlobalScope.launch {
            totalCount=getAllData(applicationContext).size
            binding.notificationLayout.notificationCount.text=""+totalCount.toString()
        }
        when (type) {
            1 -> {
                binding.tittle.text = getString(R.string.my_trip_list)
            }

            2 -> {
                binding.tittle.text = getString(R.string.vahan_info)
            }

            3 -> {
                binding.tittle.text = getString(R.string.news)
            }

            4 -> {
                binding.tittle.text = getString(R.string.profile)
            }
        }
    }

}
