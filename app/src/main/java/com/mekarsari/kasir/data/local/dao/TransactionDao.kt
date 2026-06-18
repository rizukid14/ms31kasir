package com.mekarsari.kasir.data.local.dao

import androidx.room.*
import com.mekarsari.kasir.data.local.entity.Transaction
import com.mekarsari.kasir.data.local.entity.TransactionItem
import kotlinx.coroutines.flow.Flow

data class TransactionWithItems(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "id",
        entityColumn = "transaction_id"
    )
    val items: List<TransactionItem>
)

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransactionOnly(transaction: Transaction): Long

    @Insert
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Update
    suspend fun updateTransactionOnly(transaction: Transaction)

    @Query("DELETE FROM transaction_items WHERE transaction_id = :transactionId")
    suspend fun deleteTransactionItemsByTransactionId(transactionId: Int)

    @androidx.room.Transaction
    suspend fun updateTransactionWithItems(transaction: Transaction, items: List<TransactionItem>) {
        updateTransactionOnly(transaction)
        deleteTransactionItemsByTransactionId(transaction.id)
        val itemsWithId = items.map { it.copy(transactionId = transaction.id) }
        insertTransactionItems(itemsWithId)
    }

    @androidx.room.Transaction
    suspend fun insertTransactionWithItems(transaction: Transaction, items: List<TransactionItem>): Long {
        val transactionId = insertTransactionOnly(transaction).toInt()
        val itemsWithId = items.map { it.copy(transactionId = transactionId) }
        insertTransactionItems(itemsWithId)
        return transactionId.toLong()
    }

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    fun getAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionWithItemsById(id: Int): TransactionWithItems?

    @Query("DELETE FROM transactions WHERE created_at >= :startMs AND created_at < :endMs")
    suspend fun deleteTransactionsByTimeRange(startMs: Long, endMs: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("SELECT product_id FROM transaction_items GROUP BY product_id ORDER BY SUM(qty) DESC")
    fun getMostOrderedProductIds(): Flow<List<Int>>
}
