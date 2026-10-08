package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.PhotoEntity
import com.example.model.TransferStatus
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun TransfersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeTransfers by viewModel.activeTransfers.collectAsState()
    val photos by viewModel.eventPhotos.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurfaceBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Column {
                Text("NETWORK INGESTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 1.5.sp)
                Text("Transfer Manager", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        // Active Ingestion Transfers
        item {
            Text("ACTIVE TRANSFERS (${activeTransfers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }

        if (activeTransfers.isEmpty()) {
            item {
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = StatusConnected, modifier = Modifier.size(36.dp))
                        Text("All Camera Queues Idle", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("New captures from connected cameras and FTP server will stream here.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        } else {
            items(activeTransfers) { job ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(job.filename, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("${(job.progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 14.sp)
                        }

                        LinearProgressIndicator(
                            progress = { job.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = CyanAccent,
                            trackColor = DarkSurfaceCard
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${job.speedKbps / 1024} MB/s", fontSize = 12.sp, color = AmberAccent)
                            Text(job.cameraModel, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Recent Ingest Completed History
        val recentPhotos = photos.take(15)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("RECENTLY RECEIVED FILES (${recentPhotos.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }

        items(recentPhotos) { photo ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusConnected, modifier = Modifier.size(20.dp))
                        Column {
                            Text(photo.filename, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text("${photo.sourceName} • ${photo.folderName} • ${photo.fileSizeFormatted}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Text("INGESTED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusConnected)
                }
            }
        }
    }
}
