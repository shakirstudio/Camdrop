package com.example.camera

import com.example.model.CameraDevice
import com.example.model.CapturedPhoto
import com.example.model.ConnectionStatus
import com.example.model.TransferJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CameraProvider {
    val connectionStatus: StateFlow<ConnectionStatus>
    val activeTransfers: StateFlow<List<TransferJob>>
    val incomingPhotos: Flow<CapturedPhoto>

    suspend fun discoverCameras(): List<CameraDevice>
    suspend fun connect(device: CameraDevice): Result<Boolean>
    suspend fun disconnect()
    suspend fun getCameraInfo(): String
    suspend fun getSupportedCapabilities(): List<String>
    suspend fun startPhotoMonitoring()
    suspend fun stopPhotoMonitoring()
    suspend fun triggerManualFetch(): Result<Int>
}
