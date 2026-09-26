# ZXing needs no keep rules: it uses no reflection or serialization, so R8 keeps
# exactly the readers and writers the app's code reaches (MultiFormatReader pulls
# in every format in SCAN_FORMATS) and strips the rest, like any other code.

# Suppress warnings for the parts of ZXing that R8 strips.
-dontwarn com.google.zxing.**
