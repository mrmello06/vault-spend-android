package com.moneytracker.vault.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.vault.ai.IterativeLedgerAnalyzer
import com.moneytracker.vault.ai.MerchantKnowledgeBase
import com.moneytracker.vault.ai.OfflineExpenseAiEngine
import com.moneytracker.vault.data.local.LearnedMerchantEntity
import com.moneytracker.vault.data.local.TransactionEntity
import com.moneytracker.vault.data.local.VaultDatabase
import com.moneytracker.vault.domain.model.AccountSpendSummary
import com.moneytracker.vault.domain.model.CategoryBreakdown
import com.moneytracker.vault.domain.model.MerchantSummary
import com.moneytracker.vault.domain.model.MonthlyTrendPoint
import com.moneytracker.vault.domain.model.PaymentRail
import com.moneytracker.vault.domain.model.SpendCategory
import com.moneytracker.vault.domain.model.TransactionType
import com.moneytracker.vault.domain.model.UpiChannelSummary
import com.moneytracker.vault.domain.model.WalletSummary
import com.moneytracker.vault.sms.SmsInboxScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.random.Random

enum class MoneyMatterFilter {
    DEBITED,
    CREDITED,
    CARD_BILLS,
    SELF_TRANSFER
}

data class VaultUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val isCurrentMonthSelected: Boolean = true,
    val selectedMonthLabel: String = "",
    val monthTotalDebited: Double = 0.0,
    val monthTotalCredited: Double = 0.0,
    val monthSelfTransferTotal: Double = 0.0,
    val monthCardBillsPaidTotal: Double = 0.0,
    val monthDailyAverage: Double = 0.0,
    val monthTransactionCount: Int = 0,
    val previousMonthDeltaPercent: Float? = null,
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val totalAllTimeCount: Int = 0,
    val hasDemoTransactions: Boolean = false,
    val categoryBreakdowns: List<CategoryBreakdown> = emptyList(),
    val topMerchants: List<MerchantSummary> = emptyList(),
    val creditCardSummaries: List<AccountSpendSummary> = emptyList(),
    val upiChannelSummaries: List<UpiChannelSummary> = emptyList(),
    val walletSummaries: List<WalletSummary> = emptyList(),
    val bankAccountSummaries: List<AccountSpendSummary> = emptyList(),
    val monthlyTrend: List<MonthlyTrendPoint> = emptyList(),
    val learnedMerchants: List<LearnedMerchantEntity> = emptyList(),
    val aiSearchSuggestions: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedMoneyMatter: MoneyMatterFilter? = null,
    val selectedCategory: SpendCategory? = null,
    val selectedAccountKey: String? = null,
    val selectedUpiApp: String? = null,
    val isSyncing: Boolean = false,
    val aiTrainingStatus: String = "",
    val statusBanner: String? = null,
    val undoDeletedTransaction: TransactionEntity? = null,
    val undoSecondsRemaining: Int = 0,
    val sandboxResult: OfflineExpenseAiEngine.AiParseOutcome? = null
)

private fun TransactionEntity.isSelfTransferEntry(): Boolean {
    return isSelfTransfer || category == SpendCategory.SELF_TRANSFER.name
}

private fun TransactionEntity.isCardBillSettlement(): Boolean {
    return category == SpendCategory.CREDIT_CARD_BILL.name
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val dao = VaultDatabase.getInstance(appContext).vaultDao()

    private val selectedMonthFlow = MutableStateFlow(YearMonth.now())
    private val searchQueryFlow = MutableStateFlow("")
    private val selectedMoneyMatterFlow = MutableStateFlow<MoneyMatterFilter?>(null)
    private val selectedCategoryFlow = MutableStateFlow<SpendCategory?>(null)
    private val selectedAccountFlow = MutableStateFlow<String?>(null)
    private val selectedUpiAppFlow = MutableStateFlow<String?>(null)

    private var undoTimerJob: Job? = null

    private data class FilterState(
        val query: String,
        val moneyMatter: MoneyMatterFilter?,
        val category: SpendCategory?,
        val accountKey: String?,
        val upiApp: String?
    )

    private data class AuxUiMeta(
        val isSyncing: Boolean = false,
        val aiTrainingStatus: String = "",
        val statusBanner: String? = null,
        val undoDeletedTransaction: TransactionEntity? = null,
        val undoSecondsRemaining: Int = 0,
        val sandboxResult: OfflineExpenseAiEngine.AiParseOutcome? = null,
        val suggestionSeed: Long = System.nanoTime()
    )

    private val auxMetaFlow = MutableStateFlow(
        AuxUiMeta(aiTrainingStatus = SmsInboxScanner.getTrainingStatsLabel(appContext))
    )

    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    private val shortMonthYearFormatter = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH)

    private val filtersFlow = combine(
        searchQueryFlow,
        selectedMoneyMatterFlow,
        selectedCategoryFlow,
        selectedAccountFlow,
        selectedUpiAppFlow
    ) { q, mm, c, a, u -> FilterState(q, mm, c, a, u) }

    val uiState: StateFlow<VaultUiState> = combine(
        dao.observeAllTransactions(),
        dao.observeLearnedMerchants(),
        selectedMonthFlow,
        filtersFlow,
        auxMetaFlow
    ) { allTxns, learnedList, selectedYm, filters, aux ->
        val zone = ZoneId.systemDefault()
        val todayYm = YearMonth.now(zone)

        val txnsByMonth = allTxns.groupBy { txn ->
            YearMonth.from(Instant.ofEpochMilli(txn.timestamp).atZone(zone))
        }

        val monthTxns = txnsByMonth[selectedYm].orEmpty()

        // True Spends exclude Self-Transfers AND exclude Credit Card Bill settlements (so card swipes aren't double-counted!)
        val activeDebits = monthTxns.filter {
            it.type == TransactionType.DEBIT.name &&
                !it.isSelfTransferEntry() &&
                !it.isCardBillSettlement() &&
                it.isIncludedInSpend
        }
        val activeCredits = monthTxns.filter {
            it.type == TransactionType.CREDIT.name &&
                !it.isSelfTransferEntry() &&
                !it.isCardBillSettlement() &&
                it.isIncludedInSpend
        }

        val totalDebited = activeDebits.sumOf { it.amount }
        val totalCredited = activeCredits.sumOf { it.amount }

        val selfDebitSum = monthTxns.filter { it.isSelfTransferEntry() && it.type == TransactionType.DEBIT.name }.sumOf { it.amount }
        val selfCreditSum = monthTxns.filter { it.isSelfTransferEntry() && it.type == TransactionType.CREDIT.name }.sumOf { it.amount }
        val selfTransferVolume = maxOf(selfDebitSum, selfCreditSum)

        val cardBillsPaid = monthTxns
            .filter { it.isCardBillSettlement() && it.type == TransactionType.DEBIT.name }
            .sumOf { it.amount }

        val prevMonthSpent = txnsByMonth[selectedYm.minusMonths(1)]
            .orEmpty()
            .filter {
                it.type == TransactionType.DEBIT.name &&
                    !it.isSelfTransferEntry() &&
                    !it.isCardBillSettlement() &&
                    it.isIncludedInSpend
            }
            .sumOf { it.amount }

        val momDelta = if (prevMonthSpent > 0.0) {
            (((totalDebited - prevMonthSpent) / prevMonthSpent) * 100.0).toFloat()
        } else null

        val elapsedDays = if (selectedYm == todayYm) {
            Instant.now().atZone(zone).dayOfMonth.coerceAtLeast(1)
        } else {
            selectedYm.lengthOfMonth()
        }
        val dailyAvg = if (totalDebited > 0) totalDebited / elapsedDays else 0.0

        val q = filters.query.trim()
        val baseFeed = if (q.isNotEmpty()) allTxns else monthTxns

        val filtered = baseFeed.filter { txn ->
            val catObj = SpendCategory.fromNameOrDefault(txn.category)
            val matchesQuery = q.isEmpty() ||
                txn.fullSmsBody.contains(q, ignoreCase = true) ||
                txn.sanitizedPreview.contains(q, ignoreCase = true) ||
                txn.note.contains(q, ignoreCase = true) ||
                txn.merchantDisplayName.contains(q, ignoreCase = true) ||
                txn.merchantRawCode.contains(q, ignoreCase = true) ||
                txn.bankName.contains(q, ignoreCase = true) ||
                txn.accountLast4.contains(q, ignoreCase = true) ||
                txn.upiApp.contains(q, ignoreCase = true) ||
                txn.paymentRail.contains(q, ignoreCase = true) ||
                txn.category.contains(q, ignoreCase = true) ||
                catObj.displayName.contains(q, ignoreCase = true) ||
                txn.senderAddress.contains(q, ignoreCase = true) ||
                txn.amount.toString().contains(q) ||
                txn.amount.toInt().toString().contains(q)

            val matchesMoneyMatter = when (filters.moneyMatter) {
                null -> true
                MoneyMatterFilter.DEBITED ->
                    txn.type == TransactionType.DEBIT.name &&
                        !txn.isSelfTransferEntry() &&
                        !txn.isCardBillSettlement()
                MoneyMatterFilter.CREDITED ->
                    txn.type == TransactionType.CREDIT.name &&
                        !txn.isSelfTransferEntry() &&
                        !txn.isCardBillSettlement()
                MoneyMatterFilter.CARD_BILLS -> txn.isCardBillSettlement()
                MoneyMatterFilter.SELF_TRANSFER -> txn.isSelfTransferEntry()
            }

            val matchesCat = filters.category == null ||
                (filters.category == SpendCategory.SELF_TRANSFER && txn.isSelfTransferEntry()) ||
                txn.category == filters.category.name

            val acctKey = "${txn.bankName}|${txn.accountLast4}"
            val matchesAcct = filters.accountKey == null || acctKey == filters.accountKey

            val resolvedUpiApp = txn.upiApp.ifBlank { "Bank UPI" }
            val matchesUpi = filters.upiApp == null ||
                (txn.paymentRail == PaymentRail.UPI.name && resolvedUpiApp.equals(filters.upiApp, ignoreCase = true))

            matchesQuery && matchesMoneyMatter && matchesCat && matchesAcct && matchesUpi
        }

        val catBreakdowns = if (totalDebited > 0.0) {
            activeDebits
                .groupBy { SpendCategory.fromNameOrDefault(it.category) }
                .map { (cat, list) ->
                    val sum = list.sumOf { it.amount }
                    CategoryBreakdown(
                        category = cat,
                        totalAmount = sum,
                        transactionCount = list.size,
                        percentage = ((sum / totalDebited) * 100.0).toFloat()
                    )
                }
                .sortedByDescending { it.totalAmount }
        } else emptyList()

        val topMerchants = activeDebits
            .groupBy { it.merchantDisplayName }
            .map { (name, list) ->
                val first = list.first()
                MerchantSummary(
                    merchantDisplayName = name,
                    rawCodeSample = first.merchantRawCode,
                    category = SpendCategory.fromNameOrDefault(first.category),
                    totalAmount = list.sumOf { it.amount },
                    count = list.size,
                    isSubscription = list.any { it.isSubscription }
                )
            }
            .sortedByDescending { it.totalAmount }

        val instrumentSource = monthTxns.ifEmpty { allTxns }.filter { it.isIncludedInSpend }

        val creditCards = instrumentSource
            .filter { it.paymentRail == PaymentRail.CREDIT_CARD.name }
            .groupBy { "${it.bankName}|${it.accountLast4}" }
            .map { (key, list) ->
                val first = list.first()
                AccountSpendSummary(
                    key = key,
                    bankName = first.bankName,
                    accountLast4 = first.accountLast4,
                    primaryRail = PaymentRail.CREDIT_CARD,
                    totalSpent = list.filter { it.type == TransactionType.DEBIT.name && !it.isSelfTransferEntry() && !it.isCardBillSettlement() }.sumOf { it.amount },
                    totalCredited = list.filter { it.type == TransactionType.CREDIT.name && !it.isSelfTransferEntry() && !it.isCardBillSettlement() }.sumOf { it.amount },
                    transactionCount = list.size
                )
            }
            .sortedByDescending { it.totalSpent }

        val upiTxns = instrumentSource.filter { it.paymentRail == PaymentRail.UPI.name }
        val upiTotalDebit = upiTxns
            .filter { it.type == TransactionType.DEBIT.name && !it.isSelfTransferEntry() && !it.isCardBillSettlement() }
            .sumOf { it.amount }
            .coerceAtLeast(1.0)

        val upiChannels = upiTxns
            .groupBy { it.upiApp.ifBlank { "Bank UPI" } }
            .map { (appName, list) ->
                val spent = list.filter { it.type == TransactionType.DEBIT.name && !it.isSelfTransferEntry() && !it.isCardBillSettlement() }.sumOf { it.amount }
                val cred = list.filter { it.type == TransactionType.CREDIT.name && !it.isSelfTransferEntry() && !it.isCardBillSettlement() }.sumOf { it.amount }
                val banksLabel = list
                    .map { if (it.accountLast4 != "0000") "${it.bankName} ••${it.accountLast4}" else it.bankName }
                    .distinct()
                    .take(2)
                    .joinToString(", ")

                UpiChannelSummary(
                    appName = appName,
                    linkedBanksLabel = banksLabel,
                    totalSpent = spent,
                    totalCredited = cred,
                    transactionCount = list.size,
                    sharePercentage = ((spent / upiTotalDebit) * 100.0).toFloat()
                )
            }
            .sortedByDescending { it.totalSpent }

        val wallets = instrumentSource
            .filter { it.paymentRail == PaymentRail.WALLET.name }
            .groupBy { it.bankName }
            .map { (walletName, list) ->
                WalletSummary(
                    walletName = walletName,
                    totalSpent = list.filter { it.type == TransactionType.DEBIT.name && !it.isSelfTransferEntry() && !it.isCardBillSettlement() }.sumOf { it.amount },
                    totalLoadedOrCredited = list.filter { it.type == TransactionType.CREDIT.name }.sumOf { it.amount },
                    transactionCount = list.size
                )
            }
            .sortedByDescending { it.totalSpent }

        val bankAccounts = instrumentSource
            .filter { it.paymentRail != PaymentRail.CREDIT_CARD.name && it.paymentRail != PaymentRail.WALLET.name }
            .groupBy { "${it.bankName}|${it.accountLast4}" }
            .map { (key, list) ->
                val first = list.first()
                val dominantRail = list
                    .groupingBy { PaymentRail.fromNameOrDefault(it.paymentRail) }
                    .eachCount()
                    .maxByOrNull { it.value }?.key ?: PaymentRail.UPI

                AccountSpendSummary(
                    key = key,
                    bankName = first.bankName,
                    accountLast4 = first.accountLast4,
                    primaryRail = dominantRail,
                    totalSpent = list.filter { it.type == TransactionType.DEBIT.name && !it.isSelfTransferEntry() && !it.isCardBillSettlement() }.sumOf { it.amount },
                    totalCredited = list.filter { it.type == TransactionType.CREDIT.name && !it.isSelfTransferEntry() && !it.isCardBillSettlement() }.sumOf { it.amount },
                    transactionCount = list.size
                )
            }
            .sortedByDescending { it.totalSpent }

        val earliestTxnYm = txnsByMonth.keys.minOrNull() ?: todayYm.minusMonths(11)
        val startYm = minOf(earliestTxnYm, todayYm.minusMonths(11), selectedYm)
        val endYm = maxOf(todayYm, selectedYm)
        val monthSpan = ChronoUnit.MONTHS.between(startYm, endYm).toInt().coerceIn(5, 36)

        val trendPoints = (0..monthSpan).map { step ->
            val ym = startYm.plusMonths(step.toLong())
            val spent = txnsByMonth[ym]
                .orEmpty()
                .filter {
                    it.type == TransactionType.DEBIT.name &&
                        !it.isSelfTransferEntry() &&
                        !it.isCardBillSettlement() &&
                        it.isIncludedInSpend
                }
                .sumOf { it.amount }
            val label = if (ym.year == todayYm.year) {
                ym.format(shortMonthFormatter).uppercase(Locale.ENGLISH)
            } else {
                ym.format(shortMonthYearFormatter).uppercase(Locale.ENGLISH)
            }
            MonthlyTrendPoint(
                yearMonthKey = ym.toString(),
                shortLabel = label,
                totalSpent = spent
            )
        }

        val aiSuggestions = buildDynamicAiSearchSuggestions(allTxns, aux.suggestionSeed)

        VaultUiState(
            selectedMonth = selectedYm,
            isCurrentMonthSelected = selectedYm == todayYm,
            selectedMonthLabel = selectedYm.format(monthTitleFormatter).uppercase(Locale.ENGLISH),
            monthTotalDebited = totalDebited,
            monthTotalCredited = totalCredited,
            monthSelfTransferTotal = selfTransferVolume,
            monthCardBillsPaidTotal = cardBillsPaid,
            monthDailyAverage = dailyAvg,
            monthTransactionCount = monthTxns.size,
            previousMonthDeltaPercent = momDelta,
            filteredTransactions = filtered,
            totalAllTimeCount = allTxns.size,
            hasDemoTransactions = allTxns.any { it.senderAddress.startsWith("DEMO-") },
            categoryBreakdowns = catBreakdowns,
            topMerchants = topMerchants,
            creditCardSummaries = creditCards,
            upiChannelSummaries = upiChannels,
            walletSummaries = wallets,
            bankAccountSummaries = bankAccounts,
            monthlyTrend = trendPoints,
            learnedMerchants = learnedList,
            aiSearchSuggestions = aiSuggestions,
            searchQuery = filters.query,
            selectedMoneyMatter = filters.moneyMatter,
            selectedCategory = filters.category,
            selectedAccountKey = filters.accountKey,
            selectedUpiApp = filters.upiApp,
            isSyncing = aux.isSyncing,
            aiTrainingStatus = aux.aiTrainingStatus,
            statusBanner = aux.statusBanner,
            undoDeletedTransaction = aux.undoDeletedTransaction,
            undoSecondsRemaining = aux.undoSecondsRemaining,
            sandboxResult = aux.sandboxResult
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultUiState(
            selectedMonthLabel = YearMonth.now().format(monthTitleFormatter).uppercase(Locale.ENGLISH),
            aiTrainingStatus = SmsInboxScanner.getTrainingStatsLabel(appContext)
        )
    )

    fun shiftMonth(delta: Long) {
        selectedMonthFlow.value = selectedMonthFlow.value.plusMonths(delta)
    }

    fun jumpToCurrentMonth() {
        selectedMonthFlow.value = YearMonth.now()
    }

    fun selectYearMonthKey(key: String) {
        runCatching { YearMonth.parse(key) }.onSuccess {
            selectedMonthFlow.value = it
        }
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun toggleMoneyMatterFilter(filter: MoneyMatterFilter) {
        selectedCategoryFlow.value = null
        selectedMoneyMatterFlow.value = if (selectedMoneyMatterFlow.value == filter) null else filter
    }

    fun toggleCategoryFilter(category: SpendCategory?) {
        selectedMoneyMatterFlow.value = null
        selectedCategoryFlow.value = if (category == null || selectedCategoryFlow.value == category) null else category
    }

    fun shuffleSearchSuggestions() {
        auxMetaFlow.value = auxMetaFlow.value.copy(suggestionSeed = System.nanoTime())
    }

    private fun buildDynamicAiSearchSuggestions(
        allTxns: List<TransactionEntity>,
        seed: Long
    ): List<String> {
        if (allTxns.isEmpty()) {
            return listOf("UPI", "Credit Card", "Bank", "Wallet")
        }

        val genericPlaceholders = setOf(
            "SELF ACCOUNT TRANSFER",
            "DIRECT SPEND",
            "BANK SPEND",
            "CREDIT CARD SPEND",
            "BANK CREDIT",
            "WALLET SPEND",
            "WALLET CREDIT",
            "ACCOUNT TRANSFER",
            "ATM CASH WITHDRAWAL",
            "CREDIT CARD BILL",
            "WALLET LOAD / TRANSFER"
        )

        val merchantCandidates = allTxns
            .asSequence()
            .map { it.merchantDisplayName.trim() }
            .filter { name ->
                name.length in 3..22 &&
                    !name.contains("••") &&
                    !name.startsWith("UPI ", ignoreCase = true) &&
                    !name.startsWith("Bank A/c", ignoreCase = true) &&
                    name.uppercase(Locale.US) !in genericPlaceholders
            }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(18)
            .map { it.key }

        val bankAndAppCandidates = buildList {
            allTxns.map { it.bankName.trim() }
                .filter { it.length in 3..16 && !it.equals("Bank", ignoreCase = true) }
                .distinct()
                .take(5)
                .let { addAll(it) }

            allTxns.map { it.upiApp.trim() }
                .filter { it.length in 3..14 && !it.equals("Bank UPI", ignoreCase = true) }
                .distinct()
                .take(4)
                .let { addAll(it) }
        }

        val noteCandidates = allTxns
            .map { it.note.trim() }
            .filter { it.length in 3..18 }
            .distinct()
            .take(5)

        val categoryCandidates = allTxns
            .map { SpendCategory.fromNameOrDefault(it.category).displayName }
            .filter { !it.equals("Others", ignoreCase = true) }
            .distinct()
            .take(6)

        val pool = (merchantCandidates + noteCandidates + bankAndAppCandidates + categoryCandidates)
            .distinctBy { it.lowercase(Locale.US) }

        if (pool.isEmpty()) {
            return listOf("UPI", "Credit Card", "Bank")
        }

        return pool.shuffled(Random(seed)).take(6)
    }

    fun toggleAccountFilter(accountKey: String?) {
        selectedUpiAppFlow.value = null
        selectedAccountFlow.value = if (selectedAccountFlow.value == accountKey) null else accountKey
    }

    fun toggleUpiAppFilter(upiApp: String?) {
        selectedAccountFlow.value = null
        selectedUpiAppFlow.value = if (selectedUpiAppFlow.value == upiApp) null else upiApp
    }

    fun clearDrilldownFilters() {
        selectedAccountFlow.value = null
        selectedUpiAppFlow.value = null
    }

    fun dismissBanner() {
        auxMetaFlow.value = auxMetaFlow.value.copy(statusBanner = null)
    }

    fun showStatusMessage(message: String) {
        auxMetaFlow.value = auxMetaFlow.value.copy(statusBanner = message)
    }

    fun syncDeviceSmsInbox() {
        if (auxMetaFlow.value.isSyncing) return
        viewModelScope.launch {
            auxMetaFlow.value = auxMetaFlow.value.copy(isSyncing = true)
            val result = SmsInboxScanner.scanInbox(appContext)
            val statsLabel = SmsInboxScanner.getTrainingStatsLabel(appContext)
            val msg = when {
                result.permissionMissing -> "Grant SMS permission to sync your transactions"
                result.newlyInserted > 0 -> "Synced ${result.newlyInserted} new transactions"
                else -> null
            }
            auxMetaFlow.value = auxMetaFlow.value.copy(
                isSyncing = false,
                aiTrainingStatus = statsLabel,
                statusBanner = msg ?: auxMetaFlow.value.statusBanner,
                suggestionSeed = if (result.newlyInserted > 0) System.nanoTime() else auxMetaFlow.value.suggestionSeed
            )
        }
    }

    fun retrainAiNow() {
        if (auxMetaFlow.value.isSyncing) return
        viewModelScope.launch {
            auxMetaFlow.value = auxMetaFlow.value.copy(
                isSyncing = true,
                statusBanner = "Retraining AI across your SMS history..."
            )
            val result = SmsInboxScanner.scanInbox(appContext)
            val statsLabel = SmsInboxScanner.getTrainingStatsLabel(appContext)
            val msg = if (result.permissionMissing) {
                "Grant SMS permission so AI can analyze your transactions"
            } else {
                "AI trained (${result.trainingPasses} passes): ${result.merchantsResolvedCount} categorized, ${result.cardBillsCount} Card Bills, ${result.selfTransfersCount} Self Transfers"
            }
            auxMetaFlow.value = auxMetaFlow.value.copy(
                isSyncing = false,
                aiTrainingStatus = statsLabel,
                statusBanner = msg
            )
        }
    }

    fun injectRealisticSampleSms() {
        viewModelScope.launch {
            auxMetaFlow.value = auxMetaFlow.value.copy(isSyncing = true)
            val result = SmsInboxScanner.injectDemoSmsDataset(appContext)
            val statsLabel = SmsInboxScanner.getTrainingStatsLabel(appContext)
            selectedMonthFlow.value = YearMonth.now()
            auxMetaFlow.value = auxMetaFlow.value.copy(
                isSyncing = false,
                aiTrainingStatus = statsLabel,
                statusBanner = "Added ${result.newlyInserted} sample transactions"
            )
        }
    }

    fun clearDemoSms() {
        viewModelScope.launch {
            dao.clearDemoTransactions()
            auxMetaFlow.value = auxMetaFlow.value.copy(
                statusBanner = "Cleared sample transactions"
            )
        }
    }

    fun autoSaveTransactionEdits(
        transaction: TransactionEntity,
        newDisplayName: String,
        newCategory: SpendCategory,
        newNote: String,
        isIncludedInSpend: Boolean,
        applyToAllMatchingSellerCode: Boolean
    ) {
        viewModelScope.launch {
            val cleanDisplay = newDisplayName.trim().ifBlank { transaction.merchantDisplayName }
            val cleanNote = newNote.trim()
            val isSelf = newCategory == SpendCategory.SELF_TRANSFER

            dao.updateSingleTransactionFull(
                transactionId = transaction.id,
                displayName = cleanDisplay,
                category = newCategory.name,
                note = cleanNote,
                isIncludedInSpend = isIncludedInSpend,
                isSelfTransfer = isSelf
            )

            val normalizedKey = MerchantKnowledgeBase.normalizeForLookup(transaction.merchantRawCode)
            val merchantOrCatChanged = cleanDisplay != transaction.merchantDisplayName || newCategory.name != transaction.category
            if (applyToAllMatchingSellerCode && merchantOrCatChanged && normalizedKey.length >= 3 &&
                normalizedKey !in setOf("UPI", "CARD", "BANK", "CREDITCARD", "DEBITCARD", "WALLET")
            ) {
                dao.upsertLearnedMerchant(
                    LearnedMerchantEntity(
                        normalizedKey = normalizedKey,
                        displayName = cleanDisplay,
                        category = newCategory.name
                    )
                )
                dao.updateAllOtherMatchingMerchantCode(
                    rawCode = transaction.merchantRawCode,
                    displayName = cleanDisplay,
                    category = newCategory.name,
                    isSelfTransfer = isSelf,
                    excludeId = transaction.id
                )
            }
            if (merchantOrCatChanged) {
                IterativeLedgerAnalyzer.analyzeAndRefineLedger(dao)
            }
        }
    }

    fun deleteTransactionWithUndo(transaction: TransactionEntity) {
        undoTimerJob?.cancel()
        viewModelScope.launch {
            dao.deleteTransaction(transaction.id)
            auxMetaFlow.value = auxMetaFlow.value.copy(
                undoDeletedTransaction = transaction,
                undoSecondsRemaining = 5
            )
            undoTimerJob = launch {
                for (sec in 4 downTo 1) {
                    delay(1_000L)
                    auxMetaFlow.value = auxMetaFlow.value.copy(undoSecondsRemaining = sec)
                }
                delay(1_000L)
                auxMetaFlow.value = auxMetaFlow.value.copy(
                    undoDeletedTransaction = null,
                    undoSecondsRemaining = 0
                )
            }
        }
    }

    fun undoRecentDelete() {
        val snapshot = auxMetaFlow.value.undoDeletedTransaction ?: return
        undoTimerJob?.cancel()
        viewModelScope.launch {
            dao.restoreTransaction(snapshot)
            auxMetaFlow.value = auxMetaFlow.value.copy(
                undoDeletedTransaction = null,
                undoSecondsRemaining = 0,
                statusBanner = "Restored ${snapshot.merchantDisplayName}"
            )
        }
    }

    fun testLiveSmsInSandbox(sender: String, rawSms: String, saveIfValid: Boolean = false) {
        viewModelScope.launch {
            val learnedMap = dao.getAllLearnedMerchants().associateBy {
                MerchantKnowledgeBase.normalizeForLookup(it.normalizedKey)
            }
            val outcome = OfflineExpenseAiEngine.parseSms(
                senderAddress = sender.ifBlank { "VM-HDFCBK" },
                body = rawSms,
                timestampMillis = System.currentTimeMillis(),
                learnedMap = learnedMap,
                context = appContext
            )
            if (saveIfValid && outcome is OfflineExpenseAiEngine.AiParseOutcome.Parsed) {
                dao.insertTransactionIgnoreDuplicate(outcome.transaction)
            }
            auxMetaFlow.value = auxMetaFlow.value.copy(sandboxResult = outcome)
        }
    }
}
