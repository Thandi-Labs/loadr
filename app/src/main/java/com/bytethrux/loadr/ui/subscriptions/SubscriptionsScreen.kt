package com.bytethrux.loadr.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytethrux.loadr.data.network.SubscriptionPlanDto
import com.bytethrux.loadr.ui.theme.LoadrGreen
import com.bytethrux.loadr.ui.theme.LoadrGreenDim
import com.bytethrux.loadr.ui.theme.LoadrNavy
import com.bytethrux.loadr.ui.theme.LoadrNavyCard
import com.bytethrux.loadr.ui.theme.LoadrNavySurface
import com.bytethrux.loadr.ui.theme.LoadrOnGreen
import com.bytethrux.loadr.ui.theme.LoadrRed
import com.bytethrux.loadr.ui.theme.LoadrSlate
import com.bytethrux.loadr.ui.theme.LoadrWhite
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPromoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    // Tick the remaining-time label once a minute.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SubscriptionsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        containerColor = LoadrNavy,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoadrNavy),
                title = {
                    Text("Subscriptions", color = LoadrWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = LoadrWhite)
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(LoadrNavy)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val selectedPlan = uiState.plans.firstOrNull { it.id == uiState.selectedPlanId }
                Button(
                    onClick = { viewModel.activateSelectedPlan() },
                    enabled = selectedPlan != null && !uiState.isActivating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoadrGreen,
                        disabledContainerColor = LoadrNavySurface,
                        disabledContentColor = LoadrSlate,
                    )
                ) {
                    if (uiState.isActivating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = LoadrOnGreen,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (selectedPlan != null) "Activate — KES ${selectedPlan.price.toInt()}" else "Activate",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedPlan != null) LoadrOnGreen else LoadrSlate
                        )
                    }
                }
                TextButton(
                    onClick = { showPromoDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = LoadrSlate)) { append("Have a promo code? ") }
                            withStyle(SpanStyle(color = LoadrGreen, fontWeight = FontWeight.SemiBold)) {
                                append("Redeem here")
                            }
                        },
                        fontSize = 13.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Entitlement header ─────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(color = LoadrGreen, fontWeight = FontWeight.Bold)
                        ) { append(subscription.remainingLabel(now)) }
                        withStyle(SpanStyle(color = LoadrSlate)) { append(" Remaining") }
                    },
                    fontSize = 13.sp
                )
                Text(
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(color = LoadrGreen, fontWeight = FontWeight.Bold)
                        ) { append("${subscription.availableTokens(now)}") }
                        withStyle(SpanStyle(color = LoadrSlate)) { append(" Requests Left") }
                    },
                    fontSize = 13.sp
                )
            }

            Text(
                "BingwaHybrid Subscription",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LoadrWhite,
                modifier = Modifier.padding(top = 4.dp)
            )

            when {
                uiState.isLoadingPlans -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LoadrGreen)
                }

                uiState.plansError != null -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        uiState.plansError ?: "Could not load plans",
                        color = LoadrRed,
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("Retry", color = LoadrGreen, fontWeight = FontWeight.SemiBold)
                    }
                }

                else -> uiState.plans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        selected = plan.id == uiState.selectedPlanId,
                        onClick = { viewModel.selectPlan(plan.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showPromoDialog) {
        PromoCodeDialog(
            onDismiss = { showPromoDialog = false },
            onRedeem = { code ->
                viewModel.redeemPromoCode(code)
                showPromoDialog = false
            }
        )
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlanDto,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) LoadrGreenDim else LoadrNavyCard)
            .border(
                width = 1.dp,
                color = if (selected) LoadrGreen else LoadrNavySurface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(plan.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LoadrGreen)
            Text("KES ${plan.price.toInt()} /-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LoadrWhite)
            Text(plan.description, fontSize = 12.sp, color = LoadrSlate)
        }
        if (selected) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = "Selected",
                tint = LoadrGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PromoCodeDialog(
    onDismiss: () -> Unit,
    onRedeem: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LoadrNavyCard,
        title = { Text("Redeem promo code", color = LoadrWhite) },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Promo code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LoadrWhite,
                    unfocusedTextColor = LoadrWhite,
                    focusedBorderColor = LoadrGreen,
                    unfocusedBorderColor = LoadrNavySurface,
                    focusedLabelColor = LoadrGreen,
                    unfocusedLabelColor = LoadrSlate,
                    cursorColor = LoadrGreen,
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onRedeem(code.trim()) },
                enabled = code.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LoadrGreen)
            ) {
                Text("Redeem", color = LoadrOnGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = LoadrSlate) }
        }
    )
}
