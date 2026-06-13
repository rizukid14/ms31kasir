package com.mekarsari.kasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingEntry(
    @PrimaryKey val key: String,
    val value: String
)
