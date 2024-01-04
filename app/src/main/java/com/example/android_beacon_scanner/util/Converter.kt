package com.example.android_beacon_scanner.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converter {
    private val gson = Gson()

    @TypeConverter
    fun fromListInt(list: List<Int>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toListInt(json: String?): List<Int>? {
        if (json == null) {
            return null
        }

        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(time: Long?): Date? {
        return if (time == null) null else Date(time)
    }
}