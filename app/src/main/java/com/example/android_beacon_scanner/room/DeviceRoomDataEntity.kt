package com.example.android_beacon_scanner.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.android_beacon_scanner.util.Converter
import kotlinx.parcelize.Parcelize

@Entity(tableName = "device_data")
@Parcelize
data class DeviceRoomDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val deviceName: String,
    val deviceAddress: String,
    var manufacturerData: ByteArray?,
    var temperature: Int?,
    var bleDataCount: Int?,

    @TypeConverters(Converter::class)
    val currentDateAndTime: String? = null,

    @ColumnInfo(name = "value_x")
    val valueX: Int?,
    @ColumnInfo(name = "value_y")
    val valueY: Int?,
    @ColumnInfo(name = "value_z")
    val valueZ: Int?

) : Parcelable