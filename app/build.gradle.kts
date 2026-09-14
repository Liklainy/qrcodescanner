plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Version lives in gradle.properties so a release bump is a one-line change, and
// CI can override it per build:
//   ./gradlew assembleRelease -PappVersionCode=2 -PappVersionName=1.1
val appVersionCode = (property("appVersionCode") as String).toInt()
val appVersionName = property("appVersionName") as String

android {
    namespace = "ru.qrefka.qrcodescanner"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.qrefka.qrcodescanner"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables { useSupportLibrary = true }
    }

    // Release signing comes from the environment (the Release workflow decodes the
    // keystore from repository secrets). Without SIGNING_KEYSTORE the release build
    // stays unsigned, so local assembleRelease keeps working without a key.
    val signingKeystore = System.getenv("SIGNING_KEYSTORE")?.takeIf { it.isNotBlank() }
    signingConfigs {
        if (signingKeystore != null) {
            create("release") {
                storeFile = file(signingKeystore)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures { compose = true }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs {
            useLegacyPackaging = false
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    // Used directly for ContextCompat, FileProvider and @RequiresApi; pinned to the
    // versions the other AndroidX artifacts already resolve to.
    implementation("androidx.core:core:1.18.0")
    implementation("androidx.annotation:annotation:1.9.1")

    // CameraX (no Google Play services required); 1.6.x ships 16 KB-aligned .so
    val camerax = "1.6.1"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // ZXing core - pure Java barcode library (NOT Google Play services)
    implementation("com.google.zxing:core:3.5.4")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
