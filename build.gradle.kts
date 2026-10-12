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
    implementation(libs.arsclib)
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
    useJUnitPlatform { excludeTags("corpus") }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Real apps from F-Droid and GitHub releases, listed in src/test/resources/corpus/apps.json.
// Downloads ~1 GB (cached), so it is not part of `build`: run it with `./gradlew corpusTest`.
val corpusTest =
    tasks.register<Test>("corpusTest") {
        description = "Checks the detection on real APKs downloaded from F-Droid and GitHub releases."
        group = "verification"
        testClassesDirs =
            sourceSets.test
                .get()
                .output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform { includeTags("corpus") }
        systemProperty(
            "corpus.cache",
            providers.environmentVariable("CORPUS_CACHE").getOrElse(
                layout.buildDirectory
                    .dir("corpus")
                    .get()
                    .asFile.path,
            ),
        )
        providers.environmentVariable("GITHUB_TOKEN").orNull?.let { environment("GITHUB_TOKEN", it) }
        outputs.upToDateWhen { false } // the latest releases change without any change here
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
