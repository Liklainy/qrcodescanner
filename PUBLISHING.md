# QRefka Scan — AppGallery publishing

App is built and ready. No Google Play services are used, so it is fully compatible with Huawei AppGallery.

## Build the release APK

```sh
# Use JDK 21 (Temurin via sdkman)
export JAVA_HOME=~/.sdkman/candidates/java/21.0.11-tem
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk` (~2.4 MB, well under the 10 MB target).

## Sign the APK

AppGallery requires a signed APK. Generate a keystore once and **back it up** —
every future update must be signed with the same key:

```sh
keytool -genkeypair -v -keystore release.keystore -alias qrcode \
  -keyalg RSA -keysize 4096 -validity 10000
```

Signing is read from environment variables (`app/build.gradle.kts`); without
them the release build stays unsigned:

```sh
SIGNING_KEYSTORE=$PWD/release.keystore SIGNING_KEY_ALIAS=qrcode \
SIGNING_STORE_PASSWORD=*** SIGNING_KEY_PASSWORD=*** ./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`.

## Release via GitHub Actions

`.github/workflows/release.yml` builds a signed APK when a `v*` tag is pushed and
attaches it to a GitHub Release (notes from `fastlane/metadata/huawei/release_notes.txt`).

One-time setup — repository **Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i release.keystore \| pbcopy`, then paste |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | `qrcode` |
| `KEY_PASSWORD` | key password (same as keystore password for PKCS12) |

Then release:

```sh
git tag v1.0 && git push origin v1.0
```

## Bump the version

`appVersionCode` in `gradle.properties` must be higher than the last uploaded
build — AppGallery rejects a duplicate. Either edit the file or pass it in:

```sh
./gradlew assembleRelease -PappVersionCode=2 -PappVersionName=1.1
```

## Upload to AppGallery

1. Sign in at https://developer.huawei.com → AppGallery Connect.
2. My Apps → Create app → fill name (`QRefka Scan`), category Tools, device Mobile.
3. App information → upload icon (`fastlane/metadata/huawei/images/icon_216.png`;
   `icon_512.png` is the same artwork at 512x512 for stores that ask for it),
   set min/target SDK (26/37).
4. App information → paste the listing text from `fastlane/metadata/huawei/en-US/`
   (`short_description.txt` is the app introduction, `full_description.txt` the
   description), for the Russian locale from `ru-RU/`, and for Simplified Chinese
   (required when the release regions include the Chinese mainland) from `zh-CN/`.
5. App information → upload the seven screenshots from
   `fastlane/metadata/huawei/images/phoneScreenshots/` (450x800, the size the
   console accepts; see [Screenshots](#screenshots) below).
6. Release → Version information → upload the signed APK → fill release notes
   (`fastlane/metadata/huawei/release_notes.txt`) → Submit for review.
7. App information → privacy policy URL: enter the English
   `https://qrcodescanner.qrefka.ru/privacy.html` as the default and, for the
   Simplified Chinese language, `https://qrcodescanner.qrefka.ru/privacy.zh.html`.
   A mainland-China release is rejected without a Chinese policy. The policies are
   served from GitHub Pages — see [Project site](#project-site-github-pages) below.
   Check that the developer name in section 1 of each policy matches the developer
   name on the AppGallery Connect account exactly.
8. App information → Privacy tag (privacy label): declare what the app processes,
   or the review fails with "collects personal information but no privacy tag".
   It must agree with section 2 of the policy and the in-app consent text:
   - Collected data: **Photos and videos** (camera frames and a picked image),
     purpose **App functionality** (decoding codes).
   - Processed on the device only; not transmitted, not shared with third
     parties, not used for tracking or advertising, not linked to the user.
   - Nothing else: no identifiers, no location, no contacts, no usage data.
9. Content rating when prompted.

## Screenshots

AppGallery takes 3–8 phone screenshots and wants portrait ones at **450x800**
(PNG/JPG), so a raw 1080x2400 phone capture is both the wrong size and the wrong
aspect ratio. The store-ready files live in
`fastlane/metadata/huawei/images/phoneScreenshots/`; the untouched captures are
kept beside them in `phoneScreenshots-original/`.

To convert a new capture (macOS `sips`, no extra tools): crop away the status bar
and the gesture handle, pad the sides out to 9:16 in the app's teal (`#00807A`,
the theme's primary), then scale down.

```sh
sips --cropOffset 100 0 --cropToHeightWidth 2240 1080 shot.png --out /tmp/c.png
sips --padToHeightWidth 2240 1260 --padColor 00807A /tmp/c.png --out /tmp/p.png
sips -z 800 450 /tmp/p.png --out phoneScreenshots/shot.png
```

The crop offsets suit a 1080x2400 capture with gesture navigation (the
`Medium_Phone` emulator, with the demo-mode status bar); check the result if the
phone has differently sized bars. Screenshots must show what the app really does — the
scanner shot needs a real code visible in the viewfinder, not a blank preview.

## Project site (GitHub Pages)

Stores require a reachable privacy policy URL. It is served from `docs/` by
GitHub Pages, together with a small landing page.

Enable it once: repository **Settings → Pages** → Source *Deploy from a branch* →
branch `main`, folder `/docs`.

`docs/CNAME` points the site at the custom domain `qrcodescanner.qrefka.ru`, so
the live pages are:

| Page | URL |
|---|---|
| Landing page | https://qrcodescanner.qrefka.ru/ |
| Privacy policy (AppGallery default) | https://qrcodescanner.qrefka.ru/privacy.html |
| Privacy policy, Simplified Chinese (AppGallery, zh-CN) | https://qrcodescanner.qrefka.ru/privacy.zh.html |
| Privacy policy, Russian | https://qrcodescanner.qrefka.ru/privacy.ru.html |

Without the `CNAME` file they would be at `https://<owner>.github.io/<repo>/` instead.

Before submitting, fill in the remaining `TODO` markers in `docs/index.html` — the
store listing link. See [docs/README.md](docs/README.md).

## How the app meets the task

- Privacy consent: on first launch a dialog links the policy (in the device language) and the app stays blocked until the user agrees; Disagree closes it. Afterwards the ⓘ button next to the tab switcher reopens the policy and lets the user withdraw consent, which brings the first-launch dialog back.

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
- Gradle 9.7.1 (via wrapper)
- AGP 9.3.2, Kotlin 2.4.0, Compose via `org.jetbrains.kotlin.plugin.compose` plugin (no `composeOptions`/`kotlinCompilerExtensionVersion`)
- CameraX 1.6.1 (16 KB-aligned native libs), Compose BOM 2026.06.01, ZXing core 3.5.4

## Notes on the local environment

This machine has JDK 26 as the system default, which is too new for AGP 9. Build with JDK 21 via sdkman as shown above.

## Regenerating the icons

The launcher/tile vector drawables and the store PNGs are all generated from one
geometry definition, so the artwork stays in sync:

```sh
python3 tools/generate_icons.py   # no dependencies beyond the stdlib
```
