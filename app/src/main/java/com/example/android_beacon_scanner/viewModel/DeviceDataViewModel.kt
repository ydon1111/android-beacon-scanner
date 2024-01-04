package com.example.android_beacon_scanner.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity

class DeviceDataViewModel(private val repository: DeviceDataRepository) : ViewModel() {
    val allDeviceData: LiveData<List<DeviceRoomDataEntity>> = repository.allDeviceRoomData.asLiveData()
}