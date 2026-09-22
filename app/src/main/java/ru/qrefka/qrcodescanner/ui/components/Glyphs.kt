package ru.qrefka.qrcodescanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable

/**
 * The handful of marks this app needs, drawn with the Canvas primitives rather than
 * pulled from material-icons-extended - that artifact would add far more to the APK
 * than four glyphs are worth, and the geometry here is simple enough to state
 * directly. Everything is laid out in a 0..1 square and scaled to whatever size the
 * caller gives the modifier.
 */

/** The app's own emblem: three QR finder patterns plus a scatter of modules. */
@Composable
internal fun QrMark(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        fun finder(left: Float, top: Float) {
            val outer = 0.34f * u
            drawRoundRect(
                color = color,
                topLeft = Offset(left * u, top * u),
                size = Size(outer, outer),
                cornerRadius = CornerRadius(0.10f * u),
                style = Stroke(width = 0.075f * u)
            )
            val inner = 0.14f * u
            drawRoundRect(
                color = color,
                topLeft = Offset(left * u + (outer - inner) / 2, top * u + (outer - inner) / 2),
                size = Size(inner, inner),
                cornerRadius = CornerRadius(0.04f * u)
            )
        }
        finder(0.04f, 0.04f)
        finder(0.62f, 0.04f)
        finder(0.04f, 0.62f)

        // Data modules in the free quadrant, so the mark reads as a code and not as
        // three unrelated squares.
        val m = 0.11f * u
        val r = CornerRadius(0.03f * u)
        listOf(
            0.62f to 0.62f, 0.85f to 0.62f, 0.62f to 0.85f, 0.85f to 0.85f, 0.735f to 0.735f
        ).forEach { (x, y) ->
            drawRoundRect(color, Offset(x * u, y * u), Size(m, m), r)
        }
    }
}

/** Broadcast arcs over a dot. */
@Composable
internal fun WifiGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val origin = Offset(0.5f * u, 0.82f * u)
        val stroke = Stroke(width = 0.1f * u, cap = StrokeCap.Round)
        listOf(0.28f, 0.5f, 0.72f).forEach { radius ->
            val r = radius * u
            drawArc(
                color = color,
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(origin.x - r, origin.y - r),
                size = Size(r * 2, r * 2),
                style = stroke
            )
        }
        drawCircle(color, radius = 0.075f * u, center = origin)
    }
}

/** A frame with an arrow leaving it: "this opens somewhere else". */
@Composable
internal fun LinkGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val stroke = Stroke(
            width = 0.1f * u,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val left = 0.06f * u
        val top = 0.32f * u
        val right = 0.68f * u
        val bottom = 0.94f * u
        val r = 0.15f * u

        // A rounded rect with its top-right corner left open, traced counter-clockwise
        // from the middle of the top edge. Drawing a closed rect instead would put a
        // corner right where the arrow crosses, and the two would read as one blob.
        val frame = Path().apply {
            moveTo(0.40f * u, top)
            lineTo(left + r, top)
            arcTo(Rect(left, top, left + 2 * r, top + 2 * r), 270f, -90f, false)
            lineTo(left, bottom - r)
            arcTo(Rect(left, bottom - 2 * r, left + 2 * r, bottom), 180f, -90f, false)
            lineTo(right - r, bottom)
            arcTo(Rect(right - 2 * r, bottom - 2 * r, right, bottom), 90f, -90f, false)
            lineTo(right, 0.62f * u)
        }
        drawPath(frame, color = color, style = stroke)

        val arrow = Path().apply {
            moveTo(0.40f * u, 0.60f * u)
            lineTo(0.92f * u, 0.08f * u)
            moveTo(0.62f * u, 0.08f * u)
            lineTo(0.92f * u, 0.08f * u)
            lineTo(0.92f * u, 0.38f * u)
        }
        drawPath(arrow, color = color, style = stroke)
    }
}

/** Three ragged lines, the universal shorthand for a block of text. */
@Composable
internal fun TextGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        listOf(0.26f to 0.8f, 0.5f to 0.8f, 0.74f to 0.48f).forEach { (y, width) ->
            drawRule(color, y * u, 0.1f * u, width * u)
        }
    }
}

private fun DrawScope.drawRule(color: Color, y: Float, x: Float, width: Float) {
    val thickness = size.minDimension * 0.1f
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y - thickness / 2),
        size = Size(width, thickness),
        cornerRadius = CornerRadius(thickness / 2)
    )
}
