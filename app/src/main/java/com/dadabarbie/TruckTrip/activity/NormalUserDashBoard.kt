package com.dadabarbie.TruckTrip.activity

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.usermobileNumber
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.Utils.UpdateDialog
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityNormalUserDashBoardBinding
import com.dadabarbie.TruckTrip.model.CreditModel
import com.dadabarbie.TruckTrip.model.DebitModel
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.dadabarbie.TruckTrip.room.model.TripDataTestModel
import com.google.android.material.button.MaterialButton
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.vasyerp.cafvd.room.model.Products
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import com.dadabarbie.TruckTrip.Utils.ServerWarmupManager
import com.dadabarbie.TruckTrip.api.ApiService
import javax.inject.Inject

@AndroidEntryPoint
class NormalUserDashBoard : BaseActivity(),View.OnClickListener {
    @Inject
    lateinit var apiService: ApiService

    private val binding: ActivityNormalUserDashBoardBinding by lazy {
        ActivityNormalUserDashBoardBinding.inflate(layoutInflater)
    }

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val authViewModel:AuthViewModel by viewModels()
    var totalCount=0
    var tripData: String=""
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->

    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkAndOpenPdf(intent)
    }

    private fun checkAndOpenPdf(intent: Intent?) {
        val tripData = intent?.getStringExtra("tripData")

        Log.d("NormalUserDashBoard", "🔍 Checking PDF path: $tripData")

        if (!tripData.isNullOrEmpty()) {
            Log.d("NormalUserDashBoard", "✅ Valid PDF path found, opening...")

            // Small delay to ensure activity is fully ready
            Handler(Looper.getMainLooper()).postDelayed({
                openFile(tripData)
            }, 1000)
        } else {
            Log.d("NormalUserDashBoard", "⚠️ No PDF path found")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServerWarmupManager.warmupServer(apiService)
        setContentView(binding.root)

        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            // 35 (android - 15)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        }
        appVersionNameCheck()
        setObserver()
        checkAndOpenPdf(intent)
    }


    private fun openFile(data: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val myMime = MimeTypeMap.getSingleton()
            val apkURI = this?.let {
                FileProvider.getUriForFile(
                    this, it.packageName, File(data)
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

            val intent1 = Intent(this, NormalUserDashBoard::class.java)
            intent1.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent1)
        } catch (e: Exception) {
            val intent = Intent(this, NormalUserDashBoard::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
//            Toast(applicationContext, "Its not open Something issue in your file Please try again",Toast.LENGTH_SHORT).show()
        }

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
                        val serverVersion = it.data?.data?.version
                        val appVersion = getAppVersionName(applicationContext)
                        if (!serverVersion.isNullOrEmpty() &&
                            isAppVersionValid(appVersion, serverVersion)
                        ) {
                            initViews()
                            usermobileNumber= Prefs[Constants.mobileNumber,""].toString()
                            setOnClickListner()
                            getPermission()
                            askNotificationPermission()
                        } else {
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
    private fun isAppVersionValid(appVersion: String, serverVersion: String): Boolean {
        val appParts = appVersion.split(".")
        val serverParts = serverVersion.split(".")

        val maxLength = maxOf(appParts.size, serverParts.size)

        for (i in 0 until maxLength) {
            val appPart = appParts.getOrNull(i)?.toIntOrNull() ?: 0
            val serverPart = serverParts.getOrNull(i)?.toIntOrNull() ?: 0

            if (appPart > serverPart) return true      // app > server ✅
            if (appPart < serverPart) return false     // app < server ❌
        }
        return true // equal version ✅
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
            modelList1 = gson.fromJson(
                entity.modelList1,
                object : TypeToken<List<CreditModel>>() {}.type
            ),
            modelList2 = gson.fromJson(
                entity.modelList2,
                object : TypeToken<List<DebitModel>>() {}.type
            ),
            startOdometer = "",
            endOdometer = "",
            endTripKm = "",
            randomNumber = 1.toString(),
            driverIncome = 0.toString(),
            route = arrayListOf(),
            id ="",
            routeList = entity.routeJson
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
                R.id.homeNormalFragment,
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
            binding.notificationLayout.notificataionList -> {
                if (totalCount > 0) {
                    // Show desi confirmation dialog
                    showDraftConfirmationDialog()
                } else {
                    Toast.makeText(applicationContext, "You Have No Any Draft", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Show Desi Style Draft Confirmation Dialog
     */
    private fun showDraftConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_draft_confirmation)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Find views
        val ivIcon = dialog.findViewById<ImageView>(R.id.ivDraftIcon)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvDraftTitle)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvDraftMessage)
        val tvCount = dialog.findViewById<TextView>(R.id.tvDraftCount)
        val btnYes = dialog.findViewById<MaterialButton>(R.id.btnDraftYes)
        val btnNo = dialog.findViewById<MaterialButton>(R.id.btnDraftNo)

        // Set draft count
        tvCount.text = "$totalCount"

        // Yes button - Go to draft activity
        btnYes.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, SimpleDraftActivity::class.java))
        }

        // No button - Close dialog
        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }


    private fun getPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            // Android 10 and below only
            hasPermission()
        } else {
            // Android 11+ → no permission needed
            // use app-specific storage
        }
    }


    private fun hasPermission(): Boolean {
        return if (checkPermissions()) {
            return true
        } else {
            requestPermissions()
            return false
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
        val locale = Locale(Prefs[Constants.languageCode, ""])
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

    override fun onDestroy() {
        com.dadabarbie.TruckTrip.ads.AdMobManager.destroyBanner()
        super.onDestroy()
    }
}