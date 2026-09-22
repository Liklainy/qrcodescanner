package ru.qrefka.qrcodescanner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Rounder than the Material defaults across the board: buttons become full-height
 * pills, cards and sheets get a 24-28dp radius. This is the single biggest lever
 * on how current the app reads, since every component pulls its shape from here.
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
