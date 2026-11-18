package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.CartonMeasurement
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CartonMeasurementTest {
    @Test fun centimetresConvertToCubicMetresForOneCarton() {
        val measurement = requireNotNull(CartonMeasurement.parseOrNull("40*50*40"))
        assertEquals(0, BigDecimal("0.080").compareTo(measurement.cbm))
        assertEquals(0, BigDecimal("50").compareTo(measurement.widthCm))
    }
    @Test fun acceptsPastedAndBanglaDimensions() {
        listOf("40 × 50 × 40", "40x50x40", "40X50X40", "৪০*৫০*৪০", " 40 * 50 * 40 ").forEach {
            assertEquals(0, BigDecimal("0.08").compareTo(requireNotNull(CartonMeasurement.parseOrNull(it)).cbm))
        }
    }
    @Test fun keepsFractionalPrecision() {
        assertEquals(0, BigDecimal("0.00000125").compareTo(requireNotNull(CartonMeasurement.parseOrNull(".5*1*2.5")).cbm))
    }
    @Test fun measurementIsOptional() {
        assertNull(CartonMeasurement.parseOrNull(""))
        assertNull(CartonMeasurement.parseOrNull("   "))
    }
    @Test fun rejectsIncompleteMalformedAndNonPositiveMeasurements() {
        listOf("40*50", "40**40", "40*50*40*2", "0*50*40", "-1*50*40", "40*no*40", "1e2*50*40", "40cm*50*40").forEach {
            assertNull(it, CartonMeasurement.parseOrNull(it))
        }
    }
}
