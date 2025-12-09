package com.vasyerp.freshvegetables.di

import com.dadabarbie.TruckTrip.BuildConfig
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.api.ApiService
import com.dadabarbie.TruckTrip.api.SecurityInterceptor
import com.google.gson.GsonBuilder
import com.vasyerp.damacas_vendor.di.ProviderGstRetrofitQualifier
import com.vasyerp.damacas_vendor.di.ProviderRetrofitQualifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    @ProviderRetrofitQualifier
    fun provideOkHttpClient() = OkHttpClient.Builder().addInterceptor { chain ->
        //return response
        chain.proceed(
            //create request
            chain.request().newBuilder()
                //add headers to the request builder
                .also {
                    it.addHeader("api-key", BuildConfig.X_API_KEY)
                    it.addHeader("X-API-KEY", BuildConfig.X_API_KEY)
                    it.addHeader("AgentName", BuildConfig.AgentName)
//                    it.addHeader("Authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBYmhpc2hlazIiLCJpYXQiOjE2OTg4Mzk1MDksImV4cCI6MTY5ODkyNTkwOX0.fYcSukNNAXhUMFg6unhjx9bEQvj-dWTCcnmeFy8EC7mNX7LjCIrGjXJyY57-MVubXFOnwBCFjJm0VjTXjFf8KA")
                    if (Prefs[Constants.domainName, ""].toString().isNotEmpty()) {
                        it.addHeader("DOMAINNAME", Prefs[Constants.domainName])
                    }
                    if (Prefs[Constants.authToken, ""].toString().isNotEmpty()) {
                        it.addHeader("Authorization", "Bearer ${Prefs[Constants.authToken, ""]}")
                    }
                }.build()
        )
    }.also { okHttpClient ->
        //log if in debugging phase
        okHttpClient.readTimeout(60, TimeUnit.SECONDS)
        okHttpClient.connectTimeout(60, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpClient.addInterceptor(httpLoggingInterceptor)
        }
        okHttpClient.addInterceptor(SecurityInterceptor())
    }.build()


    var gson = GsonBuilder()
        .setLenient()
        .create()

    @Singleton
    @Provides
    @ProviderGstRetrofitQualifier
    fun provideGSTOkHttpClient() = OkHttpClient.Builder().addInterceptor { chain ->
        //return response
        chain.proceed(
            //create request
            chain.request().newBuilder()
                //add headers to the request builder
                .build()
        )
    }.also { okHttpClient ->
        //log if in debugging phase
        okHttpClient.readTimeout(30, TimeUnit.SECONDS)
        okHttpClient.connectTimeout(30, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpClient.addInterceptor(httpLoggingInterceptor)
        }
    }.build()

    @Singleton
    @Provides
    @ProviderRetrofitQualifier
    fun provideRetrofit(@ProviderRetrofitQualifier okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder().addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(Constants.BASE_URL).client(okHttpClient).build()


    @Singleton
    @Provides
    fun provideApiService(@ProviderRetrofitQualifier retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)


}