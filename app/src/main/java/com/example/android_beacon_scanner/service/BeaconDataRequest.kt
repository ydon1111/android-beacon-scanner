package com.example.android_beacon_scanner.service

data class BeaconDataRequest(
    val deviceName: String?,
    val deviceAddress: String?,
    val manufacturerData: ByteArray?,
    val temperature: Int?,
    val bleDataCount: Int,
    val currentDateAndTime: String?,
    val timestampNanos: String?,
    val valueX: Int?,
    val valueY: Int?,
    val valueZ: Int?,
    val rating: Int?
)
