package com.dadabarbie.TruckTrip.ads

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralized AdManager providing strict load -> ready -> show -> dismiss -> reload
 * lifecycle management, deduplication, caching, expiration checks, and structured logging.
 */
object AdMobManager {

    private const val TAG = "AdMobManager"

    // Ad State Machine
    enum class AdState {
        IDLE,
        LOADING,
        READY,
        SHOWING
    }

    // Generic Ad Holder tracking state, instance, and load timestamp
    data class AdHolder<T>(
        var ad: T? = null,
        var state: AdState = AdState.IDLE,
        var loadedTimeMs: Long = 0L
    )

    // Native Ad placement identifiers
    enum class NativePlacement {
        TOOLS_ADVANCED,
        PROFILE_LIST,
        NEWS_LIST,
        NEWS_DETAILS
    }

    private val isInitializing = AtomicBoolean(false)
    private var isInitialized = false
    private var tripSaveCount = 0

    // Full-screen Ad Holders
    private val interstitialHolder = AdHolder<InterstitialAd>()
    private val rewardedHolder = AdHolder<RewardedAd>()
    private val appOpenHolder = AdHolder<AppOpenAd>()

    // Native Ad Cache per placement
    private val nativeAdHolders = mutableMapOf<NativePlacement, AdHolder<NativeAd>>()

    // Shared Home Banner Cache to eliminate duplicate requests across bottom nav tab switches
    private var cachedHomeBannerAdView: AdView? = null
    private var isBannerLoading = false

    // Timing trackers to prevent ad spam
    private var lastInterstitialShowTimeMs: Long = 0L
    private var isAnyFullScreenAdShowing = false

    // =========================================================================
    // STRUCTURED LOGGING: REQUEST, LOADED, FAILED, READY, SHOW, IMPRESSION, DISMISSED
    // =========================================================================

    private fun logEvent(event: String, adType: String, adUnitId: String = "", details: String = "") {
        val unitStr = if (adUnitId.isNotEmpty()) " | UnitId: $adUnitId" else ""
        val detailStr = if (details.isNotEmpty()) " | $details" else ""
        Log.i(TAG, "[$event] Type: $adType$unitStr$detailStr")
    }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    fun init(context: Context) {
        if (isInitialized || isInitializing.getAndSet(true)) return
        logEvent("REQUEST", "SDK_INIT", "", "Initializing MobileAds SDK...")

        MobileAds.initialize(context.applicationContext) { status ->
            isInitialized = true
            isInitializing.set(false)
            logEvent("LOADED", "SDK_INIT", "", "MobileAds SDK Initialized successfully")

            // Prime interstitial cache once on startup so it's ready for the first natural completion point
            preloadInterstitial(context.applicationContext)
        }
    }

    private fun isRemoveAdsPurchased(context: Context): Boolean {
        return com.dadabarbie.TruckTrip.billing.SubscriptionManager.isPremium()
    }

    private fun isHolderExpired(holder: AdHolder<*>): Boolean {
        if (holder.state != AdState.READY || holder.loadedTimeMs <= 0L) return false
        val age = System.currentTimeMillis() - holder.loadedTimeMs
        return age > AdConfig.AD_EXPIRATION_MS
    }

    private fun isHolderReady(holder: AdHolder<*>): Boolean {
        if (holder.state == AdState.READY && holder.ad != null) {
            if (isHolderExpired(holder)) {
                logEvent("DISMISSED", "CACHE", "", "Cached ad expired after 1 hour TTL. Evicting.")
                holder.ad = null
                holder.state = AdState.IDLE
                holder.loadedTimeMs = 0L
                return false
            }
            return true
        }
        return false
    }

    // =========================================================================
    // 1. BANNER AD (With Caching & Tab-Recreation Deduplication)
    // =========================================================================

    /**
     * Attaches or loads a banner in the given container without firing duplicate requests
     * when switching tabs in BottomNavigationView.
     */
    fun attachHomeBanner(activity: Activity, container: ViewGroup): AdView? {
        if (activity.isFinishing || activity.isDestroyed) return null

        if (isRemoveAdsPurchased(activity)) {
            logEvent("DISMISSED", "BANNER", AdConfig.BANNER_AD_UNIT_ID, "Suppressed - Remove Ads active")
            container.visibility = View.GONE
            container.removeAllViews()
            return null
        }

        // Check if an existing loaded Banner AdView is already available
        cachedHomeBannerAdView?.let { existingAdView ->
            try {
                // Detach from previous parent if still attached (e.g. destroyed fragment view)
                (existingAdView.parent as? ViewGroup)?.removeView(existingAdView)
                container.removeAllViews()
                container.visibility = View.VISIBLE
                container.addView(existingAdView)
                existingAdView.resume()
                logEvent("READY", "BANNER", AdConfig.BANNER_AD_UNIT_ID, "Re-attached existing Banner to container without new network request")
                return existingAdView
            } catch (e: Exception) {
                logEvent("FAILED", "BANNER", AdConfig.BANNER_AD_UNIT_ID, "Error reusing cached banner: ${e.message}")
                cachedHomeBannerAdView = null
            }
        }

        if (isBannerLoading) {
            logEvent("REQUEST", "BANNER", AdConfig.BANNER_AD_UNIT_ID, "Banner is already loading. Skipping duplicate request.")
            return null
        }

        // Create new banner request
        logEvent("REQUEST", "BANNER", AdConfig.BANNER_AD_UNIT_ID, "Loading new Banner Ad")
        isBannerLoading = true
        container.removeAllViews()
        container.visibility = View.VISIBLE

        val adView = AdView(activity)
        adView.adUnitId = AdConfig.BANNER_AD_UNIT_ID

        val adSize = getAdaptiveAdSize(activity, container)
        adView.setAdSize(adSize)

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                isBannerLoading = false
                cachedHomeBannerAdView = adView
                logEvent("LOADED", "BANNER", AdConfig.BANNER_AD_UNIT_ID, "Banner loaded successfully")
                logEvent("READY", "BANNER", AdConfig.BANNER_AD_UNIT_ID, "Banner is ready and displayed")
                container.visibility = View.VISIBLE
            }

            override fun onAdImpression() {
                logEvent("IMPRESSION", "BANNER", AdConfig.BANNER_AD_UNIT_ID, "Banner impression registered")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                isBannerLoading = false
                cachedHomeBannerAdView = null
                logEvent("FAILED", "BANNER", AdConfig.BANNER_AD_UNIT_ID, "Code: ${error.code} | ${error.message}")
            }
        }

        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
        return adView
    }

    /**
     * Backward-compatible banner loading method.
     */
    fun loadBanner(activity: Activity, container: ViewGroup): AdView? {
        return attachHomeBanner(activity, container)
    }

    fun pauseBanner() {
        cachedHomeBannerAdView?.pause()
    }

    fun resumeBanner() {
        cachedHomeBannerAdView?.resume()
    }

    fun destroyBanner() {
        try {
            (cachedHomeBannerAdView?.parent as? ViewGroup)?.removeView(cachedHomeBannerAdView)
            cachedHomeBannerAdView?.destroy()
        } catch (e: Exception) {}
        cachedHomeBannerAdView = null
        isBannerLoading = false
    }

    private fun getAdaptiveAdSize(activity: Activity, container: ViewGroup): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        var adWidthPixels = container.width.toFloat()
        if (adWidthPixels <= 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    // =========================================================================
    // 2. NATIVE ADS (With Placement Caching & View Re-attachment)
    // =========================================================================

    /**
     * Loads or binds a cached Native Ad into a TemplateView based on the placement slot.
     */
    fun loadNativeAd(activity: Activity, templateView: TemplateView, placement: NativePlacement) {
        if (activity.isFinishing || activity.isDestroyed) return

        if (isRemoveAdsPurchased(activity)) {
            templateView.visibility = View.GONE
            return
        }

        val adUnitId = when (placement) {
            NativePlacement.TOOLS_ADVANCED -> AdConfig.NATIVE_ADVANCED_AD_UNIT_ID
            NativePlacement.PROFILE_LIST -> AdConfig.NATIVE_LIST_AD_UNIT_ID
            NativePlacement.NEWS_LIST, NativePlacement.NEWS_DETAILS -> AdConfig.NATIVE_NEWS_AD_UNIT_ID
        }

        val holder = nativeAdHolders.getOrPut(placement) { AdHolder() }

        // Re-use cached Native Ad if ready and not expired
        if (isHolderReady(holder)) {
            val cachedNativeAd = holder.ad
            if (cachedNativeAd != null) {
                logEvent("READY", "NATIVE", adUnitId, "Binding cached NativeAd for placement: $placement")
                templateView.visibility = View.VISIBLE
                templateView.setNativeAd(cachedNativeAd)
                return
            }
        }

        // Prevent duplicate concurrent requests for the same placement
        if (holder.state == AdState.LOADING) {
            logEvent("REQUEST", "NATIVE", adUnitId, "Placement $placement is already loading. Skipping duplicate request.")
            return
        }

        logEvent("REQUEST", "NATIVE", adUnitId, "Requesting NativeAd for placement: $placement")
        holder.state = AdState.LOADING

        val adLoader = AdLoader.Builder(activity.applicationContext, adUnitId)
            .forNativeAd { nativeAd ->
                holder.ad = nativeAd
                holder.state = AdState.READY
                holder.loadedTimeMs = System.currentTimeMillis()

                logEvent("LOADED", "NATIVE", adUnitId, "NativeAd loaded for placement: $placement")
                logEvent("READY", "NATIVE", adUnitId, "NativeAd ready for placement: $placement")

                if (!activity.isFinishing && !activity.isDestroyed) {
                    templateView.visibility = View.VISIBLE
                    templateView.setNativeAd(nativeAd)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdImpression() {
                    logEvent("IMPRESSION", "NATIVE", adUnitId, "NativeAd impression registered for placement: $placement")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    holder.state = AdState.IDLE
                    holder.ad = null
                    logEvent("FAILED", "NATIVE", adUnitId, "Placement $placement failed: ${error.code} | ${error.message}")
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        templateView.visibility = View.GONE
                    }
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    // =========================================================================
    // 3. INTERSTITIAL AD (Natural Task-Completion Points & State Machine)
    // =========================================================================

    /**
     * Preloads an interstitial ad into the centralized cache.
     * Guaranteed NO duplicate requests if already loading or ready.
     */
    fun preloadInterstitial(context: Context) {
        if (isRemoveAdsPurchased(context)) return

        if (isHolderReady(interstitialHolder)) {
            val ageSec = (System.currentTimeMillis() - interstitialHolder.loadedTimeMs) / 1000
            logEvent("READY", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Ad already ready in cache (Age: ${ageSec}s). Skipping duplicate request.")
            return
        }

        if (interstitialHolder.state == AdState.LOADING) {
            logEvent("REQUEST", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Ad is already loading. Skipping duplicate request.")
            return
        }

        logEvent("REQUEST", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Preloading interstitial ad")
        interstitialHolder.state = AdState.LOADING

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            AdConfig.INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialHolder.ad = ad
                    interstitialHolder.state = AdState.READY
                    interstitialHolder.loadedTimeMs = System.currentTimeMillis()
                    logEvent("LOADED", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Interstitial preloaded successfully")
                    logEvent("READY", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Interstitial primed for next natural completion point")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialHolder.ad = null
                    interstitialHolder.state = AdState.IDLE
                    interstitialHolder.loadedTimeMs = 0L
                    logEvent("FAILED", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Preload failed: Code: ${error.code} | ${error.message}")
                }
            }
        )
    }

    /**
     * Natural task completion point: After a trip is saved.
     */
    fun onTripSavedSuccessfully(activity: Activity, onAdDismissed: (() -> Unit)? = null) {
        tripSaveCount++
        logEvent("SHOW", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Trip save event #$tripSaveCount (Frequency: ${AdConfig.INTERSTITIAL_FREQUENCY})")

        if (tripSaveCount % AdConfig.INTERSTITIAL_FREQUENCY == 0) {
            showInterstitialIfReady(activity, force = true, onAdDismissed = onAdDismissed)
        } else {
            // Prime for next completion point if needed
            preloadInterstitial(activity)
            onAdDismissed?.invoke()
        }
    }

    /**
     * Shows the interstitial ad if ready, respecting cooldown and activity lifecycle.
     */
    fun showInterstitialIfReady(
        activity: Activity,
        force: Boolean = false,
        onAdDismissed: (() -> Unit)? = null
    ) {
        if (isRemoveAdsPurchased(activity)) {
            onAdDismissed?.invoke()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            logEvent("FAILED", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Show skipped: Activity is finishing or destroyed")
            onAdDismissed?.invoke()
            return
        }

        // Anti-spam cooldown check (minimum 45s between full-screen ads) unless forced by milestone
        val now = System.currentTimeMillis()
        if (!force && (now - lastInterstitialShowTimeMs) < AdConfig.MIN_INTERSTITIAL_INTERVAL_MS) {
            logEvent("DISMISSED", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Show skipped: Cooldown active (${(now - lastInterstitialShowTimeMs) / 1000}s / ${AdConfig.MIN_INTERSTITIAL_INTERVAL_MS / 1000}s)")
            onAdDismissed?.invoke()
            return
        }

        if (isAnyFullScreenAdShowing) {
            logEvent("DISMISSED", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Show skipped: Another full screen ad is already active")
            onAdDismissed?.invoke()
            return
        }

        if (!isHolderReady(interstitialHolder)) {
            logEvent("FAILED", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Interstitial not ready when natural point reached. Priming for next time.")
            preloadInterstitial(activity)
            onAdDismissed?.invoke()
            return
        }

        val ad = interstitialHolder.ad ?: run {
            interstitialHolder.state = AdState.IDLE
            preloadInterstitial(activity)
            onAdDismissed?.invoke()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                interstitialHolder.state = AdState.SHOWING
                isAnyFullScreenAdShowing = true
                lastInterstitialShowTimeMs = System.currentTimeMillis()
                logEvent("SHOW", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Interstitial displayed on ${activity.localClassName}")
            }

            override fun onAdImpression() {
                logEvent("IMPRESSION", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Interstitial impression registered")
            }

            override fun onAdDismissedFullScreenContent() {
                logEvent("DISMISSED", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Interstitial dismissed by user")
                interstitialHolder.ad = null
                interstitialHolder.state = AdState.IDLE
                isAnyFullScreenAdShowing = false

                // Prime next ad for upcoming natural task completion
                preloadInterstitial(activity)
                onAdDismissed?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                logEvent("FAILED", "INTERSTITIAL", AdConfig.INTERSTITIAL_AD_UNIT_ID, "Show failed: Code: ${adError.code} | ${adError.message}")
                interstitialHolder.ad = null
                interstitialHolder.state = AdState.IDLE
                isAnyFullScreenAdShowing = false

                preloadInterstitial(activity)
                onAdDismissed?.invoke()
            }
        }

        ad.show(activity)
    }

    // =========================================================================
    // 4. REWARDED AD (User Intent-Driven with Proper Callbacks)
    // =========================================================================

    /**
     * Preloads a rewarded ad. Only call when user approaches a reward-gated feature.
     */
    fun preloadRewardedAd(context: Context) {
        if (isRemoveAdsPurchased(context)) return

        if (isHolderReady(rewardedHolder)) {
            logEvent("READY", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Rewarded ad already ready in cache. Skipping duplicate request.")
            return
        }

        if (rewardedHolder.state == AdState.LOADING) {
            logEvent("REQUEST", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Rewarded ad already loading. Skipping duplicate request.")
            return
        }

        logEvent("REQUEST", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Preloading rewarded ad on user intent")
        rewardedHolder.state = AdState.LOADING

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            AdConfig.REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedHolder.ad = ad
                    rewardedHolder.state = AdState.READY
                    rewardedHolder.loadedTimeMs = System.currentTimeMillis()
                    logEvent("LOADED", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Rewarded ad loaded successfully")
                    logEvent("READY", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Rewarded ad is ready to show")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedHolder.ad = null
                    rewardedHolder.state = AdState.IDLE
                    rewardedHolder.loadedTimeMs = 0L
                    logEvent("FAILED", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Rewarded ad preload failed: Code: ${error.code} | ${error.message}")
                }
            }
        )
    }

    /**
     * Shows rewarded ad strictly with user consent.
     * No fallback to interstitial upon failure or cancellation.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdFailedOrClosed: ((String) -> Unit)? = null
    ) {
        if (isRemoveAdsPurchased(activity)) {
            logEvent("READY", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Granted directly (Remove Ads active)")
            onRewardEarned()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            logEvent("FAILED", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Activity is finishing or destroyed")
            onAdFailedOrClosed?.invoke("Activity unavailable")
            return
        }

        if (isAnyFullScreenAdShowing) {
            onAdFailedOrClosed?.invoke("Another ad is already playing")
            return
        }

        if (!isHolderReady(rewardedHolder)) {
            logEvent("FAILED", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Rewarded ad not ready yet. Preloading for retry.")
            preloadRewardedAd(activity)
            onAdFailedOrClosed?.invoke("Ad is loading. Please try again in a few seconds.")
            return
        }

        val ad = rewardedHolder.ad ?: run {
            rewardedHolder.state = AdState.IDLE
            preloadRewardedAd(activity)
            onAdFailedOrClosed?.invoke("Ad not available. Please try again later.")
            return
        }

        var userEarnedReward = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                rewardedHolder.state = AdState.SHOWING
                isAnyFullScreenAdShowing = true
                logEvent("SHOW", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Rewarded ad displayed")
            }

            override fun onAdImpression() {
                logEvent("IMPRESSION", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Rewarded ad impression registered")
            }

            override fun onAdDismissedFullScreenContent() {
                logEvent("DISMISSED", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Rewarded ad dismissed by user")
                rewardedHolder.ad = null
                rewardedHolder.state = AdState.IDLE
                isAnyFullScreenAdShowing = false

                // Trigger reward only AFTER full screen ad has closed cleanly and user is back on screen
                if (userEarnedReward) {
                    onRewardEarned()
                } else {
                    onAdFailedOrClosed?.invoke("Ad closed before earning reward")
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                logEvent("FAILED", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "Code: ${adError.code} | ${adError.message}")
                rewardedHolder.ad = null
                rewardedHolder.state = AdState.IDLE
                isAnyFullScreenAdShowing = false
                onAdFailedOrClosed?.invoke("Failed to display ad")
            }
        }

        ad.show(activity) { rewardItem ->
            userEarnedReward = true
            logEvent("READY", "REWARDED", AdConfig.REWARDED_AD_UNIT_ID, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
    }

    // =========================================================================
    // 5. APP OPEN AD (Non-intrusive Cold-Start / Background Resume)
    // =========================================================================

    fun preloadAppOpenAd(context: Context) {
        if (isRemoveAdsPurchased(context)) return

        if (isHolderReady(appOpenHolder)) {
            logEvent("READY", "APP_OPEN", AdConfig.APP_OPEN_AD_UNIT_ID, "AppOpenAd already ready in cache. Skipping duplicate request.")
            return
        }

        if (appOpenHolder.state == AdState.LOADING) {
            logEvent("REQUEST", "APP_OPEN", AdConfig.APP_OPEN_AD_UNIT_ID, "AppOpenAd already loading. Skipping duplicate request.")
            return
        }

        logEvent("REQUEST", "APP_OPEN", AdConfig.APP_OPEN_AD_UNIT_ID, "Preloading AppOpenAd")
        appOpenHolder.state = AdState.LOADING

        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            context.applicationContext,
            AdConfig.APP_OPEN_AD_UNIT_ID,
            adRequest,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenHolder.ad = ad
                    appOpenHolder.state = AdState.READY
                    appOpenHolder.loadedTimeMs = System.currentTimeMillis()
                    logEvent("LOADED", "APP_OPEN", AdConfig.APP_OPEN_AD_UNIT_ID, "AppOpenAd loaded successfully")
                    logEvent("READY", "APP_OPEN", AdConfig.APP_OPEN_AD_UNIT_ID, "AppOpenAd is ready")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenHolder.ad = null
                    appOpenHolder.state = AdState.IDLE
                    appOpenHolder.loadedTimeMs = 0L
                    logEvent("FAILED", "APP_OPEN", AdConfig.APP_OPEN_AD_UNIT_ID, "AppOpenAd failed: Code: ${error.code} | ${error.message}")
                }
            }
        )
    }

    fun showAppOpenAdIfReady(activity: Activity, onDismissed: (() -> Unit)? = null) {
        if (isRemoveAdsPurchased(activity) || activity.isFinishing || activity.isDestroyed || isAnyFullScreenAdShowing) {
            onDismissed?.invoke()
            return
        }

        if (!isHolderReady(appOpenHolder)) {
            preloadAppOpenAd(activity)
            onDismissed?.invoke()
            return
        }

        val ad = appOpenHolder.ad ?: run {
            appOpenHolder.state = AdState.IDLE
            preloadAppOpenAd(activity)
            onDismissed?.invoke()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                appOpenHolder.state = AdState.SHOWING
                isAnyFullScreenAdShowing = true
                logEvent("SHOW", "APP_OPEN", AdConfig.APP_OPEN_AD_UNIT_ID, "AppOpenAd displayed")
            }

            override fun onAdImpression() {
                logEvent("IMPRESSION", "APP_OPEN", AdConfig.APP_OPEN_AD_UNIT_ID, "AppOpenAd impression registered")
            }

            override fun onAdDismissedFullScreenContent() {
                logEvent("DISMISSED", "APP_OPEN", AdConfig.APP_OPEN_AD_UNIT_ID, "AppOpenAd dismissed")
                appOpenHolder.ad = null
                appOpenHolder.state = AdState.IDLE
                isAnyFullScreenAdShowing = false
                preloadAppOpenAd(activity)
                onDismissed?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                logEvent("FAILED", "APP_OPEN", AdConfig.APP_OPEN_AD_UNIT_ID, "Code: ${adError.code} | ${adError.message}")
                appOpenHolder.ad = null
                appOpenHolder.state = AdState.IDLE
                isAnyFullScreenAdShowing = false
                preloadAppOpenAd(activity)
                onDismissed?.invoke()
            }
        }

        ad.show(activity)
    }
}
