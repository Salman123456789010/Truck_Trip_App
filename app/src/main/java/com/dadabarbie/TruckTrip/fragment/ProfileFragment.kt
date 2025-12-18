package com.dadabarbie.TruckTrip.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.DashBoardActivity
import com.dadabarbie.TruckTrip.activity.HowToUseActivity
import com.dadabarbie.TruckTrip.activity.LanguageSelction
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.FragmentProfileBinding
import com.dadabarbie.TruckTrip.diologFragment.DraftWarningDialogFragment
import com.dadabarbie.TruckTrip.diologFragment.LogOutDialogFragment
import com.dadabarbie.TruckTrip.room.AppDatabase
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment(),View.OnClickListener {

    lateinit var binding:FragmentProfileBinding
    private val authViewModel: AuthViewModel by viewModels()
    lateinit var logOutDialogFragment: DraftWarningDialogFragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as DashBoardActivity).textChanges(4)
        // Inflate the layout for this fragment
       binding=FragmentProfileBinding.inflate(inflater,container,false)
        binding.driverNumber.text="${Constants.usermobileNumber}"
        authViewModel.page = 0
        binding.appVersion.text=getAppVersionName(requireContext())
        clickListner()
        return binding.root
    }
    fun getAppVersionName(context: Context): String {
        return try {
            val packageManager = context.packageManager
            val packageName = context.packageName

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33 and above
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                // Below API 33
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }

            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }



    private fun clickListner() {
        binding.logOutLayout.setOnClickListener(this)
        binding.deleteLayout.setOnClickListener(this)
        binding.contactLayout.setOnClickListener(this)
        binding.howtouse.setOnClickListener(this)
        binding.privacyPolicy.setOnClickListener(this)
        binding.facebookIcon.setOnClickListener(this)
        binding.instagramIcon.setOnClickListener(this)
        binding.youtubeIcon.setOnClickListener(this)
        binding.term.setOnClickListener(this)
        binding.changeLanguageLayout.setOnClickListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adsLoad()
    }

    fun sendGmail(){
        val recipient = "salmankhanbelim8@gmail.com"
        val subject = "Truck Trip FeedBack"
        val message = ""

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient)) // Recipient email
            putExtra(Intent.EXTRA_SUBJECT, subject) // Email Subject
            putExtra(Intent.EXTRA_TEXT, message) // Email Body
        }

        try {
            startActivity(Intent.createChooser(intent, "Choose an Email app"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No email clients installed!", Toast.LENGTH_SHORT).show()
        }

    }

    private fun adsLoad() {
        GlobalScope.launch {
            MobileAds.initialize(requireContext())
            val adLoader = AdLoader.Builder(requireActivity(), "ca-app-pub-8808039515208362/1007625609")
                .forNativeAd { nativeAd ->
                    binding.myTemplate.setNativeAd(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("AdMob", "Ad failed to load: ${error.message}")
                    }
                })
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    override fun onClick(v: View?) {
        when(v){
            binding.logOutLayout->{
                logOutDialogFragment= DraftWarningDialogFragment("Logout")
                logOutDialogFragment.show(childFragmentManager,"")
            }
            binding.deleteLayout->{
                logOutDialogFragment= DraftWarningDialogFragment("AccountDelete")
                logOutDialogFragment.show(childFragmentManager,"")
            }
            binding.howtouse->{
                startActivity(Intent(requireActivity(), HowToUseActivity::class.java))
            }
            binding.contactLayout->{
                val url = "https://truckwallah.co.in/"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            binding.changeLanguageLayout->{
                startActivity(Intent(requireActivity(),LanguageSelction::class.java).putExtra("languageFlag","set"))
            }
            binding.privacyPolicy->{
                val url = "https://truckwallah.co.in/privacy-policy.html"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)

            }
            binding.instagramIcon -> {
                val url = "https://www.instagram.com/invites/contact/?igsh=1n6742y7lnou7&utm_content=108miyti"
                openLink(url)
            }
            binding.youtubeIcon->{
                val url = "https://www.youtube.com/@TruckWallah_TW"
                openLink(url)
            }

// Facebook Click
            binding.facebookIcon -> {
                val url = "https://www.facebook.com/share/1D9T3eyGNF/"
                openLink(url)
            }
            binding.term->{
                val url = "https://truckwallah.co.in/terms-and-condition.html"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
//            binding.rateUs->{
//                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireActivity().applicationContext.packageName}"))
//                context?.startActivity(intent)
//            }
        }
    }
    private fun openLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }



}