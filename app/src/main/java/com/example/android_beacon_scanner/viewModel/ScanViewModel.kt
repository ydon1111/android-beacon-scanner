package com.example.android_beacon_scanner.viewModel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import com.example.android_beacon_scanner.ui.checkPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScanViewModel(
    private val bleManager: BleManager,
    private val deviceDataRepository: DeviceDataRepository,
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanList = MutableStateFlow(SnapshotStateList<DeviceRoomDataEntity>())
    val scanList: StateFlow<SnapshotStateList<DeviceRoomDataEntity>> = _scanList

    init {
        // Inject the scanList from the ViewModel into the BleManager
        bleManager.setScanList(_scanList)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toggleScan(context: Context) {
        viewModelScope.launch {
            if (!_isScanning.value) {
                if (checkPermission(context)) {
                    bleManager.startBleScan()
                } else {
                    // Handle permission denial
                }
            } else {
                bleManager.stopBleScan()
            }
            _isScanning.value = !_isScanning.value
        }
    }
}