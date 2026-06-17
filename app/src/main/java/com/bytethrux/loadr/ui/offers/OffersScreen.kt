package com.bytethrux.loadr.ui.offers

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytethrux.loadr.data.network.OfferDto
import com.bytethrux.loadr.data.network.OfferType
import com.bytethrux.loadr.ui.theme.LoadrGreen
import com.bytethrux.loadr.ui.theme.LoadrNavy
import com.bytethrux.loadr.ui.theme.LoadrNavyCard
import com.bytethrux.loadr.ui.theme.LoadrNavySurface
import com.bytethrux.loadr.ui.theme.LoadrRed
import com.bytethrux.loadr.ui.theme.LoadrSlate
import com.bytethrux.loadr.ui.theme.LoadrWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    viewModel: OffersViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var offerToEdit by remember { mutableStateOf<OfferDto?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Data", "Minutes", "SMS")
    val currentType = when (selectedTabIndex) {
        0 -> OfferType.DATA
        1 -> OfferType.MINUTES
        else -> OfferType.SMS
    }

    Scaffold(
        containerColor = LoadrNavy,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LoadrNavy),
                    title = { Text("Manage Offers", color = LoadrWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = LoadrWhite)
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = LoadrNavy,
                    contentColor = LoadrGreen,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = LoadrGreen
                        )
                    },
                    divider = { HorizontalDivider(color = LoadrNavySurface) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    title, 
                                    color = if (selectedTabIndex == index) LoadrGreen else LoadrSlate,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = LoadrGreen,
                contentColor = LoadrNavy,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Create Offer")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LoadrGreen)
            }
        } else {
            val filteredOffers = uiState.offers.filter { it.category == currentType }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredOffers.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No ${tabs[selectedTabIndex]} offers found", color = LoadrSlate)
                        }
                    }
                }
                items(filteredOffers) { offer ->
                    OfferItem(
                        offer = offer,
                        onToggleStatus = { viewModel.toggleOfferStatus(offer) },
                        onEdit = { offerToEdit = offer },
                        onDelete = { viewModel.deleteOffer(offer.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        OfferDialog(
            initialType = currentType,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc, price, type, ussd ->
                viewModel.createOffer(name, desc, price, type, ussd)
                showCreateDialog = false
            }
        )
    }

    offerToEdit?.let { offer ->
        OfferDialog(
            offer = offer,
            onDismiss = { offerToEdit = null },
            onConfirm = { name, desc, price, type, ussd ->
                viewModel.updateOffer(offer.copy(offer_name = name, amount = price, category = type, ussd = ussd))
                offerToEdit = null
            }
        )
    }
}

@Composable
private fun OfferItem(
    offer: OfferDto,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LoadrNavyCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(offer.offer_name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (offer.active) LoadrWhite else LoadrSlate)
            Text(offer.category.name, fontSize = 12.sp, color = LoadrSlate)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ksh ${offer.amount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LoadrGreen)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(LoadrNavySurface)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(offer.ussd, fontSize = 10.sp, color = LoadrSlate)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleStatus) {
                Icon(
                    imageVector = if (offer.active) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = "Toggle status",
                    tint = if (offer.active) LoadrGreen else LoadrSlate
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = LoadrWhite)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = LoadrRed)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferDialog(
    offer: OfferDto? = null,
    initialType: OfferType = OfferType.DATA,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, OfferType, String) -> Unit
) {
    var name by remember { mutableStateOf(offer?.offer_name ?: "") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf(offer?.amount?.toString() ?: "") }
    var ussd by remember { mutableStateOf(offer?.ussd ?: "") }
    var type by remember { mutableStateOf(offer?.category ?: initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LoadrNavyCard,
        title = { Text(if (offer == null) "Create Offer" else "Edit Offer", color = LoadrWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OfferType.entries.forEach { offerType ->
                        val isSelected = type == offerType
                        FilterChip(
                            selected = isSelected,
                            onClick = { type = offerType },
                            label = { Text(offerType.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = LoadrSlate,
                                selectedLabelColor = LoadrNavy,
                                selectedContainerColor = LoadrGreen,
                                containerColor = LoadrNavySurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = LoadrNavySurface,
                                selectedBorderColor = LoadrGreen,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LoadrWhite,
                        unfocusedTextColor = LoadrWhite,
                        focusedBorderColor = LoadrGreen,
                        unfocusedBorderColor = LoadrNavySurface,
                        focusedLabelColor = LoadrGreen,
                        unfocusedLabelColor = LoadrSlate
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LoadrWhite,
                        unfocusedTextColor = LoadrWhite,
                        focusedBorderColor = LoadrGreen,
                        unfocusedBorderColor = LoadrNavySurface,
                        focusedLabelColor = LoadrGreen,
                        unfocusedLabelColor = LoadrSlate
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LoadrWhite,
                            unfocusedTextColor = LoadrWhite,
                            focusedBorderColor = LoadrGreen,
                            unfocusedBorderColor = LoadrNavySurface,
                            focusedLabelColor = LoadrGreen,
                            unfocusedLabelColor = LoadrSlate
                        )
                    )
                    OutlinedTextField(
                        value = ussd,
                        onValueChange = { ussd = it },
                        label = { Text("USSD") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LoadrWhite,
                            unfocusedTextColor = LoadrWhite,
                            focusedBorderColor = LoadrGreen,
                            unfocusedBorderColor = LoadrNavySurface,
                            focusedLabelColor = LoadrGreen,
                            unfocusedLabelColor = LoadrSlate
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, price.toDoubleOrNull() ?: 0.0, type, ussd) },
                colors = ButtonDefaults.buttonColors(containerColor = LoadrGreen)
            ) {
                Text(if (offer == null) "Create" else "Save", color = LoadrNavy)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LoadrSlate)
            }
        }
    )
}
