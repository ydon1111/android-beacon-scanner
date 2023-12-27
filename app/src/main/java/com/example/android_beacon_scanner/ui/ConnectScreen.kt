package com.example.android_beacon_scanner.ui

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.navigation.NavController
import com.example.android_beacon_scanner.BleInterface
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.room.DeviceRoomData
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@Composable
fun ConnectScreen(navController: NavController, bleManager: BleManager) {
    // Previous device data
    val previousDeviceData = navController.previousBackStackEntry?.savedStateHandle?.get<DeviceRoomData>("deviceData")

    // State to track the currently connected device name
    var connectedDeviceName by remember { mutableStateOf("") }

    // State to track the accumulated manufacturer data
    var accumulatedManufacturerData by remember { mutableStateOf("") }

    val emptyDeviceAddress = ByteArray(6) // Create a byte array of length 6 to represent an empty address
    val defaultDeviceData = DeviceRoomData(0, "", "", "", emptyDeviceAddress)

    // Check if the deviceName matches the initial connection
    val isDeviceNameMatched = connectedDeviceName == (previousDeviceData?.deviceName ?: "")

    bleManager.onConnectedStateObserve(object : BleInterface {
        override fun onConnectedStateObserve(isConnected: Boolean, data: String) {
            if (isConnected) {
                // Check if the connected device name is different from the previous one
                if (!isDeviceNameMatched) {
                    // Disconnect and reconnect to get new data
                    bleManager.bleGatt?.disconnect()
                    bleManager.startBleConnectGatt(previousDeviceData ?: defaultDeviceData)
                }

                // Update the connected device name
                connectedDeviceName = previousDeviceData?.deviceName ?: ""

                // Accumulate manufacturer data
                val manufacturerData = previousDeviceData?.manufacturerData?.joinToString(", ") ?: ""
                accumulatedManufacturerData += "\n$manufacturerData"
            } else {
                // Handle disconnection
            }
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
            if (declarationDialogState) {
                InfoDialog() { declarationDialogState = false }
            }
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = connectedDeviceName,
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

        ManufacturerDataView(connectedDeviceName, accumulatedManufacturerData)

        Log.d("scan data", accumulatedManufacturerData)

        val scroll = rememberScrollState(0)
        Text(
            modifier = Modifier
                .padding(top = 5.dp)
                .verticalScroll(scroll),
            text = accumulatedManufacturerData,
            style = TextStyle(
                fontSize = 14.sp,
            )
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@Composable
fun ManufacturerDataView(deviceName: String, connectedData: String) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.padding(top = 5.dp)
    ) {
        Text(
            text = deviceName,
            style = TextStyle(
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Text(
            modifier = Modifier.verticalScroll(scrollState),
            text = connectedData,
            style = TextStyle(
                fontSize = 14.sp,
            )
        )
    }
}






@Composable
fun InfoDialog(onChangeState: ()-> Unit) {
    Dialog(
        onDismissRequest = onChangeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(50.dp)
                .height(120.dp)
                .background(
                    Color.White,
                    shape = RoundedCornerShape(2.dp)
                ),
            verticalArrangement = Arrangement.SpaceBetween

        ) {
            Text(
                modifier = Modifier.padding(5.dp),
                fontWeight = FontWeight.Bold,
                text =
                "- Service UUID" + "\n" +
                        "      Characteristic UUID"

            )
            Button(
                modifier= Modifier.align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF1D8821)),
                onClick = onChangeState
            ) {
                Text(text = "Close")
            }
        }
    }
}


