package com.moneytracker.vault

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneytracker.vault.security.VaultAuthManager
import com.moneytracker.vault.ui.auth.AuthVaultScreen
import com.moneytracker.vault.ui.auth.SecurityQuestionsFormScreen
import com.moneytracker.vault.ui.dashboard.MainVaultScreen
import com.moneytracker.vault.ui.dashboard.VaultViewModel
import com.moneytracker.vault.ui.theme.VaultColors
import com.moneytracker.vault.ui.theme.VaultTheme
import kotlin.math.roundToInt

private enum class RootScreenState {
    LOCKED,
    EDIT_RECOVERY_QUESTIONS,
    UNLOCKED
}

class MainActivity : FragmentActivity() {

    companion object {
        private const val AUTO_RELOCK_MS = 5 * 60_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        VaultColors.initTheme(applicationContext)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()
        val initialHz = requestOptimalDisplayRefreshRate()

        val authManager = VaultAuthManager(applicationContext)

        setContent {
            VaultTheme {
                val vaultViewModel: VaultViewModel = viewModel()
                val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()

                var isAppLockEnabled by remember { mutableStateOf(authManager.isAppLockEnabled()) }
                var isUnlocked by rememberSaveable { mutableStateOf(!authManager.isAppLockEnabled()) }
                var isResettingPasscode by rememberSaveable { mutableStateOf(false) }
                var isEditingSecurityQuestions by rememberSaveable { mutableStateOf(false) }
                var hasSecQuestions by remember { mutableStateOf(authManager.hasSecurityQuestionsConfigured()) }
                var backgroundedAt by rememberSaveable { mutableLongStateOf(0L) }
                var detectedRefreshRateHz by remember { mutableIntStateOf(initialHz) }

                var hasSmsPermission by remember {
                    mutableStateOf(checkSmsPermissions())
                }

                // Live Display Refresh Rate Listener (60Hz / 90Hz / 120Hz / 144Hz adaptive detection)
                DisposableEffect(Unit) {
                    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                    val listener = object : DisplayManager.DisplayListener {
                        override fun onDisplayAdded(displayId: Int) = Unit
                        override fun onDisplayRemoved(displayId: Int) = Unit
                        override fun onDisplayChanged(displayId: Int) {
                            detectedRefreshRateHz = requestOptimalDisplayRefreshRate()
                        }
                    }
                    displayManager?.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
                    onDispose {
                        displayManager?.unregisterDisplayListener(listener)
                    }
                }

                // White status bar icons in AMOLED Black & Lock screen; dark status bar icons in Light Mode
                val isAmoled = VaultColors.isAmoledDark
                val useDarkStatusBarIcons = isUnlocked && !isResettingPasscode && !isAmoled
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = useDarkStatusBarIcons
                    controller.isAppearanceLightNavigationBars = false
                }

                val smsPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { grants ->
                    val granted = grants[Manifest.permission.READ_SMS] == true
                    hasSmsPermission = granted
                    if (granted) {
                        vaultViewModel.syncDeviceSmsInbox()
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                if (isUnlocked && !isFinishing && !isChangingConfigurations) {
                                    backgroundedAt = System.currentTimeMillis()
                                }
                            }
                            Lifecycle.Event.ON_START -> {
                                if (isUnlocked && backgroundedAt > 0L) {
                                    val elapsed = System.currentTimeMillis() - backgroundedAt
                                    if (elapsed > AUTO_RELOCK_MS && isAppLockEnabled) {
                                        isUnlocked = false
                                    }
                                    backgroundedAt = 0L
                                }
                            }
                            Lifecycle.Event.ON_RESUME -> {
                                detectedRefreshRateHz = requestOptimalDisplayRefreshRate()
                                val granted = checkSmsPermissions()
                                hasSmsPermission = granted
                                if (isUnlocked && granted) {
                                    vaultViewModel.syncDeviceSmsInbox()
                                }
                            }
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(isUnlocked, hasSmsPermission) {
                    if (isUnlocked && hasSmsPermission) {
                        vaultViewModel.syncDeviceSmsInbox()
                    }
                }

                val rootState = when {
                    isAppLockEnabled && (!isUnlocked || isResettingPasscode) -> RootScreenState.LOCKED
                    !isAppLockEnabled && isResettingPasscode -> RootScreenState.LOCKED
                    isEditingSecurityQuestions -> RootScreenState.EDIT_RECOVERY_QUESTIONS
                    else -> RootScreenState.UNLOCKED
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VaultColors.AmoledBlack)
                ) {
                    AnimatedContent(
                        targetState = rootState,
                        transitionSpec = {
                            if (initialState == RootScreenState.LOCKED && targetState == RootScreenState.UNLOCKED) {
                                // Smooth GPU Vault Door Reveal Animation on Fingerprint / Passcode Unlock!
                                (scaleIn(
                                    initialScale = 0.93f,
                                    animationSpec = tween(340, easing = FastOutSlowInEasing)
                                ) + fadeIn(tween(250)))
                                    .togetherWith(
                                        scaleOut(
                                            targetScale = 1.08f,
                                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                                        ) + fadeOut(tween(200))
                                    )
                            } else {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                            }
                        },
                        label = "vaultUnlockTransition"
                    ) { state ->
                        when (state) {
                            RootScreenState.LOCKED -> {
                                AuthVaultScreen(
                                    activity = this@MainActivity,
                                    authManager = authManager,
                                    startInResetMode = isResettingPasscode,
                                    onAuthenticated = {
                                        hasSecQuestions = authManager.hasSecurityQuestionsConfigured()
                                        isResettingPasscode = false
                                        isUnlocked = true
                                    }
                                )
                            }

                            RootScreenState.EDIT_RECOVERY_QUESTIONS -> {
                                SecurityQuestionsFormScreen(
                                    title = "Recovery Questions",
                                    subtitle = "Save 3 personal answers so you can reset your passcode anytime.",
                                    buttonLabel = "SAVE RECOVERY ANSWERS",
                                    showSkip = true,
                                    skipLabel = "← CANCEL",
                                    onSkip = { isEditingSecurityQuestions = false },
                                    onSubmit = { a1, a2, a3 ->
                                        authManager.saveSecurityQuestions(a1, a2, a3)
                                        hasSecQuestions = true
                                        isEditingSecurityQuestions = false
                                        vaultViewModel.showStatusMessage("Saved 3 passcode recovery answers")
                                        true
                                    }
                                )
                            }

                            RootScreenState.UNLOCKED -> {
                                MainVaultScreen(
                                    uiState = uiState,
                                    hasSmsPermission = hasSmsPermission,
                                    hasSecurityQuestionsSet = hasSecQuestions,
                                    isAmoledDark = isAmoled,
                                    refreshRateHz = detectedRefreshRateHz,
                                    isAppLockEnabled = isAppLockEnabled,
                                    onToggleAppLock = { enabled ->
                                        authManager.setAppLockEnabled(enabled)
                                        isAppLockEnabled = enabled
                                    },
                                    onToggleAmoledTheme = {
                                        VaultColors.toggleTheme(applicationContext)
                                    },
                                    onRequestSmsPermission = {
                                        smsPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_SMS,
                                                Manifest.permission.RECEIVE_SMS
                                            )
                                        )
                                    },
                                    onSyncSms = { vaultViewModel.syncDeviceSmsInbox() },
                                    onLoadSampleSms = { vaultViewModel.injectRealisticSampleSms() },
                                    onClearSampleSms = { vaultViewModel.clearDemoSms() },
                                    onShiftMonth = { vaultViewModel.shiftMonth(it) },
                                    onJumpToCurrentMonth = { vaultViewModel.jumpToCurrentMonth() },
                                    onSelectYearMonth = { vaultViewModel.selectYearMonthKey(it) },
                                    onSearchQueryChange = { vaultViewModel.setSearchQuery(it) },
                                    onShuffleSuggestions = { vaultViewModel.shuffleSearchSuggestions() },
                                    onToggleMoneyMatterFilter = { vaultViewModel.toggleMoneyMatterFilter(it) },
                                    onToggleCategoryFilter = { vaultViewModel.toggleCategoryFilter(it) },
                                    onToggleAccountFilter = { vaultViewModel.toggleAccountFilter(it) },
                                    onToggleUpiAppFilter = { vaultViewModel.toggleUpiAppFilter(it) },
                                    onClearDrilldownFilters = { vaultViewModel.clearDrilldownFilters() },
                                    onDismissBanner = { vaultViewModel.dismissBanner() },
                                    onAutoSaveTransactionEdits = { txn, name, cat, note, included, applyAll ->
                                        vaultViewModel.autoSaveTransactionEdits(txn, name, cat, note, included, applyAll)
                                    },
                                    onDeleteTransactionWithUndo = { vaultViewModel.deleteTransactionWithUndo(it) },
                                    onUndoDelete = { vaultViewModel.undoRecentDelete() },
                                    onRunSandboxSms = { sender, sms, save ->
                                        vaultViewModel.testLiveSmsInSandbox(sender, sms, save)
                                    },
                                    onResetPasscodeClick = { isResettingPasscode = true },
                                    onEditSecurityQuestionsClick = { isEditingSecurityQuestions = true },
                                    onLockVaultNow = { isUnlocked = false },
                                    onExitApp = { finish() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Identifies the device's display capabilities (60Hz / 90Hz / 120Hz / 144Hz+)
     * and requests the highest supported native refresh rate at the active resolution.
     */
    @Suppress("DEPRECATION")
    private fun requestOptimalDisplayRefreshRate(): Int {
        return try {
            val activeDisplay: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                windowManager.defaultDisplay
            }
            if (activeDisplay == null) return 60

            val currentMode = activeDisplay.mode
            val bestHighRateMode = activeDisplay.supportedModes
                .filter { mode ->
                    mode.physicalWidth == currentMode.physicalWidth &&
                        mode.physicalHeight == currentMode.physicalHeight
                }
                .maxByOrNull { it.refreshRate }
                ?: activeDisplay.supportedModes.maxByOrNull { it.refreshRate }
                ?: currentMode

            val lp = window.attributes
            if (lp.preferredDisplayModeId != bestHighRateMode.modeId ||
                lp.preferredRefreshRate != bestHighRateMode.refreshRate
            ) {
                lp.preferredDisplayModeId = bestHighRateMode.modeId
                lp.preferredRefreshRate = bestHighRateMode.refreshRate
                window.attributes = lp
            }

            val reportedHz = maxOf(activeDisplay.refreshRate, bestHighRateMode.refreshRate).roundToInt()
            when {
                reportedHz >= 140 -> 144
                reportedHz >= 115 -> 120
                reportedHz >= 88 -> 90
                else -> 60
            }
        } catch (_: Exception) {
            60
        }
    }

    private fun checkSmsPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
