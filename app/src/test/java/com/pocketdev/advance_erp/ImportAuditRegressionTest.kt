package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.*
import com.pocketdev.advance_erp.data.entity.CostCategory
import java.math.BigDecimal
import org.junit.Test
import org.junit.Assert.*

class ImportAuditRegressionTest {
    @Test fun manualWeightsSurviveAllocationExactly() {
        val result=CartonWeightAllocationEngine.allocateWeights(BigDecimal("15"), BigDecimal("14"), listOf(
            MixedCartonLineInput("p", "s", quantity=20, manualGrossWeightKg=BigDecimal("12")),
            MixedCartonLineInput("p", "m", quantity=10, manualGrossWeightKg=BigDecimal("3"))))
        assertEquals(0, BigDecimal("12").compareTo(result[0].allocatedGrossWeightKg))
        assertEquals(0, BigDecimal("3").compareTo(result[1].allocatedGrossWeightKg))
        assertEquals(0, BigDecimal("14").compareTo(result.fold(BigDecimal.ZERO) { a,b -> a+b.allocatedNetWeightKg }))
    }
    @Test fun automaticLinesUseOnlyRemainingWeight() {
        val result=CartonWeightAllocationEngine.allocateWeights(BigDecimal("15"), lines=listOf(
            MixedCartonLineInput("p", "s", quantity=20, manualGrossWeightKg=BigDecimal("12")),
            MixedCartonLineInput("p", "m", quantity=10)))
        assertEquals(0, BigDecimal("3").compareTo(result[1].allocatedGrossWeightKg))
    }
    @Test(expected=IllegalArgumentException::class) fun excessivePrecisionHasExplicitValidation() {
        CartonWeightAllocationEngine.allocateWeights(BigDecimal("1.2345"), lines=listOf(MixedCartonLineInput("p",quantity=1)))
    }
    @Test(expected=IllegalArgumentException::class) fun manualSharesCannotExceedCartonWeight() {
        CartonWeightAllocationEngine.allocateWeights(BigDecimal("10"), lines=listOf(MixedCartonLineInput("p",quantity=1,manualGrossWeightKg=BigDecimal("12"))))
    }
    @Test fun variantAllocationKeysRoundTrip() {
        val amounts=mapOf("product:variant" to 12345L,"legacy-product" to 500L)
        assertEquals(amounts,LandedCostAllocationEngine.parseManualAllocations(LandedCostAllocationEngine.formatManualAllocations(amounts)))
    }
 @Test fun categoryFreightAndItsMirroredExpenseMustNotDoubleCharge() {
  val p=LandingCostEngine.ShipmentProductInput(productId="p",productNameEn="Audit",supplierId="s",categoryId="c",packedQuantity=30,unitPurchasePriceForeignMinor=4500,purchaseCurrency="CNY",purchaseExchangeRateToBdt=BigDecimal("17"))
  val c=LandingCostEngine.ShipmentCartonInput(cartonNumber="AUDIT-1",supplierId="s",productId="p",categoryId="c",quantity=30,grossWeightKg=BigDecimal("15"))
  val rate=LandingCostEngine.CategoryFreightRateInput(categoryId="c",categoryNameEn="Audit",defaultRatePerKgBdtMinor=12000)
  val expense=LandingCostEngine.ImportExpenseInput(expenseId="auto-freight",expenseName="Category Per-KG International Freight",category=CostCategory.INTERNATIONAL_FREIGHT,amountForeignMinor=180000,currency="BDT",exchangeRateToBdt=BigDecimal.ONE,allocationMethod=AllocationMethod.GROSS_WEIGHT)
  val r=LandingCostEngine.calculateShipmentLandingCost(listOf(p),listOf(c),emptyList(),listOf(rate),listOf(expense))
  assertEquals("Same category freight mirrored as an auto cost item should be counted once",180000L,r.totalInternationalFreightBdtMinor)
 }
 @Test fun additionalUserFreightIsNotLost() {
  val p=LandingCostEngine.ShipmentProductInput(productId="p",productNameEn="Test",supplierId="s",categoryId="c",packedQuantity=1,unitPurchasePriceForeignMinor=100)
  val carton=LandingCostEngine.ShipmentCartonInput("box","s","p",categoryId="c",quantity=1,grossWeightKg=BigDecimal.ONE)
  val rate=LandingCostEngine.CategoryFreightRateInput("c","Test",100L)
  val extra=LandingCostEngine.ImportExpenseInput("extra","Additional freight",CostCategory.INTERNATIONAL_FREIGHT,50)
  assertEquals(150L,LandingCostEngine.calculateShipmentLandingCost(listOf(p),listOf(carton),emptyList(),listOf(rate),listOf(extra)).totalInternationalFreightBdtMinor)
 }
}
