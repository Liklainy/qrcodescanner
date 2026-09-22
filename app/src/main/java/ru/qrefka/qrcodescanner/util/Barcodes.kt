package ru.qrefka.qrcodescanner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

/** Every format the app reads: the 2D codes plus the common retail and logistics barcodes. */
internal val SCAN_FORMATS = listOf(
    BarcodeFormat.QR_CODE,
    BarcodeFormat.DATA_MATRIX,
    BarcodeFormat.AZTEC,
    BarcodeFormat.PDF_417,
    BarcodeFormat.EAN_13,
    BarcodeFormat.EAN_8,
    BarcodeFormat.UPC_A,
    BarcodeFormat.UPC_E,
    BarcodeFormat.CODE_128,
    BarcodeFormat.CODE_39,
    BarcodeFormat.CODE_93,
    BarcodeFormat.ITF,
    BarcodeFormat.CODABAR,
)

/** The one-dimensional subset, which only reads along rows and so cares about rotation. */
internal val LINEAR_FORMATS = listOf(
    BarcodeFormat.EAN_13,
    BarcodeFormat.EAN_8,
    BarcodeFormat.UPC_A,
    BarcodeFormat.UPC_E,
    BarcodeFormat.CODE_128,
    BarcodeFormat.CODE_39,
    BarcodeFormat.CODE_93,
    BarcodeFormat.ITF,
    BarcodeFormat.CODABAR,
)

/** Retail numbering, where the digits identify a product rather than carry a message. */
internal fun isProductFormat(format: BarcodeFormat?): Boolean = when (format) {
    BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E -> true
    else -> false
}

/** Name printed on the result sheet; these are the names the formats go by everywhere. */
internal fun formatLabel(format: BarcodeFormat): String = when (format) {
    BarcodeFormat.QR_CODE -> "QR"
    BarcodeFormat.DATA_MATRIX -> "Data Matrix"
    BarcodeFormat.AZTEC -> "Aztec"
    BarcodeFormat.PDF_417 -> "PDF417"
    BarcodeFormat.EAN_13 -> "EAN-13"
    BarcodeFormat.EAN_8 -> "EAN-8"
    BarcodeFormat.UPC_A -> "UPC-A"
    BarcodeFormat.UPC_E -> "UPC-E"
    BarcodeFormat.CODE_128 -> "Code 128"
    BarcodeFormat.CODE_39 -> "Code 39"
    BarcodeFormat.CODE_93 -> "Code 93"
    BarcodeFormat.ITF -> "ITF"
    BarcodeFormat.CODABAR -> "Codabar"
    else -> format.name
}

/** Longest side a picked image is decoded at; phone photos are far larger than a code needs. */
private const val MAX_IMAGE_SIDE = 2048

/**
 * Finds a code in the image at [uri], or null if there is none or the image cannot
 * be read. Blocking; call it off the main thread.
 *
 * A still image gets the thorough treatment a camera frame cannot afford: TRY_HARDER,
 * inverted (light-on-dark) codes, a second binarizer, a smaller copy for photos where
 * the code fills the frame, and a quarter turn for barcodes that were photographed
 * sideways.
 */
internal fun decodeImage(context: Context, uri: Uri): Result? {
    val bitmap = try {
        loadBitmap(context, uri)
    } catch (_: Exception) {
        null
    } ?: return null
    val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to SCAN_FORMATS,
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.ALSO_INVERTED to true,
            )
        )
    }
    val candidates = sequence {
        yield(bitmap)
        if (bitmap.width > 800 || bitmap.height > 800) {
            yield(Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true))
        }
        yield(Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(90f) }, true))
    }
    return candidates.firstNotNullOfOrNull { decodeBitmap(reader, it) }
}

private fun decodeBitmap(reader: MultiFormatReader, bitmap: Bitmap): Result? {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
    for (binary in listOf(BinaryBitmap(HybridBinarizer(source)), BinaryBitmap(GlobalHistogramBinarizer(source)))) {
        try {
            return reader.decodeWithState(binary)
        } catch (_: NotFoundException) {
        } catch (_: ReaderException) {
        } finally {
            reader.reset()
        }
    }
    return null
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder also applies the EXIF rotation, so the image is upright.
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_IMAGE_SIDE) {
                val scale = MAX_IMAGE_SIDE.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_SIDE) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}
