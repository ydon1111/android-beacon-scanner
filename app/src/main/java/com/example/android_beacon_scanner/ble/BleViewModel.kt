package com.example.android_beacon_scanner.ble


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class BleViewModel @Inject constructor(private val deviceDataRepository: DeviceDataRepository) : ViewModel() {
    fun insertDeviceData(deviceData: DeviceRoomData) {
        // Room 데이터베이스에 데이터 삽입을 위한 코드
        viewModelScope.launch {
            deviceDataRepository.insertDeviceData(deviceData)
        }
    }
}
