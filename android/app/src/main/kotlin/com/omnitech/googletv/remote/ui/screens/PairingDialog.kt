package com.omnitech.googletv.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import com.omnitech.googletv.remote.model.PairingState
import com.omnitech.googletv.remote.ui.theme.*

@Composable
fun PairingDialog(
    device: DiscoveredGoogleTv,
    pairingState: PairingState,
    onSubmitPin: (String) -> Unit,
    onRetryPairing: () -> Unit = {},
    onConnectDirectly: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    val isVerifying = pairingState is PairingState.VerifyingPin
    val isConnecting = pairingState is PairingState.Connecting
    val errorMessage = (pairingState as? PairingState.Error)?.message

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, GoogleTvCardBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = GoogleTvSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = GoogleTvBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pair with ${device.name}",
                            color = GoogleTvTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GoogleTvTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Simulated TV Screen Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GoogleTvDark)
                        .border(1.dp, GoogleTvBlue.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = GoogleTvBlue,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isConnecting) "Sending Pairing Request to TV..." else "Check TV Screen",
                            color = GoogleTvTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isConnecting)
                                "Connecting to ${device.hostAddress}:6467..."
                            else
                                "Google TV displays a 6-character code (e.g. 7D8A2C)",
                            color = GoogleTvTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isConnecting) {
                    CircularProgressIndicator(
                        color = GoogleTvBlue,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Negotiating Google Polo Handshake...",
                        color = GoogleTvTextSecondary,
                        fontSize = 13.sp
                    )
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = GoogleTvRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Helpful Tips Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GoogleTvDark.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Troubleshooting:",
                                color = GoogleTvTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("1. Ensure TV is ON and not on screensaver.", color = GoogleTvTextSecondary, fontSize = 11.sp)
                            Text("2. If previously paired, try Direct Connect.", color = GoogleTvTextSecondary, fontSize = 11.sp)
                            Text("3. Restart TV once if connection is busy.", color = GoogleTvTextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRetryPairing,
                            colors = ButtonDefaults.buttonColors(containerColor = GoogleTvBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onConnectDirectly,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoogleTvGreen),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GoogleTvGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Connect Direct", color = GoogleTvGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Text(
                        text = "Enter 6-Character Code from TV",
                        color = GoogleTvTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isLetterOrDigit() }.take(6).uppercase()
                            pinText = filtered
                            if (filtered.length == 6) {
                                onSubmitPin(filtered)
                            }
                        },
                        placeholder = {
                            Text(
                                "e.g. 7D8A2C",
                                color = GoogleTvTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            color = GoogleTvTextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoogleTvBlue,
                            unfocusedBorderColor = GoogleTvCardBorder,
                            focusedContainerColor = GoogleTvDark,
                            unfocusedContainerColor = GoogleTvDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { onSubmitPin(pinText) },
                        enabled = pinText.length == 6 && !isVerifying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoogleTvBlue,
                            disabledContainerColor = GoogleTvCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying SHA-256...", color = Color.White, fontSize = 14.sp)
                        } else {
                            Text(
                                text = "Pair & Connect",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
