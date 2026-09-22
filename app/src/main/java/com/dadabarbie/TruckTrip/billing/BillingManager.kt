package com.dadabarbie.TruckTrip.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.dadabarbie.TruckTrip.Utils.Constants
import java.util.concurrent.atomic.AtomicBoolean

class BillingManager private constructor(context: Context) : PurchasesUpdatedListener {

    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var billingClient: BillingClient? = null
    private val isConnecting = AtomicBoolean(false)

    private var premiumProductDetails: ProductDetails? = null
    private var selectedOfferToken: String? = null

    private val _formattedPrice = MutableLiveData<String>("₹49/month")
    val formattedPrice: LiveData<String> get() = _formattedPrice

    private val _billingState = MutableLiveData<BillingState>(BillingState.Idle)
    val billingState: LiveData<BillingState> get() = _billingState

    // Callback for when a purchase is made and needs backend verification
    var onPurchaseCompleted: ((purchaseToken: String, productId: String, orderId: String?, purchaseTime: Long) -> Unit)? = null
    var onPurchaseCancelled: (() -> Unit)? = null
    var onPurchaseError: ((errorMsg: String) -> Unit)? = null

    sealed class BillingState {
        object Idle : BillingState()
        object Connecting : BillingState()
        object Ready : BillingState()
        data class Error(val message: String, val code: Int) : BillingState()
        object Purchasing : BillingState()
    }

    companion object {
        private const val TAG = "BillingManager"

        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context).also { instance = it }
            }
        }
    }

    init {
        initBillingClient()
    }

    private fun initBillingClient() {
        billingClient = BillingClient.newBuilder(applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()
    }

    fun startConnection(onReady: ((Boolean) -> Unit)? = null) {
        val client = billingClient ?: run {
            initBillingClient()
            billingClient!!
        }

        if (client.isReady) {
            queryProductDetails()
            queryPurchases()
            onReady?.invoke(true)
            return
        }

        if (isConnecting.getAndSet(true)) {
            Log.d(TAG, "BillingClient connection already in progress")
            return
        }

        _billingState.postValue(BillingState.Connecting)

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting.set(false)
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing setup succeeded (Code: ${billingResult.responseCode})")
                    _billingState.postValue(BillingState.Ready)
                    queryProductDetails()
                    queryPurchases()
                    onReady?.invoke(true)
                } else {
                    val msg = "Billing setup failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
                    Log.e(TAG, msg)
                    _billingState.postValue(BillingState.Error(billingResult.debugMessage, billingResult.responseCode))
                    onReady?.invoke(false)
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting.set(false)
                Log.w(TAG, "Google Play Billing service disconnected. Will reconnect on next request.")
                _billingState.postValue(BillingState.Idle)
            }
        })
    }

    /**
     * Queries Google Play for the subscription product details and updates the dynamic price.
     */
    fun queryProductDetails(onCompleted: ((ProductDetails?) -> Unit)? = null) {
        val client = billingClient
        if (client == null || !client.isReady) {
            startConnection { success ->
                if (success) queryProductDetails(onCompleted)
                else onCompleted?.invoke(null)
            }
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Constants.PRODUCT_ID_PREMIUM)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsList = queryProductDetailsResult.productDetailsList
                val premiumDetails = detailsList.firstOrNull { it.productId == Constants.PRODUCT_ID_PREMIUM }
                if (premiumDetails != null) {
                    premiumProductDetails = premiumDetails
                    extractPriceAndOfferToken(premiumDetails)
                    Log.d(TAG, "Loaded product details for: ${premiumDetails.productId}")
                    mainHandler.post { onCompleted?.invoke(premiumDetails) }
                } else {
                    Log.w(TAG, "Product ${Constants.PRODUCT_ID_PREMIUM} not found in Google Play Console product list")
                    mainHandler.post { onCompleted?.invoke(null) }
                }
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
                mainHandler.post { onCompleted?.invoke(null) }
            }
        }
    }

    private fun extractPriceAndOfferToken(productDetails: ProductDetails) {
        val offers = productDetails.subscriptionOfferDetails
        if (offers.isNullOrEmpty()) {
            _formattedPrice.postValue("₹49/month")
            return
        }

        // Match base plan "monthly" or take the first available offer
        val selectedOffer = offers.firstOrNull { it.basePlanId == Constants.BASE_PLAN_MONTHLY }
            ?: offers.first()

        selectedOfferToken = selectedOffer.offerToken

        val pricingPhases = selectedOffer.pricingPhases.pricingPhaseList
        if (pricingPhases.isNotEmpty()) {
            val formatted = pricingPhases.first().formattedPrice
            _formattedPrice.postValue("$formatted / month")
        } else {
            _formattedPrice.postValue("₹49/month")
        }
    }

    /**
     * Launches the Google Play subscription purchase flow.
     */
    fun launchPurchaseFlow(activity: Activity): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false

        val client = billingClient
        if (client == null || !client.isReady) {
            startConnection { success ->
                if (success) {
                    launchPurchaseFlow(activity)
                } else {
                    onPurchaseError?.invoke("Google Play Billing is currently unavailable. Please try again later.")
                }
            }
            return false
        }

        val details = premiumProductDetails
        if (details == null) {
            queryProductDetails { loadedDetails ->
                if (loadedDetails != null) {
                    launchPurchaseFlow(activity)
                } else {
                    onPurchaseError?.invoke("Subscription product not found. Please check your internet connection or try again later.")
                }
            }
            return false
        }

        val offerToken = selectedOfferToken ?: details.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            onPurchaseError?.invoke("Subscription offer is unavailable.")
            return false
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )

        // Associate the purchase with the logged-in user using privacy-safe obfuscated account ID
        val obfuscatedAccountId = SubscriptionManager.getObfuscatedAccountId()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .setObfuscatedAccountId(obfuscatedAccountId)
            .build()

        _billingState.postValue(BillingState.Purchasing)
        val responseCode = client.launchBillingFlow(activity, billingFlowParams).responseCode

        return if (responseCode == BillingClient.BillingResponseCode.OK) {
            true
        } else {
            Log.e(TAG, "Failed to launch billing flow: Response code $responseCode")
            _billingState.postValue(BillingState.Ready)
            false
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
                _billingState.postValue(BillingState.Ready)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled the purchase flow")
                _billingState.postValue(BillingState.Ready)
                onPurchaseCancelled?.invoke()
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned. Querying existing purchases.")
                _billingState.postValue(BillingState.Ready)
                queryPurchases()
            }
            else -> {
                val errorMsg = "Purchase failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})"
                Log.e(TAG, errorMsg)
                _billingState.postValue(BillingState.Error(billingResult.debugMessage, billingResult.responseCode))
                onPurchaseError?.invoke(billingResult.debugMessage.ifBlank { "Purchase could not be completed." })
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val productId = purchase.products.firstOrNull() ?: Constants.PRODUCT_ID_PREMIUM
            val token = purchase.purchaseToken
            val orderId = purchase.orderId
            val purchaseTime = purchase.purchaseTime

            Log.d(TAG, "Purchase succeeded locally. Sending to backend for verification: token=$token, product=$productId")

            // Forward to listener for backend verification
            mainHandler.post {
                onPurchaseCompleted?.invoke(token, productId, orderId, purchaseTime)
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase is pending completion (e.g. offline payment / UPI pending)")
            mainHandler.post {
                onPurchaseError?.invoke("Purchase is pending. Your subscription will activate once payment is confirmed.")
            }
        }
    }

    /**
     * Queries active subscription purchases from Google Play to restore/verify entitlements.
     */
    fun queryPurchases(onPurchasesFound: ((List<Purchase>) -> Unit)? = null) {
        val client = billingClient
        if (client == null || !client.isReady) {
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchases = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                Log.d(TAG, "Found ${activePurchases.size} active purchases on Google Play")

                for (purchase in activePurchases) {
                    val productId = purchase.products.firstOrNull() ?: Constants.PRODUCT_ID_PREMIUM
                    mainHandler.post {
                        onPurchaseCompleted?.invoke(
                            purchase.purchaseToken,
                            productId,
                            purchase.orderId,
                            purchase.purchaseTime
                        )
                    }
                }
                mainHandler.post { onPurchasesFound?.invoke(activePurchases) }
            } else {
                Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }

    fun endConnection() {
        try {
            billingClient?.endConnection()
            billingClient = null
            _billingState.postValue(BillingState.Idle)
        } catch (e: Exception) {
            Log.e(TAG, "Error ending billing connection: ${e.message}")
        }
    }
}