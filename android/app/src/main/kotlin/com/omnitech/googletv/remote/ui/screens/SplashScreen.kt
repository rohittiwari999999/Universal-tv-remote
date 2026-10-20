package com.omnitech.googletv.remote.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitech.googletv.remote.R
import com.omnitech.googletv.remote.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    // Try loading bitmap from assets as ultra-reliable fallback
    val assetBitmap = remember {
        try {
            context.assets.open("splash_photo.jpg").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    // Auto-transition to main discovery after 3 seconds
    LaunchedEffect(Unit) {
        isVisible = true
        delay(3200)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashPulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF070B14),
                        Color(0xFF020617)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(900)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(900, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Top App Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(GoogleTvBlue.copy(alpha = 0.15f))
                        .border(1.dp, GoogleTvBlue.copy(alpha = 0.35f), RoundedCornerShape(30.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = GoogleTvBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GOOGLE TV REMOTE",
                        color = GoogleTvBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Photo Frame with Glow & Border
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(glowScale)
                        .size(280.dp)
                        .shadow(24.dp, shape = RoundedCornerShape(28.dp), ambientColor = GoogleTvBlue, spotColor = GoogleTvBlue)
                        .clip(RoundedCornerShape(28.dp))
                        .background(GoogleTvSurface)
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF60A5FA),
                                    Color(0xFF818CF8),
                                    Color(0xFF38BDF8)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    if (assetBitmap != null) {
                        Image(
                            bitmap = assetBitmap.asImageBitmap(),
                            contentDescription = "Shreyash Tripathi & Shrijay Tripathi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.splash_photo),
                            contentDescription = "Shreyash Tripathi & Shrijay Tripathi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Prominent Names requested by User
                Text(
                    text = "SHREYASH TRIPATHI",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "AND",
                    color = GoogleTvBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "SHRIJAY TRIPATHI",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Smart Google TV Remote Controller",
                    color = GoogleTvTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Skip / Continue Button
                Button(
                    onClick = onSplashFinished,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleTvBlue.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Enter Remote",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
