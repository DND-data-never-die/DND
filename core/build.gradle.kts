plugins {
    alias(libs.plugins.kotlin.jvm)
}

// No jvmToolchain: the build runs on whatever JDK starts Gradle (17 in CI, 19 on the laptop)
// and always emits Java 17 bytecode.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit)
}

// The engine must not depend on the machine's zone or locale. The default test task runs in
// Asia/Seoul; testUtc runs the same tests with the JVM default zone set to UTC and the default
// locale set to en-US (GitHub runners use UTC). Both run with `./gradlew :core:test` and `check`.
tasks.named<Test>("test") {
    systemProperty("user.timezone", "Asia/Seoul")
}

val testUtc by tasks.registering(Test::class) {
    description = "Runs the core unit tests with -Duser.timezone=UTC and an en-US default locale."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    systemProperty("user.timezone", "UTC")
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
    shouldRunAfter(tasks.named("test"))
}

tasks.named("test") { finalizedBy(testUtc) }
tasks.named("check") { dependsOn(testUtc) }
