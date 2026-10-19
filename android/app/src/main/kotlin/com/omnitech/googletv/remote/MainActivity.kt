package com.omnitech.googletv.remote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.omnitech.googletv.remote.discovery.GoogleTvDiscoveryManager
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import com.omnitech.googletv.remote.model.PairingState
import com.omnitech.googletv.remote.model.RemoteConnectionState
import com.omnitech.googletv.remote.protocol.GoogleTvPairingEngine
import com.omnitech.googletv.remote.protocol.GoogleTvRemoteEngine
import com.omnitech.googletv.remote.ui.screens.DiscoveryScreen
import com.omnitech.googletv.remote.ui.screens.PairingDialog
import com.omnitech.googletv.remote.ui.screens.RemoteScreen
import com.omnitech.googletv.remote.ui.theme.GoogleTvDark
import com.omnitech.googletv.remote.ui.theme.GoogleTvRemoteTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var discoveryManager: GoogleTvDiscoveryManager
    private lateinit var pairingEngine: GoogleTvPairingEngine
    private lateinit var remoteEngine: GoogleTvRemoteEngine

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Automatically start discovery once permissions are checked
        discoveryManager.startDiscovery()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        discoveryManager = GoogleTvDiscoveryManager(this)
        pairingEngine = GoogleTvPairingEngine(this)
        remoteEngine = GoogleTvRemoteEngine(this)

        checkPermissionsAndStartDiscovery()

        setContent {
            GoogleTvRemoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GoogleTvDark
                ) {
                    val devices by discoveryManager.devices.collectAsState()
                    val isScanning by discoveryManager.isScanning.collectAsState()
                    val pairingState by pairingEngine.pairingState.collectAsState()
                    val connectionState by remoteEngine.connectionState.collectAsState()

                    var selectedTv by remember { mutableStateOf<DiscoveredGoogleTv?>(null) }
                    var showPairingDialog by remember { mutableStateOf(false) }

                    // Auto-start discovery as soon as app opens
                    LaunchedEffect(Unit) {
                        discoveryManager.startDiscovery()
                    }

                    // When pairing succeeds, automatically connect to remote command channel
                    LaunchedEffect(pairingState) {
                        if (pairingState is PairingState.Paired) {
                            val pairedDevice = (pairingState as PairingState.Paired).device
                            discoveryManager.markDevicePaired(pairedDevice.id)
                            showPairingDialog = false
                            selectedTv = pairedDevice
                            remoteEngine.connect(pairedDevice)
                        }
                    }

                    if (connectionState is RemoteConnectionState.Connected && selectedTv != null) {
                        // Main Remote Control Screen
                        RemoteScreen(
                            device = selectedTv!!,
                            connectionState = connectionState,
                            onSendKey = { key ->
                                lifecycleScope.launch {
                                    remoteEngine.sendKey(key)
                                }
                            },
                            onSendText = { text ->
                                lifecycleScope.launch {
                                    remoteEngine.sendText(text)
                                }
                            },
                            onLaunchApp = { packageName ->
                                lifecycleScope.launch {
                                    remoteEngine.launchApp(packageName)
                                }
                            },
                            onDisconnect = {
                                remoteEngine.disconnect()
                                selectedTv = null
                                discoveryManager.startDiscovery()
                            }
                        )
                    } else {
                        // Discovery Radar Screen (Opens automatically on launch)
                        DiscoveryScreen(
                            devices = devices,
                            isScanning = isScanning,
                            onStartScan = {
                                discoveryManager.startDiscovery()
                            },
                            onSelectDevice = { tv ->
                                selectedTv = tv
                                if (tv.isPaired) {
                                    // Already paired, directly connect
                                    lifecycleScope.launch {
                                        remoteEngine.connect(tv)
                                    }
                                } else {
                                    // Start real pairing sequence
                                    showPairingDialog = true
                                    lifecycleScope.launch {
                                        pairingEngine.startPairing(tv)
                                    }
                                }
                            },
                            onAddManualIp = { ip ->
                                discoveryManager.addManualDevice(ip)
                            }
                        )
                    }

                    // Pairing PIN Code Dialog
                    if (showPairingDialog && selectedTv != null) {
                        PairingDialog(
                            device = selectedTv!!,
                            pairingState = pairingState,
                            onSubmitPin = { pin ->
                                lifecycleScope.launch {
                                    pairingEngine.submitPin(pin)
                                }
                            },
                            onDismiss = {
                                showPairingDialog = false
                                pairingEngine.reset()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStartDiscovery() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
        )

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        } else {
            discoveryManager.startDiscovery()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryManager.stopDiscovery()
        remoteEngine.disconnect()
        pairingEngine.reset()
    }
}
