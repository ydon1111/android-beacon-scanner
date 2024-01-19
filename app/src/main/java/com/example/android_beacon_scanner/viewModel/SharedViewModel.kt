package com.example.android_beacon_scanner.viewModel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedViewModel : ViewModel() {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    fun setIsScanning(value: Boolean) {
        _isScanning.value = value
    }
}