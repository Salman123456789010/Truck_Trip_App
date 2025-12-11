package com.dadabarbie.TruckTrip.auth.viewmodel

import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dadabarbie.TruckTrip.Utils.Constants.tripName
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.auth.authmodel.LoginUserResponseModel
import com.dadabarbie.TruckTrip.auth.repo.AuthRepository
import com.dadabarbie.TruckTrip.languagemodel.LanguageResponseModel
import com.dadabarbie.TruckTrip.model.addTrip.AddTripRequestModel
import com.dadabarbie.TruckTrip.model.deleteTrip.DeleteTripResponseModel
import com.dadabarbie.TruckTrip.model.getTrip.TripGetResponseModel
import com.dadabarbie.TruckTrip.model.news.NewsDetailsModel
import com.dadabarbie.TruckTrip.model.versionModel.AppVersionModel
import com.vasyerp.damacas_vendor.utils.NetworkHelper
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
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

    var filePdfPath:String=""

    private val _res = MutableLiveData<NetworkResult<LoginUserResponseModel>>()
    val res: LiveData<NetworkResult<LoginUserResponseModel>>
        get() = _res

    private val _languageSupported = MutableLiveData<NetworkResult<LanguageResponseModel>>()
    val languageSupported: LiveData<NetworkResult<LanguageResponseModel>>
        get() = _languageSupported

    private val _appVersionName= MutableLiveData<NetworkResult<AppVersionModel>>()
    val appVersionName: LiveData<NetworkResult<AppVersionModel>>
        get() = _appVersionName

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

    var attemptForResendOtp : Int = 2
    var otpVerificationTimer : CountDownTimer? = null


    private val _timerTick = MutableLiveData<Long>()
    val timerTick : LiveData<Long>
        get() = _timerTick



    var page: Int = 0
    var maxPossiblePageCount: Int = 1

    var page_news: Int = 0
    var maxPossiblePageCount_news: Int = 1

    fun requestOTP(mobileNumber: String, fcmToken:String,idToken:String,lang:String) = viewModelScope.launch {
        _res.postValue(NetworkResult.Loading())
        if (networkHelper.isNetworkConnected()) {
            authRepository.requestOTP(mobileNumber, fcmToken = fcmToken,idToken,lang).onStart {
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

    fun getAllSupporetdLanguags()=viewModelScope.launch {
        _languageSupported.postValue(NetworkResult.Loading())
        if(networkHelper.isNetworkConnected()){
            authRepository.getAllSupporetdLanguags().onStart {  }.collect{
                _languageSupported.postValue(it)
            }
        } else{
            _languageSupported.postValue(NetworkResult.Error("No internet connection"))
        }
    }

    fun getVersionName()=viewModelScope.launch {
        _appVersionName.postValue(NetworkResult.Loading())
        if(networkHelper.isNetworkConnected()){
            authRepository.getVersionName().onStart {  }.collect{
                _appVersionName.postValue(it)
            }
        } else{
            _appVersionName.postValue(NetworkResult.Error("No internet connection"))
        }
    }

    fun getAllTripData(fromDate: String, toDate: String, size: Int = 20) = viewModelScope.launch {
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


    fun getTripPdf(addTripRequestModel: AddTripRequestModel) {
        val call = authRepository.getTripPdfExport().getTripPdf(addTripRequestModel)

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

    fun getParticualrPdf( trip_id:String) {
        val call = authRepository.getTripPdfExport().getParticularPdf(trip_id)

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

}
