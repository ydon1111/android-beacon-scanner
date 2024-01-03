package com.example.android_beacon_scanner.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.android_beacon_scanner.BleInterface
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomData

import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@SuppressLint("MissingPermission")
@Composable
fun ConnectScreen(
    navController: NavHostController,
    bleManager: BleManager,
    deviceDataRepository: DeviceDataRepository
) {
    val deviceData = navController.previousBackStackEntry?.savedStateHandle?.get<DeviceRoomDataEntity>("deviceData")
    val isConnecting = remember { mutableStateOf(false) }
    val connectedData = remember { mutableStateOf("") }
    var manufacturerDataList by remember { mutableStateOf<List<ByteArray?>>(emptyList()) }
    val allDeviceDataState = deviceDataRepository.allDeviceRoomData.collectAsState(emptyList())

    val temperatureList = remember { mutableStateListOf<String>() }

    LaunchedEffect(deviceData?.deviceName) {
        deviceData?.deviceName?.let { deviceName ->
            val data = withContext(Dispatchers.IO) {
                deviceDataRepository.getDeviceData(deviceName)
            }
            if (data != null) {
                manufacturerDataList = allDeviceDataState.value
                    .filter { it.deviceName == deviceName }
                    .mapNotNull { it.manufacturerData }

                // You can add code to update the temperatureList
                val temperatureData = allDeviceDataState.value
                    .filter { it.deviceName == deviceName }
                    .mapNotNull { it.temperature }
                temperatureList.clear()
                temperatureList.addAll(temperatureData.map { it.toString() })
            }
        }
    }

    // Use a LaunchedEffect to collect data from the database and update manufacturerDataList
    LaunchedEffect(allDeviceDataState.value) {
        manufacturerDataList = allDeviceDataState.value
            .filter { it.deviceName == deviceData?.deviceName }
            .mapNotNull { it.manufacturerData }
    }

    LaunchedEffect(allDeviceDataState.value) {
        manufacturerDataList = allDeviceDataState.value
            .filter { it.deviceName == deviceData?.deviceName }
            .mapNotNull { it.manufacturerData }
    }

    bleManager.onConnectedStateObserve(object : BleInterface {
        override fun onConnectedStateObserve(isConnected: Boolean, data: String) {
            isConnecting.value = isConnected
            connectedData.value = connectedData.value + "\n" + data
        }
    })

    var declarationDialogState by remember {
        mutableStateOf(false)
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
            IconButton(
                onClick = {
                    declarationDialogState = true
                },
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Info,
                    tint = Color(0xFF1D8821),
                    contentDescription = "inform"
                )
            }
        }

        val scroll = rememberScrollState(0)
        Column(
            modifier = Modifier
                .padding(top = 5.dp)
                .verticalScroll(scroll)
        ) {

            Text(
                text = connectedData.value,
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            // Room의 Flow를 사용하여 데이터를 표시
            manufacturerDataList.forEachIndexed { index, manufacturerData ->
                Text(
                    text = "Device Data $index: ${manufacturerData?.contentToString()}"
                )
            }

            Log.d("TemperatureData", temperatureList.toString())

            // Display temperature data
            temperatureList.forEachIndexed { index, temperature ->
                Text(
                    text = "Temperature Data $index: $temperature",
                    style = TextStyle(
                        fontSize = 14.sp,
                    )
                )
            }
        }
    }
}




