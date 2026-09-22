package ru.qrefka.qrcodescanner.ui

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.WriterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.qrefka.qrcodescanner.R
import ru.qrefka.qrcodescanner.ui.components.FillScrollColumn
import ru.qrefka.qrcodescanner.util.QrEncoder
import java.io.File
import java.io.FileOutputStream

/** Buttons are taller than the Material minimum; it suits the roomier layout. */
private val ActionHeight = 54.dp

/**
 * @param bottomReserve space the floating tab switcher takes at the bottom of the
 *   window. It is given back while the keyboard is up, since the switcher hides then.
 */
@Composable
fun GeneratorScreen(bottomReserve: Dp = 0.dp) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var generatedText by rememberSaveable { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val inputScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Offload encode point to Dispatchers.Default so the UI thread doesn't block
    val generationState by produceState<GenerationState>(
        initialValue = GenerationState.Idle,
        key1 = generatedText
    ) {
        val text = generatedText
        if (text == null) {
            value = GenerationState.Idle
        } else {
            value = GenerationState.Encoding
            val bm = withContext(Dispatchers.Default) { encodeOrNull(text) }
            value = if (bm != null) GenerationState.Success(bm) else GenerationState.Error
        }
    }
    val error = generationState is GenerationState.Error
    val encoding = generationState is GenerationState.Encoding
    val currentBitmap = (generationState as? GenerationState.Success)?.bitmap

    val generateQr: () -> Unit = {
        keyboard?.hide()
        saved = false
        generatedText = input.trim()
    }

    LaunchedEffect(saved) {
        if (saved) {
            delay(2000)
            saved = false
        }
    }

    // The keyboard and the tab switcher never show together, so the bottom inset is
    // whichever is taller rather than their sum. Taking the max keeps the padding
    // continuous while the keyboard animates: it follows the keyboard down until it
    // meets the switcher's reserve and then holds, where swapping one for the other
    // on a visibility flag made the whole layout jump by the reserve in one frame.
    val bottomInsets = WindowInsets.navigationBars
        .add(WindowInsets(bottom = bottomReserve))
        .union(WindowInsets.ime)
        .only(WindowInsetsSides.Bottom)
    val otherInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)

    AnimatedContent(
        targetState = currentBitmap,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.94f))
                .togetherWith(fadeOut(tween(160)))
        },
        label = "generatorStage"
    ) { bitmap ->
        if (bitmap == null) {
            FillScrollColumn(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(otherInsets)
                    .windowInsetsPadding(bottomInsets)
                    .padding(horizontal = 24.dp)
                    .padding(vertical = 20.dp),
                scrollState = inputScrollState,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.generator_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.generator_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        // Clear a previous "too much text" error.
                        generatedText = null
                    },
                    placeholder = { Text(stringResource(R.string.enter_text)) },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    // Grows with the text instead of claiming the whole screen the
                    // moment it appears; the button stays pinned to the bottom either
                    // way, via the spacer below. Where the minimum does not fit, the
                    // column scrolls rather than hiding the button.
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 190.dp, max = 420.dp)
                )
                Spacer(Modifier.weight(1f))
                if (error) {
                    Text(
                        stringResource(R.string.too_much_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = generateQr,
                    enabled = input.isNotBlank() && !encoding,
                    modifier = Modifier.fillMaxWidth().height(ActionHeight)
                ) {
                    // A long payload takes long enough on a slow device that the button
                    // would otherwise look unresponsive: Idle no longer stands in for
                    // "encoding", so the wait can be shown.
                    if (encoding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current
                        )
                    } else {
                        Text(stringResource(R.string.generate))
                    }
                }
            }
        } else {
            BackHandler {
                generatedText = null
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(otherInsets)
                    .windowInsetsPadding(bottomInsets)
                    .padding(horizontal = 24.dp)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // The code itself always sits on white: a tinted or dark card
                    // behind it is what breaks scanning on other people's phones.
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.qr_content_description),
                            modifier = Modifier
                                .size(300.dp)
                                .padding(24.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    PayloadPreview(generatedText.orEmpty())
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Saving via MediaStore without WRITE_EXTERNAL_STORAGE needs API 29+.
                    // Older devices are told to use Share rather than left wondering
                    // where the Save button went.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        saveBitmapToGallery(context, bitmap)
                                    }
                                    if (ok) {
                                        saved = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            R.string.save_failed,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(ActionHeight)
                        ) { Text(stringResource(if (saved) R.string.saved else R.string.save)) }
                    } else {
                        Text(
                            stringResource(R.string.save_unsupported),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    FilledTonalButton(
                        onClick = { scope.launch { shareBitmap(context, bitmap) } },
                        modifier = Modifier.fillMaxWidth().height(ActionHeight)
                    ) { Text(stringResource(R.string.share)) }
                    TextButton(
                        onClick = { generatedText = null },
                        modifier = Modifier.fillMaxWidth().height(ActionHeight)
                    ) { Text(stringResource(R.string.back)) }
                }
            }
        }
    }
}

/** A pill under the code echoing what was encoded, so a stale code is obvious. */
@Composable
private fun PayloadPreview(text: String) {
    if (text.isEmpty()) return
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

private fun encodeOrNull(text: String): Bitmap? =
    try {
        QrEncoder.encode(text, 600)
    } catch (_: WriterException) {
        // Text exceeds QR code capacity.
        null
    } catch (_: IllegalArgumentException) {
        null
    }

private suspend fun shareBitmap(context: Context, bitmap: Bitmap) {
    val uri = withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            // Drop stale files from previous shares; keep recent ones in case a
            // receiver still holds a grant on them.
            val cutoff = System.currentTimeMillis() - 60 * 60 * 1000
            cachePath.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
            val file = File(cachePath, "qr_${System.currentTimeMillis()}.png")
            val written = FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (!written) return@withContext null
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }
    if (uri == null) {
        Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            clipData = ClipData.newRawUri(null, uri)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, null).apply {
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "QR_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/QR Scanner"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: return false
        val written = resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        } ?: false
        if (!written) {
            resolver.delete(uri, null, null)
            return false
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        true
    } catch (e: Exception) {
        false
    }
}

private sealed interface GenerationState {
    data object Idle : GenerationState
    data object Encoding : GenerationState
    data class Success(val bitmap: Bitmap) : GenerationState
    data object Error : GenerationState
}
