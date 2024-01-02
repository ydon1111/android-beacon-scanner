package com.example.android_beacon_scanner.ble


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomData
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class BleViewModel @Inject constructor(
    private val deviceDataRepository: DeviceDataRepository
) : ViewModel() {
    fun insertDeviceData(deviceData: DeviceRoomDataEntity) {
        // Room 데이터베이스에 데이터 삽입을 위한 코드
        viewModelScope.launch {
            deviceDataRepository.insertDeviceData(deviceData)
        }
    }

    val deviceDataList: Flow<List<DeviceRoomDataEntity>> = deviceDataRepository.allDeviceRoomData

    // 데이터 삭제를 수행하는 함수 추가
    fun deleteAllDeviceData() {
        viewModelScope.launch(Dispatchers.IO) {
            deviceDataRepository.deleteAllDeviceData()
        }
    }
}
