package com.moneytracker.vault

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

        val authManager = VaultAuthManager(applicationContext)

        setContent {
            VaultTheme {
                val vaultViewModel: VaultViewModel = viewModel()
                val uiState by vaultViewModel.uiState.collectAsStateWithLifecycle()

                var isUnlocked by rememberSaveable { mutableStateOf(false) }
                var isResettingPasscode by rememberSaveable { mutableStateOf(false) }
                var isEditingSecurityQuestions by rememberSaveable { mutableStateOf(false) }
                var hasSecQuestions by remember { mutableStateOf(authManager.hasSecurityQuestionsConfigured()) }
                var backgroundedAt by rememberSaveable { mutableLongStateOf(0L) }

                var hasSmsPermission by remember {
                    mutableStateOf(checkSmsPermissions())
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
                                if (isUnlocked && !isFinishing) {
                                    backgroundedAt = System.currentTimeMillis()
                                }
                            }
                            Lifecycle.Event.ON_START -> {
                                if (isUnlocked && backgroundedAt > 0L) {
                                    val elapsed = System.currentTimeMillis() - backgroundedAt
                                    if (elapsed > AUTO_RELOCK_MS) {
                                        isUnlocked = false
                                    }
                                    backgroundedAt = 0L
                                }
                            }
                            Lifecycle.Event.ON_RESUME -> {
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

                when {
                    !isUnlocked || isResettingPasscode -> {
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

                    isEditingSecurityQuestions -> {
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

                    else -> {
                        MainVaultScreen(
                            uiState = uiState,
                            hasSmsPermission = hasSmsPermission,
                            hasSecurityQuestionsSet = hasSecQuestions,
                            isAmoledDark = isAmoled,
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

    private fun checkSmsPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
