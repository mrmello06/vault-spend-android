package com.moneytracker.vault.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditScore
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.vault.data.local.TransactionEntity
import com.moneytracker.vault.domain.model.PaymentRail
import com.moneytracker.vault.domain.model.SpendCategory
import com.moneytracker.vault.domain.model.TransactionType
import com.moneytracker.vault.ui.components.CategoryBadgeIcon
import com.moneytracker.vault.ui.components.CredExplorePill
import com.moneytracker.vault.ui.components.CredSurfaceCard
import com.moneytracker.vault.ui.components.iconVector
import com.moneytracker.vault.ui.theme.CredSansFontFamily
import com.moneytracker.vault.ui.theme.VaultColors
import com.moneytracker.vault.ui.theme.VaultFormatters
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailModal(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onAutoSaveEdits: (
        newDisplayName: String,
        newCategory: SpendCategory,
        newNote: String,
        isIncludedInSpend: Boolean,
        applyToAllMatching: Boolean
    ) -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialCategory = if (transaction.isSelfTransfer) {
        SpendCategory.SELF_TRANSFER
    } else {
        SpendCategory.fromNameOrDefault(transaction.category)
    }

    var merchantNameDraft by remember(transaction.id) { mutableStateOf(transaction.merchantDisplayName) }
    var noteDraft by remember(transaction.id) { mutableStateOf(transaction.note) }
    var includeInSpend by remember(transaction.id) { mutableStateOf(transaction.isIncludedInSpend) }
    var selectedCategory by remember(transaction.id) { mutableStateOf(initialCategory) }
    var teachAllMatching by remember { mutableStateOf(true) }
    var hasInitialized by remember(transaction.id) { mutableStateOf(false) }
    var autoSavedBadgeVisible by remember { mutableStateOf(false) }

    fun hasAnyDirtyChange(): Boolean {
        return merchantNameDraft.trim() != transaction.merchantDisplayName ||
            noteDraft.trim() != transaction.note ||
            includeInSpend != transaction.isIncludedInSpend ||
            selectedCategory != initialCategory
    }

    fun pushAutoSaveNow(
        name: String = merchantNameDraft,
        cat: SpendCategory = selectedCategory,
        note: String = noteDraft,
        included: Boolean = includeInSpend,
        applyAll: Boolean = teachAllMatching
    ) {
        onAutoSaveEdits(name, cat, note, included, applyAll)
        autoSavedBadgeVisible = true
    }

    LaunchedEffect(noteDraft, merchantNameDraft) {
        if (!hasInitialized) {
            hasInitialized = true
            return@LaunchedEffect
        }
        if (!hasAnyDirtyChange()) return@LaunchedEffect
        delay(320L)
        pushAutoSaveNow()
    }

    val isCredit = transaction.type == TransactionType.CREDIT.name
    val isSelf = selectedCategory == SpendCategory.SELF_TRANSFER
    val isCardBill = selectedCategory == SpendCategory.CREDIT_CARD_BILL
    val rail = PaymentRail.fromNameOrDefault(transaction.paymentRail)

    val amountColor = when {
        !includeInSpend && !isSelf && !isCardBill -> VaultColors.InkMuted
        isSelf -> VaultColors.SelfYellowText
        isCardBill -> VaultColors.CardBillBlue
        isCredit -> VaultColors.CreditGreen
        else -> VaultColors.DebitRed
    }

    ModalBottomSheet(
        onDismissRequest = {
            // ONLY save if the user actually modified something (viewing alone never locks isUserModified = 1!)
            if (hasAnyDirtyChange()) {
                onAutoSaveEdits(
                    merchantNameDraft,
                    selectedCategory,
                    noteDraft,
                    includeInSpend,
                    teachAllMatching
                )
            }
            onDismiss()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = VaultColors.CardSurface,
        contentColor = VaultColors.InkPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // STICKY HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VaultColors.CardSurface)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CategoryBadgeIcon(category = selectedCategory, size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val prefix = when {
                            isSelf -> "⇄ "
                            isCardBill -> ""
                            isCredit -> "+"
                            else -> "-"
                        }
                        Text(
                            text = "$prefix${VaultFormatters.formatInr(transaction.amount, true)}",
                            fontFamily = CredSansFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            color = amountColor,
                            textDecoration = if (!includeInSpend && !isSelf && !isCardBill) {
                                TextDecoration.LineThrough
                            } else TextDecoration.None
                        )
                        Text(
                            text = VaultFormatters.formatFullDateTime(transaction.timestamp),
                            fontFamily = CredSansFontFamily,
                            fontSize = 11.sp,
                            color = VaultColors.InkSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (autoSavedBadgeVisible) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(VaultColors.CreditGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = VaultColors.CreditGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "saved",
                                fontFamily = CredSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultColors.CreditGreen
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete transaction",
                            tint = VaultColors.DebitRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (hasAnyDirtyChange()) {
                                onAutoSaveEdits(
                                    merchantNameDraft,
                                    selectedCategory,
                                    noteDraft,
                                    includeInSpend,
                                    teachAllMatching
                                )
                            }
                            onDismiss()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = VaultColors.InkSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = VaultColors.CredBorder, thickness = 1.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                when {
                    isSelf -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(VaultColors.SelfYellowBg)
                                .border(1.dp, VaultColors.SelfYellowBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.CompareArrows,
                                contentDescription = null,
                                tint = VaultColors.SelfYellowText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Self transfer between your accounts — automatically excluded from monthly spend & credit totals.",
                                fontFamily = CredSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = VaultColors.SelfYellowText
                            )
                        }
                    }

                    isCardBill -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(VaultColors.CardBillBg)
                                .border(1.dp, VaultColors.CardBillBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CreditScore,
                                contentDescription = null,
                                tint = VaultColors.CardBillBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Credit card bill payment (e.g. CRED Club) — tracked separately so individual card purchases aren't counted twice.",
                                fontFamily = CredSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = VaultColors.CardBillBlue
                            )
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (includeInSpend) VaultColors.CredCanvasSubtle else VaultColors.BlackedOutCard
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (includeInSpend) VaultColors.CredBorderStrong else VaultColors.BlackedOutCard,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val next = !includeInSpend
                                    includeInSpend = next
                                    pushAutoSaveNow(included = next)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (includeInSpend) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = if (includeInSpend) VaultColors.InkPrimary else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (includeInSpend) "counted in total spends" else "excluded from total spends",
                                        fontFamily = CredSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (includeInSpend) VaultColors.InkPrimary else Color.White
                                    )
                                    Text(
                                        text = if (includeInSpend) "toggle off to black out & exclude from totals"
                                        else "blacked out in list & excluded from monthly math",
                                        fontFamily = CredSansFontFamily,
                                        fontSize = 11.sp,
                                        color = if (includeInSpend) VaultColors.InkSecondary else Color(0xFF9CA3AF)
                                    )
                                }
                            }

                            Switch(
                                checked = includeInSpend,
                                onCheckedChange = { next ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    includeInSpend = next
                                    pushAutoSaveNow(included = next)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = VaultColors.OnInkPrimary,
                                    checkedTrackColor = VaultColors.InkPrimary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFF4B5563)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Notes,
                        contentDescription = null,
                        tint = VaultColors.InkSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PERSONAL NOTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultColors.InkSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    placeholder = {
                        Text(
                            "Add a personal note...",
                            fontFamily = CredSansFontFamily,
                            fontSize = 13.sp,
                            color = VaultColors.InkMuted
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultColors.InkPrimary,
                        unfocusedBorderColor = VaultColors.CredBorderStrong,
                        focusedTextColor = VaultColors.InkPrimary,
                        unfocusedTextColor = VaultColors.InkPrimary,
                        focusedContainerColor = VaultColors.CredCanvasSubtle,
                        unfocusedContainerColor = VaultColors.CredCanvasSubtle
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MERCHANT / PAYEE NAME",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultColors.InkSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = merchantNameDraft,
                    onValueChange = { merchantNameDraft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultColors.InkPrimary,
                        unfocusedBorderColor = VaultColors.CredBorderStrong,
                        focusedTextColor = VaultColors.InkPrimary,
                        unfocusedTextColor = VaultColors.InkPrimary,
                        focusedContainerColor = VaultColors.CredCanvasSubtle,
                        unfocusedContainerColor = VaultColors.CredCanvasSubtle
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultColors.InkSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpendCategory.entries.forEach { cat ->
                        CredExplorePill(
                            label = cat.displayName,
                            icon = cat.iconVector(),
                            accentColor = if (cat == SpendCategory.SELF_TRANSFER) VaultColors.SelfYellowText else Color(cat.colorHex),
                            selected = cat == selectedCategory,
                            onClick = {
                                selectedCategory = cat
                                pushAutoSaveNow(cat = cat)
                            }
                        )
                    }
                }

                if (transaction.merchantRawCode.length >= 3 &&
                    transaction.merchantRawCode !in setOf("UPI", "CARD", "BANK", "WALLET")
                ) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { teachAllMatching = !teachAllMatching }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = teachAllMatching,
                            onCheckedChange = { teachAllMatching = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = VaultColors.InkPrimary,
                                checkmarkColor = VaultColors.OnInkPrimary
                            )
                        )
                        Text(
                            text = "remember for all '${transaction.merchantRawCode}' messages",
                            fontFamily = CredSansFontFamily,
                            fontSize = 13.sp,
                            color = VaultColors.InkPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                CredSurfaceCard(backgroundColor = VaultColors.CredCanvasSubtle) {
                    Text(
                        text = "SMS & PAYMENT DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultColors.InkSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailRow("SELLER CODE", transaction.merchantRawCode, mono = true)
                    DetailRow(
                        "INSTRUMENT",
                        buildString {
                            append(transaction.bankName)
                            if (transaction.accountLast4 != "0000") append(" ••${transaction.accountLast4}")
                            append(" • ")
                            append(if (transaction.upiApp.isNotBlank()) transaction.upiApp else rail.label)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = transaction.fullSmsBody.ifBlank { transaction.sanitizedPreview },
                        fontFamily = CredSansFontFamily,
                        fontSize = 12.sp,
                        color = VaultColors.InkSecondary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    mono: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = CredSansFontFamily,
            fontSize = 11.sp,
            color = VaultColors.InkSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = if (mono) FontFamily.Monospace else CredSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            color = VaultColors.InkPrimary
        )
    }
}
