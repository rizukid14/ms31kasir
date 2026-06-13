package com.mekarsari.kasir.ui.riwayat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mekarsari.kasir.data.local.dao.TransactionWithItems
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RiwayatScreen(
    viewModel: RiwayatViewModel,
    onNavigateToDetail: (transactionId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsState()

    // Group transactions by Date (e.g. "13 Juni 2026")
    val groupedTransactions = transactions.groupBy { tx ->
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
        sdf.format(Date(tx.transaction.createdAt))
    }

    if (transactions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Belum ada riwayat transaksi")
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Riwayat Transaksi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            groupedTransactions.forEach { (date, txList) ->
                stickyHeader {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 8.dp)
                    )
                }

                items(txList) { txWithItems ->
                    TransactionItemRow(
                        txWithItems = txWithItems,
                        onClick = { onNavigateToDetail(txWithItems.transaction.id) }
                    )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TX#${transaction.id} — $timeFormatted",
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
