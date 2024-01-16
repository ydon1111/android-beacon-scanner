package com.example.android_beacon_scanner.ui

import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.launch
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@SuppressLint("MissingPermission")
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
        val latestDataFlow =
            deviceDataRepository.observeLatestDeviceData(deviceData?.deviceName ?: "")
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

    // Create a coroutine scope
    val coroutineScope = rememberCoroutineScope()

    var showToast by remember { mutableStateOf(false) }

    fun exportDataToCsv(dataToExport: List<DeviceRoomDataEntity>) {
        val currentTime =
            SimpleDateFormat("yyyy_MM_dd_HH.mm.ss", Locale.getDefault()).format(Date())
        val filePath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/$currentTime.csv"

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

    var painRating by remember { mutableIntStateOf(0) }
    val additionalInfo by remember { mutableStateOf("") }

    // NRS 차트 데이터 관련 상태 변수
    var nrsData by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }

    // NRS 차트 데이터 초기화 함수
    fun initializeNrsData() {
        nrsData = List(11) { index -> Pair(index, "") }
    }

    // NRS 차트 데이터 업데이트 함수
    fun updateNrsData() {
        nrsData = nrsData.map { (rating, _) ->
            if (rating == painRating) {
                Pair(rating, additionalInfo)
            } else {
                nrsData[rating]
            }
        }
    }

    // NRS 차트 데이터 초기화
    LaunchedEffect(Unit) {
        initializeNrsData()
    }

    // Display a Toast message when the file is saved
    if (showToast) {
        Toast.makeText(LocalContext.current, "저장 되었습니다.", Toast.LENGTH_SHORT).show()
        showToast = false // Reset the flag
    }

    val scroll = rememberScrollState(0)

    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // Device Name 및 CSV 다운로드 버튼
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
                        val dataToExport =
                            deviceDataRepository.getDeviceDataWithBleCountGreaterOrEqual(
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
                    .padding(16.dp)
            ) {
                Text(text = "CSV 다운로드")
            }
        }

        // Latest 데이터
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

        // NRS 차트
        Text(
            text = "NRS Chart",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )

        // Display the NRS Chart vertically
        NrsChart(
            nrsData = nrsData,
            onRatingChange = { newRating ->
                painRating = newRating
                updateNrsData()
            },
            onInfoChange = { newInfo ->
                // 여기에 정보 업데이트 로직 추가
            }
        )
    }
}

@Composable
fun NrsChart(
    nrsData: List<Pair<Int, String>>,
    onRatingChange: (Int) -> Unit,
    onInfoChange: (String) -> Unit
) {
    // Using a vertical column to display the NRS chart items vertically
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Ensure that we have data for 0 to 11
        (0..11).forEach { i ->
            NrsChartItem(
                rating = i,
                info = nrsData.getOrNull(i)?.second ?: "",
                onRatingChange = onRatingChange,
                onInfoChange = onInfoChange
            )
        }
    }
}

@Composable
fun NrsChartItem(
    rating: Int,
    info: String,
    onRatingChange: (Int) -> Unit,
    onInfoChange: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp) // 패딩을 조절할 수 있습니다.
    ) {
        Button(
            onClick = { onRatingChange(rating) },
            modifier = Modifier
                .fillMaxWidth()
                .width(40.dp) // 버튼의 너비를 조절할 수 있습니다.
                .height(40.dp) // 버튼의 높이를 조절할 수 있습니다.
        ) {
            Text(
                text = "$rating",
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontSize = 16.sp) // 텍스트 크기를 조절할 수 있습니다.
            )
        }
    }
}





