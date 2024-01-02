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
    var manufacturerData: ByteArray?,
    var date: Int? = null,
    var temperature: Double? = null,
    var velcro: Boolean? = null,
    var count: Int? = null,
    var accX: Int? = null,  // Add accX field here
    var accY: Int? = null,  // Add accY field here
    var accZ: Int? = null,  // Add accZ field here
    var gyroX: Int? = null, // Add gyroX field here
    var gyroY: Int? = null, // Add gyroY field here
    var gyroZ: Int? = null,  // Add gyroZ field here
): Parcelable