package com.dadabarbie.TruckTrip.Utils

import android.app.Activity
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

object SystemUiUtils {
    fun setupStatusBar(activity: Activity, colorRes: Int, lightBackground: Boolean) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = ContextCompat.getColor(activity, colorRes)
        }
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = lightBackground
    }
}
