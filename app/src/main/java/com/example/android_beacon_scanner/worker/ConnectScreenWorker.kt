package com.example.android_beacon_scanner.worker

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ConnectScreenWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val applicationContext = applicationContext
        val deviceDataRepository = DeviceDataRepository.getInstance(applicationContext)
        val bleManager = BleManager(applicationContext, deviceDataRepository)

        // 예시: BLE 스캔 시작
        bleManager.startBleScan()

        // BLE 데이터를 가져와서 Room 데이터베이스에 저장
        val deviceData = bleManager.getConnectedDevice()
        if (deviceData != null) {
            val temperature = deviceData.temperature ?: 0
            val bleDataCount = deviceData.bleDataCount ?: 0
            val currentDateAndTime = deviceData.currentDateAndTime ?: ""
            val timestampNanos = deviceData.timestampNanos ?: ""
            val valueX = deviceData.valueX ?: 0
            val valueY = deviceData.valueY ?: 0
            val valueZ = deviceData.valueZ ?: 0

            // 데이터베이스에 저장
            val insertData = DeviceRoomDataEntity(
                deviceName = deviceData.deviceName,
                deviceAddress = deviceData.deviceAddress,
                manufacturerData = deviceData.manufacturerData,
                temperature = temperature,
                bleDataCount = bleDataCount,
                currentDateAndTime = currentDateAndTime,
                timestampNanos = timestampNanos,
                valueX = valueX,
                valueY = valueY,
                valueZ = valueZ
            )

            MainScope().launch(Dispatchers.IO) {
                deviceDataRepository.insertDeviceData(insertData)
            }
        }

        return Result.success()
    }
}