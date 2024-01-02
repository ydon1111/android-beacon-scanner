package com.example.android_beacon_scanner.room

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "device_data")
@Parcelize
data class DeviceRoomDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val deviceName: String,
    val serviceUuid: String,
    val deviceAddress: String,
    var manufacturerData: ByteArray?
): Parcelable