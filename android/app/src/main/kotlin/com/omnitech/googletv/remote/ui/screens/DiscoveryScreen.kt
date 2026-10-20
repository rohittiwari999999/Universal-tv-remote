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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import com.omnitech.googletv.remote.ui.theme.*

@Composable
fun DiscoveryScreen(
    devices: List<DiscoveredGoogleTv>,
    isScanning: Boolean,
    connectingDeviceId: String? = null,
    errorMessage: String? = null,
    onStartScan: () -> Unit,
    onSelectDevice: (DiscoveredGoogleTv) -> Unit,
    onForcePairDevice: (DiscoveredGoogleTv) -> Unit,
    onUnpairDevice: (DiscoveredGoogleTv) -> Unit,
    onResetAllSettings: () -> Unit,
    onAddManualIp: (String) -> Unit,
    onDismissError: () -> Unit
) {
    var showManualDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

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
                    text = if (isScanning) "Searching Wi-Fi for Google TVs..." else "${devices.size} TV(s) available",
                    color = GoogleTvTextSecondary,
                    fontSize = 13.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GoogleTvSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Reset Settings",
                        tint = GoogleTvTextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

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
        }

        // Connection Error Alert Banner
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = GoogleTvRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoogleTvRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = GoogleTvRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connection Notice",
                            color = GoogleTvRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = errorMessage,
                            color = GoogleTvTextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    IconButton(onClick = onDismissError, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = GoogleTvTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                        text = if (isScanning) "Searching Local Wi-Fi..." else "No Google TVs found yet",
                        color = GoogleTvTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Ensure Phone & TV are on the same Wi-Fi.\n2. Or connect directly using your TV's IP address.",
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
                        Text("Add TV by IP Address", color = GoogleTvTextPrimary, fontSize = 14.sp)
                    }
                }
            }
        } else {
            // Discovered Devices List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AVAILABLE GOOGLE TVs",
                    color = GoogleTvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = { showManualDialog = true }, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GoogleTvBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add IP", color = GoogleTvBlue, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices) { tv ->
                    val isConnecting = (connectingDeviceId == tv.id)
                    DeviceCard(
                        device = tv,
                        isConnecting = isConnecting,
                        onClick = { onSelectDevice(tv) },
                        onForcePair = { onForcePairDevice(tv) },
                        onUnpair = { onUnpairDevice(tv) }
                    )
                }
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

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All TV Pairings?") },
            text = {
                Text("This will clear all saved pairings and reset your client certificate credentials. You can pair afresh with your TV.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetAllSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleTvRed)
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = GoogleTvSurface
        )
    }
}

@Composable
fun DeviceCard(
    device: DiscoveredGoogleTv,
    isConnecting: Boolean = false,
    onClick: () -> Unit,
    onForcePair: () -> Unit,
    onUnpair: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
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
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${device.model} • ${device.hostAddress}",
                        color = GoogleTvTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = GoogleTvTextSecondary)
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        modifier = Modifier.background(GoogleTvSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Pair Again (New PIN)", color = GoogleTvTextPrimary) },
                            onClick = {
                                expandedMenu = false
                                onForcePair()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = GoogleTvBlue)
                            }
                        )
                        if (device.isPaired || device.id.startsWith("manual_")) {
                            DropdownMenuItem(
                                text = { Text("Unpair / Remove", color = GoogleTvRed) },
                                onClick = {
                                    expandedMenu = false
                                    onUnpair()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = GoogleTvRed)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (device.isPaired) {
                    Button(
                        onClick = onClick,
                        enabled = !isConnecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoogleTvBlue,
                            disabledContainerColor = GoogleTvBlue.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect Remote", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = onForcePair,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(42.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvCardBorder)
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = GoogleTvTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-pair", color = GoogleTvTextSecondary, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onForcePair,
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleTvBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pair with TV (Get PIN on Screen)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
                .background(GoogleTvSurface)
                .border(2.dp, GoogleTvBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GoogleTvCardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = GoogleTvSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Connect by TV IP Address",
                    color = GoogleTvTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Find your TV IP address in TV Settings > Network & Internet > Wi-Fi > Status.",
                    color = GoogleTvTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it.trim() },
                    placeholder = { Text("e.g. 192.168.1.105", color = GoogleTvTextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoogleTvBlue,
                        unfocusedBorderColor = GoogleTvCardBorder,
                        focusedContainerColor = GoogleTvDark,
                        unfocusedContainerColor = GoogleTvDark,
                        focusedTextColor = GoogleTvTextPrimary,
                        unfocusedTextColor = GoogleTvTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = GoogleTvTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(ipText) },
                        enabled = ipText.length >= 7,
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleTvBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add TV", color = Color.White)
                    }
                }
            }
        }
    }
}
