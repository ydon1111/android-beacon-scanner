package com.example.android_beacon_scanner.ui

import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LineChartGraph(
    accXValues: List<Float>,
    accYValues: List<Float>,
    accZValues: List<Float>
) {
    val lineChart = LineChart(LocalContext.current)

    Log.d("LineChartGraph", "accXValues: $accXValues")
    Log.d("LineChartGraph", "accYValues: $accYValues")
    Log.d("LineChartGraph", "accZValues: $accZValues")

    // Create LineDataSet for each set of values
    val dataSetX = createLineDataSet(accXValues, "ACC_X Values", Color.RED)
    val dataSetY = createLineDataSet(accYValues, "ACC_Y Values", Color.GREEN)
    val dataSetZ = createLineDataSet(accZValues, "ACC_Z Values", Color.BLUE)

    Log.d("LineChartGraph", "dataSetX entries: ${dataSetX.entryCount}")
    Log.d("LineChartGraph", "dataSetY entries: ${dataSetY.entryCount}")
    Log.d("LineChartGraph", "dataSetZ entries: ${dataSetZ.entryCount}")

    Log.d("LineChartGraph", "dataSetX: $dataSetX")
    Log.d("LineChartGraph", "dataSetY: $dataSetY")
    Log.d("LineChartGraph", "dataSetZ: $dataSetZ")

    // Combine LineDataSets into LineData
    val lineDataSets: List<ILineDataSet> = listOf(dataSetX, dataSetY, dataSetZ)
    val lineData = LineData(lineDataSets)

    // Customize chart appearance
    lineChart.data = lineData
    lineChart.description.isEnabled = false
    lineChart.setTouchEnabled(true)
    lineChart.setPinchZoom(true)

    // Customize X-axis and Y-axis
    val xAxis: XAxis = lineChart.xAxis
    val yAxisLeft: YAxis = lineChart.axisLeft
    val yAxisRight: YAxis = lineChart.axisRight

    xAxis.position = XAxis.XAxisPosition.BOTTOM
    yAxisLeft.setDrawGridLines(false)
    yAxisRight.isEnabled = false

    // Add chart to the Composable
    AndroidView(
        { lineChart },
        modifier = Modifier.fillMaxSize().size(400.dp, 400.dp) // Adjust the size as needed
    )
}

private fun createLineDataSet(values: List<Float>, label: String, color: Int): LineDataSet {
    Log.d("createLineDataSet", "Creating dataset for: $label")
    Log.d("createLineDataSet", "Values: $values")
    val entries = values.mapIndexed { index, value ->
        Log.d("createLineDataSet", "Entry $index: x=$index, y=$value")
        Entry(index.toFloat(), value)
    }

    val dataSet = LineDataSet(entries, label)
    dataSet.color = color
    dataSet.setCircleColor(color)
    dataSet.setDrawCircleHole(false)
    dataSet.setDrawCircles(false)
    dataSet.setDrawValues(false)
    dataSet.setDrawFilled(true)

    return dataSet
}
