package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.EventEntity
import com.example.db.EventFolderEntity
import com.example.ui.theme.*
import com.example.util.QrCodeGenerator
import com.example.viewmodel.MainViewModel

@Composable
fun EventsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val events by viewModel.allEvents.collectAsState()
    val currentEventId by viewModel.currentEventId.collectAsState()
    val folders by viewModel.currentEventFolders.collectAsState()

    var showCreateEventDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var qrEventToView by remember { mutableStateOf<EventEntity?>(null) }

    // External Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPhotosFromUris(uris, context)
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
                    Text("EVENT DIRECTORY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 1.5.sp)
                    Text("Shoots & Folders", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = AmberAccent, contentColor = DarkSurfaceBackground),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showCreateEventDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("create_event_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Shoot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (events.isEmpty()) {
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
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(44.dp))
                        Text("No Events Created Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text(
                            "Create an event to assign incoming tethered camera photos to client folders with an auto-generated QR code.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { showCreateEventDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground)
                        ) {
                            Text("Create First Event", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(events) { ev ->
                val isSelected = ev.id == currentEventId

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DarkSurfaceCardHover else DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectEvent(ev.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(ev.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    if (isSelected) {
                                        Surface(color = CyanAccent, shape = RoundedCornerShape(4.dp)) {
                                            Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkSurfaceBackground, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text("${ev.clientName} • ${ev.date} • ${ev.location}", fontSize = 12.sp, color = TextSecondary)
                                Text("ID: ${ev.id}", fontSize = 11.sp, color = CyanAccent, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }

                            Row {
                                IconButton(onClick = { qrEventToView = ev }) {
                                    Icon(Icons.Default.QrCode2, contentDescription = "QR Code", tint = CyanAccent)
                                }
                                IconButton(onClick = { viewModel.deleteEvent(ev.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Event", tint = StatusDisconnected)
                                }
                            }
                        }

                        // Display Folders inside active event
                        if (isSelected) {
                            HorizontalDivider(color = DarkSurfaceBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("EVENT FOLDERS (${folders.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                TextButton(onClick = { showCreateFolderDialog = true }) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanAccent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Folder", fontSize = 12.sp, color = CyanAccent)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                folders.take(4).forEach { f ->
                                    Surface(
                                        color = DarkSurfaceCard,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberAccent)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(f.name, fontSize = 11.sp, color = TextPrimary)
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

    // Dialog: Create Event
    if (showCreateEventDialog) {
        var shootName by remember { mutableStateOf("") }
        var clientName by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateEventDialog = false },
            title = { Text("Create Photography Shoot", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = shootName,
                        onValueChange = { shootName = it },
                        label = { Text("Event Name (e.g. Wedding - Shakir & Sana)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                    )
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (e.g. Studio A / Grand Palace)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createEvent(shootName, clientName, "", location, "", "")
                        showCreateEventDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground)
                ) {
                    Text("Create Shoot", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateEventDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // Dialog: Create Folder
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Add Folder to Shoot", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name (e.g. Haldi, Ring Ceremony)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(folderName)
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground)
                ) {
                    Text("Add Folder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // QR Code Dialog for Event
    if (qrEventToView != null) {
        val ev = qrEventToView!!
        val qrPayload = "CAMDROP://${ev.id}/${ev.name.replace(" ", "_")}"
        val qrBitmap = remember(ev.id) {
            QrCodeGenerator.generateEventQrBitmap(qrPayload, 512)
        }

        AlertDialog(
            onDismissRequest = { qrEventToView = null },
            title = {
                Text("Unique Event QR Code", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = androidx.compose.ui.graphics.Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Event QR Code",
                            modifier = Modifier.size(240.dp).padding(12.dp)
                        )
                    }

                    Text(ev.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Text("Event ID: ${ev.id}", fontSize = 12.sp, color = CyanAccent, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("Scan to route incoming camera transfers or guest access directly to this event.", fontSize = 11.sp, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            },
            confirmButton = {
                Button(
                    onClick = { qrEventToView = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkSurfaceBackground)
                ) {
                    Text("Done")
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}
