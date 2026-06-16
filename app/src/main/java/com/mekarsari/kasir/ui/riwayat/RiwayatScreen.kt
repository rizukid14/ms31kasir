package com.mekarsari.kasir.ui.riwayat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mekarsari.kasir.data.local.dao.TransactionWithItems
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    viewModel: RiwayatViewModel,
    onNavigateToDetail: (transactionId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsState()

    var selectedMonth by remember { mutableStateOf(0) } // 0 = Semua Bulan, 1 = Januari, ..., 12 = Desember
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    val monthOptions = listOf(
        "Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    val years = remember(transactions) {
        val available = transactions.map { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.transaction.createdAt }
            cal.get(Calendar.YEAR)
        }.distinct().sortedDescending()
        if (available.isEmpty()) listOf(Calendar.getInstance().get(Calendar.YEAR)) else available
    }

    // Ensure selectedYear is in the list of available years
    LaunchedEffect(years) {
        if (selectedYear !in years && years.isNotEmpty()) {
            selectedYear = years.first()
        }
    }

    val filteredTransactions = remember(transactions, selectedMonth, selectedYear) {
        transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.transaction.createdAt }
            val txYear = cal.get(Calendar.YEAR)
            if (selectedMonth == 0) {
                txYear == selectedYear // still filter by selected year even when "Semua Bulan"
            } else {
                val txMonth = cal.get(Calendar.MONTH) + 1
                txMonth == selectedMonth && txYear == selectedYear
            }
        }
    }

    // Group transactions by Date (e.g. "13 Juni 2026")
    val groupedTransactions = filteredTransactions.groupBy { tx ->
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
        sdf.format(Date(tx.transaction.createdAt))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Riwayat Transaksi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Month & Year Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Month Dropdown
            var monthMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1.5f)) {
                OutlinedButton(
                    onClick = { monthMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(monthOptions[selectedMonth], maxLines = 1)
                }
                DropdownMenu(
                    expanded = monthMenuExpanded,
                    onDismissRequest = { monthMenuExpanded = false }
                ) {
                    monthOptions.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedMonth = index
                                monthMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Year Dropdown
            var yearMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { yearMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedYear.toString(), maxLines = 1)
                }
                DropdownMenu(
                    expanded = yearMenuExpanded,
                    onDismissRequest = { yearMenuExpanded = false }
                ) {
                    years.forEach { yr ->
                        DropdownMenuItem(
                            text = { Text(yr.toString()) },
                            onClick = {
                                selectedYear = yr
                                yearMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (transactions.isEmpty()) "Belum ada riwayat transaksi" else "Tidak ada transaksi di bulan pilihan",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedTransactions.forEach { (date, txList) ->
                    stickyHeader {
                        val dayTotal = txList.sumOf { it.transaction.total }
                        val dayFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply { maximumFractionDigits = 0 }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = dayFormat.format(dayTotal).replace("Rp", "Rp "),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(txList, key = { it.transaction.id }) { txWithItems ->
                        TransactionItemRow(
                            txWithItems = txWithItems,
                            onClick = { onNavigateToDetail(txWithItems.transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    txWithItems: TransactionWithItems,
    onClick: () -> Unit
) {
    val transaction = txWithItems.transaction
    val items = txWithItems.items

    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    val totalFormatted = format.format(transaction.total).replace("Rp", "Rp ")

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeFormatted = timeFormat.format(Date(transaction.createdAt))

    val itemsSummary = items.joinToString(", ") { "${it.namaProdukSnapshot} (x${it.qty})" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val titleText = if (!transaction.nomorMeja.isNullOrEmpty()) {
                    "TX#${transaction.id} (Meja ${transaction.nomorMeja}) — $timeFormatted"
                } else {
                    "TX#${transaction.id} — $timeFormatted"
                }
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = itemsSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
            Text(
                text = totalFormatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
