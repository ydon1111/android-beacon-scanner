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
    accXValues: MutableList<Int?>,
    accYValues: MutableList<Int?>,
    accZValues: MutableList<Int?>
) {
    var accumulatedXData by remember { mutableStateOf(emptyList<Float>()) }
    var accumulatedYData by remember { mutableStateOf(emptyList<Float>()) }
    var accumulatedZData by remember { mutableStateOf(emptyList<Float>()) }
    var currentIndex by remember { mutableStateOf(0) }
    var drawXAxis by remember { mutableStateOf(true) } // X 축 그리기 여부를 나타내는 변수 추가
    var maxDataPoints = 100 // 최대 데이터 포인트 수


    LaunchedEffect(accXValues, accYValues, accZValues) {
        while (true) {
            if (currentIndex < minOf(accXValues.size, accYValues.size, accZValues.size)) {
                val newXData = accXValues[currentIndex]
                val newYData = accYValues[currentIndex]
                val newZData = accZValues[currentIndex]

                // Check for null values and convert them to 0.0 if necessary
                val xData = newXData?.toFloat() ?: 0.0f
                val yData = newYData?.toFloat() ?: 0.0f
                val zData = newZData?.toFloat() ?: 0.0f

                // Add new data at the end of the lists
                accumulatedXData = (accumulatedXData + xData).takeLast(maxDataPoints)
                accumulatedYData = (accumulatedYData + yData).takeLast(maxDataPoints)
                accumulatedZData = (accumulatedZData + zData).takeLast(maxDataPoints)
                currentIndex++

            } else {
                // If all data has been processed, reset the index to start over.
                currentIndex = 0
            }

            // Delay for a specific interval (e.g., 500 milliseconds)
            delay(500)
        }
    }

    Canvas(
        modifier = Modifier.fillMaxSize(),
        onDraw = {
            if (accumulatedXData.isNotEmpty()) { // 데이터가 있는 경우에만 그래프 그리기
                val minYValue = -100f
                val maxYValue = 100f
                val numDataPoints = accumulatedXData.size
                val scaleX = size.width / numDataPoints
                val scaleY = size.height / (maxYValue - minYValue)

                // Draw Y-axis label and ticks
                val yTickInterval = (maxYValue - minYValue) / 10
                val yTickStart = minYValue
                val yTickEnd = maxYValue
                val yTickStep = (yTickEnd - yTickStart) / 10
                for (i in 0..10) {
                    val yTickValue = yTickStart + i * yTickStep
                    val yTickY = size.height - (i * size.height / 10)

                    // Adjust the position of the tick line and text to provide space
                    drawLine(
                        start = Offset(40f, yTickY), // Adjust the x-coordinate for the tick line
                        end = Offset(45f, yTickY),   // Adjust the x-coordinate for the tick line
                        color = Color.Black,
                        strokeWidth = 2f
                    )

                    val text = yTickValue.toString()
                    val textX = 30f   // Adjust the x-coordinate for the text
                    val textY = yTickY + 6 * density

                    val textStyle = TextStyle(
                        fontSize = 12.sp, // Set your desired font size here
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

                // Draw data lines
                val startX = 130f // 시작 위치 조정
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
    startX: Float // 수정된 부분: startX 추가
) {
    if (dataPoints.size < 2) return

    val startY = (dataPoints[0] - minYValue) * scaleY
    var previousX = startX // 시작 위치 설정
    var previousY = startY

    for (i in 1 until dataPoints.size) {
        val x = startX + i.toFloat() * scaleX // x-축 위치 계산
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




