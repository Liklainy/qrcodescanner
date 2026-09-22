package ru.qrefka.qrcodescanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.qrefka.qrcodescanner.ui.GeneratorScreen
import ru.qrefka.qrcodescanner.ui.PrivacyConsentScreen
import ru.qrefka.qrcodescanner.ui.PrivacyDialog
import ru.qrefka.qrcodescanner.ui.ScannerScreen
import ru.qrefka.qrcodescanner.ui.clearPrivacyAccepted
import ru.qrefka.qrcodescanner.ui.components.InfoGlyph
import ru.qrefka.qrcodescanner.ui.isPrivacyAccepted
import ru.qrefka.qrcodescanner.ui.setPrivacyAccepted
import ru.qrefka.qrcodescanner.ui.theme.QrTheme

class MainActivity : ComponentActivity() {
    private var currentTab by mutableIntStateOf(0)
    private var privacyAccepted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A tile launch names its tab explicitly and outranks the restored one: if the
        // process was killed while the task stayed in recents, onCreate sees both a
        // saved bundle and the tile's intent, and the tile has to win or it lands on
        // the wrong tab. The extra is consumed so it does not re-apply on rotation,
        // which would undo a tab the user picked after the launch.
        val launchIntent = intent
        val fromIntent = launchIntent?.getIntExtra(EXTRA_TAB, -1) ?: -1
        currentTab = if (fromIntent != -1) {
            launchIntent?.removeExtra(EXTRA_TAB)
            fromIntent
        } else {
            savedInstanceState?.getInt(STATE_TAB) ?: 0
        }
        privacyAccepted = isPrivacyAccepted(this)
        setContent {
            QrTheme {
                if (privacyAccepted) {
                    AppShell(
                        selectedTab = currentTab,
                        onTabSelected = { currentTab = it },
                        onWithdrawConsent = {
                            clearPrivacyAccepted(this)
                            privacyAccepted = false
                        }
                    )
                } else {
                    PrivacyConsentScreen(
                        onAccept = {
                            setPrivacyAccepted(this)
                            privacyAccepted = true
                        },
                        onDecline = { finishAndRemoveTask() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val tabExtra = intent.getIntExtra(EXTRA_TAB, -1)
        if (tabExtra != -1) {
            // Consumed for the same reason as in onCreate: setIntent stored this very
            // object, so a later rotation would otherwise read the extra a second time.
            intent.removeExtra(EXTRA_TAB)
            currentTab = tabExtra
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TAB, currentTab)
    }

    companion object {
        const val EXTRA_TAB = "extra_tab"
        private const val STATE_TAB = "state_tab"
        const val TAB_SCAN = 0

        fun launchScannerIntent(context: android.content.Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_TAB, TAB_SCAN)
            }
    }
}

/** Vertical space the floating switcher occupies, reserved by the screens below it. */
private val SwitcherReserve = 84.dp

@Composable
private fun AppShell(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onWithdrawConsent: () -> Unit
) {
    val tab = selectedTab.coerceIn(0, 1)
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    // The switcher floats over the content rather than sitting in a bottomBar, so the
    // camera can run to all four edges. It steps aside for the keyboard, which would
    // otherwise shove a pill full of tabs into the middle of the generator screen.
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Preserves each tab's rememberSaveable state while the other tab is shown.
        val stateHolder = rememberSaveableStateHolder()
        when (tab) {
            0 -> stateHolder.SaveableStateProvider("scan") {
                ScannerScreen(bottomReserve = SwitcherReserve)
            }

            else -> stateHolder.SaveableStateProvider("generate") {
                GeneratorScreen(bottomReserve = SwitcherReserve)
            }
        }

        AnimatedVisibility(
            visible = !imeVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp)
        ) {
            // The privacy button rides next to the switcher so the policy stays one tap
            // away on both tabs, whatever state the screen underneath is in.
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabSwitcher(selected = tab, onSelect = onTabSelected)
                PrivacyButton(onClick = { showPrivacy = true })
            }
        }
    }

    if (showPrivacy) {
        PrivacyDialog(
            onDismiss = { showPrivacy = false },
            onWithdraw = {
                showPrivacy = false
                onWithdrawConsent()
            }
        )
    }
}

@Composable
private fun PrivacyButton(onClick: () -> Unit) {
    val label = stringResource(R.string.privacy_consent_title)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp,
        modifier = Modifier
            .size(52.dp)
            .semantics { contentDescription = label }
    ) {
        Box(contentAlignment = Alignment.Center) {
            InfoGlyph(MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(22.dp))
        }
    }
}

/**
 * A floating pill with a sliding filled indicator, in place of the underlined tab
 * row. Two destinations do not justify a full navigation bar, and keeping the
 * control off the top edge leaves the viewfinder the whole screen.
 */
@Composable
private fun TabSwitcher(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf(R.string.tab_scan, R.string.tab_generate)
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(Modifier.padding(5.dp)) {
            labels.forEachIndexed { index, label ->
                val active = index == selected
                val background by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = tween(220),
                    label = "tabBackground"
                )
                val content by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "tabContent"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(background)
                        .selectable(
                            selected = active,
                            role = Role.Tab,
                            onClick = { onSelect(index) }
                        )
                        .padding(horizontal = 26.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelLarge,
                        color = content
                    )
                }
            }
        }
    }
}
