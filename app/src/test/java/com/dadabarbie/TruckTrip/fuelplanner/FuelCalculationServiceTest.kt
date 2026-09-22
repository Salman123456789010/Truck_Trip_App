package com.dadabarbie.TruckTrip.fuelplanner

import com.dadabarbie.TruckTrip.fuelplanner.model.ReachabilityStatus
import com.dadabarbie.TruckTrip.fuelplanner.service.FuelCalculationService
import com.dadabarbie.TruckTrip.fuelplanner.service.FuelStopPlanningService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelCalculationServiceTest {

    // Test 1: 525 km, 3.8 km/L -> Expected fuel ≈ 138.16 L
    @Test
    fun test1_estimatedFuelCalculation() {
        val distance = 525.0
        val mileage = 3.8
        val estimatedFuel = FuelCalculationService.calculateEstimatedFuel(distance, mileage)
        assertEquals(138.1578947, estimatedFuel, 0.01)
    }

    // Test 2: 150 L, 3.8 km/L -> Expected theoretical range = 570 km
    @Test
    fun test2_theoreticalRangeCalculation() {
        val currentFuel = 150.0
        val mileage = 3.8
        val range = FuelCalculationService.calculateTheoreticalRange(currentFuel, mileage)
        assertEquals(570.0, range, 0.001)
    }

    // Test 3: 150 L, 20% reserve, 3.8 km/L -> Expected safe range = 456 km
    @Test
    fun test3_safeRangeCalculation() {
        val currentFuel = 150.0
        val reservePercent = 20.0
        val mileage = 3.8
        val safeRange = FuelCalculationService.calculateSafeRange(currentFuel, reservePercent, mileage)
        assertEquals(456.0, safeRange, 0.001)
    }

    // Test 4: Current fuel less than required -> Expected status: REFUEL REQUIRED
    @Test
    fun test4_reachabilityRefuelRequired() {
        val estimatedFuel = 138.16
        val fuelWithSafety = 165.79
        val currentFuel = 100.0 // Less than required 138.16

        val status = FuelCalculationService.determineReachabilityStatus(currentFuel, estimatedFuel, fuelWithSafety)
        assertEquals(ReachabilityStatus.REFUEL_REQUIRED, status)
    }

    // Test 5: Current fuel enough but below reserve at destination -> Expected status: REFUEL RECOMMENDED
    @Test
    fun test5_reachabilityRefuelRecommended() {
        val estimatedFuel = 138.16
        val fuelWithSafety = 165.79
        val currentFuel = 150.0 // Enough for 138.16, but less than 165.79 (reserve violated at dest)

        val status = FuelCalculationService.determineReachabilityStatus(currentFuel, estimatedFuel, fuelWithSafety)
        assertEquals(ReachabilityStatus.REFUEL_RECOMMENDED, status)
    }

    // Test 6: Current fuel enough with reserve -> Expected status: SAFE
    @Test
    fun test6_reachabilitySafe() {
        val estimatedFuel = 138.16
        val fuelWithSafety = 165.79
        val currentFuel = 180.0 // Greater than fuel with safety

        val status = FuelCalculationService.determineReachabilityStatus(currentFuel, estimatedFuel, fuelWithSafety)
        assertEquals(ReachabilityStatus.SAFE, status)
    }

    // Test 7: Current fuel > tank capacity -> Expected validation error
    @Test
    fun test7_currentFuelGreaterThanTankCapacityValidation() {
        val result = FuelCalculationService.validateInputs(
            distanceKm = 525.0,
            averageMileageKmPerL = 3.8,
            currentFuelLiters = 350.0,
            tankCapacityLiters = 300.0,
            fuelPricePerLiter = 95.0
        )
        assertFalse(result.isValid)
        assertEquals("Current fuel cannot be greater than tank capacity.", result.errorMessage)
    }

    // Test 8: Mileage = 0 -> Expected validation error
    @Test
    fun test8_mileageZeroValidation() {
        val result = FuelCalculationService.validateInputs(
            distanceKm = 525.0,
            averageMileageKmPerL = 0.0,
            currentFuelLiters = 150.0,
            tankCapacityLiters = 300.0,
            fuelPricePerLiter = 95.0
        )
        assertFalse(result.isValid)
        assertEquals("Please enter a valid mileage.", result.errorMessage)
    }

    // Test 9: Long-distance trip -> Expected multiple fuel stops
    @Test
    fun test9_longDistanceTripMultipleFuelStops() {
        val plan = FuelStopPlanningService.planFuelTrip(
            startLocation = "Delhi",
            destinationLocation = "Chennai",
            totalDistanceKm = 2200.0,
            currentFuelLiters = 150.0,
            tankCapacityLiters = 300.0,
            averageMileageKmPerL = 3.8,
            fuelPricePerLiter = 95.0,
            safetyReservePercent = 20.0
        )

        assertTrue("Long trip should generate multiple fuel stops", plan.fuelStops.size > 1)
        assertEquals(ReachabilityStatus.REFUEL_REQUIRED, plan.reachabilityStatus)
    }
}
