package com.dadabarbie.TruckTrip.fuelplanner.service

import com.dadabarbie.TruckTrip.fuelplanner.model.FuelStation

/**
 * Service interface for searching fuel stations along a route.
 * In MVP, returns offline status with appropriate messaging without faking data.
 */
interface FuelStationService {
    suspend fun getStationsAlongRoute(
        origin: String,
        destination: String,
        routePolyline: String? = null
    ): List<FuelStation>

    fun isServiceConnected(): Boolean
    fun getStatusMessage(): String
}

class DefaultFuelStationService : FuelStationService {
    override suspend fun getStationsAlongRoute(
        origin: String,
        destination: String,
        routePolyline: String?
    ): List<FuelStation> {
        // In MVP without real API keys, return empty list (never invent fake real-world stations)
        return emptyList()
    }

    override fun isServiceConnected(): Boolean = false

    override fun getStatusMessage(): String =
        "Fuel station recommendations will appear when route/fuel station services are connected."
}
