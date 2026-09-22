package com.dadabarbie.TruckTrip.fuelplanner.service

import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.fuelplanner.model.CalculationValidationResult
import com.dadabarbie.TruckTrip.fuelplanner.model.ReachabilityStatus

/**
 * Service providing core mathematical calculations and validations for the Fuel Stop Planner.
 * Contains pure functions without Android UI or framework dependencies for easy unit testing.
 */
object FuelCalculationService {

    /**
     * Estimated Fuel Required = Total Distance / Average Mileage
     */
    fun calculateEstimatedFuel(distanceKm: Double, averageMileageKmPerL: Double): Double {
        if (averageMileageKmPerL <= 0.0) return 0.0
        return distanceKm / averageMileageKmPerL
    }

    /**
     * Fuel Required With Safety Margin = Estimated Fuel Required * (1 + Safety Reserve %)
     */
    fun calculateFuelWithSafety(estimatedFuel: Double, safetyReservePercent: Double): Double {
        val reserveFactor = 1.0 + (safetyReservePercent / 100.0)
        return estimatedFuel * reserveFactor
    }

    /**
     * Current Theoretical Range = Current Fuel * Average Mileage
     */
    fun calculateTheoreticalRange(currentFuelLiters: Double, averageMileageKmPerL: Double): Double {
        if (currentFuelLiters <= 0.0 || averageMileageKmPerL <= 0.0) return 0.0
        return currentFuelLiters * averageMileageKmPerL
    }

    /**
     * Safe Usable Fuel = Current Fuel * (1 - Safety Reserve %)
     */
    fun calculateSafeUsableFuel(currentFuelLiters: Double, safetyReservePercent: Double): Double {
        if (currentFuelLiters <= 0.0) return 0.0
        val usableFactor = (1.0 - (safetyReservePercent / 100.0)).coerceAtLeast(0.0)
        return currentFuelLiters * usableFactor
    }

    /**
     * Safe Range = Safe Usable Fuel * Average Mileage
     * = Current Fuel * (1 - Safety Reserve %) * Mileage
     */
    fun calculateSafeRange(
        currentFuelLiters: Double,
        safetyReservePercent: Double,
        averageMileageKmPerL: Double
    ): Double {
        val safeFuel = calculateSafeUsableFuel(currentFuelLiters, safetyReservePercent)
        return safeFuel * averageMileageKmPerL
    }

    /**
     * Fuel Remaining At Destination = Current Fuel - (Distance / Mileage)
     */
    fun calculateFuelRemainingAtDestination(
        currentFuelLiters: Double,
        distanceKm: Double,
        averageMileageKmPerL: Double
    ): Double {
        val consumed = calculateEstimatedFuel(distanceKm, averageMileageKmPerL)
        return currentFuelLiters - consumed
    }

    /**
     * Determine reachability status based on current fuel, estimated fuel, and safety margin.
     * - SAFE: Current fuel >= Fuel With Safety (or remaining fuel >= safety reserve fuel)
     * - REFUEL RECOMMENDED: Current fuel >= Estimated Fuel, but < Fuel With Safety
     * - REFUEL REQUIRED: Current fuel < Estimated Fuel
     */
    fun determineReachabilityStatus(
        currentFuelLiters: Double,
        estimatedFuelRequired: Double,
        fuelWithSafety: Double
    ): ReachabilityStatus {
        return when {
            currentFuelLiters < estimatedFuelRequired -> ReachabilityStatus.REFUEL_REQUIRED
            currentFuelLiters < fuelWithSafety -> ReachabilityStatus.REFUEL_RECOMMENDED
            else -> ReachabilityStatus.SAFE
        }
    }

    /**
     * Fuel Cost = Fuel Liters * Fuel Price Per Liter
     */
    fun calculateFuelCost(fuelLiters: Double, fuelPricePerLiter: Double): Double {
        if (fuelLiters <= 0.0 || fuelPricePerLiter <= 0.0) return 0.0
        return fuelLiters * fuelPricePerLiter
    }

    /**
     * Fuel Cost Per KM = Total Fuel Cost / Distance
     */
    fun calculateFuelCostPerKm(fuelCost: Double, distanceKm: Double): Double {
        if (distanceKm <= 0.0 || fuelCost <= 0.0) return 0.0
        return fuelCost / distanceKm
    }

    /**
     * Validates input values.
     */
    fun validateInputs(
        distanceKm: Double?,
        averageMileageKmPerL: Double?,
        currentFuelLiters: Double?,
        tankCapacityLiters: Double?,
        fuelPricePerLiter: Double? = null,
        safetyReservePercent: Double? = 20.0
    ): CalculationValidationResult {
        if (distanceKm == null || distanceKm <= 0.0) {
            return CalculationValidationResult(false, "Please enter the trip distance.", R.string.please_enter_distance)
        }
        if (averageMileageKmPerL == null || averageMileageKmPerL <= 0.0) {
            return CalculationValidationResult(false, "Please enter a valid mileage.", R.string.please_enter_mileage)
        }
        if (tankCapacityLiters == null || tankCapacityLiters <= 0.0) {
            return CalculationValidationResult(false, "Please enter a valid tank capacity.", R.string.please_enter_tank_capacity)
        }
        if (currentFuelLiters == null || currentFuelLiters < 0.0) {
            return CalculationValidationResult(false, "Current fuel cannot be negative.")
        }
        if (currentFuelLiters > tankCapacityLiters) {
            return CalculationValidationResult(false, "Current fuel cannot be greater than tank capacity.", R.string.current_fuel_exceeds_tank)
        }
        if (fuelPricePerLiter != null && fuelPricePerLiter < 0.0) {
            return CalculationValidationResult(false, "Fuel price cannot be negative.")
        }
        if (safetyReservePercent != null && (safetyReservePercent < 0.0 || safetyReservePercent > 100.0)) {
            return CalculationValidationResult(false, "Safety reserve must be between 0% and 100%.")
        }
        return CalculationValidationResult(true)
    }
}
