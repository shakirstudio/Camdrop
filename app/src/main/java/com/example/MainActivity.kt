package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.DarkSurfaceBackground
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.CyanAccent
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

enum class NavigationTab(val label: String) {
    HOME("Home"),
    EVENTS("Events"),
    CAMERAS("Tether"),
    PHOTOS("Gallery"),
    TRANSFERS("Transfers"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val currentUser by viewModel.authService.currentUser.collectAsState()

                var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
                var showConnectFlow by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.userNotification.collectLatest { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkSurfaceBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (currentUser != null && !showConnectFlow) {
                            NavigationBar(
                                containerColor = DarkSurfaceElevated,
                                contentColor = CyanAccent,
                                windowInsets = WindowInsets.navigationBars,
                                modifier = Modifier.testTag("main_bottom_nav")
                            ) {
                                NavigationTab.values().forEach { tab ->
                                    val isSelected = currentTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = when (tab) {
                                                    NavigationTab.HOME -> Icons.Default.Dashboard
                                                    NavigationTab.EVENTS -> Icons.Default.Event
                                                    NavigationTab.CAMERAS -> Icons.Default.CameraAlt
                                                    NavigationTab.PHOTOS -> Icons.Default.Collections
                                                    NavigationTab.TRANSFERS -> Icons.Default.Sync
                                                    NavigationTab.SETTINGS -> Icons.Default.Settings
                                                },
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label, fontSize = 10.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = CyanAccent,
                                            selectedTextColor = CyanAccent,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary,
                                            indicatorColor = CyanAccent.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (currentUser == null) {
                            AuthScreen(
                                authService = viewModel.authService,
                                onAuthSuccess = {
                                    // Signed in successfully
                                }
                            )
                        } else if (showConnectFlow) {
                            CameraConnectScreen(
                                viewModel = viewModel,
                                onConnected = {
                                    showConnectFlow = false
                                    currentTab = NavigationTab.HOME
                                }
                            )
                        } else {
                            when (currentTab) {
                                NavigationTab.HOME -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToConnect = { showConnectFlow = true },
                                    onNavigateToEvents = { currentTab = NavigationTab.EVENTS },
                                    onNavigateToPhotos = { currentTab = NavigationTab.PHOTOS },
                                    onNavigateToTransfers = { currentTab = NavigationTab.TRANSFERS }
                                )
                                NavigationTab.EVENTS -> EventsScreen(
                                    viewModel = viewModel
                                )
                                NavigationTab.CAMERAS -> CamerasListScreen(
                                    viewModel = viewModel,
                                    onNavigateToConnect = { showConnectFlow = true }
                                )
                                NavigationTab.PHOTOS -> GalleryScreen(
                                    viewModel = viewModel
                                )
                                NavigationTab.TRANSFERS -> TransfersScreen(
                                    viewModel = viewModel
                                )
                                NavigationTab.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    onLogout = {
                                        currentTab = NavigationTab.HOME
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
