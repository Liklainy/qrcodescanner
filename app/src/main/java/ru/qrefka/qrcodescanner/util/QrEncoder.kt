package ru.qrefka.qrcodescanner.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrEncoder {
    fun encode(text: String, sizePx: Int): Bitmap {
        // zxing prepends an ECI segment to every byte-mode code whenever CHARACTER_SET
        // is present, whether or not the payload needs one, and scanners that do not
        // implement ECI then fail on codes they would otherwise read. So the hint goes
        // in only for non-ASCII text, where an explicit UTF-8 ECI is what makes the
        // payload unambiguous; plain ASCII is read the same way with or without it.
        val hints = buildMap<EncodeHintType, Any> {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 1)
            if (text.any { it.code > 0x7F }) put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val w = matrix.width
        val h = matrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
}
