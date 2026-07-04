# ZXing — keep only classes used by the app
# Decoding: MultiFormatReader and its reader chain
-keep class com.google.zxing.MultiFormatReader { *; }
-keep class com.google.zxing.qrcode.QRCodeReader { *; }
-keep class com.google.zxing.BinaryBitmap { *; }
-keep class com.google.zxing.common.HybridBinarizer { *; }
-keep class com.google.zxing.PlanarYUVLuminanceSource { *; }
-keep class com.google.zxing.Result { *; }

# Encoding: MultiFormatWriter for QR generation
-keep class com.google.zxing.MultiFormatWriter { *; }
-keep class com.google.zxing.qrcode.QRCodeWriter { *; }
-keep class com.google.zxing.common.BitMatrix { *; }

# Enums and hints referenced in code
-keep class com.google.zxing.BarcodeFormat { *; }
-keep class com.google.zxing.EncodeHintType { *; }
-keep class com.google.zxing.qrcode.decoder.ErrorCorrectionLevel { *; }

# Exceptions
-keep class com.google.zxing.NotFoundException { *; }
-keep class com.google.zxing.WriterException { *; }

# Suppress warnings for unused ZXing classes that R8 may strip
-dontwarn com.google.zxing.**
