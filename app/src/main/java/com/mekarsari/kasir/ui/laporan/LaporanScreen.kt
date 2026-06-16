package com.mekarsari.kasir.ui.laporan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(
    viewModel: LaporanViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val reportState by viewModel.reportState.collectAsState()

    val indonesianMonths = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    val currentMonthName = indonesianMonths.getOrElse(selectedMonth) { "" }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Dialog konfirmasi hapus data bulanan
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Hapus Data $currentMonthName $selectedYear?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Data transaksi berikut akan dihapus permanen:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Jumlah Transaksi", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${reportState.totalTransactions} transaksi",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Omzet", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    formatRupiah(reportState.totalRevenue),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Text(
                        text = "⚠️ Aksi ini tidak bisa dibatalkan!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCurrentMonth { }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus Semua", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Month Switcher Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = borderStroke()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.prevMonth() }) {
                        Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Bulan Sebelumnya")
                    }
                    
                    Text(
                        text = "$currentMonthName $selectedYear",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Bulan Selanjutnya")
                    }
                }

                // Tombol hapus data — hanya aktif jika bulan ini punya transaksi
                if (reportState.totalTransactions > 0) {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Hapus ${reportState.totalTransactions} Data Bulan Ini",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // KPI Summary Dashboard Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Pendapatan",
                    value = formatRupiah(reportState.totalRevenue),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Transaksi",
                    value = "${reportState.totalTransactions} Kali",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Rata-rata",
                    value = formatRupiah(reportState.averageTicket),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section: Daily Sales Bar Chart
        item {
            val currentCalendar = remember { Calendar.getInstance() }
            val isCurrentMonthAndYear = selectedMonth == currentCalendar.get(Calendar.MONTH) &&
                    selectedYear == currentCalendar.get(Calendar.YEAR)
            val targetDay = if (isCurrentMonthAndYear) currentCalendar.get(Calendar.DAY_OF_MONTH) else 1

            Text(
                text = "Grafik Penjualan Harian",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            DailySalesChart(
                dailySales = reportState.dailySales,
                maxDays = reportState.maxDays,
                primaryColor = MaterialTheme.colorScheme.primary,
                targetDay = targetDay
            )
        }

        // Section: Top Selling Products
        item {
            Text(
                text = "5 Produk Terlaris",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (reportState.topProducts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada penjualan pada bulan ini.",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            val maxSoldQty = reportState.topProducts.maxOfOrNull { it.qty } ?: 1.0
            itemsIndexed(reportState.topProducts) { index, productReport ->
                TopProductRow(
                    rank = index + 1,
                    name = productReport.name,
                    qty = productReport.qty,
                    unit = productReport.unit,
                    maxQty = maxSoldQty,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = borderStroke()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun DailySalesChart(
    dailySales: Map<Int, Long>,
    maxDays: Int,
    primaryColor: Color,
    targetDay: Int
) {
    val scrollState = rememberScrollState()
    val maxSales = (dailySales.values.maxOfOrNull { it } ?: 0L).coerceAtLeast(1L)
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val viewportWidth = maxWidth
        val density = LocalDensity.current
        
        val centerDp = 30.dp + ((targetDay - 1) * 44).dp
        val targetScrollDp = centerDp - (viewportWidth / 2)
        val targetScrollPx = with(density) { targetScrollDp.toPx().toInt() }
        
        LaunchedEffect(targetDay, targetScrollPx) {
            scrollState.scrollTo(targetScrollPx.coerceAtLeast(0))
        }

        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = borderStroke()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (day in 1..maxDays) {
                    val sales = dailySales[day] ?: 0L
                    val percentage = sales.toFloat() / maxSales.toFloat()
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(28.dp)
                    ) {
                        // Tooltip/Value on top
                        if (sales > 0L) {
                            Text(
                                text = formatShortRupiah(sales),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // The Bar
                        val animatedHeight by animateFloatAsState(targetValue = percentage)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(maxOf((150 * animatedHeight).dp, 4.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (sales > 0L) primaryColor else MaterialTheme.colorScheme.surfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Date Label
                        Text(
                            text = day.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopProductRow(
    rank: Int,
    name: String,
    qty: Double,
    unit: String,
    maxQty: Double,
    color: Color
) {
    val ratio = if (maxQty > 0.0) qty.toFloat() / maxQty.toFloat() else 0f
    val animatedRatio by animateFloatAsState(targetValue = ratio)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rank Circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = color
            )
        }

        // Product Details and Progress Bar
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${formatQty(qty)} $unit",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Custom Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedRatio)
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outline
)

private fun formatQty(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

private fun formatRupiah(value: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    return format.format(value).replace("Rp", "Rp ")
}

private fun formatShortRupiah(value: Long): String {
    return when {
        value >= 1_000_000L -> "${String.format("%.1f", value.toDouble() / 1_000_000L)}M"
        value >= 1_000L -> "${value / 1_000L}K"
        else -> value.toString()
    }
}


