package com.dadabarbie.TruckTrip.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
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
                title = getString(R.string.manage_trip_title),
                description = getString(R.string.trip_transaction_dec)
            ),
            OnboardingItem(
                imageRes = R.drawable.add_expenditure,
                title = getString(R.string.add_expanse_tittle),
                description = getString(R.string.add_expense_dec)
            ),
            OnboardingItem(
                imageRes = R.drawable.add_income,
                title = getString(R.string.add_income_tittle),
                description = getString(R.string.add_income_dec)
            ),

            OnboardingItem(
                imageRes = R.drawable.add_fuel,
                title = getString(R.string.add_fuel_tittle),
                description = getString(R.string.add_fuel_dec)
            ),
            OnboardingItem(
                imageRes = R.drawable.complelte_trip,
                title = getString(R.string.complete_trip_tittle),
                description = getString(R.string.complete_trip_dec)
            ),

        )
        adapter.submitList(items)
        TabLayoutMediator(binding.indicator, binding.viewPager) { tab, _ ->
            tab.setIcon(R.drawable.tab_dot_selector)
        }.attach()
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomButtons(position)
            }
        })

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
            binding.btnSkip.text = "Skip"
        } else {
            binding.btnSkip.text = "Back"
        }
    }
}
