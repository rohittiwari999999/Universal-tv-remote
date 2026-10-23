package com.omnitech.googletv.remote.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnitech.googletv.remote.ir.CoocaaIrController
import com.omnitech.googletv.remote.ir.IrBlasterManager
import com.omnitech.googletv.remote.ir.TvBrand
import com.omnitech.googletv.remote.ui.theme.*

@Composable
fun TopPowerBrandBar(
    selectedBrand: TvBrand,
    hasIrEmitter: Boolean,
    onTransmitPower: (TvBrand) -> Unit,
    onSelectBrand: (TvBrand) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coocaaController = remember { CoocaaIrController(context) }
    var showBrandDialog by remember { mutableStateOf(false) }
    var showCoocaaTesterDialog by remember { mutableStateOf(false) }
    var isPowerBlasting by remember { mutableStateOf(false) }
    val activeCoocaaVariant = remember(showCoocaaTesterDialog) { coocaaController.getSelectedVariant() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1E1324),
                        GoogleTvSurface,
                        Color(0xFF131F2E)
                    )
                )
            )
            .border(1.dp, GoogleTvCardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Power ON Button with IR Blaster code execution
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isPowerBlasting = true
                    onTransmitPower(selectedBrand)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoogleTvRed
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power ON",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "POWER ON",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (hasIrEmitter) "IR BLASTER" else "IR / NETWORK",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Coocaa Specific: Tune / Frequency Tester Quick Launcher
            if (selectedBrand.id.equals("coocaa", ignoreCase = true)) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showCoocaaTesterDialog = true
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoogleTvBlue.copy(alpha = 0.15f))
                        .border(1.dp, GoogleTvBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tune Coocaa Frequencies",
                        tint = GoogleTvBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // TV Brand Selector Button (Default: Coocaa TV)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showBrandDialog = true
                },
                color = GoogleTvCard,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvCardBorder),
                modifier = Modifier.height(44.dp).weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = GoogleTvBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedBrand.name,
                            color = GoogleTvTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (selectedBrand.id == "coocaa") "Tap to tune/switch" else "Tap to change",
                            color = GoogleTvTextSecondary,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Brand",
                        tint = GoogleTvTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Info status subtitle (Coocaa 38kHz NEC code indicator)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (hasIrEmitter) GoogleTvGreen else GoogleTvYellow)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasIrEmitter) {
                        "IR Ready • ${selectedBrand.name} (${selectedBrand.carrierFreq / 1000} kHz)"
                    } else {
                        "IR & WoL Ready • ${selectedBrand.name}"
                    },
                    color = GoogleTvTextSecondary,
                    fontSize = 10.sp
                )
            }

            if (selectedBrand.id == "coocaa") {
                Text(
                    text = "#${activeCoocaaVariant.index} ${activeCoocaaVariant.hexCode}",
                    color = GoogleTvBlue,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showCoocaaTesterDialog = true }
                )
            }
        }
    }

    if (showCoocaaTesterDialog) {
        CoocaaFrequencyTesterDialog(
            onDismiss = { showCoocaaTesterDialog = false }
        )
    }

    // TV Brand Selection Modal Dialog
    if (showBrandDialog) {
        Dialog(onDismissRequest = { showBrandDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GoogleTvSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Select TV Brand",
                                color = GoogleTvTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configures IR Blaster frequency & power code",
                                color = GoogleTvTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { showBrandDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GoogleTvTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(IrBlasterManager.SUPPORTED_BRANDS) { brand ->
                            val isSelected = brand.id == selectedBrand.id
                            Surface(
                                onClick = {
                                    onSelectBrand(brand)
                                    showBrandDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) GoogleTvBlue.copy(alpha = 0.15f) else GoogleTvCard,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) GoogleTvBlue else GoogleTvCardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = brand.name,
                                                color = if (isSelected) GoogleTvBlue else GoogleTvTextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            )
                                            if (brand.id == "coocaa") {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(GoogleTvGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "DEFAULT",
                                                        color = GoogleTvGreen,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = brand.description,
                                            color = GoogleTvTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = GoogleTvBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Tip: Coocaa TVs (Skyworth) use 38kHz NEC IR protocol. Point your phone's top IR blaster at the TV sensor.",
                        color = GoogleTvTextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
