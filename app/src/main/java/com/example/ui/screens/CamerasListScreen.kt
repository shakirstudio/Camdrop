package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraBrand
import com.example.model.CameraDevice
import com.example.model.ConnectionType
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun CamerasListScreen(
    viewModel: MainViewModel,
    onNavigateToConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedCameras by viewModel.savedCameras.collectAsState()
    val connectedCamera by viewModel.connectedCamera.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurfaceBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PRO HARDWARE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 1.5.sp)
                    Text("Configured Cameras", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Button(
                    onClick = onNavigateToConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("cameras_add_camera_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (savedCameras.isEmpty() && connectedCamera == null) {
            item {
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(44.dp))
                        Text("No Configured Cameras", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text(
                            "Add your Sony, Canon, or Nikon camera to enable instant Wi-Fi or FTP tethering.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToConnect,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Setup Camera Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(savedCameras) { cameraEntity ->
                val isCurrentlyConnected = connectedCamera?.id == cameraEntity.id

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Camera,
                                        contentDescription = null,
                                        tint = if (isCurrentlyConnected) CyanAccent else TextTertiary
                                    )
                                }
                                Column {
                                    Text(cameraEntity.model, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text("${cameraEntity.brandName} • ${cameraEntity.connectionTypeName}", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            if (isCurrentlyConnected) {
                                Surface(
                                    color = StatusConnected.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = StatusConnected,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isCurrentlyConnected) {
                                OutlinedButton(
                                    onClick = { viewModel.disconnectCamera() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDisconnected)
                                ) {
                                    Text("Disconnect")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val brand = runCatching { CameraBrand.valueOf(cameraEntity.brandName) }.getOrDefault(CameraBrand.SONY)
                                        val connType = runCatching { ConnectionType.valueOf(cameraEntity.connectionTypeName) }.getOrDefault(ConnectionType.WIFI)
                                        viewModel.connectCamera(
                                            CameraDevice(
                                                id = cameraEntity.id,
                                                brand = brand,
                                                model = cameraEntity.model,
                                                connectionType = connType,
                                                ipAddress = cameraEntity.ipAddress,
                                                port = cameraEntity.port
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground)
                                ) {
                                    Text("Connect", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
