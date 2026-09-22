package com.dadabarbie.TruckTrip.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.adapter.FuelPriceAdapter
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.ActivityVahanInfoDetailsBinding
import com.dadabarbie.TruckTrip.Utils.Constants.gone
import com.dadabarbie.TruckTrip.Utils.Constants.visible
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint

import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.model.fuel.FuelCityRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@AndroidEntryPoint
class VahanInfoDetailsActivity : BaseActivity() {

    private val bindding: ActivityVahanInfoDetailsBinding by lazy {
        ActivityVahanInfoDetailsBinding.inflate(layoutInflater)
    }

    private val authViewModel: AuthViewModel by viewModels()

    private val fuelPriceAdapter = FuelPriceAdapter()

    private var isLoading = false
    private var currentPage = 0
    private var totalPages = 1
    private var currentCityQuery: String? = null
    private val pageSize = 40

    companion object {
        private const val PREF_KEY_DIESEL_PRICE_CACHE = "pref_key_diesel_price_cache_v2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bindding.root)
        initViews()
        loadInstantCache()
        setObservers()
    }

    private fun loadInstantCache() {
        try {
            val cachedJson = Prefs[PREF_KEY_DIESEL_PRICE_CACHE, ""]
            if (cachedJson.isNotEmpty()) {
                val type = object : TypeToken<List<FuelCityRecord>>() {}.type
                val cachedList: List<FuelCityRecord> = Gson().fromJson(cachedJson, type) ?: emptyList()
                if (cachedList.isNotEmpty()) {
                    fuelPriceAdapter.submitList(cachedList)
                    bindding.tvEmpty.gone()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initViews() {
        bindding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupSearchListener()

        bindding.rvFuelPrices.apply {
            layoutManager = LinearLayoutManager(this@VahanInfoDetailsActivity)
            adapter = fuelPriceAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return

                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading && currentPage < totalPages) {
                        if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 2) {
                            loadNextPage()
                        }
                    }
                }
            })
        }
        authViewModel.getCitywiseFuelPrices(
            state = null,
            city = "",
            page = 0,
            size = pageSize
        )
    }

    private fun setupSearchListener() {
        bindding.etCitySearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()

                if (query.length >= 3) {
                    if (query == currentCityQuery) return
                    currentCityQuery = query
                    currentPage = 0
                    totalPages = 1
                    fuelPriceAdapter.submitList(emptyList())
                    bindding.tvEmpty.gone()
                    loadPage(currentPage)
                } else if (query.isEmpty()) {
                    currentCityQuery = null
                    currentPage = 0
                    totalPages = 1
                    fuelPriceAdapter.submitList(emptyList())
                    bindding.tvEmpty.gone()
                    loadPage(currentPage)
                }
            }
        })
    }

    private fun loadNextPage() {
        if (currentPage >= totalPages) return
        loadPage(currentPage + 1)
    }

    private fun loadPage(page: Int) {
        if (isLoading || page > totalPages && page != 1) return
        isLoading = true
        if (fuelPriceAdapter.currentList.isEmpty()) {
            bindding.progressBar.visible()
        }

        val cityParam = currentCityQuery ?: ""
        authViewModel.getCitywiseFuelPrices(
            state = "",
            city = cityParam,
            page = page,
            size = pageSize
        )
    }

    private fun setObservers() {
        authViewModel.fuelPriceData.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    if (fuelPriceAdapter.currentList.isEmpty()) {
                        bindding.progressBar.visible()
                    }
                }

                is NetworkResult.Success -> {
                    bindding.progressBar.gone()
                    isLoading = false

                    val data = result.data?.data
                    if (data == null || data.records.isEmpty()) {
                        if (fuelPriceAdapter.currentList.isEmpty()) {
                            fuelPriceAdapter.submitList(emptyList())
                            bindding.tvEmpty.visible()
                        }
                        totalPages = 1
                        return@observe
                    }

                    currentPage = data.currentPage
                    totalPages = data.totalPages

                    val newList = if (currentPage == 0 || currentCityQuery != null) {
                        data.records
                    } else {
                        val current = fuelPriceAdapter.currentList.toMutableList()
                        current.addAll(data.records)
                        current
                    }

                    fuelPriceAdapter.submitList(newList)
                    bindding.tvEmpty.gone()

                    // Save initial page data to instant cache
                    if (currentCityQuery.isNullOrEmpty() && data.records.isNotEmpty()) {
                        try {
                            Prefs[PREF_KEY_DIESEL_PRICE_CACHE] = Gson().toJson(data.records)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                is NetworkResult.Error -> {
                    bindding.progressBar.gone()
                    isLoading = false
                    if (fuelPriceAdapter.currentList.isEmpty()) {
                        bindding.tvEmpty.visible()
                    }
                }
            }
        }
    }
}

