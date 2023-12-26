package com.example.android_beacon_scanner.room

import android.util.SparseArray
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_data")
data class DeviceRoomData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val deviceName: String,
    val serviceUuid: String,
    val deviceAddress: String,
    val manufacturerData: ByteArray?
)