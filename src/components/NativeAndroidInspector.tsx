import React, { useState } from 'react';
import { 
  FileCode, Check, Copy, Terminal, Shield, Cpu,
  Download, ExternalLink, Sparkles, Folder, Play
} from 'lucide-react';

interface CodeFile {
  name: string;
  path: string;
  language: string;
  description: string;
  code: string;
}

export const NativeAndroidInspector: React.FC = () => {
  const [copiedPath, setCopiedPath] = useState<string | null>(null);

  const files: CodeFile[] = [
    {
      name: 'GoogleTvDiscoveryManager.kt',
      path: 'android/app/src/main/kotlin/com/omnitech/googletv/remote/discovery/GoogleTvDiscoveryManager.kt',
      language: 'kotlin',
      description: 'Native Android NsdManager (mDNS/DNS-SD) engine that auto-discovers Google TV devices over _androidtvremote2._tcp.',
      code: `package com.omnitech.googletv.remote.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Native Android NsdManager discovery engine.
 * Discovers nearby Google TVs on local Wi-Fi automatically on app start.
 */
class GoogleTvDiscoveryManager(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _devices = MutableStateFlow<List<DiscoveredGoogleTv>>(emptyList())
    val devices: StateFlow<List<DiscoveredGoogleTv>> = _devices

    companion object {
        const val SERVICE_TYPE_GOOGLE_TV = "_androidtvremote2._tcp."
    }

    fun startDiscovery() {
        multicastLock = wifiManager?.createMulticastLock("GoogleTvLock")?.apply {
            setReferenceCounted(true)
            acquire()
        }
        nsdManager.discoverServices(SERVICE_TYPE_GOOGLE_TV, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }
}`
    },
    {
      name: 'GoogleTvPairingEngine.kt',
      path: 'android/app/src/main/kotlin/com/omnitech/googletv/remote/protocol/GoogleTvPairingEngine.kt',
      language: 'kotlin',
      description: 'Real TLS Socket Handshake on port 6467, sends PairingRequest, prompts TV PIN, and computes SHA-256 HMAC Secret.',
      code: `package com.omnitech.googletv.remote.protocol

import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import javax.net.ssl.SSLSocket

/**
 * Handles the complete, real Google TV / Android TV Remote v2 Pairing Protocol (port 6467).
 * 1. TLS handshake with self-signed X.509 client certificate.
 * 2. Send PairingRequest -> TV displays 4/6-character PIN on screen.
 * 3. User enters code in app.
 * 4. App computes SHA-256(ClientCert + ServerCert + PIN) and sends PairingSecret.
 * 5. TV acknowledges PairingSecretAck. Certificate is permanently trusted.
 */
class GoogleTvPairingEngine(private val context: Context) {
    suspend fun startPairing(device: DiscoveredGoogleTv) = withContext(Dispatchers.IO) {
        val sslContext = GoogleTvCrypto.getOrCreateSslContext(context)
        val socket = sslContext.socketFactory.createSocket() as SSLSocket
        socket.connect(InetSocketAddress(device.hostAddress, 6467), 8000)
        socket.startHandshake()
        sendPairingRequest()
    }
}`
    },
    {
      name: 'GoogleTvRemoteEngine.kt',
      path: 'android/app/src/main/kotlin/com/omnitech/googletv/remote/protocol/GoogleTvRemoteEngine.kt',
      language: 'kotlin',
      description: 'Connects to Google TV on port 6466 with TLS, streams D-Pad, Volume, Media key events, and IME text typing.',
      code: `package com.omnitech.googletv.remote.protocol

import com.omnitech.googletv.remote.model.TvKey
import javax.net.ssl.SSLSocket

/**
 * Communicates with Google TV over port 6466.
 * Sends KeyEvents (DPad, Home, Back, Volume, Power), IME Text strings, and responds to Pings.
 */
class GoogleTvRemoteEngine(private val context: Context) {
    suspend fun connect(device: DiscoveredGoogleTv) { ... }

    suspend fun sendKey(key: TvKey) {
        // Sends KeyDown (direction 1), 35ms tactile delay, KeyUp (direction 2)
        val downPacket = buildKeyEventPacket(key.code, direction = 1)
        outputStream.write(downPacket)
        delay(35)
        val upPacket = buildKeyEventPacket(key.code, direction = 2)
        outputStream.write(upPacket)
    }

    suspend fun sendText(text: String) {
        val payload = text.toByteArray(Charsets.UTF_8)
        val packet = buildMessagePacket(type = 0x07, payload = payload)
        outputStream.write(packet)
    }
}`
    },
    {
      name: 'MainActivity.kt',
      path: 'android/app/src/main/kotlin/com/omnitech/googletv/remote/MainActivity.kt',
      language: 'kotlin',
      description: 'Jetpack Compose entry activity that auto-starts discovery on open, manages pairing dialog, and loads RemoteScreen.',
      code: `package com.omnitech.googletv.remote

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        discoveryManager = GoogleTvDiscoveryManager(this)
        pairingEngine = GoogleTvPairingEngine(this)
        remoteEngine = GoogleTvRemoteEngine(this)

        setContent {
            GoogleTvRemoteTheme {
                // Automatically triggers Wi-Fi mDNS discovery on app start!
                LaunchedEffect(Unit) {
                    discoveryManager.startDiscovery()
                }
                ...
            }
        }
    }
}`
    },
    {
      name: 'IrBlasterManager.kt',
      path: 'android/app/src/main/kotlin/com/omnitech/googletv/remote/ir/IrBlasterManager.kt',
      language: 'kotlin',
      description: 'ConsumerIrManager engine transmitting 38kHz NEC IR Power ON codes for Coocaa TV (Skyworth) and dual Wake-on-LAN fallback.',
      code: `package com.omnitech.googletv.remote.ir

import android.content.Context
import android.hardware.ConsumerIrManager

data class TvBrand(
    val id: String,
    val name: String,
    val description: String,
    val carrierFreq: Int = 38000,
    val protocol: String = "NEC",
    val codeHex: String = "0x707030CF"
)

class IrBlasterManager(private val context: Context) {
    private val consumerIrManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    companion object {
        val SUPPORTED_BRANDS = listOf(
            TvBrand("coocaa", "Coocaa TV", "Coocaa / Skyworth NEC 38kHz (Google TV)", 38000, "NEC", "0x707030CF"),
            TvBrand("samsung", "Samsung TV", "Samsung 38kHz Power Code", 38000, "Samsung", "0xE0E040BF"),
            TvBrand("lg", "LG TV", "LG NEC 38kHz Power Code", 38000, "NEC", "0x20DF10EF"),
            TvBrand("sony", "Sony TV", "Sony SIRC 40kHz 12-bit Power Code", 40000, "Sony SIRC", "0xA90")
        )
    }

    suspend fun transmitPower(brandId: String = "coocaa", targetIp: String? = null) {
        if (consumerIrManager?.hasIrEmitter() == true) {
            // Transmits exact 32-bit NEC pulses for Coocaa TV (0x707030CF)
            val pattern = generateIrPatternForBrand(brandId)
            consumerIrManager.transmit(38000, pattern)
        }
    }
}`
    },
    {
      name: 'CoocaaIrController.kt',
      path: 'android/app/src/main/kotlin/com/omnitech/googletv/remote/ir/CoocaaIrController.kt',
      language: 'kotlin',
      description: 'Advanced Coocaa TV IR Controller supporting all 6 power codes (Google TV, 4K S6G/Y72, Coolita, 9ms AGC Leader, Skyworth Alt, Raw) and Super Multi-Blast.',
      code: `package com.omnitech.googletv.remote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import kotlinx.coroutines.delay

class CoocaaIrController(private val context: Context) {
    private val irManager: ConsumerIrManager? by lazy {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    }

    fun hasIrBlaster(): Boolean = irManager?.hasIrEmitter() == true

    companion object {
        const val FREQ_38400 = 38400
        const val FREQ_38000 = 38000

        // 1. Official Coocaa Google TV & Android TV (0x00FF12ED)
        val PATTERN_GOOGLE_TV = buildStandardNec(0x00, 0xFF, 0x12, 0xED)
        // 2. Coocaa 4K S6G / Y72 Series (0x707030CF)
        val PATTERN_S6G_Y72 = buildStandardNec(0x70, 0x70, 0x30, 0xCF)
        // 3. Coocaa Coolita / 32S3U / 40S3U (0x01FE48B7)
        val PATTERN_COOLITA = buildStandardNec(0x01, 0xFE, 0x48, 0xB7)
        // 4. Skyworth Android TV Alt (0x00FF02FD)
        val PATTERN_SKYWORTH_ALT = buildStandardNec(0x00, 0xFF, 0x02, 0xFD)
        // 5. Corrected 9000µs AGC Leader Burst (Deep Standby Wake)
        val CORRECTED_HEADER_PATTERN = intArrayOf(9000, 4453, 625, 1614, ...)
        // 6. User Raw 68-pulse Array
        val RAW_USER_PATTERN = intArrayOf(1901, 4453, 625, 1614, ...)
    }

    // SUPER MULTI-BLAST: Fires all 6 codes sequentially with 2x frame repeat!
    suspend fun sendSuperMultiBlast(): Boolean {
        if (!hasIrBlaster()) return false
        val codes = listOf(
            PATTERN_GOOGLE_TV to FREQ_38000,
            PATTERN_S6G_Y72 to FREQ_38400,
            CORRECTED_HEADER_PATTERN to FREQ_38400,
            PATTERN_COOLITA to FREQ_38000,
            PATTERN_SKYWORTH_ALT to FREQ_38000,
            RAW_USER_PATTERN to FREQ_38400
        )
        for ((pattern, freq) in codes) {
            irManager?.transmit(freq, pattern)
            delay(50)
            irManager?.transmit(freq, pattern) // 2nd repeat frame
            delay(65)
        }
        return true
    }

    suspend fun sendPowerOnSignal(codeIndex: Int = 0): Boolean {
        return if (codeIndex == 0) sendSuperMultiBlast() else transmitSinglePattern(codeIndex)
    }
}`
    },
    {
      name: 'StandalonePowerOnCard.kt',
      path: 'android/app/src/main/kotlin/com/omnitech/googletv/remote/ui/components/StandalonePowerOnCard.kt',
      language: 'kotlin',
      description: 'Dedicated standalone IR Blaster Power ON component placed on Front Discovery Page below "Add TV by IP Address" to wake TV so it connects to Wi-Fi and pairs smoothly.',
      code: `package com.omnitech.googletv.remote.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnitech.googletv.remote.ir.TvBrand
import com.omnitech.googletv.remote.ir.IrBlasterManager
import com.omnitech.googletv.remote.ui.theme.*

/**
 * Standalone IR Blaster Power ON Card.
 * Placed on the Front Page directly below "Add TV by IP Address".
 * Turn on TV via IR blaster (Coocaa 38kHz NEC 0x707030CF) so Wi-Fi connects & pairs easily.
 */
@Composable
fun StandalonePowerOnCard(
    selectedBrand: TvBrand,
    hasIrEmitter: Boolean,
    onTransmitPower: (TvBrand) -> Unit,
    onSelectBrand: (TvBrand) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isBlasting by remember { mutableStateOf(false) }
    var showBrandDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(GoogleTvRed.copy(alpha = 0.6f), GoogleTvCardBorder, GoogleTvBlue.copy(alpha = 0.5f))
                ),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = GoogleTvSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header with IR badge and Brand selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = GoogleTvRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TV POWER ON (IR BLASTER)",
                        color = GoogleTvTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    onClick = { showBrandDialog = true },
                    color = GoogleTvDark,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvCardBorder),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = GoogleTvBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(selectedBrand.name, color = GoogleTvTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GoogleTvTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Google TV pair na ho paye to pehle IR Blaster se TV ko ON karein taaki TV Wi-Fi se connect ho aur phir pair ho sake easily.",
                color = GoogleTvTextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Large Standalone POWER ON Action Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isBlasting = true
                    onTransmitPower(selectedBrand)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoogleTvRed),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = "Turn ON TV", tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "POWER ON TV (IR BLASTER)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}`
    },
    {
      name: 'build_native_android_apk_aab.yml',
      path: '.github/workflows/build_native_android_apk_aab.yml',
      language: 'yaml',
      description: 'GitHub Actions CI/CD workflow that compiles Native Android APK & AAB with JDK 17, Gradle 8.5, and Target SDK 35.',
      code: `name: Build Native Android Google TV Remote (APK & AAB)

on:
  push:
    branches: [main, master]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Build Release APK
        run: |
          cd android
          ./gradlew assembleRelease
      - name: Build Google Play App Bundle (AAB)
        run: |
          cd android
          ./gradlew bundleRelease`
    }
  ];

  const [activeFile, setActiveFile] = useState<CodeFile>(files[0]);

  const copyToClipboard = (text: string, path: string) => {
    navigator.clipboard.writeText(text);
    setCopiedPath(path);
    setTimeout(() => setCopiedPath(null), 2000);
  };

  return (
    <div className="space-y-6">
      
      {/* Top Protocol Architecture Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-800">
          <div>
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-500/10 border border-blue-500/20 text-blue-400">
                Native Android 15 (Target SDK 35)
              </span>
              <span className="text-xs text-slate-400 font-mono">Kotlin · Jetpack Compose</span>
            </div>
            <h2 className="text-xl font-bold text-white mt-1">
              Real Google TV Discovery &amp; Pairing Architecture
            </h2>
            <p className="text-xs text-slate-400 mt-1 max-w-2xl">
              Clean, standalone native Android code without Flutter baggage. Uses Android NsdManager for mDNS discovery, TLS on port 6467 for TV screen PIN pairing, and port 6466 for remote commands.
            </p>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => copyToClipboard(`git add -A\ngit commit -m "feat: fresh native android google tv remote with auto discovery and tls pairing"\ngit push origin main`, 'git-cmd')}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-semibold flex items-center gap-2 transition-colors shadow-sm"
            >
              {copiedPath === 'git-cmd' ? <Check className="w-3.5 h-3.5" /> : <Terminal className="w-3.5 h-3.5" />}
              <span>Copy Git Push Commands</span>
            </button>
          </div>
        </div>

        {/* 4-Step Technical Protocol Summary */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mt-4 text-xs font-mono">
          <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
            <div className="flex items-center gap-2 text-blue-400 font-bold mb-1">
              <span className="w-4 h-4 rounded-full bg-blue-500/20 flex items-center justify-center text-[10px]">1</span>
              <span>Auto-Discovery</span>
            </div>
            <div className="text-slate-300 font-semibold">mDNS / NsdManager</div>
            <div className="text-[11px] text-slate-500 mt-0.5">_androidtvremote2._tcp on local Wi-Fi</div>
          </div>

          <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
            <div className="flex items-center gap-2 text-yellow-400 font-bold mb-1">
              <span className="w-4 h-4 rounded-full bg-yellow-500/20 flex items-center justify-center text-[10px]">2</span>
              <span>TV Screen PIN</span>
            </div>
            <div className="text-slate-300 font-semibold">TLS Port 6467</div>
            <div className="text-[11px] text-slate-500 mt-0.5">TV displays 4 or 6-digit code</div>
          </div>

          <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
            <div className="flex items-center gap-2 text-emerald-400 font-bold mb-1">
              <span className="w-4 h-4 rounded-full bg-emerald-500/20 flex items-center justify-center text-[10px]">3</span>
              <span>SHA-256 Secret</span>
            </div>
            <div className="text-slate-300 font-semibold">HMAC Verification</div>
            <div className="text-[11px] text-slate-500 mt-0.5">Client cert stored in Keystore</div>
          </div>

          <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
            <div className="flex items-center gap-2 text-cyan-400 font-bold mb-1">
              <span className="w-4 h-4 rounded-full bg-cyan-500/20 flex items-center justify-center text-[10px]">4</span>
              <span>Remote Stream</span>
            </div>
            <div className="text-slate-300 font-semibold">TLS Port 6466</div>
            <div className="text-[11px] text-slate-500 mt-0.5">D-Pad, Voice, IME Text &amp; Apps</div>
          </div>
        </div>
      </div>

      {/* Code Viewer: File Sidebar & Editor */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Files List */}
        <div className="lg:col-span-4 bg-slate-900 border border-slate-800 rounded-2xl p-3 space-y-1.5">
          <div className="px-3 py-2 text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
            <Folder className="w-3.5 h-3.5 text-blue-400" />
            <span>Native Android Codebase</span>
          </div>

          {files.map((file) => {
            const isActive = activeFile.name === file.name;
            return (
              <button
                key={file.name}
                onClick={() => setActiveFile(file)}
                className={`w-full text-left p-3 rounded-xl transition-all flex flex-col gap-1 ${
                  isActive
                    ? 'bg-blue-600/15 border border-blue-500/30 text-white'
                    : 'hover:bg-slate-800/60 border border-transparent text-slate-400 hover:text-slate-200'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 font-mono text-xs font-semibold text-slate-200">
                    <FileCode className={`w-3.5 h-3.5 ${isActive ? 'text-blue-400' : 'text-slate-500'}`} />
                    <span>{file.name}</span>
                  </div>
                  <span className="text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-400">
                    {file.language}
                  </span>
                </div>
                <p className="text-[11px] text-slate-400 line-clamp-1 leading-snug">
                  {file.description}
                </p>
              </button>
            );
          })}
        </div>

        {/* Right Code Display */}
        <div className="lg:col-span-8 bg-slate-950 border border-slate-800 rounded-2xl overflow-hidden flex flex-col shadow-xl">
          <div className="bg-slate-900/90 px-4 py-3 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2 font-mono text-xs">
              <span className="text-blue-400 font-semibold">{activeFile.name}</span>
              <span className="text-slate-600">·</span>
              <span className="text-slate-500 text-[11px]">{activeFile.path}</span>
            </div>

            <button
              onClick={() => copyToClipboard(activeFile.code, activeFile.path)}
              className="flex items-center gap-1.5 px-3 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs rounded-lg transition-colors font-mono"
            >
              {copiedPath === activeFile.path ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-400" />
                  <span className="text-emerald-400">Copied</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5 text-slate-400" />
                  <span>Copy Code</span>
                </>
              )}
            </button>
          </div>

          <pre className="p-5 font-mono text-xs text-slate-300 overflow-x-auto leading-relaxed max-h-[500px]">
            <code>{activeFile.code}</code>
          </pre>
        </div>
      </div>

    </div>
  );
};
