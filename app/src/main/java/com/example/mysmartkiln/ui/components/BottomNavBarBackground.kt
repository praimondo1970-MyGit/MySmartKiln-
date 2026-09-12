package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.theme.LocalKilnColors

/** Forma curva de la barra inferior (mismo path que el diseño original). */
object CurvedBottomNavShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            val d = density.density
            val curveStartY = 34f * d

            moveTo(0f, curveStartY)
            lineTo(width * (147f / 390f), curveStartY)
            appendNavTopCurve(width, d, curveStartY)
            lineTo(width, curveStartY)
            lineTo(width, curveStartY + (20f * d))
            lineTo(width, height)
            lineTo(0f, height)
            lineTo(0f, curveStartY + (20f * d))
            close()
        }
        return Outline.Generic(path)
    }
}

/** Solo el trazo superior (recto + notch del FAB), sin lados ni borde inferior. */
private fun Path.appendNavTopCurve(width: Float, d: Float, curveStartY: Float) {
    cubicTo(
        width * (158.046f / 390f), curveStartY,
        width * (166.586f / 390f), curveStartY + (9.8745f * d),
        width * (173.422f / 390f), curveStartY + (18.5508f * d)
    )
    cubicTo(
        width * (178.915f / 390f), curveStartY + (25.5232f * d),
        width * (187.435f / 390f), curveStartY + (30f * d),
        width * (197f / 390f), curveStartY + (30f * d)
    )
    cubicTo(
        width * (206.565f / 390f), curveStartY + (30f * d),
        width * (215.085f / 390f), curveStartY + (25.5232f * d),
        width * (220.578f / 390f), curveStartY + (18.5508f * d)
    )
    cubicTo(
        width * (227.414f / 390f), curveStartY + (9.8745f * d),
        width * (235.954f / 390f), curveStartY,
        width * (247f / 390f), curveStartY
    )
}

/** Fondo del menú inferior: relleno + borde fino solo en el borde superior. */
@Composable
fun BottomNavBarBackground(modifier: Modifier = Modifier) {
    val kilnColors = LocalKilnColors.current
    val topBorderColor = kilnColors.cardBorder

    if (kilnColors.isEsp) {
        // ESP: barra plana (sin notch); el + vive dentro de la fila de tabs.
        Box(
            modifier = modifier
                .fillMaxWidth()
                .requiredHeight(141.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .requiredHeight(107.dp)
                    .background(color = kilnColors.navBar)
                    .drawWithContent {
                        drawContent()
                        drawLine(
                            color = topBorderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .requiredHeight(141.dp)
                .background(color = kilnColors.navBar, shape = CurvedBottomNavShape)
                .drawWithContent {
                    drawContent()
                    val d = density
                    val curveStartY = 34f * d
                    val topPath = Path().apply {
                        moveTo(0f, curveStartY)
                        lineTo(size.width * (147f / 390f), curveStartY)
                        appendNavTopCurve(size.width, d, curveStartY)
                        lineTo(size.width, curveStartY)
                    }
                    drawPath(
                        path = topPath,
                        color = topBorderColor,
                        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
        )
    }
}
