package com.dadabarbie.TruckTrip.activity

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.View.OnClickListener
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnRepeat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.creditList
import com.dadabarbie.TruckTrip.Utils.Constants.debitList
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.languageLocale
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.LocaleHelper
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.adapter.LanguageAdapter
import com.dadabarbie.TruckTrip.auth.activity.LoginScreenActivity
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityLanguageSelctionBinding
import com.dadabarbie.TruckTrip.languagemodel.Language
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale


@AndroidEntryPoint
class LanguageSelction : AppCompatActivity(), OnClickListener, LanguageAdapter.OnItemClick {
    lateinit var binding: ActivityLanguageSelctionBinding
    private var languageList: ArrayList<Language> = arrayListOf()
    private lateinit var languageAdapter: LanguageAdapter
    private  val  authViewModel: AuthViewModel by viewModels()
    private val MIN_CLICK_INTERVAL: Long = 1000  // 1 second
    private var lastClickTime: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelctionBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        com.dadabarbie.TruckTrip.Utils.SystemUiUtils.setupStatusBar(this, R.color.green, false)
        initViews()

        languageList.add(Language(locale_code = "hi", label = "हिन्दी", english_label = "Hindi"))
        languageList.add(Language(locale_code = "gu", label = "ગુજરાતી", english_label = "Gujarati"))
        languageList.add(Language(locale_code = "en", label = "English", english_label = "English"))
        languageList.add(Language(locale_code = "pa", label = "ਪੰਜਾਬੀ", english_label = "Punjabi"))
        languageList.add(Language(locale_code = "ta", label = "தமிழ்", english_label = "Tamil"))
        languageList.add(Language(locale_code = "mr", label = "मराठी", english_label = "Marathi"))
        languageList.add(Language(locale_code = "ml", label = "മലയാളം", english_label = "Malayalam"))
        languageList.add(Language(locale_code = "bn", label = "বাংলা", english_label = "Bangla"))
        languageList.add(Language(locale_code = "te", label = "తెలుగు", english_label = "Telugu"))
        languageList.add(Language(locale_code = "kn", label = "ಕನ್ನಡ", english_label = "Kannada"))
        languageList.add(Language(locale_code = "as", label = "অসমীয়া", english_label = "Assamese"))
        languageList.add(Language(locale_code = "mai", label = "मैथिली", english_label = "Maithili"))
        languageList.add(Language(locale_code = "or", label = "ଓଡିଆ", english_label = "Odia"))
        languageList.add(Language(locale_code = "ne", label = "नेपाली", english_label = "Nepali"))
        languageList.add(Language(locale_code = "doi", label = "डोगरी", english_label = "Dogri"))
        languageList.add(Language(locale_code = "bho", label = "भोजपुरी", english_label = "Bhojpuri"))
        languageList.add(Language(locale_code = "raj", label = "राजस्थानी", english_label = "Rajasthani"))
        languageList.add(Language(locale_code = "mni", label = "মৈত্রী", english_label = "Manipuri"))

        for(item in languageList){
            if(Prefs[Constants.languageCode, ""].toString()==item.locale_code){
                binding.submitBtn.isVisible = true
                item.flag=true
            }
        }


        setAdapter()
        setOnClickListner()
    }


    private fun initViews() {
        setObserver()
//        authViewModel.getAllSupporetdLanguags()
    }

    private fun setAdapter() {
        languageAdapter = LanguageAdapter(this, this)
        languageAdapter.submitList(languageList)
        binding.languageList.adapter = languageAdapter
        languageAdapter.notifyDataSetChanged()
    }


    private fun setOnClickListner() {
        binding.submitBtn.setOnClickListener(this)
        binding.backBtn.setOnClickListener(this)

//        binding.gujrati.setOnClickListener(this)
//        binding.englishLayout.setOnClickListener(this)
//        binding.hindiLayout.setOnClickListener(this)
//        binding.telguLayout.setOnClickListener(this)
//        binding.gujratiLayout.setOnClickListener(this)
//        binding.punjabiLayout.setOnClickListener(this)
//        binding.tamilLayout.setOnClickListener(this)
//        binding.marathiayout.setOnClickListener(this)
//        binding.banglaLayout.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.submitBtn -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime
                    if (languageLocale != "") {
                        LocaleHelper.setNewLocale(applicationContext, languageLocale)
                        if(intent.getStringExtra("languageFlag").equals("")){
                            startActivity(Intent(this, LoginScreenActivity::class.java))
                        }else{
                            authViewModel.updateLanguage(languageLocale.toString())
                        }

                        Prefs[Constants.languageCode] = languageLocale.toString()
                    }
                }
            }
            binding.backBtn->{
                onBackPressedDispatcher.onBackPressed()
            }

//            binding.english -> {
//                val locale = Locale("en")
//                Locale.setDefault(locale)
//                val config = Configuration()
//                config.locale = locale
//                resources.updateConfiguration(config, resources.displayMetrics)
//                startActivity(Intent(this, LoginScreenActivity::class.java))
//
//            }
//
//            binding.gujrati -> {
//                val locale = Locale("gu")
//                Locale.setDefault(locale)
//                val config = Configuration()
//                config.locale = locale
//                resources.updateConfiguration(config, resources.displayMetrics)
//                startActivity(Intent(this, DashBoardActivity::class.java))
//
//            }
        }
    }

    override fun clickEvent(position: Int) {
        for (item in languageList) {
            if (item.label == languageList[position].label) {
                item.flag = true
            } else {
                item.flag = false
            }
        }

        animateAndReset(binding.nextBtn)


        binding.submitBtn.isVisible = true
        languageLocale = languageList[position].locale_code
        languageAdapter.submitList(languageList)
        languageAdapter.notifyDataSetChanged()
    }

    private fun setObserver() {


        authViewModel.updateLangModel.observe(this) {
            when (it){
                is NetworkResult.Error<*> -> {

                }
                is NetworkResult.Loading<*> -> {

                }
                is NetworkResult.Success<*> -> {
                    startActivity(Intent(this, DashBoardActivity::class.java))

                }
            }

        }

    }


    private fun animateAndReset(view: View) {
        // Save the original position (X and Y coordinates) of the view
        val originalX = view.translationX
        val originalY = view.translationY

        // Create a translation animation (move view horizontally)
        val translationX = ObjectAnimator.ofFloat(view, "translationX", 0f, 100f).apply {
            duration = 3000L // Duration for translation: 2 seconds
        }

        // Create a fade-out animation
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            duration = 3000L // Duration for fade-out: 2 seconds
        }

        // Create a fade-in animation
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = 3000L // Duration for fade-in: 2 seconds
            startDelay = 500L // Delay before fade-in starts
        }

        // Create an AnimatorSet to combine the translation and fade animations
        val animatorSet = AnimatorSet().apply {
            playTogether(translationX, fadeOut) // Play translation and fade-out together
            playSequentially(fadeOut, fadeIn)  // Play fade-in after fade-out
        }

        // Listener to reset the view position after animation completes
        animatorSet.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                view.translationX = originalX
                view.translationY = originalY
                animatorSet.start()
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })

        // Start the animation set
        animatorSet.start()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }


}
