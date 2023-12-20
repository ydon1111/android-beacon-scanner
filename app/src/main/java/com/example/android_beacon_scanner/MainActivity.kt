package com.example.android_beacon_scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_beacon_scanner.ui.theme.AndroidbeaconscannerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.altbeacon.beacon.Beacon

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidbeaconscannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BeaconScanner()
                }
            }
        }
    }
}


@Composable
fun BeaconScanner() {
    // BLE 스캔 결과를 저장할 StateFlow
    val scannedDevices: MutableStateFlow<List<BluetoothDevice>> = remember { MutableStateFlow(emptyList()) }

    // BluetoothAdapter 초기화
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }

    // 스캔 콜백 초기화
    val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val updatedList = scannedDevices.value.toMutableList()
            updatedList.add(result.device)
            scannedDevices.value = updatedList
        }
    }

    // 스캔 시작 함수
    fun startScan() {
        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth가 비활성화된 경우 사용자에게 활성화를 요청할 수 있습니다.
            // 여기에서 요청 로직을 추가할 수 있습니다.
        } else {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            scanner?.startScan(scanCallback)
        }
    }

    // 스캔 중지 함수
    fun stopScan() {
        val scanner = bluetoothAdapter.bluetoothLeScanner
        scanner?.stopScan(scanCallback)
    }

    // 화면 구성
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text("BLE Scanner", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // 스캔 시작 버튼
        Button(
            onClick = { startScan() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Start Scan")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 스캔 중지 버튼
        Button(
            onClick = { stopScan() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Stop Scan")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 스캔된 장치 목록 표시
        LazyColumn {
            items(scannedDevices.value) { device ->
                Text(text = device.name ?: "Unknown Device")
            }
        }
    }

    // 스캔 결과 수신을 위한 라이프사이클 스코프 설정
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner) {
        val job = coroutineScope.launch {
            scannedDevices.collect {
                // 스캔 결과가 업데이트될 때마다 처리할 내용을 추가할 수 있습니다.
            }
        }
        onDispose {
            job.cancel()
        }
    }
}