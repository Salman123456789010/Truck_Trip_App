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

object Constants {





    var creditList:ArrayList<com.dadabarbie.TruckTrip.model.addTrip.Income> = arrayListOf()
    var debitList:ArrayList<com.dadabarbie.TruckTrip.model.addTrip.Expense> = arrayListOf()
    var isLogin=""
    var countryCode="+91"
    var mobileNumber="+91 9106695725"
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
    const val supportedLanguages=MIDDLEWARE_HISSAB+"util/supported/languages"
    const val getTripPdf=MIDDLEWARE_HISSAB+"trip/add"
    const val updatedLanguage=MIDDLEWARE_HISSAB+"auth/user/lang"
    const val getAllTripData=MIDDLEWARE_HISSAB+"trip/data"
    const val getParticualrTripPdf=MIDDLEWARE_HISSAB+"trip/generate-pdf"
    const val getAllNews=MIDDLEWARE_HISSAB+"news/feed"
    const val tripDelete=MIDDLEWARE_HISSAB+"trip/delete"
    const val logOut=MIDDLEWARE_HISSAB+"auth/logout"
    const val deleteAccount=MIDDLEWARE_HISSAB+"auth/user/delete"

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

    fun Context.showProgress() {
        dialog = getProgressView(this,true)
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        dialog?.show()
    }
    private fun getProgressView(context: Context?, isWhiteBackground: Boolean): Dialog? {
        val dialog = Dialog(context!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.itemview_loading)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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