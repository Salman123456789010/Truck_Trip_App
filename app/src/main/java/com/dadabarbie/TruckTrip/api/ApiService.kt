package com.dadabarbie.TruckTrip.api

import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.auth.authmodel.LoginUserRequestModel
import com.dadabarbie.TruckTrip.auth.authmodel.LoginUserResponseModel
import com.dadabarbie.TruckTrip.languagemodel.LanguageResponseModel
import com.dadabarbie.TruckTrip.auth.authmodel.UpdateLanguageRequestModel
import com.dadabarbie.TruckTrip.auth.repo.SubscriptionStatusResponse
import com.dadabarbie.TruckTrip.auth.repo.VerifyPurchaseRequest
import com.dadabarbie.TruckTrip.auth.repo.VerifyPurchaseResponse
import com.dadabarbie.TruckTrip.model.FeedBackRequest
import com.dadabarbie.TruckTrip.model.addTrip.AddTripRequestModel
import com.dadabarbie.TruckTrip.model.deleteTrip.DeleteTripRequestModel
import com.dadabarbie.TruckTrip.model.deleteTrip.DeleteTripResponseModel
import com.dadabarbie.TruckTrip.model.getTrip.TripGetResponseModel
import com.dadabarbie.TruckTrip.model.news.NewsDetailsModel
import com.dadabarbie.TruckTrip.model.fuel.FuelPriceResponseModel
import com.dadabarbie.TruckTrip.model.news.NewsGetRequestModel
import com.dadabarbie.TruckTrip.model.versionModel.AppVersionModel
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {

    @POST(Constants.login)
    suspend fun loginUser(
        @Body loginUserRequestModel: LoginUserRequestModel
    ): Response<LoginUserResponseModel>

    @POST("subscription/verify")
    suspend fun verifyPurchase(
        @Header("Authorization") token: String,
        @Body request: VerifyPurchaseRequest
    ): Response<VerifyPurchaseResponse>

    @GET("subscription/status")
    suspend fun getSubscriptionStatus(
        @Header("Authorization") token: String
    ): Response<SubscriptionStatusResponse>

    @GET(Constants.supportedLanguages)
    suspend fun getAllSupporetdLanguags(): Response<LanguageResponseModel>


    @POST(Constants.updatedLanguage)
    suspend fun updateLanguage(
        @Body request: UpdateLanguageRequestModel
    ): Response<DeleteTripResponseModel>

    @GET(Constants.appVersion)
    suspend fun getVersionName():Response<AppVersionModel>

    @Streaming
    @POST(Constants.getTripPdf)  // Replace with your streaming endpoint
    fun getTripPdf(
        @Body addTripRequestModel: AddTripRequestModel
    ): Call<ResponseBody>

    @POST(Constants.userFeedback)
    suspend fun userFeedback(@Body request: FeedBackRequest): Response<DeleteTripResponseModel>
    @GET(Constants.getAllTripData)
    suspend fun getAllTripData(
        @Query("from_date") from_date: String,
        @Query("to_date") to_date: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<TripGetResponseModel>

    @GET(Constants.getParticualrTripPdf)
    fun getParticularPdf(
        @Query("trip_id") trip_id: String,
        @Query("printType") printType: String = ""
    ): Call<ResponseBody>

    @GET(Constants.getAllNews)
    suspend fun getAllNews(
        @Query("page") page: Int,
        @Query("size") size: Int
    ):Response<NewsDetailsModel>

    @POST(Constants.tripDelete)
    suspend fun deleteTrip(
      @Body deleteTripRequestModel: DeleteTripRequestModel
    ):Response<DeleteTripResponseModel>

    @POST(Constants.logOut)
    suspend fun logOut():Response<DeleteTripResponseModel>

    @POST(Constants.deleteAccount)
    suspend fun deleteAccount():Response<DeleteTripResponseModel>

    @GET(Constants.citywiseFuelPrice)
    suspend fun getCitywiseFuelPrices(
        @Query("state") state: String?,
        @Query("city") city: String?="",
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<FuelPriceResponseModel>

    @Streaming
    @GET(Constants.getTripsReport)
    fun getTripsReport(
        @Query("from_date") fromDate: String,
        @Query("to_date") toDate: String,
        @Query("format") format: String? = null,
        @Query("truck_no") truckNo: String? = null
    ): Call<ResponseBody>

    @Streaming
    @GET(Constants.exportTripsReport)
    fun exportTripsReport(
        @Query("from_date") fromDate: String,
        @Query("to_date") toDate: String,
        @Query("format") format: String? = null,
        @Query("truck_no") truckNo: String? = null
    ): Call<ResponseBody>

    @POST(Constants.subscriptionVerify)
    suspend fun verifySubscription(
        @Body request: com.dadabarbie.TruckTrip.model.subscription.SubscriptionVerifyRequestModel
    ): Response<com.dadabarbie.TruckTrip.model.subscription.SubscriptionVerifyResponseModel>

    @GET(Constants.subscriptionStatus)
    suspend fun getSubscriptionStatus(): Response<com.dadabarbie.TruckTrip.model.subscription.SubscriptionStatusResponseModel>
}
