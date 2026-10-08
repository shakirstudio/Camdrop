package com.example.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.CameraBrand
import com.example.model.CameraDevice
import com.example.model.ConnectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RealBluetoothCameraScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<CameraDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<CameraDevice>> = _discoveredDevices.asStateFlow()

    private val _statusMessage = MutableStateFlow("Bluetooth Idle")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scan = ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN)
            val connect = ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            return scan == PackageManager.PERMISSION_GRANTED && connect == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                addDiscoveredDevice(device, result.rssi)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { res ->
                addDiscoveredDevice(res.device, res.rssi)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _statusMessage.value = "BLE Scan failed with error code $errorCode"
        }
    }

    private fun addDiscoveredDevice(device: BluetoothDevice, rssi: Int) {
        try {
            val name = device.name ?: return // Only list identified devices
            val address = device.address

            // Filter or categorize by Camera Brand
            val brand = when {
                name.contains("ILCE", ignoreCase = true) || name.contains("Sony", ignoreCase = true) -> CameraBrand.SONY
                name.contains("EOS", ignoreCase = true) || name.contains("Canon", ignoreCase = true) -> CameraBrand.CANON
                name.contains("Nikon", ignoreCase = true) || name.contains("Z ", ignoreCase = true) -> CameraBrand.NIKON
                else -> CameraBrand.GENERIC
            }

            val current = _discoveredDevices.value.toMutableList()
            if (current.none { it.bluetoothAddress == address }) {
                current.add(
                    CameraDevice(
                        id = "ble-$address",
                        brand = brand,
                        model = name,
                        connectionType = ConnectionType.BLUETOOTH,
                        bluetoothAddress = address,
                        signalDbm = rssi,
                        isSupported = true,
                        supportNote = "Bluetooth pairing & trigger control. (High-res photo transfer requires Wi-Fi/FTP tethering)."
                    )
                )
                _discoveredDevices.value = current
            }
        } catch (e: SecurityException) {
            _statusMessage.value = "Bluetooth permission required to read device name"
        }
    }

    fun startBleScan(): Result<Boolean> {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _statusMessage.value = "Bluetooth hardware not supported on this device"
            return Result.failure(IllegalStateException("Bluetooth not available"))
        }
        if (!adapter.isEnabled) {
            _statusMessage.value = "Bluetooth is disabled. Please enable Bluetooth in Android settings."
            return Result.failure(IllegalStateException("Bluetooth disabled"))
        }
        if (!hasPermissions()) {
            _statusMessage.value = "Bluetooth Scan and Connect permissions are required."
            return Result.failure(SecurityException("Bluetooth permission denied"))
        }

        try {
            _discoveredDevices.value = emptyList()
            val scanner = adapter.bluetoothLeScanner
            if (scanner == null) {
                _statusMessage.value = "BLE Scanner not available on this device"
                return Result.failure(IllegalStateException("BLE Scanner null"))
            }

            _isScanning.value = true
            _statusMessage.value = "Scanning for nearby discoverable cameras & BLE peripherals..."
            scanner.startScan(leScanCallback)

            // Also check already bonded devices
            adapter.bondedDevices?.forEach { dev ->
                addDiscoveredDevice(dev, -55)
            }

            return Result.success(true)
        } catch (e: SecurityException) {
            _isScanning.value = false
            _statusMessage.value = "Permission security exception: ${e.message}"
            return Result.failure(e)
        }
    }

    fun stopBleScan() {
        if (!_isScanning.value) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
        } catch (ignored: SecurityException) {}
        _isScanning.value = false
        _statusMessage.value = "Scan stopped. Found ${_discoveredDevices.value.size} device(s)."
    }
}
