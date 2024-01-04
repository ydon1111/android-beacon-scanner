package com.example.android_beacon_scanner.room

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.android_beacon_scanner.util.Converter
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity(tableName = "device_data")
@Parcelize
data class DeviceRoomDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val deviceName: String,
    val serviceUuid: String,
    val deviceAddress: String,
    var manufacturerData: ByteArray?,
    var temperature: Int?,
    var bleDataCount: Int?,

    @TypeConverters(Converter::class)
    val currentDateAndTime: Date? = null,
    @TypeConverters(Converter::class)
    val accXValues: List<Int>?, // ACC_X_values를 리스트로 추가
    @TypeConverters(Converter::class)
    val accYValues: List<Int>?, // ACC_Y_values를 리스트로 추가
    @TypeConverters(Converter::class)
    val accZValues: List<Int>?  // ACC_Z_values를 리스트로 추가
): Parcelable