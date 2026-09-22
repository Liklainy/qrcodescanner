package ru.qrefka.qrcodescanner.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.core.net.toUri
import ru.qrefka.qrcodescanner.R

private const val PREFS = "consent"
private const val KEY_ACCEPTED = "privacy_accepted"

/** Whether the user has agreed to the privacy policy on this install. */
fun isPrivacyAccepted(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ACCEPTED, false)

fun setPrivacyAccepted(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit { putBoolean(KEY_ACCEPTED, true) }
}

/**
 * First-launch prompt to read the privacy policy. It stands in for the whole app
 * until answered, so nothing — the camera request in particular — runs before the
 * user has agreed. It cannot be dismissed by back or an outside tap: the only ways
 * out are the two buttons.
 */
@Composable
fun PrivacyConsentScreen(onAccept: () -> Unit, onDecline: () -> Unit) {
    val context = LocalContext.current
    val policyUrl = stringResource(R.string.privacy_policy_url)
    val linkLabel = stringResource(R.string.privacy_policy)
    val linkColor = MaterialTheme.colorScheme.primary
    val message = buildAnnotatedString {
        append(stringResource(R.string.privacy_consent_intro))
        append(' ')
        withLink(
            LinkAnnotation.Clickable(
                tag = "policy",
                styles = TextLinkStyles(
                    SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )
                )
            ) { openPolicy(context, policyUrl) }
        ) { append(linkLabel) }
        append(stringResource(R.string.privacy_consent_outro))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.privacy_consent_title)) },
            text = {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = onAccept) { Text(stringResource(R.string.privacy_agree)) }
            },
            dismissButton = {
                TextButton(onClick = onDecline) { Text(stringResource(R.string.privacy_decline)) }
            }
        )
    }
}

private fun openPolicy(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_app_found), Toast.LENGTH_SHORT).show()
    }
}
