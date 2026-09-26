plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Version lives in gradle.properties so a release bump is a one-line change, and
// CI can override it per build:
//   ./gradlew assembleRelease -PappVersionCode=2 -PappVersionName=1.1
val appVersionCode = (property("appVersionCode") as String).toInt()
val appVersionName = property("appVersionName") as String

/** An environment variable, or null when it is unset or blank. */
fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

var releaseSigningProblems = emptyList<String>()

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
    // stays unsigned, so local assembleRelease keeps working without a key; pass
    // -PrequireSigning (the workflows and Fastlane do) to make that an error instead.
    // Once SIGNING_KEYSTORE is set, the other three must be set too.
    val signingKeystore = env("SIGNING_KEYSTORE")?.let { file(it.trim()) }
    signingConfigs {
        if (signingKeystore != null) {
            create("release") {
                storeFile = signingKeystore
                storePassword = env("SIGNING_STORE_PASSWORD")
                keyAlias = env("SIGNING_KEY_ALIAS")?.trim()
                keyPassword = env("SIGNING_KEY_PASSWORD")
            }
        }
    }
    releaseSigningProblems = when {
        signingKeystore == null ->
            if (providers.gradleProperty("requireSigning").isPresent) listOf("SIGNING_KEYSTORE is not set") else emptyList()
        else -> listOfNotNull(
            "Keystore not found: $signingKeystore".takeUnless { signingKeystore.isFile },
        ) + listOf("SIGNING_STORE_PASSWORD", "SIGNING_KEY_ALIAS", "SIGNING_KEY_PASSWORD")
            .filter { env(it) == null }
            .map { "$it is not set" }
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

// Checked when a release build runs rather than at configuration, so a half-set
// signing environment does not break debug builds or tests.
val checkReleaseSigning by tasks.registering {
    val problems = releaseSigningProblems
    doLast {
        if (problems.isNotEmpty()) {
            throw GradleException("Release signing is misconfigured:\n  " + problems.joinToString("\n  "))
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkReleaseSigning) }

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
