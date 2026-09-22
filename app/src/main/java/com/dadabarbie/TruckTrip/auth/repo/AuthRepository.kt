package com.dadabarbie.TruckTrip.auth.repo

import com.dadabarbie.TruckTrip.Utils.BaseApiResponse
import com.dadabarbie.TruckTrip.api.ApiService
import com.dadabarbie.TruckTrip.auth.authmodel.LoginUserRequestModel
import com.dadabarbie.TruckTrip.auth.authmodel.LoginUserResponseModel
import com.dadabarbie.TruckTrip.languagemodel.LanguageResponseModel
import com.dadabarbie.TruckTrip.model.deleteTrip.DeleteTripRequestModel
import com.dadabarbie.TruckTrip.model.deleteTrip.DeleteTripResponseModel
import com.dadabarbie.TruckTrip.model.getTrip.TripGetResponseModel
import com.dadabarbie.TruckTrip.model.fuel.FuelPriceResponseModel
import com.dadabarbie.TruckTrip.model.news.NewsDetailsModel
import com.dadabarbie.TruckTrip.model.news.NewsGetRequestModel
import com.dadabarbie.TruckTrip.model.versionModel.AppVersionModel
import com.vasyerp.freshvegetables.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Query
import javax.inject.Inject

class AuthRepository @Inject constructor(private val apiService: ApiService) : BaseApiResponse() {

    suspend fun requestOTP(
        mobileNumber: String, fcmToken: String, idToken: String,lang:String
    ): Flow<NetworkResult<LoginUserResponseModel>> {
        return flow<NetworkResult<LoginUserResponseModel>> {
            emit(safeApiCall {
                apiService.loginUser(
                    LoginUserRequestModel(
                        fcmToken, mobileNumber, idToken,lang
                    )
                )
            })
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getAllSupporetdLanguags(): Flow<NetworkResult<LanguageResponseModel>> {
        return flow {
            emit(safeApiCall { apiService.getAllSupporetdLanguags() })
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getVersionName():Flow<NetworkResult<AppVersionModel>>{
        return flow {
            emit(safeApiCall { apiService.getVersionName() })
        }.flowOn(Dispatchers.IO)
    }

    suspend fun updateLanguage(lang: String):Flow<NetworkResult<DeleteTripResponseModel>> {
        return flow {
            emit(safeApiCall { apiService.updateLanguage(com.dadabarbie.TruckTrip.auth.authmodel.UpdateLanguageRequestModel(lang)) })
        }.flowOn(Dispatchers.IO)
    }

    fun getTripPdfExport(): ApiService {
        return apiService
    }

    fun getParticularPdf(tripId: String, printType: String = ""): Call<ResponseBody> {
        return apiService.getParticularPdf(tripId, printType)
    }

    suspend fun getAllTripData(
        from_date: String, to_date: String, page: Int, size: Int
    ): Flow<NetworkResult<TripGetResponseModel>> {
        return flow<NetworkResult<TripGetResponseModel>> {
            emit(safeApiCall {
                apiService.getAllTripData(
                    from_date, to_date, page, size
                )
            })
        }.flowOn(
            Dispatchers.IO
        )
    }
    suspend fun getUserFeedback(feedback: String, message: String): Flow<NetworkResult<DeleteTripResponseModel>> {
        return flow {
            emit(safeApiCall { apiService.userFeedback(com.dadabarbie.TruckTrip.model.FeedBackRequest(feedback, message)) })
        }.flowOn(Dispatchers.IO)
    }


    suspend fun getAllNews(page: Int, size: Int): Flow<NetworkResult<NewsDetailsModel>> {
        return flow {
            emit(safeApiCall {
                apiService.getAllNews(
                        page,
                        size
                )
            })
        }.flowOn(
            Dispatchers.IO
        )
    }

    suspend fun getCitywiseFuelPrices(
        state: String?,
        city: String?,
        page: Int,
        size: Int
    ): Flow<NetworkResult<FuelPriceResponseModel>> {
        return flow {
            emit(
                safeApiCall {
                    apiService.getCitywiseFuelPrices(
                        state,
                        city,
                        page,
                        size
                    )
                }
            )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun logOut():Flow<NetworkResult<DeleteTripResponseModel>>{
        return flow {
            emit(safeApiCall { apiService.logOut() })
        }.flowOn(Dispatchers.IO)
    }

    suspend fun deleteAccount():Flow<NetworkResult<DeleteTripResponseModel>>{
        return flow {
            emit(safeApiCall { apiService.deleteAccount() })
        }.flowOn(Dispatchers.IO)
    }


    suspend fun deleteTrip(id:String):Flow<NetworkResult<DeleteTripResponseModel>>{
        return flow {
            emit(safeApiCall { apiService.deleteTrip(DeleteTripRequestModel(id)) })
        }.flowOn(Dispatchers.IO)
    }

    fun getTripsReport(
        fromDate: String,
        toDate: String,
        format: String? = null,
        truckNo: String? = null
    ): Call<ResponseBody> {
        return apiService.getTripsReport(fromDate, toDate, format, truckNo?.ifBlank { null })
    }

    fun exportTripsReport(
        fromDate: String,
        toDate: String,
        format: String? = null,
        truckNo: String? = null
    ): Call<ResponseBody> {
        return apiService.exportTripsReport(fromDate, toDate, format, truckNo?.ifBlank { null })
    }

    suspend fun verifySubscription(
        request: com.dadabarbie.TruckTrip.model.subscription.SubscriptionVerifyRequestModel
    ): Flow<NetworkResult<com.dadabarbie.TruckTrip.model.subscription.SubscriptionVerifyResponseModel>> {
        return flow {
            emit(safeApiCall { apiService.verifySubscription(request) })
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getSubscriptionStatus(): Flow<NetworkResult<com.dadabarbie.TruckTrip.model.subscription.SubscriptionStatusResponseModel>> {
        return flow {
            emit(safeApiCall { apiService.getSubscriptionStatus() })
        }.flowOn(Dispatchers.IO)
    }
}
