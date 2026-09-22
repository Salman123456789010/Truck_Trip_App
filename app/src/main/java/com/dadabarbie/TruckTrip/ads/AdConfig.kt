package com.dadabarbie.TruckTrip.ads

object AdConfig {
    // Set to false as requested by user to use proper production IDs directly
    const val USE_TEST_ADS: Boolean = false

    // Production Ad Unit IDs provided by User
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-8808039515208362/4892027690"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-8808039515208362/4161399957"
    const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-8808039515208362/3557131853"
    const val PROD_NATIVE_LIST_AD_UNIT_ID = "ca-app-pub-8808039515208362/1007625609"
    const val PROD_NATIVE_ADVANCED_AD_UNIT_ID = "ca-app-pub-8808039515208362/5976758477"
    const val PROD_NATIVE_NEWS_AD_UNIT_ID = "ca-app-pub-8808039515208362/9048738516"

    // Official Google Test Ad Unit IDs (Fallback)
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

    val BANNER_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    val REWARDED_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID

    val NATIVE_LIST_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_LIST_AD_UNIT_ID

    val NATIVE_ADVANCED_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_ADVANCED_AD_UNIT_ID

    val NATIVE_NEWS_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_NEWS_AD_UNIT_ID

    val APP_OPEN_AD_UNIT_ID: String
        get() = TEST_APP_OPEN_AD_UNIT_ID

    // Interstitial frequency: 1 ad after every 3 successful trip saves
    const val INTERSTITIAL_FREQUENCY = 3

    // Ad Lifecycle & Show-Rate Optimization Constants
    // AdMob considers ads older than 1 hour as expired/stale
    const val AD_EXPIRATION_MS: Long = 3600000L // 1 hour

    // Minimum cooldown between full-screen interstitial displays to prevent user disruption
    const val MIN_INTERSTITIAL_INTERVAL_MS: Long = 45000L // 45 seconds

    // Minimum background time before showing AppOpenAd on app return
    const val MIN_APP_OPEN_BACKGROUND_MS: Long = 300000L // 5 minutes

    // Google Play Billing Product ID
    const val PRODUCT_ID_REMOVE_ADS = "remove_ads"
}
