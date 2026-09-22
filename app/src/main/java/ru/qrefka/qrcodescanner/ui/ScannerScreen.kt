package ru.qrefka.qrcodescanner.ui

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.PersistableBundle
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.qrefka.qrcodescanner.R
import ru.qrefka.qrcodescanner.ui.components.FillScrollColumn
import ru.qrefka.qrcodescanner.ui.components.LinkGlyph
import ru.qrefka.qrcodescanner.ui.components.QrMark
import ru.qrefka.qrcodescanner.ui.components.TextGlyph
import ru.qrefka.qrcodescanner.ui.components.WifiGlyph
import ru.qrefka.qrcodescanner.ui.components.BarcodeGlyph
import ru.qrefka.qrcodescanner.ui.components.CalendarGlyph
import ru.qrefka.qrcodescanner.ui.components.FlashGlyph
import ru.qrefka.qrcodescanner.ui.components.ImageGlyph
import ru.qrefka.qrcodescanner.ui.components.MailGlyph
import ru.qrefka.qrcodescanner.ui.components.MessageGlyph
import ru.qrefka.qrcodescanner.ui.components.PersonGlyph
import ru.qrefka.qrcodescanner.ui.components.PhoneGlyph
import ru.qrefka.qrcodescanner.ui.components.PinGlyph
import ru.qrefka.qrcodescanner.util.LINEAR_FORMATS
import ru.qrefka.qrcodescanner.util.SCAN_FORMATS
import ru.qrefka.qrcodescanner.util.ScanContent
import ru.qrefka.qrcodescanner.util.WifiCredentials
import ru.qrefka.qrcodescanner.util.WifiSecurity
import ru.qrefka.qrcodescanner.util.decodeImage
import ru.qrefka.qrcodescanner.util.formatLabel
import ru.qrefka.qrcodescanner.util.isProductFormat
import ru.qrefka.qrcodescanner.util.parseScanContent
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @param bottomReserve space the floating tab switcher takes at the bottom of the
 *   window; the camera runs underneath it, everything else keeps clear of it.
 */
@Composable
fun ScannerScreen(bottomReserve: Dp = 0.dp) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    var autoRequested by rememberSaveable { mutableStateOf(false) }
    var deniedPermanently by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            val activity = context.findActivity()
            deniedPermanently = activity != null &&
                    !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !autoRequested) {
            autoRequested = true
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Pick up a grant made in app settings when the user returns to the app.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasCameraPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The result lives here rather than with the camera so that a code picked from
    // the gallery shows up even when camera access was refused.
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var resultFormat by rememberSaveable { mutableStateOf<String?>(null) }
    // Claimed on the analyzer thread by the first decode and released when the sheet
    // is dismissed. The camera stays bound while the sheet is up, so without this a
    // code left in frame would post a Runnable per frame for nothing. Seeded from
    // [result] because that survives a tab switch while this flag does not.
    val handled = remember { AtomicBoolean(result != null) }
    val showResult: (Result) -> Unit = {
        handled.set(true)
        result = it.text
        resultFormat = it.barcodeFormat.name
    }

    val scope = rememberCoroutineScope()
    // The photo picker needs no storage permission; where it is unavailable the
    // contract falls back to the system document picker, which needs none either.
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val appContext = context.applicationContext
        scope.launch {
            val decoded = withContext(Dispatchers.Default) { decodeImage(appContext, uri) }
            if (decoded != null) showResult(decoded) else toast(context, R.string.no_code_in_image)
        }
    }
    val pickImage = {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    when {
        hasPermission -> ScannerContent(
            bottomReserve = bottomReserve,
            handled = handled,
            onDecoded = showResult,
            onPickImage = pickImage
        )
        else -> PermissionPrompt(
            bottomReserve = bottomReserve,
            openSettings = deniedPermanently,
            onPickImage = pickImage,
            onRequest = {
                if (deniedPermanently) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            }
        )
    }

    result?.let { text ->
        ResultSheet(
            text = text,
            format = resultFormat?.let { runCatching { BarcodeFormat.valueOf(it) }.getOrNull() },
            onDismiss = {
                result = null
                resultFormat = null
                handled.set(false)
            }
        )
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

@Composable
private fun PermissionPrompt(
    bottomReserve: Dp,
    openSettings: Boolean,
    onPickImage: () -> Unit,
    onRequest: () -> Unit
) {
    // Scrolls where the prompt is taller than the space above the tab switcher
    // (landscape, large fonts) instead of spilling past both edges.
    FillScrollColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp)
            .padding(bottom = bottomReserve),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(104.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                QrMark(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(46.dp)
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.camera_perm_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.camera_perm_rationale),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth().height(ActionHeight)
        ) {
            Text(stringResource(if (openSettings) R.string.open_settings else R.string.grant_camera))
        }
        Spacer(Modifier.height(10.dp))
        // Reading a saved image needs no camera, so it stays on offer without access.
        FilledTonalButton(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth().height(ActionHeight)
        ) { Text(stringResource(R.string.scan_from_image)) }
    }
}

/** Buttons are taller than the Material minimum; it suits the roomier layout. */
private val ActionHeight = 54.dp

@Composable
private fun ScannerContent(
    bottomReserve: Dp,
    handled: AtomicBoolean,
    onDecoded: (Result) -> Unit,
    onPickImage: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val currentOnDecoded by rememberUpdatedState(onDecoded)
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf<ZoomState?>(null) }

    // The Runnable below captures the current haptics; dropping any that are still
    // queued keeps a decode from buzzing after the user has left the scanner tab.
    DisposableEffect(mainHandler) {
        onDispose { mainHandler.removeCallbacksAndMessages(null) }
    }

    // Mirrors the camera's own torch and zoom state rather than tracking taps, so the
    // button stays right when CameraX turns the torch off on its own (the app going to
    // the background, the camera being rebound after a tab switch).
    DisposableEffect(camera, lifecycleOwner) {
        val info = camera?.cameraInfo
        val torchObserver = Observer<Int> { torchOn = it == TorchState.ON }
        val zoomObserver = Observer<ZoomState> { zoom = it }
        info?.torchState?.observe(lifecycleOwner, torchObserver)
        info?.zoomState?.observe(lifecycleOwner, zoomObserver)
        onDispose {
            info?.torchState?.removeObserver(torchObserver)
            info?.zoomState?.removeObserver(zoomObserver)
            torchOn = false
            zoom = null
        }
    }

    fun setZoom(ratio: Float) {
        val state = zoom ?: return
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Pinch to zoom, double tap to jump between 1x and 2x.
            .pointerInput(Unit) {
                detectTransformGestures { _, _, gestureZoom, _ ->
                    zoom?.let { setZoom(it.zoomRatio * gestureZoom) }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    zoom?.let { setZoom(if (it.zoomRatio > it.minZoomRatio + 0.05f) it.minZoomRatio else 2f) }
                })
            }
    ) {
        CameraPreview(
            onCamera = { camera = it },
            onDecoded = { decoded ->
                if (handled.compareAndSet(false, true)) {
                    mainHandler.post {
                        currentOnDecoded(decoded)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
        )
        ViewfinderOverlay()

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            OverlayButton(
                label = stringResource(R.string.scan_from_image),
                onClick = onPickImage
            ) { tint -> ImageGlyph(tint, Modifier.size(22.dp)) }
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                OverlayButton(
                    label = stringResource(if (torchOn) R.string.torch_off else R.string.torch_on),
                    active = torchOn,
                    onClick = { camera?.cameraControl?.enableTorch(!torchOn) }
                ) { tint -> FlashGlyph(tint, filled = torchOn, modifier = Modifier.size(22.dp)) }
            }
        }

        // A pill reads over a moving camera image far better than text with a drop
        // shadow, and parking it above the tab switcher keeps the viewfinder clear.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = bottomReserve + 14.dp)
                .padding(horizontal = 24.dp)
        ) {
            zoom?.takeIf { it.zoomRatio > it.minZoomRatio + 0.05f }?.let {
                OverlayPill(String.format(LocalConfiguration.current.locales[0], "%.1f\u00d7", it.zoomRatio))
            }
            OverlayPill(stringResource(R.string.scan_instruction))
        }
    }
}

@Composable
private fun OverlayPill(text: String) {
    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

/** Round button over the camera image; [active] fills it with the accent colour. */
@Composable
private fun OverlayButton(
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    glyph: @Composable (Color) -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f),
        modifier = Modifier
            .size(52.dp)
            .semantics { contentDescription = label }
    ) {
        Box(contentAlignment = Alignment.Center) {
            glyph(if (active) MaterialTheme.colorScheme.onPrimary else Color.White)
        }
    }
}

@Composable
private fun CameraPreview(onCamera: (Camera?) -> Unit, onDecoded: (Result) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnDecoded by rememberUpdatedState(onDecoded)
    val currentOnCamera by rememberUpdatedState(onCamera)
    val previewView = remember(context) {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val executor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var disposed = false
        providerFuture.addListener({
            if (disposed) return@addListener
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(executor, BarcodeAnalyzer { currentOnDecoded(it) })
                }
            try {
                provider.unbindAll()
                val cameraSelector = when {
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }
                if (cameraSelector != null) {
                    currentOnCamera(
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview, analysis
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            currentOnCamera(null)
            // The provider future may not be resolved yet; unbind once it is.
            providerFuture.addListener({
                runCatching { providerFuture.get().unbindAll() }
            }, ContextCompat.getMainExecutor(context))
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun ViewfinderOverlay() {
    val bracketColor = MaterialTheme.colorScheme.primary
    // A slow sweep across the cutout, so the viewfinder looks alive while it waits.
    val sweep = rememberInfiniteTransition(label = "viewfinder").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Scales with the window instead of a fixed 260dp, so the cutout stays
        // proportionate on a tablet and does not crowd the edges on a small phone.
        val viewfinderSize = (size.minDimension * 0.7f).coerceAtMost(340.dp.toPx())
        val cx = size.width / 2
        val cy = size.height / 2
        val left = cx - viewfinderSize / 2
        val top = cy - viewfinderSize / 2
        val right = cx + viewfinderSize / 2
        val bottom = cy + viewfinderSize / 2
        val cr = 30.dp.toPx()
        val bl = 44.dp.toPx()
        val bw = 4.dp.toPx()

        val cutout = RoundRect(left, top, right, bottom, cr, cr)

        // Semi-transparent mask with rounded cutout
        val maskPath = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
            addRoundRect(cutout)
            fillType = PathFillType.EvenOdd
        }
        drawPath(maskPath, color = Color.Black.copy(alpha = 0.55f))

        // Sweeping band, clipped to the cutout so it never bleeds onto the mask.
        val cutoutPath = Path().apply { addRoundRect(cutout) }
        clipPath(cutoutPath) {
            val band = 150.dp.toPx()
            val centerY = top + (bottom - top) * sweep.value
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        bracketColor.copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    startY = centerY - band / 2,
                    endY = centerY + band / 2
                ),
                topLeft = Offset(left, centerY - band / 2),
                size = Size(right - left, band)
            )
            drawLine(
                color = bracketColor.copy(alpha = 0.85f),
                start = Offset(left, centerY),
                end = Offset(right, centerY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Corner brackets following the rounded cutout
        val bracketPath = Path().apply {
            // Top-left
            moveTo(left, top + bl)
            lineTo(left, top + cr)
            arcTo(Rect(left, top, left + cr * 2, top + cr * 2), 180f, 90f, false)
            lineTo(left + bl, top)

            // Top-right
            moveTo(right - bl, top)
            lineTo(right - cr, top)
            arcTo(Rect(right - cr * 2, top, right, top + cr * 2), 270f, 90f, false)
            lineTo(right, top + bl)

            // Bottom-right
            moveTo(right, bottom - bl)
            lineTo(right, bottom - cr)
            arcTo(Rect(right - cr * 2, bottom - cr * 2, right, bottom), 0f, 90f, false)
            lineTo(right - bl, bottom)

            // Bottom-left
            moveTo(left + bl, bottom)
            lineTo(left + cr, bottom)
            arcTo(Rect(left, bottom - cr * 2, left + cr * 2, bottom), 90f, 90f, false)
            lineTo(left, bottom - bl)
        }
        drawPath(
            bracketPath,
            color = bracketColor,
            style = Stroke(width = bw, cap = StrokeCap.Round)
        )
    }
}


/**
 * Reusable analyzer with cached byte buffers to prevent 1-2 MB allocations
 * on every camera frame at 30-60 FPS.
 */
private class BarcodeAnalyzer(
    private val onDecoded: (Result) -> Unit
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to SCAN_FORMATS))
    }
    private val linearReader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to LINEAR_FORMATS))
    }
    private var buffer = ByteArray(0)
    private var rotated = ByteArray(0)

    override fun analyze(proxy: ImageProxy) {
        try {
            val plane = proxy.planes[0]
            val buf: ByteBuffer = plane.buffer
            val remaining = buf.remaining()
            if (buffer.size < remaining) {
                buffer = ByteArray(remaining)
            }
            buf.get(buffer, 0, remaining)
            val width = proxy.width
            val height = proxy.height
            val stride = plane.rowStride
            // The Y plane may be padded: rowStride is the actual length of each data row.
            val source = PlanarYUVLuminanceSource(
                buffer, stride, height, 0, 0, width, height, false
            )
            var r = decode(reader, source)
            // Linear barcodes are read along pixel rows only. The sensor is mounted in
            // landscape, so with the phone upright a barcode the user holds level runs
            // down the frame's columns; a quarter-turned copy puts it back on the rows.
            // 2D codes read at any angle and do not need the second pass.
            val rotation = proxy.imageInfo.rotationDegrees
            if (r == null && (rotation == 90 || rotation == 270)) {
                if (rotated.size < width * height) rotated = ByteArray(width * height)
                for (y in 0 until height) {
                    val row = y * stride
                    val column = height - 1 - y
                    for (x in 0 until width) {
                        rotated[x * height + column] = buffer[row + x]
                    }
                }
                r = decode(
                    linearReader,
                    PlanarYUVLuminanceSource(rotated, height, width, 0, 0, height, width, false)
                )
            }
            r?.let(onDecoded)
        } catch (_: Exception) {
        } finally {
            proxy.close()
        }
    }

    // decodeWithState, not decode: decode() begins with setHints(null), which would
    // throw away the format list set in the initializer and make every frame run
    // every reader zxing has. reset() clears per-frame state but leaves the
    // configured readers in place.
    private fun decode(reader: MultiFormatReader, source: PlanarYUVLuminanceSource): Result? {
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            reader.decodeWithState(bitmap)
        } catch (_: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    }
}


/** Button on the result sheet; the first one on a sheet is drawn as the primary action. */
private class SheetAction(@StringRes val label: Int, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultSheet(
    text: String,
    format: BarcodeFormat?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val content = remember(text, format) { parseScanContent(text, isProductFormat(format)) }
    val wifi = (content as? ScanContent.Wifi)?.credentials
    // Built up front: the intent is null for payloads the system dialog cannot
    // take, which is exactly when the sheet has to offer manual entry instead.
    val addNetwork = remember(wifi) { wifi?.let { addNetworkIntentOrNull(context, it) } }
    val canAddNetwork = addNetwork != null
    val addNetworkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { outcome -> addNetworkMessage(outcome)?.let { toast(context, it) } }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // True only on the manual path, which is the one that renders the password.
    val showsPassword = wifi != null && !canAddNetwork && wifi.password.isNotEmpty()
    val actions = when (content) {
        is ScanContent.Wifi -> emptyList()
        is ScanContent.Link -> listOf(SheetAction(R.string.open) { openLink(context, content.uri) })
        is ScanContent.Phone -> listOf(
            SheetAction(R.string.action_call) { dial(context, content.number) },
            SheetAction(R.string.action_add_contact) { addPhoneContact(context, content.number) }
        )
        is ScanContent.Sms -> listOf(SheetAction(R.string.action_send_sms) { sendSms(context, content) })
        is ScanContent.Email -> listOf(SheetAction(R.string.action_send_email) { sendEmail(context, content) })
        is ScanContent.Location -> listOf(SheetAction(R.string.action_show_on_map) { showOnMap(context, content) })
        is ScanContent.Contact -> listOf(SheetAction(R.string.action_add_contact) { addContact(context, content) })
        is ScanContent.Event -> listOf(SheetAction(R.string.action_add_event) { addEvent(context, content) })
        is ScanContent.Product -> listOf(SheetAction(R.string.action_search) { webSearch(context, content.code) })
        // Only a short line is worth a search; a paragraph is something to copy.
        is ScanContent.Text -> if (text.length <= 120 && text.lines().size == 1) {
            listOf(SheetAction(R.string.action_search) { webSearch(context, text.trim()) })
        } else {
            emptyList()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        // The sheet is its own dialog window, so FLAG_SECURE has to be set here -
        // setting it on the activity window would leave this content uncovered.
        // It keeps the password out of screenshots and recents task snapshots.
        properties = ModalBottomSheetProperties(
            securePolicy =
                if (showsPassword) SecureFlagPolicy.SecureOn else SecureFlagPolicy.Inherit
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 28.dp)
        ) {
            // QR is what everyone expects; naming the format only adds information
            // for the others.
            ResultHeader(content, format?.takeIf { it != BarcodeFormat.QR_CODE }?.let(::formatLabel))
            Spacer(Modifier.height(20.dp))
            ResultDetails(content, text, canAddNetwork)
            Spacer(Modifier.height(24.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (wifi != null) {
                    if (canAddNetwork) {
                        Button(
                            onClick = { launchAddNetwork(context, addNetworkLauncher, addNetwork) },
                            modifier = Modifier.fillMaxWidth().height(ActionHeight)
                        ) { Text(stringResource(R.string.wifi_connect)) }
                    }
                    if (wifi.password.isNotEmpty()) {
                        CopyButton(
                            label = R.string.wifi_copy_password,
                            confirmation = R.string.wifi_password_copied,
                            onCopy = { copy(context, wifi.password, sensitive = true) }
                        )
                    }
                    if (!canAddNetwork) {
                        FilledTonalButton(
                            onClick = { openWifiSettings(context) },
                            modifier = Modifier.fillMaxWidth().height(ActionHeight)
                        ) { Text(stringResource(R.string.wifi_open_settings)) }
                    }
                } else {
                    actions.forEachIndexed { index, action ->
                        if (index == 0 && content !is ScanContent.Text) {
                            Button(
                                onClick = action.onClick,
                                modifier = Modifier.fillMaxWidth().height(ActionHeight)
                            ) { Text(stringResource(action.label)) }
                        } else {
                            FilledTonalButton(
                                onClick = action.onClick,
                                modifier = Modifier.fillMaxWidth().height(ActionHeight)
                            ) { Text(stringResource(action.label)) }
                        }
                    }
                    CopyButton(
                        label = R.string.copy,
                        confirmation = R.string.copied,
                        onCopy = { copy(context, copyText(content, text)) }
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(ActionHeight)
                ) { Text(stringResource(R.string.dismiss)) }
            }
        }
    }
}

/** What Copy puts on the clipboard: the useful part where there is one, else the payload. */
private fun copyText(content: ScanContent, raw: String): String = when (content) {
    is ScanContent.Phone -> content.number
    is ScanContent.Email -> content.address
    is ScanContent.Location -> "${content.latitude}, ${content.longitude}"
    is ScanContent.Product -> content.code
    else -> raw
}

/** The readable form of the payload: named fields for structured kinds, the raw text otherwise. */
@Composable
private fun ResultDetails(content: ScanContent, raw: String, canAddNetwork: Boolean) {
    val context = LocalContext.current
    when (content) {
        is ScanContent.Wifi -> WifiDetails(content.credentials, canAddNetwork)
        is ScanContent.Link, is ScanContent.Text -> PayloadCard(raw)
        is ScanContent.Phone -> Headline(content.number)
        is ScanContent.Product -> Headline(content.code)
        is ScanContent.Sms -> Fields(content.number, listOf(content.body))
        is ScanContent.Email -> Fields(content.address, listOf(content.subject, content.body))
        is ScanContent.Location -> {
            val coordinates = "${content.latitude}, ${content.longitude}"
            if (content.label.isBlank()) Headline(coordinates)
            else Fields(content.label, listOf(coordinates))
        }
        is ScanContent.Contact -> Fields(
            content.name.ifBlank { content.phones.firstOrNull() ?: content.emails.firstOrNull().orEmpty() },
            listOf(listOf(content.title, content.organization).filter(String::isNotBlank).joinToString(", ")) +
                    content.phones + content.emails +
                    listOf(content.address, content.url, content.note)
        )
        is ScanContent.Event -> Fields(
            content.title,
            listOf(formatEventTime(context, content).orEmpty(), content.location, content.description)
        )
    }
}

/** A large, selectable title - the one value the payload is about. */
@Composable
private fun Headline(text: String) {
    SelectionContainer {
        Text(
            text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** [title] as a headline, then the non-blank [lines] in a card beneath it. */
@Composable
private fun Fields(title: String, lines: List<String>) {
    val shown = lines.filter(String::isNotBlank)
    Column(Modifier.fillMaxWidth()) {
        if (title.isNotBlank()) Headline(title)
        if (shown.isNotEmpty()) {
            if (title.isNotBlank()) Spacer(Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        shown.forEach {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Tinted badge plus a plain-language name for what was scanned, and its format if notable. */
@Composable
private fun ResultHeader(content: ScanContent, formatName: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val tint = MaterialTheme.colorScheme.onPrimaryContainer
                val glyph = Modifier.size(21.dp)
                when (content) {
                    is ScanContent.Wifi -> WifiGlyph(tint, glyph)
                    is ScanContent.Link -> LinkGlyph(tint, glyph)
                    is ScanContent.Text -> TextGlyph(tint, glyph)
                    is ScanContent.Phone -> PhoneGlyph(tint, glyph)
                    is ScanContent.Sms -> MessageGlyph(tint, glyph)
                    is ScanContent.Email -> MailGlyph(tint, glyph)
                    is ScanContent.Location -> PinGlyph(tint, glyph)
                    is ScanContent.Contact -> PersonGlyph(tint, glyph)
                    is ScanContent.Event -> CalendarGlyph(tint, glyph)
                    is ScanContent.Product -> BarcodeGlyph(tint, glyph)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                stringResource(
                    when (content) {
                        is ScanContent.Wifi -> R.string.wifi_network
                        is ScanContent.Link -> R.string.result_type_link
                        is ScanContent.Text -> R.string.result_type_text
                        is ScanContent.Phone -> R.string.result_type_phone
                        is ScanContent.Sms -> R.string.result_type_sms
                        is ScanContent.Email -> R.string.result_type_email
                        is ScanContent.Location -> R.string.result_type_location
                        is ScanContent.Contact -> R.string.result_type_contact
                        is ScanContent.Event -> R.string.result_type_event
                        is ScanContent.Product -> R.string.result_type_product
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (formatName != null) {
                Text(
                    formatName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** The raw payload, in a tonal card that scrolls once it outgrows its box. */
@Composable
private fun PayloadCard(text: String) {
    val scrollState = rememberScrollState()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        SelectionContainer {
            Text(
                text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(scrollState)
                    .padding(18.dp)
            )
        }
    }
}

/**
 * Summary of a scanned `WIFI:` payload. The password is shown only when the network
 * has to be added by hand - otherwise the system dialog takes it and there is no
 * reason to put it on screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WifiDetails(wifi: WifiCredentials, canAddNetwork: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        SelectionContainer {
            Text(
                wifi.ssid,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(12.dp))
        // FlowRow, not Row: a long security name next to "Hidden network" overflows
        // a narrow screen in some locales.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(stringResource(securityLabel(wifi.security)))
            if (wifi.hidden) InfoChip(stringResource(R.string.wifi_hidden))
        }
        if (!canAddNetwork) {
            if (wifi.password.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                PayloadCard(wifi.password)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.wifi_manual_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoChip(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/** Copy button that flips to a confirmation label for a moment after each tap. */
@Composable
private fun CopyButton(
    @StringRes label: Int,
    @StringRes confirmation: Int,
    onCopy: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    FilledTonalButton(
        onClick = {
            onCopy()
            copied = true
        },
        modifier = Modifier.fillMaxWidth().height(ActionHeight)
    ) { Text(stringResource(if (copied) confirmation else label)) }
}

@StringRes
private fun securityLabel(security: WifiSecurity): Int = when (security) {
    WifiSecurity.OPEN -> R.string.wifi_security_open
    WifiSecurity.WEP -> R.string.wifi_security_wep
    WifiSecurity.WPA -> R.string.wifi_security_wpa
    WifiSecurity.SAE -> R.string.wifi_security_sae
    WifiSecurity.ENTERPRISE -> R.string.wifi_security_enterprise
}

/**
 * Intent that hands the scanned credentials to the system "add network" dialog,
 * which saves the network to the user's Wi-Fi list after they confirm. Settings owns
 * that dialog and the write, so this needs no permission of our own - the point of
 * using it over WifiManager.addNetworkSuggestions, which would cost CHANGE_WIFI_STATE
 * and only makes the network an auto-join candidate rather than a saved one.
 *
 * Null when the payload cannot be expressed as a [WifiNetworkSuggestion]: before
 * API 30 there is no such dialog, the builder covers neither WEP nor enterprise
 * networks, and it rejects a passphrase outside the lengths WPA allows. Callers show
 * the credentials for manual entry instead.
 */
private fun addNetworkIntentOrNull(context: Context, wifi: WifiCredentials): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    val suggestion = suggestionOrNull(wifi) ?: return null
    val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
        .putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion))
    // The extras carry the passphrase in the clear, so aim the intent at Settings
    // rather than letting it resolve by action alone. Nothing stops another app from
    // declaring the same action, and WifiNetworkSuggestion.getPassphrase() is public.
    // When no single system handler can be confirmed the intent goes out implicitly:
    // the alternative would be to drop the feature on any build whose resolver we
    // cannot read, which costs every user on that device to close a maybe.
    return intent.apply { systemHandler(context, this)?.let(::setComponent) }
}

/**
 * The one system activity handling [intent], or null if there is not exactly one.
 *
 * Uses queryIntentActivities rather than resolveActivity on purpose: with several
 * matches and no user default, resolveActivity answers with the chooser itself
 * (package "android"), which would pass the system check below and pin the intent to
 * the very disambiguation dialog this exists to avoid. FLAG_UPDATED_SYSTEM_APP keeps
 * an OEM Settings updated through a store in scope.
 */
private fun systemHandler(context: Context, intent: Intent): ComponentName? {
    val system = queryHandlers(context.packageManager, intent).filter {
        val mask = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        (it.activityInfo.applicationInfo.flags and mask) != 0
    }
    // Two system handlers would make the choice a guess; leave it to the resolver.
    val only = system.singleOrNull() ?: return null
    return ComponentName(only.activityInfo.packageName, only.activityInfo.name)
}

private fun queryHandlers(pm: PackageManager, intent: Intent): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

@RequiresApi(Build.VERSION_CODES.Q)
private fun suggestionOrNull(wifi: WifiCredentials): WifiNetworkSuggestion? {
    val builder = WifiNetworkSuggestion.Builder().setSsid(wifi.ssid)
    when (wifi.security) {
        WifiSecurity.OPEN -> Unit
        WifiSecurity.WPA -> builder.setWpa2Passphrase(wifi.password)
        WifiSecurity.SAE -> builder.setWpa3Passphrase(wifi.password)
        WifiSecurity.WEP, WifiSecurity.ENTERPRISE -> return null
    }
    if (wifi.hidden) builder.setIsHiddenSsid(true)
    // SSID and passphrase lengths are only validated on build().
    return try {
        builder.build()
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: IllegalStateException) {
        null
    }
}

private fun launchAddNetwork(
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    intent: Intent
) {
    try {
        launcher.launch(intent)
    } catch (e: ActivityNotFoundException) {
        toast(context, R.string.wifi_add_failed)
    }
}

/**
 * Message for the system dialog's outcome, or null when the user backed out of it
 * and has already seen that nothing happened.
 */
@StringRes
private fun addNetworkMessage(outcome: ActivityResult): Int? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    if (outcome.resultCode != Activity.RESULT_OK) return null
    val codes = outcome.data?.getIntegerArrayListExtra(Settings.EXTRA_WIFI_NETWORK_RESULT_LIST)
    // One network goes in, so one result comes back; treat a missing list as success,
    // since RESULT_OK already means the user confirmed the dialog.
    return when (codes?.firstOrNull()) {
        Settings.ADD_WIFI_RESULT_ALREADY_EXISTS -> R.string.wifi_already_saved
        Settings.ADD_WIFI_RESULT_ADD_OR_UPDATE_FAILED -> R.string.wifi_add_failed
        else -> R.string.wifi_added
    }
}

private fun openWifiSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        toast(context, R.string.no_app_found)
    }
}

private fun toast(context: Context, @StringRes message: Int) {
    android.widget.Toast
        .makeText(context, context.getString(message), android.widget.Toast.LENGTH_SHORT)
        .show()
}

/**
 * Opens a scanned URL. CATEGORY_BROWSABLE is how an activity opts into handling a URI
 * that came from somewhere untrusted, which is exactly what a QR payload is, so it is
 * tried first and keeps the link away from activities that never expected a stranger's
 * input. The retry without it is deliberate: [isOpenableUri] admits app deep links
 * (`otpauth:` among them) whose handlers do not always declare the category, and
 * dropping them would be a regression for the sake of a scheme the user aimed at.
 */
private fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(Intent(intent).addCategory(Intent.CATEGORY_BROWSABLE))
        return
    } catch (_: ActivityNotFoundException) {
        // No browsable handler; fall through to the unrestricted attempt.
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        toast(context, R.string.no_app_found)
    }
}

/**
 * Copies [text] to the clipboard. [sensitive] marks the clip as a credential, which
 * from API 33 keeps it out of the clipboard preview, clipboard history and keyboard
 * suggestions - worth setting for a password, but not for text the user wants to see
 * echoed back.
 */
private fun copy(context: Context, text: String, sensitive: Boolean = false) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("qr", text)
    if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    cm.setPrimaryClip(clip)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
