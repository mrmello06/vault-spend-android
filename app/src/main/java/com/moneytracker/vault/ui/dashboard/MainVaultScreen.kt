package com.moneytracker.vault.ui.dashboard

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.vault.ai.OfflineExpenseAiEngine
import com.moneytracker.vault.data.local.TransactionEntity
import com.moneytracker.vault.domain.model.PaymentRail
import com.moneytracker.vault.domain.model.SpendCategory
import com.moneytracker.vault.domain.model.TransactionType
import com.moneytracker.vault.ui.components.CategoryBadgeIcon
import com.moneytracker.vault.ui.components.CategoryDonutChart
import com.moneytracker.vault.ui.components.CredExplorePill
import com.moneytracker.vault.ui.components.CredSurfaceCard
import com.moneytracker.vault.ui.components.LuxuryInstrumentCard
import com.moneytracker.vault.ui.components.MonthlyTrendBarChart
import com.moneytracker.vault.ui.components.RollingOdometerAmount
import com.moneytracker.vault.ui.components.iconVector
import com.moneytracker.vault.ui.theme.CredSansFontFamily
import com.moneytracker.vault.ui.theme.VaultColors
import com.moneytracker.vault.ui.theme.VaultFormatters
import kotlinx.coroutines.launch

private enum class CredDockTab {
    HOME,
    CARDS,
    INSIGHTS
}

private enum class InstrumentSubTab(val label: String) {
    CREDIT_CARDS("CREDIT CARDS"),
    UPI("UPI"),
    WALLETS("WALLETS")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainVaultScreen(
    uiState: VaultUiState,
    hasSmsPermission: Boolean,
    hasSecurityQuestionsSet: Boolean,
    isAmoledDark: Boolean,
    refreshRateHz: Int = 60,
    isAppLockEnabled: Boolean = true,
    onToggleAppLock: (Boolean) -> Unit = {},
    onToggleAmoledTheme: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onSyncSms: () -> Unit,
    onLoadSampleSms: () -> Unit,
    onClearSampleSms: () -> Unit,
    onShiftMonth: (Long) -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    onSelectYearMonth: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onShuffleSuggestions: () -> Unit,
    onToggleMoneyMatterFilter: (MoneyMatterFilter) -> Unit,
    onToggleCategoryFilter: (SpendCategory?) -> Unit,
    onToggleAccountFilter: (String?) -> Unit,
    onToggleUpiAppFilter: (String?) -> Unit,
    onClearDrilldownFilters: () -> Unit,
    onDismissBanner: () -> Unit,
    onAutoSaveTransactionEdits: (TransactionEntity, String, SpendCategory, String, Boolean, Boolean) -> Unit,
    onDeleteTransactionWithUndo: (TransactionEntity) -> Unit,
    onUndoDelete: () -> Unit,
    onRunSandboxSms: (String, String, Boolean) -> Unit,
    onResetPasscodeClick: () -> Unit,
    onEditSecurityQuestionsClick: () -> Unit,
    onLockVaultNow: () -> Unit,
    onExitApp: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val pagerState = rememberPagerState(
        initialPage = CredDockTab.HOME.ordinal,
        pageCount = { CredDockTab.entries.size }
    )
    var instrumentSubTabIndex by rememberSaveable { mutableIntStateOf(InstrumentSubTab.CREDIT_CARDS.ordinal) }
    var inspectingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showProfileModal by rememberSaveable { mutableStateOf(false) }

    val homeListState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }

    // Derived boolean so MainVaultScreen NEVER recomposes mid-swipe!
    val isOnHomeTab by remember { derivedStateOf { pagerState.currentPage == CredDockTab.HOME.ordinal } }

    BackHandler(enabled = !isOnHomeTab) {
        scope.launch {
            pagerState.animateScrollToPage(
                page = CredDockTab.HOME.ordinal,
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            )
        }
    }
    BackHandler(enabled = isOnHomeTab) {
        onExitApp()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.AmoledBlack)
    ) {
        // =========================================================================
        // ADAPTIVE SHEET: PURE #000000 AMOLED BLACK (DEFAULT) OR CRISP #FFFFFF LIGHT
        // =========================================================================
        val sheetCorner = if (isAmoledDark) 0.dp else 24.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(bottomStart = sheetCorner, bottomEnd = sheetCorner))
                .background(VaultColors.CredWhite)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CredTopBar(
                    monthLabel = uiState.selectedMonthLabel,
                    isCurrentMonth = uiState.isCurrentMonthSelected,
                    isSyncing = uiState.isSyncing,
                    isAmoledDark = isAmoledDark,
                    isLandscape = isLandscape,
                    isAppLockEnabled = isAppLockEnabled,
                    onToggleTheme = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleAmoledTheme()
                    },
                    onPrevMonth = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onShiftMonth(-1)
                    },
                    onNextMonth = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onShiftMonth(1)
                    },
                    onJumpCurrentMonth = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onJumpToCurrentMonth()
                    },
                    onSearchIconClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = CredDockTab.HOME.ordinal,
                                animationSpec = tween(280, easing = FastOutSlowInEasing)
                            )
                            homeListState.animateScrollToItem(2)
                            runCatching { searchFocusRequester.requestFocus() }
                        }
                    },
                    onRefreshClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (hasSmsPermission) onSyncSms() else onRequestSmsPermission()
                    },
                    onAvatarLockClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showProfileModal = true
                    }
                )

                // Subtle Status Pill
                AnimatedVisibility(visible = uiState.statusBanner != null) {
                    uiState.statusBanner?.let { banner ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(VaultColors.CardSurface)
                                .border(1.dp, VaultColors.CredBorderStrong, RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = banner,
                                fontFamily = CredSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = VaultColors.InkPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Dismiss",
                                tint = VaultColors.InkSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onDismissBanner() }
                            )
                        }
                    }
                }

                // =========================================================================
                // FULL-SCREEN SWIPEABLE HORIZONTAL PAGER (3 PRIMARY TABS: HOME, CARDS, INSIGHTS)
                // =========================================================================
                HorizontalPager(
                    state = pagerState,
                    key = { page -> page },
                    modifier = Modifier.fillMaxSize(),
                    beyondBoundsPageCount = 1
                ) { pageIndex ->
                    when (CredDockTab.entries[pageIndex]) {
                        CredDockTab.HOME -> CredHomeSheetContent(
                            uiState = uiState,
                            isLandscape = isLandscape,
                            listState = homeListState,
                            searchFocusRequester = searchFocusRequester,
                            hasSmsPermission = hasSmsPermission,
                            onRequestSmsPermission = onRequestSmsPermission,
                            onLoadSampleSms = onLoadSampleSms,
                            onClearSampleSms = onClearSampleSms,
                            onJumpToCurrentMonth = onJumpToCurrentMonth,
                            onSearchQueryChange = onSearchQueryChange,
                            onShuffleSuggestions = onShuffleSuggestions,
                            onToggleMoneyMatterFilter = onToggleMoneyMatterFilter,
                            onToggleCategoryFilter = onToggleCategoryFilter,
                            onClearDrilldownFilters = onClearDrilldownFilters,
                            onOpenInsightsTab = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = CredDockTab.INSIGHTS.ordinal,
                                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                                    )
                                }
                            },
                            onTransactionClick = { inspectingTransaction = it }
                        )

                        CredDockTab.CARDS -> CardsUpiWalletsSheet(
                            uiState = uiState,
                            isLandscape = isLandscape,
                            selectedSubTabIndex = instrumentSubTabIndex,
                            onSelectSubTabIndex = { instrumentSubTabIndex = it },
                            onSelectAccount = { acctKey ->
                                onToggleAccountFilter(acctKey)
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = CredDockTab.HOME.ordinal,
                                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                                    )
                                }
                            },
                            onSelectUpiApp = { upiApp ->
                                onToggleUpiAppFilter(upiApp)
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = CredDockTab.HOME.ordinal,
                                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                        )

                        CredDockTab.INSIGHTS -> CredInsightsSheet(
                            uiState = uiState,
                            isLandscape = isLandscape,
                            onShiftMonth = onShiftMonth,
                            onJumpToCurrentMonth = onJumpToCurrentMonth,
                            onSelectYearMonth = onSelectYearMonth,
                            onCategoryClick = { cat ->
                                onToggleCategoryFilter(cat)
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = CredDockTab.HOME.ordinal,
                                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // =========================================================================
            // 5-SECOND UNDO DELETE PILL BAR (FLOATS ABOVE BOTTOM LIQUID GLASS DOCK)
            // =========================================================================
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.undoDeletedTransaction != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = if (isLandscape) 58.dp else 78.dp)
            ) {
                uiState.undoDeletedTransaction?.let { deleted ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF161822))
                            .border(1.dp, VaultColors.ChampagneGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "deleted ${deleted.merchantDisplayName.lowercase()}",
                                fontFamily = CredSansFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${VaultFormatters.formatInr(deleted.amount, true)} • undo within ${uiState.undoSecondsRemaining}s",
                                fontFamily = CredSansFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFA6AEBC)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .clickable { onUndoDelete() }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Undo,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "undo",
                                fontFamily = CredSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // FLOATING 3-TAB LIQUID GLASS BOTTOM NAVIGATION (CONTENT VISIBLE UNDERNEATH)
        // =========================================================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    horizontal = if (isLandscape) 40.dp else 18.dp,
                    vertical = if (isLandscape) 6.dp else 12.dp
                )
        ) {
            CredLiquidGlassBottomBar(
                pagerState = pagerState,
                isLandscape = isLandscape,
                onSelectTab = { nextTab ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = nextTab.ordinal,
                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                        )
                    }
                }
            )
        }
    }

    if (showProfileModal) {
        CredProfileModalSheet(
            uiState = uiState,
            isAmoledDark = isAmoledDark,
            isLandscape = isLandscape,
            refreshRateHz = refreshRateHz,
            isAppLockEnabled = isAppLockEnabled,
            hasSecurityQuestionsSet = hasSecurityQuestionsSet,
            onToggleAppLock = onToggleAppLock,
            onToggleAmoledTheme = onToggleAmoledTheme,
            onResetPasscodeClick = {
                showProfileModal = false
                onResetPasscodeClick()
            },
            onEditSecurityQuestionsClick = {
                showProfileModal = false
                onEditSecurityQuestionsClick()
            },
            onLockVaultNow = {
                showProfileModal = false
                onLockVaultNow()
            },
            onRunSandbox = onRunSandboxSms,
            onDismiss = { showProfileModal = false }
        )
    }

    inspectingTransaction?.let { txn ->
        val liveTxn = uiState.filteredTransactions.firstOrNull { it.id == txn.id } ?: txn
        TransactionDetailModal(
            transaction = liveTxn,
            onDismiss = { inspectingTransaction = null },
            onAutoSaveEdits = { newName, newCat, newNote, included, applyAll ->
                onAutoSaveTransactionEdits(liveTxn, newName, newCat, newNote, included, applyAll)
            },
            onDelete = {
                onDeleteTransactionWithUndo(liveTxn)
                inspectingTransaction = null
            }
        )
    }
}

/**
 * Clean Top Bar with:
 * - Left: Lock Avatar
 * - Center: < nov 2026 > capsule
 * - Right: Properly sized 34.dp circular buttons for AMOLED/Light Toggle, Search, and Refresh (never clipped!)
 */
@Composable
private fun CredTopBar(
    monthLabel: String,
    isCurrentMonth: Boolean,
    isSyncing: Boolean,
    isAmoledDark: Boolean,
    isLandscape: Boolean,
    isAppLockEnabled: Boolean = true,
    onToggleTheme: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpCurrentMonth: () -> Unit,
    onSearchIconClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onAvatarLockClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultColors.CredWhite)
            .statusBarsPadding()
            .padding(
                horizontal = if (isLandscape) 24.dp else 16.dp,
                vertical = if (isLandscape) 5.dp else 10.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Profile & Lock Avatar (Opens Profile Sheet)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onAvatarLockClick)
        ) {
            Box(
                modifier = Modifier
                    .size(37.dp)
                    .clip(CircleShape)
                    .background(VaultColors.CardSurface)
                    .border(1.5.dp, VaultColors.CredBorderStrong, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = "Profile & Settings",
                    tint = VaultColors.InkPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (isAppLockEnabled) VaultColors.InkPrimary else VaultColors.CreditGreen)
                    .border(1.dp, VaultColors.CredWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAppLockEnabled) Icons.Outlined.Lock else Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = if (isAppLockEnabled) VaultColors.OnInkPrimary else Color.Black,
                    modifier = Modifier.size(9.dp)
                )
            }
        }

        // Center: Month Selector Capsule
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .background(VaultColors.JackpotPinkBg)
                .border(1.dp, VaultColors.JackpotPinkBorder, RoundedCornerShape(50))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = VaultColors.InkPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPrevMonth)
                    .padding(2.dp)
            )
            AnimatedContent(
                targetState = monthLabel.lowercase(),
                transitionSpec = {
                    (fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 4 })
                        .togetherWith(fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 4 })
                },
                label = "monthLabelTransition"
            ) { label ->
                Text(
                    text = label,
                    fontFamily = CredSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultColors.InkPrimary,
                    modifier = Modifier
                        .clickable(onClick = onJumpCurrentMonth)
                        .padding(horizontal = 6.dp)
                )
            }
            AnimatedVisibility(visible = !isCurrentMonth) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(VaultColors.DebitRed)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = VaultColors.InkPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNextMonth)
                    .padding(2.dp)
            )
        }

        // Right: AMOLED/Light Mode Toggle + Search + Refresh (Properly Sized 34.dp Circles — Zero Clipping!)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Small AMOLED / Light Mode Toggle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(VaultColors.CredCanvasSubtle)
                    .border(1.dp, VaultColors.CredBorder, CircleShape)
                    .clickable(onClick = onToggleTheme),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = isAmoledDark,
                    animationSpec = tween(220),
                    label = "themeIconCrossfade"
                ) { dark ->
                    Icon(
                        imageVector = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = if (dark) "Switch to Light Mode" else "Switch to AMOLED Black",
                        tint = if (dark) VaultColors.ChampagneGold else VaultColors.InkPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // 2. Search Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(VaultColors.CredCanvasSubtle)
                    .border(1.dp, VaultColors.CredBorder, CircleShape)
                    .clickable(onClick = onSearchIconClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = VaultColors.InkPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // 3. Clean Refresh Button (Uncut & Centered)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(VaultColors.CredCanvasSubtle)
                    .border(1.dp, VaultColors.CredBorder, CircleShape)
                    .clickable(onClick = onRefreshClick),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = isSyncing,
                    animationSpec = tween(200),
                    label = "syncIconCrossfade"
                ) { syncing ->
                    if (syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = VaultColors.InkPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Sync SMS",
                            tint = VaultColors.InkPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating 3-Slot Liquid Glass Bottom Navigation Dock
 * Content is visible in the background underneath through the translucent liquid glass!
 * GPU RenderThread accelerated, zero recomposition, zero layout passes per frame at 60Hz / 120Hz / 144Hz.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CredLiquidGlassBottomBar(
    pagerState: PagerState,
    isLandscape: Boolean,
    onSelectTab: (CredDockTab) -> Unit
) {
    val tabCount = CredDockTab.entries.size

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 46.dp else 56.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.40f),
                spotColor = Color.Black.copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (VaultColors.isAmoledDark) Color(0xCC090B10) else Color(0xDCF8FAFC)
            )
            .border(
                width = 1.dp,
                brush = if (VaultColors.isAmoledDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.42f),
                            Color.White.copy(alpha = 0.10f),
                            Color(0xFF93C5FD).copy(alpha = 0.30f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x280F172A),
                            Color(0x120F172A),
                            Color(0x383B82F6)
                        )
                    )
                },
                shape = RoundedCornerShape(28.dp)
            )
            .padding(4.dp)
    ) {
        // =========================================================================
        // GPU-ACCELERATED LIQUID GLASS LENS (NO TOP SPECULAR LINE!)
        // =========================================================================
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val livePos = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            .coerceIn(0f, (tabCount - 1).toFloat())
                        translationX = size.width * livePos
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (VaultColors.isAmoledDark) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color(0xFFA5C9FF).copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.14f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x1E0F172A),
                                    Color(0x183B82F6),
                                    Color(0x120F172A)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.2.dp,
                        brush = if (VaultColors.isAmoledDark) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.65f),
                                    Color.White.copy(alpha = 0.14f),
                                    Color(0xFF93C5FD).copy(alpha = 0.45f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0x3D0F172A),
                                    Color(0x1A0F172A),
                                    Color(0x4D2563EB)
                                )
                            )
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
            )
            Spacer(modifier = Modifier.weight((tabCount - 1).toFloat()))
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CredSlimDockItem(
                tab = CredDockTab.HOME,
                pagerState = pagerState,
                isLandscape = isLandscape,
                activeIcon = Icons.Filled.Home,
                inactiveIcon = Icons.Outlined.Home,
                onClick = { onSelectTab(CredDockTab.HOME) },
                modifier = Modifier.weight(1f)
            )
            CredSlimDockItem(
                tab = CredDockTab.CARDS,
                pagerState = pagerState,
                isLandscape = isLandscape,
                activeIcon = Icons.Outlined.CreditCard,
                inactiveIcon = Icons.Outlined.CreditCard,
                onClick = { onSelectTab(CredDockTab.CARDS) },
                modifier = Modifier.weight(1f)
            )
            CredSlimDockItem(
                tab = CredDockTab.INSIGHTS,
                pagerState = pagerState,
                isLandscape = isLandscape,
                activeIcon = Icons.Outlined.Stars,
                inactiveIcon = Icons.Outlined.Stars,
                onClick = { onSelectTab(CredDockTab.INSIGHTS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CredSlimDockItem(
    tab: CredDockTab,
    pagerState: PagerState,
    isLandscape: Boolean,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected by remember(tab) {
        derivedStateOf { pagerState.currentPage == tab.ordinal }
    }

    val targetColor = when {
        selected && VaultColors.isAmoledDark -> Color.White
        selected && !VaultColors.isAmoledDark -> VaultColors.InkPrimary
        !selected && VaultColors.isAmoledDark -> Color(0xFF8A91A0)
        else -> Color(0xFF64748B)
    }
    val itemColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 200),
        label = "dockItemColor"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dockIconScale"
    )

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) activeIcon else inactiveIcon,
                contentDescription = tab.name,
                tint = itemColor,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tab.name,
                fontFamily = CredSansFontFamily,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                letterSpacing = 1.1.sp,
                color = itemColor
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) activeIcon else inactiveIcon,
                contentDescription = tab.name,
                tint = itemColor,
                modifier = Modifier
                    .size(21.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = tab.name,
                fontFamily = CredSansFontFamily,
                fontSize = 9.5.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                letterSpacing = 1.1.sp,
                color = itemColor
            )
        }
    }
}

// =========================================================================
// HOME TAB: HERO OUTFLOW + MONEY MATTERS + SEARCH + EXPLORE + TRANSACTIONS
// =========================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CredHomeSheetContent(
    uiState: VaultUiState,
    isLandscape: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    searchFocusRequester: FocusRequester,
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    onLoadSampleSms: () -> Unit,
    onClearSampleSms: () -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onShuffleSuggestions: () -> Unit,
    onToggleMoneyMatterFilter: (MoneyMatterFilter) -> Unit,
    onToggleCategoryFilter: (SpendCategory?) -> Unit,
    onClearDrilldownFilters: () -> Unit,
    onOpenInsightsTab: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val horizontalPad = if (isLandscape) 28.dp else 16.dp

    var searchTextFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = uiState.searchQuery,
                selection = TextRange(uiState.searchQuery.length)
            )
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.CredWhite),
        contentPadding = PaddingValues(start = horizontalPad, end = horizontalPad, top = 6.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. MONTHLY OUTFLOW HERO CARD (Uniform Sans-Serif Numbers, No Manual Train Button)
        item {
            CredSurfaceCard(
                cornerRadius = 16.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Warm Gold 3D Plaque
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        VaultColors.HeroGoldStart,
                                        VaultColors.HeroGoldMid,
                                        VaultColors.HeroGoldEnd
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .rotate(-7f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFE58F),
                                            Color(0xFFD99B26)
                                        )
                                    )
                                )
                                .border(1.dp, Color(0xFFFFF3C4), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹%",
                                fontFamily = CredSansFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFF6B3408)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Right: Uniform CredSansFontFamily Numbers Matching All Other Numbers!
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MONTHLY OUTFLOW",
                                fontFamily = CredSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.6.sp,
                                color = VaultColors.InkSecondary
                            )
                            if (!uiState.isCurrentMonthSelected) {
                                Text(
                                    text = "today →",
                                    fontFamily = CredSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VaultColors.ChampagneGold,
                                    modifier = Modifier.clickable { onJumpToCurrentMonth() }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "spent this month",
                            fontFamily = CredSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = VaultColors.InkSecondary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        RollingOdometerAmount(
                            amount = uiState.monthTotalDebited,
                            style = TextStyle(
                                fontFamily = CredSansFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 25.sp,
                                letterSpacing = (-0.4).sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = VaultColors.InkPrimary,
                            exactDecimals = true
                        )
                    }
                }
            }
        }

        // 2. MONEY MATTERS CAPSULE PILLS (Debited Excl. Self, Credited Excl. Self, Card Bills, Self Transfers)
        item {
            Column {
                Text(
                    text = "MONEY MATTERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultColors.InkSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isDebitedSel = uiState.selectedMoneyMatter == MoneyMatterFilter.DEBITED
                    val isCreditedSel = uiState.selectedMoneyMatter == MoneyMatterFilter.CREDITED
                    val isCardBillsSel = uiState.selectedMoneyMatter == MoneyMatterFilter.CARD_BILLS
                    val isSelfSel = uiState.selectedMoneyMatter == MoneyMatterFilter.SELF_TRANSFER

                    CredMoneyMatterCapsule(
                        title = "debited",
                        subtitle = "excl. self",
                        amount = uiState.monthTotalDebited,
                        prefix = "-",
                        amountColor = VaultColors.DebitRed,
                        selected = isDebitedSel,
                        bgColor = if (isDebitedSel) VaultColors.DebitRed.copy(alpha = 0.16f) else VaultColors.CardSurface,
                        borderColor = if (isDebitedSel) VaultColors.DebitRed else VaultColors.CredBorderStrong,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleMoneyMatterFilter(MoneyMatterFilter.DEBITED)
                        }
                    )

                    CredMoneyMatterCapsule(
                        title = "credited",
                        subtitle = "excl. self",
                        amount = uiState.monthTotalCredited,
                        prefix = "+",
                        amountColor = VaultColors.CreditGreen,
                        selected = isCreditedSel,
                        bgColor = if (isCreditedSel) VaultColors.CreditGreen.copy(alpha = 0.16f) else VaultColors.CardSurface,
                        borderColor = if (isCreditedSel) VaultColors.CreditGreen else VaultColors.CredBorderStrong,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleMoneyMatterFilter(MoneyMatterFilter.CREDITED)
                        }
                    )

                    CredMoneyMatterCapsule(
                        title = "card bills",
                        subtitle = "cred / cc",
                        amount = uiState.monthCardBillsPaidTotal,
                        prefix = "",
                        amountColor = VaultColors.CardBillBlue,
                        selected = isCardBillsSel,
                        bgColor = if (isCardBillsSel) VaultColors.CardBillBg else VaultColors.CardSurface,
                        borderColor = if (isCardBillsSel) VaultColors.CardBillBlue else VaultColors.CredBorderStrong,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleMoneyMatterFilter(MoneyMatterFilter.CARD_BILLS)
                        }
                    )

                    CredMoneyMatterCapsule(
                        title = "self transfer",
                        subtitle = "excluded",
                        amount = uiState.monthSelfTransferTotal,
                        prefix = "⇄ ",
                        amountColor = VaultColors.SelfYellowText,
                        selected = isSelfSel,
                        bgColor = if (isSelfSel) VaultColors.SelfYellowBg else VaultColors.CardSurface,
                        borderColor = if (isSelfSel) VaultColors.SelfYellowText else VaultColors.CredBorderStrong,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleMoneyMatterFilter(MoneyMatterFilter.SELF_TRANSFER)
                        }
                    )

                    if (uiState.monthSalaryTotal > 0.0 || uiState.selectedMoneyMatter == MoneyMatterFilter.SALARY) {
                        val isSalarySel = uiState.selectedMoneyMatter == MoneyMatterFilter.SALARY
                        CredMoneyMatterCapsule(
                            title = "salary",
                            subtitle = "payroll cr",
                            amount = uiState.monthSalaryTotal,
                            prefix = "+",
                            amountColor = Color(0xFF00E676),
                            selected = isSalarySel,
                            bgColor = if (isSalarySel) Color(0xFF00E676).copy(alpha = 0.20f) else VaultColors.CardSurface,
                            borderColor = if (isSalarySel) Color(0xFF00E676) else VaultColors.CredBorderStrong,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleMoneyMatterFilter(MoneyMatterFilter.SALARY)
                            }
                        )
                    }
                }
            }
        }

        // Permission or Demo prompt if inbox empty
        if (!hasSmsPermission || uiState.totalAllTimeCount == 0) {
            item {
                CredSurfaceCard(cornerRadius = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Sms,
                            contentDescription = null,
                            tint = VaultColors.InkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (!hasSmsPermission) "sync your sms inbox" else "no transactions yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = VaultColors.InkPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Grant read-only SMS permission to organize your cards, UPI apps, wallets, and merchants automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VaultColors.InkSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    CredPrimaryPillButton(
                        label = if (!hasSmsPermission) "scan sms inbox" else "load demo transactions",
                        onClick = if (!hasSmsPermission) onRequestSmsPermission else onLoadSampleSms
                    )
                }
            }
        } else if (uiState.hasDemoTransactions) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VaultColors.CredCanvasSubtle)
                        .border(1.dp, VaultColors.CredBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "sample transactions active",
                        fontFamily = CredSansFontFamily,
                        fontSize = 12.sp,
                        color = VaultColors.InkSecondary
                    )
                    Text(
                        text = "remove",
                        fontFamily = CredSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultColors.DebitRed,
                        modifier = Modifier.clickable { onClearSampleSms() }
                    )
                }
            }
        }

        // 3. STADIUM SEARCH BAR + QUICK SEARCH CHIPS + EXPLORE SPENDS PILLS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(VaultColors.CardSurface)
                        .border(1.dp, VaultColors.CredBorderStrong, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = VaultColors.InkMuted,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchTextFieldValue.text.isEmpty()) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            fontFamily = CredSansFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = VaultColors.InkSecondary
                                        )
                                    ) {
                                        append("search ")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            fontFamily = CredSansFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            color = VaultColors.InkMuted
                                        )
                                    ) {
                                        append("\"cred club, sms, notes...\"")
                                    }
                                },
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchTextFieldValue,
                            onValueChange = { newValue ->
                                searchTextFieldValue = newValue
                                if (newValue.text != uiState.searchQuery) {
                                    onSearchQueryChange(newValue.text)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                }
                            ),
                            textStyle = TextStyle(
                                fontFamily = CredSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = VaultColors.InkPrimary
                            ),
                            cursorBrush = SolidColor(VaultColors.InkPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                        )
                    }
                    if (searchTextFieldValue.text.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search",
                            tint = VaultColors.InkSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    searchTextFieldValue = TextFieldValue("")
                                    onSearchQueryChange("")
                                }
                        )
                    }
                }

                // Dynamic AI Search Suggestion Chips (randomly extracted from real SMS ledger!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onShuffleSuggestions()
                            }
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultColors.InkMuted
                        )
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = "Refresh AI suggestions",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    uiState.aiSearchSuggestions.forEach { term ->
                        val active = searchTextFieldValue.text.equals(term, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) VaultColors.InkPrimary else VaultColors.CredCanvasSubtle)
                                .border(
                                    1.dp,
                                    if (active) VaultColors.InkPrimary else VaultColors.CredBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    val nextQuery = if (active) "" else term
                                    searchTextFieldValue = TextFieldValue(nextQuery, selection = TextRange(nextQuery.length))
                                    onSearchQueryChange(nextQuery)
                                }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "\"${term.lowercase()}\"",
                                fontFamily = CredSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) VaultColors.OnInkPrimary else VaultColors.InkPrimary
                            )
                        }
                    }
                }

                // Active Account / UPI Drilldown Pill
                val drillLabel = uiState.selectedAccountKey?.replace("|", " ••")
                    ?: uiState.selectedUpiApp
                if (drillLabel != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(VaultColors.InkPrimary)
                            .clickable { onClearDrilldownFilters() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "filtered: ${drillLabel.lowercase()}  ✕",
                            fontFamily = CredSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VaultColors.OnInkPrimary
                        )
                    }
                }

                // Active Category Filter Indicator Pill
                val activeCat = uiState.selectedCategory
                if (activeCat != null) {
                    val count = uiState.filteredTransactions.size
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(activeCat.colorHex).copy(alpha = if (VaultColors.isAmoledDark) 0.26f else 0.16f))
                            .border(1.2.dp, Color(activeCat.colorHex), RoundedCornerShape(50))
                            .clickable { onToggleCategoryFilter(null) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(activeCat.colorHex))
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "category: ${activeCat.displayName.lowercase()} ($count spends)  ✕",
                            fontFamily = CredSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (VaultColors.isAmoledDark) Color.White else Color(activeCat.colorHex)
                        )
                    }
                }

                // EXPLORE SPENDS Category Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPLORE SPENDS",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultColors.InkSecondary
                    )
                    Text(
                        text = if (uiState.selectedCategory != null || uiState.selectedMoneyMatter != null) "show all >" else "insights >",
                        fontFamily = CredSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VaultColors.InkSecondary,
                        modifier = Modifier.clickable {
                            if (uiState.selectedCategory != null || uiState.selectedMoneyMatter != null) {
                                onToggleCategoryFilter(null)
                            } else {
                                onOpenInsightsTab()
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CredExplorePill(
                        label = "all spends",
                        accentColor = VaultColors.InkPrimary,
                        selected = uiState.selectedCategory == null && uiState.selectedMoneyMatter == null,
                        onClick = { onToggleCategoryFilter(null) }
                    )
                    SpendCategory.entries.forEach { cat ->
                        CredExplorePill(
                            label = cat.displayName,
                            icon = cat.iconVector(),
                            accentColor = if (cat == SpendCategory.SELF_TRANSFER) VaultColors.SelfYellowText else Color(cat.colorHex),
                            selected = uiState.selectedCategory == cat,
                            onClick = { onToggleCategoryFilter(cat) }
                        )
                    }
                }
            }
        }

        // 4. TRANSACTIONS FEED (PRECOMPUTED OFF-THREAD IN VIEWMODEL FOR 120Hz/144Hz SMOOTHNESS)
        if (uiState.dayGroups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) {
                            "no matching sms, notes, or transactions"
                        } else {
                            "no transactions in ${uiState.selectedMonthLabel.lowercase()}"
                        },
                        fontFamily = CredSansFontFamily,
                        fontSize = 14.sp,
                        color = VaultColors.InkMuted
                    )
                }
            }
        } else {
            uiState.dayGroups.forEach { group ->
                item(key = "day_${group.dayHeader}", contentType = "day_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.dayHeader,
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultColors.InkMuted
                        )
                        if (group.dayDebitTotal > 0) {
                            Text(
                                text = "-${VaultFormatters.formatInr(group.dayDebitTotal)}",
                                fontFamily = CredSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultColors.DebitRed
                            )
                        }
                    }
                }

                items(
                    items = group.transactions,
                    key = { it.id },
                    contentType = { "txn_row" }
                ) { txn ->
                    CredTransactionRowCard(
                        txn = txn,
                        onClick = { onTransactionClick(txn) },
                        modifier = Modifier.animateItemPlacement(tween(220))
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}

@Composable
private fun CredMoneyMatterCapsule(
    title: String,
    subtitle: String,
    amount: Double,
    prefix: String,
    amountColor: Color,
    selected: Boolean,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = bgColor,
        animationSpec = tween(200),
        label = "capsuleBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = borderColor,
        animationSpec = tween(200),
        label = "capsuleBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "capsuleScale"
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(50.dp)
            .clip(RoundedCornerShape(50))
            .background(animatedBg)
            .border(if (selected) 1.6.dp else 1.dp, animatedBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontFamily = CredSansFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VaultColors.InkPrimary
            )
            Text(
                text = subtitle,
                fontFamily = CredSansFontFamily,
                fontSize = 10.sp,
                color = VaultColors.InkMuted
            )
        }

        RollingOdometerAmount(
            amount = amount,
            prefix = prefix,
            style = TextStyle(
                fontFamily = CredSansFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                fontFeatureSettings = "tnum"
            ),
            color = amountColor,
            exactDecimals = false
        )

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = VaultColors.InkMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun CredTransactionRowCard(
    txn: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isSelf = txn.isSelfTransfer || txn.category == SpendCategory.SELF_TRANSFER.name
    val isCardBill = !isSelf && txn.category == SpendCategory.CREDIT_CARD_BILL.name
    val isSalary = !isSelf && !isCardBill && txn.category == SpendCategory.SALARY.name
    val category = when {
        isSelf -> SpendCategory.SELF_TRANSFER
        isCardBill -> SpendCategory.CREDIT_CARD_BILL
        isSalary -> SpendCategory.SALARY
        else -> SpendCategory.fromNameOrDefault(txn.category)
    }
    val isCredit = txn.type == TransactionType.CREDIT.name
    val isExcluded = !txn.isIncludedInSpend && !isSelf && !isCardBill
    val rail = PaymentRail.fromNameOrDefault(txn.paymentRail)

    val rowBg = when {
        isExcluded -> VaultColors.BlackedOutCard
        isSelf -> VaultColors.SelfYellowBg
        isCardBill -> VaultColors.CardBillBg
        isSalary -> if (VaultColors.isAmoledDark) Color(0xFF042010) else Color(0xFFE8F8F0)
        else -> VaultColors.CardSurface
    }

    val rowBorder = when {
        isExcluded -> Color(0xFF1A1C24)
        isSelf -> VaultColors.SelfYellowBorder
        isCardBill -> VaultColors.CardBillBlue.copy(alpha = 0.45f)
        isSalary -> Color(0xFF00E676)
        else -> VaultColors.CredBorder
    }

    val titleColor = when {
        isExcluded -> Color(0xFF848C9E)
        isSelf -> VaultColors.SelfYellowText
        isSalary -> if (VaultColors.isAmoledDark) Color(0xFF00E676) else Color(0xFF008744)
        else -> VaultColors.InkPrimary
    }

    val amountColor = when {
        isExcluded -> Color(0xFF848C9E)
        isSelf -> VaultColors.SelfYellowText
        isCardBill -> VaultColors.CardBillBlue
        isSalary -> Color(0xFF00E676)
        isCredit -> VaultColors.CreditGreen
        else -> VaultColors.DebitRed
    }

    val amountPrefix = when {
        isSelf -> "⇄ "
        isCardBill -> ""
        isCredit || isSalary -> "+"
        else -> "-"
    }

    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!VaultColors.isAmoledDark && !isExcluded) {
                    Modifier.shadow(1.5.dp, shape, ambientColor = Color.Black.copy(alpha = 0.04f))
                } else Modifier
            )
            .clip(shape)
            .background(rowBg)
            .border(1.dp, rowBorder, shape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .alpha(if (isExcluded) (if (VaultColors.isAmoledDark) 0.48f else 0.88f) else 1f)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadgeIcon(category = category, size = 42.dp)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = txn.merchantDisplayName,
                    fontFamily = CredSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (isExcluded) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "EXCLUDED",
                            fontFamily = CredSansFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                } else if (isSelf) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SELF",
                        fontFamily = CredSansFontFamily,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.7.sp,
                        color = if (VaultColors.isAmoledDark) Color.Black else VaultColors.SelfYellowText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (VaultColors.isAmoledDark) VaultColors.SelfYellowText
                                else VaultColors.SelfYellowBorder.copy(alpha = 0.35f)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                } else if (isSalary) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SALARY",
                        fontFamily = CredSansFontFamily,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF00E676))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                } else if (isCardBill) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CC BILL",
                        fontFamily = CredSansFontFamily,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.7.sp,
                        color = if (VaultColors.isAmoledDark) Color.Black else Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(VaultColors.CardBillBlue)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                } else if (txn.isSubscription) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SUB",
                        fontFamily = CredSansFontFamily,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VaultColors.RoyalViolet,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(VaultColors.RoyalViolet.copy(alpha = 0.16f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = buildString {
                    append(txn.bankName)
                    if (txn.accountLast4 != "0000") append(" ••${txn.accountLast4}")
                    append(" • ")
                    append(if (txn.upiApp.isNotBlank()) txn.upiApp else rail.label)
                },
                fontFamily = CredSansFontFamily,
                fontSize = 11.sp,
                color = if (isExcluded) Color(0xFF6B7280) else VaultColors.InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (txn.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isExcluded) Color.White.copy(alpha = 0.08f)
                            else VaultColors.CredMutedFill
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Notes,
                        contentDescription = null,
                        tint = if (isExcluded) Color(0xFF9CA3AF) else VaultColors.InkPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = txn.note,
                        fontFamily = CredSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isExcluded) Color(0xFFD1D5DB) else VaultColors.InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix${VaultFormatters.formatInr(txn.amount, true)}",
                fontFamily = CredSansFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = amountColor,
                textDecoration = if (isExcluded) TextDecoration.LineThrough else TextDecoration.None
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = category.displayName.lowercase(),
                fontFamily = CredSansFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isExcluded -> Color(0xFF6B7280)
                    isSelf -> VaultColors.SelfYellowText
                    isSalary -> Color(0xFF00E676)
                    else -> VaultColors.InkSecondary
                }
            )
        }
    }
}

// =========================================================================
// CARDS TAB: 3-COLUMN SELECTOR (CREDIT CARDS | UPI | WALLETS) + DIRECTIONAL SLIDE
// =========================================================================
@Composable
private fun CardsUpiWalletsSheet(
    uiState: VaultUiState,
    isLandscape: Boolean,
    selectedSubTabIndex: Int,
    onSelectSubTabIndex: (Int) -> Unit,
    onSelectAccount: (String) -> Unit,
    onSelectUpiApp: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val horizontalPad = if (isLandscape) 28.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.CredWhite)
    ) {
        // 3-Column Pill Selector: CREDIT CARDS | UPI | WALLETS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(VaultColors.CredCanvasSubtle)
                .border(1.dp, VaultColors.CredBorder, RoundedCornerShape(50))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            InstrumentSubTab.entries.forEachIndexed { idx, sub ->
                val isSelected = idx == selectedSubTabIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) VaultColors.InkPrimary else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectSubTabIndex(idx)
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sub.label.lowercase(),
                        fontFamily = CredSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) VaultColors.OnInkPrimary else VaultColors.InkSecondary
                    )
                }
            }
        }

        Crossfade(
            targetState = selectedSubTabIndex,
            animationSpec = tween(160),
            label = "cardsSubTabCrossfade"
        ) { subIdx ->
            val activeSub = InstrumentSubTab.entries[subIdx]
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = horizontalPad, end = horizontalPad, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (activeSub) {
                    InstrumentSubTab.CREDIT_CARDS -> {
                        val ccTotal = uiState.creditCardSummaries.sumOf { it.totalSpent }
                        item {
                            InstrumentTotalSummaryCard(
                                label = "CREDIT CARD SPENDS (${uiState.selectedMonthLabel})",
                                amount = ccTotal,
                                count = uiState.creditCardSummaries.sumOf { it.transactionCount }
                            )
                        }
                        if (uiState.creditCardSummaries.isEmpty()) {
                            item { EmptyInstrumentNotice("No credit card spends in ${uiState.selectedMonthLabel.lowercase()}") }
                        } else {
                            items(uiState.creditCardSummaries, key = { "cc_${it.key}" }) { card ->
                                LuxuryInstrumentCard(
                                    account = card,
                                    isSelected = uiState.selectedAccountKey == card.key,
                                    onClick = { onSelectAccount(card.key) }
                                )
                            }
                        }
                    }

                    InstrumentSubTab.UPI -> {
                        val upiTotal = uiState.upiChannelSummaries.sumOf { it.totalSpent }
                        item {
                            InstrumentTotalSummaryCard(
                                label = "UPI SPENDS (${uiState.selectedMonthLabel})",
                                amount = upiTotal,
                                count = uiState.upiChannelSummaries.sumOf { it.transactionCount }
                            )
                        }
                        if (uiState.upiChannelSummaries.isEmpty()) {
                            item { EmptyInstrumentNotice("No UPI transactions in ${uiState.selectedMonthLabel.lowercase()}") }
                        } else {
                            items(uiState.upiChannelSummaries, key = { "upi_${it.appName}" }) { upi ->
                                CredSurfaceCard(
                                    onClick = { onSelectUpiApp(upi.appName) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(VaultColors.CredCanvasSubtle)
                                                    .border(1.dp, VaultColors.CredBorder, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.QrCode2,
                                                    contentDescription = null,
                                                    tint = VaultColors.CreditGreen,
                                                    modifier = Modifier.size(21.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = upi.appName,
                                                    fontFamily = CredSansFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = VaultColors.InkPrimary
                                                )
                                                Text(
                                                    text = "${upi.transactionCount} txns • ${upi.linkedBanksLabel}",
                                                    fontFamily = CredSansFontFamily,
                                                    fontSize = 11.sp,
                                                    color = VaultColors.InkSecondary
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "-${VaultFormatters.formatInr(upi.totalSpent, true)}",
                                                fontFamily = CredSansFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = VaultColors.DebitRed
                                            )
                                            if (upi.totalCredited > 0) {
                                                Text(
                                                    text = "+${VaultFormatters.formatInr(upi.totalCredited)} cr",
                                                    fontFamily = CredSansFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = VaultColors.CreditGreen
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { (upi.sharePercentage / 100f).coerceIn(0.03f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = VaultColors.CreditGreen,
                                        trackColor = VaultColors.CredMutedFill,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }

                    InstrumentSubTab.WALLETS -> {
                        val walletTotal = uiState.walletSummaries.sumOf { it.totalSpent }
                        item {
                            InstrumentTotalSummaryCard(
                                label = "WALLET SPENDS (${uiState.selectedMonthLabel})",
                                amount = walletTotal,
                                count = uiState.walletSummaries.sumOf { it.transactionCount }
                            )
                        }
                        if (uiState.walletSummaries.isEmpty()) {
                            item { EmptyInstrumentNotice("No wallet transactions in ${uiState.selectedMonthLabel.lowercase()}") }
                        } else {
                            items(uiState.walletSummaries, key = { "wal_${it.walletName}" }) { wallet ->
                                CredSurfaceCard(
                                    onClick = { onSelectAccount("${wallet.walletName}|0000") }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(VaultColors.CredCanvasSubtle)
                                                    .border(1.dp, VaultColors.CredBorder, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                                    contentDescription = null,
                                                    tint = VaultColors.RoyalViolet,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = wallet.walletName,
                                                    fontFamily = CredSansFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = VaultColors.InkPrimary
                                                )
                                                Text(
                                                    text = "${wallet.transactionCount} transactions",
                                                    fontFamily = CredSansFontFamily,
                                                    fontSize = 11.sp,
                                                    color = VaultColors.InkSecondary
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "-${VaultFormatters.formatInr(wallet.totalSpent, true)}",
                                                fontFamily = CredSansFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = VaultColors.DebitRed
                                            )
                                            if (wallet.totalLoadedOrCredited > 0) {
                                                Text(
                                                    text = "+${VaultFormatters.formatInr(wallet.totalLoadedOrCredited)} loaded",
                                                    fontFamily = CredSansFontFamily,
                                                    fontSize = 11.sp,
                                                    color = VaultColors.CreditGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun InstrumentTotalSummaryCard(
    label: String,
    amount: Double,
    count: Int
) {
    CredSurfaceCard(cornerRadius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultColors.InkSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                RollingOdometerAmount(
                    amount = amount,
                    style = TextStyle(
                        fontFamily = CredSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 23.sp,
                        letterSpacing = (-0.3).sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = VaultColors.InkPrimary,
                    exactDecimals = true
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(VaultColors.CredMutedFill)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$count spends",
                    fontFamily = CredSansFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultColors.InkPrimary
                )
            }
        }
    }
}

@Composable
private fun EmptyInstrumentNotice(message: String) {
    CredSurfaceCard {
        Text(
            text = message,
            fontFamily = CredSansFontFamily,
            fontSize = 13.sp,
            color = VaultColors.InkSecondary
        )
    }
}

// =========================================================================
// INSIGHTS TAB: (1) CATEGORY BREAKDOWN -> (2) SPEND TREND -> (3) TOP MERCHANTS
// =========================================================================
@Composable
private fun CredInsightsSheet(
    uiState: VaultUiState,
    isLandscape: Boolean,
    onShiftMonth: (Long) -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    onSelectYearMonth: (String) -> Unit,
    onCategoryClick: (SpendCategory) -> Unit
) {
    val horizontalPad = if (isLandscape) 28.dp else 16.dp
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.CredWhite),
        contentPadding = PaddingValues(start = horizontalPad, end = horizontalPad, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. CATEGORY BREAKDOWN ON TOP!
        item {
            CredSurfaceCard(cornerRadius = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "category breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        color = VaultColors.InkPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = "Prev",
                            tint = VaultColors.InkPrimary,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { onShiftMonth(-1) }
                        )
                        Text(
                            text = uiState.selectedMonthLabel.lowercase(),
                            fontFamily = CredSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultColors.InkPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = VaultColors.InkPrimary,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { onShiftMonth(1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryDonutChart(
                        items = uiState.categoryBreakdowns,
                        totalSpent = uiState.monthTotalDebited
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.categoryBreakdowns.isEmpty()) {
                    Text(
                        text = "no spends recorded in ${uiState.selectedMonthLabel.lowercase()}",
                        fontFamily = CredSansFontFamily,
                        fontSize = 13.sp,
                        color = VaultColors.InkMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.categoryBreakdowns.forEach { item ->
                            val catColor = Color(item.category.colorHex)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCategoryClick(item.category) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryBadgeIcon(category = item.category, size = 36.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = item.category.displayName.lowercase(),
                                                fontFamily = CredSansFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = VaultColors.InkPrimary
                                            )
                                            Text(
                                                text = "${item.transactionCount} spends • ${"%.1f".format(item.percentage)}%",
                                                fontFamily = CredSansFontFamily,
                                                fontSize = 11.sp,
                                                color = VaultColors.InkSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "-${VaultFormatters.formatInr(item.totalAmount)}",
                                        fontFamily = CredSansFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = VaultColors.DebitRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (item.percentage / 100f).coerceIn(0.02f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = catColor,
                                    trackColor = VaultColors.CredMutedFill,
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. MONTHLY SPEND TREND NEXT!
        item {
            CredSurfaceCard(cornerRadius = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "monthly spend trend",
                            style = MaterialTheme.typography.titleLarge,
                            color = VaultColors.InkPrimary
                        )
                        Text(
                            text = "scroll horizontally & tap any month",
                            fontFamily = CredSansFontFamily,
                            fontSize = 12.sp,
                            color = VaultColors.InkSecondary
                        )
                    }

                    if (!uiState.isCurrentMonthSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(VaultColors.InkPrimary)
                                .clickable { onJumpToCurrentMonth() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "current month",
                                fontFamily = CredSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultColors.OnInkPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                MonthlyTrendBarChart(
                    points = uiState.monthlyTrend,
                    selectedKey = uiState.selectedMonth.toString(),
                    onSelectMonth = onSelectYearMonth
                )
            }
        }

        // 3. TOP MERCHANTS
        if (uiState.topMerchants.isNotEmpty()) {
            item {
                CredSurfaceCard(cornerRadius = 16.dp) {
                    Text(
                        text = "top merchants",
                        style = MaterialTheme.typography.titleLarge,
                        color = VaultColors.InkPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.topMerchants.take(10).forEachIndexed { idx, merchant ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "%02d".format(idx + 1),
                                        fontFamily = CredSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VaultColors.InkMuted,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = merchant.merchantDisplayName,
                                            fontFamily = CredSansFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = VaultColors.InkPrimary
                                        )
                                        Text(
                                            text = "${merchant.count} spends • ${merchant.category.displayName.lowercase()}",
                                            fontFamily = CredSansFontFamily,
                                            fontSize = 11.sp,
                                            color = VaultColors.InkSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = "-${VaultFormatters.formatInr(merchant.totalAmount)}",
                                    fontFamily = CredSansFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = VaultColors.DebitRed
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}

// =========================================================================
// PROFILE & SECURITY BOTTOM SHEET (ALL MORE OPTIONS + APP LOCK TOGGLE)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CredProfileModalSheet(
    uiState: VaultUiState,
    isAmoledDark: Boolean,
    isLandscape: Boolean,
    refreshRateHz: Int,
    isAppLockEnabled: Boolean,
    hasSecurityQuestionsSet: Boolean,
    onToggleAppLock: (Boolean) -> Unit,
    onToggleAmoledTheme: () -> Unit,
    onResetPasscodeClick: () -> Unit,
    onEditSecurityQuestionsClick: () -> Unit,
    onLockVaultNow: () -> Unit,
    onRunSandbox: (String, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }

    fun animateDismiss(afterDismiss: () -> Unit = onDismiss) {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                afterDismiss()
            }
        }
    }

    BackHandler {
        animateDismiss()
    }

    var gestureStartedAtTop by remember { mutableStateOf(true) }

    // Intercept downward drag and flings when list is scrolled down so user can smoothly scroll back up without closing the sheet!
    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    if (!isAtTop) {
                        gestureStartedAtTop = false
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If the gesture started while scrolled down, consume ALL excess downward scroll
                // so the modal sheet NEVER gets pulled down during content scrolling!
                return if (!gestureStartedAtTop && available.y > 0f) {
                    available
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (available.y > 0f) {
                    val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    if (!isAtTop) {
                        gestureStartedAtTop = false
                    }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val wasAtTop = gestureStartedAtTop
                // Reset for next gesture
                gestureStartedAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

                // If the fling started while scrolled down, consume ALL remaining downward fling velocity!
                // This prevents forceful downward swipes from slamming the bottom sheet closed!
                return if (!wasAtTop && available.y > 0f) {
                    available
                } else {
                    Velocity.Zero
                }
            }
        }
    }

    var senderDraft by remember { mutableStateOf("AD-HDFCBK") }
    var smsDraft by remember {
        mutableStateOf(
            "Sent Rs.5944.00 From HDFC Bank A/C *4821 To CRED Club On 05/11/26 Ref 12340936"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = 8.dp),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = VaultColors.CredWhite,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(VaultColors.CredBorderStrong)
            )
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
                .pointerInput(listState) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press) {
                                gestureStartedAtTop = (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0)
                            }
                        }
                    }
                }
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = if (isLandscape) 28.dp else 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile & Identity Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(VaultColors.CardSurface)
                            .border(1.5.dp, VaultColors.ChampagneGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = null,
                            tint = VaultColors.ChampagneGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RAIZEN VAULT",
                            fontFamily = CredSansFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 1.2.sp,
                            color = VaultColors.InkPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "100% Offline • Zero Trackers • Hardware AES",
                            fontFamily = CredSansFontFamily,
                            fontSize = 11.sp,
                            color = VaultColors.CreditGreen
                        )
                    }
                    IconButton(onClick = { animateDismiss() }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = VaultColors.InkSecondary
                        )
                    }
                }
            }

            // 1. Passcode & Security (With Optional App Lock Toggle!)
            item {
                CredSurfaceCard(cornerRadius = 16.dp) {
                    Text(
                        text = "passcode & security",
                        style = MaterialTheme.typography.titleLarge,
                        color = VaultColors.InkPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Passcode & biometric lock can be toggled optional anytime.",
                        fontFamily = CredSansFontFamily,
                        fontSize = 12.sp,
                        color = VaultColors.InkSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional App Lock Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VaultColors.CredCanvasSubtle)
                            .border(1.dp, VaultColors.CredBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "require passcode to open",
                                fontFamily = CredSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = VaultColors.InkPrimary
                            )
                            Text(
                                text = if (isAppLockEnabled) "Enabled: vault locked on launch" else "Disabled: opens without passcode",
                                fontFamily = CredSansFontFamily,
                                fontSize = 11.sp,
                                color = if (isAppLockEnabled) VaultColors.CreditGreen else VaultColors.DebitRed
                            )
                        }
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = onToggleAppLock,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = VaultColors.CreditGreen,
                                uncheckedThumbColor = Color(0xFFA6AEBC),
                                uncheckedTrackColor = VaultColors.CardSurface
                            )
                        )
                    }

                    if (isAppLockEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        CredPrimaryPillButton(
                            label = "lock app now",
                            onClick = { animateDismiss(onLockVaultNow) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CredSecondaryPillButton(
                            label = "change passcode",
                            onClick = { animateDismiss(onResetPasscodeClick) },
                            modifier = Modifier.weight(1f)
                        )
                        CredSecondaryPillButton(
                            label = if (hasSecurityQuestionsSet) "3 questions ✓" else "setup questions",
                            onClick = { animateDismiss(onEditSecurityQuestionsClick) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Display & Motion
            item {
                CredSurfaceCard(cornerRadius = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "display & motion",
                                style = MaterialTheme.typography.titleLarge,
                                color = VaultColors.InkPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${if (isAmoledDark) "AMOLED Black" else "Light Mode"} • Adaptive ${refreshRateHz}Hz",
                                fontFamily = CredSansFontFamily,
                                fontSize = 12.sp,
                                color = VaultColors.InkSecondary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(VaultColors.InkPrimary)
                                .clickable(onClick = onToggleAmoledTheme)
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAmoledDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = null,
                                tint = VaultColors.OnInkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAmoledDark) "light" else "amoled",
                                fontFamily = CredSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultColors.OnInkPrimary
                            )
                        }
                    }
                }
            }

            // 3. Live SMS Sandbox
            item {
                CredSurfaceCard(cornerRadius = 16.dp) {
                    Text(
                        text = "test any sms",
                        style = MaterialTheme.typography.titleLarge,
                        color = VaultColors.InkPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap a preset or paste any bank, card, UPI, or salary SMS to verify classification.",
                        fontFamily = CredSansFontFamily,
                        fontSize = 12.sp,
                        color = VaultColors.InkSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetSmsChip("cred club bill") {
                            senderDraft = "AD-HDFCBK"
                            smsDraft = "Sent Rs.5944.00 From HDFC Bank A/C *4821 To CRED Club On 05/11/26 Ref 12340936"
                            onRunSandbox(senderDraft, smsDraft, false)
                        }
                        PresetSmsChip("salary credit") {
                            senderDraft = "AD-HDFCBK"
                            smsDraft = "A/c XX4821 credited with Rs 65,000.00 on 31-Oct-26 by NEFT-ACME CORP-SALARY FOR OCT. Ref: HDFCN12345"
                            onRunSandbox(senderDraft, smsDraft, false)
                        }
                        PresetSmsChip("irctc rail") {
                            senderDraft = "AD-HDFCBK"
                            smsDraft = "Sent Rs.745.00 From HDFC Bank A/C *4821 To BDPG2 IR On 14/11/26 Ref 998812"
                            onRunSandbox(senderDraft, smsDraft, false)
                        }
                        PresetSmsChip("self transfer") {
                            senderDraft = "AD-HDFCBK"
                            smsDraft = "Rs. 15,000.00 debited from A/c XX4821 and credited to A/c XX9115 via Self Transfer."
                            onRunSandbox(senderDraft, smsDraft, false)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = smsDraft,
                        onValueChange = { smsDraft = it },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultColors.InkPrimary,
                            unfocusedBorderColor = VaultColors.CredBorderStrong,
                            focusedContainerColor = VaultColors.CredCanvasSubtle,
                            unfocusedContainerColor = VaultColors.CredCanvasSubtle,
                            focusedTextColor = VaultColors.InkPrimary,
                            unfocusedTextColor = VaultColors.InkPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CredPrimaryPillButton(
                            label = "test parse",
                            onClick = { onRunSandbox(senderDraft, smsDraft, false) },
                            modifier = Modifier.weight(1f)
                        )
                        CredSecondaryPillButton(
                            label = "add to vault",
                            onClick = { onRunSandbox(senderDraft, smsDraft, true) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    uiState.sandboxResult?.let { outcome ->
                        Spacer(modifier = Modifier.height(14.dp))
                        SandboxOutcomeView(outcome = outcome)
                    }
                }
            }

            // 4. Custom Learned Rules
            item {
                CredSurfaceCard(cornerRadius = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "custom merchant rules (${uiState.learnedMerchants.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = VaultColors.InkPrimary
                        )
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = VaultColors.CreditGreen,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.learnedMerchants.isEmpty()) {
                        Text(
                            text = "Tap any transaction to customize a merchant or category — your learned rules appear here.",
                            fontFamily = CredSansFontFamily,
                            fontSize = 12.sp,
                            color = VaultColors.InkSecondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.learnedMerchants.forEach { rule ->
                                val cat = SpendCategory.fromNameOrDefault(rule.category)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = rule.displayName,
                                            fontFamily = CredSansFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = VaultColors.InkPrimary
                                        )
                                        Text(
                                            text = "code: ${rule.normalizedKey}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = VaultColors.InkMuted
                                        )
                                    }
                                    Text(
                                        text = cat.displayName.lowercase(),
                                        fontFamily = CredSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(cat.colorHex)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CredPrimaryPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp)
            .clip(RoundedCornerShape(50))
            .background(VaultColors.InkPrimary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = CredSansFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = VaultColors.OnInkPrimary
        )
    }
}

@Composable
private fun CredSecondaryPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(45.dp)
            .clip(RoundedCornerShape(50))
            .background(VaultColors.CardSurface)
            .border(1.dp, VaultColors.CredBorderStrong, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = CredSansFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = VaultColors.InkPrimary
        )
    }
}

@Composable
private fun PresetSmsChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(VaultColors.CredCanvasSubtle)
            .border(1.dp, VaultColors.CredBorderStrong, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontFamily = CredSansFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = VaultColors.InkPrimary
        )
    }
}

@Composable
private fun SandboxOutcomeView(outcome: OfflineExpenseAiEngine.AiParseOutcome) {
    when (outcome) {
        is OfflineExpenseAiEngine.AiParseOutcome.BlockedOtp -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VaultColors.SelfYellowBg)
                    .border(1.dp, VaultColors.SelfYellowBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "OTP / PROMO BLOCKED",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultColors.SelfYellowText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = outcome.reason,
                    fontFamily = CredSansFontFamily,
                    fontSize = 13.sp,
                    color = VaultColors.InkPrimary
                )
            }
        }

        is OfflineExpenseAiEngine.AiParseOutcome.IgnoredMessage -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VaultColors.CredCanvasSubtle)
                    .border(1.dp, VaultColors.CredBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "IGNORED NON-SPEND MESSAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultColors.InkSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = outcome.reason,
                    fontFamily = CredSansFontFamily,
                    fontSize = 13.sp,
                    color = VaultColors.InkPrimary
                )
            }
        }

        is OfflineExpenseAiEngine.AiParseOutcome.Parsed -> {
            val t = outcome.transaction
            val cat = SpendCategory.fromNameOrDefault(t.category)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VaultColors.CredCanvasSubtle)
                    .border(1.dp, VaultColors.CreditGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "AI PARSED RESULT",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultColors.CreditGreen
                )
                Text(
                    text = "${t.merchantDisplayName}  •  ${VaultFormatters.formatInr(t.amount, true)} (${t.type})",
                    fontFamily = CredSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = VaultColors.InkPrimary
                )
                Text(
                    text = "Category: ${cat.displayName}  •  Self Transfer: ${if (t.isSelfTransfer) "YES" else "NO"}",
                    fontFamily = CredSansFontFamily,
                    fontSize = 12.sp,
                    color = VaultColors.InkSecondary
                )
                Text(
                    text = "Instrument: ${t.bankName} ••${t.accountLast4} (${t.paymentRail})",
                    fontFamily = CredSansFontFamily,
                    fontSize = 12.sp,
                    color = VaultColors.InkSecondary
                )
            }
        }
    }
}
