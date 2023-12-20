package com.example.android_beacon_scanner.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.android_beacon_scanner.BleInterface
import com.example.android_beacon_scanner.DeviceData
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManger @Inject constructor(
    private val context: Context
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanList: SnapshotStateList<DeviceData>? = null
    private var connectedStateObserver: BleInterface? = null
    var bleGatt: BluetoothGatt? = null

    private val scanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d("onScanResult", result.toString())
            if (result.device.name != null) {
                var uuid = "null"
                if (result.scanRecord?.serviceUuids != null) {
                    uuid = result.scanRecord!!.serviceUuids.toString()
                }
                val scanItem = DeviceData(
                    result.device.name ?: "null",
                    uuid,
                    result.device.address ?: "null"
                )
                if (!scanList!!.contains(scanItem)) {
                    scanList!!.add(scanItem)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            println("onScanFailed  $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BleManger", "연결 성공")
                gatt?.discoverServices()
                connectedStateObserver?.onConnectedStateObserve(
                    true,
                    "onConnectionStateChange:  STATE_CONNECTED"
                            + "\n"
                            + "---"
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BleManger", "연결 해제")
                connectedStateObserver?.onConnectedStateObserve(
                    false,
                    "onConnectionStateChange:  STATE_DISCONNECTED"
                            + "\n"
                            + "---"
                )
            }
        }
        // 장치에 대한 새로운 서비스가 발견되었을 때 호출되는 콜백
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            // 원격 장치가 성공적으로 탐색된 경우
            if (status == BluetoothGatt.GATT_SUCCESS) {
                MainScope().launch {
                    bleGatt = gatt
                    Toast.makeText(context, " ${gatt?.device?.name} 연결 성공", Toast.LENGTH_SHORT)
                        .show()
                    var sendText =
                        "onServicesDiscovered:  GATT_SUCCESS" + "\n" + "                         ↓" + "\n"

                    for (service in gatt?.services!!) {
                        sendText += "- " + service.uuid.toString() + "\n"
                        for (characteristics in service.characteristics) {
                            sendText += "       " + characteristics.uuid.toString() + "\n"
                        }
                    }
                    sendText += "---"
                    connectedStateObserver?.onConnectedStateObserve(
                        true,
                        sendText
                    )
                }.cancel()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    fun startBleScan() {
        scanList?.clear()

//        val scanFilter: ScanFilter = ScanFilter.Builder()
//            .setDeviceName("DeviceName")
//            .setDeviceAddress("DeviceAddress")
//            .setServiceUuid(ParcelUuid(serviceUuid))
//            .setManufacturerData(manufacturerId, manufacturerData, manufacturerDataMask)
//            .setServiceData(serviceUuid, serviceData)
//            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setLegacy(false) // 이 부분이 ble 5.0 ext adv 를 스캔 가능하게함
            .build()

        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        bluetoothLeScanner.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun startBleConnectGatt(deviceData: DeviceData) {
        bluetoothAdapter
            .getRemoteDevice(deviceData.address)
            .connectGatt(context, false, gattCallback)
    }

    fun setScanList(pScanList: SnapshotStateList<DeviceData>) {
        scanList = pScanList
    }

    fun onConnectedStateObserve(pConnectedStateObserver: BleInterface) {
        connectedStateObserver = pConnectedStateObserver
    }



}
