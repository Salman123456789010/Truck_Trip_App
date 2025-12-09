package com.dadabarbie.TruckTrip.diologFragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.dismissProgress
import com.dadabarbie.TruckTrip.Utils.Constants.languageLocale
import com.dadabarbie.TruckTrip.Utils.Constants.loginUser
import com.dadabarbie.TruckTrip.Utils.Constants.showProgress
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.LanguageSelction
import com.dadabarbie.TruckTrip.auth.activity.LoginScreenActivity
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.LogoutDialogFragmentBinding
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LogOutDialogFragment(val flag:String) : DialogFragment(), View.OnClickListener {
    private lateinit var logoutDialogFragmentBinding: LogoutDialogFragmentBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        logoutDialogFragmentBinding = LogoutDialogFragmentBinding.inflate(
            inflater, container, false
        )

        if(flag=="Logout"){
            logoutDialogFragmentBinding.title.text=getString(R.string.log_out)
            logoutDialogFragmentBinding.descreption.text=getString(R.string.are_you_sure_want_to_log_out)
        }else{
            logoutDialogFragmentBinding.title.text=getString(R.string.delete_account)
            logoutDialogFragmentBinding.descreption.text= getString(R.string.are_you_sure_want_to_delete_your_account)
        }
        setOnClickListner()
        setObserver()
        return logoutDialogFragmentBinding.root
    }
    private fun unSubscribeToTopic(topic:String){
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnCompleteListener { task->
            if(task.isSuccessful){
                Log.d("TAG123", "subscribeToTopic: ")
            }else{

            }

        }
    }

    private fun setObserver() {
        authViewModel.logOutUser.observe(this) {
            when (it) {
                is NetworkResult.Error -> {
                    dialog?.dismiss()
                    dismissProgress()
                    Toast.makeText(requireContext(),"Something issue in Server Please try Again later",Toast.LENGTH_SHORT).show()
                }

                is NetworkResult.Loading -> {
                   requireContext().showProgress()
                }

                is NetworkResult.Success -> {
                    dismissProgress()
                    unSubscribeToTopic(loginUser)
                    Prefs.clearAll()
                    Prefs[Constants.languageCode] = languageLocale.toString()
                    GlobalScope.launch {
                        getDelete(requireContext())
                    }
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(requireActivity(), LoginScreenActivity::class.java).putExtra("languageFlag",""))

                }
            }
        }
        authViewModel.deleteAccount.observe(this){
            when (it) {
                is NetworkResult.Error -> {
                    dialog?.dismiss()
                    dismissProgress()
                    Toast.makeText(requireContext(),
                        getString(R.string.something_issue_in_server_please_try_again_later),Toast.LENGTH_SHORT).show()
                }

                is NetworkResult.Loading -> {
                    requireContext().showProgress()
                }

                is NetworkResult.Success -> {
                    dismissProgress()
                    unSubscribeToTopic(loginUser)
                    Prefs.clearAll()
                    Prefs[Constants.languageCode] = languageLocale.toString()
                    GlobalScope.launch {
                        getDelete(requireContext())
                    }
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(requireActivity(), LoginScreenActivity::class.java).putExtra("languageFlag",""))
                }
            }
        }
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
                if(flag=="Logout"){
                    authViewModel.logOut()
                }else{
                    authViewModel.deleteAccount()
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