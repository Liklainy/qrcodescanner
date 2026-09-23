# QRefka Scan

A lightweight, privacy-focused QR code scanner and generator for Android. Built with Jetpack Compose and CameraX — **zero Google Play Services required**.

## Features

- **Scan** — Real-time QR code scanning via CameraX with ZXing decoding
- **Wi-Fi codes** — A scanned `WIFI:` code shows the network, its security type and
  whether it is hidden; on Android 11+ one tap hands it to the system dialog that
  saves the network, with no Wi-Fi permission of the app's own
- **Generate** — Create QR codes from any text input
- **Share & Save** — Share generated QR codes, or save them to your gallery (Android 10+)
- **Quick Settings Tile** — Launch the scanner directly from the notification shade
- **Dark Mode** — Full Material 3 theming with light/dark support
- **Localized** — English, Russian and Simplified Chinese
- **Tiny APK** — ~2.4 MB release build with R8 minification and resource shrinking

## Screenshots

<!-- Add screenshots here -->
<!-- ![Scanner](screenshots/scanner.png) -->
<!-- ![Generator](screenshots/generator.png) -->

## Tech Stack

| Component | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Camera | CameraX 1.6.x |
| QR Decoding | ZXing core 3.5.4 |
| QR Encoding | ZXing core 3.5.4 |
| Language | Kotlin |
| Min SDK | 26 (Android 8.0); gallery save needs 29+ |
| Target SDK | 37 |

## Building

### Prerequisites

- **JDK 21** (Temurin recommended — [sdkman](https://sdkman.io/) or [Adoptium](https://adoptium.net/))
- **Android SDK** with build-tools 37+

### Debug build

```sh
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release build

```sh
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

See [PUBLISHING.md](PUBLISHING.md) for signing and AppGallery upload instructions.

### Run tests

```sh
./gradlew test
```

### Versioning

`versionCode` / `versionName` live in `gradle.properties` as `appVersionCode` and
`appVersionName`. Bump `appVersionCode` for **every** store upload — AppGallery
rejects a re-used one. A build can override them without editing the file:

```sh
./gradlew assembleRelease -PappVersionCode=2 -PappVersionName=1.1
```

Fastlane picks up the `VERSION_CODE` / `VERSION_NAME` environment variables and
passes them through the same way.

## Continuous integration

`.github/workflows/ci.yml` runs unit tests, `lintDebug` and a debug build on every
push to `main` and on pull requests, uploading the test and lint reports as
artifacts.

## Deployment (Fastlane)

The project uses [Fastlane](https://fastlane.tools/) with the [`huawei_appgallery_connect`](https://github.com/pifleo/fastlane-plugin-huawei_appgallery_connect) plugin for automated publishing to Huawei AppGallery.

### Setup

1. Install Fastlane and dependencies:
   ```sh
   bundle install
   ```

2. Copy the env template and fill in your credentials:
   ```sh
   cp fastlane/.env.example fastlane/.env.default
   ```

3. Get your credentials from [AppGallery Connect](https://developer.huawei.com/consumer/en/service/josp/agc/index.html):
   - **Client ID / Secret** → Users and Permissions → Connect API
   - **App ID** → App Information page

### Available lanes

| Lane | Command | Description |
|---|---|---|
| `build_debug` | `bundle exec fastlane build_debug` | Build debug APK |
| `test` | `bundle exec fastlane test` | Run unit tests |
| `build_release` | `bundle exec fastlane build_release` | Build signed release APK |
| `deploy_huawei` | `bundle exec fastlane deploy_huawei` | Build + upload to AppGallery (no review) |
| `release_huawei` | `bundle exec fastlane release_huawei` | Build + upload + submit for review |


## Project Structure

```
app/src/main/java/ru/qrefka/qrcodescanner/
├── MainActivity.kt          # Entry point, theme, tab navigation
├── ScannerTileService.kt    # Quick Settings tile
├── ui/
│   ├── ScannerScreen.kt     # Camera preview, viewfinder overlay, result sheet,
│   │                        #   Wi-Fi details and the system add-network hand-off
│   └── GeneratorScreen.kt   # Text input, QR generation, share/save
└── util/
    ├── QrEncoder.kt         # ZXing QR encoding wrapper
    ├── UriUtils.kt          # Decides whether scanned text is openable
    └── WifiQr.kt            # Parses the `WIFI:` payload (escapes, field order)

app/src/main/res/
├── values/strings.xml       # English (default)
└── values-ru/strings.xml    # Russian

docs/                        # GitHub Pages site (landing page + privacy policy)
```

## Architecture

The app follows a single-Activity architecture with Compose navigation via tabs:

- **No ViewModel** — State is managed locally in composables. The app's scope is small enough that this is simpler than adding a ViewModel layer.
- **No Google Play Services** — All dependencies are pure AndroidX or pure Java (ZXing). Fully compatible with Huawei AppGallery, F-Droid, and other alternative stores.
- **Background thread decoding** — QR frame analysis runs on a dedicated single-thread executor, keeping the main thread free.
- **No persisted state** — No database, preference file, or log; `allowBackup` is off, and
  scanned content never leaves the device. The two files the app can write are both
  user-initiated: a gallery PNG via `MediaStore`, and a `cacheDir/shared_images/` copy that
  backs the Share action and is pruned after an hour (`GeneratorScreen.kt:215`).
- **Wi-Fi without a Wi-Fi permission** — Saving a scanned network is delegated to the
  system "add network" dialog via `Settings.ACTION_WIFI_ADD_NETWORKS`
  (`ScannerScreen.kt:608`). Settings owns the dialog and the write, so the app needs no
  `CHANGE_WIFI_STATE`; `WifiManager.addNetworkSuggestions` would cost that permission and
  only make the network an auto-join candidate rather than a saved one. The dialog exists
  from API 30 and its builder covers neither WEP nor enterprise networks, so anything it
  cannot express falls back to showing the credentials for manual entry.
- **Passwords treated as credentials** — On the manual path the result sheet renders a
  passphrase, so that sheet — its own dialog window — sets `FLAG_SECURE` to keep it out of
  screenshots and recents snapshots (`ScannerScreen.kt:439`), and **Copy password** marks
  the clip `EXTRA_IS_SENSITIVE` so API 33+ leaves it out of the clipboard preview, history
  and keyboard suggestions (`ScannerScreen.kt:748`).

## Permissions

| Permission | Purpose | When requested |
|---|---|---|
| `CAMERA` | QR code scanning | When the Scan tab opens |

`CAMERA` is the only permission in the installed package. Two install-time
permissions that libraries would otherwise merge in — `ACCESS_NETWORK_STATE`
(from `androidx.media3:media3-common`, pulled in transitively by `camera-view`)
and the signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (from
`androidx.core`) — are stripped with `tools:node="remove"` in
`app/src/main/AndroidManifest.xml`. Neither has a surviving code path after R8.

The manifest also declares a `<queries>` element for `android.settings.WIFI_ADD_NETWORKS`.
That is package visibility, not a permission — the installed set stays CAMERA-only. It is
needed because from API 30 `queryIntentActivities` is filtered without it, and the app aims
the add-network intent at the one *system* handler it can confirm rather than letting an
intent whose extras carry the passphrase resolve by action alone (`ScannerScreen.kt:631`).

Dependency bumps can reintroduce merged permissions, so re-check after one:

```sh
apkanalyzer manifest permissions app/build/outputs/apk/release/app-release-unsigned.apk
```

No internet, network-state, storage, or location permissions.

## Project site

`docs/` holds a static GitHub Pages site: a landing page and the privacy policy
that stores require a URL for. Enable it under **Settings → Pages** with source
*Deploy from a branch*, branch `main`, folder `/docs`. Details and the `TODO`
placeholders to fill in are in [docs/README.md](docs/README.md).

Preview locally with `python3 -m http.server -d docs 8000`.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
