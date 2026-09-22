package com.dadabarbie.TruckTrip.backhaul

import com.dadabarbie.TruckTrip.backhaul.model.BackhaulEvaluationStatus
import com.dadabarbie.TruckTrip.backhaul.service.BackhaulCalculatorService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit test suite for BackhaulCalculatorService covering all test cases
 * specified in the feature requirements (Section 44 and edge cases).
 */
class BackhaulCalculatorServiceTest {

    // Test 1 — Empty Return: Outbound revenue = ₹40,000, Total cost = ₹33,000 -> Expected profit = ₹7,000
    @Test
    fun test1_emptyReturnProfit() {
        val outboundRevenue = 40000.0
        val totalCost = 33000.0
        val profit = BackhaulCalculatorService.calculateProfit(outboundRevenue, totalCost)
        assertEquals(7000.0, profit, 0.001)
    }

    // Test 2 — Backhaul: Outbound revenue = ₹40,000, Return revenue = ₹25,000, Total cost = ₹33,000 -> Expected profit = ₹32,000
    @Test
    fun test2_backhaulProfit() {
        val outboundRevenue = 40000.0
        val returnRevenue = 25000.0
        val totalCost = 33000.0
        val totalRevenue = BackhaulCalculatorService.calculateTotalRevenue(
            outboundRevenue = outboundRevenue,
            returnRevenue = returnRevenue,
            isWithBackhaul = true
        )
        val profit = BackhaulCalculatorService.calculateProfit(totalRevenue, totalCost)
        assertEquals(65000.0, totalRevenue, 0.001)
        assertEquals(32000.0, profit, 0.001)
    }

    // Test 3 — Empty Return Cost: Return fuel = ₹10,000, Return toll = ₹2,000 -> Expected empty return cost = ₹12,000
    @Test
    fun test3_emptyReturnCost() {
        val returnFuel = 10000.0
        val returnToll = 2000.0
        val returnDriver = 0.0
        val returnOther = 0.0
        val emptyReturnCost = BackhaulCalculatorService.calculateEmptyReturnCost(
            returnFuel = returnFuel,
            returnToll = returnToll,
            returnDriver = returnDriver,
            returnOther = returnOther
        )
        assertEquals(12000.0, emptyReturnCost, 0.001)
    }

    // Test 4 — Minimum Backhaul: Empty return cost = ₹12,000 -> Expected minimum break-even backhaul = ₹12,000
    @Test
    fun test4_minimumBackhaulBreakEvenPrice() {
        val emptyReturnCost = 12000.0
        val minBackhaul = BackhaulCalculatorService.calculateMinimumBackhaulPrice(emptyReturnCost)
        assertEquals(12000.0, minBackhaul, 0.001)
    }

    // Test 5 — Target Profit: Empty return cost = ₹12,000, Target additional profit = ₹10,000 -> Expected required backhaul = ₹22,000
    @Test
    fun test5_targetProfitBackhaulPrice() {
        val emptyReturnCost = 12000.0
        val targetAdditionalProfit = 10000.0
        val targetBackhaul = BackhaulCalculatorService.calculateTargetProfitBackhaulPrice(
            emptyReturnCost = emptyReturnCost,
            desiredAdditionalProfit = targetAdditionalProfit
        )
        assertEquals(22000.0, targetBackhaul, 0.001)
    }

    // Test 6 — Negative Profit: Outbound Revenue = ₹30,000, Total Cost = ₹35,000 -> Expected profit = -₹5,000
    @Test
    fun test6_negativeProfitLoss() {
        val revenue = 30000.0
        val cost = 35000.0
        val profit = BackhaulCalculatorService.calculateProfit(revenue, cost)
        assertEquals(-5000.0, profit, 0.001)
    }

    // Test 7 — Different Return Distance: Outbound = 500 km, Return = 600 km -> Expected total distance = 1,100 km
    @Test
    fun test7_differentReturnDistance() {
        val outbound = 500.0
        val returnDist = 600.0
        val totalDistance = BackhaulCalculatorService.calculateTotalDistance(outbound, returnDist)
        assertEquals(1100.0, totalDistance, 0.001)
    }

    // Test 8 — Zero Cost: Cost = ₹0 -> Ensure no division-by-zero errors or NaN
    @Test
    fun test8_zeroCostAndZeroDistanceSafety() {
        val totalCost = BackhaulCalculatorService.calculateTotalCost(0.0, 0.0, 0.0, 0.0)
        assertEquals(0.0, totalCost, 0.001)

        val profit = BackhaulCalculatorService.calculateProfit(40000.0, totalCost)
        assertEquals(40000.0, profit, 0.001)

        val profitPerKmZeroDistance = BackhaulCalculatorService.calculateProfitPerKm(profit, 0.0)
        assertEquals(0.0, profitPerKmZeroDistance, 0.001)
        assertFalse(profitPerKmZeroDistance.isNaN())
        assertFalse(profitPerKmZeroDistance.isInfinite())
    }

    // Test 9 — Profit Per KM Calculation
    @Test
    fun test9_profitPerKm() {
        // Without backhaul: ₹7,000 / 1,000 km = ₹7/km
        val profitEmpty = 7000.0
        val totalDistance = 1000.0
        val profitPerKmEmpty = BackhaulCalculatorService.calculateProfitPerKm(profitEmpty, totalDistance)
        assertEquals(7.0, profitPerKmEmpty, 0.001)

        // With backhaul: ₹32,000 / 1,000 km = ₹32/km
        val profitBackhaul = 32000.0
        val profitPerKmBackhaul = BackhaulCalculatorService.calculateProfitPerKm(profitBackhaul, totalDistance)
        assertEquals(32.0, profitPerKmBackhaul, 0.001)
    }

    // Test 10 — Profit Improvement and Percentage
    @Test
    fun test10_profitImprovement() {
        val emptyReturnProfit = 7000.0
        val backhaulProfit = 32000.0
        val improvement = BackhaulCalculatorService.calculateProfitImprovement(backhaulProfit, emptyReturnProfit)
        assertEquals(25000.0, improvement, 0.001)

        val percentage = BackhaulCalculatorService.calculateProfitImprovementPercentage(backhaulProfit, emptyReturnProfit)
        // (25000 / 7000) * 100 ≈ 357.14%
        assertEquals(357.142857, percentage!!, 0.01)

        // Baseline negative or zero profit returns null percentage (no misleading %)
        val zeroBasePercent = BackhaulCalculatorService.calculateProfitImprovementPercentage(10000.0, 0.0)
        assertNull(zeroBasePercent)

        val negativeBasePercent = BackhaulCalculatorService.calculateProfitImprovementPercentage(5000.0, -2000.0)
        assertNull(negativeBasePercent)
    }

    // Test 11 — Return Load Acceptance Evaluation
    @Test
    fun test11_returnLoadAcceptanceCheck() {
        val emptyReturnCost = 12000.0

        // Case 1: Empty Return
        val evalEmpty = BackhaulCalculatorService.evaluateBackhaulStatus(
            isWithBackhaul = false,
            returnRevenue = 0.0,
            emptyReturnCost = emptyReturnCost
        )
        assertEquals(BackhaulEvaluationStatus.EMPTY_RETURN, evalEmpty.status)

        // Case 2: Below return cost
        val evalBelow = BackhaulCalculatorService.evaluateBackhaulStatus(
            isWithBackhaul = true,
            returnRevenue = 8000.0,
            emptyReturnCost = emptyReturnCost
        )
        assertEquals(BackhaulEvaluationStatus.BELOW_RETURN_COST, evalBelow.status)

        // Case 3: Exactly covers return cost (break-even)
        val evalBreakEven = BackhaulCalculatorService.evaluateBackhaulStatus(
            isWithBackhaul = true,
            returnRevenue = 12000.0,
            emptyReturnCost = emptyReturnCost
        )
        assertEquals(BackhaulEvaluationStatus.BREAK_EVEN, evalBreakEven.status)

        // Case 4: Strong backhaul
        val evalStrong = BackhaulCalculatorService.evaluateBackhaulStatus(
            isWithBackhaul = true,
            returnRevenue = 25000.0,
            emptyReturnCost = emptyReturnCost
        )
        assertEquals(BackhaulEvaluationStatus.STRONG_BACKHAUL, evalStrong.status)
    }

    // Test 12 — Overall Trip Break-Even vs Empty Return Break-Even distinction
    @Test
    fun test12_breakEvenDistinction() {
        val totalTripCost = 33000.0
        val outboundRevenue = 40000.0
        val emptyReturnCost = 12000.0

        // Overall trip break-even: Outbound already covers 33k cost -> Required backhaul = 0
        val overallBreakEven = BackhaulCalculatorService.calculateOverallTripBreakEvenBackhaul(
            totalCost = totalTripCost,
            outboundRevenue = outboundRevenue
        )
        assertEquals(0.0, overallBreakEven, 0.001)

        // Empty return break-even remains the cost of the return leg
        val emptyReturnBreakEven = BackhaulCalculatorService.calculateMinimumBackhaulPrice(emptyReturnCost)
        assertEquals(12000.0, emptyReturnBreakEven, 0.001)
    }

    // Test 13 — Complete Plan Computation
    @Test
    fun test13_completePlanComputation() {
        val plan = BackhaulCalculatorService.computePlan(
            outboundDistanceKm = 500.0,
            returnDistanceKm = 500.0,
            outboundRevenue = 40000.0,
            isWithBackhaul = true,
            returnRevenue = 25000.0,
            fuelCost = 22000.0,
            tollCost = 4000.0,
            driverCost = 5000.0,
            otherCost = 2000.0,
            explicitEmptyReturnCost = 12000.0,
            targetAdditionalProfit = 10000.0
        )

        assertEquals(1000.0, plan.totalDistanceKm, 0.001)
        assertEquals(65000.0, plan.totalRevenue, 0.001)
        assertEquals(33000.0, plan.totalTripCost, 0.001)
        assertEquals(12000.0, plan.emptyReturnCost, 0.001)
        assertEquals(7000.0, plan.emptyReturnProfit, 0.001)
        assertEquals(7.0, plan.emptyReturnProfitPerKm, 0.001)
        assertEquals(32000.0, plan.backhaulProfit, 0.001)
        assertEquals(32.0, plan.backhaulProfitPerKm, 0.001)
        assertEquals(25000.0, plan.profitImprovement, 0.001)
        assertEquals(12000.0, plan.minimumBackhaulPrice, 0.001)
        assertEquals(22000.0, plan.targetProfitBackhaulPrice, 0.001)
        assertEquals(13000.0, plan.additionalReturnContribution, 0.001)
        assertEquals(BackhaulEvaluationStatus.STRONG_BACKHAUL, plan.evaluation.status)
    }

    // Test 14 — Scenario Simulation
    @Test
    fun test14_scenarioSimulation() {
        val scenarios = BackhaulCalculatorService.simulateScenarios(
            outboundRevenue = 40000.0,
            totalTripCost = 33000.0,
            totalDistanceKm = 1000.0,
            emptyReturnCost = 12000.0,
            emptyReturnProfit = 7000.0,
            prices = listOf(10000.0, 15000.0, 20000.0, 25000.0)
        )

        assertEquals(4, scenarios.size)
        assertEquals(17000.0, scenarios[0].totalProfit, 0.001) // 40k + 10k - 33k = 17k
        assertEquals(22000.0, scenarios[1].totalProfit, 0.001) // 40k + 15k - 33k = 22k
        assertEquals(27000.0, scenarios[2].totalProfit, 0.001) // 40k + 20k - 33k = 27k
        assertEquals(32000.0, scenarios[3].totalProfit, 0.001) // 40k + 25k - 33k = 32k
        assertFalse(scenarios[0].coversEmptyReturnCost) // 10k < 12k
        assertTrue(scenarios[1].coversEmptyReturnCost) // 15k >= 12k
    }

    // Test 15 — Input Validations
    @Test
    fun test15_inputValidation() {
        // Distance 0 or negative
        val v1 = BackhaulCalculatorService.validateInputs(
            outboundDistanceKm = 0.0,
            returnDistanceKm = 500.0,
            outboundRevenue = 40000.0,
            returnRevenue = 20000.0,
            isWithBackhaul = true
        )
        assertFalse(v1.isValid)

        // Negative expenses
        val v2 = BackhaulCalculatorService.validateInputs(
            outboundDistanceKm = 500.0,
            returnDistanceKm = 500.0,
            outboundRevenue = 40000.0,
            returnRevenue = 20000.0,
            isWithBackhaul = true,
            fuelCost = -500.0
        )
        assertFalse(v2.isValid)

        // Valid inputs
        val v3 = BackhaulCalculatorService.validateInputs(
            outboundDistanceKm = 500.0,
            returnDistanceKm = 500.0,
            outboundRevenue = 40000.0,
            returnRevenue = 25000.0,
            isWithBackhaul = true,
            fuelCost = 22000.0,
            tollCost = 4000.0
        )
        assertTrue(v3.isValid)
    }
}
