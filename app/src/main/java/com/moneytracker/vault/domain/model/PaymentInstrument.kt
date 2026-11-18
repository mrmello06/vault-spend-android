package com.moneytracker.vault.domain.model

import androidx.compose.runtime.Immutable
import com.moneytracker.vault.data.local.TransactionEntity

enum class TransactionType(val label: String) {
    DEBIT("Debited"),
    CREDIT("Credited")
}

enum class PaymentRail(val label: String, val badgeText: String) {
    CREDIT_CARD("Credit Card", "CREDIT CARD"),
    UPI("UPI", "UPI"),
    WALLET("Wallet", "WALLET"),
    DEBIT_CARD("Debit Card", "DEBIT CARD"),
    BANK_TRANSFER("Bank Transfer", "BANK"),
    ATM_CASH("ATM Cash", "ATM");

    companion object {
        fun fromNameOrDefault(name: String?): PaymentRail {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: UPI
        }
    }
}

@Immutable
data class DayTransactionGroup(
    val dayHeader: String,
    val dayDebitTotal: Double,
    val transactions: List<TransactionEntity>
)

@Immutable
data class CategoryBreakdown(
    val category: SpendCategory,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Float
)

@Immutable
data class MerchantSummary(
    val merchantDisplayName: String,
    val rawCodeSample: String,
    val category: SpendCategory,
    val totalAmount: Double,
    val count: Int,
    val isSubscription: Boolean
)

@Immutable
data class AccountSpendSummary(
    val key: String,
    val bankName: String,
    val accountLast4: String,
    val primaryRail: PaymentRail,
    val totalSpent: Double,
    val totalCredited: Double,
    val transactionCount: Int
)

@Immutable
data class UpiChannelSummary(
    val appName: String,
    val linkedBanksLabel: String,
    val totalSpent: Double,
    val totalCredited: Double,
    val transactionCount: Int,
    val sharePercentage: Float
)

@Immutable
data class WalletSummary(
    val walletName: String,
    val totalSpent: Double,
    val totalLoadedOrCredited: Double,
    val transactionCount: Int
)

@Immutable
data class MonthlyTrendPoint(
    val yearMonthKey: String,
    val shortLabel: String,
    val totalSpent: Double
)
