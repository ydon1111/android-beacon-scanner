package com.example.android_beacon_scanner.room

import android.content.Context
import androidx.lifecycle.distinctUntilChanged
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

    // New function to fetch entries with the same bleDataCount and currentDateAndTime
    suspend fun getDeviceDataWithSameValues(
        deviceName: String,
        bleDataCount: Int,
        currentDateAndTime: String
    ): List<DeviceRoomDataEntity> {
        return deviceDataDao.getDeviceDataWithSameValues(
            deviceName,
            bleDataCount,
            currentDateAndTime
        )
    }

    suspend fun getDeviceData(deviceName: String): DeviceRoomDataEntity? {
        return withContext(Dispatchers.IO) {
            deviceDataDao.getDeviceData(deviceName)
        }
    }

    suspend fun deleteAllDeviceData() {
        withContext(Dispatchers.IO) {
            deviceDataDao.deleteAllDeviceData()
        }
    }

    fun getDeviceDataFlow(deviceName: String): Flow<List<DeviceRoomDataEntity>> {
        return deviceDataDao.getDeviceDataFlow(deviceName)
    }

    fun observeLatestDeviceData(deviceName: String): Flow<DeviceRoomDataEntity?> {
        return deviceDataDao.getLatestDeviceDataFlow(deviceName)
    }

    suspend fun getDeviceDataWithBleCountGreaterOrEqual(
        deviceName: String,
        bleCount: Int
    ): List<DeviceRoomDataEntity> {
        return deviceDataDao.getDeviceDataWithBleCountGreaterOrEqual(deviceName, bleCount)
    }
    companion object {
        private var instance: DeviceDataRepository? = null

        fun getInstance(applicationContext: Context): DeviceDataRepository {
            return instance ?: synchronized(this) {
                instance ?: createInstance(applicationContext).also { instance = it }
            }
        }

        private fun createInstance(applicationContext: Context): DeviceDataRepository {
            val appDatabase = AppDatabase.getInstance(applicationContext)
            val deviceDataDao = appDatabase!!.deviceDataDao()
            return DeviceDataRepository(deviceDataDao)
        }
    }

    // Rating 값을 업데이트하는 함수 추가
    suspend fun updateRating(deviceName: String, newRating: Int) {
        deviceDataDao.updateLatest18Ratings(deviceName, newRating)
    }

}