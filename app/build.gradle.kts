import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// Release signing comes only from the environment (CI decodes the keystore from secrets).
// Without all three values there is no release signing config, and checkReleaseSigning below
// stops every release build before it starts, so no unsigned or debug-signed release APK exists.
fun env(name: String): String? = providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }

val releaseKeystoreFile = env("DND_KEYSTORE_FILE")
val releaseKeystorePassword = env("DND_KEYSTORE_PASSWORD")
val releaseKeyAlias = env("DND_KEY_ALIAS")
val hasReleaseSigning = releaseKeystoreFile != null && releaseKeystorePassword != null && releaseKeyAlias != null

android {
    namespace = "dnd.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.dnddataneverdie.dnd"
        minSdk = 28
        // Same as the Pleos sample app; set explicitly because AGP 8.13 would follow compileSdk.
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                // PKCS12 keystores keep one password for the store and the key.
                keyPassword = releaseKeystorePassword
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Lets Robolectric resolve R.font.* and the other app resources.
            isIncludeAndroidResources = true
            all { test ->
                // One 2560x1440 ARGB frame is about 15 MB; the Gradle default test heap is smaller than we want.
                test.maxHeapSize = "2g"
                test.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                // Robolectric on JDK 17+ (https://robolectric.org/getting-started/).
                test.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                )
                test.testLogging {
                    events("failed", "skipped")
                    exceptionFormat = TestExceptionFormat.FULL
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

val checkReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails unless DND_KEYSTORE_FILE, DND_KEYSTORE_PASSWORD and DND_KEY_ALIAS are set."
    val ready = hasReleaseSigning
    doFirst {
        if (!ready) {
            throw GradleException(
                "Release signing is not configured. Set DND_KEYSTORE_FILE, DND_KEYSTORE_PASSWORD and " +
                    "DND_KEY_ALIAS. The build stops here so that no unsigned or debug-signed release APK is written."
            )
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkReleaseSigning) }

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    // Provides the ComponentActivity that createComposeRule() launches under Robolectric.
    debugImplementation(libs.compose.ui.test.manifest)
}
