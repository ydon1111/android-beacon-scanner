package com.example.android_beacon_scanner.room

import androidx.lifecycle.LiveData
import com.example.android_beacon_scanner.room.DeviceDataDao
import com.example.android_beacon_scanner.room.DeviceRoomData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceDataRepository @Inject constructor(private val deviceDataDao: DeviceDataDao) {
    val allDeviceRoomData: LiveData<List<DeviceRoomData>> = deviceDataDao.getAllDeviceData()

    suspend fun insertDeviceData(deviceRoomData: DeviceRoomData) {
        deviceDataDao.insertDeviceData(deviceRoomData)
    }

    suspend fun isDeviceDataExists(deviceAddress: String): Boolean {
        return deviceDataDao.isDeviceDataExists(deviceAddress)
    }

    suspend fun getDeviceData(deviceName: String): DeviceRoomData? {
        return deviceDataDao.getDeviceData(deviceName)
    }
}
