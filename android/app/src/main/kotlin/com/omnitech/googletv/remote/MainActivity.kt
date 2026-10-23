package com.omnitech.googletv.remote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.omnitech.googletv.remote.model.TvKey
import com.omnitech.googletv.remote.protocol.GoogleTvPairingEngine
import com.omnitech.googletv.remote.protocol.GoogleTvRemoteEngine
import com.omnitech.googletv.remote.ui.screens.DiscoveryScreen
import com.omnitech.googletv.remote.ui.screens.PairingDialog
import android.widget.Toast
import com.omnitech.googletv.remote.ir.CoocaaIrController
import com.omnitech.googletv.remote.ir.IrBlasterManager
import com.omnitech.googletv.remote.ir.TvBrand
import com.omnitech.googletv.remote.ui.screens.IrDiagnosticScreen
import com.omnitech.googletv.remote.ui.screens.RemoteScreen
import com.omnitech.googletv.remote.ui.screens.SplashScreen
import com.omnitech.googletv.remote.ui.theme.GoogleTvDark
import com.omnitech.googletv.remote.ui.theme.GoogleTvRemoteTheme
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private lateinit var discoveryManager: GoogleTvDiscoveryManager
    private lateinit var pairingEngine: GoogleTvPairingEngine
    private lateinit var remoteEngine: GoogleTvRemoteEngine
    private lateinit var irBlasterManager: IrBlasterManager
    private lateinit var coocaaIrController: CoocaaIrController

    private val crashHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Caught uncaught coroutine exception: ${throwable.message}", throwable)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        try {
            discoveryManager.startDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery after permission: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            discoveryManager = GoogleTvDiscoveryManager(this)
            pairingEngine = GoogleTvPairingEngine(this)
            remoteEngine = GoogleTvRemoteEngine(this)
            irBlasterManager = IrBlasterManager(this)
            coocaaIrController = CoocaaIrController(this)
        } catch (e: Exception) {
            Log.e(TAG, "Initialization error: ${e.message}", e)
        }

        checkPermissionsAndStartDiscovery()

        setContent {
            GoogleTvRemoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GoogleTvDark
                ) {
                    var showSplashScreen by remember { mutableStateOf(true) }
                    var showIrDiagnostics by remember { mutableStateOf(false) }

                    if (showIrDiagnostics) {
                        IrDiagnosticScreen(
                            onNavigateBack = { showIrDiagnostics = false }
                        )
                    } else if (showSplashScreen) {
                        SplashScreen(
                            onSplashFinished = {
                                showSplashScreen = false
                            }
                        )
                    } else {
                        val devices by discoveryManager.devices.collectAsState()
                        val isScanning by discoveryManager.isScanning.collectAsState()
                        val pairingState by pairingEngine.pairingState.collectAsState()
                        val connectionState by remoteEngine.connectionState.collectAsState()

                        val lastPairedDevice = remember { discoveryManager.getLastPairedDevice() }
                        var selectedTv by remember { mutableStateOf<DiscoveredGoogleTv?>(lastPairedDevice) }
                        var autoConnectTriggered by remember { mutableStateOf(false) }
                        var userNavigatedToDiscovery by remember { mutableStateOf(false) }
                        var showPairingDialog by remember { mutableStateOf(false) }
                        var connectionErrorMessage by remember { mutableStateOf<String?>(null) }

                        // IR Blaster state & TV Brand (Default: Coocaa TV with 38.4kHz Power Code)
                        val hasIrEmitter = remember { coocaaIrController.hasIrBlaster() || irBlasterManager.hasIrEmitter() }
                        var selectedBrand by remember { mutableStateOf(irBlasterManager.getSelectedBrand()) }

                        // Function to transmit Power ON via IR Blaster and Network
                        val transmitPowerAction: (TvBrand) -> Unit = { brand ->
                            lifecycleScope.launch(crashHandler) {
                                if (brand.id.equals("coocaa", ignoreCase = true)) {
                                    if (coocaaIrController.hasIrBlaster()) {
                                        val activeVariant = coocaaIrController.getSelectedVariant()
                                        coocaaIrController.sendSavedPowerSignal()
                                        Toast.makeText(this@MainActivity, "Coocaa IR Fired: ${activeVariant.name} (${activeVariant.hexCode})", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Phone me IR Blaster nahi mila! Wi-Fi Wake-on-LAN send kiya gaya.", Toast.LENGTH_SHORT).show()
                                    }
                                    // Only send Wake-on-LAN packet over network (No hardware IR collision)
                                    irBlasterManager.sendWakeOnLanOnly(selectedTv?.hostAddress)
                                } else {
                                    val targetIp = (connectionState as? RemoteConnectionState.Connected)?.device?.hostAddress
                                        ?: selectedTv?.hostAddress
                                    val result = irBlasterManager.transmitPower(
                                        brandId = brand.id,
                                        targetIp = targetIp
                                    )
                                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                                }
                                // If already connected to Google TV, also send remote key
                                if (connectionState is RemoteConnectionState.Connected) {
                                    remoteEngine.sendKey(TvKey.POWER)
                                }
                            }
                        }

                        val selectBrandAction: (TvBrand) -> Unit = { brand ->
                            irBlasterManager.setSelectedBrand(brand.id)
                            selectedBrand = brand
                            Toast.makeText(this@MainActivity, "Selected ${brand.name} for IR Blaster", Toast.LENGTH_SHORT).show()
                        }

                        // Automatically connect to saved paired TV on launch once splash screen finishes
                        LaunchedEffect(showSplashScreen) {
                            if (!showSplashScreen && selectedTv != null && !autoConnectTriggered && !userNavigatedToDiscovery) {
                                autoConnectTriggered = true
                                Log.i(TAG, "Auto-connecting to saved paired TV: ${selectedTv?.name} (${selectedTv?.hostAddress})")
                                lifecycleScope.launch(crashHandler) {
                                    remoteEngine.connect(selectedTv!!)
                                }
                            }
                        }

                        // Surface connection errors back to the user
                        LaunchedEffect(connectionState) {
                            if (connectionState is RemoteConnectionState.Error) {
                                connectionErrorMessage = (connectionState as RemoteConnectionState.Error).message
                            } else if (connectionState is RemoteConnectionState.Connected) {
                                connectionErrorMessage = null
                            }
                        }

                        // Auto-advance to remote control when pairing is authenticated
                        LaunchedEffect(pairingState) {
                            if (pairingState is PairingState.Paired) {
                                val pairedDevice = (pairingState as PairingState.Paired).device
                                discoveryManager.markDevicePaired(pairedDevice)
                                showPairingDialog = false
                                selectedTv = pairedDevice
                                userNavigatedToDiscovery = false
                                pairingEngine.reset()
                                lifecycleScope.launch(crashHandler) {
                                    remoteEngine.connect(pairedDevice)
                                }
                            }
                        }

                        // Resolve currently active connected or target device
                        val activeConnectedDevice = (connectionState as? RemoteConnectionState.Connected)?.device
                            ?: (connectionState as? RemoteConnectionState.Connecting)?.device
                            ?: selectedTv

                        // Direct automatic Remote screen if user has a saved paired device and hasn't explicitly tapped close
                        val shouldShowRemote = !userNavigatedToDiscovery && activeConnectedDevice != null && (
                            connectionState is RemoteConnectionState.Connected ||
                            connectionState is RemoteConnectionState.Connecting ||
                            connectionState is RemoteConnectionState.Error ||
                            connectionState is RemoteConnectionState.Disconnected ||
                            activeConnectedDevice.isPaired
                        )

                        if (shouldShowRemote && activeConnectedDevice != null) {
                            // Main Remote Control Screen (Direct auto-entry, no need to tap connect remote)
                            RemoteScreen(
                                device = activeConnectedDevice,
                                connectionState = connectionState,
                                onSendKey = { key ->
                                    lifecycleScope.launch(crashHandler) {
                                        remoteEngine.sendKey(key)
                                    }
                                },
                                onSendText = { text ->
                                    lifecycleScope.launch(crashHandler) {
                                        remoteEngine.sendText(text)
                                    }
                                },
                                onLaunchApp = { packageName ->
                                    lifecycleScope.launch(crashHandler) {
                                        remoteEngine.launchApp(packageName)
                                    }
                                },
                                onReconnect = {
                                    lifecycleScope.launch(crashHandler) {
                                        remoteEngine.connect(activeConnectedDevice)
                                    }
                                },
                                onRePair = {
                                    discoveryManager.unpairDevice(activeConnectedDevice.id)
                                    selectedTv = activeConnectedDevice
                                    showPairingDialog = true
                                    lifecycleScope.launch(crashHandler) {
                                        pairingEngine.startPairing(activeConnectedDevice)
                                    }
                                },
                                selectedBrand = selectedBrand,
                                hasIrEmitter = hasIrEmitter,
                                onTransmitPower = transmitPowerAction,
                                onSelectBrand = selectBrandAction,
                                onOpenDiagnostics = { showIrDiagnostics = true },
                                onDisconnect = {
                                    // User explicitly tapped close (X) to pick/scan for another TV
                                    remoteEngine.disconnect()
                                    userNavigatedToDiscovery = true
                                    selectedTv = null
                                    discoveryManager.startDiscovery()
                                }
                            )
                        } else {
                            // Discovery & Pairing Hub Screen
                            val connectingDeviceId = (connectionState as? RemoteConnectionState.Connecting)?.device?.id
                            DiscoveryScreen(
                                devices = devices,
                                isScanning = isScanning,
                                connectingDeviceId = connectingDeviceId,
                                errorMessage = connectionErrorMessage,
                                onStartScan = {
                                    discoveryManager.startDiscovery()
                                },
                                onSelectDevice = { tv ->
                                    selectedTv = tv
                                    userNavigatedToDiscovery = false
                                    connectionErrorMessage = null
                                    if (tv.isPaired) {
                                        discoveryManager.saveLastPairedDevice(tv)
                                        lifecycleScope.launch(crashHandler) {
                                            remoteEngine.connect(tv)
                                        }
                                    } else {
                                        showPairingDialog = true
                                        lifecycleScope.launch(crashHandler) {
                                            pairingEngine.startPairing(tv)
                                        }
                                    }
                                },
                                onConnectDirectly = { tv ->
                                    selectedTv = tv
                                    userNavigatedToDiscovery = false
                                    connectionErrorMessage = null
                                    if (tv.isPaired) {
                                        discoveryManager.saveLastPairedDevice(tv)
                                        lifecycleScope.launch(crashHandler) {
                                            remoteEngine.connect(tv)
                                        }
                                    } else {
                                        showPairingDialog = true
                                        lifecycleScope.launch(crashHandler) {
                                            pairingEngine.startPairing(tv)
                                        }
                                    }
                                },
                                onForcePairDevice = { tv ->
                                    selectedTv = tv
                                    connectionErrorMessage = null
                                    showPairingDialog = true
                                    lifecycleScope.launch(crashHandler) {
                                        pairingEngine.startPairing(tv)
                                    }
                                },
                                onUnpairDevice = { tv ->
                                    discoveryManager.unpairDevice(tv.id)
                                    if (tv.id.startsWith("manual_")) {
                                        discoveryManager.removeManualDevice(tv.id)
                                    }
                                    remoteEngine.disconnect()
                                    if (selectedTv?.id == tv.id) selectedTv = null
                                },
                                onResetAllSettings = {
                                    discoveryManager.resetAllSettings()
                                    remoteEngine.disconnect()
                                    pairingEngine.reset()
                                    selectedTv = null
                                    connectionErrorMessage = null
                                    discoveryManager.startDiscovery()
                                },
                                onAddManualIp = { ip ->
                                    connectionErrorMessage = null
                                    discoveryManager.addManualDevice(ip)
                                },
                                onDismissError = {
                                    connectionErrorMessage = null
                                },
                                onShowSplash = {
                                    showSplashScreen = true
                                },
                                selectedBrand = selectedBrand,
                                hasIrEmitter = hasIrEmitter,
                                onTransmitPower = transmitPowerAction,
                                onSelectBrand = selectBrandAction,
                                onOpenDiagnostics = { showIrDiagnostics = true }
                            )
                        }

                        // Pairing PIN Code Dialog
                        if (showPairingDialog && selectedTv != null) {
                            PairingDialog(
                                device = selectedTv!!,
                                pairingState = pairingState,
                                onSubmitPin = { pin ->
                                    lifecycleScope.launch(crashHandler) {
                                        pairingEngine.submitPin(pin)
                                    }
                                },
                                onRetryPairing = {
                                    lifecycleScope.launch(crashHandler) {
                                        pairingEngine.startPairing(selectedTv!!)
                                    }
                                },
                                onConnectDirectly = {
                                    showPairingDialog = false
                                    lifecycleScope.launch(crashHandler) {
                                        remoteEngine.connect(selectedTv!!)
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
            try {
                discoveryManager.startDiscovery()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting discovery: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            discoveryManager.stopDiscovery()
            remoteEngine.disconnect()
            pairingEngine.reset()
        } catch (ignored: Exception) {}
    }
}
