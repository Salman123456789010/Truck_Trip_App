package com.dadabarbie.TruckTrip.api

import android.content.Intent
import com.dadabarbie.TruckTrip.TruckTripApplication
import com.dadabarbie.TruckTrip.auth.activity.LoginScreenActivity
import okhttp3.Interceptor

class SecurityInterceptor() : Interceptor {

    private var context = TruckTripApplication.appContext

    @Throws(java.io.IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request: okhttp3.Request = chain.request()
        val response: okhttp3.Response = chain.proceed(request)
        if (response.code == 401) {
            context.startActivity(
                Intent(context, LoginScreenActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            )
        }
        return response
    }

}