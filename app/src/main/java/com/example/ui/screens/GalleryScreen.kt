package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.db.PhotoEntity
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photos by viewModel.eventPhotos.collectAsState()
    val selectedPhotoIds by viewModel.selectedPhotoIds.collectAsState()
    val folders by viewModel.currentEventFolders.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()

    var previewPhoto by remember { mutableStateOf<PhotoEntity?>(null) }
    var showMoveFolderDialog by remember { mutableStateOf(false) }

    if (previewPhoto != null) {
        BackHandler { previewPhoto = null }
    }

    val displayedPhotos = remember(photos, currentFolderId) {
        if (currentFolderId == null) photos else photos.filter { it.folderId == currentFolderId }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkSurfaceBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("EVENT GALLERY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 1.sp)
                        Text(
                            text = if (selectedPhotoIds.isEmpty()) "Photos (${displayedPhotos.size})" else "Selected: ${selectedPhotoIds.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurfaceElevated,
                    actionIconContentColor = TextPrimary
                ),
                actions = {
                    if (selectedPhotoIds.isNotEmpty()) {
                        IconButton(onClick = { showMoveFolderDialog = true }) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move to folder", tint = AmberAccent)
                        }
                        IconButton(
                            onClick = {
                                val selectedList = displayedPhotos.filter { selectedPhotoIds.contains(it.id) }
                                viewModel.sharePhotos(context, selectedList)
                            },
                            modifier = Modifier.testTag("gallery_share_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = CyanAccent)
                        }
                        IconButton(
                            onClick = { viewModel.deleteSelectedPhotos() },
                            modifier = Modifier.testTag("gallery_delete_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusDisconnected)
                        }
                        IconButton(onClick = { viewModel.clearPhotoSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Deselect")
                        }
                    } else if (displayedPhotos.isNotEmpty()) {
                        IconButton(onClick = { viewModel.selectAllPhotos() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Folder Filter Row
                if (folders.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = if (currentFolderId == null) 0 else folders.indexOfFirst { it.id == currentFolderId } + 1,
                        containerColor = DarkSurfaceElevated,
                        contentColor = CyanAccent,
                        edgePadding = 16.dp,
                        divider = {}
                    ) {
                        Tab(
                            selected = currentFolderId == null,
                            onClick = { viewModel.selectFolder(null) },
                            text = { Text("All Folders (${photos.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        folders.forEach { f ->
                            val count = photos.count { it.folderId == f.id }
                            Tab(
                                selected = currentFolderId == f.id,
                                onClick = { viewModel.selectFolder(f.id) },
                                text = { Text("${f.name} ($count)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                if (displayedPhotos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Text("No photographs in this event folder", color = TextSecondary, fontSize = 14.sp)
                            Text("Tether camera or tap 'Import' to ingest photos.", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(displayedPhotos, key = { it.id }) { photo ->
                            val isSelected = selectedPhotoIds.contains(photo.id)

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceElevated)
                                    .clickable {
                                        if (selectedPhotoIds.isNotEmpty()) {
                                            viewModel.togglePhotoSelection(photo.id)
                                        } else {
                                            previewPhoto = photo
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = File(photo.localFilePath),
                                    contentDescription = photo.filename,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Checkbox overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) CyanAccent else DarkSurfaceBackground.copy(alpha = 0.6f))
                                        .clickable { viewModel.togglePhotoSelection(photo.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = DarkSurfaceBackground, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Bottom metadata badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(DarkSurfaceBackground.copy(alpha = 0.8f))
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = "${photo.sourceName} • ${photo.filename}",
                                        color = TextPrimary,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Move Folder Dialog
            if (showMoveFolderDialog) {
                AlertDialog(
                    onDismissRequest = { showMoveFolderDialog = false },
                    title = { Text("Move Selected to Folder", color = TextPrimary) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            folders.forEach { f ->
                                Button(
                                    onClick = {
                                        viewModel.moveSelectedPhotosToFolder(f.id, f.name)
                                        showMoveFolderDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard)
                                ) {
                                    Text(f.name, color = CyanAccent)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showMoveFolderDialog = false }) { Text("Cancel", color = TextSecondary) }
                    },
                    containerColor = DarkSurfaceElevated
                )
            }

            // Full-screen Viewer Dialog
            if (previewPhoto != null) {
                val p = previewPhoto!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { previewPhoto = null }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { previewPhoto = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.sharePhotos(context, listOf(p)) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = CyanAccent)
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = File(p.localFilePath),
                                contentDescription = p.filename,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Surface(color = DarkSurfaceElevated, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(p.filename, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    Text(p.fileSizeFormatted, fontSize = 13.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                                }
                                Text("Folder: ${p.folderName} | Source: ${p.sourceName} (${p.cameraModel})", fontSize = 12.sp, color = TextSecondary)
                                Text("Checksum: ${p.fileChecksum}", fontSize = 10.sp, color = TextTertiary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                Text(
                                    "Captured: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(p.captureTimestamp)),
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
