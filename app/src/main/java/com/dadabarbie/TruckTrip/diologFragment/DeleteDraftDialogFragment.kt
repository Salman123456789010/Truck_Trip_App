package com.dadabarbie.TruckTrip.diologFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.DialogFragment
import com.dadabarbie.TruckTrip.activity.DraftActivity
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.activity.SimpleDraftActivity
import com.dadabarbie.TruckTrip.databinding.DeleteDialogFragmentBinding

class DeleteDraftDialogFragment(val position: Int) : DialogFragment(), View.OnClickListener {

    lateinit var binding: DeleteDialogFragmentBinding
    var mobileNumber: String = ""

    override fun onClick(v: View?) {
        when (v) {
            binding.btCancel -> {
                dialog?.dismiss()
            }

            binding.btSignup -> {
                try {
                    (requireActivity() as DraftActivity).deleteFromDatabse(position)
                }catch (e: Exception){}

                try {
                    (requireActivity() as SimpleDraftActivity).deleteFromDatabse(position)
                }catch (e: Exception){}

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