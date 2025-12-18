package com.dadabarbie.TruckTrip.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.adapter.OnboardingAdapter
import com.dadabarbie.TruckTrip.adapter.OnboardingVoiceListener
import com.dadabarbie.TruckTrip.auth.activity.LoginScreenActivity
import com.dadabarbie.TruckTrip.databinding.ActivityHowToUseBinding
import com.dadabarbie.TruckTrip.model.OnboardingItem
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Locale

class HowToUseActivity : AppCompatActivity(), OnboardingVoiceListener {

    lateinit var binding: ActivityHowToUseBinding
    private lateinit var adapter: OnboardingAdapter

    private var tts: TextToSpeech? = null
    private var sentences: List<String> = emptyList()
    private var currentSentenceIndex = 0
    private var isSpeaking = false
    private var isPaused = false

    // Handler for posting back to main thread from UtteranceProgressListener
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHowToUseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)

        initTts()
        initAdapter()
        setOnClickListeners()
    }

    // ---------------------------
    // TEXT TO SPEECH SETUP
    // ---------------------------
    private fun initTts() {
        val langCode = Prefs[Constants.languageCode] ?: "hi"   // default Hindi

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {

                val locale = getLocaleFromCode(langCode)

                val result = tts?.setLanguage(locale)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {

                    // fallback -> Hindi
                    tts?.language = Locale("hi", "IN")
                }

                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.85f)

                // IMPORTANT: set UtteranceProgressListener once TTS initialized
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        // nothing to do here
                    }

                    override fun onDone(utteranceId: String?) {
                        // called on a background thread; post to main
                        mainHandler.post {
                            if (!isSpeaking || isPaused) return@post

                            // advance to next sentence if any
                            currentSentenceIndex++
                            if (currentSentenceIndex < sentences.size) {
                                speakSentence(currentSentenceIndex)
                            } else {
                                // finished all sentences
                                isSpeaking = false
                                isPaused = false
                                currentSentenceIndex = 0
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            // stop speaking safely on error
                            stopSpeaking()
                        }
                    }

                    // API 23+ has onError(utteranceId, errorCode) but above is fine as fallback
                })
            }
        }
    }

    private fun getLocaleFromCode(code: String): Locale {
        return when (code) {
            "hi" -> Locale("hi", "IN")  // Hindi
            "mr" -> Locale("mr", "IN")  // Marathi
            "gu" -> Locale("gu", "IN")  // Gujarati
            "pa" -> Locale("pa", "IN")  // Punjabi (use pa)
            "bn" -> Locale("bn", "IN")  // Bengali
            "or" -> Locale("or", "IN")  // Odia
            "ta" -> Locale("ta", "IN")  // Tamil
            "te" -> Locale("te", "IN")  // Telugu
            "kn" -> Locale("kn", "IN")  // Kannada
            "ml" -> Locale("ml", "IN")  // Malayalam
            "as" -> Locale("as", "IN")  // Assamese
            "ne" -> Locale("ne", "NP")  // Nepali
            "ur" -> Locale("ur", "IN")  // Urdu
            "en" -> Locale("en", "IN")  // English (India)
            else -> Locale("hi", "IN")  // default Hindi
        }
    }

    private fun speakSentence(index: Int) {
        if (index >= sentences.size) return
        val sentence = sentences[index]

        // prepare utterance params
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utt_$index")
        }

        // Ensure we call speak on main thread
        mainHandler.post {
            tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, params, "utt_$index")
        }
    }

    // ---------------------------
    // ADAPTER SETUP
    // ---------------------------
    private fun initAdapter() {

        adapter = OnboardingAdapter(this)  // IMPORTANT: listener added
        binding.viewPager.adapter = adapter

        val items = listOf(
            OnboardingItem(R.drawable.trip_list, getString(R.string.trip_list_title), getString(R.string.trip_help_text)),
            OnboardingItem(R.drawable.add_trip_dialog, getString(R.string.start_new_trip), getString(R.string.add_trip_dec)),
            OnboardingItem(R.drawable.trip_transcation, getString(R.string.manage_trip_title), getString(R.string.trip_transaction_dec)),
            OnboardingItem(R.drawable.add_expenditure, getString(R.string.add_expanse_tittle), getString(R.string.add_expense_dec)),
            OnboardingItem(R.drawable.add_income, getString(R.string.add_income_tittle), getString(R.string.add_income_dec)),
            OnboardingItem(R.drawable.add_fuel, getString(R.string.add_fuel_tittle), getString(R.string.add_fuel_dec)),
            OnboardingItem(R.drawable.complelte_trip, getString(R.string.complete_trip_tittle), getString(R.string.complete_trip_dec))
        )

        adapter.submitList(items)

        TabLayoutMediator(binding.indicator, binding.viewPager) { tab, _ ->
            tab.setIcon(R.drawable.tab_dot_selector)
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateBottomButtons(position)

                // STOP speaking automatically when user slides to another page
                if (isSpeaking) {
                    stopSpeaking()
                }
            }
        })
    }

    // ---------------------------
    // BUTTONS (NEXT / BACK)
    // ---------------------------
    private fun setOnClickListeners() {

        binding.backBtn.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnSkip.setOnClickListener {
            val pos = binding.viewPager.currentItem
            if (pos == 0) {
                if (Prefs[Constants.isLogin]) {
                    finish()
                    onBackPressedDispatcher.onBackPressed()
                }else{
                    startActivity(Intent(this, LoginScreenActivity::class.java))
                    finish()
                }

            } else {
                binding.viewPager.currentItem = pos - 1
            }
        }

        binding.btnNext.setOnClickListener {
            val nextIndex = binding.viewPager.currentItem + 1
            if (nextIndex < adapter.itemCount) {
                binding.viewPager.currentItem = nextIndex
            } else {

                if (Prefs[Constants.isLogin]) {
                    finish()
                    onBackPressedDispatcher.onBackPressed()
                }else{
                    startActivity(Intent(this, LoginScreenActivity::class.java))
                    finish()
                }

            }
        }
    }

    private fun updateBottomButtons(position: Int) {
        if (position == 0)
            binding.btnSkip.text = getString(R.string.onboard_skip)
        else
            binding.btnSkip.text = getString(R.string.onboard_back)
    }

    // ---------------------------
    // SPEAK METHODS (INTERFACE)
    // ---------------------------
    override fun onRequestSpeak(text: String) {

        if (text.isBlank()) return

        // If paused (user pressed stop previously), resume from current index
        if (isSpeaking && isPaused) {
            isPaused = false
            // resume speaking current sentence (if index valid)
            if (currentSentenceIndex < sentences.size) {
                speakSentence(currentSentenceIndex)
            }
            return
        }

        // If already speaking a different text, stop it first
        if (isSpeaking) stopSpeaking()

        // split into sentences (keeps punctuation)
        sentences = text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (sentences.isEmpty()) sentences = listOf(text)

        currentSentenceIndex = 0
        isSpeaking = true
        isPaused = false

        speakSentence(0)
    }

    override fun onRequestStop() {
        if (!isSpeaking) return
        // pause (user tapped stop) — allow resume via speak button
        tts?.stop()
        isPaused = true
    }

    private fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
        isPaused = false
        currentSentenceIndex = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
}
