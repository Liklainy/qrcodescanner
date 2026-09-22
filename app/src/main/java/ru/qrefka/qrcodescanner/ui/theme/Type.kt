package ru.qrefka.qrcodescanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val Default = Typography()

/**
 * The platform sans at tighter tracking and heavier weights than the Material
 * defaults. No font file is bundled - the APK stays the size it was, and the
 * headings still read as deliberate rather than stock.
 */
internal val AppTypography = Default.copy(
    displaySmall = Default.displaySmall.tight(FontWeight.SemiBold, (-0.5).sp),
    headlineLarge = Default.headlineLarge.tight(FontWeight.SemiBold, (-0.5).sp),
    headlineMedium = Default.headlineMedium.tight(FontWeight.SemiBold, (-0.4).sp),
    headlineSmall = Default.headlineSmall.tight(FontWeight.SemiBold, (-0.3).sp),
    titleLarge = Default.titleLarge.tight(FontWeight.SemiBold, (-0.2).sp),
    titleMedium = Default.titleMedium.tight(FontWeight.SemiBold, 0.sp),
    bodyLarge = Default.bodyLarge.copy(lineHeight = 26.sp),
    labelLarge = Default.labelLarge.tight(FontWeight.SemiBold, 0.1.sp),
    labelMedium = Default.labelMedium.tight(FontWeight.Medium, 0.6.sp),
    labelSmall = Default.labelSmall.tight(FontWeight.Medium, 0.6.sp),
)

private fun TextStyle.tight(weight: FontWeight, tracking: TextUnit) =
    copy(fontFamily = FontFamily.SansSerif, fontWeight = weight, letterSpacing = tracking)
