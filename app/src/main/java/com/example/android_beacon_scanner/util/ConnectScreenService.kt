package com.example.android_beacon_scanner.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.R
import com.example.android_beacon_scanner.room.DeviceDataRepository


class ConnectScreenService : Service() {

    private val CHANNEL_ID = "ConnectScreenServiceChannel"
    private val NOTIFICATION_ID = 1
    private var bleManager: BleManager? = null
    private lateinit var deviceDataRepository: DeviceDataRepository
    private var wakeLock: PowerManager.WakeLock? = null

    private var isScreenOn = true // 화면 상태를 나타내는 플래그
    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        deviceDataRepository = DeviceDataRepository.getInstance(applicationContext)
        bleManager = BleManager(applicationContext, deviceDataRepository)

        // 화면 상태를 감지하는 BroadcastReceiver 등록
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    @SuppressLint("ForegroundServiceType", "WakelockTimeout")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ConnectScreenService", "onStartCommand called")

        // Foreground Service with ongoing notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 화면이 꺼진 상태일 때만 BLE 스캔을 시작
        if (isScreenOn) {
            // Acquire wake lock
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ConnectScreenService::WakeLock"
            )
            wakeLock?.acquire()

            // Initialize and start BLE scanning
            val bleManager = BleManager(applicationContext, deviceDataRepository)
            bleManager.startBleScan()

            // Log that the service has started
            Log.d("ConnectScreenService", "Service started")
        }

        return START_STICKY
    }

    private val screenReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    // 화면 켜진 상태에서 BLE 스캔 시작
                    val bleManager = BleManager(applicationContext, deviceDataRepository)
                    bleManager.startBleScan()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    // 화면 꺼진 상태에서 BLE 스캔 중지
                    val bleManager = BleManager(applicationContext, deviceDataRepository)
                    bleManager.stopBleScan()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        // Wake Lock 및 BroadcastReceiver 해제
        wakeLock?.release()
        unregisterReceiver(screenReceiver)

        // Log that the service has been destroyed
        Log.d("ConnectScreenService", "Service destroyed")
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
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)  // This makes the notification ongoing
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