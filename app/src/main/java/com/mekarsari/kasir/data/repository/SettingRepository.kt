package com.mekarsari.kasir.data.repository

import com.mekarsari.kasir.data.local.dao.SettingDao
import com.mekarsari.kasir.data.local.entity.SettingEntry
import kotlinx.coroutines.flow.Flow

class SettingRepository(private val settingDao: SettingDao) {
    val allSettings: Flow<List<SettingEntry>> = settingDao.getAllSettings()

    suspend fun getSettingValue(key: String): String? {
        return settingDao.getSettingValue(key)
    }

    suspend fun saveSetting(key: String, value: String) {
        settingDao.insertSetting(SettingEntry(key, value))
    }
}
