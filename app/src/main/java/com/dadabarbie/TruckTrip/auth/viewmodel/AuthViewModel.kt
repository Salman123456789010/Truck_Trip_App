package com.dadabarbie.TruckTrip.auth.viewmodel

import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.tripName
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.auth.authmodel.LoginUserResponseModel
import com.dadabarbie.TruckTrip.auth.repo.AuthRepository
import com.dadabarbie.TruckTrip.languagemodel.LanguageResponseModel
import com.dadabarbie.TruckTrip.model.addTrip.AddTripRequestModel
import com.dadabarbie.TruckTrip.model.deleteTrip.DeleteTripResponseModel
import com.dadabarbie.TruckTrip.model.getTrip.TripGetResponseModel
import com.dadabarbie.TruckTrip.model.news.NewsDetailsModel
import com.dadabarbie.TruckTrip.model.fuel.FuelPriceResponseModel
import com.dadabarbie.TruckTrip.model.versionModel.AppVersionModel
import com.vasyerp.damacas_vendor.utils.NetworkHelper
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkHelper: NetworkHelper
): ViewModel() {

    var filePdfPath: String = ""

    private val _res = MutableLiveData<NetworkResult<LoginUserResponseModel>>()
    val res: LiveData<NetworkResult<LoginUserResponseModel>>
        get() = _res

    private val _languageSupported = MutableLiveData<NetworkResult<LanguageResponseModel>>()
    val languageSupported: LiveData<NetworkResult<LanguageResponseModel>>
        get() = _languageSupported

    private val _appVersionName = MutableLiveData<NetworkResult<AppVersionModel>>()
    val appVersionName: LiveData<NetworkResult<AppVersionModel>>
        get() = _appVersionName


    private val _updateLangModel = MutableLiveData<NetworkResult<DeleteTripResponseModel>>()
    val updateLangModel: LiveData<NetworkResult<DeleteTripResponseModel>>
        get() = _updateLangModel
    private val _getAllTripData = MutableLiveData<Event<NetworkResult<TripGetResponseModel>>>()
    val getAllTripData: LiveData<Event<NetworkResult<TripGetResponseModel>>>
        get() = _getAllTripData


    private val _getDeleteTripData = MutableLiveData<NetworkResult<DeleteTripResponseModel>>()
    val getDeleteTripData: LiveData<NetworkResult<DeleteTripResponseModel>>
        get() = _getDeleteTripData


    private val _logOutUser = MutableLiveData<NetworkResult<DeleteTripResponseModel>>()
    val logOutUser: LiveData<NetworkResult<DeleteTripResponseModel>>
        get() = _logOutUser

    private val _deleteAccount = MutableLiveData<NetworkResult<DeleteTripResponseModel>>()
    val deleteAccount: LiveData<NetworkResult<DeleteTripResponseModel>>
        get() = _deleteAccount


    private val _getAllNewsData = MutableLiveData<NetworkResult<NewsDetailsModel>>()
    val getAllNewsData: LiveData<NetworkResult<NewsDetailsModel>>
        get() = _getAllNewsData

    private val _fuelPriceData = MutableLiveData<NetworkResult<FuelPriceResponseModel>>()
    val fuelPriceData: LiveData<NetworkResult<FuelPriceResponseModel>>
        get() = _fuelPriceData

    var attemptForResendOtp: Int = 2
    var otpVerificationTimer: CountDownTimer? = null


    private val _timerTick = MutableLiveData<Long>()
    val timerTick: LiveData<Long>
        get() = _timerTick


    var page: Int = 0
    var maxPossiblePageCount: Int = 1

    var page_news: Int = 0
    var maxPossiblePageCount_news: Int = 1

    private var getAllTripJob: Job? = null

    fun requestOTP(mobileNumber: String, fcmToken: String, idToken: String, lang: String) =
        viewModelScope.launch {
            _res.postValue(NetworkResult.Loading())
            if (networkHelper.isNetworkConnected()) {
                authRepository.requestOTP(mobileNumber, fcmToken = fcmToken, idToken, lang)
                    .onStart {
                        Log.d("sendOTP", "StartStream: ")
                    }.onCompletion {
                    Log.d("sendOTP", "EndStream: ")
                }.onEach {
                    Log.d("sendOTP", "EachStream: ")
                }.collect {
                    _res.postValue(it)
                }
            } else {
                _res.postValue(NetworkResult.Error("No internet connection"))
            }
        }

    fun getAllSupporetdLanguags() = viewModelScope.launch {
        _languageSupported.postValue(NetworkResult.Loading())
        if (networkHelper.isNetworkConnected()) {
            authRepository.getAllSupporetdLanguags().onStart { }.collect {
                _languageSupported.postValue(it)
            }
        } else {
            _languageSupported.postValue(NetworkResult.Error("No internet connection"))
        }
    }

    fun getVersionName() = viewModelScope.launch {
        _appVersionName.postValue(NetworkResult.Loading())
        if (networkHelper.isNetworkConnected()) {
            authRepository.getVersionName().onStart { }.collect {
                _appVersionName.postValue(it)
            }
        } else {
            _appVersionName.postValue(NetworkResult.Error("No internet connection"))
        }
    }

    fun updateLanguage(lang: String) = viewModelScope.launch {
        _updateLangModel.postValue(NetworkResult.Loading())
        if (networkHelper.isNetworkConnected()) {
            authRepository.updateLanguage(lang).onStart { }.collect {
                _updateLangModel.postValue(it)
            }
        }
        else {
            _updateLangModel.postValue(NetworkResult.Error("No internet connection"))
        }
    }


    fun getAllTripData(fromDate: String, toDate: String, size: Int = 20) {
        getAllTripJob?.cancel()
        getAllTripJob = viewModelScope.launch {
            _getAllTripData.postValue(Event(NetworkResult.Loading()))
            if (networkHelper.isNetworkConnected()) {
                authRepository.getAllTripData(fromDate, toDate, page, size).onStart { }.collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            maxPossiblePageCount =
                                ceil((result.data?.data?.totalRecords?.toInt() ?: 0) / size.toDouble()).toInt()
                            if (maxPossiblePageCount > page) {
                                this@AuthViewModel.page += 1
                            }
                            _getAllTripData.postValue(Event(result))
                        }
                        is NetworkResult.Error -> {
                            _getAllTripData.postValue(Event(result))
                        }
                        is NetworkResult.Loading -> {
                            _getAllTripData.postValue(Event(result))
                        }
                    }
                }
            } else {
                _getAllTripData.postValue(Event(NetworkResult.Error("No internet connection")))
            }
        }
    }

    fun resetTripPagination() {
        page = 0
        maxPossiblePageCount = 1
    }

    fun deleteTrip(id:String)=viewModelScope.launch {
        _getDeleteTripData.postValue(NetworkResult.Loading())
        if(networkHelper.isNetworkConnected()){
            authRepository.deleteTrip(id).collect{
                _getDeleteTripData.postValue(it)
            }
        }else{
            _getDeleteTripData.postValue((NetworkResult.Error("No internet connection")))
        }
    }

    fun getUserFeedback(feedback:String,message:String)=viewModelScope.launch {
        _getDeleteTripData.postValue(NetworkResult.Loading())
        if(networkHelper.isNetworkConnected()){
            authRepository.getUserFeedback(feedback,message).collect{
                _getDeleteTripData.postValue(it)
            }
        }else{
            _getDeleteTripData.postValue((NetworkResult.Error("No internet connection")))
        }
    }

    fun logOut()=viewModelScope.launch {
        _logOutUser.postValue(NetworkResult.Loading())
        if(networkHelper.isNetworkConnected()){
            authRepository.logOut().collect{
                _logOutUser.postValue(it)
            }
        }else{
            _logOutUser.postValue((NetworkResult.Error("No internet connection")))
        }
    }

    fun deleteAccount()=viewModelScope.launch {
        _deleteAccount.postValue(NetworkResult.Loading())
        if(networkHelper.isNetworkConnected()){
            authRepository.deleteAccount().collect{
                _deleteAccount.postValue(it)
            }
        }else{
            _deleteAccount.postValue((NetworkResult.Error("No internet connection")))
        }
    }


    fun getAllNews(size: Int=5)=viewModelScope.launch {
        _getAllNewsData.postValue(NetworkResult.Loading())
        if(networkHelper.isNetworkConnected()){
            authRepository.getAllNews(page_news,size).collect{
                maxPossiblePageCount_news =
                    ceil((it.data?.data?.totalRecords?.toInt() ?: 0) / size.toDouble()).toInt()
                if (maxPossiblePageCount_news > page_news) {
                    this@AuthViewModel.page_news += 1
                }
                _getAllNewsData.postValue(it)
            }
        }else{
            _getAllNewsData.postValue(NetworkResult.Error("No internet connection"))
        }
    }

    fun getCitywiseFuelPrices(
        state: String?,
        city: String?,
        page: Int,
        size: Int = 10
    ) = viewModelScope.launch {
        _fuelPriceData.postValue(NetworkResult.Loading())
        if (networkHelper.isNetworkConnected()) {
            authRepository.getCitywiseFuelPrices(state, city, page, size).collect {
                _fuelPriceData.postValue(it)
            }
        } else {
            _fuelPriceData.postValue(NetworkResult.Error("No internet connection"))
        }
    }


    val pdfErrorEvent = MutableLiveData<Event<String>>()

    fun getTripPdf(addTripRequestModel: AddTripRequestModel) {

        val call = authRepository
            .getTripPdfExport()
            .getTripPdf(addTripRequestModel)

        call.enqueue(object : Callback<ResponseBody> {

            override fun onResponse(
                call: Call<ResponseBody>,
                response: retrofit2.Response<ResponseBody>
            ) {
                dismissProgress()

                if (response.isSuccessful && response.body() != null) {

                    savePdfToFile(response.body()!!)

                    Log.d(
                        "TripPdf",
                        "PDF downloaded successfully"
                    )

                } else {

                    val message = when (response.code()) {
                        400 -> "Invalid trip data"
                        401 -> "Session expired. Please login again."
                        404 -> "PDF service not found"
                        408 -> "Request timed out"
                        429 -> "Too many requests. Please try again later."
                        500 -> "Server error. Please try again later."
                        502 -> "Service temporarily unavailable. Please try again."
                        503 -> "Service temporarily unavailable. Please try again."
                        else -> "Unable to generate PDF. Error: ${response.code()}"
                    }

                    Log.e(
                        "TripPdf",
                        "PDF API failed: ${response.code()}"
                    )

                    pdfErrorEvent.postValue(
                        Event(message)
                    )
                }
            }

            override fun onFailure(
                call: Call<ResponseBody>,
                t: Throwable
            ) {
                dismissProgress()

                Log.e(
                    "TripPdf",
                    "PDF API failure",
                    t
                )

                val message = when (t) {
                    is java.net.SocketTimeoutException ->
                        "Request timed out. Please try again."

                    is java.net.UnknownHostException ->
                        "No internet connection."

                    is java.io.IOException ->
                        "Network error. Please try again."

                    else ->
                        "Unable to generate PDF. Please try again."
                }

                pdfErrorEvent.postValue(
                    Event(message)
                )
            }
        })
    }

    fun getParticualrPdf(trip_id: String, printType: String = "") {
        val call = authRepository.getTripPdfExport().getParticularPdf(trip_id, printType)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: retrofit2.Call<okhttp3.ResponseBody>,
                response: retrofit2.Response<okhttp3.ResponseBody>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    savePdfToFile(responseBody)
                    Log.d("line", "onResponse: $responseBody")
//                    val bufferedReader = BufferedReader(InputStreamReader(responseBody.byteStream()))
//
//                    try {
//                        var line: String?
//                        while (bufferedReader.readLine().also { line = it } != null) {
//                            Log.d("line", "onResponse: $line")
//                            // Update LiveData for each line received in the stream
//                            _streamingData.postValue(line!!)
//                        }
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    } finally {
//                        bufferedReader.close()
//                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
                Log.d("line", "onResponse: ${t.message}")
//                _streamingData.postValue("Error: ${t.message}")
            }
        })
    }

    val downloadProgress = MutableLiveData<Int>()

    // LiveData to track the download completion
    val downloadCompleted = MutableLiveData<String>()




    private fun savePdfToFile(body: okhttp3.ResponseBody) {
        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/${tripName+System.currentTimeMillis()}.pdf"
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            val file = File(filePath)
            inputStream = body.byteStream()
            outputStream = FileOutputStream(file)

            val buffer = ByteArray(4096)
            val fileSize = body.contentLength()
            var totalBytesRead: Long = 0
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                val progress = (totalBytesRead * 100 / fileSize).toInt()
                downloadProgress.postValue(progress)  // Post download progress
            }

            outputStream.flush()
            downloadCompleted.postValue("$filePath")

        } catch (e: IOException) {
            e.printStackTrace()
            downloadCompleted.postValue("$filePath")

        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    val tripsReportDownloadCompleted = MutableLiveData<Event<String>>()
    val tripsReportErrorEvent = MutableLiveData<Event<String>>()

    fun downloadTripsReport(fromDate: String, toDate: String, truckNo: String = "") {
        val sanitizedTruckNo = truckNo.trim().ifBlank { null }
        val call = authRepository.exportTripsReport(fromDate, toDate, format = "pdf", truckNo = sanitizedTruckNo)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    val truckLabel = if (truckNo.isBlank()) "All" else truckNo.replace("[^a-zA-Z0-9]".toRegex(), "_")
                    val fileName = "TripReport_${truckLabel}_${System.currentTimeMillis()}.pdf"
                    saveReportPdfToFile(response.body()!!, fileName)
                } else {
                    val errorMsg = try {
                        response.errorBody()?.string()?.let { errStr ->
                            val json = org.json.JSONObject(errStr)
                            json.optString("message", "")
                        } ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                    val displayMsg = if (errorMsg.isNotBlank()) errorMsg else "Failed to generate report (Code: ${response.code()})"
                    tripsReportErrorEvent.postValue(Event(displayMsg))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
                tripsReportErrorEvent.postValue(Event("Error: ${t.localizedMessage ?: "Network request failed"}"))
            }
        })
    }

    val tripsExportDownloadCompleted = MutableLiveData<Event<String>>()
    val tripsExportErrorEvent = MutableLiveData<Event<String>>()

    fun exportTripsReport(fromDate: String, toDate: String, truckNo: String = "") {
        val sanitizedTruckNo = truckNo.trim().ifBlank { null }
        val call = authRepository.exportTripsReport(fromDate, toDate, format = null, truckNo = sanitizedTruckNo)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    val contentDisposition = response.headers()["Content-Disposition"]
                    val contentType = response.headers()["Content-Type"] ?: ""

                    var fileName = ""
                    if (!contentDisposition.isNullOrEmpty() && contentDisposition.contains("filename=")) {
                        fileName = contentDisposition.substringAfter("filename=").replace("\"", "").trim()
                    }
                    if (fileName.isEmpty()) {
                        val ext = when {
                            contentType.contains("excel", ignoreCase = true) || contentType.contains("spreadsheet", ignoreCase = true) -> "xlsx"
                            contentType.contains("csv", ignoreCase = true) -> "csv"
                            contentType.contains("pdf", ignoreCase = true) -> "pdf"
                            else -> "xlsx"
                        }
                        val truckLabel = if (truckNo.isBlank()) "All" else truckNo.replace("[^a-zA-Z0-9]".toRegex(), "_")
                        fileName = "TripExport_${truckLabel}_${System.currentTimeMillis()}.$ext"
                    }
                    saveReportFile(response.body()!!, fileName, isExport = true)
                } else {
                    val errorMsg = try {
                        response.errorBody()?.string()?.let { errStr ->
                            val json = org.json.JSONObject(errStr)
                            json.optString("message", "")
                        } ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                    val displayMsg = if (errorMsg.isNotBlank()) errorMsg else "Failed to export report (Code: ${response.code()})"
                    tripsExportErrorEvent.postValue(Event(displayMsg))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
                tripsExportErrorEvent.postValue(Event("Error: ${t.localizedMessage ?: "Network request failed"}"))
            }
        })
    }

    private fun saveReportPdfToFile(body: ResponseBody, fileName: String) {
        saveReportFile(body, fileName, isExport = false)
    }

    private fun saveReportFile(body: ResponseBody, fileName: String, isExport: Boolean = false) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = File(downloadsDir, fileName)
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            inputStream = body.byteStream()
            outputStream = FileOutputStream(file)

            val buffer = ByteArray(4096)
            val fileSize = body.contentLength()
            var totalBytesRead: Long = 0
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                if (fileSize > 0) {
                    totalBytesRead += bytesRead
                    val progress = (totalBytesRead * 100 / fileSize).toInt()
                    downloadProgress.postValue(progress)
                }
            }

            outputStream.flush()
            if (isExport) {
                tripsExportDownloadCompleted.postValue(Event(file.absolutePath))
            } else {
                tripsReportDownloadCompleted.postValue(Event(file.absolutePath))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val err = "Failed to save file: ${e.localizedMessage}"
            if (isExport) {
                tripsExportErrorEvent.postValue(Event(err))
            } else {
                tripsReportErrorEvent.postValue(Event(err))
            }
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (_: Exception) {}
        }
    }

    fun otpVerificationTimer(){
        if(otpVerificationTimer == null){
            otpVerificationTimer = object : CountDownTimer(60000,1000){
                override fun onTick(millisUntilFinished: Long) {
                    _timerTick.postValue(millisUntilFinished)
                }
                override fun onFinish() {
                }
            }
        }
    }

    private val _subscriptionVerifyResult = MutableLiveData<Event<NetworkResult<com.dadabarbie.TruckTrip.model.subscription.SubscriptionVerifyResponseModel>>>()
    val subscriptionVerifyResult: LiveData<Event<NetworkResult<com.dadabarbie.TruckTrip.model.subscription.SubscriptionVerifyResponseModel>>>
        get() = _subscriptionVerifyResult

    private val _subscriptionStatusResult = MutableLiveData<Event<NetworkResult<com.dadabarbie.TruckTrip.model.subscription.SubscriptionStatusResponseModel>>>()
    val subscriptionStatusResult: LiveData<Event<NetworkResult<com.dadabarbie.TruckTrip.model.subscription.SubscriptionStatusResponseModel>>>
        get() = _subscriptionStatusResult

    fun verifySubscription(
        userId: String,
        productId: String,
        purchaseToken: String,
        orderId: String? = null,
        purchaseTime: Long? = null
    ) = viewModelScope.launch {
        _subscriptionVerifyResult.postValue(Event(NetworkResult.Loading()))
        if (networkHelper.isNetworkConnected()) {
            val req = com.dadabarbie.TruckTrip.model.subscription.SubscriptionVerifyRequestModel(
                userId = userId,
                productId = productId,
                purchaseToken = purchaseToken,
                orderId = orderId,
                purchaseTime = purchaseTime
            )
            authRepository.verifySubscription(req)
                .onEach { result ->
                    if (result is NetworkResult.Success) {
                        val isPrem = result.data?.data?.isPremium == true
                        com.dadabarbie.TruckTrip.billing.SubscriptionManager.setPremium(
                            isPrem,
                            result.data?.data?.expiryDate,
                            result.data?.data?.autoRenewEnabled ?: true
                        )
                    }
                    _subscriptionVerifyResult.postValue(Event(result))
                }
                .collect()
        } else {
            _subscriptionVerifyResult.postValue(Event(NetworkResult.Error("No internet connection")))
        }
    }

    fun checkSubscriptionStatus() = viewModelScope.launch {
        _subscriptionStatusResult.postValue(Event(NetworkResult.Loading()))
        if (networkHelper.isNetworkConnected()) {
            authRepository.getSubscriptionStatus()
                .onEach { result ->
                    if (result is NetworkResult.Success) {
                        val isPrem = result.data?.data?.isPremium == true
                        com.dadabarbie.TruckTrip.billing.SubscriptionManager.setPremium(
                            isPrem,
                            result.data?.data?.expiryDate,
                            result.data?.data?.autoRenewEnabled ?: true
                        )
                    }
                    _subscriptionStatusResult.postValue(Event(result))
                }
                .collect()
        } else {
            _subscriptionStatusResult.postValue(Event(NetworkResult.Error("No internet connection")))
        }
    }
}
