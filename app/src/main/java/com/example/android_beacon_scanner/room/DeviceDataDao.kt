package com.example.android_beacon_scanner.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceData(deviceRoomData: DeviceRoomDataEntity)


    @Query("DELETE FROM device_data")
    suspend fun deleteAllDeviceData()

    @Query("SELECT * FROM device_data")
    fun getAllDeviceData(): Flow<List<DeviceRoomDataEntity>>

    @Query("SELECT EXISTS (SELECT 1 FROM device_data WHERE deviceAddress = :deviceAddress)")
    suspend fun isDeviceDataExists(deviceAddress: String): Boolean

    @Query("SELECT * FROM device_data WHERE deviceName = :deviceName LIMIT 1")
    fun getDeviceData(deviceName: String): DeviceRoomDataEntity?

    @Query("SELECT * FROM device_data WHERE deviceName = :deviceName")
    fun getDeviceDataFlow(deviceName: String): Flow<List<DeviceRoomDataEntity>>

    @Query("SELECT * FROM device_data WHERE deviceName = :deviceName ORDER BY currentDateAndTime DESC LIMIT 18")
    suspend fun getLatestDeviceData(deviceName: String): DeviceRoomDataEntity?


    @Query("SELECT * FROM device_data WHERE deviceName = :deviceName ORDER BY currentDateAndTime DESC")
    fun getLatestDeviceDataFlow(deviceName: String): Flow<DeviceRoomDataEntity?>

    @Query("SELECT * FROM device_data WHERE deviceName = :deviceName AND bleDataCount = :bleDataCount AND currentDateAndTime = :currentDateAndTime")
    suspend fun getDeviceDataWithSameValues(
        deviceName: String,
        bleDataCount: Int,
        currentDateAndTime: String
    ): List<DeviceRoomDataEntity>

    @Query("SELECT * FROM device_data WHERE deviceName = :deviceName AND bleDataCount >= :bleCount")
    suspend fun getDeviceDataWithBleCountGreaterOrEqual(deviceName: String, bleCount: Int): List<DeviceRoomDataEntity>

}
