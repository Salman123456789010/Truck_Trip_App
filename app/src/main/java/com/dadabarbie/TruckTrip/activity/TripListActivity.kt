package com.dadabarbie.TruckTrip.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.adapter.TripAdapter
import com.dadabarbie.TruckTrip.databinding.ActivityTripListBinding


class TripListActivity : AppCompatActivity() {
    lateinit var binding: ActivityTripListBinding
    lateinit var tripAdapter: TripAdapter
    private val tripList:ArrayList<String> = arrayListOf("Trip : Mangrol to Rajastan.pdf","Trip : Veraval to Kerla.pdf")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityTripListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    private fun initViews() {
        tripAdapter= TripAdapter()
        binding.tripList.adapter = tripAdapter
        tripAdapter.submitList(tripList)
        tripAdapter.notifyItemRangeChanged(0, Constants.creditList.size)
    }

}
