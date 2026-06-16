package com.mekarsari.kasir.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DatabaseBackupManager {
    private const val DB_NAME = "mekarsari_kasir_db"

    suspend fun backupDatabase(context: Context, outputUri: Uri, db: AppDatabase): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Force checkpoint so WAL data is merged into the main db file
            val supportDb = db.openHelper.writableDatabase
            val cursor = supportDb.query("PRAGMA wal_checkpoint(FULL)")
            cursor.moveToFirst()
            cursor.close()

            // 2. Get the main database file path
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("File database tidak ditemukan"))
            }

            // 3. Copy db file to the output URI (Storage Access Framework)
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(Exception("Gagal membuka output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreDatabase(context: Context, inputUri: Uri, db: AppDatabase): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Close database to release locks
            db.close()

            val dbFile = context.getDatabasePath(DB_NAME)
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

            // 2. Copy the backup file to the database path
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(Exception("Gagal membuka input stream"))

            // 3. Delete WAL and SHM files to avoid consistency issues with the new database
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
