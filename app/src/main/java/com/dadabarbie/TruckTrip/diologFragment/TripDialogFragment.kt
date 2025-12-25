package com.dadabarbie.TruckTrip.diologFragment

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.adapter.RouteAdapter
import com.dadabarbie.TruckTrip.databinding.TripDialogFragmentBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.play.integrity.internal.ac
import java.text.SimpleDateFormat
import java.util.*

class TripDialogFragment(
    val date: String,
    val startingPlace: String,
    val endingPlace: String,
    val driverIncome: String,
    val truckNumber: String,
    val truckAvg: String,
    val totalDays: String,
    val update: String,
    val startOdometer: String = "",
    val existingRoute: ArrayList<String> = arrayListOf()
) : DialogFragment() {

    private var _binding: TripDialogFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var routeAdapter: RouteAdapter
    private val routeList = ArrayList<String>()

    private var selectedDate: String = ""
    private var textToSpeech: TextToSpeech? = null

    private val DATE_PICKER_REQUEST = 100
    private val SRC_SPEECH_REQUEST = 101
    private val DEST_SPEECH_REQUEST = 102
    private val MIDDLE_SPEECH_REQUEST = 103
    private val TRUCK_SPEECH_REQUEST = 104

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TripDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTextToSpeech()
        setupRouteRecyclerView()
        loadExistingData()
        setupClickListeners()
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("hi", "IN")
            }
        }
    }

    private fun setupRouteRecyclerView() {
        routeAdapter = RouteAdapter(routeList) { position ->
            // Delete middle place
            if (position > 0 && position < routeList.size - 1) {
                routeList.removeAt(position)
                routeAdapter.notifyItemRemoved(position)
                routeAdapter.notifyItemRangeChanged(position, routeList.size)
            }
        }

        binding.rvRoute.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = routeAdapter
        }
    }

    private fun loadExistingData() {
        // Load date
        if (date.isNotEmpty()) {
            binding.date.text = date
            selectedDate = date
        }

        // Load route - priority order: existingRoute > individual places
        if (existingRoute.isNotEmpty()) {
            routeList.clear()
            routeList.addAll(existingRoute)

            // Set source and destination from route
            if (routeList.size >= 2) {
                binding.srcPlaceValue.setText(routeList[0])
                binding.destPlaceValue.setText(routeList[routeList.size - 1])
            }
        } else {
            // Load from individual parameters (backward compatibility)
            if (startingPlace.isNotEmpty()) {
                routeList.add(startingPlace)
                binding.srcPlaceValue.setText(startingPlace)
            }
            if (endingPlace.isNotEmpty()) {
                if (routeList.isEmpty()) {
                    routeList.add("") // Add empty source if missing
                }
                routeList.add(endingPlace)
                binding.destPlaceValue.setText(endingPlace)
            }
        }

        // Update RecyclerView
        routeAdapter.notifyDataSetChanged()
        binding.rvRoute.visibility = if (routeList.size > 2) View.VISIBLE else View.GONE

        // Load truck number
        if (truckNumber.isNotEmpty()) {
            binding.truckNumber.setText(truckNumber)
        }

        // Load start odometer
        if (startOdometer.isNotEmpty()) {
            binding.etStartOdometer.setText(startOdometer)
        }

        // Set dialog title
        if (update == "yes") {
            binding.addTripDataLabel.text = getString(R.string.update_trip_data)
            binding.submit.text = getString(R.string.update)
            binding.closeBtn.visibility=View.GONE
        }else{
            binding.closeBtn.visibility=View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        // Close button
        binding.closeBtn.setOnClickListener {
            dismiss()
            activity?.onBackPressedDispatcher!!.onBackPressed()
        }

        // Date picker
        binding.datePicker.setOnClickListener {
            showDatePicker()
        }

        // Date speaker
        binding.speaker.setOnClickListener {
            speakText(binding.date.text.toString())
        }

        // Source place text changed
        binding.srcPlaceValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()
                if (routeList.isEmpty()) {
                    routeList.add(text)
                } else {
                    routeList[0] = text
                }
                routeAdapter.notifyItemChanged(0)
            }
        })

        // Destination place text changed
        binding.destPlaceValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()

                // Ensure we have at least source place
                if (routeList.isEmpty()) {
                    routeList.add("") // Add empty source
                }

                // Update or add destination
                if (routeList.size == 1) {
                    routeList.add(text)
                } else {
                    routeList[routeList.size - 1] = text
                }
                routeAdapter.notifyItemChanged(routeList.size - 1)
            }
        })

        // Source place speaker
        binding.speakLan.setOnClickListener {
            speakText(binding.srcPlaceValue.text.toString())
        }

        // Source place mic
        binding.srcMic.setOnClickListener {
            startSpeechToText(SRC_SPEECH_REQUEST)
        }

        // Destination place speaker
        binding.speakLandest.setOnClickListener {
            speakText(binding.destPlaceValue.text.toString())
        }

        // Destination place mic
        binding.destMic.setOnClickListener {
            startSpeechToText(DEST_SPEECH_REQUEST)
        }

        // Add middle place button
        binding.btnAddMiddlePlace.setOnClickListener {
            val middlePlace = binding.etMiddlePlace.text.toString().trim()

            if (middlePlace.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.please_enter_middle_place), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ensure we have at least source and destination
            if (routeList.size < 2) {
                Toast.makeText(requireContext(), getString(R.string.please_add_source_destination_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Insert middle place before destination
            val insertPosition = routeList.size - 1
            routeList.add(insertPosition, middlePlace)
            routeAdapter.notifyItemInserted(insertPosition)

            // Clear input
            binding.etMiddlePlace.setText("")

            // Show RecyclerView if it was hidden
            binding.rvRoute.visibility = View.VISIBLE

            Toast.makeText(requireContext(), getString(R.string.middle_place_added), Toast.LENGTH_SHORT).show()
        }

        // Middle place mic
        binding.middlePlaceMic.setOnClickListener {
            startSpeechToText(MIDDLE_SPEECH_REQUEST)
        }

        // Middle place speaker
        binding.middlePlaceSpeaker.setOnClickListener {
            speakText(binding.etMiddlePlace.text.toString())
        }

        // Truck number speaker
        binding.truckSpeaker.setOnClickListener {
            speakText(binding.truckNumber.text.toString())
        }

        // Submit button
        binding.submit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun showDatePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_trip_date))
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = sdf.format(Date(selection))
            binding.date.text = selectedDate
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun startSpeechToText(requestCode: Int) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_now))
        }

        try {
            startActivityForResult(intent, requestCode)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(R.string.speech_not_supported), Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakText(text: String) {
        if (text.isNotEmpty()) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: ""

            when (requestCode) {
                SRC_SPEECH_REQUEST -> {
                    binding.srcPlaceValue.setText(spokenText)
                }
                DEST_SPEECH_REQUEST -> {
                    binding.destPlaceValue.setText(spokenText)
                }
                MIDDLE_SPEECH_REQUEST -> {
                    binding.etMiddlePlace.setText(spokenText)
                }
                TRUCK_SPEECH_REQUEST -> {
                    binding.truckNumber.setText(spokenText)
                }
            }
        }
    }

    private fun validateAndSubmit() {
        val srcPlace = binding.srcPlaceValue.text.toString().trim()
        val destPlace = binding.destPlaceValue.text.toString().trim()
        val truckNo = binding.truckNumber.text.toString().trim()
        val startOdo = binding.etStartOdometer.text.toString().trim()

        // Validation
        if (selectedDate.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.please_select_date), Toast.LENGTH_SHORT).show()
            return
        }

        if (srcPlace.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.please_enter_source), Toast.LENGTH_SHORT).show()
            binding.srcPlaceValue.requestFocus()
            return
        }

        if (destPlace.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.please_enter_destination), Toast.LENGTH_SHORT).show()
            binding.destPlaceValue.requestFocus()
            return
        }

        if (truckNo.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.please_enter_truck_number), Toast.LENGTH_SHORT).show()
            binding.truckNumber.requestFocus()
            return
        }

        // Update route list with final values
        if (routeList.isEmpty()) {
            routeList.add(srcPlace)
            routeList.add(destPlace)
        } else {
            routeList[0] = srcPlace
            routeList[routeList.size - 1] = destPlace
        }

        // Pass data back to MainActivity
        (activity as? MainActivity)?.getDataFill(
            truckNumber = truckNo,
            srcPlaceValue = srcPlace,
            destPlaceValue = destPlace,
            startDate = selectedDate,
            endDate = "",
            truckAvg = "",
            driverTripAvak = "",
            totalDays = "0",
            startOdometer = startOdo,
            route = ArrayList(routeList) // Pass the complete route
        )

        dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        // Set dialog to be full width with proper styling
        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)

            // Add margins
            attributes?.let { params ->
                val margin = context.resources.getDimensionPixelSize(R.dimen._12sdp)
                params.width = context.resources.displayMetrics.widthPixels - (margin * 2)
                attributes = params
            }
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()

        // Alternative: Set dialog width here if onCreateDialog doesn't work
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)

            attributes?.let { params ->
                val margin = context.resources.getDimensionPixelSize(R.dimen._12sdp)
                params.width = context.resources.displayMetrics.widthPixels - (margin * 2)
                attributes = params
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech?.shutdown()
        _binding = null
    }
}