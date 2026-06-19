package com.mekarsari.kasir.ui.kasir

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mekarsari.kasir.domain.model.CartItem
import com.mekarsari.kasir.util.CurrencyUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditPrice: (Long) -> Unit,
    onEditPortion: (Double?) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showPriceEditDialog by remember { mutableStateOf(false) }
    var showPortionEditDialog by remember { mutableStateOf(false) }
    var tempPriceText by remember { mutableStateOf(item.customHarga.toString()) }
    var selectedPortion by remember { mutableStateOf<Double?>(item.customPortion) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    tempPriceText = item.customHarga.toString()
                    showPriceEditDialog = true
                }
        ) {
            val displayName = when {
                item.customPortion != null -> "${item.product.nama} (${item.customPortion} Porsi)"
                else -> item.product.nama
            }
            Text(
                text = displayName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = CurrencyUtil.formatRupiah(item.customHarga),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit harga",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                if (item.customHarga != item.product.harga) {
                    Text(
                        text = "(${CurrencyUtil.formatRupiah(item.product.harga)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onDecrement()
                },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold)
            }

            // Clickable quantity text to adjust fractional portions
            Box(
                modifier = Modifier
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        selectedPortion = item.customPortion
                        showPortionEditDialog = true
                    }
                    .padding(horizontal = 8.dp)
            ) {
                val displayQty = if (item.customPortion != null) {
                    val totalPortion = item.customPortion * item.quantity
                    // Format to remove trailing zeros if it's a whole number (e.g. 4.0 -> 4)
                    if (totalPortion % 1.0 == 0.0) totalPortion.toInt().toString() else totalPortion.toString()
                } else {
                    item.quantity.toString()
                }
                Text(
                    text = displayQty,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.customPortion != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            OutlinedButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onIncrement()
                },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Dialog 1: Sesuaikan Harga Barang (Klik Nama / Harga)
    if (showPriceEditDialog) {
        AlertDialog(
            onDismissRequest = { showPriceEditDialog = false },
            title = { Text("Sesuaikan Harga Barang") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Harga Master: ${CurrencyUtil.formatRupiah(item.product.harga)}")
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempPriceText,
                        onValueChange = { tempPriceText = it },
                        label = { Text("Harga Baru (Rupiah)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val parsed = tempPriceText.toLongOrNull()
                        if (parsed != null && parsed in 0..10_000_000L) {
                            onEditPrice(parsed)
                            showPriceEditDialog = false
                        } else if (parsed != null && parsed > 10_000_000L) {
                            Toast.makeText(context, "Harga maksimal adalah Rp10.000.000", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onEditPrice(item.product.harga)
                        showPriceEditDialog = false
                    }
                ) {
                    Text("Reset Asli")
                }
            }
        )
    }

    // Dialog 2: Sesuaikan Porsi Pecahan (Klik Angka Quantity)
    if (showPortionEditDialog) {
        var tempPortionText by remember { mutableStateOf(item.customPortion?.toString() ?: "1.0") }

        AlertDialog(
            onDismissRequest = { showPortionEditDialog = false },
            title = { Text("Sesuaikan Jumlah Porsi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Harga Master: ${CurrencyUtil.formatRupiah(item.product.harga)} / porsi")
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = tempPortionText,
                        onValueChange = { 
                            // Allow digits, dots, and empty string
                            if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) {
                                tempPortionText = it
                            }
                        },
                        label = { Text("Jumlah Porsi (Desimal, contoh: 4.6)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pilihan Pintasan Cepat:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    
                    // Horizontal scrollable chips for portions
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val portions = listOf(0.5, 0.7, 1.0, 1.5, 2.5, 3.5, 4.5)
                        items(portions) { p ->
                            FilterChip(
                                selected = tempPortionText.toDoubleOrNull() == p,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    tempPortionText = p.toString()
                                },
                                label = { Text(text = if (p == 1.0) "Normal (1.0)" else p.toString(), fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val parsedPortion = tempPortionText.toDoubleOrNull()
                        if (parsedPortion != null && parsedPortion > 0.0) {
                            val finalPortion = if (parsedPortion == 1.0) null else parsedPortion
                            onEditPortion(finalPortion)
                        }
                        showPortionEditDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onEditPortion(null)
                        showPortionEditDialog = false
                    }
                ) {
                    Text("Reset Normal")
                }
            }
        )
    }
}
