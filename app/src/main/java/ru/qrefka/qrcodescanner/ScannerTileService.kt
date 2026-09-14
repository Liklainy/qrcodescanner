package ru.qrefka.qrcodescanner

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.camera.lifecycle.ProcessCameraProvider

class ScannerTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.tile_label)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        
        // Asynchronously pre-warm the CameraX ProcessCameraProvider so the scanner
        // starts faster. The future is intentionally not observed: initialization
        // errors, if any, surface when the scanner screen requests the provider.
        runCatching { ProcessCameraProvider.getInstance(this) }

        val launchAction = Runnable {
            val intent = MainActivity.launchScannerIntent(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Only the *creator* mode belongs in the options a PendingIntent is
                // built with; setPendingIntentBackgroundActivityStartMode is the
                // sender's to set. Through API 36 the platform quietly reset a sender
                // mode found here, but an app targeting 37 gets IllegalArgumentException
                // ("must not be set when creating a PendingIntent") and the tile crashes.
                @Suppress("DEPRECATION")
                val options = ActivityOptions.makeBasic().apply {
                    pendingIntentCreatorBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    options.toBundle()
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
                startActivityAndCollapse(intent)
            }
        }

        if (isLocked) {
            unlockAndRun(launchAction)
        } else {
            launchAction.run()
        }
    }
}
