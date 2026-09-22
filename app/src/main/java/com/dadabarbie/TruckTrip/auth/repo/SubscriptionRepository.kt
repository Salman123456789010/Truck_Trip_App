package com.dadabarbie.TruckTrip.auth.repo



import android.util.Log

import com.dadabarbie.TruckTrip.api.ApiService

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
data class VerifyPurchaseRequest(
    val userId: String,
    val packageName: String,
    val productId: String,
    val purchaseToken: String,
    val purchaseTime: Long
)

data class VerifyPurchaseResponse(
    val success: Boolean,
    val isActive: Boolean,
    val expiryDate: String?,
    val message: String
)

data class SubscriptionStatusResponse(
    val isActive: Boolean,
    val expiryDate: String?,
    val productId: String?,
    val autoRenewing: Boolean
)

class SubscriptionRepository(
    private val apiService: ApiService
) {

    companion object {
        private const val TAG = "SubscriptionRepo"
        // ⚠️ Replace with your actual package name
        private const val PACKAGE_NAME = "com.yourapp"
    }

    // ─── Verify Purchase with Your Backend ───────────────────────────────────

//    suspend fun verifyPurchase(
//        userToken: String,
//        userId: String,
//        purchase: Purchase
//    ): Result<VerifyPurchaseResponse> {
//        return try {
////            val request = VerifyPurchaseRequest(
////                userId = userId,
////                packageName = PACKAGE_NAME,
//////                productId = purchase.products.firstOrNull() ?: "",
//////                purchaseToken = purchase.purchaseToken,
//////                purchaseTime = purchase.purchaseTime
////            )
//
//            val response = apiService.verifyPurchase(
//                token = "Bearer $userToken",
//                request = request
//            )
//
//            if (response.isSuccessful && response.body() != null) {
//                Result.Success(response.body()!!)
//            } else {
//                Result.Error("Server error: ${response.code()} - ${response.message()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "verifyPurchase failed", e)
//            Result.Error("Network error: ${e.message}")
//        }
//    }

    // ─── Get Subscription Status from Backend ────────────────────────────────

    suspend fun getSubscriptionStatus(userToken: String): Result<SubscriptionStatusResponse> {
        return try {
            val response = apiService.getSubscriptionStatus("Bearer $userToken")
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Failed to get status: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getSubscriptionStatus failed", e)
            Result.Error("Network error: ${e.message}")
        }
    }
}