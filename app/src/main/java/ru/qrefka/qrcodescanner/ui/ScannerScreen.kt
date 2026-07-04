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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.qrefka.qrcodescanner.R
import ru.qrefka.qrcodescanner.util.WifiCredentials
import ru.qrefka.qrcodescanner.util.WifiSecurity
import ru.qrefka.qrcodescanner.util.isOpenableUri
import ru.qrefka.qrcodescanner.util.parseWifiQr
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScannerScreen() {
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

    when {
        hasPermission -> ScannerContent()
        else -> PermissionPrompt(
            openSettings = deniedPermanently,
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
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

@Composable
private fun PermissionPrompt(openSettings: Boolean, onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.camera_perm_rationale))
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) {
            Text(stringResource(if (openSettings) R.string.open_settings else R.string.grant_camera))
        }
    }
}

@Composable
private fun ScannerContent() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    // Claimed on the analyzer thread by the first decode and released when the sheet
    // is dismissed. The camera stays bound while the sheet is up, so without this a
    // QR left in frame would post a Runnable per frame for nothing. Seeded from
    // [result] because that survives a tab switch while this flag does not.
    val handled = remember { AtomicBoolean(result != null) }

    // The Runnable below captures the current haptics; dropping any that are still
    // queued keeps a decode from buzzing after the user has left the scanner tab.
    DisposableEffect(mainHandler) {
        onDispose { mainHandler.removeCallbacksAndMessages(null) }
    }

    Box(Modifier.fillMaxSize()) {
        CameraPreview(
            onDecoded = { decoded ->
                if (handled.compareAndSet(false, true)) {
                    mainHandler.post {
                        result = decoded
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
        )
        ViewfinderOverlay()
        Text(
            stringResource(R.string.scan_instruction),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f
                )
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 155.dp)
        )
    }

    result?.let { text ->
        val wifi = remember(text) { parseWifiQr(text) }
        // Built up front: the intent is null for payloads the system dialog cannot
        // take, which is exactly when the sheet has to offer manual entry instead.
        val addNetwork = remember(wifi) { wifi?.let { addNetworkIntentOrNull(context, it) } }
        val addNetworkLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { outcome -> addNetworkMessage(outcome)?.let { toast(context, it) } }
        ResultSheet(
            text = text,
            wifi = wifi,
            canAddNetwork = addNetwork != null,
            onDismiss = {
                result = null
                handled.set(false)
            },
            onOpenLink = { openLink(context, text) },
            onCopy = { copy(context, text) },
            onConnectWifi = { addNetwork?.let { launchAddNetwork(context, addNetworkLauncher, it) } },
            onCopyPassword = { wifi?.let { copy(context, it.password, sensitive = true) } },
            onOpenWifiSettings = { openWifiSettings(context) }
        )
    }
}

@Composable
private fun CameraPreview(onDecoded: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnDecoded by rememberUpdatedState(onDecoded)
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
                    setAnalyzer(executor, QrAnalyzer { currentOnDecoded(it) })
                }
            try {
                provider.unbindAll()
                val cameraSelector = when {
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }
                if (cameraSelector != null) {
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview, analysis
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
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
    Canvas(modifier = Modifier.fillMaxSize()) {
        val viewfinderSize = 260.dp.toPx()
        val cx = size.width / 2
        val cy = size.height / 2
        val left = cx - viewfinderSize / 2
        val top = cy - viewfinderSize / 2
        val right = cx + viewfinderSize / 2
        val bottom = cy + viewfinderSize / 2
        val cr = 16.dp.toPx()
        val bl = 40.dp.toPx()
        val bw = 4.dp.toPx()

        // Semi-transparent mask with rounded cutout
        val maskPath = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
            addRoundRect(RoundRect(left, top, right, bottom, cr, cr))
            fillType = PathFillType.EvenOdd
        }
        drawPath(maskPath, color = Color.Black.copy(alpha = 0.5f))

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
 * Reusable analyzer with a cached byte buffer to prevent 1-2 MB allocations
 * on every camera frame at 30-60 FPS.
 */
private class QrAnalyzer(
    private val onDecoded: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }
    private var buffer = ByteArray(0)

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
            // The Y plane may be padded: rowStride is the actual length of each data row.
            val source = PlanarYUVLuminanceSource(
                buffer, plane.rowStride, height, 0, 0, width, height, false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            // decodeWithState, not decode: decode() begins with setHints(null), which
            // would throw away the QR-only restriction set in the initializer and make
            // every frame run the 1D, Aztec, Data Matrix and PDF417 readers too.
            // reset() clears per-frame state but leaves the configured readers in place.
            val r = try { reader.decodeWithState(bitmap) } catch (_: NotFoundException) { null }
            reader.reset()
            r?.text?.let(onDecoded)
        } catch (_: Exception) {
        } finally {
            proxy.close()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultSheet(
    text: String,
    wifi: WifiCredentials?,
    canAddNetwork: Boolean,
    onDismiss: () -> Unit,
    onOpenLink: () -> Unit,
    onCopy: () -> Unit,
    onConnectWifi: () -> Unit,
    onCopyPassword: () -> Unit,
    onOpenWifiSettings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // True only on the manual path, which is the one that renders the password.
    val showsPassword = wifi != null && !canAddNetwork && wifi.password.isNotEmpty()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The sheet is its own dialog window, so FLAG_SECURE has to be set here -
        // setting it on the activity window would leave this content uncovered.
        // It keeps the password out of screenshots and recents task snapshots.
        properties = ModalBottomSheetProperties(
            securePolicy =
                if (showsPassword) SecureFlagPolicy.SecureOn else SecureFlagPolicy.Inherit
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (wifi != null) {
                WifiDetails(wifi, canAddNetwork)
            } else {
                val scrollState = rememberScrollState()
                SelectionContainer {
                    Text(
                        text,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(scrollState)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (wifi != null) {
                    if (canAddNetwork) {
                        Button(
                            onClick = onConnectWifi,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.wifi_connect)) }
                    }
                    if (wifi.password.isNotEmpty()) {
                        CopyButton(
                            label = R.string.wifi_copy_password,
                            confirmation = R.string.wifi_password_copied,
                            onCopy = onCopyPassword
                        )
                    }
                    if (!canAddNetwork) {
                        OutlinedButton(
                            onClick = onOpenWifiSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.wifi_open_settings)) }
                    }
                } else {
                    if (isOpenableUri(text)) {
                        Button(
                            onClick = onOpenLink,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.open)) }
                    }
                    CopyButton(
                        label = R.string.copy,
                        confirmation = R.string.copied,
                        onCopy = onCopy
                    )
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.dismiss)) }
            }
        }
    }
}

/**
 * Summary of a scanned `WIFI:` payload. The password is shown only when the network
 * has to be added by hand — otherwise the system dialog takes it and there is no
 * reason to put it on screen.
 */
@Composable
private fun WifiDetails(wifi: WifiCredentials, canAddNetwork: Boolean) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.wifi_network),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        SelectionContainer {
            Text(
                wifi.ssid,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            buildString {
                append(stringResource(securityLabel(wifi.security)))
                if (wifi.hidden) append(" \u00b7 ").append(stringResource(R.string.wifi_hidden))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!canAddNetwork) {
            if (wifi.password.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        wifi.password,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace
                    )
                }
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
    OutlinedButton(
        onClick = {
            onCopy()
            copied = true
        },
        modifier = Modifier.fillMaxWidth()
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
