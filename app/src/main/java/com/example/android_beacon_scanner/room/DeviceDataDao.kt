package com.example.android_beacon_scanner.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeviceDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceData(deviceRoomData: DeviceRoomData)

    @Query("SELECT * FROM device_data")
    fun getAllDeviceData(): LiveData<List<DeviceRoomData>>
}
