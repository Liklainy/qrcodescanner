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
 * than a dozen glyphs are worth, and the geometry here is simple enough to state
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

private fun DrawScope.lineStroke() = Stroke(
    width = 0.1f * size.minDimension,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round
)

/** A picture frame with a mountain and a sun: "pick an image". */
@Composable
internal fun ImageGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val stroke = lineStroke()
        drawRoundRect(
            color = color,
            topLeft = Offset(0.08f * u, 0.14f * u),
            size = Size(0.84f * u, 0.72f * u),
            cornerRadius = CornerRadius(0.14f * u),
            style = stroke
        )
        val mountain = Path().apply {
            moveTo(0.18f * u, 0.72f * u)
            lineTo(0.42f * u, 0.46f * u)
            lineTo(0.58f * u, 0.62f * u)
            lineTo(0.66f * u, 0.54f * u)
            lineTo(0.82f * u, 0.72f * u)
        }
        drawPath(mountain, color = color, style = stroke)
        drawCircle(color, radius = 0.07f * u, center = Offset(0.66f * u, 0.34f * u))
    }
}

/** A circled "i", for the privacy and about entry. */
@Composable
internal fun InfoGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val stroke = lineStroke()
        drawCircle(color, radius = 0.42f * u, center = Offset(0.5f * u, 0.5f * u), style = stroke)
        drawCircle(color, radius = 0.065f * u, center = Offset(0.5f * u, 0.3f * u))
        drawLine(
            color,
            start = Offset(0.5f * u, 0.46f * u),
            end = Offset(0.5f * u, 0.72f * u),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
    }
}

/** A lightning bolt, for the torch. [filled] shows it lit. */
@Composable
internal fun FlashGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val bolt = Path().apply {
            moveTo(0.58f * u, 0.06f * u)
            lineTo(0.2f * u, 0.56f * u)
            lineTo(0.48f * u, 0.56f * u)
            lineTo(0.42f * u, 0.94f * u)
            lineTo(0.8f * u, 0.44f * u)
            lineTo(0.52f * u, 0.44f * u)
            close()
        }
        if (filled) drawPath(bolt, color = color)
        drawPath(bolt, color = color, style = lineStroke())
    }
}

/** A handset seen from the front: a phone number. */
@Composable
internal fun PhoneGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        drawRoundRect(
            color = color,
            topLeft = Offset(0.24f * u, 0.06f * u),
            size = Size(0.52f * u, 0.88f * u),
            cornerRadius = CornerRadius(0.12f * u),
            style = lineStroke()
        )
        drawRule(color, 0.78f * u, 0.42f * u, 0.16f * u)
    }
}

/** A speech bubble: a text message. */
@Composable
internal fun MessageGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val r = 0.16f * u
        val left = 0.08f * u
        val top = 0.12f * u
        val right = 0.92f * u
        val bottom = 0.7f * u
        val bubble = Path().apply {
            moveTo(0.3f * u, bottom)
            lineTo(left + r, bottom)
            arcTo(Rect(left, bottom - 2 * r, left + 2 * r, bottom), 90f, 90f, false)
            lineTo(left, top + r)
            arcTo(Rect(left, top, left + 2 * r, top + 2 * r), 180f, 90f, false)
            lineTo(right - r, top)
            arcTo(Rect(right - 2 * r, top, right, top + 2 * r), 270f, 90f, false)
            lineTo(right, bottom - r)
            arcTo(Rect(right - 2 * r, bottom - 2 * r, right, bottom), 0f, 90f, false)
            lineTo(0.5f * u, bottom)
            lineTo(0.3f * u, 0.9f * u)
            close()
        }
        drawPath(bubble, color = color, style = lineStroke())
    }
}

/** An envelope: an email. */
@Composable
internal fun MailGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val stroke = lineStroke()
        drawRoundRect(
            color = color,
            topLeft = Offset(0.06f * u, 0.18f * u),
            size = Size(0.88f * u, 0.64f * u),
            cornerRadius = CornerRadius(0.1f * u),
            style = stroke
        )
        val flap = Path().apply {
            moveTo(0.14f * u, 0.3f * u)
            lineTo(0.5f * u, 0.56f * u)
            lineTo(0.86f * u, 0.3f * u)
        }
        drawPath(flap, color = color, style = stroke)
    }
}

/** A map pin: a location. */
@Composable
internal fun PinGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val pin = Path().apply {
            moveTo(0.5f * u, 0.94f * u)
            lineTo(0.24f * u, 0.56f * u)
            arcTo(Rect(0.18f * u, 0.06f * u, 0.82f * u, 0.7f * u), 146f, 248f, false)
            close()
        }
        drawPath(pin, color = color, style = lineStroke())
        drawCircle(color, radius = 0.1f * u, center = Offset(0.5f * u, 0.38f * u))
    }
}

/** Head and shoulders: a contact card. */
@Composable
internal fun PersonGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val stroke = lineStroke()
        drawCircle(color, radius = 0.19f * u, center = Offset(0.5f * u, 0.3f * u), style = stroke)
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(0.14f * u, 0.62f * u),
            size = Size(0.72f * u, 0.6f * u),
            style = stroke
        )
    }
}

/** A calendar page with binder rings: an event. */
@Composable
internal fun CalendarGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val stroke = lineStroke()
        drawRoundRect(
            color = color,
            topLeft = Offset(0.08f * u, 0.16f * u),
            size = Size(0.84f * u, 0.76f * u),
            cornerRadius = CornerRadius(0.12f * u),
            style = stroke
        )
        drawRule(color, 0.4f * u, 0.08f * u, 0.84f * u)
        listOf(0.32f, 0.68f).forEach { x ->
            drawLine(color, Offset(x * u, 0.06f * u), Offset(x * u, 0.24f * u), stroke.width, StrokeCap.Round)
        }
    }
}

/** Vertical bars of uneven width: a linear barcode. */
@Composable
internal fun BarcodeGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        listOf(
            0.08f to 0.08f, 0.22f to 0.05f, 0.33f to 0.11f, 0.5f to 0.05f,
            0.61f to 0.08f, 0.75f to 0.05f, 0.86f to 0.08f
        ).forEach { (x, width) ->
            drawRect(color, Offset(x * u, 0.16f * u), Size(width * u, 0.68f * u))
        }
    }
}
