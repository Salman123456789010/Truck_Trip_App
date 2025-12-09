package com.dadabarbie.TruckTrip.Utils

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import com.dadabarbie.TruckTrip.databinding.DialogUpdateBinding

// Replace with your actual package name

class UpdateDialog(private val context: Context) {

    private lateinit var dialog: Dialog

    fun show() {
        val binding = DialogUpdateBinding.inflate(LayoutInflater.from(context))
        dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(binding.root)
            setCancelable(false)  // Prevents user from dismissing the dialog
            setCanceledOnTouchOutside(false)  // Prevents dismissal by touching outside
        }

        // Set Click Listener for "Update" button
        binding.btnUpdate.setOnClickListener {
            openPlayStore()
        }

        dialog.show()
    }

    private fun openPlayStore() {
        val appPackageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
            context.startActivity(intent)
        }
    }
}
