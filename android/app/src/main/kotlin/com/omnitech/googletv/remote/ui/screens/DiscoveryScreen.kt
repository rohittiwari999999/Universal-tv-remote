package com.omnitech.googletv.remote.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import com.omnitech.googletv.remote.ui.theme.*

@Composable
fun DiscoveryScreen(
    devices: List<DiscoveredGoogleTv>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onSelectDevice: (DiscoveredGoogleTv) -> Unit,
    onAddManualIp: (String) -> Unit
) {
    var showManualDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoogleTvDark)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // App Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isScanning) GoogleTvBlue else GoogleTvGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google TV Remote",
                        color = GoogleTvTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isScanning) "Searching Wi-Fi for Google TVs..." else "${devices.size} TV(s) found nearby",
                    color = GoogleTvTextSecondary,
                    fontSize = 13.sp
                )
            }

            IconButton(
                onClick = onStartScan,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GoogleTvSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan",
                    tint = GoogleTvBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Scanning Radar Animation (When searching or zero devices found)
        if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                RadarAnimation(isScanning = isScanning)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 180.dp)
                ) {
                    Text(
                        text = if (isScanning) "Scanning your Wi-Fi Network" else "No Google TVs found",
                        color = GoogleTvTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isScanning)
                            "Looking for _androidtvremote2._tcp services...\nMake sure your phone and TV are on the same Wi-Fi."
                        else
                            "Turn on your Google TV / Chromecast and verify it's connected to Wi-Fi.",
                        color = GoogleTvTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showManualDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleTvCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = GoogleTvBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect by IP Address", color = GoogleTvTextPrimary, fontSize = 14.sp)
                    }
                }
            }
        } else {
            // Discovered Devices List
            Text(
                text = "NEARBY GOOGLE TVs",
                color = GoogleTvTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices) { tv ->
                    DeviceCard(device = tv, onClick = { onSelectDevice(tv) })
                }
            }

            // Bottom Manual Connect Link
            TextButton(
                onClick = { showManualDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(Icons.Default.SettingsEthernet, contentDescription = null, tint = GoogleTvBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("TV not showing? Enter IP Manually", color = GoogleTvBlue, fontSize = 14.sp)
            }
        }
    }

    if (showManualDialog) {
        ManualIpDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { ip ->
                showManualDialog = false
                onAddManualIp(ip)
            }
        )
    }
}

@Composable
fun DeviceCard(device: DiscoveredGoogleTv, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GoogleTvCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GoogleTvSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(GoogleTvBlue.copy(alpha = 0.2f), GoogleTvBlue.copy(alpha = 0.05f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = GoogleTvBlue,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        color = GoogleTvTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (device.isPaired) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PAIRED",
                            color = GoogleTvGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoogleTvGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${device.model} • ${device.hostAddress}",
                        color = GoogleTvTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = GoogleTvBlue),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (device.isPaired) "Connect" else "Pair",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun RadarAnimation(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse1"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Alpha1"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        if (isScanning) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulse1)
                    .clip(CircleShape)
                    .background(GoogleTvBlue.copy(alpha = alpha1))
            )
        }
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(GoogleTvCard)
                .border(2.dp, GoogleTvBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cast,
                contentDescription = null,
                tint = GoogleTvBlue,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun ManualIpDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var ipText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GoogleTvSurface,
        title = {
            Text("Enter Google TV IP Address", color = GoogleTvTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "You can find this on your Google TV: Settings > Network & Internet > Connected Wi-Fi > IP Address.",
                    color = GoogleTvTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    placeholder = { Text("e.g. 192.168.1.105", color = GoogleTvTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GoogleTvTextPrimary,
                        unfocusedTextColor = GoogleTvTextPrimary,
                        focusedBorderColor = GoogleTvBlue,
                        unfocusedBorderColor = GoogleTvCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (ipText.isNotBlank()) onConfirm(ipText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = GoogleTvBlue)
            ) {
                Text("Search & Connect", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GoogleTvTextSecondary)
            }
        }
    )
}
