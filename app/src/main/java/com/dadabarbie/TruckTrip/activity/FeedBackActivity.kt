package com.dadabarbie.TruckTrip.activity

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.creditList
import com.dadabarbie.TruckTrip.Utils.Constants.debitList
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityFeedBackBinding
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.gson.annotations.SerializedName
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@AndroidEntryPoint
class FeedBackActivity : BaseActivity(), TextToSpeech.OnInitListener {
    private val binding: ActivityFeedBackBinding by lazy {
        ActivityFeedBackBinding.inflate(layoutInflater)
    }

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private val authViewModel: AuthViewModel by viewModels()
    // Data
    private var likedApp: Boolean? = null
    var message: String =""
    var feedBack: String =""
    private var wantFeedback: Boolean? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            // 35 (android - 15)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        }
        setupListeners()
        textToSpeech = TextToSpeech(this, this)
        setObserver()
    }

    private fun speakFeedbackMessage() {
        if (!isTtsInitialized) {
            Toast.makeText(
                this,
                "Text-to-Speech is still initializing...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Stop any ongoing speech
        textToSpeech?.stop()

        // The message to speak
        val message = getString(R.string.feedback_get_message)

        // Speak the message
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "feedback_message")



        // Visual feedback - animate the speaker button

    }
    private fun setupListeners() {
        // Question 1: Like App
        binding.btnLikeYes.setOnClickListener {
            animateClick(it)
            likedApp = true
            checkFormValidity()
            updateButtonStates(binding.btnLikeYes as MaterialButton,
                binding.btnLikeNo as MaterialButton
            )
            checkFormValidity()
        }
        binding.youtubeIcon.setOnClickListener {
            speakFeedbackMessage()
        }
        binding.btnLikeNo.setOnClickListener {
            likedApp = false
            updateButtonStates(binding.btnLikeNo as MaterialButton,
                binding.btnLikeYes as MaterialButton
            )
            checkFormValidity()
        }

        // Question 2: Want Feedback
        binding.btnFeedbackYes.setOnClickListener {
            wantFeedback = true
            updateButtonStates(binding.btnFeedbackYes as MaterialButton,
                binding.btnFeedbackNo as MaterialButton
            )
            binding.layoutFeedbackInput.visibility = View.VISIBLE
            checkFormValidity()
        }

        binding.btnFeedbackNo.setOnClickListener {
            wantFeedback = false
            updateButtonStates(binding.btnFeedbackNo as MaterialButton,
                binding.btnFeedbackYes as MaterialButton
            )
            binding.layoutFeedbackInput.visibility = View.GONE
            binding.etFeedback.setText("")
            checkFormValidity()
        }

        // Submit Button
        binding.btnSubmit.setOnClickListener {
            submitFeedback()
        }
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun animateClick(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .duration = 80
            }
    }


    private fun updateButtonStates(
        selectedBtn: MaterialButton,
        otherBtn: MaterialButton
    ) {
        // Selected button
        selectedBtn.alpha = 1.0f
        selectedBtn.isEnabled = true
        selectedBtn.elevation = 8f

        // Unselected button (fade effect)
        otherBtn.alpha = 0.5f
        otherBtn.isEnabled = true
        otherBtn.elevation = 2f
    }


    private fun checkFormValidity() {
        binding.btnSubmit.isEnabled = likedApp != null && wantFeedback != null
    }

    private fun submitFeedback() {
        if (likedApp == null || wantFeedback == null) {
            Toast.makeText(this,
                getString(R.string.please_answer_both_questions), Toast.LENGTH_SHORT).show()
            return
        }

        val feedbackText = if (wantFeedback == true) {
            binding.etFeedback.text.toString().trim()
        } else {
            null
        }

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        // Create request
        val request = FeedbackRequest(
            likedApp = likedApp!!,
            wantToGiveFeedback = wantFeedback!!,
            feedbackText = feedbackText,
            timestamp = System.currentTimeMillis()
        )
        if((likedApp == true)){
            message="Yes"
        }else{
            message="No"
        }
        if(wantFeedback==true){
            feedBack="Negitive"
        }else{
            feedBack="Positive"
        }

        authViewModel.getUserFeedback(feedBack,message)


        // Make API call
//        lifecycleScope.launch {
//            try {
////                val response = RetrofitClient.api.submitFeedback(request)
//
//                binding.progressBar.visibility = View.GONE
//
////                if (response.success) {
////                    Toast.makeText(
////                        this@FeedbackActivity,
////                        "Dhanyavaad! 🙏 Feedback submitted successfully!",
////                        Toast.LENGTH_LONG
////                    ).show()
////
////                    // Optional: Close activity or reset form
////                    finish()
////                } else {
////                    Toast.makeText(
////                        this@FeedbackActivity,
////                        "Error: ${response.message}",
////                        Toast.LENGTH_SHORT
////                    ).show()
////                    binding.btnSubmit.isEnabled = true
////                }
//            } catch (e: Exception) {
//                binding.progressBar.visibility = View.GONE
//                binding.btnSubmit.isEnabled = true
//
//                Toast.makeText(
//                    this,
//                    "Failed to submit: ${e.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//
//                e.printStackTrace()
//            }
//        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setObserver() {
        authViewModel.getDeleteTripData.observe(this){
            when(it){
                is NetworkResult.Error -> {
                    dismissProgress()
                    Toast.makeText(applicationContext,"${it.message}", Toast.LENGTH_SHORT).show()

                }
                is NetworkResult.Loading -> {

                }
                is NetworkResult.Success -> {
                    dismissProgress()
                    Toast.makeText(applicationContext,
                        getString(R.string.thanks_for_this_feedback), Toast.LENGTH_SHORT).show()

                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            var langCode =
                Prefs[Constants.languageCode] ?: "hi"
            // Set language to Hindi
            val result = textToSpeech?.setLanguage(Locale(langCode, "IN"))

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(
                    this,
                    "Hindi language not supported on this device",
                    Toast.LENGTH_SHORT
                ).show()
                // Fallback to English
                textToSpeech?.setLanguage(Locale.US)
            }

            isTtsInitialized = true

            // Set speech rate (0.5 to 2.0, 1.0 is normal)
            textToSpeech?.setSpeechRate(0.85f)

            // Set pitch (0.5 to 2.0, 1.0 is normal)
            textToSpeech?.setPitch(1.0f)
        } else {
            Toast.makeText(
                this,
                "Text-to-Speech initialization failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
data class FeedbackRequest(
    @SerializedName("liked_app")
    val likedApp: Boolean,

    @SerializedName("want_to_give_feedback")
    val wantToGiveFeedback: Boolean,

    @SerializedName("feedback_text")
    val feedbackText: String?,

    @SerializedName("timestamp")
    val timestamp: Long
)

data class FeedbackResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String
)