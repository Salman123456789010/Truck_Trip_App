package com.dadabarbie.TruckTrip.Utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.model.CreditModel
import com.dadabarbie.TruckTrip.model.DebitModel
import com.dadabarbie.TruckTrip.model.getTrip.Expense
import com.dadabarbie.TruckTrip.model.getTrip.Income
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.PhoneAuthProvider
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object Constants {



    fun Context.getExpenseText(typeKey: String): String {
        return when (typeKey) {
            "Food" -> getString(R.string.food)
            "Toll/Highway Charges" -> getString(R.string.toll_tax)
            "Puncture/Tire Repair" -> getString(R.string.puncture_tire)
            "Driver Expanse(Kharcha)" -> getString(R.string.driver_kharch)
            "Truck Maintenance" -> getString(R.string.maintenance)
            "Loading Charges" -> getString(R.string.loading_charge)
            "Unloading Charges" -> getString(R.string.unloading_charge)
            "Parking Charges" -> getString(R.string.parking_charge)
            "Police/RTO Fine" -> getString(R.string.police_rto_fine)
            "Documents/Papers" -> getString(R.string.document_paper_kharcha)
            "Broker/Commission" -> getString(R.string.broker_commission)
            "Engine Oil/Lubricants" -> getString(R.string.engine_oil_lubricants)
            "Truck Cleaning/Washing" -> getString(R.string.truck_service)
            "Phone/Communication" -> getString(R.string.phone_recharge)
            "Tea" -> getString(R.string.tea_snacks)
            "Hotel/Accommodation" -> getString(R.string.hotel_stay)
            "Permit/Entry Tax" -> getString(R.string.border_entry)
            "Weighbridge Charges" -> getString(R.string.weighbridge_charges)
            "Spare Parts" -> getString(R.string.spare_parts)
            "Driver Bath/Facilities" -> getString(R.string.driver_personal_kharcha)
            "Bulb/Electrical Items" -> getString(R.string.truck_electrical_items)
            "Mechanic Charges" -> getString(R.string.mechanic_charges)
            "Road Repair Contribution" -> getString(R.string.road_repair_contribution)
            "Tools/Equipment" -> getString(R.string.truck_tools_equipment)
            "Medicine/First Aid" -> getString(R.string.medicine_first_aid)
            "Challan/Fine" -> getString(R.string.challan_fine)
            "Truck Insurance" -> getString(R.string.truck_insurance)
            "Registration/Fitness" -> getString(R.string.registration_fitness)
            "Market Fee/Mandi Charges" -> getString(R.string.mandi_charges)
            "OTHER" -> getString(R.string.other_expenses)
            else -> getString(R.string.other_expenses)
        }
    }

    fun Context.getExpenseKeyFromText(text: String): String {
        return when (text.trim()) {

            getString(R.string.food) -> "Food"
            getString(R.string.toll_tax) -> "Toll/Highway Charges"
            getString(R.string.puncture_tire) -> "Puncture/Tire Repair"
            getString(R.string.driver_kharch) -> "Driver Expanse(Kharcha)"
            getString(R.string.maintenance) -> "Truck Maintenance"

            getString(R.string.loading_charge) -> "Loading Charges"
            getString(R.string.unloading_charge) -> "Unloading Charges"
            getString(R.string.parking_charge) -> "Parking Charges"

            getString(R.string.police_rto_fine) -> "Police/RTO Fine"
            getString(R.string.document_paper_kharcha) -> "Documents/Papers"
            getString(R.string.broker_commission) -> "Broker/Commission"

            getString(R.string.engine_oil_lubricants) -> "Engine Oil/Lubricants"
            getString(R.string.truck_service) -> "Truck Cleaning/Washing"
            getString(R.string.phone_recharge) -> "Phone/Communication"
            getString(R.string.tea_snacks) -> "Tea"

            getString(R.string.hotel_stay) -> "Hotel/Accommodation"
            getString(R.string.border_entry) -> "Permit/Entry Tax"
            getString(R.string.weighbridge_charges) -> "Weighbridge Charges"

            getString(R.string.spare_parts) -> "Spare Parts"
            getString(R.string.driver_personal_kharcha) -> "Driver Bath/Facilities"
            getString(R.string.truck_electrical_items) -> "Bulb/Electrical Items"
            getString(R.string.mechanic_charges) -> "Mechanic Charges"

            getString(R.string.road_repair_contribution) -> "Road Repair Contribution"
            getString(R.string.truck_tools_equipment) -> "Tools/Equipment"
            getString(R.string.medicine_first_aid) -> "Medicine/First Aid"

            getString(R.string.challan_fine) -> "Challan/Fine"
            getString(R.string.truck_insurance) -> "Truck Insurance"
            getString(R.string.registration_fitness) -> "Registration/Fitness"
            getString(R.string.mandi_charges) -> "Market Fee/Mandi Charges"

            getString(R.string.other_expenses) -> "OTHER"

            else -> "OTHER"
        }
    }




    var creditList:ArrayList<com.dadabarbie.TruckTrip.model.addTrip.Income> = arrayListOf()
    var debitList:ArrayList<com.dadabarbie.TruckTrip.model.addTrip.Expense> = arrayListOf()
    const val isLogin = "isLogin"
    const val countryCode = "+91"
    const val mobileNumber = "mobileNumber"
    var tripName=""
    var resendToken: PhoneAuthProvider.ForceResendingToken?=null
    private var dialog: Dialog? = null
    private val _events = MutableLiveData<Event<CreditModel>>()
    val events: LiveData<Event<CreditModel>> = _events

    //    const val BASE_URL = "http://192.168.149.133:8077/"
    const val BASE_URL = "https://truck-trip-hisab.vercel.app/"
//    const val BASE_URL = "http://192.168.1.11:8000/"
    private const val MIDDLEWARE_HISSAB = "api/"


    const val login=MIDDLEWARE_HISSAB+"auth/login"
    const val appVersion=MIDDLEWARE_HISSAB+"util/app/version"
    const val userFeedback=MIDDLEWARE_HISSAB+"util/user/feedback"
    const val supportedLanguages=MIDDLEWARE_HISSAB+"util/supported/languages"
    const val getTripPdf=MIDDLEWARE_HISSAB+"trip/add"
    const val updatedLanguage=MIDDLEWARE_HISSAB+"auth/user/lang"
    const val getAllTripData=MIDDLEWARE_HISSAB+"trip/data"
    const val getParticualrTripPdf=MIDDLEWARE_HISSAB+"trip/generate-pdf"
    const val getAllNews=MIDDLEWARE_HISSAB+"news/feed"
    const val tripDelete=MIDDLEWARE_HISSAB+"trip/delete"
    const val logOut=MIDDLEWARE_HISSAB+"auth/logout"
    const val deleteAccount=MIDDLEWARE_HISSAB+"auth/user/delete"
    const val citywiseFuelPrice = MIDDLEWARE_HISSAB + "util/citywise/fuelprice"
    const val getTripsReport = MIDDLEWARE_HISSAB + "trip/report"
    const val exportTripsReport = MIDDLEWARE_HISSAB + "trip/report/export"
    const val subscriptionVerify = MIDDLEWARE_HISSAB + "subscription/verify"
    const val subscriptionStatus = MIDDLEWARE_HISSAB + "subscription/status"

    // Subscription & Billing Constants
    const val PRODUCT_ID_PREMIUM = "truck_trip_premium"
    const val BASE_PLAN_MONTHLY = "monthly"
    const val PREF_IS_PREMIUM = "is_premium_user"
    const val PREF_PREMIUM_EXPIRY = "premium_expiry_date"
    const val PREF_PREMIUM_AUTO_RENEW = "premium_auto_renew"
    const val PREF_BONUS_TRIPS = "bonus_trips_reward_count"
    const val FREE_TRIP_LIMIT = 10
    const val BONUS_TRIPS_PER_AD = 5

    const val loginUser="LOGIN_USERS"
    var languageLocale=""

    const val english = "English"
    const val hindi = "हिंदी"
    const val telgu = "తెలుగు"
    const val gujrati = "ગુજરાતી"
    const val punjabi = "ਪੰਜਾਬੀ"
    const val tamil = "தமிழ்"
    const val marathi = "मराठी"
    const val bangla = "বাংলা"
    const val malyalam = "മലയാളം"
    const val kannada = "ಕನ್ನಡ"
    const val aasamise = "অসমীয়া"
    const val maithili = "मैथिल"
    const val odisa = "ଓଡିଆ"
    const val nepali = "नेपाली"
    const val dogri = "डोगरी"
    const val bhojpuri = "भोजपुरी"
    const val rajastani = "राजस्थानी"
    const val domainName = "domainName"

    const val authToken = "authToken"
    const val appMode = "appMode"

    var usermobileNumber=""

    const val languageCode = "languageCode"



    fun emitEvent(event: Event<CreditModel>) {
        _events.postValue(event)
    }

    private val _debitevents = MutableLiveData<Event<DebitModel>>()
    val debitevents: LiveData<Event<DebitModel>> = _debitevents

    fun emitDebitEvent(event: Event<DebitModel>) {
        _debitevents.postValue(event)
    }
    fun clearTripData() {
        creditList.clear()
        debitList.clear()
    }

    private val _avg = MutableLiveData<Event<String>>()
    val avg: LiveData<Event<String>> = _avg

    fun emitAvg(event: Event<String>) {
        _avg.postValue(event)
    }



    private val _deleteTrip = MutableLiveData<Event<Int>>()
    val deleteTrip: LiveData<Event<Int>> = _deleteTrip

    fun emitDeleteTrip(event: Event<Int>) {
        _deleteTrip.postValue(event)
    }

    private val _refreshApi=MutableLiveData<Event<Int>>()
    val refreshApi:LiveData<Event<Int>> = _refreshApi

    fun refreshApiGet(event: Event<Int>){
        _refreshApi.postValue(event)
    }

    fun Context.showProgress(message: String? = null) {
        dialog = getProgressView(this, message)
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        dialog?.show()
    }

    private fun getProgressView(context: Context?, message: String? = null): Dialog? {
        if (context == null) return null
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.itemview_loading)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (!message.isNullOrEmpty()) {
            val tvMsg = dialog.findViewById<TextView>(R.id.tvLoadingMessage)
            tvMsg?.text = message
        }
        dialog.show()
        return dialog
    }
    fun dismissProgress() {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
    }

    fun showSnackBar(mainView: View, message: String, isError: Boolean = true) {
        val snackBarView = Snackbar.make(mainView, message, Snackbar.LENGTH_LONG)
        val view = snackBarView.view
        val params: ViewGroup.LayoutParams = view.layoutParams
        if (params is CoordinatorLayout.LayoutParams) {
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.width = WindowManager.LayoutParams.MATCH_PARENT
        } else {
            (params as FrameLayout.LayoutParams).gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.width = WindowManager.LayoutParams.MATCH_PARENT
        }
        view.layoutParams = params
        if (isError) {
            view.setBackgroundColor(Color.parseColor("#B00020")) // for custom background
        } else {
            view.setBackgroundColor(Color.parseColor("#198754")) // for custom background
        }
        snackBarView.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
        snackBarView.show()
        /*  Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
              .show()*/
    }
    fun View.visible() {
        this.visibility = View.VISIBLE
    }
    fun View.gone() {
        this.visibility = View.GONE
    }
}
object DraftIdManager {
    fun generate(): String = UUID.randomUUID().toString()
}

object DateUtils {

    fun uiToDbDate(
        uiDate: String,
        langCode: String = "en"
    ): String {
        if (uiDate.isBlank()) return ""

        val inputFormats = listOf(
            SimpleDateFormat("dd MMM yyyy", Locale(langCode)),
            SimpleDateFormat("dd MMMM yyyy", Locale(langCode)), // July
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
        )

        for (format in inputFormats) {
            try {
                val date = format.parse(uiDate)
                if (date != null) {
                    return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date)
                }
            } catch (_: Exception) { }
        }

        // If already DB format, return as is
        if (uiDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            return uiDate
        }

        // Fallback (safe)
        return ""
    }
}

// Add to your Utils or create NetworkUtils.kt
object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
}
