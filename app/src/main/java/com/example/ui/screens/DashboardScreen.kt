package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ConnectionStatus
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.NetworkUtils
import com.example.viewmodel.MainViewModel
import java.io.File

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToConnect: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToPhotos: () -> Unit,
    onNavigateToTransfers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val connectedCamera by viewModel.connectedCamera.collectAsState()
    val photos by viewModel.eventPhotos.collectAsState()
    val events by viewModel.allEvents.collectAsState()
    val currentEventId by viewModel.currentEventId.collectAsState()
    val activeTransfers by viewModel.activeTransfers.collectAsState()
    val isFtpRunning by viewModel.isFtpServerRunning.collectAsState()

    val currentEvent = events.find { it.id == currentEventId }

    val (storageUsedBytes, storageFreeBytes) = remember(photos) {
        NetworkUtils.getStorageUsage(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurfaceBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Studio Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CAMDROP PRO PLATFORM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Live Tether Dashboard",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                StatusBadge(status = connectionStatus)
            }
        }

        // Active Event & Client Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CURRENT ACTIVE SHOOT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                            Text(
                                text = currentEvent?.name ?: "No Event Selected",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (currentEvent != null) {
                                Text("Client: ${currentEvent.clientName} • ID: ${currentEvent.id}", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Button(
                            onClick = onNavigateToEvents,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard, contentColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Switch Shoot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Hardware Connection Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = null,
                                    tint = if (connectedCamera != null) CyanAccent else TextTertiary
                                )
                            }
                            Column {
                                Text(
                                    text = connectedCamera?.model ?: "No Active Camera Connected",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (connectedCamera != null)
                                        "${connectedCamera?.brand?.displayName} • ${connectedCamera?.connectionType?.displayName}"
                                    else "Ready for FTP Server / Wi-Fi Direct / USB Cable",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (connectedCamera != null) {
                            FilledTonalButton(
                                onClick = { viewModel.disconnectCamera() },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = StatusDisconnected.copy(alpha = 0.2f),
                                    contentColor = StatusDisconnected
                                )
                            ) {
                                Text("Disconnect", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceBorder)

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${photos.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Received Photos", fontSize = 11.sp, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${activeTransfers.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                            Text(text = "Active Ingests", fontSize = 11.sp, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isFtpRunning) "ACTIVE" else "IDLE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFtpRunning) StatusConnected else TextTertiary
                            )
                            Text(text = "FTP Tether", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    // Primary Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToConnect,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera Setup", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToPhotos,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery (${photos.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Embedded FTP Tether Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Android FTP Camera Receiver", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isFtpRunning) StatusConnected else TextTertiary)
                            )
                        }
                        Text(
                            text = if (isFtpRunning)
                                "Phone IP: ${NetworkUtils.getLocalIpAddress()}:2121 (Active in background)"
                            else "Start server to ingest Sony/Canon/Nikon photos automatically",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    FilledIconToggleButton(
                        checked = isFtpRunning,
                        onCheckedChange = { checked ->
                            if (checked) viewModel.startFtpServer() else viewModel.stopFtpServer()
                        },
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            checkedContainerColor = StatusConnected,
                            containerColor = DarkSurfaceCard
                        )
                    ) {
                        Icon(if (isFtpRunning) Icons.Default.CloudDone else Icons.Default.CloudOff, contentDescription = null)
                    }
                }
            }
        }

        // Live Photos Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RECENTLY INGESTED FRAMES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextSecondary, letterSpacing = 1.sp)
                    if (photos.isNotEmpty()) {
                        Text(
                            text = "View All (${photos.size})",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToPhotos() }
                        )
                    }
                }

                if (photos.isEmpty()) {
                    Surface(
                        color = DarkSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(32.dp))
                            Text("No photographs ingested yet", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(photos.take(6)) { p ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(120.dp)
                                    .clickable { onNavigateToPhotos() }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = File(p.localFilePath),
                                        contentDescription = p.filename,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(DarkSurfaceBackground.copy(alpha = 0.8f))
                                            .padding(4.dp)
                                    ) {
                                        Text(p.filename, color = TextPrimary, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Storage Details
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DEVICE STORAGE METRICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("App Ingested Cache:", fontSize = 13.sp, color = TextPrimary)
                        Text(String.format("%.1f MB", storageUsedBytes / (1024.0 * 1024.0)), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Phone Storage Free:", fontSize = 13.sp, color = TextPrimary)
                        Text(String.format("%.1f GB", storageFreeBytes / (1024.0 * 1024.0 * 1024.0)), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusConnected)
                    }
                }
            }
        }
    }
}
