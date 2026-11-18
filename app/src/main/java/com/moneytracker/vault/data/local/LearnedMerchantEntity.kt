package com.moneytracker.vault.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learned_merchants")
data class LearnedMerchantEntity(
    @PrimaryKey
    val normalizedKey: String,
    val displayName: String,
    val category: String,
    val updatedAt: Long = System.currentTimeMillis()
)
