package com.example.android_beacon_scanner.ui

import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

        // Display the NRS Chart at the bottom
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight() // Adjust height to content
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for ((rating, info) in nrsData) {
                NrsChartItem(
                    rating = rating,
                    info = info,
                    onRatingChange = onRatingChange,
                    onInfoChange = onInfoChange
                )
                Divider()
            }
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
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            text = "Pain Rating $rating:",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Arrange buttons in two rows
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0..5) { // First row of buttons from 0 to 5
                    Button(
                        onClick = { onRatingChange(i) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        Text(text = "$i", textAlign = TextAlign.Center)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 6..10) { // Second row of buttons from 6 to 10
                    Button(
                        onClick = { onRatingChange(i) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        Text(text = "$i", textAlign = TextAlign.Center)
                    }
                }
            }
        }

        BasicTextField(
            value = info,
            onValueChange = onInfoChange,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    // Handle the Done action of the keyboard
                }
            ),
            textStyle = TextStyle(
                fontSize = 5.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        )
    }
}





