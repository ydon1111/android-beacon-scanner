package com.example.android_beacon_scanner.ui

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.android_beacon_scanner.BleInterface
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.room.DeviceDataRepository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.android_beacon_scanner.R
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.launch
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@SuppressLint("MissingPermission", "MutableCollectionMutableState")
@Composable
fun ConnectScreen(
    navController: NavHostController,
    bleManager: BleManager,
    deviceDataRepository: DeviceDataRepository,
) {
    val deviceData =
        navController.previousBackStackEntry?.savedStateHandle?.get<DeviceRoomDataEntity>("deviceData")
    val isConnecting = remember { mutableStateOf(false) }
    val connectedData = remember { mutableStateOf("") }

    var latestDeviceData by remember {
        mutableStateOf<DeviceRoomDataEntity?>(null)
    }

    LaunchedEffect(deviceData?.deviceName) {
        Log.d("ConnectScreen", "LaunchedEffect started")
        val latestDataFlow = deviceDataRepository.observeLatestDeviceData(deviceData?.deviceName ?: "")
        latestDataFlow.collect { updatedDeviceData ->
            Log.d("ConnectScreen", "Latest data received: $updatedDeviceData")
            latestDeviceData = updatedDeviceData
        }
    }

    // Declare mutable state lists for accelerometer data
    val updatedAccXValues = remember { mutableStateListOf<Int?>() }
    val updatedAccYValues = remember { mutableStateListOf<Int?>() }
    val updatedAccZValues = remember { mutableStateListOf<Int?>() }

    if (latestDeviceData != null) {
        // Collect accX, accY, and accZ values from the latest data
        updatedAccXValues.addAll(listOf(latestDeviceData!!.valueX))
        updatedAccYValues.addAll(listOf(latestDeviceData!!.valueY))
        updatedAccZValues.addAll(listOf(latestDeviceData!!.valueZ))
    }

    bleManager.onConnectedStateObserve(object : BleInterface {
        override fun onConnectedStateObserve(isConnected: Boolean, data: String) {
            isConnecting.value = isConnected
            connectedData.value = connectedData.value + "\n" + data
        }
    })

    var startBleCount by remember { mutableStateOf(0) }

    // Create a coroutine scope
    val coroutineScope = rememberCoroutineScope()

    var showToast by remember { mutableStateOf(false) }

    fun exportDataToCsv(dataToExport: List<DeviceRoomDataEntity>) {
        val currentTime = SimpleDateFormat("yyyy_MM_dd_HH.mm.ss", Locale.getDefault()).format(Date())
        val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/$currentTime.csv"

        // Open a file writer
        val fileWriter = FileWriter(filePath)

        // Write the CSV header (column names)
        fileWriter.append("Temperature, BLE Data Count, Date and Time, AccX,AccY,AccZ\n")

        // Write each data row to the CSV file
        for (data in dataToExport) {
            fileWriter.append("${data.temperature}, ${data.bleDataCount}, ${data.currentDateAndTime}, ${data.valueX}, ${data.valueY}, ${data.valueZ}\n")
        }

        // Close the file writer
        fileWriter.close()

        // Set the showToast flag to true
        showToast = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = deviceData?.deviceName ?: "Null",
                style = TextStyle(
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Button(
                onClick = {
                    // When the button is pressed, fetch all data from 0 to the current count
                    coroutineScope.launch {
                        val dataToExport = deviceDataRepository.getDeviceDataWithBleCountGreaterOrEqual(
                            deviceData?.deviceName ?: "",
                            0 // Start from 0
                        )

                        // Export data to CSV file
                        if (dataToExport.isNotEmpty()) {
                            exportDataToCsv(dataToExport)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "Save Data to CSV")
            }
        }

        // Display a Toast message when the file is saved
        if (showToast) {
            Toast.makeText(LocalContext.current, "저장 되었습니다.", Toast.LENGTH_SHORT).show()
            showToast = false // Reset the flag
        }

        val scroll = rememberScrollState(0)

        Column(
            modifier = Modifier
                .padding(top = 5.dp)
                .verticalScroll(scroll)
        ) {
            // Display the latest data
            if (latestDeviceData != null) {
                Text(
                    text = "Latest Temperature: ${latestDeviceData!!.temperature}",
                    style = TextStyle(
                        fontSize = 14.sp,
                    )
                )

                Text(
                    text = "Latest BLE Data Count: ${latestDeviceData!!.bleDataCount}",
                    style = TextStyle(
                        fontSize = 14.sp,
                    )
                )

                Text(
                    text = "Latest Date and Time: ${latestDeviceData!!.currentDateAndTime}",
                    style = TextStyle(
                        fontSize = 14.sp,
                    )
                )
            }

            Text(
                text = connectedData.value,
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )
        }

        // Display the LineChartGraph with the latest accelerometer data
        if (latestDeviceData != null) {
            LineChartGraph(
                listOf(latestDeviceData!!.valueX!!.toFloat()),
                listOf(latestDeviceData!!.valueY!!.toFloat()),
                listOf(latestDeviceData!!.valueZ!!.toFloat())
            )
        }
    }
}




