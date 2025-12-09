package com.dadabarbie.TruckTrip.api

import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.auth.authmodel.LoginUserRequestModel
import com.dadabarbie.TruckTrip.auth.authmodel.LoginUserResponseModel
import com.dadabarbie.TruckTrip.languagemodel.LanguageResponseModel
import com.dadabarbie.TruckTrip.model.addTrip.AddTripRequestModel
import com.dadabarbie.TruckTrip.model.deleteTrip.DeleteTripRequestModel
import com.dadabarbie.TruckTrip.model.deleteTrip.DeleteTripResponseModel
import com.dadabarbie.TruckTrip.model.getTrip.TripGetResponseModel
import com.dadabarbie.TruckTrip.model.news.NewsDetailsModel
import com.dadabarbie.TruckTrip.model.news.NewsGetRequestModel
import com.dadabarbie.TruckTrip.model.versionModel.AppVersionModel
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {

    @POST(Constants.login)
    suspend fun loginUser(
        @Body loginUserRequestModel: LoginUserRequestModel
    ): Response<LoginUserResponseModel>

    @GET(Constants.supportedLanguages)
    suspend fun getAllSupporetdLanguags(): Response<LanguageResponseModel>
    @GET(Constants.appVersion)
    suspend fun getVersionName():Response<AppVersionModel>

    @Streaming
    @POST(Constants.getTripPdf)  // Replace with your streaming endpoint
    fun getTripPdf(
        @Body addTripRequestModel: AddTripRequestModel
    ): Call<ResponseBody>

    @GET(Constants.getAllTripData)
    suspend fun getAllTripData(
        @Query("from_date") from_date: String,
        @Query("to_date") to_date: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<TripGetResponseModel>

    @GET(Constants.getParticualrTripPdf)
    fun getParticularPdf(
        @Query("trip_id") trip_id: String
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



}