package com.dadabarbie.TruckTrip.api

import android.content.Intent
import com.dadabarbie.TruckTrip.TruckTripApplication
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.auth.activity.LoginScreenActivity
import okhttp3.Interceptor

class SecurityInterceptor() : Interceptor {

    private var context = TruckTripApplication.appContext

    @Throws(java.io.IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request: okhttp3.Request = chain.request()
        val response: okhttp3.Response = chain.proceed(request)
        if (response.code == 401) {
            val urlPath = request.url.encodedPath
            val isPublicOrAuthEndpoint = urlPath.contains("auth/login") ||
                    urlPath.contains("util/app/version") ||
                    urlPath.contains("util/supported/languages") ||
                    urlPath.contains("subscription/status")

            val isUserLoggedIn: Boolean = try {
                Prefs[Constants.isLogin, false]
            } catch (_: Exception) {
                false
            }

            if (!isPublicOrAuthEndpoint && isUserLoggedIn) {
                Prefs[Constants.isLogin] = false
                Prefs[Constants.authToken] = ""
                val intent = Intent(context, LoginScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(intent)
            }
        }
        return response
    }

}