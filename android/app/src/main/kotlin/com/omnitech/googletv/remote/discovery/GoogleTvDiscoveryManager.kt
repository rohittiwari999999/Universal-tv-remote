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

/**
 * Manages local network mDNS discovery exclusively for Google TV & Android TV devices.
 * Automatically filters out Google Home Mini, Nest Mini, smart speakers, and audio-only Cast devices.
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

    private var discoveryListenerAtv: NsdManager.DiscoveryListener? = null
    private var discoveryListenerCast: NsdManager.DiscoveryListener? = null

    companion object {
        const val SERVICE_TYPE_GOOGLE_TV = "_androidtvremote2._tcp."
        const val SERVICE_TYPE_CAST = "_googlecast._tcp."
        private const val PREFS_NAME = "google_tv_paired_devices"
        private const val MANUAL_IPS_KEY = "manual_ips"

        // Google Cast Capability bitmasks
        // Bit 0 = Video Out (TVs and display Chromecasts have this bit set)
        private const val CAPABILITY_VIDEO_OUT = 1L
    }

    init {
        loadSavedManualDevices()
    }

    /**
     * Starts active scanning on the local Wi-Fi network.
     */
    fun startDiscovery() {
        if (_isScanning.value) return

        try {
            multicastLock = wifiManager?.createMulticastLock("GoogleTvMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire MulticastLock: ${e.message}")
        }

        _isScanning.value = true

        // 1. Primary discovery: Official Android TV / Google TV Remote v2 protocol
        discoveryListenerAtv = createDiscoveryListener("GoogleTV")
        try {
            nsdManager.discoverServices(SERVICE_TYPE_GOOGLE_TV, NsdManager.PROTOCOL_DNS_SD, discoveryListenerAtv)
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering _androidtvremote2: ${e.message}")
        }

        // 2. Cast discovery fallback: Only for TV/Display devices (video-out enabled)
        discoveryListenerCast = createDiscoveryListener("CastFallback")
        try {
            nsdManager.discoverServices(SERVICE_TYPE_CAST, NsdManager.PROTOCOL_DNS_SD, discoveryListenerCast)
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering _googlecast: ${e.message}")
        }
    }

    private fun createDiscoveryListener(tag: String): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "[$tag] Discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "[$tag] Service found: ${service.serviceName} (${service.serviceType})")
                resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "[$tag] Service lost: ${service.serviceName}")
                removeDevice(service.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "[$tag] Discovery stopped: $serviceType")
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "[$tag] Start discovery failed: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "[$tag] Stop discovery failed: $errorCode")
            }
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                val host = resolvedInfo.host?.hostAddress ?: return
                val port = resolvedInfo.port
                val rawName = resolvedInfo.serviceName ?: "Google TV"
                val serviceType = resolvedInfo.serviceType ?: ""

                val friendlyName = cleanTvName(rawName)
                val model = extractModelFromAttributes(resolvedInfo)
                val attributes = resolvedInfo.attributes

                // STRICT FILTER: Discard Google Home Mini, Nest Audio, smart speakers, etc.
                if (isAudioOnlySpeakerDevice(rawName, friendlyName, model, serviceType, attributes)) {
                    Log.i(TAG, "Filtered out non-TV audio speaker: '$friendlyName' (model: $model)")
                    return
                }

                val deviceId = "${host}_$friendlyName"

                val discoveredTv = DiscoveredGoogleTv(
                    id = deviceId,
                    name = friendlyName,
                    hostAddress = host,
                    pairingPort = 6467,
                    remotePort = if (port == 6467) 6466 else port,
                    model = model,
                    signalStrengthDbm = -45 - (Math.random() * 20).toInt(),
                    isPaired = isDevicePreviouslyPaired(deviceId)
                )

                addOrUpdateDevice(discoveredTv)
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.w(TAG, "Resolve call error: ${e.message}")
        }
    }

    /**
     * Determines whether the discovered device is an audio-only speaker (like Google Home Mini,
     * Nest Mini, Nest Audio, Chromecast Audio) rather than a Google TV / Android TV screen device.
     */
    private fun isAudioOnlySpeakerDevice(
        rawName: String,
        friendlyName: String,
        model: String,
        serviceType: String,
        attributes: Map<String, ByteArray>
    ): Boolean {
        val lowerRaw = rawName.lowercase()
        val lowerFriendly = friendlyName.lowercase()
        val lowerModel = model.lowercase()

        // 1. Blacklist speaker / audio-only brand names
        val speakerKeywords = listOf(
            "home mini",
            "google home",
            "nest mini",
            "nest audio",
            "home max",
            "nest hub",
            "chromecast audio",
            "smart speaker",
            "bluetooth speaker",
            "smart clock",
            "lenovo smart clock",
            "jbl link",
            "bose portable",
            "sonos",
            "speaker"
        )

        for (kw in speakerKeywords) {
            if (lowerRaw.contains(kw) || lowerFriendly.contains(kw) || lowerModel.contains(kw)) {
                return true
            }
        }

        // 2. For Google Cast services, check the 'ca' (capabilities) attribute
        // Cast devices without Video Out capability (bit 0 = 0x01) are pure audio devices
        val caBytes = attributes["ca"]
        if (caBytes != null) {
            val caStr = String(caBytes).trim()
            val caVal = caStr.toLongOrNull() ?: 0L
            if ((caVal and CAPABILITY_VIDEO_OUT) == 0L) {
                // Device does not have video out -> It is an audio speaker / smart clock
                return true
            }
        }

        // 3. If service is Google Cast and model name is missing or unknown, verify it's not a speaker
        if (serviceType.contains("googlecast", ignoreCase = true)) {
            // If Cast TXT attributes explicitly identify it as audio-only
            val fnBytes = attributes["fn"]
            if (fnBytes != null) {
                val fnStr = String(fnBytes).lowercase()
                for (kw in speakerKeywords) {
                    if (fnStr.contains(kw)) return true
                }
            }
        }

        return false
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

    fun markDevicePaired(deviceId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("paired_$deviceId", true).apply()
        val current = _devices.value.map {
            if (it.id == deviceId) it.copy(isPaired = true) else it
        }
        _devices.value = current
    }

    fun unpairDevice(deviceId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("paired_$deviceId").apply()
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

    fun stopDiscovery() {
        try {
            discoveryListenerAtv?.let { nsdManager.stopServiceDiscovery(it) }
            discoveryListenerAtv = null
        } catch (ignored: Exception) {}

        try {
            discoveryListenerCast?.let { nsdManager.stopServiceDiscovery(it) }
            discoveryListenerCast = null
        } catch (ignored: Exception) {}

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                multicastLock = null
            }
        } catch (ignored: Exception) {}

        _isScanning.value = false
    }
}
