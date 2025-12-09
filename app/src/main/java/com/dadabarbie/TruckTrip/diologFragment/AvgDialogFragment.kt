package com.dadabarbie.TruckTrip.diologFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.databinding.AvgGetBinding

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.math.RoundingMode
import java.text.DecimalFormat

class AvgDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {
    lateinit var binding: AvgGetBinding
    override fun onClick(v: View?) {
        when (v) {
            binding.save -> {
                val avgValue=roundOffDecimal(( binding.enterKilometer.text.toString().toInt().toDouble() / binding.literDiesel.text.toString().toInt().toDouble())).toString()
                Constants.emitAvg(Event(avgValue))
                dialog?.dismiss()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val width = (resources.displayMetrics.widthPixels * 0.80)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog!!.window!!.setLayout(width.toInt(), ActionBar.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = AvgGetBinding.inflate(layoutInflater, container, false)
        setOnClickListner()
        return binding.root
    }

    private fun setOnClickListner() {
        binding.save.setOnClickListener(this)
    }
    private fun roundOffDecimal(number: Double): Double? {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }

}