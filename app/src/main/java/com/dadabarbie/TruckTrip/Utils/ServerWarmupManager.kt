package com.dadabarbie.TruckTrip.Utils

import android.util.Log
import com.dadabarbie.TruckTrip.api.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ServerWarmupManager {

    private const val TAG = "ServerWarmupManager"
    private var lastWarmupTime: Long = 0L
    private const val MIN_WARMUP_INTERVAL_MS = 3 * 60 * 1000L // 3 minutes

    fun warmupServer(apiService: ApiService?) {
        val now = System.currentTimeMillis()
        if (now - lastWarmupTime < MIN_WARMUP_INTERVAL_MS) {
            Log.d(TAG, "Server container was warmed up recently, skipping ping.")
            return
        }
        lastWarmupTime = now

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Pre-warming server container in background...")
                val response = apiService?.getVersionName()
                if (response != null && response.isSuccessful) {
                    Log.d(TAG, "Server container pre-warmed successfully!")
                } else {
                    Log.d(TAG, "Server container ping returned code: ${response?.code()}")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Server pre-warm ping executed (caught cold start): ${e.message}")
            }
        }
    }
}
