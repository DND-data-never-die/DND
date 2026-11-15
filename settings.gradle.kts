pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DND"

include(":core")

// The Android module needs an Android SDK. A machine without one (no ANDROID_HOME, no
// ANDROID_SDK_ROOT, no local.properties) still builds and tests :core.
val hasAndroidSdk = providers.environmentVariable("ANDROID_HOME").isPresent ||
    providers.environmentVariable("ANDROID_SDK_ROOT").isPresent ||
    settingsDir.resolve("local.properties").exists()
if (hasAndroidSdk && settingsDir.resolve("app/build.gradle.kts").exists()) include(":app")
