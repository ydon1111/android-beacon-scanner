package com.example.android_beacon_scanner.room

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase


@Database(entities = [DeviceRoomData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDataDao(): DeviceDataDao
    // 데이터 베이스 객체를 싱글톤으로 인스턴스.
    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context): AppDatabase? {
            if (instance == null)
                synchronized(AppDatabase::class) {
                    instance = databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "device_data"
                    )
                        .build()
                }
            return instance
        }

        fun destroyInstance() {
            instance = null
        }
    }
}




