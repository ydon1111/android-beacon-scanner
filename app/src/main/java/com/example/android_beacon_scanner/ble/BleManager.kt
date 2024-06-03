package com.example.android_beacon_scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.util.containsKey
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import com.example.android_beacon_scanner.service.ApiService
import com.example.android_beacon_scanner.service.BeaconDataRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Singleton
class BleManager @Inject constructor(
    private val context: Context,
    private val deviceDataRepository: DeviceDataRepository
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var connectedStateObserver: BleInterface? = null
    private var bleGatt: BluetoothGatt? = null
    private var bleDataCount = 0
    private var isScanning = false // Indicates if scanning is in progress
    private var connectedDevice: DeviceRoomDataEntity? = null
    private var connectionRetries = 0
    private lateinit var _scanList: MutableStateFlow<SnapshotStateList<DeviceRoomDataEntity>>
    private val collectedData = mutableListOf<DeviceRoomDataEntity>() // List to collect data in memory
    private val maxDataSize = 180 // Maximum number of items before sending to the server

    private val handler = Handler(Looper.getMainLooper())
    private val scanInterval: Long = 30000L // 30 seconds

    private val logging = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://121.184.192.5:8081/") // Ensure this is correct
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    private val scanRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            if (!isScanning) {
                startBleScan()
            } else {
                stopBleScan()
            }
            handler.postDelayed(this, scanInterval)
        }
    }

    fun setConnectedDevice(deviceData: DeviceRoomDataEntity) {
        connectedDevice = deviceData
    }

    fun getConnectedDevice(): DeviceRoomDataEntity? {
        return connectedDevice
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name
            if (deviceName != null && deviceName.contains("M")) {
                val manufacturerData = result.scanRecord?.manufacturerSpecificData
                if (manufacturerData != null && manufacturerData.containsKey(16505)) {
                    val manufacturerDataValue = manufacturerData[16505]
                    val manufacturerDataIntArray = manufacturerDataValue?.map { it.toInt() }?.toIntArray()
                    val packetSize = manufacturerDataValue?.size ?: 0
                    Log.d("onScanResult", "Packet Size: $packetSize bytes")
                    Log.d("onScanResult", result.toString())
                    val temperature = manufacturerDataIntArray?.let {
                        if (it.size >= 2) it[it.size - 2] else null
                    }
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                    val timestampNano = result.timestampNanos
                    val formattedTimestamp = dateFormat.format(Date(timestampNano / 1000000L))
                    val currentDateAndTime = Date()
                    val formattedDate = dateFormat.format(currentDateAndTime)
                    val startIndex = 0
                    val step = 12
                    val count = 18
                    for (i in 0 until count) {
                        val indexX = startIndex + i * step
                        val indexY = (startIndex + 2) + i * step
                        val indexZ = (startIndex + 4) + i * step
                        val valueX = manufacturerDataIntArray?.getOrNull(indexX) ?: 0
                        val valueY = manufacturerDataIntArray?.getOrNull(indexY) ?: 0
                        val valueZ = manufacturerDataIntArray?.getOrNull(indexZ) ?: 0
                        val scanItem = DeviceRoomDataEntity(
                            deviceName = deviceName,
                            deviceAddress = result.device.address ?: "null",
                            manufacturerData = manufacturerData[16505],
                            temperature = temperature,
                            bleDataCount = bleDataCount,
                            currentDateAndTime = formattedDate,
                            timestampNanos = formattedTimestamp,
                            valueX = valueX,
                            valueY = valueY,
                            valueZ = valueZ,
                            rating = null
                        )
                        _scanList.value.add(scanItem)

                        // Add to collected data
                        collectedData.add(scanItem)

                        // Save to local database in real-time
                        saveDataToDeviceDataRepository(scanItem)

                        // Check if collected data has reached the maximum size
                        if (collectedData.size >= maxDataSize) {
                            Log.d("BleManager", "Maximum data size reached. Uploading data to server.")
                            uploadDataToServer()
                        }
                    }
                    bleDataCount++
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("onScanFailed", "Scan failed with error code $errorCode")
        }
    }

    private fun sendDataToServer(dataToSend: List<DeviceRoomDataEntity>) {
        val beaconDataRequests = dataToSend.map { scanItem ->
            scanItem.bleDataCount?.let {
                BeaconDataRequest(
                    deviceName = scanItem.deviceName,
                    deviceAddress = scanItem.deviceAddress,
                    // Removed manufacturerData from the request
                    temperature = scanItem.temperature,
                    bleDataCount = it,
                    currentDateAndTime = scanItem.currentDateAndTime,
                    timestampNanos = scanItem.timestampNanos,
                    valueX = scanItem.valueX,
                    valueY = scanItem.valueY,
                    valueZ = scanItem.valueZ,
                    rating = scanItem.rating
                )
            }
        }.filterNotNull()

        Log.d("BleManager", "Prepared BeaconDataRequest List: $beaconDataRequests")

        val call = apiService.sendBeaconDataList(beaconDataRequests)
        call.enqueue(object : retrofit2.Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("BleManager", "Data sent successfully: ${response.body()}")
                } else {
                    Log.e("BleManager", "Failed to send data: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("BleManager", "Error sending data", t)
            }
        })
    }

    private fun uploadDataToServer() {
        if (collectedData.isNotEmpty()) {
            val dataToSend = collectedData.toList() // Create a copy of the list to avoid concurrency issues
            collectedData.clear() // Clear the original list for new data
            Log.d("BleManager", "Sending data to server: ${dataToSend.size} items")
            sendDataToServer(dataToSend)
        } else {
            Log.d("BleManager", "No data to send to the server")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    fun startBleScan() {
        if (!isScanning) {
            _scanList.value.clear()
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setLegacy(false)
                .build()
            val filters = listOf(
                ScanFilter.Builder().setManufacturerData(16505, byteArrayOf(), byteArrayOf()).build()
            )
            bluetoothLeScanner.startScan(filters, scanSettings, scanCallback)
            isScanning = true
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        if (isScanning) {
            bluetoothLeScanner.stopScan(scanCallback)
            bleDataCount = 0
            isScanning = false
        }
    }

    fun setScanList(scanList: MutableStateFlow<SnapshotStateList<DeviceRoomDataEntity>>) {
        _scanList = scanList
    }

    fun onConnectedStateObserve(pConnectedStateObserver: BleInterface) {
        connectedStateObserver = pConnectedStateObserver
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveDataToDeviceDataRepository(scanItem: DeviceRoomDataEntity) {
        MainScope().launch(Dispatchers.IO) {
            deviceDataRepository.insertDeviceData(scanItem)
        }
    }

    private var scanResultCallback: ((ScanResult) -> Unit)? = null

    fun setScanResultCallback(callback: (ScanResult) -> Unit) {
        scanResultCallback = callback
    }

    @SuppressLint("MissingPermission")
    fun pairWithBeacon(device: BluetoothDevice) {
        bleGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BleManager", "Connected")
                gatt?.discoverServices()
                connectedStateObserver?.onConnectedStateObserve(true, "Connected")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BleManager", "Disconnected")
                connectedStateObserver?.onConnectedStateObserve(false, "Disconnected")
                bleDataCount = 0
                if (connectionRetries < MAX_CONNECTION_RETRIES) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        gatt?.connect()
                        connectionRetries++
                    }, RETRY_DELAY_MILLIS)
                } else {
                    Log.e("BleManager", "Max connection retries reached")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                MainScope().launch {
                    bleGatt = gatt
                    Toast.makeText(context, "Connected to ${gatt?.device?.name}", Toast.LENGTH_SHORT).show()
                    var sendText = "Services Discovered: GATT_SUCCESS\n"
                    gatt?.services?.forEach { service ->
                        sendText += "- ${service.uuid}\n"
                        service.characteristics.forEach { characteristic ->
                            sendText += "    ${characteristic.uuid}\n"
                            gatt.readCharacteristic(characteristic)
                        }
                    }
                    sendText += "---"
                    connectedStateObserver?.onConnectedStateObserve(true, sendText)
                }
            }
        }
    }

    companion object {
        private const val MAX_CONNECTION_RETRIES = 3
        private const val RETRY_DELAY_MILLIS = 1000L // 1 second delay
    }
}
