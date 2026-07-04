package com.bytethrux.loadr.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.DialerSip
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytethrux.loadr.ui.theme.LoadrGreen
import com.bytethrux.loadr.ui.theme.LoadrGreenDim
import com.bytethrux.loadr.ui.theme.LoadrNavy
import com.bytethrux.loadr.ui.theme.LoadrSlate
import com.bytethrux.loadr.ui.theme.LoadrWhite

/**
 * Placeholder destination for features that are on the roadmap but not
 * built yet. Keeps drawer navigation honest instead of falling back to Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(
    title: String,
    onBackClick: () -> Unit,
) {
    Scaffold(
        containerColor = LoadrNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoadrNavy),
                title = {
                    Text(title, color = LoadrWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = LoadrWhite)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(LoadrGreenDim)
                    .border(1.dp, LoadrGreen.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconFor(title),
                    contentDescription = null,
                    tint = LoadrGreen,
                    modifier = Modifier.size(38.dp)
                )
            }

            Column(
                modifier = Modifier.padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Coming soon",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoadrWhite
                )
                Text(
                    "$title is under construction. We're working hard to bring it to your agent toolkit.",
                    fontSize = 13.sp,
                    color = LoadrSlate,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(LoadrGreenDim)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    "ON THE ROADMAP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LoadrGreen,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
private fun iconFor(title: String): ImageVector = when (title) {
    "Quick Dial" -> Icons.Outlined.DialerSip
    "Auto Renewals" -> Icons.Outlined.Autorenew
    "Site Link" -> Icons.Outlined.Link
    "Subscriptions" -> Icons.Outlined.CardMembership
    "AutoReplies" -> Icons.AutoMirrored.Outlined.Reply
    "Community" -> Icons.Outlined.Group
    else -> Icons.Outlined.RocketLaunch
}
