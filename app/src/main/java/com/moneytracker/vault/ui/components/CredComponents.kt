package com.moneytracker.vault.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.CreditScore
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.vault.domain.model.AccountSpendSummary
import com.moneytracker.vault.domain.model.CategoryBreakdown
import com.moneytracker.vault.domain.model.MonthlyTrendPoint
import com.moneytracker.vault.domain.model.PaymentRail
import com.moneytracker.vault.domain.model.SpendCategory
import com.moneytracker.vault.ui.theme.CredSansFontFamily
import com.moneytracker.vault.ui.theme.VaultColors
import com.moneytracker.vault.ui.theme.VaultFormatters

/**
 * Adaptive CRED Card Surface (AMOLED #0E1015 on #000000 in Dark Mode, Crisp #FFFFFF in Light Mode)
 */
@Composable
fun CredSurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    borderColor: Color = VaultColors.CredBorder,
    backgroundColor: Color = VaultColors.CardSurface,
    shadowElevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(cornerRadius)
    val effectiveShadow = if (VaultColors.isAmoledDark) 0.dp else shadowElevation

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (effectiveShadow > 0.dp) {
                    Modifier.shadow(
                        elevation = effectiveShadow,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.07f)
                    )
                } else Modifier
            )
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                } else Modifier
            )
            .padding(16.dp),
        content = content
    )
}

/**
 * Adaptive CRED Capsule Filter Pill (High-Contrast Vivid AMOLED & Light Mode readable)
 */
@Composable
fun CredExplorePill(
    label: String,
    icon: ImageVector? = null,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(50)

    val targetBgColor = if (selected) {
        if (VaultColors.isAmoledDark) accentColor.copy(alpha = 0.28f) else accentColor.copy(alpha = 0.18f)
    } else {
        if (VaultColors.isAmoledDark) Color(0xFF141720) else VaultColors.CardSurface
    }
    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "pillBgColor"
    )

    val targetBorderColor = if (selected) accentColor else VaultColors.CredBorderStrong
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "pillBorderColor"
    )

    Row(
        modifier = Modifier
            .clip(shape)
            .background(animatedBgColor)
            .border(
                width = if (selected) 1.8.dp else 1.dp,
                color = animatedBorderColor,
                shape = shape
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else VaultColors.InkSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = label.lowercase(),
            fontFamily = CredSansFontFamily,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) (if (VaultColors.isAmoledDark) Color.White else accentColor) else VaultColors.InkPrimary
        )
    }
}

/**
 * Rolling Odometer Counter ("total spends number goes up and down")
 * Always uses uniform CredSansFontFamily tabular numbers across the entire app.
 */
@Composable
fun RollingOdometerAmount(
    amount: Double,
    modifier: Modifier = Modifier,
    prefix: String = "",
    style: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = VaultColors.InkPrimary,
    exactDecimals: Boolean = true
) {
    var previousAmount by remember { mutableDoubleStateOf(amount) }
    var hasChangedSinceMount by remember { mutableStateOf(false) }

    val deltaDetected = amount != previousAmount
    val isGoingUp = amount >= previousAmount

    SideEffect {
        if (deltaDetected) {
            hasChangedSinceMount = true
            previousAmount = amount
        }
    }

    val uniformNumberStyle = style.copy(
        fontFamily = CredSansFontFamily,
        fontFeatureSettings = "tnum"
    )

    val formatted = "$prefix${VaultFormatters.formatInr(amount, exactDecimals)}"

    if (!hasChangedSinceMount && !deltaDetected) {
        Text(
            text = formatted,
            style = uniformNumberStyle,
            color = color,
            modifier = modifier,
            maxLines = 1
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        formatted.forEachIndexed { index, ch ->
            if (ch.isDigit()) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        if (isGoingUp) {
                            (slideInVertically(tween(280, easing = FastOutSlowInEasing)) { h -> h } + fadeIn(tween(180)))
                                .togetherWith(slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { h -> -h } + fadeOut(tween(150)))
                        } else {
                            (slideInVertically(tween(280, easing = FastOutSlowInEasing)) { h -> -h } + fadeIn(tween(180)))
                                .togetherWith(slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { h -> h } + fadeOut(tween(150)))
                        }
                    },
                    label = "odometerDigit_$index"
                ) { digitChar ->
                    Text(
                        text = digitChar.toString(),
                        style = uniformNumberStyle,
                        color = color
                    )
                }
            } else {
                Text(
                    text = ch.toString(),
                    style = uniformNumberStyle,
                    color = color
                )
            }
        }
    }
}

fun SpendCategory.iconVector(): ImageVector = when (this) {
    SpendCategory.ENTERTAINMENT -> Icons.Outlined.Movie
    SpendCategory.FOOD_DINING -> Icons.Outlined.Restaurant
    SpendCategory.GROCERIES -> Icons.Outlined.LocalGroceryStore
    SpendCategory.SHOPPING -> Icons.Outlined.ShoppingBag
    SpendCategory.TRANSPORT_TRAVEL -> Icons.Outlined.DirectionsCar
    SpendCategory.FUEL_AUTO -> Icons.Outlined.LocalGasStation
    SpendCategory.BILLS_UTILITIES -> Icons.AutoMirrored.Outlined.ReceiptLong
    SpendCategory.HEALTH_WELLNESS -> Icons.Outlined.FavoriteBorder
    SpendCategory.INVESTMENTS_FINANCE -> Icons.AutoMirrored.Outlined.TrendingUp
    SpendCategory.EDUCATION -> Icons.Outlined.School
    SpendCategory.SALARY -> Icons.Outlined.AccountBalance
    SpendCategory.CREDIT_CARD_BILL -> Icons.Outlined.CreditScore
    SpendCategory.SELF_TRANSFER -> Icons.AutoMirrored.Outlined.CompareArrows
    SpendCategory.TRANSFERS -> Icons.Outlined.PersonOutline
    SpendCategory.OTHERS -> Icons.Outlined.AutoAwesome
}

@Composable
fun CategoryBadgeIcon(
    category: SpendCategory,
    size: Dp = 44.dp
) {
    val tint = when (category) {
        SpendCategory.SELF_TRANSFER -> VaultColors.SelfYellowText
        SpendCategory.SALARY -> Color(0xFF00E676)
        else -> Color(category.colorHex)
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(VaultColors.CredCanvasSubtle)
            .border(1.dp, VaultColors.CredBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.iconVector(),
            contentDescription = category.displayName,
            tint = tint,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

@Composable
fun CategoryDonutChart(
    items: List<CategoryBreakdown>,
    totalSpent: Double,
    modifier: Modifier = Modifier
) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(items) { played = true }

    val progress by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "donutSweep"
    )

    val ringTrackColor = VaultColors.CredMutedFill

    Box(
        modifier = modifier.size(216.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(196.dp)) {
            val strokeWidth = 22.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = ringTrackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            if (items.isNotEmpty() && totalSpent > 0.0) {
                var currentStart = -90f
                val gapDegrees = if (items.size > 1) 3.2f else 0f

                items.forEach { slice ->
                    val rawSweep = (slice.percentage / 100f) * 360f * progress
                    val sweep = (rawSweep - gapDegrees).coerceAtLeast(1.5f)
                    drawArc(
                        color = Color(slice.category.colorHex),
                        startAngle = currentStart + gapDegrees / 2f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    currentStart += rawSweep
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TOTAL SPENT",
                style = MaterialTheme.typography.labelSmall,
                color = VaultColors.InkMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            RollingOdometerAmount(
                amount = totalSpent,
                style = MaterialTheme.typography.headlineMedium,
                color = VaultColors.InkPrimary,
                exactDecimals = false
            )
            Text(
                text = "${items.sumOf { it.transactionCount }} spends",
                style = MaterialTheme.typography.bodyMedium,
                color = VaultColors.InkSecondary
            )
        }
    }
}

@Composable
fun MonthlyTrendBarChart(
    points: List<MonthlyTrendPoint>,
    selectedKey: String,
    onSelectMonth: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val maxVal = (points.maxOfOrNull { it.totalSpent } ?: 1.0).coerceAtLeast(1.0)
    val listState = rememberLazyListState()

    LaunchedEffect(points.size) {
        if (points.isNotEmpty()) {
            val targetIdx = points.indexOfFirst { it.yearMonthKey == selectedKey }
                .takeIf { it >= 0 } ?: points.lastIndex
            listState.scrollToItem((targetIdx - 3).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .height(164.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        itemsIndexed(points, key = { _, pt -> pt.yearMonthKey }) { _, pt ->
            val isSelected = pt.yearMonthKey == selectedKey
            val fraction = (pt.totalSpent / maxVal).toFloat().coerceIn(0.06f, 1f)
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(550, easing = FastOutSlowInEasing),
                label = "barHeight"
            )

            Column(
                modifier = Modifier
                    .width(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) VaultColors.InkPrimary.copy(alpha = 0.08f)
                        else Color.Transparent
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMonth(pt.yearMonthKey)
                    }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (pt.totalSpent > 0) VaultFormatters.formatCompactInr(pt.totalSpent) else "—",
                    fontFamily = CredSansFontFamily,
                    fontSize = 10.sp,
                    color = if (isSelected) VaultColors.InkPrimary else VaultColors.InkMuted,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height((88f * animatedFraction).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    if (VaultColors.isAmoledDark) {
                                        listOf(VaultColors.ChampagneGold, Color(0xFFB8860B))
                                    } else {
                                        listOf(VaultColors.InkPrimary, Color(0xFF3A3D46))
                                    }
                                )
                            } else {
                                Brush.verticalGradient(
                                    if (VaultColors.isAmoledDark) {
                                        listOf(Color(0xFF282D3D), Color(0xFF1A1E29))
                                    } else {
                                        listOf(Color(0xFFD5D9E2), Color(0xFFE8EBF0))
                                    }
                                )
                            }
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = pt.shortLabel.lowercase(),
                    fontFamily = CredSansFontFamily,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isSelected) VaultColors.InkPrimary else VaultColors.InkSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Rich Dark-Metallic CRED Skeuomorphic Credit Card with uniform CredSansFontFamily numbers
 */
@Composable
fun LuxuryInstrumentCard(
    account: AccountSpendSummary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isFlipped by remember { mutableStateOf(false) }

    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "card3DFlip"
    )
    val density = LocalDensity.current.density

    val accent = when (account.primaryRail) {
        PaymentRail.CREDIT_CARD -> VaultColors.ChampagneGold
        PaymentRail.WALLET -> Color(0xFFC084FC)
        else -> Color(0xFF34D399)
    }

    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0A0B10))
            .then(
                if (!VaultColors.isAmoledDark) {
                    Modifier.shadow(6.dp, shape, ambientColor = Color.Black.copy(alpha = 0.14f))
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.rotationY = rotationY
                    cameraDistance = 16f * density
                    clip = true
                }
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF222531),
                            Color(0xFF13151C),
                            Color(0xFF0A0B0F)
                        )
                    )
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) accent else Color(0xFF363A4B),
                    shape = shape
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .padding(20.dp)
        ) {
            if (rotationY <= 90f) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (account.primaryRail == PaymentRail.CREDIT_CARD) Icons.Outlined.CreditCard else Icons.Outlined.AccountBalance,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = account.bankName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    letterSpacing = 1.8.sp
                                )
                                Text(
                                    text = account.primaryRail.label.lowercase(),
                                    fontFamily = CredSansFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFFA6AEBC)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (account.accountLast4 == "0000") account.primaryRail.badgeText else "•••• ${account.accountLast4}",
                                    fontFamily = CredSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isFlipped = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FlipCameraAndroid,
                                    contentDescription = "Flip",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "TOTAL SPENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA6AEBC),
                                fontSize = 9.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "-${VaultFormatters.formatInr(account.totalSpent, true)}",
                                fontFamily = CredSansFontFamily,
                                fontSize = 21.sp,
                                color = Color(0xFFFF6B6B),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Text(
                            text = "${account.transactionCount} spends >",
                            fontFamily = CredSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { this.rotationY = 180f }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${account.bankName.uppercase()} • STATS",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Text(
                            text = "flip back ↺",
                            fontFamily = CredSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable { isFlipped = false }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    val avg = if (account.transactionCount > 0) account.totalSpent / account.transactionCount else 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("AVG SPEND", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA6AEBC), fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = VaultFormatters.formatInr(avg),
                                fontFamily = CredSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("CREDITS / REFUNDS", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA6AEBC), fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "+${VaultFormatters.formatInr(account.totalCredited)}",
                                fontFamily = CredSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                }
            }
        }
    }
}
