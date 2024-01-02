package com.example.android_beacon_scanner.room

import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext


@Singleton
class DeviceDataRepository @Inject constructor(private val deviceDataDao: DeviceDataDao) {
    val allDeviceRoomData: Flow<List<DeviceRoomDataEntity>> = deviceDataDao.getAllDeviceData()

    suspend fun insertDeviceData(deviceRoomData: DeviceRoomDataEntity) {
        withContext(Dispatchers.IO) {
            deviceDataDao.insertDeviceData(deviceRoomData)
        }
    }

    suspend fun isDeviceDataExists(deviceAddress: String): Boolean {
        return deviceDataDao.isDeviceDataExists(deviceAddress)
    }

    suspend fun getDeviceData(deviceName: String): DeviceRoomData? {
        return withContext(Dispatchers.IO) {
            deviceDataDao.getDeviceData(deviceName)?.toDeviceRoomData()
        }
    }

    private fun DeviceRoomData.toDeviceRoomData(): DeviceRoomData {
        return DeviceRoomData(
            id = this.id,
            deviceName = this.deviceName,
            serviceUuid = this.serviceUuid,
            deviceAddress = this.deviceAddress,
            manufacturerData = this.manufacturerData
        )
    }


    suspend fun deleteAllDeviceData() {
        withContext(Dispatchers.IO) {
            deviceDataDao.deleteAllDeviceData()
        }
    }


}