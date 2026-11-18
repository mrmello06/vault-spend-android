package com.moneytracker.vault.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.moneytracker.vault.security.VaultAuthManager
import com.moneytracker.vault.ui.components.CredSurfaceCard
import com.moneytracker.vault.ui.theme.CredSansFontFamily
import com.moneytracker.vault.ui.theme.CredSerifFontFamily
import com.moneytracker.vault.ui.theme.VaultColors
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

    var stage by remember {
        mutableStateOf(
            when {
                !authManager.isPasscodeConfigured() -> AuthStage.CREATE_NEW_PIN
                startInResetMode -> AuthStage.CREATE_NEW_PIN
                else -> AuthStage.UNLOCK_PIN
            }
        )
    }

    var firstPinDraft by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
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

    fun triggerErrorShake(message: String) {
        isError = true
        statusText = message
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f).forEach { target ->
                shakeOffset.animateTo(target, animationSpec = tween(45))
            }
        }
    }

    LaunchedEffect(stage) {
        if (stage == AuthStage.UNLOCK_PIN && authManager.isBiometricEnabled()) {
            authManager.triggerBiometricPrompt(
                activity = activity,
                onSuccess = {
                    if (!authManager.hasSecurityQuestionsConfigured()) {
                        stage = AuthStage.SETUP_SECURITY_QUESTIONS
                    } else {
                        onAuthenticated()
                    }
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
        if (pinInput.length >= 4) return
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
                    if (!authManager.hasSecurityQuestionsConfigured()) {
                        stage = AuthStage.SETUP_SECURITY_QUESTIONS
                    } else {
                        onAuthenticated()
                    }
                } else {
                    firstPinDraft = ""
                    pinInput = ""
                    triggerErrorShake("PASSCODES DID NOT MATCH • TRY AGAIN")
                }
            } else {
                if (authManager.verifyPasscode(pinInput)) {
                    if (!authManager.hasSecurityQuestionsConfigured()) {
                        stage = AuthStage.SETUP_SECURITY_QUESTIONS
                    } else {
                        onAuthenticated()
                    }
                } else {
                    pinInput = ""
                    triggerErrorShake("INCORRECT PASSCODE")
                }
            }
        }
    }

    // CRED Pitch-Black #000000 Lock Screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.AmoledBlack)
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(VaultColors.DockOrbSurface)
                        .border(1.dp, VaultColors.DockOrbBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = if (stage == AuthStage.CREATE_NEW_PIN) "set passcode" else "unlock vault",
                    fontFamily = CredSerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isError) Color(0xFFFF6B6B) else Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(30.dp))

                Row(
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    repeat(4) { idx ->
                        val filled = idx < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> Color(0xFFFF6B6B)
                                        filled -> Color.White
                                        else -> Color(0xFF1F2128)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (filled) Color.White else Color(0xFF3E414C),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { digit ->
                            CredLockKeyButton(
                                text = digit,
                                onClick = { onDigitPressed(digit) }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stage == AuthStage.UNLOCK_PIN && authManager.canUseBiometricHardware()) {
                        CredLockIconKey(
                            icon = Icons.Outlined.Fingerprint,
                            tint = VaultColors.ChampagneGold,
                            contentDescription = "Fingerprint",
                            onClick = {
                                authManager.triggerBiometricPrompt(
                                    activity = activity,
                                    onSuccess = {
                                        if (!authManager.hasSecurityQuestionsConfigured()) {
                                            stage = AuthStage.SETUP_SECURITY_QUESTIONS
                                        } else {
                                            onAuthenticated()
                                        }
                                    },
                                    onErrorMessage = { msg -> statusText = msg.uppercase() }
                                )
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }

                    CredLockKeyButton(
                        text = "0",
                        onClick = { onDigitPressed("0") }
                    )

                    CredLockIconKey(
                        icon = Icons.AutoMirrored.Outlined.Backspace,
                        tint = Color(0xFF9CA3AF),
                        contentDescription = "Delete",
                        onClick = {
                            if (pinInput.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pinInput = pinInput.dropLast(1)
                            }
                        }
                    )
                }

                if (stage == AuthStage.UNLOCK_PIN) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "forgot passcode?",
                        fontFamily = CredSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD1D5DB),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                if (authManager.hasSecurityQuestionsConfigured()) {
                                    stage = AuthStage.VERIFY_SECURITY_QUESTIONS_FOR_RESET
                                } else {
                                    triggerErrorShake("UNLOCK WITH FINGERPRINT FIRST TO SET QUESTIONS")
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
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
    var a1 by remember { mutableStateOf("") }
    var a2 by remember { mutableStateOf("") }
    var a3 by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.CredWhite)
            .systemBarsPadding()
            .imePadding()
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
            fontSize = 30.sp,
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
                .height(52.dp),
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

        Spacer(modifier = Modifier.height(24.dp))
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(VaultColors.DockOrbSurface)
            .border(1.dp, VaultColors.DockOrbBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = CredSansFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun CredLockIconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
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
            modifier = Modifier.size(25.dp)
        )
    }
}
