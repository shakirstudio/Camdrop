package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraBrand
import com.example.model.CameraDevice
import com.example.model.ConnectionType
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.NetworkUtils
import com.example.viewmodel.MainViewModel

@Composable
fun CameraConnectScreen(
    viewModel: MainViewModel,
    onConnected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf(ConnectionType.FTP) }
    var selectedBrand by remember { mutableStateOf(CameraBrand.SONY) }

    // Direct Tether IP
    var customIp by remember { mutableStateOf("") }
    var customPort by remember { mutableStateOf("") }

    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredCameras by viewModel.discoveredCameras.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isFtpRunning by viewModel.isFtpServerRunning.collectAsState()

    // Bluetooth scanner state
    val bleScanning by viewModel.bluetoothScanner.isScanning.collectAsState()
    val bleDevices by viewModel.bluetoothScanner.discoveredDevices.collectAsState()
    val bleStatusMsg by viewModel.bluetoothScanner.statusMessage.collectAsState()

    // USB state
    var usbDevices by remember { mutableStateOf<List<CameraDevice>>(emptyList()) }

    // Bluetooth permission request launcher
    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            viewModel.bluetoothScanner.startBleScan()
        } else {
            viewModel.addLog("Bluetooth permissions denied by user.")
        }
    }

    LaunchedEffect(connectionStatus) {
        if (connectionStatus == com.example.model.ConnectionStatus.CONNECTED) {
            onConnected()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurfaceBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("REAL HARDWARE CONNECTIVITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 1.5.sp)
                    Text("Camera Tether Setup", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                StatusBadge(status = connectionStatus)
            }
        }

        // Connection Protocol Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = when (selectedMethod) {
                    ConnectionType.FTP -> 0
                    ConnectionType.WIFI -> 1
                    ConnectionType.USB_MTP -> 2
                    ConnectionType.BLUETOOTH -> 3
                },
                containerColor = DarkSurfaceElevated,
                contentColor = CyanAccent,
                edgePadding = 0.dp,
                divider = {}
            ) {
                Tab(
                    selected = selectedMethod == ConnectionType.FTP,
                    onClick = { selectedMethod = ConnectionType.FTP },
                    text = { Text("1. FTP Server", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedMethod == ConnectionType.WIFI,
                    onClick = { selectedMethod = ConnectionType.WIFI },
                    text = { Text("2. Wi-Fi Direct", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedMethod == ConnectionType.USB_MTP,
                    onClick = {
                        selectedMethod = ConnectionType.USB_MTP
                        usbDevices = viewModel.usbCameraManager.scanConnectedUsbDevices()
                    },
                    text = { Text("3. USB Cable", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedMethod == ConnectionType.BLUETOOTH,
                    onClick = { selectedMethod = ConnectionType.BLUETOOTH },
                    text = { Text("4. Bluetooth", fontWeight = FontWeight.Bold) }
                )
            }
        }

        // ─── TAB 1: FTP SERVER ───────────────────────────────────────────────
        if (selectedMethod == ConnectionType.FTP) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Android Embedded FTP Server", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text("Standard tether protocol for Sony, Canon & Nikon", fontSize = 12.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = isFtpRunning,
                                onCheckedChange = { if (it) viewModel.startFtpServer() else viewModel.stopFtpServer() },
                                colors = SwitchDefaults.colors(checkedThumbColor = StatusConnected)
                            )
                        }

                        HorizontalDivider(color = DarkSurfaceBorder)

                        val phoneIp = NetworkUtils.getLocalIpAddress()
                        Text("CAMERA FTP SETTINGS CONFIGURATION:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAccent)

                        Surface(color = DarkSurfaceCard, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Server Host/IP:", fontSize = 13.sp, color = TextSecondary)
                                    Text(phoneIp, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Port:", fontSize = 13.sp, color = TextSecondary)
                                    Text("2121", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Username / Password:", fontSize = 13.sp, color = TextSecondary)
                                    Text("pro / studio", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Mode:", fontSize = 13.sp, color = TextSecondary)
                                    Text("Passive (PASV)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }

                        Text("Step 1: Turn on camera FTP function in camera menu.\nStep 2: Enter the IP and Port above.\nStep 3: Photos shot will stream directly into the active shoot folder.", fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
                    }
                }
            }
        }

        // ─── TAB 2: WI-FI DIRECT / LOCAL SUBNET ──────────────────────────────
        if (selectedMethod == ConnectionType.WIFI) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Wi-Fi Camera Direct Subnet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text("Current Wi-Fi: ${viewModel.wifiDetector.getCurrentSsid()}", fontSize = 12.sp, color = CyanAccent)
                            }
                            Button(
                                onClick = { viewModel.scanForCameras(selectedBrand) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground)
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Scan Subnet", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Brand Selector
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CameraBrand.values().take(3).forEach { b ->
                                Surface(
                                    color = if (selectedBrand == b) CyanAccent else DarkSurfaceCard,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).clickable { selectedBrand = b }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                        Text(b.displayName, color = if (selectedBrand == b) DarkSurfaceBackground else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        if (discoveredCameras.isEmpty()) {
                            Surface(color = DarkSurfaceCard, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("No camera responding on candidate ports. Connect phone to Camera Wi-Fi hotspot or enter fixed IP below.", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(14.dp))
                            }
                        } else {
                            discoveredCameras.forEach { dev ->
                                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(dev.model, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                            Text("${dev.ipAddress}:${dev.port}", fontSize = 12.sp, color = CyanAccent)
                                        }
                                        Button(onClick = { viewModel.connectCamera(dev) }) { Text("Connect") }
                                    }
                                }
                            }
                        }

                        // Manual Fixed IP Option
                        OutlinedTextField(
                            value = customIp,
                            onValueChange = { customIp = it },
                            label = { Text("Manual Camera IP (e.g. 192.168.1.150)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customPort,
                            onValueChange = { customPort = it },
                            label = { Text("Port (8080 / 15740)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val dev = CameraDevice(
                                    id = "manual-${System.currentTimeMillis()}",
                                    brand = selectedBrand,
                                    model = "${selectedBrand.displayName} Direct Tether",
                                    connectionType = ConnectionType.WIFI,
                                    ipAddress = customIp.ifBlank { "192.168.1.100" },
                                    port = customPort.toIntOrNull() ?: 8080
                                )
                                viewModel.connectCamera(dev)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground)
                        ) {
                            Text("Connect Manual IP", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ─── TAB 3: USB CABLE / MTP ──────────────────────────────────────────
        if (selectedMethod == ConnectionType.USB_MTP) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("USB Cable / OTG Tether", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text("Physical USB-C cable to camera PTP/MTP storage", fontSize = 12.sp, color = TextSecondary)
                            }
                            IconButton(onClick = { usbDevices = viewModel.usbCameraManager.scanConnectedUsbDevices() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh USB", tint = CyanAccent)
                            }
                        }

                        if (usbDevices.isEmpty()) {
                            Surface(color = DarkSurfaceCard, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("No USB camera detected.", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text("1. Connect camera to phone with USB-C cable / OTG adapter.\n2. In camera menu, set USB Connection to 'MTP' or 'PC Remote'.\n3. Tap Refresh.", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        } else {
                            usbDevices.forEach { dev ->
                                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(dev.model, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                        Text(dev.supportNote, fontSize = 12.sp, color = CyanAccent)
                                        Button(
                                            onClick = { viewModel.connectCamera(dev) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground)
                                        ) {
                                            Text("Open USB Camera", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── TAB 4: BLUETOOTH / BLE ──────────────────────────────────────────
        if (selectedMethod == ConnectionType.BLUETOOTH) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Bluetooth BLE Camera Discovery", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text("Pairing & Camera Power Control", fontSize = 12.sp, color = TextSecondary)
                            }
                            Button(
                                onClick = {
                                    if (bleScanning) {
                                        viewModel.bluetoothScanner.stopBleScan()
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            btPermissionLauncher.launch(
                                                arrayOf(
                                                    android.Manifest.permission.BLUETOOTH_SCAN,
                                                    android.Manifest.permission.BLUETOOTH_CONNECT,
                                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                                                )
                                            )
                                        } else {
                                            viewModel.bluetoothScanner.startBleScan()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (bleScanning) StatusDisconnected else CyanAccent,
                                    contentColor = DarkSurfaceBackground
                                )
                            ) {
                                Text(if (bleScanning) "Stop Scan" else "Scan BLE", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(bleStatusMsg, fontSize = 12.sp, color = AmberAccent)

                        if (bleDevices.isEmpty()) {
                            Surface(color = DarkSurfaceCard, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("No nearby discoverable BLE cameras. Note: High-resolution photo ingestion operates via Wi-Fi/FTP/USB.", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(14.dp))
                            }
                        } else {
                            bleDevices.forEach { dev ->
                                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(dev.model, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                        Text("MAC: ${dev.bluetoothAddress} | RSSI: ${dev.signalDbm} dBm", fontSize = 11.sp, color = CyanAccent)
                                        Text(dev.supportNote, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
