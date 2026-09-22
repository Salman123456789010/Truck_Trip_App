package com.dadabarbie.TruckTrip.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.activity.DashBoardActivity
import com.dadabarbie.TruckTrip.activity.FeedBackActivity
import com.dadabarbie.TruckTrip.activity.HowToUseActivity
import com.dadabarbie.TruckTrip.activity.HowToUseNormalActivity
import com.dadabarbie.TruckTrip.activity.LanguageSelction
import com.dadabarbie.TruckTrip.activity.NormalUserDashBoard
import com.dadabarbie.TruckTrip.activity.SubscriptionActivity
import com.dadabarbie.TruckTrip.activity.TripModeSelectionActivity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class ProfileFragment : Fragment(),View.OnClickListener {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    lateinit var logOutDialogFragment: DraftWarningDialogFragment

    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveProfilePhoto(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
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
        binding.privacyPolicy.setOnClickListener(this)
        binding.facebookIcon.setOnClickListener(this)
        binding.instagramIcon.setOnClickListener(this)
        binding.youtubeIcon.setOnClickListener(this)
        binding.feedbackGive.setOnClickListener(this)
        binding.term.setOnClickListener(this)
        binding.changeLanguageLayout.setOnClickListener(this)

        // Profile photo click handlers
        binding.ivEditPhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }
        binding.cardProfileAvatar.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }
        binding.ivDeletePhoto.setOnClickListener {
            deleteProfilePhoto()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            (activity as DashBoardActivity).textChanges(4)
        }catch (e: Exception){

        }

        try {
            (activity as NormalUserDashBoard).textChanges(4)
        }catch (e: Exception){}
        adsLoad()
        loadProfilePhoto()
        loadFleetStats()
    }

    override fun onResume() {
        super.onResume()
        loadProfilePhoto()
        loadFleetStats()
    }

    private fun getActiveUserId(): String {
        val mobile = Constants.usermobileNumber.ifEmpty {
            Prefs[Constants.mobileNumber, ""].toString()
        }
        val clean = mobile.filter { it.isLetterOrDigit() }
        return if (clean.isNotEmpty()) clean else "default_user"
    }

    private fun getProfilePhotoFile(): File {
        val userId = getActiveUserId()
        return File(requireContext().filesDir, "profile_avatar_${userId}.jpg")
    }

    private fun loadProfilePhoto() {
        if (_binding == null) return
        try {
            val file = getProfilePhotoFile()
            if (file.exists() && file.length() > 0) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val circularDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap).apply {
                        isCircular = true
                    }
                    binding.profileLogo.setImageDrawable(circularDrawable)
                    binding.profileLogo.scaleType = ImageView.ScaleType.CENTER_CROP
                    binding.ivDeletePhoto.visibility = View.VISIBLE
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error loading profile photo: ${e.message}")
        }
        // Fallback default avatar
        binding.profileLogo.setImageResource(R.drawable.truck_just_logo)
        binding.profileLogo.scaleType = ImageView.ScaleType.CENTER_INSIDE
        binding.ivDeletePhoto.visibility = View.GONE
    }

    private fun saveProfilePhoto(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val file = getProfilePhotoFile()
                    val outputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.flush()
                    outputStream.close()

                    withContext(Dispatchers.Main) {
                        loadProfilePhoto()
                        Toast.makeText(requireContext(), getString(R.string.profile_photo_updated), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error saving photo: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteProfilePhoto() {
        android.app.AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.delete_photo_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                try {
                    val file = getProfilePhotoFile()
                    if (file.exists()) {
                        file.delete()
                    }
                    loadProfilePhoto()
                    Toast.makeText(requireContext(), getString(R.string.profile_photo_deleted), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error deleting photo: ${e.message}")
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun loadFleetStats() {
        val isSimpleMode = Prefs[Constants.appMode, ""] == "A"
        binding.tvActiveModeName.text = if (isSimpleMode) {
            getString(R.string.mode_simple_title)
        } else {
            getString(R.string.mode_full_title)
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val cachedTrips = db.tripRecordDao().getAll()
                val drafts = db.productsDao().getAllProducts()
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    binding.tvTotalTripsCount.text = cachedTrips.size.toString()
                    if (drafts.isNotEmpty()) {
                        binding.tvProfileDraftNotice.visibility = View.VISIBLE
                        binding.tvProfileDraftNotice.text = getString(R.string.active_draft_count, drafts.size)
                        binding.tvProfileDraftNotice.setOnClickListener {
                            startActivity(Intent(requireContext(), com.dadabarbie.TruckTrip.activity.DraftActivity::class.java))
                        }
                    } else {
                        binding.tvProfileDraftNotice.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error loading fleet stats: ${e.message}")
            }
        }
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
        com.dadabarbie.TruckTrip.ads.AdMobManager.loadNativeAd(
            requireActivity(),
            binding.myTemplate,
            com.dadabarbie.TruckTrip.ads.AdMobManager.NativePlacement.PROFILE_LIST
        )
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
            binding.feedbackGive->{
                startActivity(Intent(requireActivity(), FeedBackActivity::class.java))
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}