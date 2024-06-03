package com.example.android_beacon_scanner.service

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/beacon-data/upload")
    fun sendBeaconDataList(@Body beaconDataRequests: List<BeaconDataRequest>): Call<Void>
}