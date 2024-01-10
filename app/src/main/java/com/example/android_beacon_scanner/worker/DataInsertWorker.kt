package com.example.android_beacon_scanner.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity

class DataInsertWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val deviceDataRepository: DeviceDataRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 데이터 전달
        val deviceName = inputData.getString("deviceName")
        val deviceAddress = inputData.getString("deviceAddress")
        val manufacturerData = inputData.getByteArray("manufacturerData")
        val temperature = inputData.getInt("temperature", 0) // 기본값 0으로 설정
        val bleDataCount = inputData.getInt("bleDataCount", 0) // 기본값 0으로 설정
        val currentDateAndTime = inputData.getString("currentDateAndTime")
        val timestampNanos = inputData.getString("timestampNanos")
        val valueX = inputData.getInt("valueX", 0) // 기본값 0으로 설정
        val valueY = inputData.getInt("valueY", 0) // 기본값 0으로 설정
        val valueZ = inputData.getInt("valueZ", 0) // 기본값 0으로 설정

        // 데이터 생성
        val deviceData = DeviceRoomDataEntity(
            deviceName = deviceName ?: "Unknown",
            deviceAddress = deviceAddress ?: "Unknown",
            manufacturerData = manufacturerData,
            temperature = temperature,
            bleDataCount = bleDataCount,
            currentDateAndTime = currentDateAndTime ?: "Unknown",
            timestampNanos = timestampNanos ?: "Unknown",
            valueX = valueX,
            valueY = valueY,
            valueZ = valueZ
        )

        // 데이터베이스에 데이터 추가 작업 수행
        deviceDataRepository.insertDeviceData(deviceData)

        return Result.success()
    }
}




