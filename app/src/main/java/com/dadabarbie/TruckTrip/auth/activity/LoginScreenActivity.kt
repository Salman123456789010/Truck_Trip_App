package com.dadabarbie.TruckTrip.auth.activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants.countryCode
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.resendToken
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.UpdateDialog
import com.dadabarbie.TruckTrip.activity.BaseActivity
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityLoginScreenBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.hbb20.countrypicker.models.CPCountry
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import kotlin.getValue


@AndroidEntryPoint
class LoginScreenActivity : BaseActivity(), View.OnClickListener {
    private val binding: ActivityLoginScreenBinding by lazy {
        ActivityLoginScreenBinding.inflate(layoutInflater)
    }
    private var verificationId: String? = null
    private var mAuth: FirebaseAuth? = null
    var phone = ""
    private val authViewModel:AuthViewModel by viewModels()
    private var selectedPhoneCode = "91"
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // 35 (android - 15)
            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
        appVersionNameCheck()
        setupCountryPickerView()
        setOnClickListner()
        setupKeyboardHandling()
        setObserver()
    }
    private fun setupKeyboardHandling() {
        // Method 1: Window soft input mode
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )

        // Method 2: Handle insets for Android 11+
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply padding when keyboard is visible
            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                imeInsets.bottom
            )

            // Scroll to focused view when keyboard appears
            if (imeInsets.bottom > 0) {
                binding.root.post {
                    binding.root.smoothScrollTo(0, binding.llMobileNo.bottom + 200)
                }
            }

            insets
        }

        // Method 3: Global layout listener for older Android versions
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var wasKeyboardVisible = false

            override fun onGlobalLayout() {
                val heightDiff = binding.root.rootView.height - binding.root.height
                val isKeyboardVisible = heightDiff > 200 // Threshold for keyboard

                if (isKeyboardVisible && !wasKeyboardVisible) {
                    // Keyboard just opened
                    binding.root.post {
                        binding.root.smoothScrollTo(0, binding.llMobileNo.bottom + 200)
                    }
                }

                wasKeyboardVisible = isKeyboardVisible
            }
        })

        // Method 4: Focus listener on EditText
        binding.etPhone.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.root.postDelayed({
                    binding.root.smoothScrollTo(0, binding.llMobileNo.bottom + 200)
                }, 300)
            }
        }
    }
    private fun appVersionNameCheck() {
        authViewModel.getVersionName()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setObserver() {
        authViewModel.appVersionName.observe(this){
            when(it){

                is NetworkResult.Error<*> -> {

                }
                is NetworkResult.Loading<*> -> {

                }
                is NetworkResult.Success<*> -> {
                    if(getAppVersionName(applicationContext)!="Unknown"){
                        val serverVersion = it.data?.data?.version
                        val appVersion = getAppVersionName(applicationContext)
                        Log.d("issueHappend", "setObserver: ${appVersion} ${serverVersion} ")
                        Log.d("issueHappend", "setObserver: ${isAppVersionValid(appVersion, serverVersion?:"")}")

                        if (!serverVersion.isNullOrEmpty() &&
                            isAppVersionValid(appVersion, serverVersion)
                        ) {

                        } else {
                            UpdateDialog(this).show()
                        }
                    } else {

                    }
                }
            }
        }

    }

    private fun isAppVersionValid(appVersion: String, serverVersion: String): Boolean {
        val appParts = appVersion.split(".")
        val serverParts = serverVersion.split(".")

        val maxLength = maxOf(appParts.size, serverParts.size)

        for (i in 0 until maxLength) {
            val appPart = appParts.getOrNull(i)?.toIntOrNull() ?: 0
            val serverPart = serverParts.getOrNull(i)?.toIntOrNull() ?: 0

            if (appPart > serverPart) return true      // app > server ✅
            if (appPart < serverPart) return false     // app < server ❌
        }
        return true // equal version ✅
    }
    fun getAppVersionName(context: Context): String {
        return try {
            val packageManager = context.packageManager
            val packageName = context.packageName

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33 and above
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                // Below API 33
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }

            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }

    override fun onResume() {
        super.onResume()
        mAuth = FirebaseAuth.getInstance()
    }

    private fun setOnClickListner() {
        binding.btLogin.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btLogin -> {
                when {
                    binding.etPhone.text?.trim().isNullOrEmpty() -> {
                        binding.etPhone.error =
                            getString(R.string.please_enter_valid_mobile_number)
                    }

                    binding.etPhone.text?.trim()?.length!! < 10 -> {
                        binding.etPhone.error =
                            getString(R.string.please_enter_valid_mobile_number)
                    }

                    !(binding.etPhone.text?.trim().isNullOrEmpty()) -> {
                        val cleanPhone = binding.etPhone.text.toString().trim()
                        val codePrefix = if (selectedPhoneCode.startsWith("+")) selectedPhoneCode else "+$selectedPhoneCode"
                        phone = "$codePrefix$cleanPhone"
                        showProgress()
                        sendVerificationCode(phone)
                    }
                }
            }

        }
    }

    private fun sendVerificationCode(number: String) {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance()
        }
        val options = PhoneAuthOptions.newBuilder(mAuth!!)
            .setPhoneNumber(number)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity for callback binding
            .setCallbacks(mCallBack) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val mCallBack: OnVerificationStateChangedCallbacks =
        object : OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(s: String, forceResendingToken: ForceResendingToken) {
                super.onCodeSent(s, forceResendingToken)
                verificationId = s
                resendToken = forceResendingToken

                val i = Intent(applicationContext, OTPVerificationScreen::class.java)
                    .putExtra("number", phone)
                    .putExtra("otp", s)
                    .putExtra("flag", true)
                startActivity(i)
                finish()
                dismissProgress()
            }

            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                if (mAuth == null) {
                    mAuth = FirebaseAuth.getInstance()
                }
                mAuth!!.signInWithCredential(phoneAuthCredential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        mAuth!!.currentUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                tokenTask.result?.token?.let { idToken ->
                                    val i = Intent(applicationContext, OTPVerificationScreen::class.java)
                                        .putExtra("number", phone)
                                        .putExtra("token", idToken)
                                        .putExtra("flag", false)
                                    startActivity(i)
                                    finish()
                                }
                            }
                        }
                    }
                }
                dismissProgress()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                dismissProgress()
                Toast.makeText(this@LoginScreenActivity, e.message, Toast.LENGTH_LONG).show()
            }

        }

    private fun setupCountryPickerView() {
        binding.tvCountryCode.cpViewHelper.cpViewConfig.viewTextGenerator =
            { cpCountry: CPCountry ->
                "+${cpCountry.phoneCode}"
            }
        binding.tvCountryCode.cpViewHelper.cpRowConfig.highlightedTextGenerator = { cpCountry ->
            "+${cpCountry.phoneCode}"
        }
        binding.tvCountryCode.cpViewHelper.selectedCountry.observe(
            this
        ) { selectedCountry: CPCountry? ->
            selectedPhoneCode = "${selectedCountry?.phoneCode}"
        }
        binding.tvCountryCode.cpViewHelper.refreshView()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }


}
