package com.example.android_beacon_scanner.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_data")
data class DeviceRoomData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val deviceName: String,
    val serviceUuid: String,
    val deviceAddress: String,
    val manufacturerData: ByteArray // 여기서 바이트 배열로 manufacturerData를 저장합니다.
)