package com.mekarsari.kasir.ui.kasir

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mekarsari.kasir.data.local.entity.Product
import com.mekarsari.kasir.util.CurrencyUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactProductRow(
    product: Product,
    quantity: Int = 0,
    showCategoryLabel: Boolean = true,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Card(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = if (quantity > 0) 2.dp else 0.5.dp,
            color = if (quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (quantity > 0) {
                        Text(
                            text = "${quantity}x ",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = product.nama,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (quantity > 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (showCategoryLabel && !product.kategori.isNullOrEmpty()) {
                    Text(
                        text = product.kategori,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = if (quantity > 0) 0.7f else 1.0f)
                    )
                }
            }
            Text(
                text = CurrencyUtil.formatRupiah(product.harga),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (quantity > 0) 0.7f else 1.0f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
