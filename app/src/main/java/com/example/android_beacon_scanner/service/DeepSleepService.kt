package com.example.android_beacon_scanner.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.android_beacon_scanner.R

class DeepSleepService : Service() {
    private val CHANNEL_ID = "DeepSleepServiceChannel"
    private val NOTIFICATION_ID = 1
    private val INTERVAL_MS = 3 * 60 * 1000L  // 30분 간격으로 서비스 실행

    private lateinit var wakeLock: PowerManager.WakeLock
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Acquire a wake lock to keep the CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        var wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DeepSleepService::WakeLock"
        )
        wakeLock.acquire()



        // Foreground Service with ongoing notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 서비스를 주기적으로 실행하기 위한 알람 설정
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, ConnectScreenService::class.java).let { intent ->
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        // 주기적으로 알람 설정
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            INTERVAL_MS,
            alarmIntent
        )

        Log.d("DeepSleepService", "Service is running")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // Release the wake lock when the service is no longer needed
        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        Log.d("DeepSleepService", "Service is being destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val notificationChannelId = createNotificationChannel()
        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("DeepSleepService")
            .setContentText("Service is running in deep sleep mode")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "DeepSleepServiceChannel"
        val channelName = "DeepSleepService"
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