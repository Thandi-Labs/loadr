package com.bytethrux.loadr

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytethrux.loadr.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "dotAlpha"
    )

    // Capture theme colours for use inside Canvas draw scopes.
    val greenColor = LoadrGreen
    val navyColor = LoadrNavy

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoadrNavy),
        contentAlignment = Alignment.Center
    ) {
        // Animated dot grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 32.dp.toPx()
            val dotRadius = 2.dp.toPx()
            var x = spacing / 2
            while (x < size.width) {
                var y = spacing / 2
                while (y < size.height) {
                    drawCircle(
                        color = greenColor.copy(alpha = dotAlpha),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += spacing
                }
                x += spacing
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo mark
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(LoadrGreen),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(40.dp)) {
                    val cx = size.width / 2
                    drawLine(
                        color = navyColor,
                        start = Offset(4.dp.toPx(), size.height),
                        end = Offset(cx, 6.dp.toPx()),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = navyColor,
                        start = Offset(cx, 6.dp.toPx()),
                        end = Offset(size.width - 4.dp.toPx(), size.height),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = navyColor,
                        start = Offset(12.dp.toPx(), 26.dp.toPx()),
                        end = Offset(size.width - 12.dp.toPx(), 26.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brand name
            Text(
                text = buildAnnotatedString {
                    append("Load")
                    withStyle(SpanStyle(color = LoadrGreen)) { append("r") }
                },
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = LoadrWhite,
                letterSpacing = (-1).sp
            )

            Text(
                text = "DATA. DELIVERED.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = LoadrSlate,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Stats bar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(LoadrNavyCard)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatItem("2.4M", "MBs sold")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(LoadrNavySurface))
                StatItem("18K", "Agents")
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(LoadrNavySurface))
                StatItem("99.2%", "Uptime")
            }
        }

        // Loader dots
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i == 0) LoadrGreen else LoadrNavySurface)
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LoadrGreen)
        Text(label.uppercase(), fontSize = 9.sp, color = LoadrSlate, letterSpacing = 1.sp)
    }
}