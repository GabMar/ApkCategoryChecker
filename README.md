# ApkCategoryChecker

[![CI](https://github.com/GabMar/ApkCategoryChecker/actions/workflows/ci.yml/badge.svg)](https://github.com/GabMar/ApkCategoryChecker/actions/workflows/ci.yml)

A command-line tool that detects which framework was used to build Android apps: Flutter, React Native, Cordova, Capacitor/Ionic, .NET MAUI, Unity and [more](#how-frameworks-are-recognized), or plain native.

APKs are read directly as zip archives (DEX class names, binary XML manifest and resources): nothing is decompiled or extracted to disk, so even large batches are analyzed in seconds. For every detected framework the tool reports the evidence it found.

## Installation

Download `ApkCategoryChecker.jar` from the [latest release](https://github.com/GabMar/ApkCategoryChecker/releases/latest). It requires Java 21 or later.

## Usage

```sh
java -jar ApkCategoryChecker.jar [OPTIONS] PATH...
```

Each `PATH` is an APK file or a directory, searched recursively for `.apk` files. Results go to standard output (or to `--output`), progress and errors to standard error.

Option | Description
-------|------------
`-o`, `--output FILE` | Write the results to `FILE` instead of standard output
`-f`, `--format FORMAT` | `csv`, `json` or `jsonl` ([JSON Lines](https://jsonlines.org)). Default: from the output file extension, otherwise `csv`
`-j`, `--threads N` | APKs analyzed in parallel. Default: number of CPUs
`--web-app-threshold N` | Report apps with at least N `.html`/`.js`/`.css` files and no other match as `Undefined_Web_App`. Default: disabled
`-h`, `--help` | Show the help
`-V`, `--version` | Show the version

Examples:

```sh
# Analyze a single APK, print CSV
java -jar ApkCategoryChecker.jar app.apk

# Analyze a directory tree, write JSON
java -jar ApkCategoryChecker.jar ~/apks -o results.json

# Stream one JSON object per APK into jq
java -jar ApkCategoryChecker.jar ~/apks -f jsonl | jq -r 'select(.mainFramework == "Flutter") | .packageName'
```

The exit code is 0 even when some APKs cannot be analyzed: they are reported with an `error`. It is 2 for invalid arguments (e.g. a missing path).

## Result format

APKs are reported in path order. JSON fields are camelCase (`mainFramework`); CSV columns are snake_case (`main_framework`), with lists separated by `; ` and RFC 4180 quoting. Unknown values are `null` in JSON and empty in CSV.

Field | Description
------|------------
`file`, `path` | Name and path of the APK
`fileSize` | Size of the APK in bytes
`packageName` | Package name from `AndroidManifest.xml`
`mainFramework` | The most specific framework detected, or `Native`; null if the APK could not be analyzed
`baseFramework` | The runtime the main framework is built on, if detected (e.g. `Capacitor` for `Ionic`)
`evidence` | For each detected framework, what was found in the APK. In CSV: `Ionic: assets/public/main.js contains "ion-app" \| Capacitor: ...`
`htmlFiles`, `jsFiles`, `cssFiles` | Number of `.html`, `.js` and `.css` files in the APK
`jsFileNames` | Names of the `.js` files in the APK
`debuggable` | Value of `android:debuggable`
`permissions` | Permissions requested with `uses-permission`
`minSdkVersion`, `targetSdkVersion`, `maxSdkVersion` | From `uses-sdk`
`analysisStart` | When the analysis of the APK started (ISO-8601)
`analysisDurationMs` | How long the analysis of the APK took
`success` (CSV only), `error` | Whether the APK could be analyzed, and if not why

## How frameworks are recognized

Each framework is recognised by a set of signals: native libraries (in any `lib/<abi>/` directory), classes in the DEX files, files in the archive or text in the web assets. Unless noted, any one signal is enough; where possible both native libraries and classes are checked, because split APKs from App Bundles keep the native libraries in a separate per-ABI APK.

When more than one framework matches, the most specific one is reported as `Main_Framework` and the runtime it is built on (if detected) as `Base_Framework`.

Framework | Signals | Base framework
----------|---------|---------------
[Apache Cordova](https://cordova.apache.org/) | class `CordovaActivity` **and** either "org.apache.cordova" in `res/xml/config.xml` or `assets/www/cordova.js` |
[Capacitor](https://capacitorjs.com/) | classes in `com.getcapacitor`, `assets/capacitor.config.json` |
[React Native](https://reactnative.dev/) | `libreactnativejni.so` (up to 0.75), `libreactnative.so` (0.76+), `assets/index.android.bundle`, classes in `com.facebook.react` |
[.NET for Android](https://learn.microsoft.com/dotnet/android/) (formerly Xamarin.Android) | `libmonodroid.so`, classes in `mono.android` |
[Kivy](https://kivy.org/) | classes in `org.kivy.android`, "PythonActivity" in `AndroidManifest.xml` |
[Titanium](https://titaniumsdk.com/) | classes `TitaniumModule` **and** `TiActivity` |
[NativeScript](https://nativescript.org/) | `libNativeScript.so`, classes in `com.tns` |
[Flutter](https://flutter.dev/) | `libflutter.so`, classes in `io.flutter` |
[Qt](https://www.qt.io/) | `libQt5Core*.so` / `libQt6Core*.so`, classes in `org.qtproject.qt` / `org.qtproject.qt5` |
[Unity](https://unity.com/) | `libunity.so`, classes in `com.unity3d.player` |
[Unreal Engine](https://www.unrealengine.com/) | `libUE4.so`, `libUnreal.so`, classes in `com.epicgames.ue4` / `com.epicgames.unreal` |
[Godot](https://godotengine.org/) | `libgodot_android.so`, classes in `org.godotengine.godot` |
[Cocos](https://www.cocos.com/) (Cocos2d-x, Cocos Creator) | `libcocos2dcpp.so`, `libcocos.so`, classes in `org.cocos2dx.lib` / `com.cocos.lib` |
[Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) | classes in `org.jetbrains.compose.resources`, files under `composeResources/` |
[Sencha](https://www.sencha.com/) | "Ext.create" **and** "Ext.application" in a `.js` web asset | Capacitor, Apache Cordova
[Ionic](https://ionicframework.com/) | "ion-app" in a `.js` web asset | Capacitor, Apache Cordova
[Expo](https://expo.dev/) | classes in `expo.modules` | React Native
[Xamarin.Forms](https://dotnet.microsoft.com/apps/xamarin/xamarin-forms) | classes `FormsAppCompatActivity`, `FormsApplicationActivity` | .NET for Android
[.NET MAUI](https://dotnet.microsoft.com/apps/maui) | class `MauiAppCompatActivity`, classes in `com.microsoft.maui` | .NET for Android

Web assets are the files under `assets/www/` (Cordova) and `assets/public/` (Capacitor). With `--web-app-threshold N`, an app with at least N `.html`, `.js` and `.css` files and no other match is reported as `Undefined_Web_App`. An app matching none of the above is reported as `Native`.

## Building from source

Requires JDK 21; Gradle downloads one automatically if missing.

```sh
./gradlew build                     # compile and run the tests
./gradlew shadowJar                 # runnable jar in build/libs/ApkCategoryChecker.jar
./gradlew run --args="path/to/app.apk -f json"
```

To publish a release, push a tag: the [release workflow](.github/workflows/release.yml) builds the jar with the tag's version and attaches it to a GitHub release.

```sh
git tag v2.0.0 && git push origin v2.0.0
```

## License

[MIT](LICENSE) © [Gabriele Martini](https://github.com/GabMar)

Built with [picocli](https://picocli.info/), [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/) and [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization).
