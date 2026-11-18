package com.pocketdev.advance_erp.core.engine

import java.math.BigDecimal
import java.math.RoundingMode

data class MixedCartonLineInput(
    val productId: String,
    val variantId: String? = null,
    val productName: String = "",
    val quantity: Int,
    val manualGrossWeightKg: BigDecimal? = null
)

data class MixedCartonLineAllocated(
    val productId: String,
    val variantId: String? = null,
    val productName: String = "",
    val quantity: Int,
    val allocatedGrossWeightKg: BigDecimal,
    val allocatedNetWeightKg: BigDecimal
)

object CartonWeightAllocationEngine {
    private const val WEIGHT_SCALE = 3

    /** Manual shares are kept exactly; only the remaining weight is distributed by quantity. */
    fun allocateWeights(
        cartonGrossWeightKg: BigDecimal,
        cartonNetWeightKg: BigDecimal = BigDecimal.ZERO,
        lines: List<MixedCartonLineInput>
    ): List<MixedCartonLineAllocated> {
        require(cartonGrossWeightKg > BigDecimal.ZERO) { "Carton gross weight must be greater than zero" }
        require(cartonNetWeightKg >= BigDecimal.ZERO && cartonNetWeightKg <= cartonGrossWeightKg) {
            "Net weight must be between zero and gross weight"
        }
        fun validatePrecision(weight: BigDecimal) {
            require(weight.stripTrailingZeros().scale() <= WEIGHT_SCALE) { "Weight supports up to 3 decimal places (kg)" }
        }
        validatePrecision(cartonGrossWeightKg)
        validatePrecision(cartonNetWeightKg)
        require(lines.isNotEmpty()) { "Carton must contain at least one product line" }
        lines.forEach {
            require(it.quantity > 0) { "Product line quantity must be strictly positive: ${it.productName}" }
            it.manualGrossWeightKg?.let { weight ->
                require(weight >= BigDecimal.ZERO) { "Manual weight cannot be negative" }
                validatePrecision(weight)
            }
        }
        val manualSum = lines.fold(BigDecimal.ZERO) { sum, line -> sum + (line.manualGrossWeightKg ?: BigDecimal.ZERO) }
        require(manualSum <= cartonGrossWeightKg) { "Manual weights exceed carton gross weight" }
        val automatic = lines.indices.filter { lines[it].manualGrossWeightKg == null }
        require(automatic.isNotEmpty() || manualSum.compareTo(cartonGrossWeightKg) == 0) {
            "Manual weights must add up to carton gross weight"
        }
        fun distribute(total: BigDecimal, indices: List<Int>, weights: List<BigDecimal>): List<BigDecimal> {
            if (indices.isEmpty()) return List(lines.size) { BigDecimal.ZERO }
            val denominator = indices.fold(BigDecimal.ZERO) { acc, index -> acc + weights[index] }
            require(denominator > BigDecimal.ZERO || total.signum() == 0) { "Cannot distribute weight over zero quantity" }
            val shares = MutableList(lines.size) { BigDecimal.ZERO }
            indices.forEach { index ->
                shares[index] = if (total.signum() == 0) BigDecimal.ZERO else total.multiply(weights[index]).divide(denominator, WEIGHT_SCALE, RoundingMode.DOWN)
            }
            var remaining = (total - shares.fold(BigDecimal.ZERO, BigDecimal::add)).movePointRight(WEIGHT_SCALE).intValueExact()
            val ordered = indices.sortedByDescending { weights[it] }
            var cursor = 0
            while (remaining > 0) {
                val index = ordered[cursor++ % ordered.size]
                shares[index] = shares[index] + BigDecimal("0.001")
                remaining--
            }
            return shares
        }
        val gross = distribute(cartonGrossWeightKg - manualSum, automatic, lines.map { BigDecimal(it.quantity) }).toMutableList()
        lines.forEachIndexed { index, line -> line.manualGrossWeightKg?.let { gross[index] = it } }
        // Net follows the actual gross shares so no line can have net greater than gross.
        val net = distribute(cartonNetWeightKg, lines.indices.toList(), gross)
        return lines.mapIndexed { index, line ->
            MixedCartonLineAllocated(line.productId, line.variantId, line.productName, line.quantity, gross[index], net[index])
        }
    }
}
