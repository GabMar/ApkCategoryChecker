plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.ktlint)
    application
}

group = "com.apkcategorychecker"
// Release builds pass -PreleaseVersion=x.y.z (see .github/workflows/release.yml).
version = providers.gradleProperty("releaseVersion").getOrElse("2.0.0-SNAPSHOT")

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.picocli)
    implementation(libs.commons.csv)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Style rules are configured in .editorconfig; `./gradlew check` fails on violations.
ktlint {
    version = libs.versions.ktlint
}

application {
    mainClass = "com.apkcategorychecker.cli.MainKt"
}

tasks.jar {
    manifest.attributes("Implementation-Version" to project.version)
}

tasks.shadowJar {
    archiveBaseName = "ApkCategoryChecker"
    archiveClassifier = ""
    archiveVersion = ""
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
