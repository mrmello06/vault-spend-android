package com.moneytracker.vault.domain.model

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

data class CategoryBreakdown(
    val category: SpendCategory,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Float
)

data class MerchantSummary(
    val merchantDisplayName: String,
    val rawCodeSample: String,
    val category: SpendCategory,
    val totalAmount: Double,
    val count: Int,
    val isSubscription: Boolean
)

data class AccountSpendSummary(
    val key: String,
    val bankName: String,
    val accountLast4: String,
    val primaryRail: PaymentRail,
    val totalSpent: Double,
    val totalCredited: Double,
    val transactionCount: Int
)

data class UpiChannelSummary(
    val appName: String,
    val linkedBanksLabel: String,
    val totalSpent: Double,
    val totalCredited: Double,
    val transactionCount: Int,
    val sharePercentage: Float
)

data class WalletSummary(
    val walletName: String,
    val totalSpent: Double,
    val totalLoadedOrCredited: Double,
    val transactionCount: Int
)

data class MonthlyTrendPoint(
    val yearMonthKey: String,
    val shortLabel: String,
    val totalSpent: Double
)
