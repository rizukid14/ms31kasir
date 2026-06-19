package com.mekarsari.kasir.ui.laporan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mekarsari.kasir.data.repository.ProductRepository
import com.mekarsari.kasir.data.repository.TransactionRepository
import com.mekarsari.kasir.data.repository.SettingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class TopProductReport(
    val name: String,
    val qty: Double,
    val unit: String
)

data class MonthlyReportState(
    val totalRevenue: Long = 0L,
    val totalTransactions: Int = 0,
    val averageTicket: Long = 0L,
    val topProducts: List<TopProductReport> = emptyList(),
    val dailySales: Map<Int, Long> = emptyMap(),
    val maxDays: Int = 30
)

class LaporanViewModel(
    private val transactionRepository: TransactionRepository,
    private val productRepository: ProductRepository,
    private val settingRepository: SettingRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    val reportState: StateFlow<MonthlyReportState> = combine(
        transactionRepository.allTransactionsWithItems,
        productRepository.allProducts,
        _selectedMonth,
        _selectedYear
    ) { transactions, products, month, year ->
        val txCalendar = Calendar.getInstance()
        
        // Filter transactions for the selected month and year
        val filtered = transactions.filter { tx ->
            txCalendar.timeInMillis = tx.transaction.createdAt
            txCalendar.get(Calendar.MONTH) == month && txCalendar.get(Calendar.YEAR) == year
        }

        // Get actual maximum days in the selected month/year
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 1. Total revenue
        val totalRevenue = filtered.sumOf { it.transaction.total }

        // 2. Total transactions
        val totalTxCount = filtered.size

        // 3. Average ticket size
        val avgTicket = if (totalTxCount > 0) totalRevenue / totalTxCount else 0L

        // Create a map of product name to category
        val productCategoryMap = products.associate { it.nama to (it.kategori ?: "Makanan") }

        // 4. Calculate Top Products (Combining full portion and 1/2 portion)
        val productQuantities = mutableMapOf<String, Double>()
        val productUnits = mutableMapOf<String, String>()

        filtered.forEach { tx ->
            tx.items.forEach { item ->
                val name = item.namaProdukSnapshot
                val (baseName, multiplier) = if (name.endsWith(" (1/2 Porsi)")) {
                    name.removeSuffix(" (1/2 Porsi)") to 0.5
                } else {
                    name to 1.0
                }
                val qtyToAdd = item.qty * multiplier
                productQuantities[baseName] = (productQuantities[baseName] ?: 0.0) + qtyToAdd

                // Determine unit ("Gelas" for Minuman, "Porsi" for others)
                val category = productCategoryMap[baseName] ?: "Makanan"
                val unit = if (category.equals("Minuman", ignoreCase = true)) "Gelas" else "Porsi"
                productUnits[baseName] = unit
            }
        }
        
        val topProducts = productQuantities.toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { (baseName, qty) ->
                TopProductReport(
                    name = baseName,
                    qty = qty,
                    unit = productUnits[baseName] ?: "Porsi"
                )
            }

        // 5. Daily Sales mapping
        val dailySalesMap = (1..maxDays).associateWith { 0L }.toMutableMap()
        filtered.forEach { tx ->
            txCalendar.timeInMillis = tx.transaction.createdAt
            val day = txCalendar.get(Calendar.DAY_OF_MONTH)
            dailySalesMap[day] = (dailySalesMap[day] ?: 0L) + tx.transaction.total
        }

        MonthlyReportState(
            totalRevenue = totalRevenue,
            totalTransactions = totalTxCount,
            averageTicket = avgTicket,
            topProducts = topProducts,
            dailySales = dailySalesMap,
            maxDays = maxDays
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyReportState()
    )

    val todayReportState: StateFlow<MonthlyReportState> = combine(
        transactionRepository.allTransactionsWithItems,
        productRepository.allProducts
    ) { transactions, products ->
        val txCalendar = Calendar.getInstance()
        val todayCalendar = Calendar.getInstance()
        
        // Filter transactions created today (same year, month, and day)
        val filtered = transactions.filter { tx ->
            txCalendar.timeInMillis = tx.transaction.createdAt
            txCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                    txCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                    txCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)
        }

        val totalRevenue = filtered.sumOf { it.transaction.total }
        val totalTxCount = filtered.size
        val avgTicket = if (totalTxCount > 0) totalRevenue / totalTxCount else 0L

        val productCategoryMap = products.associate { it.nama to (it.kategori ?: "Makanan") }
        val productQuantities = mutableMapOf<String, Double>()
        val productUnits = mutableMapOf<String, String>()

        filtered.forEach { tx ->
            tx.items.forEach { item ->
                val name = item.namaProdukSnapshot
                val (baseName, multiplier) = if (name.endsWith(" (1/2 Porsi)")) {
                    name.removeSuffix(" (1/2 Porsi)") to 0.5
                } else {
                    name to 1.0
                }
                val qtyToAdd = item.qty * multiplier
                productQuantities[baseName] = (productQuantities[baseName] ?: 0.0) + qtyToAdd

                val category = productCategoryMap[baseName] ?: "Makanan"
                val unit = if (category.equals("Minuman", ignoreCase = true)) "Gelas" else "Porsi"
                productUnits[baseName] = unit
            }
        }
        
        val topProducts = productQuantities.toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { (baseName, qty) ->
                TopProductReport(
                    name = baseName,
                    qty = qty,
                    unit = productUnits[baseName] ?: "Porsi"
                )
            }

        MonthlyReportState(
            totalRevenue = totalRevenue,
            totalTransactions = totalTxCount,
            averageTicket = avgTicket,
            topProducts = topProducts,
            dailySales = emptyMap(),
            maxDays = 1
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyReportState()
    )

    val settingsMap: StateFlow<Map<String, String>> = settingRepository.allSettings
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun nextMonth() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == Calendar.DECEMBER) {
            _selectedMonth.value = Calendar.JANUARY
            _selectedYear.value = currentYear + 1
        } else {
            _selectedMonth.value = currentMonth + 1
        }
    }

    fun prevMonth() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == Calendar.JANUARY) {
            _selectedMonth.value = Calendar.DECEMBER
            _selectedYear.value = currentYear - 1
        } else {
            _selectedMonth.value = currentMonth - 1
        }
    }

    fun deleteCurrentMonth(onDone: () -> Unit) {
        viewModelScope.launch {
            transactionRepository.deleteTransactionsByMonth(
                month = _selectedMonth.value,
                year = _selectedYear.value
            )
            onDone()
        }
    }
}
