package ru.qrefka.qrcodescanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ru.qrefka.qrcodescanner.ui.GeneratorScreen
import ru.qrefka.qrcodescanner.ui.ScannerScreen

class MainActivity : ComponentActivity() {
    private var currentTab by mutableIntStateOf(0)

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
        setContent {
            val dark = isSystemInDarkTheme()
            val colors = if (dark) DarkColors else LightColors
            MaterialTheme(colorScheme = colors) {
                AppScaffold(
                    selectedTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
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

private val TealPrimary = Color(0xFF00695C)
private val TealPrimaryDark = Color(0xFF80CBC4)
private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF003C33),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFF26A69A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2F1),
)
private val DarkColors = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color(0xFF003C33),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF003C33),
    secondaryContainer = Color(0xFF00504A),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF003C33),
    tertiaryContainer = Color(0xFF004D47),
)

@Composable
private fun AppScaffold(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tab = selectedTab.coerceIn(0, 1)
    Scaffold(
        topBar = {
            PrimaryTabRow(
                selectedTabIndex = tab,
                modifier = Modifier.statusBarsPadding()
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { onTabSelected(0) },
                    text = { Text(stringResource(R.string.tab_scan)) }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { onTabSelected(1) },
                    text = { Text(stringResource(R.string.tab_generate)) }
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            // Preserves each tab's rememberSaveable state while the other tab is shown.
            val stateHolder = rememberSaveableStateHolder()
            when (tab) {
                0 -> stateHolder.SaveableStateProvider("scan") { ScannerScreen() }
                else -> stateHolder.SaveableStateProvider("generate") { GeneratorScreen() }
            }
        }
    }
}
