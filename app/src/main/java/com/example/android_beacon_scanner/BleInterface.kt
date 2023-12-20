package com.example.android_beacon_scanner

interface BleInterface {
    fun onConnectedStateObserve(isConnected: Boolean, data: String)
}
