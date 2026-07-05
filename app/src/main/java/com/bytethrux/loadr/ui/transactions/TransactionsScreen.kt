package com.bytethrux.loadr.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bytethrux.loadr.data.network.TransactionDto
import com.bytethrux.loadr.data.transactions.DateFilter
import com.bytethrux.loadr.data.transactions.StatusFilter
import com.bytethrux.loadr.ui.theme.LoadrGreen
import com.bytethrux.loadr.ui.theme.LoadrGreenDim
import com.bytethrux.loadr.ui.theme.LoadrNavy
import com.bytethrux.loadr.ui.theme.LoadrNavyCard
import com.bytethrux.loadr.ui.theme.LoadrNavySurface
import com.bytethrux.loadr.ui.theme.LoadrRed
import com.bytethrux.loadr.ui.theme.LoadrSlate
import com.bytethrux.loadr.ui.theme.LoadrWhite
import com.bytethrux.loadr.ui.theme.LocalLoadrColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = LoadrNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoadrNavy),
                title = {
                    Text("Transactions", color = LoadrWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = LoadrWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(
                            imageVector = if (uiState.isSearchOpen) Icons.Outlined.Close else Icons.Outlined.Search,
                            contentDescription = if (uiState.isSearchOpen) "Close search" else "Search",
                            tint = LoadrWhite
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isSearchOpen) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search name, phone or package", color = LoadrSlate, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LoadrWhite,
                        unfocusedTextColor = LoadrWhite,
                        focusedBorderColor = LoadrGreen,
                        unfocusedBorderColor = LoadrNavySurface,
                        cursorColor = LoadrGreen,
                    )
                )
            }

            // ── Status filter chips ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusFilter.entries.forEach { filter ->
                    LoadrFilterChip(
                        label = filter.label,
                        selected = uiState.statusFilter == filter,
                        onClick = { viewModel.setStatusFilter(filter) }
                    )
                }
            }

            HorizontalDivider(color = LoadrNavySurface.copy(alpha = 0.5f), thickness = 0.5.dp)

            // ── Date filter chips ──────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateFilter.entries.forEach { filter ->
                    LoadrFilterChip(
                        label = filter.label,
                        selected = uiState.dateFilter == filter,
                        onClick = { viewModel.setDateFilter(filter) }
                    )
                }
            }

            // ── Content ────────────────────────────────────
            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LoadrGreen)
                }

                uiState.errorMessage != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.errorMessage ?: "Something went wrong", color = LoadrRed, fontSize = 13.sp)
                }

                uiState.filtered.isEmpty() -> EmptyState(uiState.statusFilter)

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filtered, key = { it.id }) { tx ->
                        TransactionRow(tx)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadrFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = LoadrGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = LoadrNavyCard,
            labelColor = LoadrSlate,
            selectedContainerColor = LoadrGreenDim,
            selectedLabelColor = LoadrGreen,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = LoadrNavySurface,
            selectedBorderColor = LoadrGreen,
        ),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun EmptyState(filter: StatusFilter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Inbox,
            contentDescription = null,
            tint = LoadrSlate,
            modifier = Modifier.size(44.dp)
        )
        Text(
            filter.emptyMessage,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LoadrSlate,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun TransactionRow(tx: TransactionDto) {
    val colors = LocalLoadrColors.current
    val isSuccess = tx.status == "success"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LoadrNavyCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSuccess) colors.successBg else colors.errorBg)
                .border(1.dp, if (isSuccess) LoadrGreen else LoadrRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Outlined.Check else Icons.Outlined.Close,
                contentDescription = tx.status,
                tint = if (isSuccess) LoadrGreen else LoadrRed,
                modifier = Modifier.size(14.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(tx.customer_name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LoadrWhite)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tx.package_name ?: "Bundle", fontSize = 11.sp, color = LoadrGreen)
                tx.customer_phone?.let {
                    Text(it, fontSize = 11.sp, color = LoadrSlate)
                }
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(tx.created_at, fontSize = 10.sp, color = LoadrSlate)
            Text("Ksh ${tx.amount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LoadrWhite)
        }
    }
}
