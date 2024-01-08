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
    accXValues: List<Int?>,
    accYValues: List<Int?>,
    accZValues: List<Int?>
) {
    var accumulatedXData by remember { mutableStateOf(emptyList<Float>()) }
    var accumulatedYData by remember { mutableStateOf(emptyList<Float>()) }
    var accumulatedZData by remember { mutableStateOf(emptyList<Float>()) }
    var currentIndex by remember { mutableStateOf(0) }
    var drawXAxis by remember { mutableStateOf(true) }
    var maxDataPoints = 100

    LaunchedEffect(accXValues, accYValues, accZValues) { // Wrap the entire Canvas in a LaunchedEffect
        while (true) {
            if (currentIndex < minOf(accXValues.size, accYValues.size, accZValues.size)) {
                val newXData = accXValues[currentIndex]
                val newYData = accYValues[currentIndex]
                val newZData = accZValues[currentIndex]

                val xData = newXData?.toFloat() ?: 0.0f
                val yData = newYData?.toFloat() ?: 0.0f
                val zData = newZData?.toFloat() ?: 0.0f

                accumulatedXData = (accumulatedXData + xData).takeLast(maxDataPoints)
                accumulatedYData = (accumulatedYData + yData).takeLast(maxDataPoints)
                accumulatedZData = (accumulatedZData + zData).takeLast(maxDataPoints)
                currentIndex++

            } else {
                currentIndex = 0
            }

            // Delay for a specific interval (e.g., 500 milliseconds)
            delay(100)
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



