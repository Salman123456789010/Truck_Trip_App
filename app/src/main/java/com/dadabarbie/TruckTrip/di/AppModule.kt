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
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Response
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
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .addHeader("api-key", BuildConfig.X_API_KEY)
                    .addHeader("X-API-KEY", BuildConfig.X_API_KEY)
                    .addHeader("x-api-key", BuildConfig.X_API_KEY)
                    .addHeader("AgentName", BuildConfig.AgentName)

                val domain = Prefs[Constants.domainName, ""].toString()
                if (domain.isNotEmpty()) {
                    requestBuilder.addHeader("DOMAINNAME", domain)
                }

                val token = Prefs[Constants.authToken, ""].toString()
                if (token.isNotEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                chain.proceed(requestBuilder.build())
            }
            .also { okHttpClient ->
                if (BuildConfig.DEBUG) {
                    val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    okHttpClient.addInterceptor(httpLoggingInterceptor)
                }
                okHttpClient.addInterceptor(SecurityInterceptor())
            }
            .build()
    }


    var gson = GsonBuilder()
        .setLenient()
        .create()

    @Singleton
    @Provides
    @ProviderGstRetrofitQualifier
    fun provideGSTOkHttpClient() = OkHttpClient.Builder().addInterceptor { chain ->
        chain.proceed(
            chain.request().newBuilder().build()
        )
    }.also { okHttpClient ->
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