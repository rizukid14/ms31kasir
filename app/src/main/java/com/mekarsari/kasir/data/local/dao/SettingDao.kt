package com.mekarsari.kasir.data.local.dao

import androidx.room.*
import com.mekarsari.kasir.data.local.entity.SettingEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingDao {
    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(entry: SettingEntry)

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingEntry>>
}
