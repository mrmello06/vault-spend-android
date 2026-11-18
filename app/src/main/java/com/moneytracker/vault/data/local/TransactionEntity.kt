package com.moneytracker.vault.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["smsFingerprint"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["merchantRawCode"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val smsFingerprint: String,
    val amount: Double,
    val type: String, // DEBIT or CREDIT
    val merchantRawCode: String,
    val merchantDisplayName: String,
    val category: String, // SpendCategory.name
    val confidenceScore: Float,
    val matchReason: String,
    val bankName: String,
    val accountLast4: String,
    val paymentRail: String, // PaymentRail.name
    val upiApp: String,
    val mccCode: String?,
    val isSubscription: Boolean,
    val timestamp: Long,
    val senderAddress: String,
    val sanitizedPreview: String,
    val isUserModified: Boolean = false,
    val note: String = "",
    val isIncludedInSpend: Boolean = true,
    val isSelfTransfer: Boolean = false,
    val parserVersion: Int = 6,
    val fullSmsBody: String = ""
)
