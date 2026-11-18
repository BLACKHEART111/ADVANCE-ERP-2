package com.pocketdev.advance_erp.core.util

import java.math.BigDecimal

/** Outside carton dimensions in centimetres. CBM is for one physical carton. */
data class CartonMeasurement(val lengthCm: BigDecimal, val widthCm: BigDecimal, val heightCm: BigDecimal) {
    val cbm: BigDecimal get() = lengthCm.multiply(widthCm).multiply(heightCm).movePointLeft(6)

    companion object {
        /** Blank means not supplied; malformed or non-positive dimensions are rejected. */
        fun parseOrNull(input: String): CartonMeasurement? {
            val normalized = input.map { ch ->
                if (ch in '০'..'৯') ('0'.code + ch.code - '০'.code).toChar() else ch
            }.joinToString("")
            val parts = normalized.split(Regex("[xX×*]"))
            if (parts.size != 3) return null
            val number = Regex("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")
            val dimensions = parts.map { part ->
                val text = part.trim()
                if (!number.matches(text)) return null
                text.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null
            }
            return CartonMeasurement(dimensions[0], dimensions[1], dimensions[2])
        }
    }
}
