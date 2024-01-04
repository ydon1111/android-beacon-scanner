package com.example.android_beacon_scanner.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.NavHostController
import com.example.android_beacon_scanner.BleInterface
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.room.DeviceDataRepository

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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

    // Collect the data based on the deviceName
    val allDeviceData = deviceDataRepository.getDeviceDataFlow(deviceData?.deviceName ?: "")
        .collectAsState(emptyList()).value


    // Access the values you want from the list of DeviceRoomDataEntity
    val temperatureList = mutableListOf<String>()
    val bleDataCountList = mutableListOf<String>()
    val currentDateAndTimeList = mutableListOf<String>()
    val accXValuesList = mutableListOf<String>()
    val accYValuesList = mutableListOf<String>()
    val accZValuesList = mutableListOf<String>()


    allDeviceData.forEach { data ->
        val temperature = data.temperature
        val bleDataCount = data.bleDataCount
        val currentDateAndTime = data.currentDateAndTime
        val accXValues = data.accXValues
        val accYValues = data.accYValues
        val accZValues = data.accZValues

        // Add the values to their respective lists
        temperatureList.add(temperature.toString())
        bleDataCountList.add(bleDataCount.toString())
        currentDateAndTimeList.add(currentDateAndTime.toString())
        accXValuesList.add(accXValues.toString())
        accYValuesList.add(accYValues.toString())
        accZValuesList.add(accZValues.toString())

//        // You can use these values to update your UI or perform other actions
//        Log.d("ConnectScreen", "Temperature: $temperature, BLE Data Count: $bleDataCount")
//        Log.d("ConnectScreen", "Date and Time: $currentDateAndTime")
//        Log.d("ConnectScreen", "ACC_X Values: $accXValues")
//        Log.d("ConnectScreen", "ACC_Y Values: $accYValues")
//        Log.d("ConnectScreen", "ACC_Z Values: $accZValues")
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

        // Access the most recent values from the lists
        val latestTemperature = temperatureList.lastOrNull() ?: "N/A"
        val latestBleDataCount = bleDataCountList.lastOrNull() ?: "N/A"
        val latestCurrentDateAndTime = currentDateAndTimeList.lastOrNull() ?: "N/A"
        val latestAccXValues = accXValuesList.lastOrNull() ?: "N/A"
        val latestAccYValues = accYValuesList.lastOrNull() ?: "N/A"
        val latestAccZValues = accZValuesList.lastOrNull() ?: "N/A"


        Column(
            modifier = Modifier
                .padding(top = 5.dp)
                .verticalScroll(scroll)
        ) {
            // Access and display temperature
// Display the most recent values in your UI
            Text(
                text = "Latest Temperature: $latestTemperature",
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            Text(
                text = "Latest BLE Data Count: $latestBleDataCount",
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            Text(
                text = "Latest Date and Time: $latestCurrentDateAndTime",
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            Text(
                text = "Latest ACC_X Values: $latestAccXValues",
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            Text(
                text = "Latest ACC_Y Values: $latestAccYValues",
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            Text(
                text = "Latest ACC_Z Values: $latestAccZValues",
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )

            Text(
                text = connectedData.value,
                style = TextStyle(
                    fontSize = 14.sp,
                )
            )
        }
    }
}




