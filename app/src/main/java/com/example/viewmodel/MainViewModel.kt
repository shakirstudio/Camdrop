package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthenticationService
import com.example.bluetooth.RealBluetoothCameraScanner
import com.example.camera.CameraProvider
import com.example.camera.CameraProviderFactory
import com.example.db.*
import com.example.ftp.CameraFtpReceiverServer
import com.example.model.*
import com.example.service.TransferForegroundService
import com.example.usb.UsbCameraManager
import com.example.util.NetworkUtils
import com.example.util.QrCodeGenerator
import com.example.wifi.RealWifiCameraDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val authService = AuthenticationService(application)
    private val db = AppDatabase.getInstance(application)
    private val eventDao = db.eventDao()
    private val folderDao = db.eventFolderDao()
    private val photoDao = db.photoDao()
    private val cameraDao = db.cameraDao()
    private val providerFactory = CameraProviderFactory(application)

    // Hardware Managers
    val bluetoothScanner = RealBluetoothCameraScanner(application)
    val usbCameraManager = UsbCameraManager(application)
    val wifiDetector = RealWifiCameraDetector(application)

    // Active Camera Provider
    private var activeProvider: CameraProvider? = null

    // UI Connection & Transfers State
    val connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val activeTransfers = MutableStateFlow<List<TransferJob>>(emptyList())
    val connectedCamera = MutableStateFlow<CameraDevice?>(null)

    // Discovery lists
    val discoveredCameras = MutableStateFlow<List<CameraDevice>>(emptyList())
    val isScanning = MutableStateFlow(false)

    // Selection
    val selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())

    // Database flows
    val allEvents: StateFlow<List<EventEntity>> = eventDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentEventId = MutableStateFlow<String?>(null)
    val currentFolderId = MutableStateFlow<String?>(null)

    val currentEventFolders: StateFlow<List<EventFolderEntity>> = currentEventId
        .flatMapLatest { id ->
            if (id != null) folderDao.getFoldersForEvent(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eventPhotos: StateFlow<List<PhotoEntity>> = currentEventId
        .flatMapLatest { evId ->
            if (evId != null) photoDao.getPhotosForEvent(evId) else photoDao.getAllPhotos()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedCameras: StateFlow<List<SavedCameraEntity>> = cameraDao.getAllCameras()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Embedded Real FTP Server for Direct Camera Tethering
    private val ftpServer = CameraFtpReceiverServer(
        context = application,
        port = 2121
    ) { file, filename, size ->
        viewModelScope.launch {
            ingestReceivedPhotoFile(
                sourceFile = file,
                originalName = filename,
                source = PhotoSource.FTP,
                cameraModel = connectedCamera.value?.model ?: "FTP Camera"
            )
        }
    }
    val isFtpServerRunning = MutableStateFlow(false)

    // Diagnostic log & Notifications
    val diagnosticLogs = MutableStateFlow<List<String>>(listOf("[INIT] CamDrop Pro Engine initialized."))
    val userNotification = MutableSharedFlow<String>()

    init {
        // Auto-select or create default event if none exists
        viewModelScope.launch {
            allEvents.collect { events ->
                if (events.isNotEmpty() && currentEventId.value == null) {
                    currentEventId.value = events.first().id
                }
            }
        }
    }

    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val log = "[$time] $msg"
        diagnosticLogs.value = (listOf(log) + diagnosticLogs.value).take(150)
    }

    // ─── EVENT & FOLDER MANAGEMENT ──────────────────────────────────────────────

    fun createEvent(
        name: String,
        clientName: String,
        date: String,
        location: String,
        description: String,
        notes: String
    ) {
        viewModelScope.launch {
            val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val randomSuffix = (1000..9999).random()
            val newEventId = "CAM-$datePart-$randomSuffix"

            val event = EventEntity(
                id = newEventId,
                name = name.ifBlank { "Untitled Shoot" },
                clientName = clientName.ifBlank { "Client" },
                date = date.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) },
                location = location.ifBlank { "Studio" },
                description = description,
                notes = notes,
                status = "ACTIVE",
                createdTimestamp = System.currentTimeMillis()
            )

            eventDao.insertEvent(event)

            // Auto-create standard photography folders
            val standardFolders = listOf("Bride", "Groom", "Wedding", "Reception", "Candid", "Selected Photos")
            standardFolders.forEach { fName ->
                folderDao.insertFolder(
                    EventFolderEntity(
                        id = UUID.randomUUID().toString(),
                        eventId = newEventId,
                        name = fName,
                        createdTimestamp = System.currentTimeMillis()
                    )
                )
            }

            currentEventId.value = newEventId
            userNotification.emit("Created Event $newEventId")
            addLog("Created Event: ${event.name} ($newEventId) with standard folders.")
        }
    }

    fun selectEvent(eventId: String) {
        currentEventId.value = eventId
        currentFolderId.value = null
        addLog("Switched active event to $eventId")
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            eventDao.deleteEvent(eventId)
            if (currentEventId.value == eventId) {
                currentEventId.value = allEvents.value.firstOrNull { it.id != eventId }?.id
            }
            userNotification.emit("Event deleted.")
        }
    }

    fun createFolder(name: String) {
        val evId = currentEventId.value ?: return
        viewModelScope.launch {
            val f = EventFolderEntity(
                id = UUID.randomUUID().toString(),
                eventId = evId,
                name = name.trim().ifEmpty { "New Folder" },
                createdTimestamp = System.currentTimeMillis()
            )
            folderDao.insertFolder(f)
            userNotification.emit("Folder '${f.name}' created.")
            addLog("Added folder ${f.name} to event $evId")
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            folderDao.renameFolder(folderId, newName)
            userNotification.emit("Folder renamed to $newName")
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            folderDao.deleteFolder(folderId)
            if (currentFolderId.value == folderId) currentFolderId.value = null
            userNotification.emit("Folder deleted.")
        }
    }

    fun selectFolder(folderId: String?) {
        currentFolderId.value = folderId
    }

    // ─── REAL PHOTO INGESTION & DUPLICATE DETECTION ────────────────────────────

    suspend fun ingestReceivedPhotoFile(
        sourceFile: File,
        originalName: String,
        source: PhotoSource,
        cameraModel: String
    ): Result<PhotoEntity> = withContext(Dispatchers.IO) {
        try {
            val evId = currentEventId.value ?: run {
                // Ensure default event exists
                val defId = "CAM-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}-0001"
                eventDao.insertEvent(
                    EventEntity(defId, "Default Event", "Studio Client", "Today", "Studio", "Auto-created", "", "ACTIVE", System.currentTimeMillis())
                )
                currentEventId.value = defId
                defId
            }

            val folders = currentEventFolders.value
            val folderId = currentFolderId.value ?: folders.firstOrNull()?.id ?: run {
                val f = EventFolderEntity(UUID.randomUUID().toString(), evId, "Main Ingest", System.currentTimeMillis())
                folderDao.insertFolder(f)
                f.id
            }
            val folderName = folders.find { it.id == folderId }?.name ?: "Main Ingest"

            // Check duplicate by MD5 Checksum
            val checksum = QrCodeGenerator.calculateFileChecksum(sourceFile)
            val existing = photoDao.findPhotoByChecksum(checksum)
            if (existing != null) {
                addLog("Duplicate photo detected ($originalName matches ${existing.filename}). Skipping.")
                userNotification.emit("Skipped duplicate photo: $originalName")
                return@withContext Result.failure(IllegalStateException("Duplicate photo skipped"))
            }

            // Copy to secure permanent app-private photo storage
            val photosDir = File(getApplication<Application>().filesDir, "photos/$evId").apply { if (!exists()) mkdirs() }
            val cleanName = originalName.replace(" ", "_")
            val targetFile = File(photosDir, cleanName)

            sourceFile.inputStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val photoEntity = PhotoEntity(
                id = UUID.randomUUID().toString(),
                eventId = evId,
                folderId = folderId,
                folderName = folderName,
                cameraId = connectedCamera.value?.id ?: "unknown-cam",
                cameraModel = cameraModel,
                filename = cleanName,
                originalFilename = originalName,
                fileSizeFormatted = String.format(Locale.US, "%.1f MB", targetFile.length() / (1024.0 * 1024.0)),
                fileSizeBytes = targetFile.length(),
                captureTimestamp = System.currentTimeMillis(),
                localFilePath = targetFile.absolutePath,
                mimeType = "image/jpeg",
                sourceName = source.name,
                transferStatusName = TransferStatus.DOWNLOADED.name,
                fileChecksum = checksum,
                iso = "ISO 400",
                shutterSpeed = "1/250s",
                aperture = "f/2.8",
                focalLength = "50mm"
            )

            photoDao.insertPhoto(photoEntity)
            addLog("Ingested $cleanName from ${source.displayName} into $folderName.")
            userNotification.emit("Photo received: $cleanName")
            Result.success(photoEntity)
        } catch (e: Exception) {
            addLog("Error ingesting photo: ${e.message}")
            Result.failure(e)
        }
    }

    // ─── EXTERNAL / GALLERY / FILE PICKER IMPORT ──────────────────────────────

    fun importPhotosFromUris(uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            var importedCount = 0
            val evId = currentEventId.value ?: return@launch
            val contentResolver = context.contentResolver

            for (uri in uris) {
                try {
                    val filename = "IMG_IMPORT_${System.currentTimeMillis()}_${(100..999).random()}.JPG"
                    val tempFile = File(context.cacheDir, filename)

                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0) {
                        val res = ingestReceivedPhotoFile(
                            sourceFile = tempFile,
                            originalName = filename,
                            source = PhotoSource.GALLERY,
                            cameraModel = "Imported File"
                        )
                        if (res.isSuccess) importedCount++
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    addLog("Failed importing file from URI: ${e.message}")
                }
            }

            userNotification.emit("Successfully imported $importedCount photo(s).")
            addLog("External import complete: $importedCount files added to event $evId.")
        }
    }

    // ─── HARDWARE CONNECTIVITY ──────────────────────────────────────────────────

    fun startFtpServer() {
        viewModelScope.launch {
            val res = ftpServer.startServer()
            if (res.isSuccess) {
                isFtpServerRunning.value = true
                TransferForegroundService.startService(getApplication())
                val ip = NetworkUtils.getLocalIpAddress()
                addLog("Real FTP Server running on $ip:2121. Configure Camera FTP to point to $ip:2121")
                userNotification.emit("Tether Server running on $ip:2121")
            } else {
                addLog("FTP Server error: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun stopFtpServer() {
        ftpServer.stopServer()
        isFtpServerRunning.value = false
        TransferForegroundService.stopService(getApplication())
        addLog("FTP Tether Server stopped.")
    }

    fun scanForCameras(brand: CameraBrand) {
        viewModelScope.launch {
            isScanning.value = true
            addLog("Scanning for $brand on local Wi-Fi subnet...")
            val list = mutableListOf<CameraDevice>()

            // 1. Check Wi-Fi subnet for real open ports
            val wifiDevices = wifiDetector.probeConnectedCameraSubnet()
            list.addAll(wifiDevices.filter { it.brand == brand || brand == CameraBrand.GENERIC })

            // 2. Check USB connected cameras
            val usbDevices = usbCameraManager.scanConnectedUsbDevices()
            list.addAll(usbDevices.filter { it.brand == brand || brand == CameraBrand.GENERIC })

            discoveredCameras.value = list
            isScanning.value = false
            addLog("Scan complete. Detected ${list.size} active device(s).")
        }
    }

    fun connectCamera(device: CameraDevice) {
        viewModelScope.launch {
            connectionStatus.value = ConnectionStatus.CONNECTING
            addLog("Establishing connection to ${device.brand.displayName} ${device.model} (${device.ipAddress}:${device.port})...")

            val provider = providerFactory.createProvider(device.brand)
            activeProvider = provider

            val result = provider.connect(device)
            if (result.isSuccess) {
                connectionStatus.value = ConnectionStatus.CONNECTED
                connectedCamera.value = device
                TransferForegroundService.startService(getApplication())
                addLog("Connected to ${device.model}! Tethering active.")
                userNotification.emit("Connected to ${device.model}")

                cameraDao.insertCamera(
                    SavedCameraEntity(
                        id = device.id,
                        brandName = device.brand.name,
                        model = device.model,
                        connectionTypeName = device.connectionType.name,
                        ipAddress = device.ipAddress,
                        port = device.port,
                        ftpUsername = device.ftpUsername,
                        ftpPath = device.ftpPath,
                        isAutoReconnect = device.isAutoReconnect,
                        lastConnectedTime = System.currentTimeMillis()
                    )
                )
            } else {
                connectionStatus.value = ConnectionStatus.ERROR
                val err = result.exceptionOrNull()?.message ?: "Connection failed"
                addLog("Connection failed: $err")
                userNotification.emit(err)
            }
        }
    }

    fun disconnectCamera() {
        viewModelScope.launch {
            activeProvider?.disconnect()
            activeProvider = null
            connectedCamera.value = null
            connectionStatus.value = ConnectionStatus.DISCONNECTED
            TransferForegroundService.stopService(getApplication())
            addLog("Camera disconnected.")
            userNotification.emit("Camera disconnected.")
        }
    }

    fun triggerShutterOrFetch() {
        viewModelScope.launch {
            val provider = activeProvider
            if (provider == null || connectionStatus.value != ConnectionStatus.CONNECTED) {
                userNotification.emit("No active camera connected.")
                return@launch
            }
            addLog("Triggering tether capture & fetch...")
            val res = provider.triggerManualFetch()
            if (res.isFailure) {
                val err = res.exceptionOrNull()?.message ?: "Fetch error"
                addLog("Fetch error: $err")
                userNotification.emit(err)
            }
        }
    }

    // ─── GALLERY, SHARING & SELECTION ──────────────────────────────────────────

    fun togglePhotoSelection(photoId: String) {
        val curr = selectedPhotoIds.value.toMutableSet()
        if (curr.contains(photoId)) curr.remove(photoId) else curr.add(photoId)
        selectedPhotoIds.value = curr
    }

    fun selectAllPhotos() {
        selectedPhotoIds.value = eventPhotos.value.map { it.id }.toSet()
    }

    fun clearPhotoSelection() {
        selectedPhotoIds.value = emptySet()
    }

    fun deleteSelectedPhotos() {
        viewModelScope.launch {
            val toDelete = selectedPhotoIds.value
            toDelete.forEach { id ->
                val p = eventPhotos.value.find { it.id == id }
                if (p != null) {
                    File(p.localFilePath).delete()
                    photoDao.deletePhoto(id)
                }
            }
            selectedPhotoIds.value = emptySet()
            userNotification.emit("Deleted ${toDelete.size} photo(s)")
            addLog("Deleted ${toDelete.size} photos.")
        }
    }

    fun moveSelectedPhotosToFolder(targetFolderId: String, targetFolderName: String) {
        viewModelScope.launch {
            val ids = selectedPhotoIds.value
            ids.forEach { id ->
                photoDao.movePhotoToFolder(id, targetFolderId, targetFolderName)
            }
            selectedPhotoIds.value = emptySet()
            userNotification.emit("Moved ${ids.size} photo(s) to $targetFolderName")
            addLog("Moved ${ids.size} photos to $targetFolderName")
        }
    }

    fun sharePhotos(context: Context, photosToShare: List<PhotoEntity>) {
        if (photosToShare.isEmpty()) return
        val uris = ArrayList<Uri>()
        val packageName = context.packageName

        photosToShare.forEach { p ->
            val file = File(p.localFilePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "$packageName.fileprovider", file)
                uris.add(uri)
            }
        }

        if (uris.isEmpty()) return

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        val chooser = Intent.createChooser(intent, "Share ${photosToShare.size} Photos")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun clearAppCache() {
        viewModelScope.launch {
            val photosDir = File(getApplication<Application>().filesDir, "photos")
            photosDir.deleteRecursively()
            allEvents.value.forEach { ev ->
                eventDao.deleteEvent(ev.id)
            }
            addLog("Cache and photo files cleared.")
            userNotification.emit("Storage cache cleared.")
        }
    }
}
