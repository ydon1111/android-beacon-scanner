package com.example.android_beacon_scanner.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DeviceRoomData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDataDao(): DeviceDataDao
}