plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

group = "com.apkcategorychecker"
version = "2.0.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

// The legacy jars bundle partial copies of commons-cli / commons-io that shadow the real
// libraries on the classpath; repackage them without those classes.
fun filteredLegacyJar(name: String, vararg excludes: String) =
    tasks.register<Jar>("filter${name.replaceFirstChar(Char::uppercase)}") {
        archiveFileName = "$name-filtered.jar"
        destinationDirectory = layout.buildDirectory.dir("legacy")
        from(zipTree("libs/legacy/$name.jar")) {
            exclude(*excludes, "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

val apktoolFiltered = filteredLegacyJar("apktool", "org/apache/commons/cli/**", "org/apache/commons/io/**")
val commonsLiteFiltered = filteredLegacyJar("commons-lite-1.15", "org/apache/commons/io/**")

dependencies {
    implementation(libs.commons.cli)
    implementation(libs.commons.csv)
    implementation(libs.json.simple)
    implementation(libs.commons.io)

    // apktool 2.0.0 (fat jar) and dex2jar 0.0.9.15 are not on Maven Central.
    // Temporary: they go away when the analysis engine reads the APK directly.
    implementation(files(apktoolFiltered, commonsLiteFiltered))
    implementation(fileTree("libs/legacy") {
        include("*.jar")
        exclude("apktool.jar", "commons-lite-1.15.jar")
    })

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "com.apkcategorychecker.Main"
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
