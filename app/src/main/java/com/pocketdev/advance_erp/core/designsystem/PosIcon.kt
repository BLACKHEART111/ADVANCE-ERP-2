package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom crisp vector icon representing a modern POS counter terminal with top receipt & screen.
 */
val PosTerminalIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PosTerminal",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Top receipt paper with zig-zag / clean edge
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(7.5f, 2f)
            lineTo(16.5f, 2f)
            lineTo(16.5f, 6.2f)
            lineTo(7.5f, 6.2f)
            close()
            // Receipt line cutout
            moveTo(9.2f, 3.6f)
            lineTo(14.8f, 3.6f)
            lineTo(14.8f, 4.7f)
            lineTo(9.2f, 4.7f)
            close()
        }
        // Main POS terminal body with screen & keypad cutouts
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd
        ) {
            // Outer rounded-corner terminal chassis (x: 4.5..19.5, y: 7.2..22)
            moveTo(6.5f, 7.2f)
            lineTo(17.5f, 7.2f)
            quadTo(19.5f, 7.2f, 19.5f, 9.2f)
            lineTo(19.5f, 20f)
            quadTo(19.5f, 22f, 17.5f, 22f)
            lineTo(6.5f, 22f)
            quadTo(4.5f, 22f, 4.5f, 20f)
            lineTo(4.5f, 9.2f)
            quadTo(4.5f, 7.2f, 6.5f, 7.2f)
            close()

            // Display screen cutout (x: 7..17, y: 9..13)
            moveTo(7.6f, 9f)
            lineTo(16.4f, 9f)
            quadTo(17f, 9f, 17f, 9.6f)
            lineTo(17f, 12.4f)
            quadTo(17f, 13f, 16.4f, 13f)
            lineTo(7.6f, 13f)
            quadTo(7f, 13f, 7f, 12.4f)
            lineTo(7f, 9.6f)
            quadTo(7f, 9f, 7.6f, 9f)
            close()

            // Keypad row 1 (3 buttons)
            moveTo(7.2f, 14.7f); lineTo(9.4f, 14.7f); lineTo(9.4f, 16.5f); lineTo(7.2f, 16.5f); close()
            moveTo(10.9f, 14.7f); lineTo(13.1f, 14.7f); lineTo(13.1f, 16.5f); lineTo(10.9f, 16.5f); close()
            moveTo(14.6f, 14.7f); lineTo(16.8f, 14.7f); lineTo(16.8f, 16.5f); lineTo(14.6f, 16.5f); close()

            // Keypad row 2 (2 buttons + wide green/confirm key)
            moveTo(7.2f, 17.8f); lineTo(9.4f, 17.8f); lineTo(9.4f, 19.8f); lineTo(7.2f, 19.8f); close()
            moveTo(10.9f, 17.8f); lineTo(16.8f, 17.8f); lineTo(16.8f, 19.8f); lineTo(10.9f, 19.8f); close()
        }
    }.build()
}
