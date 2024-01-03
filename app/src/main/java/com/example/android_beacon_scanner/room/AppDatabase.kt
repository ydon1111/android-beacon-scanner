package com.example.android_beacon_scanner.room

import android.content.Context
import android.util.Log
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase


@Database(
    entities = [DeviceRoomDataEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDataDao(): DeviceDataDao

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context): AppDatabase? {
            if (instance == null)
                synchronized(AppDatabase::class) {
                    // Update the database name to "device_data"
                    instance = databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "device_data_all"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                }
            return instance
        }

        fun destroyInstance() {
            instance = null
        }
    }
}




