package com.example.android_beacon_scanner.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.navigation.NavHostController
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.MainActivity
import com.example.android_beacon_scanner.R
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.O)
class ConnectScreenService : LifecycleService() {

    private val CHANNEL_ID = "ConnectScreenServiceChannel"
    private val NOTIFICATION_ID = 1
    private lateinit var deviceDataRepository: DeviceDataRepository
    private lateinit var bleManager: BleManager
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        deviceDataRepository = DeviceDataRepository.getInstance(applicationContext)
        bleManager = BleManager(applicationContext, deviceDataRepository)

        acquireWakeLock()
        startForeground(NOTIFICATION_ID, createNotification(null))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        acquireWakeLock()

        val deviceData = intent?.getParcelableExtra<DeviceRoomDataEntity>("deviceData")
        startForeground(NOTIFICATION_ID, createNotification(deviceData))

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        stopForeground(true)
    }

    private fun createNotification(deviceData: DeviceRoomDataEntity?): Notification {
        val notificationChannelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        val connectScreenIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, connectScreenIntent,
            flags or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("서울아산병원 보조기앱")
            .setContentText("서울아산병원 보조기앱이 동작중 입니다")
            .setSmallIcon(R.drawable.baseline_local_hospital_24)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = CHANNEL_ID
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

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyApp::MyWakeLockTag"
        ).apply {
            if (!isHeld) {
                acquire(Long.MAX_VALUE)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
}
