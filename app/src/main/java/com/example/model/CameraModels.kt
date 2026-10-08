package com.example.model

enum class CameraBrand(val displayName: String) {
    SONY("Sony"),
    CANON("Canon"),
    NIKON("Nikon"),
    USB_CAMERA("USB / PTP Device"),
    GENERIC("Generic Camera")
}

enum class ConnectionType(val displayName: String) {
    WIFI("Wi-Fi Direct / Local IP"),
    FTP("FTP Camera Server Tether"),
    BLUETOOTH("Bluetooth BLE"),
    USB_MTP("USB Cable (MTP/PTP)")
}

enum class ConnectionStatus {
    DISCONNECTED,
    SEARCHING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

enum class TransferStatus {
    WAITING,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
    SKIPPED_DUPLICATE
}

enum class PhotoSource(val displayName: String) {
    WIFI("Wi-Fi Direct"),
    FTP("FTP Tether Server"),
    BLUETOOTH("Bluetooth"),
    USB("USB MTP/PTP Cable"),
    GALLERY("Android Gallery"),
    FILES("Files / Document Provider"),
    EXTERNAL_IMPORT("External Storage Import")
}

data class CameraDevice(
    val id: String,
    val brand: CameraBrand,
    val model: String,
    val connectionType: ConnectionType,
    val ipAddress: String = "",
    val port: Int = 0,
    val ftpUsername: String = "",
    val ftpPath: String = "",
    val bluetoothAddress: String = "",
    val signalDbm: Int = -50,
    val isAutoReconnect: Boolean = true,
    val lastConnectedTime: Long = 0L,
    val batteryPercent: Int = 100,
    val isSupported: Boolean = true,
    val supportNote: String = ""
)

data class EventItem(
    val id: String, // e.g. CAM-20261007-0001
    val name: String,
    val clientName: String,
    val date: String,
    val location: String,
    val description: String,
    val notes: String = "",
    val status: String = "ACTIVE",
    val createdTimestamp: Long = System.currentTimeMillis()
)

data class EventFolder(
    val id: String,
    val eventId: String,
    val name: String, // e.g. "Bride", "Groom", "Haldi", "Reception"
    val createdTimestamp: Long = System.currentTimeMillis()
)

data class CapturedPhoto(
    val id: String,
    val eventId: String = "",
    val folderId: String = "",
    val folderName: String = "",
    val cameraId: String = "",
    val cameraModel: String = "",
    val filename: String,
    val originalFilename: String = filename,
    val fileSizeFormatted: String,
    val fileSizeBytes: Long,
    val captureTimestamp: Long,
    val localFilePath: String, // Absolute path in storage or content URI
    val mimeType: String = "image/jpeg",
    val source: PhotoSource = PhotoSource.FTP,
    val transferStatus: TransferStatus = TransferStatus.DOWNLOADED,
    val transferProgress: Float = 1.0f,
    val fileChecksum: String = "",
    val iso: String = "N/A",
    val shutterSpeed: String = "N/A",
    val aperture: String = "N/A",
    val focalLength: String = "N/A"
)

data class TransferJob(
    val id: String,
    val photoId: String,
    val filename: String,
    val cameraModel: String,
    val progress: Float,
    val speedKbps: Int,
    val status: TransferStatus,
    val errorMessage: String? = null
)

data class UserSession(
    val userId: String,
    val email: String,
    val fullName: String,
    val token: String,
    val studioName: String = "Pro Studio"
)
