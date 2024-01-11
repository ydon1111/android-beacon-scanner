package com.example.android_beacon_scanner.viewModel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.flow.Flow

class ScanViewModel(
    private val bleManager: BleManager,
    private val deviceDataRepository: DeviceDataRepository
) : ViewModel() {
    // LiveData를 사용하여 스캔 결과를 저장
    private val _scanList = MutableLiveData<List<DeviceRoomDataEntity>>()
    val scanList: LiveData<List<DeviceRoomDataEntity>> = _scanList


    // BLE 스캔을 시작하는 함수
    @RequiresApi(Build.VERSION_CODES.O)
    fun startBleScan() {
        bleManager.startBleScan()
    }

    // BLE 스캔을 중지하는 함수
    fun stopBleScan() {
        bleManager.stopBleScan()
    }

    // BLE 스캔 결과를 업데이트하는 함수
    fun updateScanResults(results: List<DeviceRoomDataEntity>) {
        _scanList.postValue(results)
    }
}