package com.bytethrux.loadr.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DialerSip
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytethrux.loadr.data.network.HomeStatsDto
import com.bytethrux.loadr.data.network.TransactionDto
import com.bytethrux.loadr.ui.theme.LoadrColorScheme
import com.bytethrux.loadr.ui.theme.LoadrGreen
import com.bytethrux.loadr.ui.theme.LoadrNavy
import com.bytethrux.loadr.ui.theme.LoadrNavyCard
import com.bytethrux.loadr.ui.theme.LoadrNavySurface
import com.bytethrux.loadr.ui.theme.LoadrOnGreen
import com.bytethrux.loadr.ui.theme.LoadrRed
import com.bytethrux.loadr.ui.theme.LoadrSlate
import com.bytethrux.loadr.ui.theme.LoadrWhite
import com.bytethrux.loadr.ui.theme.LocalLoadrColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    username: String,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenTransactions: (com.bytethrux.loadr.data.transactions.StatusFilter) -> Unit = {},
    onOpenSubscriptions: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAttentionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.simsNeedAttention) {
        if (uiState.simsNeedAttention) {
            showAttentionSheet = true
        }
    }

    if (showAttentionSheet) {
        SimAttentionBottomSheet(
            onDismiss = { showAttentionSheet = false },
            onReconfigure = {
                showAttentionSheet = false
                onNavigate("Settings")
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LoadrDrawer(
                username = username,
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                },
                onNavigate = { screen ->
                    scope.launch { drawerState.close() }
                    onNavigate(screen)
                }
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Scaffold(
            containerColor = LoadrNavy,
            topBar = {
                HomeTopBar(
                    username = username,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LoadrGreen)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.simsNeedAttention) {
                        item {
                            SimAttentionBanner(onClick = { onNavigate("Settings") })
                        }
                    }
                    item {
                        uiState.stats?.let { stats ->
                            StatCardsRow(
                                stats = stats,
                                onSuccessfulClick = {
                                    onOpenTransactions(com.bytethrux.loadr.data.transactions.StatusFilter.SUCCESSFUL)
                                },
                                onFailedClick = {
                                    onOpenTransactions(com.bytethrux.loadr.data.transactions.StatusFilter.FAILED)
                                },
                                onTokensClick = onOpenSubscriptions,
                            )
                        }
                    }
                    item {
                        uiState.stats?.let { stats ->
                            AirtimeRow(
                                stats = stats,
                                isRefreshing = uiState.isAirtimeRefreshing,
                                hidden = uiState.hideAirtime,
                                onRefreshBalance = { viewModel.refreshAirtimeBalance() },
                                onToggleHidden = { viewModel.toggleHideAirtime() },
                            )
                        }
                    }
                    item {
                        uiState.stats?.let { stats ->
                            CommissionChart(stats)
                        }
                    }
                    item {
                        TransactionHeader(
                            onSeeAll = {
                                onOpenTransactions(com.bytethrux.loadr.data.transactions.StatusFilter.ALL)
                            }
                        )
                    }
                    items(uiState.transactions.take(5)) { tx ->
                        TransactionItem(tx)
                    }
                }
            }
        }
    }
}

// ── SIM ATTENTION ─────────────────────────────────────────

@Composable
private fun SimAttentionBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LoadrRed.copy(alpha = 0.1f))
            .border(1.dp, LoadrRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Outlined.Warning,
            contentDescription = null,
            tint = LoadrRed,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "SIM Change Detected",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LoadrWhite
            )
            Text(
                "Automation paused. Please reconfigure your SIM slots.",
                fontSize = 12.sp,
                color = LoadrSlate
            )
        }
        Text(
            "FIX →",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = LoadrRed
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimAttentionBottomSheet(
    onDismiss: () -> Unit,
    onReconfigure: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { /* Non-dismissable */ },
        containerColor = LoadrNavyCard,
        contentColor = LoadrWhite,
        dragHandle = null, // Remove drag handle to discourage dismissal
        scrimColor = Color.Black.copy(alpha = 0.8f) // Darker scrim
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp, top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(LoadrRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = LoadrRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                "Action Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = LoadrWhite
            )

            Text(
                "A SIM card change was detected. Automation is paused to prevent errors. You must verify your SIM configuration before continuing.",
                style = MaterialTheme.typography.bodyMedium,
                color = LoadrSlate,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )

            Button(
                onClick = onReconfigure,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LoadrGreen, contentColor = LoadrOnGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Configure SIM Slots", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ── TOP BAR ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(username: String, onMenuClick: () -> Unit) {
    val initials = username.take(2).uppercase()
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning,"
        hour < 17 -> "Good afternoon,"
        else -> "Good evening,"
    }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = LoadrNavy),
        title = {
            Column {
                Text(greeting, fontSize = 11.sp, color = LoadrSlate)
                Text(
                    username.replaceFirstChar { it.uppercase() },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoadrWhite
                )
            }
        },
        actions = {
            IconButton(onClick = { /* notifications */ }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = LoadrSlate
                )
            }
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(LoadrGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LoadrOnGreen)
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Menu,
                    contentDescription = "Open menu",
                    tint = LoadrSlate
                )
            }
        }
    )
}

// ── STAT CARDS ────────────────────────────────────────────

@Composable
private fun StatCardsRow(
    stats: HomeStatsDto,
    onSuccessfulClick: () -> Unit,
    onFailedClick: () -> Unit,
    onTokensClick: () -> Unit,
) {
    val colors = LocalLoadrColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            value = stats.successful_today.toString(),
            label = "Successful",
            valueColor = LoadrGreen,
            borderColor = LoadrGreen,
            bgColor = colors.successBg,
            onClick = onSuccessfulClick,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = stats.failed_today.toString(),
            label = "Failed",
            valueColor = LoadrRed,
            borderColor = LoadrRed,
            bgColor = colors.errorBg,
            onClick = onFailedClick,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = stats.token_balance.toString(),
            label = "Tokens",
            valueColor = colors.infoText,
            borderColor = colors.infoBorder,
            bgColor = colors.infoBg,
            onClick = onTokensClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    valueColor: Color,
    borderColor: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(
            label.uppercase(),
            fontSize = 9.sp,
            color = LoadrSlate,
            letterSpacing = 0.6.sp
        )
    }
}

// ── AIRTIME ROW ───────────────────────────────────────────

@Composable
private fun AirtimeRow(
    stats: HomeStatsDto,
    isRefreshing: Boolean,
    hidden: Boolean,
    onRefreshBalance: () -> Unit,
    onToggleHidden: () -> Unit,
) {
    fun mask(amount: Double) = if (hidden) "Ksh ••••••" else "Ksh %.2f".format(amount)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AirtimeCard(
            label = "Airtime used today",
            value = mask(stats.airtime_used),
            modifier = Modifier.weight(1f),
            trailing = {
                IconButton(onClick = onToggleHidden, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (hidden)
                            androidx.compose.material.icons.Icons.Outlined.VisibilityOff
                        else
                            androidx.compose.material.icons.Icons.Outlined.Visibility,
                        contentDescription = if (hidden) "Show airtime figures" else "Hide airtime figures",
                        tint = LoadrSlate,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
        AirtimeCard(
            label = "Airtime balance",
            value = mask(stats.airtime_balance),
            modifier = Modifier.weight(1f),
            trailing = {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        color = LoadrGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    IconButton(onClick = onRefreshBalance, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.Refresh,
                            contentDescription = "Refresh airtime balance",
                            tint = LoadrGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun AirtimeCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LoadrNavyCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label.uppercase(), fontSize = 9.sp, color = LoadrSlate, letterSpacing = 0.4.sp)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LoadrWhite)
        }
        trailing?.invoke()
    }
}

// ── COMMISSION CHART ──────────────────────────────────────

@Composable
private fun CommissionChart(stats: HomeStatsDto) {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val values = stats.commission_by_day
    val maxVal = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0

    // Animate the chart drawing in
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(1000, easing = EaseInOutCubic))
    }
    val progress by animProgress.asState()
    val chartColors = LocalLoadrColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LoadrNavyCard)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "This week's commission",
                fontSize = 11.sp,
                color = LoadrSlate,
                letterSpacing = 0.3.sp
            )
            Text(
                "Ksh %.2f".format(stats.weekly_commission),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = LoadrGreen
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            drawCommissionChart(values, maxVal, days, progress, chartColors)
        }
    }
}

private fun DrawScope.drawCommissionChart(
    values: List<Double>,
    maxVal: Double,
    days: List<String>,
    progress: Float,
    colors: LoadrColorScheme
) {
    if (values.size < 2) return
    val w = size.width
    val h = size.height - 20.dp.toPx()  // reserve bottom for labels
    val stepX = w / (values.size - 1).toFloat()

    fun xOf(i: Int) = i * stepX
    fun yOf(v: Double) = h - (v / maxVal * h).toFloat()

    // Build points up to progress
    val points = values.mapIndexed { i, v -> Offset(xOf(i), yOf(v)) }
    val cutIndex = (progress * (points.size - 1)).toInt().coerceAtMost(points.size - 2)
    val partialFraction = (progress * (points.size - 1)) - cutIndex

    // Draw filled area
    val path = Path()
    path.moveTo(points[0].x, h)
    path.lineTo(points[0].x, points[0].y)
    for (i in 0 until cutIndex) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val cx = (p1.x + p2.x) / 2
        path.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
    }
    if (cutIndex < points.size - 1) {
        val p1 = points[cutIndex]
        val p2 = points[cutIndex + 1]
        val cx = (p1.x + p2.x) / 2
        val endX = p1.x + (p2.x - p1.x) * partialFraction
        val endY = p1.y + (p2.y - p1.y) * partialFraction.toFloat()
        path.cubicTo(cx, p1.y, cx, p2.y, endX, endY)
        path.lineTo(endX, h)
    } else {
        path.lineTo(points[cutIndex].x, h)
    }
    path.close()

    drawPath(
        path,
        brush = Brush.verticalGradient(
            colors = listOf(colors.green.copy(alpha = 0.3f), Color.Transparent),
            startY = 0f, endY = h
        )
    )

    // Draw line
    val linePath = Path()
    linePath.moveTo(points[0].x, points[0].y)
    for (i in 0 until cutIndex) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val cx = (p1.x + p2.x) / 2
        linePath.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
    }
    drawPath(linePath, colors.green, style = androidx.compose.ui.graphics.drawscope.Stroke(
        width = 2.dp.toPx(),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    ))

    // Draw dots and day labels
    val labelPaint = android.graphics.Paint().apply {
        textSize = 10.sp.toPx()
        color = colors.slate.toArgb()
        textAlign = android.graphics.Paint.Align.CENTER
    }
    values.forEachIndexed { i, _ ->
        val p = points[i]
        drawCircle(colors.slate, radius = 3.dp.toPx(), center = p)
        drawCircle(colors.chartDot, radius = 1.5.dp.toPx(), center = p)
        drawContext.canvas.nativeCanvas.drawText(
            days[i], p.x, size.height, labelPaint
        )
    }
    // Highlight last point
    if (progress >= 1f) {
        drawCircle(colors.red, radius = 4.dp.toPx(), center = points.last())
        drawCircle(colors.chartDot, radius = 2.dp.toPx(), center = points.last())
    }
}

// ── TRANSACTIONS ──────────────────────────────────────────

@Composable
private fun TransactionHeader(onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Recent transactions", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LoadrWhite)
        TextButton(onClick = onSeeAll, contentPadding = PaddingValues(0.dp)) {
            Text("All →", fontSize = 12.sp, color = LoadrGreen)
        }
    }
}

@Composable
private fun TransactionItem(tx: TransactionDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(LoadrNavyCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status icon
        val isSuccess = tx.status == "success"
        val colors = LocalLoadrColors.current
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSuccess) colors.successBg else colors.errorBg)
                .border(1.dp, if (isSuccess) LoadrGreen else LoadrRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSuccess)
                    androidx.compose.material.icons.Icons.Outlined.Check
                else
                    androidx.compose.material.icons.Icons.Outlined.Close,
                contentDescription = null,
                tint = if (isSuccess) LoadrGreen else LoadrRed,
                modifier = Modifier.size(14.dp)
            )
        }

        // Info
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(tx.customer_name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LoadrWhite)
            Text(tx.package_name ?: "Bundle", fontSize = 11.sp, color = LoadrGreen)
        }

        // Amount + time
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(tx.created_at, fontSize = 10.sp, color = LoadrSlate)
            Text("Ksh ${tx.amount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LoadrWhite)
        }
    }
}

// ── DRAWER ────────────────────────────────────────────────

@Composable
private fun LoadrDrawer(
    username: String,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        "Home" to androidx.compose.material.icons.Icons.Outlined.Home,
        "Offers" to androidx.compose.material.icons.Icons.Outlined.Storefront,
        "Quick Dial" to androidx.compose.material.icons.Icons.Outlined.DialerSip,
        "Auto Renewals" to androidx.compose.material.icons.Icons.Outlined.Autorenew,
        "Site Link" to androidx.compose.material.icons.Icons.Outlined.Link,
        "Subscriptions" to androidx.compose.material.icons.Icons.Outlined.CardMembership,
        "AutoReplies" to androidx.compose.material.icons.Icons.Outlined.Reply,
        "Community" to androidx.compose.material.icons.Icons.Outlined.Group,
        "Settings" to androidx.compose.material.icons.Icons.Outlined.Settings,
    )

    var activeItem by remember { mutableStateOf("Home") }
    val colors = LocalLoadrColors.current

    ModalDrawerSheet(
        drawerContainerColor = colors.drawerBg,
        drawerContentColor = LoadrWhite,
        modifier = Modifier.width(280.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(LoadrGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(username.take(2).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LoadrOnGreen)
                }
                Column {
                    Text(username.replaceFirstChar { it.uppercase() }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LoadrWhite)
                    Text("Agent account", fontSize = 12.sp, color = LoadrSlate)
                }
            }
        }

        HorizontalDivider(color = LoadrNavySurface, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Nav items
        items.forEach { (label, icon) ->
            val isActive = label == activeItem
            NavigationDrawerItem(
                label = { Text(label, fontSize = 14.sp) },
                selected = isActive,
                onClick = {
                    activeItem = label
                    onNavigate(label)
                },
                icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = colors.greenDim,
                    unselectedContainerColor = Color.Transparent,
                    selectedTextColor = LoadrGreen,
                    unselectedTextColor = colors.navText,
                    selectedIconColor = LoadrGreen,
                    unselectedIconColor = LoadrSlate
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp),
                shape = RoundedCornerShape(10.dp),
                badge = if (isActive) ({ Box(Modifier.size(6.dp).clip(CircleShape).background(LoadrGreen)) }) else null
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(color = LoadrNavySurface, thickness = 0.5.dp)

        // Logout
        NavigationDrawerItem(
            label = { Text("Logout", fontSize = 14.sp) },
            selected = false,
            onClick = onLogout,
            icon = { Icon(androidx.compose.material.icons.Icons.Outlined.Logout, contentDescription = "Logout", modifier = Modifier.size(20.dp)) },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                unselectedTextColor = LoadrRed,
                unselectedIconColor = LoadrRed
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}