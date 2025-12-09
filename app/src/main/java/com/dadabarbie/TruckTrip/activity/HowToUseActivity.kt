package com.dadabarbie.TruckTrip.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.adapter.OnboardingAdapter
import com.dadabarbie.TruckTrip.databinding.ActivityHowToUseBinding
import com.dadabarbie.TruckTrip.model.OnboardingItem
import com.google.android.material.tabs.TabLayoutMediator

class HowToUseActivity : AppCompatActivity() {
    lateinit var binding: ActivityHowToUseBinding
    private lateinit var adapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHowToUseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Constants.setStatusBar(this, isLight = true, colorRes = R.color.white)
        initAdapter()
        setOnClickListeners()
    }

    private fun initAdapter() {
        adapter = OnboardingAdapter()
        binding.viewPager.adapter = adapter

        val items = listOf(
            OnboardingItem(
                imageRes = R.drawable.trip_list,
                title = getString(R.string.trip_list_title),
                description = getString(R.string.trip_help_text)
            ),
            OnboardingItem(
                imageRes = R.drawable.add_trip_dialog,
                title = getString(R.string.start_new_trip),
                description = getString(R.string.add_trip_dec)
            ),
            OnboardingItem(
                imageRes = R.drawable.trip_transcation,
                title = "Manage Trip Hisab",
                description = "Add all trip records from here.\n" +
                        "Use Add Expenditure to add expenses,\n" +
                        "Add Income to add earnings,\n" +
                        "and Add Fuel to enter diesel details.\n" +
                        "All transactions appear in the list below.\n" +
                        "Total profit is shown at the top in blue."
            ),
            OnboardingItem(
                imageRes = R.drawable.add_expenditure,
                title = "Add expenses",
                description = "Enter the expense amount, note and place.\n" +
                        "Select the expense type and date.\n" +
                        "You can also use the mic to speak and add details.\n" +
                        "Tap Save and the expense will appear in the trip hisab list."
            ),
            OnboardingItem(
                imageRes = R.drawable.add_income,
                title = "Add income (bhada / advance)",
                description = "Enter the income amount and add a note like party name or reason.\n" +
                        "You can also use the mic to speak and add details.\n" +
                        "Tap Save and the income will be added to the trip hisab and total profit."
            ),

            OnboardingItem(
                imageRes = R.drawable.add_fuel,
                title = "Add fuel Details",
                description = "Enter diesel liters and amount to add fuel expense.\n" +
                        "Enter the KM reading (odometer) to calculate mileage.\n" +
                        "Add the place or pump name and select the date.\n" +
                        "Tap Save to add fuel to trip hisab and average."
            ),
            OnboardingItem(
                imageRes = R.drawable.complelte_trip,
                title = "Complete the Trip",
                description = "Select the trip end date and enter the driver income for display.\n" +
                        "This amount is only for showing driver income, it is not used in calculations.\n" +
                        "Tap Submit to complete the trip and download the PDF for sharing."
            ),

        )
        adapter.submitList(items)
        TabLayoutMediator(binding.indicator, binding.viewPager) { tab, _ ->
            tab.setIcon(R.drawable.tab_dot_selector)
        }.attach()
//        TabLayoutMediator(binding.indicator, binding.viewPager) { _, _ -> }.attach()
    }

    private fun setOnClickListeners() {
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnSkip.setOnClickListener {
            val pos = binding.viewPager.currentItem
            if (pos == 0) {
                finish()
            } else {
                binding.viewPager.currentItem = pos - 1
            }
        }
        binding.btnNext.setOnClickListener {
            val nextIndex = binding.viewPager.currentItem + 1
            if (nextIndex < (binding.viewPager.adapter?.itemCount ?: 0)) {
                binding.viewPager.currentItem = nextIndex
            } else {
                finish()
            }
        }
    }

    private fun updateBottomButtons(position: Int) {
        if (position == 0) {
            binding.btnSkip.text = getString(R.string.onboard_skip)
        } else {
            binding.btnSkip.text = getString(R.string.onboard_back)
        }
    }
}
