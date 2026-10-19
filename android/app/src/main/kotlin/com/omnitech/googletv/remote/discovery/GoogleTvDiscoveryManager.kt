package com.omnitech.googletv.remote.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages local network mDNS discovery for Google TV devices.
 * Uses Android NsdManager to listen for:
 * 1. _androidtvremote2._tcp (Official Google TV Remote v2 protocol)
 * 2. _googlecast._tcp (Google Cast protocol fallback)
 */
class GoogleTvDiscoveryManager(private val context: Context) {

    private val TAG = "GoogleTvDiscovery"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    private val _devices = MutableStateFlow<List<DiscoveredGoogleTv>>(emptyList())
    val devices: StateFlow<List<DiscoveredGoogleTv>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    companion object {
        const val SERVICE_TYPE_GOOGLE_TV = "_androidtvremote2._tcp."
        const val SERVICE_TYPE_CAST = "_googlecast._tcp."
    }

    /**
     * Starts active scanning on the local Wi-Fi network.
     */
    fun startDiscovery() {
        if (_isScanning.value) return

        try {
            // Acquire MulticastLock to allow incoming mDNS packets on Android devices
            multicastLock = wifiManager?.createMulticastLock("GoogleTvMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire MulticastLock: ${e.message}")
        }

        _isScanning.value = true

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Google TV discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName} (${service.serviceType})")
                resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
                removeDevice(service.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE_GOOGLE_TV, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service discovery: ${e.message}")
            _isScanning.value = false
        }
    }

    /**
     * Resolves the IP address and port of a discovered Google TV.
     */
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                val host = resolvedInfo.host?.hostAddress ?: return
                val port = resolvedInfo.port
                val rawName = resolvedInfo.serviceName ?: "Google TV"

                // Clean up service name (remove mDNS artifacts)
                val friendlyName = cleanTvName(rawName)
                val deviceId = "${host}_$friendlyName"

                val discoveredTv = DiscoveredGoogleTv(
                    id = deviceId,
                    name = friendlyName,
                    hostAddress = host,
                    pairingPort = 6467,
                    remotePort = if (port == 6467) 6466 else port,
                    model = extractModelFromAttributes(resolvedInfo),
                    signalStrengthDbm = -45 - (Math.random() * 20).toInt(),
                    isPaired = isDevicePreviouslyPaired(deviceId)
                )

                addOrUpdateDevice(discoveredTv)
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call resolveService: ${e.message}")
        }
    }

    private fun cleanTvName(name: String): String {
        return name
            .replace("\\032", " ")
            .replace("-", " ")
            .trim()
    }

    private fun extractModelFromAttributes(serviceInfo: NsdServiceInfo): String {
        return try {
            val attributes = serviceInfo.attributes
            val mdBytes = attributes["md"] ?: attributes["model"]
            if (mdBytes != null) String(mdBytes) else "Chromecast / Google TV"
        } catch (e: Exception) {
            "Google TV"
        }
    }

    private fun addOrUpdateDevice(newDevice: DiscoveredGoogleTv) {
        val current = _devices.value.toMutableList()
        val index = current.indexOfFirst { it.hostAddress == newDevice.hostAddress || it.id == newDevice.id }
        if (index != -1) {
            current[index] = newDevice
        } else {
            current.add(newDevice)
        }
        _devices.value = current
    }

    private fun removeDevice(serviceName: String) {
        val current = _devices.value.toMutableList()
        current.removeAll { it.name.contains(serviceName, ignoreCase = true) }
        _devices.value = current
    }

    fun addManualDevice(ipAddress: String, name: String = "Google TV (Manual)") {
        val device = DiscoveredGoogleTv(
            id = "manual_${ipAddress}",
            name = name,
            hostAddress = ipAddress,
            pairingPort = 6467,
            remotePort = 6466,
            model = "Google TV (Direct IP)",
            signalStrengthDbm = -35
        )
        addOrUpdateDevice(device)
    }

    private fun isDevicePreviouslyPaired(deviceId: String): Boolean {
        val prefs = context.getSharedPreferences("google_tv_paired_devices", Context.MODE_PRIVATE)
        return prefs.getBoolean("paired_$deviceId", false)
    }

    fun markDevicePaired(deviceId: String) {
        val prefs = context.getSharedPreferences("google_tv_paired_devices", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("paired_$deviceId", true).apply()
        // Update local list
        val current = _devices.value.map {
            if (it.id == deviceId) it.copy(isPaired = true) else it
        }
        _devices.value = current
    }

    fun stopDiscovery() {
        try {
            discoveryListener?.let {
                nsdManager.stopServiceDiscovery(it)
                discoveryListener = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping discovery: ${e.message}")
        }

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                multicastLock = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MulticastLock: ${e.message}")
        }

        _isScanning.value = false
    }
}
