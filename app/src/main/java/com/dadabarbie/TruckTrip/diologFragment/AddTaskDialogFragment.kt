package com.dadabarbie.TruckTrip.diologFragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.creditList
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.model.CreditModel
import com.dadabarbie.TruckTrip.model.DebitModel
import com.dadabarbie.TruckTrip.databinding.AddTaskDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class AddTaskDialogFragment(var expanseText:String,var amount:String="",var flagTag:String,var position:Int) : BottomSheetDialogFragment(), View.OnClickListener {
    lateinit var binding: AddTaskDialogBinding
    var typeFlag: Boolean = true
    var extractNewAmount="0"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = AddTaskDialogBinding.inflate(layoutInflater, container, false)
        setOnClickListner()
        binding.addText.setText(expanseText.toString())
        binding.amount.setText(amount.toString())
        if(flagTag=="Credit"){
            binding.income.setBackgroundColor(Color.parseColor("#2155FF"))
            binding.income.setTextColor(Color.WHITE)
            binding.expanse.setTextColor(Color.parseColor("#2155FF"))
            binding.expanse.setBackgroundColor(Color.WHITE)
            typeFlag = false
        }else if(flagTag=="Debit"){
            binding.expanse.setBackgroundColor(Color.parseColor("#2155FF"))
            binding.expanse.setTextColor(Color.WHITE)
            binding.income.setTextColor(Color.parseColor("#2155FF"))
            binding.income.setBackgroundColor(Color.WHITE)
            typeFlag = true
        }

        return binding.root
    }

    private fun setOnClickListner() {
        binding.save.setOnClickListener(this)
        binding.save.setOnClickListener(this)
        binding.income.setOnClickListener(this)
        binding.mic.setOnClickListener(this)
        binding.expanse.setOnClickListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val width = (resources.displayMetrics.widthPixels * 0.80)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog!!.window!!.setLayout(width.toInt(), ActionBar.LayoutParams.WRAP_CONTENT)
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.save -> {
                if(flagTag=="Credit" || flagTag=="Debit"){
                    if(flagTag=="Credit"){
                        creditList[position].desc=binding.addText.text.toString()
                        creditList[position].amount=binding.amount.text.toString()
                        (requireActivity() as MainActivity).creditDataUpdate()
                        dialog?.dismiss()
                    }else{
                        Constants.debitList[position].desc=binding.addText.text.toString()
                        Constants.debitList[position].amount=binding.amount.text.toString()
                        (requireActivity() as MainActivity).debitDataUpdate()
                        dialog?.dismiss()
                    }
                }
                else{
                    if (!binding.addText.text.toString().isNullOrEmpty()) {
                        if (!typeFlag) {
                            Constants.emitEvent(
                                Event(
                                    CreditModel(
                                        desc = binding.addText.text.toString(),
                                        amount = if (binding.amount.text.toString() != "") binding.amount.text.toString() else "0"
                                    )
                                )
                            )
                        } else {
                            Log.d("TAG12", "onClick: ")
//                            Constants.emitDebitEvent(
//                                Event(
//                                    DebitModel(
//                                        desc = binding.addText.text.toString(),
//                                        amount = if (binding.amount.text.toString() != "") binding.amount.text.toString() else "0"
//                                    )
//                                )
//                            )
                        }
                        dialog?.dismiss()
                    } else {
                        binding.addText.requestFocus()
                        Toast.makeText(
                            requireActivity(), "Please Add First", Toast.LENGTH_SHORT
                        ).show()
                    }
                }


            }


            binding.mic -> {
                getTextToSpeech()
            }

            binding.income -> {
                binding.income.setBackgroundColor(Color.parseColor("#2155FF"))
                binding.income.setTextColor(Color.WHITE)
                binding.addText.hint=getString(R.string.enter_income_name)
                binding.amount.hint=getString(R.string.enter_income_amount)
                binding.expanse.setTextColor(Color.parseColor("#2155FF"))
                binding.expanse.setBackgroundColor(Color.WHITE)
                typeFlag = false
                flagTag=""
            }

            binding.expanse -> {
                binding.expanse.setBackgroundColor(Color.parseColor("#2155FF"))
                binding.expanse.setTextColor(Color.WHITE)
                binding.addText.hint=getString(R.string.enter_expenditure_name)
                binding.amount.hint=getString(R.string.enter_expenditure_amount)
                binding.income.setTextColor(Color.parseColor("#2155FF"))
                binding.income.setBackgroundColor(Color.WHITE)
                typeFlag = true
                flagTag=""
                if(flagTag=="Credit" || flagTag=="Debit"){

                }else{

                }
            }
        }
    }

    private fun getTextToSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            10 -> if (resultCode == RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                binding.amount.setText(processExtractedText(result!![0].toString()).toString())
                binding.addText.setText(result!![0].toString().replace(binding.amount.text.toString(),""))
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

    fun processExtractedText(text: String):Int {
        val resultText = text
        println("Extracted Text: $resultText")

        // You can extract specific numbers with regex here
        val regex = Regex("\\b\\d+\\b")  // matches individual numbers in the text
        val numbers = regex.findAll(resultText).map { it.value.toInt() }.toList()
        println("Extracted Numbers: $numbers")
        if(numbers.size>0){
            return numbers[0]
        }
        return 0
    }
}