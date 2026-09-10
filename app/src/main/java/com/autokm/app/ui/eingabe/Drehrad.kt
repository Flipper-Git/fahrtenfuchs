package com.autokm.app.ui.eingabe

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rundes Drehrad: der Nutzer "dreht" mit dem Finger im Kreis, jede volle Umdrehung entspricht
 * [kmProUmdrehung] km. Meldet bei jeder Fingerbewegung das anteilige Kilometer-Delta über
 * [onDrehung], damit die Anzeige flüssig mitläuft statt nur pro voller Umdrehung zu springen.
 */
@Composable
fun Drehrad(
    kmProUmdrehung: Double = 1.0,
    modifier: Modifier = Modifier,
    onDrehung: (deltaKm: Double) -> Unit,
) {
    var zeigerGrad by remember { mutableFloatStateOf(0f) }
    var letzterWinkel by remember { mutableFloatStateOf(Float.NaN) }

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { letzterWinkel = Float.NaN },
                        onDragCancel = { letzterWinkel = Float.NaN },
                    ) { change, _ ->
                        val mitte = Offset(size.width / 2f, size.height / 2f)
                        val position = change.position
                        val winkelJetzt = winkelGrad(position, mitte)

                        if (!letzterWinkel.isNaN()) {
                            var delta = winkelJetzt - letzterWinkel
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            zeigerGrad += delta
                            onDrehung((delta / 360.0) * kmProUmdrehung)
                        }
                        letzterWinkel = winkelJetzt
                    }
                }
        ) {
            val radius = size.minDimension / 2f
            val mitte = center
            drawCircle(
                color = TerracottaDrehrad,
                radius = radius,
                center = mitte,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f),
            )
            val winkelRad = Math.toRadians(zeigerGrad.toDouble())
            val zeigerEnde = Offset(
                x = mitte.x + (radius - 20f) * cos(winkelRad).toFloat(),
                y = mitte.y + (radius - 20f) * sin(winkelRad).toFloat(),
            )
            drawLine(
                color = TerracottaDrehrad,
                start = mitte,
                end = zeigerEnde,
                strokeWidth = 10f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawCircle(color = TerracottaDrehrad, radius = 14f, center = mitte)
        }
        Text(
            text = "Drehen",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private val TerracottaDrehrad = androidx.compose.ui.graphics.Color(0xFFCC785C)

private fun winkelGrad(position: Offset, mitte: Offset): Float {
    val winkelRad = atan2((position.y - mitte.y).toDouble(), (position.x - mitte.x).toDouble())
    return Math.toDegrees(winkelRad).toFloat()
}
