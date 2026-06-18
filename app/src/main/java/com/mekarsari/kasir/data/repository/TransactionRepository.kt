package com.mekarsari.kasir.data.repository

import com.mekarsari.kasir.data.local.dao.TransactionDao
import com.mekarsari.kasir.data.local.dao.TransactionWithItems
import com.mekarsari.kasir.data.local.entity.Transaction
import com.mekarsari.kasir.data.local.entity.TransactionItem
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactionsWithItems: Flow<List<TransactionWithItems>> = 
        transactionDao.getAllTransactionsWithItems()

    suspend fun getTransactionWithItemsById(id: Int): TransactionWithItems? {
        return transactionDao.getTransactionWithItemsById(id)
    }

    suspend fun insertTransactionWithItems(transaction: Transaction, items: List<TransactionItem>): Long {
        return transactionDao.insertTransactionWithItems(transaction, items)
    }

    suspend fun updateTransactionWithItems(transaction: Transaction, items: List<TransactionItem>) {
        transactionDao.updateTransactionWithItems(transaction, items)
    }

    suspend fun deleteTransactionsByMonth(month: Int, year: Int): Int {
        val cal = java.util.Calendar.getInstance()
        // Start: 1st day of month at 00:00:00.000
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        // End: 1st day of NEXT month at 00:00:00.000
        cal.add(java.util.Calendar.MONTH, 1)
        val endMs = cal.timeInMillis
        transactionDao.deleteTransactionsByTimeRange(startMs, endMs)
        // Return count of deleted rows (approximated from filtered list not needed;
        // caller already knows count from reportState)
        return 0
    }

    suspend fun deleteTransactionById(id: Int) {
        transactionDao.deleteTransactionById(id)
    }

    fun getMostOrderedProductIds(): Flow<List<Int>> {
        return transactionDao.getMostOrderedProductIds()
    }
}
