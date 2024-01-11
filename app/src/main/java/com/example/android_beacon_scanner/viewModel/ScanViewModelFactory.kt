package com.example.android_beacon_scanner.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.room.DeviceDataRepository

class ScanViewModelFactory(
    private val bleManager: BleManager,
    private val deviceDataRepository: DeviceDataRepository
) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
            return ScanViewModel(bleManager, deviceDataRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}