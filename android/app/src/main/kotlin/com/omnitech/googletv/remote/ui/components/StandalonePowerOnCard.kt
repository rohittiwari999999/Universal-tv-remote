package com.omnitech.googletv.remote.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitech.googletv.remote.ir.IrBlasterManager
import com.omnitech.googletv.remote.ir.TvBrand
import com.omnitech.googletv.remote.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Standalone IR Blaster Power ON Card.
 * Positioned on the Front Page (Discovery / Pairing) directly below "Add TV by IP Address".
 *
 * Use-case: When Google TV is off or in deep standby, it disconnects from Wi-Fi
 * and cannot pair. Tap POWER ON to send an IR blaster beam (e.g. Coocaa 38.4kHz Skyworth Raw),
 * waking the TV so it connects to Wi-Fi and pairs smoothly.
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
    val coroutineScope = rememberCoroutineScope()
    var isBlasting by remember { mutableStateOf(false) }
    var showBrandDialog by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }

    val displayProtocol = remember(selectedBrand) {
        if (selectedBrand.id.equals("coocaa", ignoreCase = true)) "Skyworth Raw" else "NEC"
    }
    val displayCode = remember(selectedBrand) {
        if (selectedBrand.id.equals("coocaa", ignoreCase = true)) "38.4 kHz Raw Pattern" else "${selectedBrand.carrierFreq / 1000} kHz"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        GoogleTvRed.copy(alpha = 0.6f),
                        GoogleTvCardBorder,
                        GoogleTvBlue.copy(alpha = 0.5f)
                    )
                ),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = GoogleTvSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Header: IR Blaster badge & status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GoogleTvRed.copy(alpha = 0.18f))
                            .border(1.dp, GoogleTvRed.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GoogleTvRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "TV POWER ON (IR BLASTER)",
                            color = GoogleTvTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "For Offline or Unpaired TV",
                            color = GoogleTvTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Brand Pill selector
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showBrandDialog = true
                    },
                    color = GoogleTvDark,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvCardBorder),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = GoogleTvBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = selectedBrand.name,
                            color = GoogleTvTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = GoogleTvTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Explanatory note in Hindi/English as user specified
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
                    showSuccessToast = true
                    onTransmitPower(selectedBrand)
                    coroutineScope.launch {
                        delay(1200)
                        isBlasting = false
                        delay(2500)
                        showSuccessToast = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBlasting) Color(0xFFE53935) else GoogleTvRed
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Turn ON TV",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBlasting) "TRANSMITTING IR CODE..." else "POWER ON TV (IR BLASTER)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            // Real-time IR beam feedback banner
            AnimatedVisibility(
                visible = showSuccessToast,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Surface(
                        color = Color(0xFF13281E),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GoogleTvGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "IR Sent: ${selectedBrand.name} $displayProtocol ($displayCode). TV will connect to Wi-Fi now!",
                                color = GoogleTvTextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info with frequency & code info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (hasIrEmitter) GoogleTvGreen else GoogleTvBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasIrEmitter) "Hardware IR Emitter Active" else "IR Ready / Network Wake",
                        color = GoogleTvTextSecondary,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "$displayProtocol • $displayCode",
                    color = GoogleTvBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Brand Selection Dialog (Uses standard Material3 AlertDialog with vertical scroll - 100% compile safe)
    if (showBrandDialog) {
        val brands = remember { IrBlasterManager.SUPPORTED_BRANDS }
        AlertDialog(
            onDismissRequest = { showBrandDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = GoogleTvBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select TV Brand", color = GoogleTvTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showBrandDialog = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GoogleTvTextSecondary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Configures carrier frequency (e.g. Coocaa 38.4 kHz Raw) for your TV model:",
                        color = GoogleTvTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    brands.forEach { brand ->
                        val isSelected = (brand.id == selectedBrand.id)
                        Surface(
                            onClick = {
                                onSelectBrand(brand)
                                showBrandDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GoogleTvBlue.copy(alpha = 0.18f) else GoogleTvDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) GoogleTvBlue else GoogleTvCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(brand.name, color = GoogleTvTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        if (brand.id == "coocaa") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = GoogleTvBlue,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "DEFAULT 38.4kHz",
                                                    color = Color.White,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(brand.description, color = GoogleTvTextSecondary, fontSize = 11.sp)
                                }

                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = GoogleTvBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = GoogleTvSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
