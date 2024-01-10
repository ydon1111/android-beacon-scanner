package com.example.android_beacon_scanner.worker


import android.annotation.SuppressLint
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest.*
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


@SuppressLint("InvalidPeriodicWorkRequestInterval")
fun setupWorkManager(context: Context) {
    val workRequest = Builder(
        ConnectScreenWorker::class.java,
        4, // 주기 (초 단위)
        TimeUnit.SECONDS
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "ConnectScreenWork",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}