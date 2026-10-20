package com.omnitech.googletv.remote.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import com.omnitech.googletv.remote.protocol.GoogleTvCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.ArrayDeque

/**
 * Manages local network mDNS discovery exclusively for Google TV & Android TV devices.
 * Uses only the official _androidtvremote2._tcp service, strictly preventing
 * Google Home Mini, Nest Mini, or smart speakers from appearing.
 *
 * Implements a thread-safe sequential resolve queue to prevent Android NsdManager
 * 'listener already in use' crashes when multiple devices respond.
 */
class GoogleTvDiscoveryManager(private val context: Context) {

    private val TAG = "GoogleTvDiscovery"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null

    private val _devices = MutableStateFlow<List<DiscoveredGoogleTv>>(emptyList())
    val devices: StateFlow<List<DiscoveredGoogleTv>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    // Sequential Queue to resolve services safely one by one
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var isResolving = false
    private val queueLock = Any()

    companion object {
        // Official Android TV / Google TV Remote v2 protocol service
        // Audio speakers (like Google Home Mini) NEVER broadcast this service.
        const val SERVICE_TYPE_GOOGLE_TV = "_androidtvremote2._tcp."
        private const val PREFS_NAME = "google_tv_paired_devices"
        private const val MANUAL_IPS_KEY = "manual_ips"
        private const val KEY_LAST_PAIRED_ID = "last_paired_id"
        private const val KEY_LAST_PAIRED_NAME = "last_paired_name"
        private const val KEY_LAST_PAIRED_HOST = "last_paired_host"
        private const val KEY_LAST_PAIRED_REMOTE_PORT = "last_paired_remote_port"
        private const val KEY_LAST_PAIRED_PAIRING_PORT = "last_paired_pairing_port"
        private const val KEY_LAST_PAIRED_MODEL = "last_paired_model"
    }

    init {
        loadSavedManualDevices()
        loadSavedPairedDevice()
    }

    /**
     * Starts active scanning on the local Wi-Fi network for Google TV devices.
     */
    @Synchronized
    fun startDiscovery() {
        if (_isScanning.value) {
            stopDiscovery()
        }

        try {
            multicastLock = wifiManager?.createMulticastLock("GoogleTvMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire MulticastLock: ${e.message}")
        }

        _isScanning.value = true

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started for $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName} (${service.serviceType})")
                enqueueResolve(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
                removeDevice(service.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped for $serviceType")
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $errorCode")
            }
        }

        discoveryListener = listener

        try {
            nsdManager.discoverServices(SERVICE_TYPE_GOOGLE_TV, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discoverServices: ${e.message}")
            _isScanning.value = false
        }
    }

    private fun enqueueResolve(serviceInfo: NsdServiceInfo) {
        synchronized(queueLock) {
            // Avoid duplicate resolutions in flight
            if (resolveQueue.none { it.serviceName == serviceInfo.serviceName }) {
                resolveQueue.addLast(serviceInfo)
            }
            processNextResolve()
        }
    }

    private fun processNextResolve() {
        synchronized(queueLock) {
            if (isResolving || resolveQueue.isEmpty()) return
            val nextService = resolveQueue.removeFirst()
            isResolving = true

            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: code $errorCode")
                    synchronized(queueLock) {
                        isResolving = false
                        processNextResolve()
                    }
                }

                override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                    try {
                        handleResolvedService(resolvedInfo)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling resolved service: ${e.message}", e)
                    } finally {
                        synchronized(queueLock) {
                            isResolving = false
                            processNextResolve()
                        }
                    }
                }
            }

            try {
                nsdManager.resolveService(nextService, resolveListener)
            } catch (e: Exception) {
                Log.e(TAG, "Exception calling resolveService: ${e.message}", e)
                isResolving = false
                processNextResolve()
            }
        }
    }

    private fun handleResolvedService(resolvedInfo: NsdServiceInfo) {
        val host = resolvedInfo.host?.hostAddress ?: return
        val rawName = resolvedInfo.serviceName ?: "Google TV"
        val friendlyName = cleanTvName(rawName)
        val model = extractModelFromAttributes(resolvedInfo)

        // Safety check: ensure no speaker device can ever be shown
        if (isAudioSpeakerDevice(friendlyName, model)) {
            Log.i(TAG, "Discarded audio speaker: '$friendlyName' ($model)")
            return
        }

        val deviceId = "${host}_$friendlyName"

        val discoveredTv = DiscoveredGoogleTv(
            id = deviceId,
            name = friendlyName,
            hostAddress = host,
            pairingPort = 6467,
            remotePort = 6466,
            model = model,
            signalStrengthDbm = -45 - (Math.random() * 20).toInt(),
            isPaired = isDevicePreviouslyPaired(deviceId)
        )

        addOrUpdateDevice(discoveredTv)
    }

    /**
     * Extra safety layer: checks if a device name or model refers to a speaker.
     */
    private fun isAudioSpeakerDevice(friendlyName: String, model: String): Boolean {
        val lower = "${friendlyName.lowercase()} ${model.lowercase()}"
        val speakerTerms = listOf(
            "home mini", "google home", "nest mini", "nest audio",
            "home max", "nest hub", "chromecast audio", "smart speaker",
            "bluetooth speaker", "smart clock", "speaker"
        )
        return speakerTerms.any { lower.contains(it) }
    }

    private fun cleanTvName(name: String): String {
        return name
            .replace("\\032", " ")
            .replace("-", " ")
            .trim()
    }

    private fun extractModelFromAttributes(serviceInfo: NsdServiceInfo): String {
        return try {
            val attributes = serviceInfo.attributes ?: return "Google TV"
            val mdBytes = attributes["md"] ?: attributes["model"] ?: attributes["fn"]
            if (mdBytes != null) String(mdBytes) else "Google TV / Android TV"
        } catch (e: Exception) {
            "Google TV"
        }
    }

    private fun addOrUpdateDevice(newDevice: DiscoveredGoogleTv) {
        val current = _devices.value.toMutableList()
        val index = current.indexOfFirst { it.hostAddress == newDevice.hostAddress || it.id == newDevice.id }
        if (index != -1) {
            val existing = current[index]
            current[index] = newDevice.copy(isPaired = existing.isPaired || newDevice.isPaired)
        } else {
            current.add(newDevice)
        }
        _devices.value = current
    }

    private fun removeDevice(serviceName: String) {
        val current = _devices.value.toMutableList()
        current.removeAll { it.name.contains(serviceName, ignoreCase = true) && !it.id.startsWith("manual_") }
        _devices.value = current
    }

    fun addManualDevice(ipAddress: String, name: String = "Google TV (Manual)") {
        val cleanIp = ipAddress.trim()
        val deviceId = "manual_$cleanIp"
        val device = DiscoveredGoogleTv(
            id = deviceId,
            name = if (name.isNotBlank()) name else "Google TV ($cleanIp)",
            hostAddress = cleanIp,
            pairingPort = 6467,
            remotePort = 6466,
            model = "Google TV (Direct IP)",
            signalStrengthDbm = -35,
            isPaired = isDevicePreviouslyPaired(deviceId)
        )
        addOrUpdateDevice(device)
        saveManualIp(cleanIp)
    }

    private fun loadSavedManualDevices() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(MANUAL_IPS_KEY, "[]") ?: "[]"
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val ip = array.getString(i)
                val deviceId = "manual_$ip"
                val device = DiscoveredGoogleTv(
                    id = deviceId,
                    name = "Google TV ($ip)",
                    hostAddress = ip,
                    pairingPort = 6467,
                    remotePort = 6466,
                    model = "Google TV (Saved)",
                    signalStrengthDbm = -40,
                    isPaired = isDevicePreviouslyPaired(deviceId)
                )
                addOrUpdateDevice(device)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading saved manual devices: ${e.message}")
        }
    }

    private fun saveManualIp(ip: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(MANUAL_IPS_KEY, "[]") ?: "[]"
            val array = JSONArray(json)
            var exists = false
            for (i in 0 until array.length()) {
                if (array.getString(i) == ip) {
                    exists = true
                    break
                }
            }
            if (!exists) {
                array.put(ip)
                prefs.edit().putString(MANUAL_IPS_KEY, array.toString()).apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error saving manual IP: ${e.message}")
        }
    }

    fun removeManualDevice(deviceId: String) {
        val current = _devices.value.toMutableList()
        val removed = current.find { it.id == deviceId }
        current.removeAll { it.id == deviceId }
        _devices.value = current

        if (removed != null) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = prefs.getString(MANUAL_IPS_KEY, "[]") ?: "[]"
                val array = JSONArray(json)
                val newArray = JSONArray()
                for (i in 0 until array.length()) {
                    val ip = array.getString(i)
                    if (ip != removed.hostAddress) {
                        newArray.put(ip)
                    }
                }
                prefs.edit().putString(MANUAL_IPS_KEY, newArray.toString()).apply()
            } catch (e: Exception) {
                Log.w(TAG, "Error removing manual IP: ${e.message}")
            }
        }
    }

    fun isDevicePreviouslyPaired(deviceId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("paired_$deviceId", false)
    }

    fun saveLastPairedDevice(device: DiscoveredGoogleTv) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_LAST_PAIRED_ID, device.id)
                .putString(KEY_LAST_PAIRED_NAME, device.name)
                .putString(KEY_LAST_PAIRED_HOST, device.hostAddress)
                .putInt(KEY_LAST_PAIRED_REMOTE_PORT, device.remotePort)
                .putInt(KEY_LAST_PAIRED_PAIRING_PORT, device.pairingPort)
                .putString(KEY_LAST_PAIRED_MODEL, device.model)
                .putBoolean("paired_${device.id}", true)
                .apply()
            Log.d(TAG, "Saved last paired device: ${device.name} (${device.hostAddress})")
        } catch (e: Exception) {
            Log.w(TAG, "Error saving last paired device: ${e.message}")
        }
    }

    fun getLastPairedDevice(): DiscoveredGoogleTv? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val id = prefs.getString(KEY_LAST_PAIRED_ID, null) ?: return null
            val host = prefs.getString(KEY_LAST_PAIRED_HOST, null) ?: return null
            val name = prefs.getString(KEY_LAST_PAIRED_NAME, "Google TV") ?: "Google TV"
            val remotePort = prefs.getInt(KEY_LAST_PAIRED_REMOTE_PORT, 6466)
            val pairingPort = prefs.getInt(KEY_LAST_PAIRED_PAIRING_PORT, 6467)
            val model = prefs.getString(KEY_LAST_PAIRED_MODEL, "Google TV / Android TV") ?: "Google TV"

            DiscoveredGoogleTv(
                id = id,
                name = name,
                hostAddress = host,
                pairingPort = pairingPort,
                remotePort = remotePort,
                model = model,
                signalStrengthDbm = -35,
                isPaired = true
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error getting last paired device: ${e.message}")
            null
        }
    }

    fun clearLastPairedDevice() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(KEY_LAST_PAIRED_ID)
                .remove(KEY_LAST_PAIRED_NAME)
                .remove(KEY_LAST_PAIRED_HOST)
                .remove(KEY_LAST_PAIRED_REMOTE_PORT)
                .remove(KEY_LAST_PAIRED_PAIRING_PORT)
                .remove(KEY_LAST_PAIRED_MODEL)
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing last paired device: ${e.message}")
        }
    }

    private fun loadSavedPairedDevice() {
        val lastDevice = getLastPairedDevice()
        if (lastDevice != null) {
            addOrUpdateDevice(lastDevice)
        }
    }

    fun markDevicePaired(device: DiscoveredGoogleTv) {
        saveLastPairedDevice(device)
        val current = _devices.value.map {
            if (it.id == device.id || it.hostAddress == device.hostAddress) it.copy(isPaired = true) else it
        }
        _devices.value = current
    }

    fun markDevicePaired(deviceId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("paired_$deviceId", true).apply()
        val found = _devices.value.find { it.id == deviceId }
        if (found != null) {
            saveLastPairedDevice(found)
        }
        val current = _devices.value.map {
            if (it.id == deviceId) it.copy(isPaired = true) else it
        }
        _devices.value = current
    }

    fun unpairDevice(deviceId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("paired_$deviceId").apply()
        val lastPaired = getLastPairedDevice()
        if (lastPaired?.id == deviceId) {
            clearLastPairedDevice()
        }
        val current = _devices.value.map {
            if (it.id == deviceId) it.copy(isPaired = false) else it
        }
        _devices.value = current
        Log.d(TAG, "Unpaired device $deviceId")
    }

    /**
     * Complete reset of all saved paired devices and TLS certificate.
     */
    fun resetAllSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        GoogleTvCrypto.resetCredentials(context)
        val resetList = _devices.value.map { it.copy(isPaired = false) }
        _devices.value = resetList
        Log.i(TAG, "Wiped all paired devices and TLS identity credentials")
    }

    @Synchronized
    fun stopDiscovery() {
        synchronized(queueLock) {
            resolveQueue.clear()
            isResolving = false
        }

        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping discovery: ${e.message}")
            }
            discoveryListener = null
        }

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                multicastLock = null
            }
        } catch (ignored: Exception) {}

        _isScanning.value = false
    }
}
