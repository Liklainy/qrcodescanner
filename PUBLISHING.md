# QR Scanner — AppGallery publishing

App is built and ready. No Google Play services are used, so it is fully compatible with Huawei AppGallery.

## Build the release APK

```sh
# Use JDK 21 (Temurin via sdkman)
export JAVA_HOME=~/.sdkman/candidates/java/21.0.11-tem
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk` (~2.4 MB, well under the 10 MB target).

## Sign the APK

AppGallery requires a signed APK. Generate a keystore once (skip if you already have one):

```sh
keytool -genkey -v -keystore release.keystore -alias qrcode \
  -keyalg RSA -keysize 2048 -validity 10000
```

Configure signing in `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = System.getenv("KEYSTORE_PASS")
            keyAlias = "qrcode"
            keyPassword = System.getenv("KEY_PASS")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... existing minify/shrink config
        }
    }
}
```

Then rebuild:

```sh
KEYSTORE_PASS=*** KEY_PASS=*** ./gradlew assembleRelease
```

## Bump the version

`appVersionCode` in `gradle.properties` must be higher than the last uploaded
build — AppGallery rejects a duplicate. Either edit the file or pass it in:

```sh
./gradlew assembleRelease -PappVersionCode=2 -PappVersionName=1.1
```

## Upload to AppGallery

1. Sign in at https://developer.huawei.com → AppGallery Connect.
2. My Apps → Create app → fill name (`QR Scanner`), category Tools, device Mobile.
3. App information → upload icon (`fastlane/metadata/huawei/images/icon_216.png`;
   `icon_512.png` is the same artwork at 512x512 for stores that ask for it),
   set min/target SDK (26/36).
4. Release → Version information → upload the signed APK → fill release notes → Submit for review.
5. Add the privacy URL and content rating when prompted. The privacy policy is
   served from GitHub Pages — see [Project site](#project-site-github-pages) below.

## Project site (GitHub Pages)

Stores require a reachable privacy policy URL. It is served from `docs/` by
GitHub Pages, together with a small landing page.

Enable it once: repository **Settings → Pages** → Source *Deploy from a branch* →
branch `main`, folder `/docs`. The pages then are:

| Page | URL |
|---|---|
| Landing page | `https://<owner>.github.io/<repo>/` |
| Privacy policy (paste this into AppGallery) | `https://<owner>.github.io/<repo>/privacy.html` |
| Privacy policy, Russian | `https://<owner>.github.io/<repo>/privacy.ru.html` |

Before submitting, fill in the `TODO` markers in `docs/*.html` — the support
contact address and the store listing link. See [docs/README.md](docs/README.md).

## How the app meets the task

- Scan: CameraX preview + ZXing `MultiFormatReader` analyze frames; on a hit, show the text with **Open** (shown when the content is a single token starting with a URI scheme, e.g. `https:`, `mailto:`, `geo:`) and **Copy** buttons.
- Wi-Fi codes: a `WIFI:` payload is parsed (`util/WifiQr.kt`) and the sheet shows the SSID, security type and hidden flag instead of the raw text. **Add network** hands the credentials to the system add-network dialog (`Settings.ACTION_WIFI_ADD_NETWORKS`, API 30+), which does the saving — the app gains no Wi-Fi permission. Where that dialog cannot take the network (API < 30, WEP, enterprise, an out-of-range passphrase) the sheet shows the password for manual entry instead, under `FLAG_SECURE`, with **Copy password** marking the clip sensitive.
- Generate: ZXing `MultiFormatWriter` → `Bitmap`, rendered with Compose `Image`.
- Quick action tile: `ScannerTileService` (`ScannerTileService.kt`) appears in the Quick Settings panel alongside WiFi/BT; tapping it launches the scanner.
- Permissions: only `CAMERA`, requested at runtime when the Scan tab opens. The manifest's `<queries>` entry for the add-network intent is package visibility, not a permission, and adds nothing to the installed set.
- Bundle: R8 + resource shrinking keep the release APK to ~2.4 MB.
- No Google services: AndroidX (CameraX, Compose) and ZXing `core` are the only deps; no `com.google.android.gms` anywhere.

## Build requirements

- JDK 21 (Temurin)
- Android SDK 37 (build-tools 37.0.0); SDK 35/36 also present locally
- Gradle 9.4.1 (via wrapper)
- AGP 9.2.1, Kotlin 2.4.0, Compose via `org.jetbrains.kotlin.plugin.compose` plugin (no `composeOptions`/`kotlinCompilerExtensionVersion`)
- CameraX 1.6.1 (16 KB-aligned native libs), Compose BOM 2026.06.01, ZXing core 3.5.4

## Notes on the local environment

This machine has JDK 26 as the system default, which is too new for AGP 9. Build with JDK 21 via sdkman as shown above.

## Regenerating the icons

The launcher/tile vector drawables and the store PNGs are all generated from one
geometry definition, so the artwork stays in sync:

```sh
python3 tools/generate_icons.py   # no dependencies beyond the stdlib
```
