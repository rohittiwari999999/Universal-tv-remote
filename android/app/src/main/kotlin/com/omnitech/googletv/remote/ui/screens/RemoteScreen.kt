package com.omnitech.googletv.remote.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import com.omnitech.googletv.remote.model.RemoteConnectionState
import com.omnitech.googletv.remote.model.TvKey
import com.omnitech.googletv.remote.ui.theme.*

@Composable
fun RemoteScreen(
    device: DiscoveredGoogleTv,
    connectionState: RemoteConnectionState,
    onSendKey: (TvKey) -> Unit,
    onSendText: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedMode by remember { mutableStateOf(0) } // 0: D-Pad, 1: Touchpad, 2: Keyboard
    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    fun triggerKey(key: TvKey) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onSendKey(key)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoogleTvDark)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Connected Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GoogleTvSurface)
                .border(1.dp, GoogleTvCardBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(GoogleTvGreen)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = device.name,
                        color = GoogleTvTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Connected • ${device.hostAddress}",
                        color = GoogleTvTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GoogleTvCard)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disconnect",
                    tint = GoogleTvTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Selector: D-Pad / Touchpad / Keyboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GoogleTvSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val modes = listOf("D-Pad", "Touchpad", "Keyboard")
            modes.forEachIndexed { index, modeTitle ->
                val isSelected = selectedMode == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GoogleTvBlue else Color.Transparent)
                        .clickable { selectedMode = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = modeTitle,
                        color = if (isSelected) Color.White else GoogleTvTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Top Control Row: Power, Settings, Mute
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteIconButton(
                icon = Icons.Default.PowerSettingsNew,
                label = "Power",
                tint = GoogleTvRed,
                onClick = { triggerKey(TvKey.POWER) }
            )
            RemoteIconButton(
                icon = Icons.Default.Settings,
                label = "Settings",
                tint = GoogleTvTextPrimary,
                onClick = { triggerKey(TvKey.SETTINGS) }
            )
            RemoteIconButton(
                icon = Icons.Default.VolumeMute,
                label = "Mute",
                tint = GoogleTvYellow,
                onClick = { triggerKey(TvKey.VOLUME_MUTE) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center Interaction Area (D-Pad, Touchpad, or Keyboard)
        when (selectedMode) {
            0 -> DPadControl(onSendKey = { triggerKey(it) })
            1 -> TouchpadControl(onSendKey = { triggerKey(it) })
            2 -> KeyboardControl(
                textInput = textInput,
                onTextChange = { textInput = it },
                onSend = {
                    if (textInput.isNotBlank()) {
                        onSendText(textInput)
                        textInput = ""
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Essential Navigation Bar: Back, Google Assistant, Home
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { triggerKey(TvKey.BACK) },
                colors = ButtonDefaults.buttonColors(containerColor = GoogleTvSurface),
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoogleTvTextPrimary)
            }

            // Google Assistant (Large Center Button with Google 4-color accent)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(GoogleTvBlue, GoogleTvRed, GoogleTvYellow, GoogleTvGreen, GoogleTvBlue)
                        )
                    )
                    .clickable { triggerKey(TvKey.ASSISTANT) }
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(GoogleTvDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Google Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Button(
                onClick = { triggerKey(TvKey.HOME) },
                colors = ButtonDefaults.buttonColors(containerColor = GoogleTvSurface),
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = GoogleTvTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Volume & Playback Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Volume Rocker
            Card(
                colors = CardDefaults.cardColors(containerColor = GoogleTvSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GoogleTvCardBorder)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    IconButton(onClick = { triggerKey(TvKey.VOLUME_UP) }) {
                        Icon(Icons.Default.Add, contentDescription = "Vol +", tint = GoogleTvTextPrimary)
                    }
                    Text("VOL", color = GoogleTvTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { triggerKey(TvKey.VOLUME_DOWN) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Vol -", tint = GoogleTvTextPrimary)
                    }
                }
            }

            // Media Playback Controls
            Card(
                colors = CardDefaults.cardColors(containerColor = GoogleTvSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GoogleTvCardBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    IconButton(onClick = { triggerKey(TvKey.REWIND) }) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = GoogleTvTextPrimary)
                    }
                    IconButton(onClick = { triggerKey(TvKey.PLAY_PAUSE) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = GoogleTvBlue)
                    }
                    IconButton(onClick = { triggerKey(TvKey.FAST_FORWARD) }) {
                        Icon(Icons.Default.FastForward, contentDescription = "Fast Forward", tint = GoogleTvTextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Launch App Shortcuts
        Text(
            text = "QUICK APPS",
            color = GoogleTvTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppShortcutChip("YouTube", Color(0xFFFF0000), Modifier.weight(1f)) {
                onLaunchApp("com.google.android.youtube.tv")
            }
            AppShortcutChip("Netflix", Color(0xFFE50914), Modifier.weight(1f)) {
                onLaunchApp("com.netflix.ninja")
            }
            AppShortcutChip("Prime", Color(0xFF00A8E1), Modifier.weight(1f)) {
                onLaunchApp("com.amazon.amazonvideo.livingroom")
            }
            AppShortcutChip("Hotstar", Color(0xFF0C20D6), Modifier.weight(1f)) {
                onLaunchApp("in.startv.hotstar")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun DPadControl(onSendKey: (TvKey) -> Unit) {
    Box(
        modifier = Modifier
            .size(230.dp)
            .clip(CircleShape)
            .background(GoogleTvSurface)
            .border(2.dp, GoogleTvCardBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Up
        IconButton(
            onClick = { onSendKey(TvKey.DPAD_UP) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(44.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = GoogleTvTextPrimary, modifier = Modifier.size(32.dp))
        }

        // Down
        IconButton(
            onClick = { onSendKey(TvKey.DPAD_DOWN) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .size(44.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = GoogleTvTextPrimary, modifier = Modifier.size(32.dp))
        }

        // Left
        IconButton(
            onClick = { onSendKey(TvKey.DPAD_LEFT) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .size(44.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = GoogleTvTextPrimary, modifier = Modifier.size(32.dp))
        }

        // Right
        IconButton(
            onClick = { onSendKey(TvKey.DPAD_RIGHT) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(44.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = GoogleTvTextPrimary, modifier = Modifier.size(32.dp))
        }

        // Center OK Button
        Button(
            onClick = { onSendKey(TvKey.DPAD_CENTER) },
            colors = ButtonDefaults.buttonColors(containerColor = GoogleTvCard),
            shape = CircleShape,
            modifier = Modifier
                .size(92.dp)
                .border(1.dp, GoogleTvBlue.copy(alpha = 0.5f), CircleShape),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("OK", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TouchpadControl(onSendKey: (TvKey) -> Unit) {
    var accumulatedDx by remember { mutableFloatStateOf(0f) }
    var accumulatedDy by remember { mutableFloatStateOf(0f) }
    val threshold = 70f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GoogleTvSurface)
            .border(1.dp, GoogleTvCardBorder, RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDx += dragAmount.x
                        accumulatedDy += dragAmount.y

                        if (accumulatedDx > threshold) {
                            onSendKey(TvKey.DPAD_RIGHT)
                            accumulatedDx = 0f
                        } else if (accumulatedDx < -threshold) {
                            onSendKey(TvKey.DPAD_LEFT)
                            accumulatedDx = 0f
                        }

                        if (accumulatedDy > threshold) {
                            onSendKey(TvKey.DPAD_DOWN)
                            accumulatedDy = 0f
                        } else if (accumulatedDy < -threshold) {
                            onSendKey(TvKey.DPAD_UP)
                            accumulatedDy = 0f
                        }
                    },
                    onDragEnd = {
                        accumulatedDx = 0f
                        accumulatedDy = 0f
                    }
                )
            }
            .clickable { onSendKey(TvKey.DPAD_CENTER) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.TouchApp, contentDescription = null, tint = GoogleTvBlue, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Swipe to Navigate", color = GoogleTvTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Tap anywhere for OK / Select", color = GoogleTvTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun KeyboardControl(textInput: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        colors = CardDefaults.cardColors(containerColor = GoogleTvSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GoogleTvCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Type directly into Google TV Search",
                color = GoogleTvTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextChange,
                placeholder = { Text("Search movies, YouTube, apps...", color = GoogleTvTextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoogleTvBlue,
                    unfocusedBorderColor = GoogleTvCardBorder,
                    focusedTextColor = GoogleTvTextPrimary,
                    unfocusedTextColor = GoogleTvTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSend,
                colors = ButtonDefaults.buttonColors(containerColor = GoogleTvBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Text to TV", color = Color.White)
            }
        }
    }
}

@Composable
fun RemoteIconButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(GoogleTvSurface)
                .border(1.dp, GoogleTvCardBorder, CircleShape)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = GoogleTvTextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun AppShortcutChip(name: String, accentColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GoogleTvSurface)
            .border(1.dp, GoogleTvCardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                color = GoogleTvTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
