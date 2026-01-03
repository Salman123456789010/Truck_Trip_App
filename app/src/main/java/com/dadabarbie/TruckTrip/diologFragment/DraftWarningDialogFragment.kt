package com.dadabarbie.TruckTrip.diologFragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.LanguageSelction
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.LogoutDialogFragmentBinding
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.vasyerp.freshvegetables.util.NetworkResult

class DraftWarningDialogFragment (val flag:String) : DialogFragment(), View.OnClickListener {
    private lateinit var logoutDialogFragmentBinding: LogoutDialogFragmentBinding
    lateinit var logOutDialogFragment: LogOutDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        logoutDialogFragmentBinding = LogoutDialogFragmentBinding.inflate(
            inflater, container, false
        )
        if (flag == "Logout") {
            logoutDialogFragmentBinding.title.text = getString(R.string.draft_data)
            logoutDialogFragmentBinding.descreption.text =
                getString(R.string.if_you_want_to_logout_so_your_draft_data_will_be_clear_so_are_you_want_to_logout)

        }else{
            logoutDialogFragmentBinding.title.text = getString(R.string.delete_data)
            logoutDialogFragmentBinding.descreption.text =
                getString(R.string.if_you_want_to_delete_so_your_draft_data_will_be_clear_so_are_you_want_to_logout)

        }
            setOnClickListner()
        return logoutDialogFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val width = (resources.displayMetrics.widthPixels * 0.90)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog!!.window!!.setLayout(width.toInt(), ActionBar.LayoutParams.WRAP_CONTENT)
    }

    private fun setOnClickListner() {
        logoutDialogFragmentBinding.btCancel.setOnClickListener(this)
        logoutDialogFragmentBinding.btSignup.setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        when (v) {
            logoutDialogFragmentBinding.btCancel -> {
                dialog?.dismiss()
            }

            logoutDialogFragmentBinding.btSignup -> {
                if (flag == "Logout") {
                    logOutDialogFragment= LogOutDialogFragment("Logout")
                    logOutDialogFragment.show(childFragmentManager,"")
//                    dialog?.dismiss()
                } else {
                    logOutDialogFragment= LogOutDialogFragment("AccountDelete")
                    logOutDialogFragment.show(childFragmentManager,"")
//                    dialog?.dismiss()
                }


            }
        }
    }

    private suspend fun getDelete(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val tripDataDao = database.productsDao()
        val entities = tripDataDao.deleteAll()
    }
}