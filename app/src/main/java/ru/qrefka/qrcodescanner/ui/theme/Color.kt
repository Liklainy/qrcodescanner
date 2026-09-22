package ru.qrefka.qrcodescanner.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * A full Material 3 scheme built around an aqua-teal seed with an indigo accent.
 *
 * Every role is spelled out - including the surface container tones - because the
 * defaults that ship with [lightColorScheme] are neutral grey, and the tinted
 * containers are what give cards, sheets and the tab pill their depth.
 */
internal val LightColors = lightColorScheme(
    primary = Color(0xFF00807A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA5F3EA),
    onPrimaryContainer = Color(0xFF00201E),
    inversePrimary = Color(0xFF5CD6C8),
    secondary = Color(0xFF4B635F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8E3),
    onSecondaryContainer = Color(0xFF06201D),
    tertiary = Color(0xFF4F5DDB),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1E0FF),
    onTertiaryContainer = Color(0xFF0A1060),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FBF9),
    onBackground = Color(0xFF171D1C),
    surface = Color(0xFFF5FBF9),
    onSurface = Color(0xFF171D1C),
    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    surfaceTint = Color(0xFF00807A),
    inverseSurface = Color(0xFF2B3231),
    inverseOnSurface = Color(0xFFECF2F0),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF5FBF9),
    surfaceDim = Color(0xFFD5DBDA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F3),
    surfaceContainer = Color(0xFFE9F0EE),
    surfaceContainerHigh = Color(0xFFE4EAE8),
    surfaceContainerHighest = Color(0xFFDEE4E2),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF5CD6C8),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFA5F3EA),
    inversePrimary = Color(0xFF00807A),
    secondary = Color(0xFFB1CCC7),
    onSecondary = Color(0xFF1C3531),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCDE8E3),
    tertiary = Color(0xFFBEC2FF),
    onTertiary = Color(0xFF1E2578),
    tertiaryContainer = Color(0xFF3742A8),
    onTertiaryContainer = Color(0xFFE1E0FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDEE4E2),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDEE4E2),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    surfaceTint = Color(0xFF5CD6C8),
    inverseSurface = Color(0xFFDEE4E2),
    inverseOnSurface = Color(0xFF2B3231),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4947),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF343B39),
    surfaceDim = Color(0xFF0E1514),
    surfaceContainerLowest = Color(0xFF090F0E),
    surfaceContainerLow = Color(0xFF171D1C),
    surfaceContainer = Color(0xFF1B2120),
    surfaceContainerHigh = Color(0xFF252B2A),
    surfaceContainerHighest = Color(0xFF303635),
)
