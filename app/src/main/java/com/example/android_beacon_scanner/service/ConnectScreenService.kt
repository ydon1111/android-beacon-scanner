package com.example.android_beacon_scanner.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.android_beacon_scanner.BleInterface
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.R
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class ConnectScreenService : Service() {

    private val CHANNEL_ID = "ConnectScreenServiceChannel"
    private val NOTIFICATION_ID = 1
    private lateinit var deviceDataRepository: DeviceDataRepository
    private lateinit var bleManager: BleManager

    override fun onCreate() {
        super.onCreate()
        deviceDataRepository = DeviceDataRepository.getInstance(applicationContext)
        bleManager = BleManager(applicationContext, deviceDataRepository)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground Service with ongoing notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // BLE 스캔 시작
        bleManager.startBleScan()

        // BLE 데이터를 처리하고 Room 데이터베이스에 저장
        bleManager.onConnectedStateObserve(object : BleInterface {
            override fun onConnectedStateObserve(isConnected: Boolean, data: String) {
                // BLE 데이터를 처리하는 작업을 수행합니다.
                if (isConnected) {
                    // 연결이 성공했을 때 BLE 데이터를 가져오고 Room 데이터베이스에 저장
                    val deviceData = bleManager.getConnectedDevice()
                    if (deviceData != null) {
                        // 데이터를 가져와서 Room 데이터베이스에 저장하는 작업을 수행
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
                            Log.d("ConnectScreenService", "Inserted data into Room database: $insertData")
                        }
                    }
                }
            }
        })
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // BLE 스캔 중지
        bleManager.stopBleScan()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val notificationChannelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("ConnectScreenService")
            .setContentText("App is running in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)  // This makes the notification ongoing
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "ConnectScreenServiceChannel"
        val channelName = "ConnectScreenService"
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        return channelId
    }
}
