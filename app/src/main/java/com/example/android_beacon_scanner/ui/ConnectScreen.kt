package com.example.android_beacon_scanner.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import com.example.android_beacon_scanner.room.DeviceDataRepository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.compose.rememberNavController
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@Composable
fun ConnectScreen(
    navController: NavHostController,
    deviceDataRepository: DeviceDataRepository,
    bleManager: BleManager,
) {

    var latestDeviceData by remember {
        mutableStateOf<DeviceRoomDataEntity?>(null)
    }

    // 이전의 통증 점수를 저장하는 변수 추가
    var latestPainScore by remember { mutableStateOf<Int?>(null) }

    val deviceData =
        navController.previousBackStackEntry?.savedStateHandle?.get<DeviceRoomDataEntity>("deviceData")

    LaunchedEffect(deviceData?.deviceName) {
        Log.d("ConnectScreen", "LaunchedEffect started")
        val latestDataFlow =
            deviceDataRepository.observeLatestDeviceData(deviceData?.deviceName ?: "")
        latestDataFlow.collect { updatedDeviceData ->
//            Log.d("ConnectScreen", "Latest data received: $updatedDeviceData")
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

    // Create a coroutine scope
    val coroutineScope = rememberCoroutineScope()

    var showToast by remember { mutableStateOf(false) }

    var dataCount by remember { mutableStateOf(0) }

    // CSV 다운로드 버튼을 눌렀을 때 호출되는 함수
    fun exportDataToCsv(dataToExport: List<DeviceRoomDataEntity>) {
        val currentTime =
            SimpleDateFormat("yyyy_MM_dd_HH.mm.ss", Locale.getDefault()).format(Date())

        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dirPath = "$downloadsDir/서울아산병원"

        val dir = File(dirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        // 파일 경로 생성
        val filePath = "$dirPath/$currentTime.csv"
        // Open a file writer
        val fileWriter = FileWriter(filePath)

        // Write the CSV header (column names)
        fileWriter.append("Temperature, BLE Data Count, Date and Time, AccX,AccY,AccZ,Rating\n")

        // Write each data row to the CSV file
        for (i in 0 until dataCount) { // 0부터 dataCount까지의 데이터만을 가져와서 CSV로 내보냅니다.
            val data = dataToExport[i]
            fileWriter.append("${data.temperature}, ${data.bleDataCount}, ${data.currentDateAndTime}, ${data.valueX}, ${data.valueY}, ${data.valueZ},${data.rating}\n")
        }

        // Close the file writer
        fileWriter.close()

        // Set the showToast flag to true
        showToast = true
    }

    var painRating by remember { mutableStateOf<Int?>(null) }

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
                Pair(rating, "")
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

    // NRSChartItem에서 사용할 latestDeviceData를 정의하고 전달
    val latestDeviceDataForNrsChartItem = latestDeviceData

    // Remember the NavController
    val rememberNavController = rememberNavController()

    // State variable to control the confirmation dialog
    var showCancelConfirmationDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // Device Name
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "서울아산병원 보조기",
            style = TextStyle(
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        // CSV 다운로드 및 데이터 수집 버튼
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    // When the button is pressed, fetch all data from 0 to the current count
                    coroutineScope.launch {
                        val dataToExport =
                            deviceDataRepository.getDeviceDataWithBleCountGreaterOrEqual(
                                deviceData?.deviceName ?: "",
                                dataCount
                            )

                        // Update dataCount with the current data size
                        dataCount = dataToExport.size

                        // Export data to CSV file
                        if (dataToExport.isNotEmpty()) {
                            exportDataToCsv(dataToExport)
                        }
                    }
                },
                modifier = Modifier
                    .padding(4.dp)
            ) {
                Text(text = "CSV 다운로드")
            }
            Button(
                onClick = {
                    // 버튼을 클릭하면 BLE 스캔을 취소합니다.
                    bleManager.stopBleScan()
                    showCancelConfirmationDialog = true
                },
                modifier = Modifier
                    .padding(4.dp)
            ) {
                Text(text = "데이터 수집 취소")
            }
        }


        // Latest 데이터
        if (latestDeviceData != null) {
            Text(
                text = "최근 측정 온도: ${latestDeviceData!!.temperature}",
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            // Latest Pain Score (이전 값이 null이 아니면 이전 값 사용)
            if (latestDeviceData!!.rating != null) {
                painRating = latestDeviceData!!.rating
            }
            Text(
                text = "최근 측정 통증 정도: $latestPainScore",
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            Text(
                text = "데이터 수집 횟수: ${latestDeviceData!!.bleDataCount}",
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            Text(
                text = "최근 데이터 수집 시간: ${latestDeviceData!!.currentDateAndTime}",
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            latestPainScore = painRating
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
            latestDeviceData = latestDeviceDataForNrsChartItem, // latestDeviceData 전달
            onRatingChange = { newRating ->
                painRating = newRating
                updateNrsData()
            },
            coroutineScope = coroutineScope,
            deviceDataRepository = deviceDataRepository,
        )
    }

    // Add a Spacer to create some space between the content above and the buttons below
    Spacer(modifier = Modifier.height(16.dp))

    // Confirmation dialog for canceling data collection
    ConfirmationDialog(
        showDialog = showCancelConfirmationDialog,
        onConfirm = {
            // User confirmed, navigate to ScanScreen
            navController.navigate("ScanScreen") // Adjust the destination route as needed
        },
        onDismiss = {
            // Dialog dismissed or canceled
            showCancelConfirmationDialog = false
        }
    )
}

@Composable
fun ConfirmationDialog(
    showDialog: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = {
                Text(text = "데이터 수집 취소")
            },
            text = {
                Text(text = "정말로 데이터 수집을 취소하시겠습니까?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    }
                ) {
                    Text(text = "확인")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        onDismiss()
                    }
                ) {
                    Text(text = "취소")
                }
            }
        )
    }
}

@Composable
fun NrsChart(
    latestDeviceData: DeviceRoomDataEntity?,
    onRatingChange: (Int) -> Unit,
    coroutineScope: CoroutineScope,
    deviceDataRepository: DeviceDataRepository,
) {
    // Using a vertical column to display the NRS chart items vertically
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Ensure that we have data for 0 to 11
        (0..10).forEach { i ->
            NrsChartItem(
                rating = i,
                latestDeviceData = latestDeviceData, // latestDeviceData를 전달
                onRatingChange = onRatingChange,
                coroutineScope = coroutineScope,
                deviceDataRepository = deviceDataRepository// coroutineScope를 전달
            )
        }
    }
}

@Composable
fun NrsChartItem(
    rating: Int,
    latestDeviceData: DeviceRoomDataEntity?,
    onRatingChange: (Int) -> Unit,
    coroutineScope: CoroutineScope,
    deviceDataRepository: DeviceDataRepository,

    ) {
    val showDialog = remember { mutableStateOf(false) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth() // 수평으로 전체 너비를 차지하도록 합니다.
            .padding(4.dp) // 패딩을 조절할 수 있습니다.
    ) {
        Button(
            onClick = {
                // 사용자에게 확인 팝업을 보여줍니다.
                showDialog.value = true
            },
            modifier = Modifier
                .height(screenHeight * 0.048f)
                .fillMaxWidth(0.5f) // 버튼 너비도 조정합니다.
        ) {
            Text(
                text = "$rating",
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontSize = 16.sp) // 텍스트 크기를 조절할 수 있습니다.
            )
        }

        // 통증 강도 설명 텍스트 추가
        Text(
            text = when (rating) {
                0 -> "무 통증"
                1 -> "약간 통증"
                2 -> "약간 이상의 통증"
                3 -> "보통 통증"
                4 -> "보통 이상의 통증"
                5 -> "중간 통증"
                6 -> "중간 이상의 통증"
                7 -> "강한 통증"
                8 -> "강한 이상의 통증"
                9 -> "매우 강한 통증"
                10 -> "극심한 통증"
                else -> ""
            },
            textAlign = TextAlign.Center,
            style = TextStyle(fontSize = 16.sp) // 텍스트 크기를 조절할 수 있습니다.
        )

        // 확인 팝업 창
        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                title = {
                    Text(text = "$rating 등급 확인")
                },
                text = {
                    Text(text = "이 등급을 선택 하시겠습니까?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // 사용자가 확인 버튼을 누르면 업데이트
                            onRatingChange(rating)
                            showDialog.value = false

                            // Room 데이터베이스 업데이트
                            latestDeviceData?.let { deviceData ->
                                coroutineScope.launch {
                                    deviceDataRepository.updateRating(deviceData.deviceName, rating)
                                }
                            }
                        }
                    ) {
                        Text(text = "확인")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showDialog.value = false
                        }
                    ) {
                        Text(text = "취소")
                    }
                }
            )
        }
    }
}





