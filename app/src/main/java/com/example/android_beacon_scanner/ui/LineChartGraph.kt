package com.example.android_beacon_scanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@Composable
fun LineChartGraph(
    accXValues: List<Any>,
    accYValues: List<Any>,
    accZValues: List<Any>
) {
    var accumulatedXData by remember { mutableStateOf(emptyList<Float>()) }
    var accumulatedYData by remember { mutableStateOf(emptyList<Float>()) }
    var accumulatedZData by remember { mutableStateOf(emptyList<Float>()) }
    var currentIndex by remember { mutableStateOf(0) }
    var drawXAxis by remember { mutableStateOf(true) }
    var maxDataPoints = 100

    LaunchedEffect(accXValues, accYValues, accZValues) {
        while (true) {
            if (currentIndex < minOf(accXValues.size, accYValues.size, accZValues.size)) {
                val newXData = accXValues[currentIndex]
                val newYData = accYValues[currentIndex]
                val newZData = accZValues[currentIndex]

                val xData = newXData?.toString()?.toFloatOrNull() ?: 0.0f
                val yData = newYData?.toString()?.toFloatOrNull() ?: 0.0f
                val zData = newZData?.toString()?.toFloatOrNull() ?: 0.0f

                accumulatedXData = (accumulatedXData + xData).takeLast(maxDataPoints)
                accumulatedYData = (accumulatedYData + yData).takeLast(maxDataPoints)
                accumulatedZData = (accumulatedZData + zData).takeLast(maxDataPoints)
                currentIndex++
            } else {
                currentIndex = 0
            }

            delay(500)
        }
    }

    Canvas(
        modifier = Modifier.fillMaxSize(),
        onDraw = {
            if (accumulatedXData.isNotEmpty()) {
                val minYValue = -100f
                val maxYValue = 100f
                val numDataPoints = accumulatedXData.size
                val scaleX = size.width / numDataPoints
                val scaleY = size.height / (maxYValue - minYValue)

                val yTickInterval = (maxYValue - minYValue) / 10
                val yTickStart = minYValue
                val yTickEnd = maxYValue
                val yTickStep = (yTickEnd - yTickStart) / 10
                for (i in 0..10) {
                    val yTickValue = yTickStart + i * yTickStep
                    val yTickY = size.height - (i * size.height / 10)

                    drawLine(
                        start = Offset(40f, yTickY),
                        end = Offset(45f, yTickY),
                        color = Color.Black,
                        strokeWidth = 2f
                    )

                    val text = yTickValue.toString()
                    val textX = 30f
                    val textY = yTickY + 6 * density

                    val textStyle = TextStyle(
                        fontSize = 12.sp,
                        color = Color.Black
                    )

                    drawIntoCanvas { canvas ->
                        val paint = Paint().asFrameworkPaint()
                        paint.textSize = 12 * density
                        paint.color = Color.Black.toArgb()
                        paint.isAntiAlias = true
                        canvas.nativeCanvas.drawText(text, textX, textY, paint)
                    }
                }

                val startX = 130f
                drawLineGraph(accumulatedXData, scaleX, scaleY, minYValue, Color.Red, startX)
                drawLineGraph(accumulatedYData, scaleX, scaleY, minYValue, Color.Green, startX)
                drawLineGraph(accumulatedZData, scaleX, scaleY, minYValue, Color.Blue, startX)
            }
        }
    )
}

private fun DrawScope.drawLineGraph(
    dataPoints: List<Float>,
    scaleX: Float,
    scaleY: Float,
    minYValue: Float,
    color: Color,
    startX: Float
) {
    if (dataPoints.size < 2) return

    val startY = (dataPoints[0] - minYValue) * scaleY
    var previousX = startX
    var previousY = startY

    for (i in 1 until dataPoints.size) {
        val x = startX + i.toFloat() * scaleX
        val y = (dataPoints[i] - minYValue) * scaleY

        drawLine(
            start = Offset(previousX, previousY),
            end = Offset(x, y),
            color = color,
            strokeWidth = 4f
        )

        previousX = x
        previousY = y
    }
}

