package com.moneytracker.vault.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.moneytracker.vault.security.VaultAuthManager
import com.moneytracker.vault.ui.components.CredSurfaceCard
import com.moneytracker.vault.ui.theme.CredSansFontFamily
import com.moneytracker.vault.ui.theme.CredSerifFontFamily
import com.moneytracker.vault.ui.theme.VaultColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class AuthStage {
    UNLOCK_PIN,
    CREATE_NEW_PIN,
    SETUP_SECURITY_QUESTIONS,
    VERIFY_SECURITY_QUESTIONS_FOR_RESET
}

@Composable
fun AuthVaultScreen(
    activity: FragmentActivity,
    authManager: VaultAuthManager,
    startInResetMode: Boolean = false,
    onAuthenticated: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var stage by rememberSaveable {
        mutableStateOf(
            when {
                !authManager.isPasscodeConfigured() -> AuthStage.CREATE_NEW_PIN
                startInResetMode -> AuthStage.CREATE_NEW_PIN
                else -> AuthStage.UNLOCK_PIN
            }
        )
    }

    var firstPinDraft by rememberSaveable { mutableStateOf("") }
    var pinInput by rememberSaveable { mutableStateOf("") }
    var hasAutoPromptedBiometric by rememberSaveable { mutableStateOf(false) }
    var isUnlockSuccessAnimating by remember { mutableStateOf(false) }

    var statusText by remember(stage) {
        mutableStateOf(
            when (stage) {
                AuthStage.CREATE_NEW_PIN ->
                    if (startInResetMode) "ENTER NEW 4-DIGIT PASSCODE" else "CREATE YOUR 4-DIGIT PASSCODE"
                AuthStage.UNLOCK_PIN -> "ENTER PASSCODE TO UNLOCK"
                else -> ""
            }
        )
    }
    var isError by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    val unlockHaloProgress = remember { Animatable(0f) }

    val emblemScale by animateFloatAsState(
        targetValue = if (isUnlockSuccessAnimating) 1.14f else 1f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "unlockEmblemScale"
    )

    fun triggerErrorShake(message: String) {
        isError = true
        statusText = message
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f).forEach { target ->
                shakeOffset.animateTo(target, animationSpec = tween(42))
            }
        }
    }

    fun playUnlockAnimationThenProceed() {
        if (isUnlockSuccessAnimating) return
        isUnlockSuccessAnimating = true
        isError = false
        pinInput = "••••"
        statusText = "VAULT UNLOCKED"
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            launch {
                unlockHaloProgress.snapTo(0f)
                unlockHaloProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            delay(230L)
            if (!authManager.hasSecurityQuestionsConfigured()) {
                isUnlockSuccessAnimating = false
                stage = AuthStage.SETUP_SECURITY_QUESTIONS
            } else {
                onAuthenticated()
            }
        }
    }

    LaunchedEffect(stage) {
        if (stage == AuthStage.UNLOCK_PIN && authManager.isBiometricEnabled() && !hasAutoPromptedBiometric) {
            hasAutoPromptedBiometric = true
            authManager.triggerBiometricPrompt(
                activity = activity,
                onSuccess = {
                    playUnlockAnimationThenProceed()
                },
                onErrorMessage = { msg ->
                    statusText = msg.uppercase()
                }
            )
        }
    }

    if (stage == AuthStage.SETUP_SECURITY_QUESTIONS) {
        SecurityQuestionsFormScreen(
            title = "passcode recovery",
            subtitle = "Set 3 personal answers so you can reset your passcode anytime.",
            buttonLabel = "save recovery answers",
            showSkip = true,
            onSkip = onAuthenticated,
            onSubmit = { a1, a2, a3 ->
                authManager.saveSecurityQuestions(a1, a2, a3)
                onAuthenticated()
                true
            }
        )
        return
    }

    if (stage == AuthStage.VERIFY_SECURITY_QUESTIONS_FOR_RESET) {
        SecurityQuestionsFormScreen(
            title = "reset passcode",
            subtitle = "Answer your 3 personal security questions to create a new passcode.",
            buttonLabel = "verify & reset passcode",
            showSkip = true,
            skipLabel = "← back",
            onSkip = { stage = AuthStage.UNLOCK_PIN },
            onSubmit = { a1, a2, a3 ->
                val verified = authManager.verifySecurityQuestions(a1, a2, a3)
                if (verified) {
                    firstPinDraft = ""
                    pinInput = ""
                    stage = AuthStage.CREATE_NEW_PIN
                }
                verified
            }
        )
        return
    }

    fun onDigitPressed(digit: String) {
        if (isUnlockSuccessAnimating || pinInput.length >= 4) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        isError = false
        pinInput += digit

        if (pinInput.length == 4) {
            if (stage == AuthStage.CREATE_NEW_PIN) {
                if (firstPinDraft.isEmpty()) {
                    firstPinDraft = pinInput
                    pinInput = ""
                    statusText = "CONFIRM YOUR 4-DIGIT PASSCODE"
                } else if (firstPinDraft == pinInput) {
                    authManager.setupPasscode(pinInput)
                    playUnlockAnimationThenProceed()
                } else {
                    firstPinDraft = ""
                    pinInput = ""
                    triggerErrorShake("PASSCODES DID NOT MATCH • TRY AGAIN")
                }
            } else {
                if (authManager.verifyPasscode(pinInput)) {
                    playUnlockAnimationThenProceed()
                } else {
                    pinInput = ""
                    triggerErrorShake("INCORRECT PASSCODE")
                }
            }
        }
    }

    val onBiometricClick = {
        if (!isUnlockSuccessAnimating) {
            authManager.triggerBiometricPrompt(
                activity = activity,
                onSuccess = { playUnlockAnimationThenProceed() },
                onErrorMessage = { msg -> statusText = msg.uppercase() }
            )
        }
    }

    val onBackspaceClick = {
        if (!isUnlockSuccessAnimating && pinInput.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pinInput = pinInput.dropLast(1)
        }
    }

    val onForgotClick = {
        if (!isUnlockSuccessAnimating) {
            if (authManager.hasSecurityQuestionsConfigured()) {
                stage = AuthStage.VERIFY_SECURITY_QUESTIONS_FOR_RESET
            } else {
                triggerErrorShake("UNLOCK WITH FINGERPRINT FIRST TO SET QUESTIONS")
            }
        }
    }

    // =========================================================================
    // RESPONSIVE PORTRAIT & LANDSCAPE PITCH-BLACK #000000 VAULT LOCK SCREEN
    // =========================================================================
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.AmoledBlack)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        val isLandscape = maxWidth > maxHeight || maxHeight < 520.dp

        if (isLandscape) {
            // 2-COLUMN HORIZONTAL SPLIT FOR LANDSCAPE (ALL KEYS 1-9, 0, BIOMETRIC FIT 100% ON SCREEN!)
            val landscapeKeySize = ((maxHeight - 28.dp) / 4.45f).coerceIn(42.dp, 62.dp)
            val landscapeRowGap = ((maxHeight - landscapeKeySize * 4f) / 4.6f).coerceIn(6.dp, 13.dp)

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LockHeaderAndPinDots(
                        stage = stage,
                        statusText = statusText,
                        pinLength = pinInput.length,
                        isError = isError,
                        isUnlockSuccess = isUnlockSuccessAnimating,
                        emblemScale = emblemScale,
                        haloProgress = unlockHaloProgress.value,
                        shakePx = shakeOffset.value,
                        compact = true
                    )

                    if (stage == AuthStage.UNLOCK_PIN) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "forgot passcode?",
                            fontFamily = CredSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD1D5DB),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onForgotClick)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NumericBiometricKeypad(
                        showBiometric = stage == AuthStage.UNLOCK_PIN && authManager.canUseBiometricHardware(),
                        keySize = landscapeKeySize,
                        rowGap = landscapeRowGap,
                        colGap = 20.dp,
                        onDigitPressed = ::onDigitPressed,
                        onBiometricClick = onBiometricClick,
                        onBackspaceClick = onBackspaceClick
                    )
                }
            }
        } else {
            // PORTRAIT LAYOUT (WITH ADAPTIVE KEY SIZING FOR ALL SCREEN HEIGHTS)
            val portraitKeySize = ((maxHeight - 275.dp) / 4.65f).coerceIn(52.dp, 72.dp)
            val portraitRowGap = ((maxHeight - 275.dp - portraitKeySize * 4f) / 5f).coerceIn(8.dp, 14.dp)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                LockHeaderAndPinDots(
                    stage = stage,
                    statusText = statusText,
                    pinLength = pinInput.length,
                    isError = isError,
                    isUnlockSuccess = isUnlockSuccessAnimating,
                    emblemScale = emblemScale,
                    haloProgress = unlockHaloProgress.value,
                    shakePx = shakeOffset.value,
                    compact = false
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NumericBiometricKeypad(
                        showBiometric = stage == AuthStage.UNLOCK_PIN && authManager.canUseBiometricHardware(),
                        keySize = portraitKeySize,
                        rowGap = portraitRowGap,
                        colGap = 24.dp,
                        onDigitPressed = ::onDigitPressed,
                        onBiometricClick = onBiometricClick,
                        onBackspaceClick = onBackspaceClick
                    )

                    if (stage == AuthStage.UNLOCK_PIN) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "forgot passcode?",
                            fontFamily = CredSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD1D5DB),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onForgotClick)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LockHeaderAndPinDots(
    stage: AuthStage,
    statusText: String,
    pinLength: Int,
    isError: Boolean,
    isUnlockSuccess: Boolean,
    emblemScale: Float,
    haloProgress: Float,
    shakePx: Float,
    compact: Boolean
) {
    val emeraldUnlock = Color(0xFF34D399)
    val emblemSize = if (compact) 54.dp else 64.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(emblemSize * 1.6f),
            contentAlignment = Alignment.Center
        ) {
            if (haloProgress > 0f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val s = 0.85f + (haloProgress * 0.75f)
                            scaleX = s
                            scaleY = s
                            alpha = (1f - haloProgress).coerceIn(0f, 1f)
                        }
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                emeraldUnlock.copy(alpha = 0.55f),
                                VaultColors.ChampagneGold.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(emblemSize)
                    .graphicsLayer {
                        scaleX = emblemScale
                        scaleY = emblemScale
                    }
                    .clip(CircleShape)
                    .background(
                        if (isUnlockSuccess) emeraldUnlock.copy(alpha = 0.20f)
                        else VaultColors.DockOrbSurface
                    )
                    .border(
                        width = if (isUnlockSuccess) 1.6.dp else 1.dp,
                        color = if (isUnlockSuccess) emeraldUnlock else VaultColors.DockOrbBorder,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlockSuccess) Icons.Outlined.Check else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (isUnlockSuccess) emeraldUnlock else Color.White,
                    modifier = Modifier.size(if (compact) 23.dp else 26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(if (compact) 8.dp else 14.dp))

        Text(
            text = when {
                isUnlockSuccess -> "vault unlocked"
                stage == AuthStage.CREATE_NEW_PIN -> "set passcode"
                else -> "unlock vault"
            },
            fontFamily = CredSerifFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 24.sp else 30.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isUnlockSuccess -> emeraldUnlock
                isError -> Color(0xFFFF6B6B)
                else -> Color(0xFF9CA3AF)
            },
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(if (compact) 18.dp else 26.dp))

        val dotScale by animateFloatAsState(
            targetValue = if (isUnlockSuccess) 1.24f else 1f,
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            label = "pinDotUnlockScale"
        )

        Row(
            modifier = Modifier.offset { IntOffset(shakePx.roundToInt(), 0) },
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            repeat(4) { idx ->
                val filled = idx < pinLength
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer {
                            scaleX = dotScale
                            scaleY = dotScale
                        }
                        .clip(CircleShape)
                        .background(
                            when {
                                isUnlockSuccess -> emeraldUnlock
                                isError -> Color(0xFFFF6B6B)
                                filled -> Color.White
                                else -> Color(0xFF1F2128)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                isUnlockSuccess -> emeraldUnlock
                                filled -> Color.White
                                else -> Color(0xFF3E414C)
                            },
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun NumericBiometricKeypad(
    showBiometric: Boolean,
    keySize: Dp,
    rowGap: Dp,
    colGap: Dp,
    onDigitPressed: (String) -> Unit,
    onBiometricClick: () -> Unit,
    onBackspaceClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(rowGap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(colGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { digit ->
                    CredLockKeyButton(
                        text = digit,
                        size = keySize,
                        onClick = { onDigitPressed(digit) }
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(colGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBiometric) {
                CredLockIconKey(
                    icon = Icons.Outlined.Fingerprint,
                    tint = VaultColors.ChampagneGold,
                    contentDescription = "Fingerprint",
                    size = keySize,
                    onClick = onBiometricClick
                )
            } else {
                Spacer(modifier = Modifier.size(keySize))
            }

            CredLockKeyButton(
                text = "0",
                size = keySize,
                onClick = { onDigitPressed("0") }
            )

            CredLockIconKey(
                icon = Icons.AutoMirrored.Outlined.Backspace,
                tint = Color(0xFF9CA3AF),
                contentDescription = "Delete",
                size = keySize,
                onClick = onBackspaceClick
            )
        }
    }
}

@Composable
fun SecurityQuestionsFormScreen(
    title: String,
    subtitle: String,
    buttonLabel: String,
    showSkip: Boolean,
    skipLabel: String = "skip for now >",
    onSkip: () -> Unit,
    onSubmit: (String, String, String) -> Boolean
) {
    val questions = VaultAuthManager.SECURITY_QUESTIONS
    var a1 by rememberSaveable { mutableStateOf("") }
    var a2 by rememberSaveable { mutableStateOf("") }
    var a3 by rememberSaveable { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.CredWhite)
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VaultColors.CredMutedFill)
                        .border(1.dp, VaultColors.CredBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = VaultColors.InkPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (showSkip) {
                    Text(
                        text = skipLabel,
                        fontFamily = CredSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VaultColors.InkSecondary,
                        modifier = Modifier
                            .clickable { onSkip() }
                            .padding(8.dp)
                    )
                }
            }

            Text(
                text = title,
                fontFamily = CredSerifFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = VaultColors.InkPrimary
            )

            Text(
                text = subtitle,
                fontFamily = CredSansFontFamily,
                fontSize = 13.sp,
                color = VaultColors.InkSecondary
            )

            CredSurfaceCard {
                SecurityQuestionInput(number = "01", question = questions[0], value = a1, onValueChange = { a1 = it; errorMsg = null })
                Spacer(modifier = Modifier.height(16.dp))
                SecurityQuestionInput(number = "02", question = questions[1], value = a2, onValueChange = { a2 = it; errorMsg = null })
                Spacer(modifier = Modifier.height(16.dp))
                SecurityQuestionInput(number = "03", question = questions[2], value = a3, onValueChange = { a3 = it; errorMsg = null })
            }

            errorMsg?.let { err ->
                Text(
                    text = err,
                    fontFamily = CredSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VaultColors.DebitRed
                )
            }

            Button(
                onClick = {
                    if (a1.isBlank() || a2.isBlank() || a3.isBlank()) {
                        errorMsg = "Please answer all 3 security questions."
                    } else {
                        val ok = onSubmit(a1, a2, a3)
                        if (!ok) {
                            errorMsg = "Answers did not match your saved recovery answers."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultColors.InkPrimary,
                    contentColor = VaultColors.OnInkPrimary
                )
            ) {
                Text(
                    text = buttonLabel,
                    fontFamily = CredSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SecurityQuestionInput(
    number: String,
    question: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VaultColors.InkMuted
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = question,
                fontFamily = CredSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = VaultColors.InkPrimary,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text("your answer...", fontFamily = CredSansFontFamily, fontSize = 13.sp, color = VaultColors.InkMuted) },
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
    }
}

@Composable
private fun CredLockKeyButton(
    text: String,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(VaultColors.DockOrbSurface)
            .border(1.dp, VaultColors.DockOrbBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = CredSansFontFamily,
            fontSize = (size.value * 0.34f).sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun CredLockIconKey(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF101115))
            .border(1.dp, Color(0xFF282A33), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.36f)
        )
    }
}
