package com.omnitech.googletv.remote.ui.components

import android.content.Context
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitech.googletv.remote.ir.CoocaaIrController
import com.omnitech.googletv.remote.ir.CoocaaVariantItem
import com.omnitech.googletv.remote.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CoocaaFrequencyTesterDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val controller = remember { CoocaaIrController(context) }
    val variants = remember { CoocaaIrController.ALL_VARIANTS }

    var selectedIndex by remember { mutableStateOf(controller.getSelectedVariantIndex()) }
    var testedIndex by remember { mutableStateOf<Int?>(null) }
    var isAutoTesting by remember { mutableStateOf(false) }
    var currentAutoVariant by remember { mutableStateOf<CoocaaVariantItem?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Auto test sequence: fires 1 code every 3.0s cleanly
    LaunchedEffect(isAutoTesting) {
        if (isAutoTesting) {
            for (variant in variants) {
                if (!isAutoTesting) break
                currentAutoVariant = variant
                testedIndex = variant.index
                controller.sendPowerOnSignal(variant.index)
                toastMessage = "Auto Test: ${variant.name} (${variant.frequencyHz / 1000}kHz)"
                Toast.makeText(context, "Testing: ${variant.name}", Toast.LENGTH_SHORT).show()
                delay(3000)
            }
            isAutoTesting = false
            currentAutoVariant = null
            testedIndex = null
        }
    }

    AlertDialog(
        onDismissRequest = {
            isAutoTesting = false
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(GoogleTvRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = GoogleTvRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Coocaa Frequency & Code Tester", color = GoogleTvTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("iQOO Neo 10 IR Blaster (36 - 40 kHz)", color = GoogleTvTextSecondary, fontSize = 11.sp)
                    }
                }
                IconButton(
                    onClick = {
                        isAutoTesting = false
                        onDismiss()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = GoogleTvTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Auto Test Card
                Surface(
                    color = if (isAutoTesting) GoogleTvRed.copy(alpha = 0.18f) else GoogleTvDark,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAutoTesting) GoogleTvRed else GoogleTvBlue.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAutoTesting) Icons.Default.PlayCircle else Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (isAutoTesting) GoogleTvRed else GoogleTvBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAutoTesting) "AUTO-TESTING ALL CODES (3s PAUSE)" else "Automatic Step-by-Step Test",
                                color = GoogleTvTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAutoTesting) {
                                "Currently firing: ${currentAutoVariant?.name ?: "..."}\nTV ON hote hi 'STOP TEST' dabayein aur use Default set karein."
                            } else {
                                "Phone ko TV ke aage rakhein. App har 3 second me 1-1 code test karega. TV jaise hi on ho, Pause karein:"
                            },
                            color = GoogleTvTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isAutoTesting = !isAutoTesting
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAutoTesting) GoogleTvRed else GoogleTvBlue
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(if (isAutoTesting) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAutoTesting) "STOP / PAUSE AUTO-TEST" else "START AUTO TEST (1 BY 1)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // iQOO Neo 10 Match Banner
                Surface(
                    color = Color(0xFF13281E),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = GoogleTvGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "iQOO Neo 10 / Vivo Match (Important)",
                                color = GoogleTvGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Aapke Vivo Smart Remote me Coocaa TV ke liye Skyworth Model 1 (0x40BF12ED) ya Model 3 (0x47EF827D) chalta hai. Pehle Preset 1 aur Preset 2 ka 'TEST ON' dabayein!",
                                color = GoogleTvTextSecondary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Ek-ek karke Test karein (All Frequencies & Presets):",
                    color = GoogleTvTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                variants.forEach { variant ->
                    val isTestingThis = (testedIndex == variant.index)
                    val isDefault = (selectedIndex == variant.index)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTestingThis) GoogleTvRed.copy(alpha = 0.15f) else if (isDefault) GoogleTvBlue.copy(alpha = 0.1f) else GoogleTvDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                isTestingThis -> GoogleTvRed
                                isDefault -> GoogleTvBlue
                                variant.isRecommended -> GoogleTvGreen.copy(alpha = 0.6f)
                                else -> GoogleTvCardBorder
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = when {
                                            isDefault -> GoogleTvBlue
                                            variant.isRecommended -> GoogleTvGreen
                                            else -> GoogleTvCardBorder
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isDefault -> "ACTIVE DEFAULT"
                                                variant.isRecommended -> "iQOO MATCH"
                                                else -> "${variant.frequencyHz / 1000} kHz"
                                            },
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "#${variant.index} ${variant.hexCode}",
                                        color = GoogleTvTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isDefault) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoogleTvBlue, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Selected", color = GoogleTvBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                text = variant.name,
                                color = GoogleTvTextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = "${variant.protocol} • ${variant.description}",
                                color = GoogleTvTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )

                            // Action buttons: TEST ON + SET AS DEFAULT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isDefault) {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            controller.setSelectedVariantIndex(variant.index)
                                            selectedIndex = variant.index
                                            Toast.makeText(context, "Set #${variant.index} (${variant.hexCode}) as default Power code!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvBlue.copy(alpha = 0.6f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = GoogleTvBlue, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SET DEFAULT", color = GoogleTvBlue, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        testedIndex = variant.index
                                        coroutineScope.launch {
                                            controller.sendPowerOnSignal(variant.index)
                                            Toast.makeText(context, "IR Fired: #${variant.index} (${variant.hexCode} @ ${variant.frequencyHz / 1000}kHz)", Toast.LENGTH_SHORT).show()
                                            delay(1500)
                                            testedIndex = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isTestingThis) Color(0xFFE53935) else GoogleTvRed
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isTestingThis) "FIRING..." else "TEST ON", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isAutoTesting = false
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoogleTvBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close Tester", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = GoogleTvSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
