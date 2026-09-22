package com.dadabarbie.TruckTrip.billing

import android.util.Base64
import android.util.Log
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SubscriptionManager {

    private const val TAG = "SubscriptionManager"

    enum class PremiumFeature {
        DIESEL_PRICES,
        FUEL_STOP_PLANNER,
        BACKHAUL_CALCULATOR,
        DETAILED_REPORTS,
        ADVANCED_ANALYTICS,
        UNLIMITED_TRIPS
    }

    private val _isPremiumFlow = MutableStateFlow(isLocallyPremium())
    val isPremiumFlow: StateFlow<Boolean> = _isPremiumFlow.asStateFlow()

    // Temporary unlock set for features unlocked via rewarded ads during current app session
    private val sessionUnlockedFeatures = mutableSetOf<PremiumFeature>()

    private fun isLocallyPremium(): Boolean {
        return try {
            val cached: Boolean = Prefs[Constants.PREF_IS_PREMIUM, false]
            if (!cached) return false

            val expiryStr: String = Prefs[Constants.PREF_PREMIUM_EXPIRY, ""]
            if (expiryStr.isNotBlank()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val expiryDate = sdf.parse(expiryStr)
                if (expiryDate != null && expiryDate.before(Date())) {
                    // Subscription expired
                    Prefs[Constants.PREF_IS_PREMIUM] = false
                    return false
                }
            }
            cached
        } catch (e: Exception) {
            false
        }
    }

    fun isPremium(): Boolean {
        val current = isLocallyPremium()
        if (_isPremiumFlow.value != current) {
            _isPremiumFlow.value = current
        }
        return current
    }

    fun setPremium(isPremium: Boolean, expiryDate: String? = null, autoRenew: Boolean = true) {
        Prefs[Constants.PREF_IS_PREMIUM] = isPremium
        if (!expiryDate.isNullOrBlank()) {
            Prefs[Constants.PREF_PREMIUM_EXPIRY] = expiryDate
        }
        Prefs[Constants.PREF_PREMIUM_AUTO_RENEW] = autoRenew
        _isPremiumFlow.value = isPremium
        Log.d(TAG, "Subscription status updated: isPremium=$isPremium, expiryDate=$expiryDate")
    }

    /**
     * Extracts the logged-in User ID from JWT Token or preferences.
     */
    fun getUserId(): String {
        return try {
            val token: String = Prefs[Constants.authToken, ""]
            if (token.isNotEmpty() && token.contains(".")) {
                val parts = token.split(".")
                if (parts.size >= 2) {
                    val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
                    val json = JSONObject(payloadJson)
                    val id = json.optString("_id", json.optString("id", json.optString("userId", "")))
                    if (id.isNotBlank()) return id
                }
            }
            // Fallback to mobile number if token parsing fails
            val phone: String = Prefs[Constants.mobileNumber, ""]
            if (phone.isNotBlank()) phone else "guest_user"
        } catch (e: Exception) {
            "guest_user"
        }
    }

    /**
     * Generates a privacy-safe SHA-256 hash for Google Play Billing's setObfuscatedAccountId.
     * Google recommends obfuscating user identifiers rather than passing raw PII.
     */
    fun getObfuscatedAccountId(): String {
        val rawId = getUserId()
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(rawId.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }.take(64)
        } catch (e: Exception) {
            rawId.hashCode().toString()
        }
    }

    fun getBonusTrips(): Int {
        return try {
            Prefs[Constants.PREF_BONUS_TRIPS, 0]
        } catch (e: Exception) {
            0
        }
    }

    fun addBonusTrips(count: Int = Constants.BONUS_TRIPS_PER_AD) {
        val current = getBonusTrips()
        val newTotal = current + count
        Prefs[Constants.PREF_BONUS_TRIPS] = newTotal
        Log.d(TAG, "Bonus trips added: +$count (Total bonus trips: $newTotal)")
    }

    fun getMaxAllowedFreeTrips(): Int {
        return Constants.FREE_TRIP_LIMIT + getBonusTrips()
    }

    fun canSaveTrip(currentSavedTripsCount: Int): Boolean {
        if (isPremium()) return true
        return currentSavedTripsCount < getMaxAllowedFreeTrips()
    }

    fun getRemainingTrips(currentSavedTripsCount: Int): Int {
        if (isPremium()) return Int.MAX_VALUE
        val remaining = getMaxAllowedFreeTrips() - currentSavedTripsCount
        return if (remaining > 0) remaining else 0
    }

    fun isFeatureLocked(feature: PremiumFeature): Boolean {
        if (isPremium()) return false
        if (sessionUnlockedFeatures.contains(feature)) return false
        return true
    }

    fun grantTemporaryFeatureAccess(feature: PremiumFeature) {
        sessionUnlockedFeatures.add(feature)
        Log.d(TAG, "Temporary session access granted for: $feature")
    }
}
