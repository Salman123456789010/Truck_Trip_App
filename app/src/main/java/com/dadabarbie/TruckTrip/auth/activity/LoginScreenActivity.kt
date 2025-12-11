package com.dadabarbie.TruckTrip.auth.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants.countryCode
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.resendToken
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
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
import java.util.concurrent.TimeUnit


class LoginScreenActivity : AppCompatActivity(), View.OnClickListener {
    private val binding: ActivityLoginScreenBinding by lazy {
        ActivityLoginScreenBinding.inflate(layoutInflater)
    }
    private var verificationId: String? = null
    private var mAuth: FirebaseAuth? = null
    var phone = ""

    private var selectedPhoneCode = "91"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupCountryPickerView()
        setOnClickListner()
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
                        phone = countryCode + binding.etPhone.text.toString()
                        showProgress()
                        sendVerificationCode(phone)
                    }
                }
            }

        }
    }

    private fun sendVerificationCode(number: String) {
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
                resendToken=forceResendingToken

                val i = Intent(applicationContext, OTPVerificationScreen::class.java).putExtra("number", phone).putExtra("otp", s)
                    .putExtra("flag",true)
                startActivity(i)
                finish()
                resendToken=forceResendingToken
                dismissProgress()

            }

            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                mAuth!!.signInWithCredential(phoneAuthCredential).addOnCompleteListener {
                    if(it.isSuccessful){
                        mAuth!!.currentUser?.getIdToken(true)?.addOnCompleteListener {
                            if(it.isSuccessful){
                                it.result?.token?.let { it1 ->
                                    val i = Intent(applicationContext, OTPVerificationScreen::class.java).putExtra("number", phone).putExtra("token", it1).putExtra("flag",false)
                                    startActivity(i)
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
