package ru.qrefka.qrcodescanner.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QrEncoderTest {

    @Test
    fun `encode produces bitmap of requested size`() {
        val size = 300
        val bitmap = QrEncoder.encode("hello", size)
        assertEquals(size, bitmap.width)
        assertEquals(size, bitmap.height)
    }

    @Test
    fun `encode produces bitmap that decodes back to original text`() {
        val text = "https://example.com"
        val bitmap = QrEncoder.encode(text, 400)

        // Decode the generated bitmap back using ZXing
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = MultiFormatReader().decode(binaryBitmap)

        assertEquals(text, result.text)
    }

    @Test
    fun `encode handles empty string by throwing`() {
        try {
            QrEncoder.encode("", 200)
            // ZXing should throw for empty input
            assertTrue("Expected exception for empty input", false)
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `encode throws WriterException when text exceeds QR capacity`() {
        try {
            QrEncoder.encode("a".repeat(5000), 200)
            assertTrue("Expected WriterException for oversized input", false)
        } catch (_: WriterException) {
            // expected
        }
    }

    @Test
    fun `encode produces non-null ARGB_8888 bitmap`() {
        val bitmap = QrEncoder.encode("test", 100)
        assertNotNull(bitmap)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun `encode produces bitmap that decodes non-ascii unicode text correctly`() {
        val text = "Привет, мир! 🚀 QR-код с UTF-8: 日本語"
        val bitmap = QrEncoder.encode(text, 400)

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val result = MultiFormatReader().decode(binaryBitmap)

        assertEquals(text, result.text)
    }

    @Test
    fun `ascii payloads are encoded without a character-set ECI`() {
        val text = "https://example.com"
        assertSameMatrix(QrEncoder.encode(text, SIZE), encodeWith(text, charset = null))
        // The two encodings differ, so the assertion above is actually load-bearing:
        // naming UTF-8 costs the ECI header and changes the code.
        assertNotSameMatrix(QrEncoder.encode(text, SIZE), encodeWith(text, charset = "UTF-8"))
    }

    @Test
    fun `non-ascii payloads are encoded with an explicit UTF-8 ECI`() {
        val text = "Привет"
        assertSameMatrix(QrEncoder.encode(text, SIZE), encodeWith(text, charset = "UTF-8"))
    }

    private fun encodeWith(text: String, charset: String?): BitMatrix {
        val hints = buildMap<EncodeHintType, Any> {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 1)
            if (charset != null) put(EncodeHintType.CHARACTER_SET, charset)
        }
        return MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, SIZE, SIZE, hints)
    }

    private fun matches(bitmap: Bitmap, matrix: BitMatrix): Boolean {
        if (bitmap.width != matrix.width || bitmap.height != matrix.height) return false
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                val dark = bitmap.getPixel(x, y) == Color.BLACK
                if (dark != matrix[x, y]) return false
            }
        }
        return true
    }

    private fun assertSameMatrix(bitmap: Bitmap, matrix: BitMatrix) =
        assertTrue("expected the same modules", matches(bitmap, matrix))

    private fun assertNotSameMatrix(bitmap: Bitmap, matrix: BitMatrix) =
        assertFalse("expected different modules", matches(bitmap, matrix))

    private companion object {
        const val SIZE = 400
    }
}
