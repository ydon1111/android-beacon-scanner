package com.example.android_beacon_scanner.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.android_beacon_scanner.R


class ConnectScreenService : Service() {

    private val CHANNEL_ID = "ConnectScreenServiceChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground Service를 시작합니다.
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 여기에서 앱의 백그라운드 작업을 수행할 수 있습니다.
        // 화면이 꺼져도 계속 동작합니다.

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // 서비스가 종료될 때 필요한 정리 작업을 수행합니다.
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
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "ConnectScreenServiceChannel"
        val channelName = "ConnectScreenService"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        return channelId
    }
}