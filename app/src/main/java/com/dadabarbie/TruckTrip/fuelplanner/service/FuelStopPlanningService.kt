package com.dadabarbie.TruckTrip.fuelplanner.service

import com.dadabarbie.TruckTrip.fuelplanner.model.FuelPlan
import com.dadabarbie.TruckTrip.fuelplanner.model.FuelStop
import com.dadabarbie.TruckTrip.fuelplanner.model.LoadCondition
import com.dadabarbie.TruckTrip.fuelplanner.model.ReachabilityStatus
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Service that synthesizes calculations, generates multi-stop fuel plans,
 * and determines refuel recommendations with safety buffers.
 */
object FuelStopPlanningService {

    /**
     * Default buffer in km before safe range limit to recommend refuelling.
     * E.g. for safe range = 456 km, buffer gives a window around 400 - 430 km.
     */
    const val DEFAULT_BUFFER_KM_MIN = 25.0
    const val DEFAULT_BUFFER_KM_MAX = 55.0

    /**
     * Generates a complete FuelPlan based on inputs.
     */
    fun planFuelTrip(
        startLocation: String,
        destinationLocation: String,
        totalDistanceKm: Double,
        currentFuelLiters: Double,
        tankCapacityLiters: Double,
        averageMileageKmPerL: Double,
        fuelPricePerLiter: Double? = null,
        safetyReservePercent: Double = 20.0,
        loadCondition: LoadCondition = LoadCondition.FULLY_LOADED,
        truckNumber: String? = null,
        tripId: String? = null
    ): FuelPlan {
        val estimatedFuel = FuelCalculationService.calculateEstimatedFuel(totalDistanceKm, averageMileageKmPerL)
        val fuelWithSafety = FuelCalculationService.calculateFuelWithSafety(estimatedFuel, safetyReservePercent)
        val theoreticalRange = FuelCalculationService.calculateTheoreticalRange(currentFuelLiters, averageMileageKmPerL)
        val safeRange = FuelCalculationService.calculateSafeRange(currentFuelLiters, safetyReservePercent, averageMileageKmPerL)
        val fuelRemainingAtDest = FuelCalculationService.calculateFuelRemainingAtDestination(
            currentFuelLiters, totalDistanceKm, averageMileageKmPerL
        )
        val status = FuelCalculationService.determineReachabilityStatus(
            currentFuelLiters, estimatedFuel, fuelWithSafety
        )

        val fuelCost = if (fuelPricePerLiter != null && fuelPricePerLiter > 0.0) {
            FuelCalculationService.calculateFuelCost(estimatedFuel, fuelPricePerLiter)
        } else null

        val fuelCostPerKm = if (fuelCost != null) {
            FuelCalculationService.calculateFuelCostPerKm(fuelCost, totalDistanceKm)
        } else null

        // Recommended refuel window:
        val (refuelDistMin, refuelDistMax) = calculateRefuelDistanceWindow(safeRange, totalDistanceKm, status)

        // Recommended fuel quantity to add:
        val recommendedFuelToAdd = calculateRecommendedFuelToAdd(
            currentFuelLiters = currentFuelLiters,
            tankCapacityLiters = tankCapacityLiters,
            fuelWithSafetyLiters = fuelWithSafety,
            status = status
        )

        // Generate multi-stop itinerary for long trips or when refuel is required
        val stops = generateFuelStops(
            totalDistanceKm = totalDistanceKm,
            currentFuelLiters = currentFuelLiters,
            tankCapacityLiters = tankCapacityLiters,
            averageMileageKmPerL = averageMileageKmPerL,
            safetyReservePercent = safetyReservePercent,
            fuelPricePerLiter = fuelPricePerLiter,
            status = status
        )

        return FuelPlan(
            tripId = tripId,
            startLocation = startLocation,
            destinationLocation = destinationLocation,
            totalDistanceKm = totalDistanceKm,
            truckNumber = truckNumber,
            loadCondition = loadCondition,
            currentFuelLiters = currentFuelLiters,
            tankCapacityLiters = tankCapacityLiters,
            averageMileageKmPerL = averageMileageKmPerL,
            fuelPricePerLiter = fuelPricePerLiter,
            safetyReservePercent = safetyReservePercent,
            estimatedFuelRequiredLiters = estimatedFuel,
            fuelWithSafetyLiters = fuelWithSafety,
            theoreticalRangeKm = theoreticalRange,
            safeRangeKm = safeRange,
            fuelRemainingAtDestinationLiters = fuelRemainingAtDest,
            reachabilityStatus = status,
            recommendedRefuelDistanceMinKm = refuelDistMin,
            recommendedRefuelDistanceMaxKm = refuelDistMax,
            recommendedFuelQuantityLiters = recommendedFuelToAdd,
            estimatedFuelCost = fuelCost,
            fuelCostPerKm = fuelCostPerKm,
            fuelStops = stops
        )
    }

    /**
     * Calculates the recommended refuel distance window before reaching the safe range limit.
     * E.g. Safe range 456 km -> Window ~ 400 to 430 km.
     */
    fun calculateRefuelDistanceWindow(
        safeRangeKm: Double,
        totalDistanceKm: Double,
        status: ReachabilityStatus
    ): Pair<Double?, Double?> {
        if (status == ReachabilityStatus.SAFE) {
            return Pair(null, null)
        }

        val maxSafe = min(safeRangeKm, totalDistanceKm)
        if (maxSafe <= 0.0) {
            return Pair(0.0, 0.0)
        }

        val bufferMax = (maxSafe * 0.12).coerceIn(DEFAULT_BUFFER_KM_MIN, DEFAULT_BUFFER_KM_MAX)
        val bufferMin = (maxSafe * 0.05).coerceIn(10.0, 25.0)

        val windowMin = (maxSafe - bufferMax).coerceAtLeast(0.0)
        val windowMax = (maxSafe - bufferMin).coerceAtLeast(windowMin)

        return Pair(windowMin, windowMax)
    }

    /**
     * Calculates how much fuel should be added to safely complete the trip or restore reserve,
     * strictly capped at tank capacity.
     */
    fun calculateRecommendedFuelToAdd(
        currentFuelLiters: Double,
        tankCapacityLiters: Double,
        fuelWithSafetyLiters: Double,
        status: ReachabilityStatus
    ): Double {
        if (status == ReachabilityStatus.SAFE) {
            return 0.0
        }

        val neededToSafety = fuelWithSafetyLiters - currentFuelLiters
        val availableTankSpace = (tankCapacityLiters - currentFuelLiters).coerceAtLeast(0.0)

        return min(availableTankSpace, max(0.0, neededToSafety))
    }

    /**
     * Generates multiple fuel stops for journeys exceeding safe range or requiring refueling.
     */
    fun generateFuelStops(
        totalDistanceKm: Double,
        currentFuelLiters: Double,
        tankCapacityLiters: Double,
        averageMileageKmPerL: Double,
        safetyReservePercent: Double,
        fuelPricePerLiter: Double?,
        status: ReachabilityStatus
    ): List<FuelStop> {
        // If trip is safe without refuel, no stops needed
        if (status == ReachabilityStatus.SAFE && totalDistanceKm <= FuelCalculationService.calculateSafeRange(currentFuelLiters, safetyReservePercent, averageMileageKmPerL)) {
            return emptyList()
        }

        val stops = mutableListOf<FuelStop>()
        val fullTankSafeRange = FuelCalculationService.calculateSafeRange(
            tankCapacityLiters, safetyReservePercent, averageMileageKmPerL
        )

        var currentDistanceTravelled = 0.0
        var remainingDistance = totalDistanceKm
        var activeFuel = currentFuelLiters
        var stopSequence = 1

        // Maximum 10 stops safeguard
        while (remainingDistance > 0.0 && stopSequence <= 10) {
            val currentSafeRange = FuelCalculationService.calculateSafeRange(
                activeFuel, safetyReservePercent, averageMileageKmPerL
            )

            // Can we reach destination with current fuel safely?
            val fuelNeededForRest = FuelCalculationService.calculateEstimatedFuel(remainingDistance, averageMileageKmPerL)
            val fuelWithSafetyForRest = FuelCalculationService.calculateFuelWithSafety(fuelNeededForRest, safetyReservePercent)

            if (activeFuel >= fuelWithSafetyForRest) {
                // We have reached safe state for destination
                break
            }

            // Decide distance for this stop:
            // Stop around 85-90% of safe range (or safe range minus buffer),
            // but not past the remaining distance
            val targetLegDistance = if (currentSafeRange <= 20.0) {
                // Immediate fuel stop required (low fuel)
                min(15.0, remainingDistance)
            } else {
                val legBuffer = (currentSafeRange * 0.12).coerceIn(20.0, 50.0)
                val preferredLeg = (currentSafeRange - legBuffer).coerceAtLeast(20.0)
                min(preferredLeg, remainingDistance)
            }

            currentDistanceTravelled += targetLegDistance
            val fuelConsumedLeg = targetLegDistance / averageMileageKmPerL
            val fuelBeforeStop = (activeFuel - fuelConsumedLeg).coerceAtLeast(0.0)
            remainingDistance = (totalDistanceKm - currentDistanceTravelled).coerceAtLeast(0.0)

            // Calculate recommended refuel quantity at this stop:
            val fuelNeededRemaining = FuelCalculationService.calculateEstimatedFuel(remainingDistance, averageMileageKmPerL)
            val fuelWithSafetyRemaining = FuelCalculationService.calculateFuelWithSafety(fuelNeededRemaining, safetyReservePercent)
            val availableSpace = (tankCapacityLiters - fuelBeforeStop).coerceAtLeast(0.0)

            val recommendedFuel = min(
                availableSpace,
                max(20.0, fuelWithSafetyRemaining - fuelBeforeStop)
            )

            val stopCost = if (fuelPricePerLiter != null && fuelPricePerLiter > 0.0) {
                FuelCalculationService.calculateFuelCost(recommendedFuel, fuelPricePerLiter)
            } else null

            stops.add(
                FuelStop(
                    sequence = stopSequence,
                    stationId = null,
                    stationName = "Fuel Stop $stopSequence",
                    distanceFromStartKm = (currentDistanceTravelled * 10.0).roundToInt() / 10.0,
                    distanceFromPreviousKm = (targetLegDistance * 10.0).roundToInt() / 10.0,
                    estimatedFuelBeforeStopLiters = (fuelBeforeStop * 10.0).roundToInt() / 10.0,
                    recommendedFuelQuantityLiters = (recommendedFuel * 10.0).roundToInt() / 10.0,
                    estimatedCost = stopCost,
                    remainingDistanceKm = (remainingDistance * 10.0).roundToInt() / 10.0,
                    truckFriendly = true,
                    notes = if (remainingDistance <= fullTankSafeRange) "Final refuel for destination" else "En-route stop"
                )
            )

            // Truck refuels: new active fuel is fuelBeforeStop + recommendedFuel
            activeFuel = fuelBeforeStop + recommendedFuel
            stopSequence++

            // If remaining distance is 0, break
            if (remainingDistance <= 0.0) break
        }

        return stops
    }
}
