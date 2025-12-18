package com.dadabarbie.TruckTrip.auth.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.gone
import com.dadabarbie.TruckTrip.Utils.Constants.loginUser
import com.dadabarbie.TruckTrip.Utils.Constants.resendToken
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.Constants.showSnackBar
import com.dadabarbie.TruckTrip.Utils.Constants.visible
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.DashBoardActivity
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityOtpverificationScreenBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.messaging.FirebaseMessaging
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import kotlin.math.log

@AndroidEntryPoint
class OTPVerificationScreen : AppCompatActivity(), View.OnClickListener {
    private val binding: ActivityOtpverificationScreenBinding by lazy {
        ActivityOtpverificationScreenBinding.inflate(layoutInflater)
    }

    private val authViewModel: AuthViewModel by viewModels()

    private var number = ""
    private var otp = ""
    var fcmToken=""
    var flag:Boolean=false
    var token:String=""
    private var mAuth: FirebaseAuth? = null
     var verificationId1: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        mAuth = FirebaseAuth.getInstance()
        setOnClickListner()
        if (savedInstanceState == null) {
            authViewModel.otpVerificationTimer()
            authViewModel.otpVerificationTimer?.start()
        }
        getFcmToken()
        otpCountDown()
        number = intent.getStringExtra("number").toString()
        flag=intent.getBooleanExtra("flag",false)
        if(flag){
            verificationId1 = intent.getStringExtra("otp").toString()
        }else{
             token= intent.getStringExtra("token").toString()
             authViewModel.requestOTP(number,fcmToken,token,Prefs[Constants.languageCode, ""].toString())
        }
//        spannableChanges()
//        clickableChanges()
        setObserver()

    }

    @SuppressLint("StringFormatInvalid")
    private fun otpCountDown() {
        authViewModel.timerTick.observe(this) { millisUntilFinished ->
            millisUntilFinished?.let {
                if ((((millisUntilFinished) / 1000)) != 0L) {
                    binding.resendOtp.text =  getString(R.string.resend_otp_in_s)+" ${(millisUntilFinished) / 1000}s"

                } else {
                    if (authViewModel.attemptForResendOtp == 0) {
                        binding.resendOtp.text =getString(R.string.resend_otp)
                    } else {
                         binding.resendOtp.gone()
                         binding.resend.visible()
                        binding.resendOtp.isClickable = true
                        binding.btLogin.isEnabled = true
                        binding.resendOtp.text =getString(R.string.resend_otp)
                    }

                }
                try {
                    if (binding.resendOtp.currentTextColor != R.color.date_hint_color) {
                        binding.resendOtp.setTextColor(
                            ColorStateList.valueOf(
                                ContextCompat.getColor(this, R.color.black)
                            )
                        )
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
    }



    private fun setObserver() {
        authViewModel.res.observe(this) {
            when (it) {
                is NetworkResult.Error -> {
//                    showSnackBar(binding.root, it.message.toString())
                    Toast.makeText(applicationContext, it.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                    dismissProgress()
                }

                is NetworkResult.Loading -> {
                }

                is NetworkResult.Success -> {
                    if (it.data?.message == "User login successfully") {
                        subscribeToTopic(loginUser)
                        Prefs[Constants.isLogin] = true
                        Prefs[Constants.authToken] = it.data.data.token
                        Prefs[Constants.mobileNumber] =number.toString()
                        val i = Intent(applicationContext, DashBoardActivity::class.java)
                        startActivity(i)
                        finish()
                    }
                }

                else -> {}
            }
        }
    }
     private fun subscribeToTopic(topic:String){
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener { task->
            if(task.isSuccessful){
                Log.d("TAG123", "subscribeToTopic: ")
            }else{

            }

        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
         finishAffinity()
    }


//
//    private fun spannableChanges() {
//        var s = "Enter the code from the sms we sent to $number";
//        val spannable = SpannableString(s);
//        spannable.setSpan(
//            ForegroundColorSpan(Color.parseColor("#FF000000")),
//            39,
//            52,
//            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//        )
//        binding.otpVerificationMsg.text = spannable
//    }

    private fun setOnClickListner() {
        binding.btLogin.setOnClickListener(this)
        binding.resend.setOnClickListener(this)
    }

    private fun verifyCode(code: String) {
        val credential = verificationId1?.let { PhoneAuthProvider.getCredential(it, code) }
        if (credential != null) {
            signInWithCredential(credential)
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        mAuth!!.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    mAuth!!.currentUser?.getIdToken(true)?.addOnCompleteListener {
                        if(it.isSuccessful){
                            it.result?.token?.let { it1 ->
                                authViewModel.requestOTP(number,
                                  fcmToken,it1,Prefs[Constants.languageCode, ""].toString()
                                )
                            }

                        }
                    }


                } else {
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btLogin -> {
                showProgress()
                verifyCode(binding.etOtpVerify.text.toString())
            }
            binding.resend->{
                showProgress()
                if (authViewModel.attemptForResendOtp >= 0) {
                    resendToken?.let { resendVerificationCode(number) }
                } else {
                    binding.resendOtp.isClickable = false
                }

            }
        }
    }

    private fun sendVerificationCode(number: String) {
        val options = resendToken?.let {
            PhoneAuthOptions.newBuilder(mAuth!!)
                .setPhoneNumber(number) // Phone number to verify
                .setTimeout(20L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this) // Activity (for callback binding)
                .setCallbacks(mCallBack)
                .setForceResendingToken(it) // OnVerificationStateChangedCallbacks
                .build()
        }
        if (options != null) {
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    private val mCallBack: PhoneAuthProvider.OnVerificationStateChangedCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(
                s: String,
                forceResendingToken: PhoneAuthProvider.ForceResendingToken
            ) {
                super.onCodeSent(s, forceResendingToken)
                verificationId1 = s
                binding.resendOtp.visible()
                binding.resend.gone()
                Constants.dismissProgress()

            }

            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                Constants.dismissProgress()
                Toast.makeText(applicationContext, "Coming", Toast.LENGTH_SHORT).show()

            }

            override fun onVerificationFailed(e: FirebaseException) {
                Constants.dismissProgress()
                Toast.makeText(this@OTPVerificationScreen, e.message, Toast.LENGTH_LONG).show()
            }
        }
    private fun getFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@OnCompleteListener
                }
                fcmToken=task.result
            })
    }

    private fun resendVerificationCode(phoneNumber: String) {
        val options = resendToken?.let {
            mAuth?.let { it1 ->
                PhoneAuthOptions.newBuilder(it1)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            Constants.dismissProgress()
                              Toast.makeText(applicationContext,"${e.message}",Toast.LENGTH_SHORT).show()
                        }

                        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                            verificationId1 = verificationId
                            binding.resendOtp.visible()
                            binding.resend.gone()
                            authViewModel.otpVerificationTimer?.start()
                            Constants.dismissProgress()
                        }
                    })
                    .setForceResendingToken(it)  // Use the stored resendToken here
                    .build()
            }
        }

        if (options != null) {
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }


}
