package com.example.android_beacon_scanner.ui

import android.annotation.SuppressLint
import android.os.Build
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
import com.example.android_beacon_scanner.DeviceData
import com.example.android_beacon_scanner.room.DeviceRoomData


@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@Composable
fun ConnectScreen(navController: NavController, bleManager: BleManager) {
    val deviceData = navController.previousBackStackEntry?.savedStateHandle?.get<DeviceRoomData>("deviceData")
    val isConnecting = remember { mutableStateOf(false) }
    val connectedData = remember { mutableStateOf("") }

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
            if (declarationDialogState) {
                InfoDialog() {declarationDialogState = false}
            }
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
            ){
                Icon(
                    imageVector = Icons.TwoTone.Info,
                    tint = Color(0xFF1D8821),
                    contentDescription = "inform"
                )
            }
        }

        ConnectButton(bleManager, isConnecting, deviceData)
        ManufacturerDataView(connectedData)
        val scroll = rememberScrollState(0)
        Text(
            modifier = Modifier
                .padding(top = 5.dp)
                .verticalScroll(scroll),
            text = connectedData.value,
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

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@Composable
fun ConnectButton(
    bleManager: BleManager,
    isConnecting: MutableState<Boolean>,
    deviceData: DeviceRoomData?
) {
    val manufacturerData = byteArrayOf(2, 28, -63, -6, 15, -2, 0, 0, 0, 0, 0, 0, 0, -10, -62, 14, 16, 120, 0, 0, 0, 0, 0, 0, 0, 76, -62, 122, 16, -34, 0, 0, 0, 0, 0, 0, 0, -54, -62, -118, 16, 70, 0, 0, 0, 0, 0, 0, -1, -22, -62, -114, 15, 82, 0, 0, 0, 0, 0, 0, 0, 12, -61, -110, 16, -90, 0, 0, 0, 0, 0, 0, 0, -36, -62, 84, 14, -122, 0, 0, 0, 0, 0, 0, 0, -36, -63, -128, 13, -16, 0, 0, 0, 0, 0, 0, 0, -122, -63, -120, 14, -98, 0, 0, 0, 0, 0, 0, 0, -104, -62, 40, 15, 12, 0, 0, 0, 0, 0, 0, 0, 74, -62, 86, 14, -126, 0, 0, 0, 0, 0, 0, -2, 124, -62, 108, 18, -84, 0, 0, 0, 0, 0, 0, -3, 78, -58, 36, 24, 72, 0, 0, 0, 0, 0, 0, -6, 56, -58, -28, 30, -42, 0, 0, 0, 0, 0, 0, -9, -48, -54, -30, 37, -22, 0, 0, 0, 0, 0, 0, -8, 80, -47, -94, 41, 66, 0, 0, 0, 0, 0, 0, -28, -108, -57, 120, 19, 86, 0, 0, 0, 0, 0, 0, -30, -44, -56, 32, 8, 14, 0, 0, 0, 0, 0, 0, 32, 1)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 5.dp)
    ) {
        Button(
            modifier = Modifier
                .weight(1f)
                .padding(end = 2.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF1D8821)),
            enabled = !isConnecting.value,
            onClick = {
                bleManager.startBleConnectGatt(deviceData?: DeviceRoomData(0, "", "","",manufacturerData))
            }
        ) {
            Text(text = "Connect")
        }
        Button(
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF1D8821)),
            enabled = isConnecting.value,
            onClick = { bleManager.bleGatt!!.disconnect() }
        ) {
            Text(text = "Disconnect")
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@Composable
fun ManufacturerDataView(connectedData: MutableState<String>) {
    Text(
        modifier = Modifier
            .padding(top = 5.dp)
            .verticalScroll(rememberScrollState()),
        text = connectedData.value,
        style = TextStyle(
            fontSize = 14.sp,
        )
    )
}




