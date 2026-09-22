package com.dadabarbie.TruckTrip.fuelplanner.model

import com.dadabarbie.TruckTrip.R
import java.io.Serializable

/**
 * Extensible enum for truck load conditions.
 */
enum class LoadCondition(val displayName: String) {
    EMPTY("Empty"),
    HALF_LOADED("Half Loaded"),
    FULLY_LOADED("Fully Loaded")
}

/**
 * Three clear destination reachability statuses:
 * - SAFE: Can reach destination while maintaining safety reserve.
 * - REFUEL_RECOMMENDED: Can reach destination technically, but fuel falls below safety reserve.
 * - REFUEL_REQUIRED: Cannot safely reach destination with current fuel.
 */
enum class ReachabilityStatus(
    val title: String,
    val description: String,
    val titleRes: Int = 0,
    val descriptionRes: Int = 0
) {
    SAFE(
        "SAFE",
        "Destination reachable without refuelling.",
        R.string.status_safe_title,
        R.string.status_safe_desc
    ),
    REFUEL_RECOMMENDED(
        "REFUEL RECOMMENDED",
        "Your current fuel can cover the trip, but refuelling is recommended to maintain your safety reserve.",
        R.string.status_refuel_rec_title,
        R.string.status_refuel_rec_desc
    ),
    REFUEL_REQUIRED(
        "REFUEL REQUIRED",
        "Fuel stop required before destination.",
        R.string.status_refuel_req_title,
        R.string.status_refuel_req_desc
    )
}

/**
 * Represents an individual recommended fuel stop along a trip route.
 */
data class FuelStop(
    val sequence: Int,
    val stationId: String? = null,
    val stationName: String? = null,
    val distanceFromStartKm: Double,
    val distanceFromPreviousKm: Double,
    val estimatedFuelBeforeStopLiters: Double,
    val recommendedFuelQuantityLiters: Double,
    val estimatedCost: Double? = null,
    val remainingDistanceKm: Double,
    val truckFriendly: Boolean = true,
    val notes: String? = null
) : Serializable

/**
 * Complete plan representing inputs, calculations, reachability status, recommendations, and fuel stops.
 */
data class FuelPlan(
    val tripId: String? = null,
    val startLocation: String,
    val destinationLocation: String,
    val totalDistanceKm: Double,
    val truckNumber: String? = null,
    val loadCondition: LoadCondition = LoadCondition.FULLY_LOADED,
    val currentFuelLiters: Double,
    val tankCapacityLiters: Double,
    val averageMileageKmPerL: Double,
    val fuelPricePerLiter: Double? = null,
    val safetyReservePercent: Double = 20.0,
    val estimatedFuelRequiredLiters: Double,
    val fuelWithSafetyLiters: Double,
    val theoreticalRangeKm: Double,
    val safeRangeKm: Double,
    val fuelRemainingAtDestinationLiters: Double,
    val reachabilityStatus: ReachabilityStatus,
    val recommendedRefuelDistanceMinKm: Double? = null,
    val recommendedRefuelDistanceMaxKm: Double? = null,
    val recommendedFuelQuantityLiters: Double = 0.0,
    val estimatedFuelCost: Double? = null,
    val fuelCostPerKm: Double? = null,
    val fuelStops: List<FuelStop> = emptyList(),
    val createdAtTimestamp: Long = System.currentTimeMillis()
) : Serializable

/**
 * Model representing fuel stations for future map/places integration.
 */
data class FuelStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceFromRouteKm: Double = 0.0,
    val distanceFromPreviousStopKm: Double = 0.0,
    val fuelPrice: Double? = null,
    val dieselAvailable: Boolean = true,
    val is24x7: Boolean = true,
    val truckAccessible: Boolean = true,
    val parkingAvailable: Boolean = true,
    val toiletAvailable: Boolean = false,
    val restaurantAvailable: Boolean = false,
    val repairServiceAvailable: Boolean = false,
    val adBlueAvailable: Boolean = false,
    val brandName: String? = null
) : Serializable

/**
 * Result of input validation.
 */
data class CalculationValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val errorRes: Int = 0
)
