package com.example.android_beacon_scanner.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.R
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import com.example.android_beacon_scanner.ui.theme.ScanItemTypography


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScanScreen(
    navController: NavHostController,
    bleManager: BleManager) {

    val scanList = remember { mutableStateListOf<DeviceRoomDataEntity>() }
    val isScanning = remember { mutableStateOf(false) }
    val context = LocalContext.current

    bleManager.setScanList(scanList)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ScanButton(context,bleManager, isScanning)
        ScanList(navController, bleManager, scanList)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
@SuppressLint("MissingPermission")
fun ScanButton(
    context: Context,
    bleManager: BleManager,
    isScanning: MutableState<Boolean>
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // todo : 권한 결과 처리
    }

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 5.dp),
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.buttonColors(Color(0xFF1D8821)),
        onClick = {
            if(!isScanning.value) {
                if (checkPermission(context)) {
                    bleManager.startBleScan()
                } else {
                    launcher.launch(permissionArray)
                }
            } else {
                bleManager.stopBleScan()
            }
            isScanning.value =!isScanning.value
        }
    ) {
        Text(
            text = if (!isScanning.value) {
                stringResource(id = R.string.scan)
            } else {
                stringResource(id = R.string.stop)
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScanList(
    navController: NavHostController,
    bleManager: BleManager,
    scanList: SnapshotStateList<DeviceRoomDataEntity>
) {

    val uniqueDeviceNames = scanList.distinctBy { it.deviceName }
    val visibleDevices = rememberUpdatedState(uniqueDeviceNames) // Remember the list of visible devices

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        items(uniqueDeviceNames) { topic->
            ScanItem(navController, topic)
        }
    }

    // Check for devices that are no longer visible and remove them from scanList
    LaunchedEffect(visibleDevices.value) {
        val visibleDeviceNames = visibleDevices.value.map { it.deviceName }
        val removedDevices = scanList.filterNot { it.deviceName in visibleDeviceNames }
        scanList.removeAll(removedDevices)
    }


}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanItem(
    navController: NavHostController,
    deviceData: DeviceRoomDataEntity,
) {
    var expanded by remember { mutableStateOf(false) }
    val deviceDataState = rememberUpdatedState(deviceData) // Remember the deviceData

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF569097)
        ),
        modifier = Modifier.padding(vertical = 4.dp),
        onClick = {
            navController.currentBackStackEntry?.savedStateHandle?.set(key = "deviceData", value = deviceDataState.value)
            navController.navigate("ConnectScreen")
        }
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp)
            .padding(start = 2.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = deviceData.deviceName)

                if(expanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Temperature\n>> ${deviceData.temperature ?: "N/A"}", // 온도가 null인 경우 "N/A"로 표시
                        style = ScanItemTypography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Address\n>> ${deviceData.deviceAddress}", style = ScanItemTypography.bodySmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "data\n>> ${deviceData.manufacturerData}", style = ScanItemTypography.bodySmall)
                }
            }

            IconButton(onClick = { expanded=!expanded }) {
                Icon(
                    imageVector = if(expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if(expanded){
                        stringResource(id = R.string.show_less)
                    } else {
                        stringResource(id = R.string.show_more)
                    })
            }
        }
    }
}



fun checkPermission(context: Context): Boolean {
    val permissionArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    if(Build.VERSION.SDK_INT >= 31){
        // 블루투스와 카메라 권한이 허용되었는지 체크
        return permissionArray.all{ ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED}
    }
    return true
}

private val permissionArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE, // Add Bluetooth Advertise permission
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE, // Add WRITE_EXTERNAL_STORAGE permission

    )
} else {
    arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE // Add WRITE_EXTERNAL_STORAGE permission
    )
}
