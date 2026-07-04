package com.bytethrux.loadr.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytethrux.loadr.data.local.ProcessingMode
import com.bytethrux.loadr.data.sim.SimInfo
import com.bytethrux.loadr.ui.theme.LoadrGreen
import com.bytethrux.loadr.ui.theme.LoadrNavy
import com.bytethrux.loadr.ui.theme.LoadrNavyCard
import com.bytethrux.loadr.ui.theme.LoadrNavySurface
import com.bytethrux.loadr.ui.theme.LoadrOnGreen
import com.bytethrux.loadr.ui.theme.LoadrRed
import com.bytethrux.loadr.ui.theme.LoadrSlate
import com.bytethrux.loadr.ui.theme.LoadrWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    username: String,
    onBackClick: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showEditName by remember { mutableStateOf(false) }
    var showAuthorizedSenders by remember { mutableStateOf(false) }
    var showSuffixDialog by remember { mutableStateOf(false) }
    var showBlacklist by remember { mutableStateOf(false) }

    val displayName = settings.displayName.ifBlank {
        username.replaceFirstChar { it.uppercase() }
    }

    // At least two selectable slots even when SIM info is unavailable.
    val sims = viewModel.availableSims.ifEmpty {
        listOf(SimInfo(0, -1, "SIM 1"), SimInfo(1, -1, "SIM 2"))
    }

    Scaffold(
        containerColor = LoadrNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoadrNavy),
                title = {
                    Text("Settings", color = LoadrWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Profile header ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(LoadrGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayName.take(2).uppercase(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = LoadrOnGreen
                    )
                }
                Text(
                    displayName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoadrWhite,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showEditName = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit name", tint = LoadrSlate)
                }
            }
            HorizontalDivider(color = LoadrNavySurface, thickness = 0.5.dp)

            // ── Message Processing ────────────────────────
            SectionHeader("Message Processing")
            SettingsCard {
                ToggleRow("Process M-Pesa Messages", checked = settings.processMpesa, onChange = viewModel::setProcessMpesa)
                RowDivider()
                ToggleRow("Process Till Messages", checked = settings.processTill, onChange = viewModel::setProcessTill)
                RowDivider()
                ToggleRow("Process PayBill Messages", checked = settings.processPayBill, onChange = viewModel::setProcessPayBill)
                RowDivider()
                ToggleRow("Process SiteLink Messages", checked = settings.processSiteLink, onChange = viewModel::setProcessSiteLink)
                RowDivider()
                NavRow(
                    title = "Authorized Senders",
                    subtitle = "Messages from these senders will be processed too",
                    onClick = { showAuthorizedSenders = true }
                )
            }

            // ── My Customers ──────────────────────────────
            SectionHeader("My Customers")
            SettingsCard {
                ToggleRow(
                    title = "Auto-Save Contacts",
                    subtitle = "Tap to configure the name suffix",
                    checked = settings.autoSaveContacts,
                    onChange = viewModel::setAutoSaveContacts,
                    onRowClick = { showSuffixDialog = true }
                )
                RowDivider()
                NavRow(
                    title = "BlackList",
                    subtitle = "Blacklisted customers will not be recommended offers",
                    onClick = { showBlacklist = true }
                )
            }

            // ── SIM Setup ─────────────────────────────────
            SectionHeader("SIM Setup")
            SettingsCard {
                SimGroup(
                    title = "SIM to receive payments",
                    sims = sims,
                    selectedSlot = settings.paymentSimSlot,
                    onSelect = viewModel::setPaymentSimSlot
                )
                RowDivider()
                SimGroup(
                    title = "Bingwa SIM (To run USSDs)",
                    sims = sims,
                    selectedSlot = settings.bingwaSimSlot,
                    onSelect = viewModel::setBingwaSimSlot
                )
                RowDivider()
                SimGroup(
                    title = "Send Auto-Replies Using",
                    sims = sims,
                    selectedSlot = settings.autoReplySimSlot,
                    onSelect = viewModel::setAutoReplySimSlot
                )
            }

            // ── Hybrid Portal ─────────────────────────────
            SectionHeader("Hybrid Portal")
            SettingsCard {
                ToggleRow(
                    title = "Hybrid Portal",
                    subtitle = "View and manage transactions remotely",
                    checked = settings.hybridPortal,
                    onChange = viewModel::setHybridPortal
                )
            }

            // ── Customer Tools ────────────────────────────
            SectionHeader("Customer Tools")
            SettingsCard {
                ToggleRow(
                    title = "EngageBot",
                    subtitle = "Engage your customers on already recommended offers",
                    checked = settings.engageBot,
                    onChange = viewModel::setEngageBot
                )
            }

            // ── Processing Mode ───────────────────────────
            SectionHeader("Processing Mode")
            SettingsCard {
                RadioRow(
                    title = "Express Mode",
                    subtitle = "Processes USSDs that can be completed in one request",
                    selected = settings.processingMode == ProcessingMode.EXPRESS,
                    onSelect = { viewModel.setProcessingMode(ProcessingMode.EXPRESS) }
                )
                RowDivider()
                RadioRow(
                    title = "Advanced Mode",
                    subtitle = "Processes USSDs that require multiple steps to complete",
                    selected = settings.processingMode == ProcessingMode.ADVANCED,
                    onSelect = { viewModel.setProcessingMode(ProcessingMode.ADVANCED) }
                )
            }

            // ── Appearance ────────────────────────────────
            SectionHeader("Appearance")
            SettingsCard {
                RadioRow(
                    title = "Use phone setting",
                    subtitle = "Follow the system light/dark preference",
                    selected = settings.themeMode == "system",
                    onSelect = { viewModel.setThemeMode("system") }
                )
                RowDivider()
                RadioRow(
                    title = "Light",
                    subtitle = "Always use the light theme",
                    selected = settings.themeMode == "light",
                    onSelect = { viewModel.setThemeMode("light") }
                )
                RowDivider()
                RadioRow(
                    title = "Dark",
                    subtitle = "Always use the dark theme",
                    selected = settings.themeMode == "dark",
                    onSelect = { viewModel.setThemeMode("dark") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Dialogs ───────────────────────────────────────────
    if (showEditName) {
        TextInputDialog(
            title = "Display name",
            label = "Name",
            initialValue = displayName,
            onDismiss = { showEditName = false },
            onConfirm = {
                viewModel.setDisplayName(it)
                showEditName = false
            }
        )
    }

    if (showSuffixDialog) {
        TextInputDialog(
            title = "Contact name suffix",
            label = "Suffix",
            initialValue = settings.contactNameSuffix,
            supportingText = "Saved contacts appear as \"CUSTOMER NAME ${settings.contactNameSuffix}\"",
            onDismiss = { showSuffixDialog = false },
            onConfirm = {
                viewModel.setContactNameSuffix(it)
                showSuffixDialog = false
            }
        )
    }

    if (showAuthorizedSenders) {
        ListManagerDialog(
            title = "Authorized Senders",
            emptyHint = "No extra senders. Messages from M-PESA are always considered.",
            inputLabel = "Sender ID (e.g. SAF-BINGWA)",
            items = settings.authorizedSenders,
            onAdd = viewModel::addAuthorizedSender,
            onRemove = viewModel::removeAuthorizedSender,
            onDismiss = { showAuthorizedSenders = false }
        )
    }

    if (showBlacklist) {
        ListManagerDialog(
            title = "BlackList",
            emptyHint = "No blacklisted customers.",
            inputLabel = "Phone number (07XXXXXXXX)",
            items = settings.blacklist,
            onAdd = viewModel::addToBlacklist,
            onRemove = viewModel::removeFromBlacklist,
            onDismiss = { showBlacklist = false }
        )
    }
}

// ── BUILDING BLOCKS ───────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = LoadrGreen,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LoadrNavyCard)
    ) {
        content()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = LoadrNavySurface.copy(alpha = 0.5f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
    onRowClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onRowClick != null) Modifier.clickable(onClick = onRowClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LoadrWhite)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = LoadrSlate)
            }
        }
        LoadrSwitch(checked = checked, onChange = onChange)
    }
}

@Composable
private fun LoadrSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = LoadrOnGreen,
            checkedTrackColor = LoadrGreen,
            uncheckedThumbColor = LoadrSlate,
            uncheckedTrackColor = LoadrNavySurface,
            uncheckedBorderColor = LoadrSlate,
        )
    )
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LoadrWhite)
            Text(subtitle, fontSize = 11.sp, color = LoadrSlate)
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = LoadrSlate
        )
    }
}

@Composable
private fun SimGroup(
    title: String,
    sims: List<SimInfo>,
    selectedSlot: Int,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = LoadrWhite,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        sims.forEach { sim ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(sim.slotIndex) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Outlined.SimCard,
                    contentDescription = null,
                    tint = if (sim.slotIndex == selectedSlot) LoadrGreen else LoadrSlate,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("SIM ${sim.slotIndex + 1}", fontSize = 13.sp, color = LoadrWhite)
                    if (sim.carrierName.isNotBlank() && sim.carrierName != "SIM ${sim.slotIndex + 1}") {
                        Text(sim.carrierName, fontSize = 10.sp, color = LoadrSlate)
                    }
                }
                LoadrSwitch(
                    checked = sim.slotIndex == selectedSlot,
                    onChange = { if (it) onSelect(sim.slotIndex) }
                )
            }
        }
    }
}

@Composable
private fun RadioRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = LoadrGreen,
                unselectedColor = LoadrSlate
            )
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LoadrWhite)
            Text(subtitle, fontSize = 11.sp, color = LoadrSlate)
        }
    }
}

// ── DIALOGS ───────────────────────────────────────────────

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    supportingText: String? = null,
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LoadrNavyCard,
        title = { Text(title, color = LoadrWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = settingsFieldColors()
                )
                if (supportingText != null) {
                    Text(supportingText, fontSize = 11.sp, color = LoadrSlate)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LoadrGreen)
            ) {
                Text("Save", color = LoadrOnGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = LoadrSlate) }
        }
    )
}

@Composable
private fun ListManagerDialog(
    title: String,
    emptyHint: String,
    inputLabel: String,
    items: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newItem by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LoadrNavyCard,
        title = { Text(title, color = LoadrWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (items.isEmpty()) {
                    Text(emptyHint, fontSize = 12.sp, color = LoadrSlate)
                } else {
                    items.sorted().forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LoadrNavySurface.copy(alpha = 0.4f))
                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item, fontSize = 13.sp, color = LoadrWhite, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemove(item) }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Remove $item",
                                    tint = LoadrRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newItem,
                        onValueChange = { newItem = it },
                        label = { Text(inputLabel, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = settingsFieldColors()
                    )
                    Button(
                        onClick = {
                            onAdd(newItem)
                            newItem = ""
                        },
                        enabled = newItem.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = LoadrGreen),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
                    ) {
                        Text("Add", color = LoadrOnGreen)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = LoadrGreen) }
        }
    )
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LoadrWhite,
    unfocusedTextColor = LoadrWhite,
    focusedBorderColor = LoadrGreen,
    unfocusedBorderColor = LoadrNavySurface,
    focusedLabelColor = LoadrGreen,
    unfocusedLabelColor = LoadrSlate,
    cursorColor = LoadrGreen,
)
