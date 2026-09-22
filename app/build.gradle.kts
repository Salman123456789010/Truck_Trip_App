import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("kapt")
    id ("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
}
val localProperties = readProperties(file("../local.properties"))
android {
    namespace = "com.dadabarbie.TruckTrip"
    compileSdk = 36

        //2.0.3 25 old version applied

    defaultConfig {
        applicationId = "com.dadabarbie.TruckTrip"
        minSdk = 24
        targetSdk = 36
        versionCode = 40
        versionName = "2.0.14"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = false
            buildConfigField("String", "GST_API_KEY", localProperties["GST_API_KEY"] as String)
            buildConfigField("String", "X_API_KEY", localProperties["X_API_KEY"] as String)
            buildConfigField("String", "AgentName", localProperties["AgentName"] as String)
        }
        release {
            isMinifyEnabled = false
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
            buildConfigField("String", "GST_API_KEY", localProperties["GST_API_KEY"] as String)
            buildConfigField("String", "X_API_KEY", localProperties["X_API_KEY"] as String)
            buildConfigField("String", "AgentName", localProperties["AgentName"] as String)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }
    android {
        bundle {
            language {
                enableSplit = false
            }
        }
    }

}


dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation ("com.google.android.gms:play-services-ads:21.5.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation ("com.github.fondesa:kpermissions-coroutines:3.5.0")
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("io.github.chaosleung:pinview:1.4.4")
    implementation("com.hbb20:android-country-picker:0.0.7")
    implementation ("com.google.android.libraries.places:places:3.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.intuit.sdp:sdp-android:1.0.6")
    implementation ("androidx.activity:activity-ktx:1.7.0")
    implementation("androidx.activity:activity:1.9.3")
    testImplementation("junit:junit:4.13.2")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    implementation ("com.google.firebase:firebase-crashlytics:18.2.6")
    implementation ("com.google.firebase:firebase-analytics:20.0.2")
    implementation ("com.google.firebase:firebase-config:21.0.1")
    //ExoPlayer
    implementation ("com.google.android.exoplayer:exoplayer:2.18.0")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")



    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-android-compiler:2.57.2")

    //swipLayout
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.0.0")

    implementation ("androidx.viewpager2:viewpager2:1.0.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // Activity KTX for viewModels()
    implementation("androidx.activity:activity-ktx:1.9.0")

    // KProgressHUD
    implementation("io.github.rupinderjeet:kprogresshud:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")


    // Room database (using KSP to avoid Kapt metadata parser incompatibilities)
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")

    implementation (project(":nativetemplates"))

    // Google Play Billing Library (v9.1.0)
    implementation("com.android.billingclient:billing-ktx:9.1.0")
}

fun readProperties(propertiesFile: File) = Properties().apply {
    propertiesFile.inputStream().use { fis ->
        load(fis)
    }
}
kapt {
    correctErrorTypes = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}