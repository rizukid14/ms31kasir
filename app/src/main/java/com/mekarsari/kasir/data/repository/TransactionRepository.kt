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
}
