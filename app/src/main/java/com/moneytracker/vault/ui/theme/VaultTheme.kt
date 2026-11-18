package com.moneytracker.vault.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.moneytracker.vault.R
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dual-Mode CRED Color System:
 * - Default: True #000000 Pitch-Black AMOLED Mode with high-contrast readable Obsidian cards & crisp text
 * - Toggleable: Crisp Alabaster White (#FFFFFF) Light Mode
 */
object VaultColors {
    private const val THEME_PREFS = "vault_ui_theme_prefs"
    private const val KEY_AMOLED_DARK = "is_amoled_dark"

    var isAmoledDark by mutableStateOf(true)
        private set

    fun initTheme(context: Context) {
        val prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        isAmoledDark = prefs.getBoolean(KEY_AMOLED_DARK, true)
    }

    fun toggleTheme(context: Context) {
        val next = !isAmoledDark
        isAmoledDark = next
        context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AMOLED_DARK, next)
            .apply()
    }

    // Pure #000000 AMOLED Lock & Dock Constants
    val AmoledBlack = Color(0xFF000000)
    val DockOrbSurface = Color(0xFF16181F)
    val DockOrbBorder = Color(0xFF2F3341)

    // Dynamic Sheet & Card Surfaces
    val CredWhite: Color
        get() = if (isAmoledDark) Color(0xFF000000) else Color(0xFFFFFFFF)

    val CardSurface: Color
        get() = if (isAmoledDark) Color(0xFF0E1015) else Color(0xFFFFFFFF)

    val CredCanvasSubtle: Color
        get() = if (isAmoledDark) Color(0xFF151821) else Color(0xFFF8F9FB)

    val CredMutedFill: Color
        get() = if (isAmoledDark) Color(0xFF1B1F2B) else Color(0xFFF2F4F8)

    val CredBorder: Color
        get() = if (isAmoledDark) Color(0xFF242836) else Color(0xFFE4E7EE)

    val CredBorderStrong: Color
        get() = if (isAmoledDark) Color(0xFF343A4D) else Color(0xFFD5D9E2)

    val BlackedOutCard: Color
        get() = if (isAmoledDark) Color(0xFF07080B) else Color(0xFF141519)

    // Dynamic High-Contrast Readable Ink Colors
    val InkPrimary: Color
        get() = if (isAmoledDark) Color(0xFFF8FAFC) else Color(0xFF141518)

    val OnInkPrimary: Color
        get() = if (isAmoledDark) Color(0xFF090A0D) else Color(0xFFFFFFFF)

    val InkSecondary: Color
        get() = if (isAmoledDark) Color(0xFFB0B7C6) else Color(0xFF646975)

    val InkMuted: Color
        get() = if (isAmoledDark) Color(0xFF848C9E) else Color(0xFF949AA7)

    // Semantic Financial Accents (High-contrast on both #000000 AMOLED & #FFFFFF Light)
    val DebitRed: Color
        get() = if (isAmoledDark) Color(0xFFFF5C5C) else Color(0xFFE53935)

    val CreditGreen: Color
        get() = if (isAmoledDark) Color(0xFF34D399) else Color(0xFF059669)

    val SelfYellowText: Color
        get() = if (isAmoledDark) Color(0xFFFACC15) else Color(0xFFB45309)

    val SelfYellowBorder: Color
        get() = if (isAmoledDark) Color(0xFFEAB308).copy(alpha = 0.65f) else Color(0xFFFACC15)

    val SelfYellowBg: Color
        get() = if (isAmoledDark) Color(0xFF1D1807) else Color(0xFFFFFBEB)

    val CardBillBlue: Color
        get() = if (isAmoledDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val CardBillBg: Color
        get() = if (isAmoledDark) Color(0xFF081926) else Color(0xFFF0F9FF)

    // Month Capsule & Brand Accents
    val JackpotPinkBg: Color
        get() = if (isAmoledDark) Color(0xFF161822) else Color(0xFFFFF4F7)

    val JackpotPinkBorder: Color
        get() = if (isAmoledDark) Color(0xFF32374A) else Color(0xFFF5C6D6)

    val HeroGoldStart = Color(0xFF591914)
    val HeroGoldMid = Color(0xFFC86D28)
    val HeroGoldEnd = Color(0xFFF6CA56)
    val ChampagneGold = Color(0xFFE8C547)

    val RoyalViolet: Color
        get() = if (isAmoledDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
}

@OptIn(ExperimentalTextApi::class)
val CredSansFontFamily = FontFamily(
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        resId = R.font.plus_jakarta_sans,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

@OptIn(ExperimentalTextApi::class)
val CredSerifFontFamily = FontFamily(
    Font(
        resId = R.font.playfair_display,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.playfair_display,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

private val CredTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = CredSansFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontFamily = CredSansFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum"
    ),
    titleLarge = TextStyle(
        fontFamily = CredSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = CredSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = CredSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = CredSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = CredSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.8.sp
    )
)

@Composable
fun VaultTheme(content: @Composable () -> Unit) {
    val colorScheme = if (VaultColors.isAmoledDark) {
        darkColorScheme(
            primary = VaultColors.InkPrimary,
            onPrimary = VaultColors.OnInkPrimary,
            secondary = VaultColors.CreditGreen,
            onSecondary = Color.Black,
            tertiary = VaultColors.CardBillBlue,
            background = VaultColors.CredWhite,
            onBackground = VaultColors.InkPrimary,
            surface = VaultColors.CardSurface,
            onSurface = VaultColors.InkPrimary,
            surfaceVariant = VaultColors.CredCanvasSubtle,
            onSurfaceVariant = VaultColors.InkSecondary,
            error = VaultColors.DebitRed
        )
    } else {
        lightColorScheme(
            primary = VaultColors.InkPrimary,
            onPrimary = VaultColors.OnInkPrimary,
            secondary = VaultColors.CreditGreen,
            onSecondary = Color.White,
            tertiary = VaultColors.CardBillBlue,
            background = VaultColors.CredWhite,
            onBackground = VaultColors.InkPrimary,
            surface = VaultColors.CardSurface,
            onSurface = VaultColors.InkPrimary,
            surfaceVariant = VaultColors.CredCanvasSubtle,
            onSurfaceVariant = VaultColors.InkSecondary,
            error = VaultColors.DebitRed
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CredTypography,
        content = content
    )
}

object VaultFormatters {
    private val inrWhole = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    private val inrExact = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    private val dayHeaderFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy • hh:mm a", Locale.ENGLISH)

    fun formatInr(amount: Double, exactDecimals: Boolean = false): String {
        return if (exactDecimals && amount % 1.0 != 0.0) {
            inrExact.format(amount)
        } else {
            inrWhole.format(amount)
        }
    }

    fun formatCompactInr(amount: Double): String {
        return when {
            amount >= 10_000_000 -> "₹%.2fCr".format(Locale.US, amount / 10_000_000.0)
            amount >= 100_000 -> "₹%.1fL".format(Locale.US, amount / 100_000.0)
            amount >= 1_000 -> "₹%.1fk".format(Locale.US, amount / 1_000.0)
            else -> "₹${amount.toInt()}"
        }
    }

    fun formatDayGroup(epochMillis: Long): String {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        val today = Instant.now().atZone(zone).toLocalDate()
        return when (date) {
            today -> "TODAY"
            today.minusDays(1) -> "YESTERDAY"
            else -> date.format(dayHeaderFormatter).uppercase(Locale.ENGLISH)
        }
    }

    fun formatFullDateTime(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(fullDateFormatter)
    }
}
