package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.NetworkUtils
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var autoReconnect by remember { mutableStateOf(true) }
    var highResTether by remember { mutableStateOf(true) }
    var soundVibrateFeedback by remember { mutableStateOf(true) }

    val userSession by viewModel.authService.currentUser.collectAsState()
    val diagnosticLogs by viewModel.diagnosticLogs.collectAsState()
    var isDiagnosticOpen by remember { mutableStateOf(false) }

    val (storageUsedBytes, storageFreeBytes) = remember {
        NetworkUtils.getStorageUsage(context)
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
            Column {
                Text("CONFIGURATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 1.5.sp)
                Text("Studio Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        // Profile Card
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
                    Column {
                        Text(userSession?.fullName ?: "Studio Photographer", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text(userSession?.email ?: "pro@photography.studio", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Active Token: TLS Session Encrypted", fontSize = 11.sp, color = StatusConnected)
                    }
                    Button(
                        onClick = {
                            viewModel.authService.logout()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusDisconnected.copy(alpha = 0.2f), contentColor = StatusDisconnected),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("settings_logout_btn")
                    ) {
                        Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Tether Preferences
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("TETHER & TRANSFER PREFERENCES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Automatic Reconnect", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Resume tether when camera powers back on", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = autoReconnect,
                            onCheckedChange = { autoReconnect = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = DarkSurfaceCard)
                        )
                    }

                    HorizontalDivider(color = DarkSurfaceBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Full Quality RAW+JPEG Ingest", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Download full fidelity image without downsampling", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = highResTether,
                            onCheckedChange = { highResTether = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = DarkSurfaceCard)
                        )
                    }

                    HorizontalDivider(color = DarkSurfaceBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Haptic Feedback on Ingest", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Vibrate phone when new photo is saved", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = soundVibrateFeedback,
                            onCheckedChange = { soundVibrateFeedback = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = DarkSurfaceCard)
                        )
                    }
                }
            }
        }

        // Storage Management Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("LOCAL STORAGE CONTROL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Text("Photos are stored securely on local phone storage without unwanted cloud uploads.", fontSize = 12.sp, color = TextSecondary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cache Used:", fontSize = 13.sp, color = TextPrimary)
                        Text(String.format("%.1f MB", storageUsedBytes / (1024.0 * 1024.0)), fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { viewModel.clearAppCache() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard, contentColor = AmberAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("clear_cache_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Local Photo Cache", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Internal Developer Diagnostics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tether Diagnostic Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Real network & protocol handshake trace", fontSize = 12.sp, color = TextSecondary)
                        }
                        IconButton(onClick = { isDiagnosticOpen = !isDiagnosticOpen }) {
                            Icon(if (isDiagnosticOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary)
                        }
                    }

                    if (isDiagnosticOpen) {
                        Surface(
                            color = DarkSurfaceBackground,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(diagnosticLogs) { log ->
                                    Text(log, fontSize = 11.sp, color = CyanAccent, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
