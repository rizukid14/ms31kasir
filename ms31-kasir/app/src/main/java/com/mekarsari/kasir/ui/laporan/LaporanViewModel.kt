package com.mekarsari.kasir.ui.laporan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mekarsari.kasir.data.local.dao.TransactionWithItems
import com.mekarsari.kasir.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar

data class MonthlyReportState(
    val totalRevenue: Long = 0L,
    val totalTransactions: Int = 0,
    val averageTicket: Long = 0L,
    val topProducts: List<Pair<String, Int>> = emptyList(),
    val dailySales: Map<Int, Long> = emptyMap(),
    val maxDays: Int = 30
)

class LaporanViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    val reportState: StateFlow<MonthlyReportState> = combine(
        transactionRepository.allTransactionsWithItems,
        _selectedMonth,
        _selectedYear
    ) { transactions, month, year ->
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

        // 4. Calculate Top Products
        val productQuantities = mutableMapOf<String, Int>()
        filtered.forEach { tx ->
            tx.items.forEach { item ->
                val name = item.namaProdukSnapshot
                productQuantities[name] = (productQuantities[name] ?: 0) + item.qty
            }
        }
        val topProducts = productQuantities.toList()
            .sortedByDescending { it.second }
            .take(5)

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
}
