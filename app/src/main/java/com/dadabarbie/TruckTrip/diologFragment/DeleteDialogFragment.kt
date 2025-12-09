package com.dadabarbie.TruckTrip.diologFragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Event
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.databinding.DeleteDialogFragmentBinding
import com.dadabarbie.TruckTrip.fragment.HomeFragment

import dagger.hilt.android.AndroidEntryPoint
import java.text.FieldPosition

@AndroidEntryPoint
class DeleteDialogFragment(private var position: Int, private var flagTag:String) : DialogFragment(),
    View.OnClickListener {

    lateinit var binding: DeleteDialogFragmentBinding
    var mobileNumber: String = ""

    override fun onClick(v: View?) {
        when(v){
            binding.btCancel->{
                dialog?.dismiss()
            }
            binding.btSignup->{
                   if(flagTag=="Debit"){
                       (requireActivity() as MainActivity).debitDeleteHissab(position)
                   }else if(flagTag=="Credit"){
                       (requireActivity() as MainActivity).creditDeleteHissab(position)
                   }else{
                       Constants.emitDeleteTrip(Event(position))

                   }
                dialog?.dismiss()
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DeleteDialogFragmentBinding.inflate(inflater, container, false)
        setOnClickListner()
        setObserver()
        return binding.root
    }

    private fun setObserver() {

    }

    private fun setOnClickListner() {
        binding.btCancel.setOnClickListener(this)
        binding.btSignup.setOnClickListener(this)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val width = (resources.displayMetrics.widthPixels * 0.90)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog!!.window!!.setLayout(width.toInt(), ActionBar.LayoutParams.WRAP_CONTENT)
    }
}