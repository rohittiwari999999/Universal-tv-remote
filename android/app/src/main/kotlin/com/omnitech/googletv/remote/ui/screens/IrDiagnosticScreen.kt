package com.omnitech.googletv.remote.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitech.googletv.remote.ir.CoocaaIrController
import com.omnitech.googletv.remote.ir.IrBlasterManager
import com.omnitech.googletv.remote.ir.IrExecutionResult
import com.omnitech.googletv.remote.ir.IrHardwareStatus
import com.omnitech.googletv.remote.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun IrDiagnosticScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val irManager = remember { IrBlasterManager(context) }
    val coocaaController = remember { CoocaaIrController(context) }

    var hardwareDiag by remember { mutableStateOf(irManager.getHardwareDiagnostic()) }
    var configuredHex by remember { mutableStateOf(coocaaController.getCustomHexCode()) }
    var configuredFreq by remember { mutableStateOf(coocaaController.getCustomFrequency().toString()) }
    var selectedRepeat by remember { mutableStateOf(coocaaController.getCustomRepeat()) }
    var rawPulsesInput by remember { mutableStateOf(coocaaController.getCustomRawPulses()) }
    var inputMode by remember { mutableStateOf(0) } // 0: Hex Code, 1: Raw Microsecond Pulses

    var lastExecutionResult by remember { mutableStateOf<IrExecutionResult?>(null) }
    var isTransmitting by remember { mutableStateOf(false) }

    fun refreshHardware() {
        hardwareDiag = irManager.getHardwareDiagnostic()
        Toast.makeText(context, "Hardware status refreshed", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoogleTvDark)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GoogleTvSurface)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoogleTvTextPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "IR Blaster Diagnostics",
                        color = GoogleTvTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "iQOO Neo 10 & Coocaa Y74 Plus 55\"",
                        color = GoogleTvBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = { refreshHardware() },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GoogleTvSurface)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoogleTvBlue)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: Hardware Capabilities & Driver Detection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, GoogleTvCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = GoogleTvSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hardwareDiag.hasIrEmitter) GoogleTvGreen.copy(alpha = 0.2f)
                                    else GoogleTvRed.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hardwareDiag.hasIrEmitter) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hardwareDiag.hasIrEmitter) GoogleTvGreen else GoogleTvRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Phone IR Hardware Status",
                            color = GoogleTvTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = if (hardwareDiag.hasIrEmitter) GoogleTvGreen else GoogleTvRed,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (hardwareDiag.hasIrEmitter) "READY" else "NOT FOUND",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail rows
                HardwareDetailRow(
                    label = "PackageManager feature (FEATURE_CONSUMER_IR)",
                    value = if (hardwareDiag.hasSystemFeature) "Detected" else "Missing",
                    isOk = hardwareDiag.hasSystemFeature
                )
                HardwareDetailRow(
                    label = "ConsumerIrManager system service",
                    value = if (hardwareDiag.isServiceAvailable) "Available" else "Null",
                    isOk = hardwareDiag.isServiceAvailable
                )
                HardwareDetailRow(
                    label = "ConsumerIrManager.hasIrEmitter()",
                    value = if (hardwareDiag.hasIrEmitter) "true (Hardware Emitter Active)" else "false",
                    isOk = hardwareDiag.hasIrEmitter
                )

                // Supported Carrier Frequencies
                val rangesStr = if (hardwareDiag.supportedCarrierRanges.isNotEmpty()) {
                    hardwareDiag.supportedCarrierRanges.joinToString(", ") { "${it.minFrequencyHz / 1000} - ${it.maxFrequencyHz / 1000} kHz" }
                } else if (hardwareDiag.hasIrEmitter) {
                    "30 kHz - 60 kHz (Standard ConsumerIR)"
                } else {
                    "None"
                }

                HardwareDetailRow(
                    label = "Supported Carrier Frequencies (getCarrierFrequencies)",
                    value = rangesStr,
                    isOk = hardwareDiag.hasIrEmitter
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hardwareDiag.diagnosticSummary,
                    color = GoogleTvTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: Configurable Coocaa Y74 Plus 55" Code Editor
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, GoogleTvBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = GoogleTvSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Target: Coocaa Y74 Plus 55\"",
                            color = GoogleTvTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure verified Power IR code & modulation frequency",
                            color = GoogleTvTextSecondary,
                            fontSize = 10.5.sp
                        )
                    }

                    // Mode switch: Hex vs Raw
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoogleTvDark)
                            .padding(2.dp)
                    ) {
                        Surface(
                            onClick = { inputMode = 0 },
                            color = if (inputMode == 0) GoogleTvBlue else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "HEX",
                                color = if (inputMode == 0) Color.White else GoogleTvTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            onClick = { inputMode = 1 },
                            color = if (inputMode == 1) GoogleTvBlue else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "RAW",
                                color = if (inputMode == 1) Color.White else GoogleTvTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (inputMode == 0) {
                    // HEX CODE INPUT
                    OutlinedTextField(
                        value = configuredHex,
                        onValueChange = {
                            configuredHex = it
                            coocaaController.setCustomHexCode(it)
                        },
                        label = { Text("32-bit NEC Hex Code") },
                        placeholder = { Text("0x40BF12ED") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoogleTvBlue,
                            unfocusedBorderColor = GoogleTvCardBorder,
                            focusedTextColor = GoogleTvTextPrimary,
                            unfocusedTextColor = GoogleTvTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick-load candidate presets for Coocaa Y74 Plus Google TV
                    Text(
                        text = "Candidate Presets for Coocaa Y74 Plus Google TV:",
                        color = GoogleTvTextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val presets = listOf(
                        Pair("0x40BF12ED", "iQOO Model 1 OEM (38kHz)"),
                        Pair("0x40BF02FD", "iQOO Model 2 (38kHz)"),
                        Pair("0x47EF827D", "Skyworth 4K Y74 (38kHz)"),
                        Pair("0x00FF12ED", "Google TV Std (38kHz)"),
                        Pair("0x707030CF", "Coocaa 4K (38.4kHz)")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.take(3).forEach { (hex, label) ->
                            Surface(
                                onClick = {
                                    configuredHex = hex
                                    coocaaController.setCustomHexCode(hex)
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (configuredHex.equals(hex, ignoreCase = true)) GoogleTvBlue.copy(alpha = 0.25f) else GoogleTvDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (configuredHex.equals(hex, ignoreCase = true)) GoogleTvBlue else GoogleTvCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(hex, color = GoogleTvTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(label.split(" ").first(), color = GoogleTvTextSecondary, fontSize = 8.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.drop(3).forEach { (hex, label) ->
                            Surface(
                                onClick = {
                                    configuredHex = hex
                                    coocaaController.setCustomHexCode(hex)
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (configuredHex.equals(hex, ignoreCase = true)) GoogleTvBlue.copy(alpha = 0.25f) else GoogleTvDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (configuredHex.equals(hex, ignoreCase = true)) GoogleTvBlue else GoogleTvCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(hex, color = GoogleTvTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(label, color = GoogleTvTextSecondary, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                } else {
                    // RAW TIMINGS INPUT
                    OutlinedTextField(
                        value = rawPulsesInput,
                        onValueChange = {
                            rawPulsesInput = it
                            coocaaController.setCustomRawPulses(it)
                        },
                        label = { Text("Raw Microsecond Timings (Comma-Separated)") },
                        placeholder = { Text("9000, 4500, 560, 560, 560, 1690...") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoogleTvBlue,
                            unfocusedBorderColor = GoogleTvCardBorder,
                            focusedTextColor = GoogleTvTextPrimary,
                            unfocusedTextColor = GoogleTvTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Paste timing pulses extracted from physical remote learning or Flipper Zero.",
                        color = GoogleTvTextSecondary,
                        fontSize = 9.5.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Carrier Frequency & Repeat configuration
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = configuredFreq,
                        onValueChange = {
                            configuredFreq = it
                            it.toIntOrNull()?.let { f -> coocaaController.setCustomFrequency(f) }
                        },
                        label = { Text("Carrier Freq (Hz)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoogleTvBlue,
                            unfocusedBorderColor = GoogleTvCardBorder,
                            focusedTextColor = GoogleTvTextPrimary,
                            unfocusedTextColor = GoogleTvTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Repeat Frames: 1 Frame (Single Tap 67p) vs 2 Frames (Standby Wake)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Burst Frames:", color = GoogleTvTextSecondary, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = {
                                    selectedRepeat = 1
                                    coocaaController.setCustomRepeat(1)
                                },
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                                color = if (selectedRepeat == 1) GoogleTvBlue else GoogleTvDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvCardBorder),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("1 (67p)", color = if (selectedRepeat == 1) Color.White else GoogleTvTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                onClick = {
                                    selectedRepeat = 2
                                    coocaaController.setCustomRepeat(2)
                                },
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                                color = if (selectedRepeat == 2) GoogleTvBlue else GoogleTvDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvCardBorder),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("2 (Wake)", color = if (selectedRepeat == 2) Color.White else GoogleTvTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Transmit Action Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isTransmitting = true
                        coroutineScope.launch {
                            val freq = configuredFreq.toIntOrNull() ?: 38000
                            val result = if (inputMode == 0) {
                                coocaaController.transmitConfiguredCode(configuredHex, freq, selectedRepeat)
                            } else {
                                coocaaController.transmitRawTimingString(rawPulsesInput, freq)
                            }
                            lastExecutionResult = result
                            isTransmitting = false
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleTvRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTransmitting) "TRANSMITTING VIA CONSUMER_IR..." else "TEST TRANSMIT (CALL NATIVE API)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: Live Execution Log & Confirmed Response Disclaimer
        if (lastExecutionResult != null) {
            val res = lastExecutionResult!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        if (res.apiExecutedSuccessfully) GoogleTvGreen.copy(alpha = 0.5f) else GoogleTvRed.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (res.apiExecutedSuccessfully) Color(0xFF102619) else Color(0xFF261012)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (res.apiExecutedSuccessfully) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (res.apiExecutedSuccessfully) GoogleTvGreen else GoogleTvRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (res.apiExecutedSuccessfully) "NATIVE API EXECUTION: SUCCESS" else "NATIVE API EXECUTION: FAILED",
                            color = if (res.apiExecutedSuccessfully) GoogleTvGreen else GoogleTvRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = res.message,
                        color = GoogleTvTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Carrier: ${res.carrierFreqHz} Hz • Pulses: ${res.pulseCount} • Est. Duration: ${res.durationMicros / 1000} ms",
                        color = GoogleTvTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // CRITICAL DISCLAIMER (Requirement 10)
                    Surface(
                        color = GoogleTvDark.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = GoogleTvYellow, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "IMPORTANT: An API call success confirms that Android's ConsumerIrManager.transmit() executed the IR emitter. It DOES NOT prove the TV has turned on. You must verify if your Coocaa Y74 Plus screen or standby LED responds.",
                                color = GoogleTvYellow,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // SECTION 4: How to Obtain Verified Compatible Code for Coocaa Y74 Plus 55"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, GoogleTvCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = GoogleTvSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoogleTvYellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How to find verified code for Coocaa Y74 Plus:",
                        color = GoogleTvTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                GuideStep(
                    step = "1",
                    title = "Check iQOO Neo 10 built-in Smart Remote",
                    desc = "In your iQOO phone's native 'Smart Remote' app, test TV -> Skyworth / Coocaa. When Power toggles your Y74 Plus TV, note the Model number. Model 1 is 0x40BF12ED and Model 3 is 0x47EF827D."
                )

                GuideStep(
                    step = "2",
                    title = "Capture from original physical remote",
                    desc = "If you have an IR receiver (ESP32 / Arduino / USB dongle or Flipper Zero), point the original Coocaa remote at it, press Power, and view the decoded 32-bit NEC hex code."
                )

                GuideStep(
                    step = "3",
                    title = "Use Standby Wake (2-Frames)",
                    desc = "Coocaa 55\" Google TVs enter deep sleep (TSOP sensor AGC sleep). Switch 'Burst Frames' to '2 (Wake)' above so the TV wakes from deep sleep."
                )
            }
        }
    }
}

@Composable
private fun HardwareDetailRow(label: String, value: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = GoogleTvTextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = if (isOk) GoogleTvGreen else GoogleTvRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GuideStep(step: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(GoogleTvBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(step, color = GoogleTvBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = GoogleTvTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = GoogleTvTextSecondary, fontSize = 10.5.sp, lineHeight = 14.sp)
        }
    }
}
