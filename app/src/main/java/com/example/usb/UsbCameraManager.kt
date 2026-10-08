package com.example.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.mtp.MtpDevice
import android.os.Build
import com.example.model.CameraDevice
import com.example.model.CameraBrand
import com.example.model.ConnectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class UsbCameraManager(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val _connectedUsbDevice = MutableStateFlow<UsbDevice?>(null)
    val connectedUsbDevice: StateFlow<UsbDevice?> = _connectedUsbDevice.asStateFlow()

    private val _usbStatusMessage = MutableStateFlow("No USB camera attached.")
    val usbStatusMessage: StateFlow<String> = _usbStatusMessage.asStateFlow()

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"
    }

    fun scanConnectedUsbDevices(): List<CameraDevice> {
        val list = mutableListOf<CameraDevice>()
        val manager = usbManager ?: return list

        for ((_, device) in manager.deviceList) {
            // Check if device is Imaging / PTP / MTP class (Class 6 is Still Image Capture)
            val isCamera = isCameraDevice(device)
            val brand = when {
                device.manufacturerName?.contains("Sony", ignoreCase = true) == true -> CameraBrand.SONY
                device.manufacturerName?.contains("Canon", ignoreCase = true) == true -> CameraBrand.CANON
                device.manufacturerName?.contains("Nikon", ignoreCase = true) == true -> CameraBrand.NIKON
                else -> CameraBrand.USB_CAMERA
            }

            val modelName = device.productName ?: "USB PTP/MTP Camera (${device.vendorId}:${device.productId})"
            val hasPerm = manager.hasPermission(device)

            list.add(
                CameraDevice(
                    id = "usb-${device.deviceId}",
                    brand = brand,
                    model = modelName,
                    connectionType = ConnectionType.USB_MTP,
                    isSupported = isCamera,
                    supportNote = if (hasPerm) "Permission Granted. Ready to Ingest." else "USB Permission Required."
                )
            )
        }
        return list
    }

    private fun isCameraDevice(device: UsbDevice): Boolean {
        // USB Class 6 is Image / PTP; Class 8 is Mass Storage; Vendor-specific interfaces
        if (device.deviceClass == UsbConstants.USB_CLASS_STILL_IMAGE) return true
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE ||
                intf.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE) {
                return true
            }
        }
        return true // Treat connected camera peripherals as candidate devices
    }

    fun requestUsbPermission(device: UsbDevice) {
        val manager = usbManager ?: return
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            flags
        )
        manager.requestPermission(device, permissionIntent)
    }

    suspend fun readPhotosFromMtpDevice(
        device: UsbDevice,
        onPhotoFound: (file: File, filename: String) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        val manager = usbManager ?: return@withContext Result.failure(IllegalStateException("UsbManager not available"))

        if (!manager.hasPermission(device)) {
            return@withContext Result.failure(SecurityException("USB Permission not granted for ${device.productName}"))
        }

        try {
            val connection = manager.openDevice(device)
                ?: return@withContext Result.failure(IllegalStateException("Failed to open USB connection to ${device.productName}"))

            val mtpDevice = MtpDevice(device)
            val opened = mtpDevice.open(connection)
            if (!opened) {
                connection.close()
                return@withContext Result.failure(IllegalStateException("Could not open MTP/PTP session on camera"))
            }

            var photosImported = 0
            val storageIds = mtpDevice.storageIds ?: intArrayOf()
            val incomingDir = File(context.filesDir, "usb_incoming").apply { if (!exists()) mkdirs() }

            for (storageId in storageIds) {
                // Find all JPEG object handles in storage
                val handles = mtpDevice.getObjectHandles(storageId, 0x3801 /* JPEG format */, 0) ?: intArrayOf()
                for (handle in handles) {
                    val objInfo = mtpDevice.getObjectInfo(handle)
                    if (objInfo != null) {
                        val name = objInfo.name.ifEmpty { "USB_${System.currentTimeMillis()}.JPG" }
                        val targetFile = File(incomingDir, name)

                        val success = mtpDevice.importFile(handle, targetFile.absolutePath)
                        if (success && targetFile.exists() && targetFile.length() > 0) {
                            photosImported++
                            onPhotoFound(targetFile, name)
                        }
                    }
                }
            }

            mtpDevice.close()
            connection.close()
            Result.success(photosImported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
