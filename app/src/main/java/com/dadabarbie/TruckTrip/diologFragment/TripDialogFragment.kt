package com.dadabarbie.TruckTrip.diologFragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.core.util.Pair
import androidx.fragment.app.DialogFragment
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.gone
import com.dadabarbie.TruckTrip.Utils.Constants.visible
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.databinding.TripDialogFragmentBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TripDialogFragment(val bothDate:String, val startingPlace:String, val endingPlace:String
                         , val driverIncomeTrip:String, val truckNumber:String, val truckAvg:String, val totalDays:String,val update:String) : DialogFragment(), View.OnClickListener,TextToSpeech.OnInitListener {
    lateinit var binding: TripDialogFragmentBinding
    lateinit var avgDialogFragment: AvgDialogFragment
    private lateinit var materialDateBuilder: MaterialDatePicker.Builder<Pair<Long, Long>>
    private lateinit var materialDatePicker: MaterialDatePicker<*>
    var startDate: String = ""
    var endDate: String = ""
    private val MIN_CLICK_INTERVAL: Long = 1000  // 1 second
    private var lastClickTime: Long = 0
    var truckTrip="0"
    private lateinit var tts: TextToSpeech
    override fun onClick(v: View?) {
        when (v) {
            binding.date -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime
                    materialDatePicker.show(childFragmentManager, "MATERIAL_DATE_PICKER")
                }
            }
            binding.speaker->{
                val textToSpeak = getString(R.string.ahi_trip_ni_saruaat_ni_ane_end_ni_date_nakho)
                speakText(textToSpeak)
            }
            binding.speakLan->{
                val textToSpeak = getString(R.string.sarvat_no_place_nakho)
                speakText(textToSpeak)
            }
            binding.speakLandest->{
                val textToSpeak = getString(R.string.end_no_place_nakho)
                speakText(textToSpeak)
            }
            binding.driverAvakSpeaker->{
                val textToSpeak = getString(R.string.driver_ni_per_trip_aavak_nakho)
                speakText(textToSpeak)
            }
            binding.truckMic->{
                val textToSpeak = getString(R.string.truck_number_add_karo)
                speakText(textToSpeak)
            }

            binding.closeBtn -> {
//                dialog?.dismiss()
                requireActivity().onBackPressed()
            }

            binding.submit -> {
                if (validation()) {
                    (context as MainActivity).getDataFill(
                        binding.truckNumber.text.toString(),
                        binding.srcPlaceValue.text.toString(),
                        binding.destPlaceValue.text.toString(),
                        startDate, endDate, binding.truckAvgValue.text.toString(),
                        binding.driverTripAvak.text.toString(),
                        truckTrip
                    )
                    (context as MainActivity).databaseAddFlag=false
                    if(update==""){
                        Constants.creditList.clear()
                        Constants.debitList.clear()
                    }
                    dialog?.dismiss()
                }
            }

            binding.avgButton -> {
                avgDialogFragment = AvgDialogFragment()
                avgDialogFragment.show(childFragmentManager, "")
            }

            binding.srcMic -> {
                getTextToSpeech()
            }

            binding.destMic -> {
                getTextToSpeechDest()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TripDialogFragmentBinding.inflate(
            inflater,
            container, false
        )
        Constants.emitAvg(Event(""))
        tts = TextToSpeech(requireContext(), this)
        setData()
        binding.truckNumber.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                binding.srcPlaceValue.error = null
                binding.destPlaceValue.error = null
                binding.truckNumber.error = null
            }
        }
        binding.destPlaceValue.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                binding.srcPlaceValue.error = null
                binding.destPlaceValue.error = null
                binding.truckNumber.error = null
            }
        }
        binding.srcPlaceValue.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                binding.srcPlaceValue.error = null
                binding.destPlaceValue.error = null
                binding.truckNumber.error = null
            }
        }


        initDatePicker()
        setObserver()
        setOnClickListner()
        return binding.root
    }

    private fun setData() {
        binding.date.text = "From:$bothDate"
        binding.srcPlaceValue.setText(startingPlace)
        binding.destPlaceValue.setText(endingPlace)
        binding.driverTripAvak.setText(driverIncomeTrip)
        binding.truckNumber.setText(truckNumber)
        binding.truckAvgValue.text = truckAvg
    }

    private fun setObserver() {
        Constants.avg.observe(this) {
            it.getContentIfNotHandled()?.let { event ->
                event.let {
                    if (it == "") {
                        binding.truckAvgLayout.gone()
                    } else {
                        binding.truckAvgLayout.visible()
                        binding.truckAvgValue.text = " : " + it

                    }
                }
            }

        }
    }

    private fun setOnClickListner() {
        binding.date.setOnClickListener(this)
        binding.submit.setOnClickListener(this)
        binding.avgButton.setOnClickListener(this)
        binding.srcMic.setOnClickListener(this)
        binding.destMic.setOnClickListener(this)
        binding.speaker.setOnClickListener(this)
        binding.speakLan.setOnClickListener(this)
        binding.speakLandest.setOnClickListener(this)
        binding.driverAvakSpeaker.setOnClickListener(this)
        binding.truckMic.setOnClickListener(this)
        binding.closeBtn.setOnClickListener(this)


        materialDatePicker.addOnPositiveButtonClickListener { selection: Any ->
            val myDates = selection as Pair<Long, Long>

            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val simpleDateFormatExport = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStart = simpleDateFormat.format(Date(myDates.first))
            val dateEnd = simpleDateFormat.format(Date(myDates.second))
            startDate = dateStart
            endDate = dateEnd
            val dateStartExport = simpleDateFormatExport.format(Date(myDates.first))
            val dateEndExport = simpleDateFormatExport.format(Date(myDates.second))
            binding.date.text = "From:$dateStart To:$dateEnd"
            binding.totalDays.text =
                getString(R.string.total_days)+": ${printDifference(Date(myDates.first), Date(myDates.second))}"
            truckTrip=printDifference(Date(myDates.first), Date(myDates.second))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val width = (resources.displayMetrics.widthPixels * 0.90)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog!!.window!!.setLayout(width.toInt(), ActionBar.LayoutParams.WRAP_CONTENT)
    }

    fun validation(): Boolean {
        if (binding.truckNumber.text.toString().isNullOrEmpty() || binding.truckNumber.text.toString()=="") {
            binding.truckNumber.requestFocus()
            binding.truckNumber.error = getString(R.string.please_add_truck_number)
            return false
        } else {
            binding.truckNumber.clearFocus()
            binding.truckNumber.error = null
        }
        if (binding.srcPlaceValue.text.toString().isNullOrEmpty() || binding.srcPlaceValue.text.toString()=="") {
            binding.srcPlaceValue.requestFocus()
            binding.srcPlaceValue.error = getString(R.string.please_add_source_place)
            return false
        } else {
            binding.srcPlaceValue.clearFocus()
            binding.srcPlaceValue.error = null
        }
        if (binding.destPlaceValue.text.toString().isNullOrEmpty() ||binding.destPlaceValue.text.toString()=="" ) {
            binding.destPlaceValue.requestFocus()
            binding.destPlaceValue.error = getString(R.string.please_add_destination_place)
            return false
        } else {
            binding.destPlaceValue.clearFocus()
            binding.destPlaceValue.error = null
        }

        if (binding.totalDays.text.toString() == "Total Days : 0") {
            Toast.makeText(
                requireActivity(),
                "Please add Trip Starting Date & Ending Date",
                Toast.LENGTH_SHORT
            ).show()
            binding.datePicker.requestFocus()
            return false
        } else {

        }
        return true
    }

    private fun initDatePicker() {

        materialDateBuilder = MaterialDatePicker.Builder.dateRangePicker().setCalendarConstraints(
            CalendarConstraints.Builder().setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
        )
        materialDatePicker = materialDateBuilder.build()
        materialDateBuilder.setTitleText("Select Date")
        val date = getCurrentDateTime()
        val dateStart = date.toString(format = "yyyy-MM-dd")
        val ateStartExport = date.toString(format = "yyyy-MM-dd")
        val dateEnd = date.toString("yyyy-MM-dd")
        val dateEndExport = date.toString(format = "yyyy-MM-dd")
        startDate = dateStart
        endDate = dateEnd
        if(bothDate==""){
            binding.date.text = "From:$dateStart To:$dateEnd"
            binding.totalDays.text = "Total Days : ${printDifference(date, date)}"
            truckTrip=printDifference(date, date)
        }
    }
    fun getOneYearFromNow(): Date {
        // Get the current date
        val calendar = Calendar.getInstance()

        // Add 1 year to the current date
        calendar.add(Calendar.YEAR, 1)

        // Return the time in milliseconds (1 year from now)
        return calendar.time
    }
    private fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }



    fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    fun printDifference(startDate: Date, endDate: Date): String {
        //milliseconds
        var different = endDate.time - startDate.time
        println("startDate : $startDate")
        println("endDate : $endDate")
        println("different : $different")
        val secondsInMilli: Long = 1000
        val minutesInMilli = secondsInMilli * 60
        val hoursInMilli = minutesInMilli * 60
        val daysInMilli = hoursInMilli * 24
        val elapsedDays = different / daysInMilli
        different = different % daysInMilli
        val elapsedHours = different / hoursInMilli
        different = different % hoursInMilli
        val elapsedMinutes = different / minutesInMilli
        different = different % minutesInMilli
        val elapsedSeconds = different / secondsInMilli
        return elapsedDays.toString()

    }

    private fun allCloseFocus() {
        binding.truckNumber.clearFocus()
        binding.srcPlaceValue.clearFocus()
        binding.destPlaceValue.clearFocus()
    }

    fun getTextToSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        Log.d("whatsanswer", "getTextToSpeech: ${ Prefs[Constants.languageCode, ""].toString()}")
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Prefs[Constants.languageCode, ""].toString()+"-IN"); // Set Gujarati language
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your language");

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, 10)
        } else {
            Toast.makeText(
                requireActivity(),
                "Your Device Don't Support Speech Input",
                Toast.LENGTH_SHORT
            )
                .show()
        }



    }

    private fun getTextToSpeechDest() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Prefs[Constants.languageCode, ""].toString()+"-IN"); // Set Gujarati language
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your language");

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, 11)
        } else {
            Toast.makeText(
                requireActivity(),
                "Your Device Don't Support Speech Input",
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 10) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                binding.srcPlaceValue.setText(result!![0].toString())
                val amount = extractAmount(result[0].toString())
            } else {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                binding.destPlaceValue.setText(result!![0].toString())
            }

        }

    }
    fun extractAmount(text: String): Int {
        val regex = Regex("(\\d+)|(hundred|thousand)")
        var amount = 0
        var multiplier = 1

        regex.findAll(text).forEach { matchResult ->
            val value = matchResult.value
            when (value) {
                "hundred" -> multiplier = 100
                "thousand" -> multiplier = 1000
                else -> amount += (value.toIntOrNull() ?: 0) * multiplier
            }
        }
        return amount
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to US English
            val result = tts.setLanguage(Locale("gu", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Handle language not supported error
            }
        } else {
            // Initialization failed
        }
    }
    private fun speakText(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }



        override fun onDestroy() {
            if (tts != null) {
                tts.stop()
                tts.shutdown()
            }
            super.onDestroy()
        }


}
