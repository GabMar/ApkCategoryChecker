# APK CATEGORY CHECKER #

Author: [Gabriele Martini](https://github.com/GabMar)

## DESCRIPTION ##

A Command-Line Program written in [Kotlin](https://kotlinlang.org/) to check what Framework has been used to build an APK file, or each APK in a directory. APKs are read directly as zip archives (DEX class names, binary XML manifest and resources): nothing is decompiled or extracted to disk.

## COMMAND LINE PARAMETERS ##

Parameter | Description
----------|------------
-p | The Path of an APK or the Path of a directory containing APKs
-csv | Export the results in a [CSV](http://it.wikipedia.org/wiki/Comma-separated_values) file with specified path
-k | Deprecated and ignored: APKs are no longer decoded to disk
-deep | Number of threshold to detect if defined APK is hybrid

## USAGE EXAMPLE ##

* To analyze the directory containing the `ApkCategoryChecker.jar` and put the CSV result file in the same directory, open a terminal, navigate in the directory and type:

	`java -jar ApkCategoryChecker.jar -p . -csv .`

* To analyze a directory or an APK file and put the CSV result file in a different directory, open a terminal, navigate in the directory containing the 'ApkCategoryChecker.jar' file and type:

	`java -jar ApkCategoryChecker.jar -p /Path/of/The/Directory/or/APK/To/Analyze -csv /Destination/Path/for/Result/File`

* To set the level of Analysis (number of web resource files to search) use the parameter -deep:

	(example with choosed level 4)
	
	`java -jar ApkCategoryChecker.jar -p /Path/of/The/Directory/or/APK/To/Analyze -csv . -deep 4`

## FILE RESULT FORMAT ##

For now the only supported output format is CSV with the following columns

Column | Description
----------|------------
App_ID | The id of analyzed APK
APK_File_Name | The APK name
APK_File_Path | The path of analyzed APK
APK_Package | The package of APK
Main_Framework | The Main Framework used to build the APK
Base_Framework | The Base Framework used to build the APK
HTML | Number of ".html" files in the APK
JS | Number of ".js" files in the APK
CSS | Number of ".css" files in the APK
Android_Debuggable | The value of Debuggable parameter in AndroidManifest.xml
Android_Permissions | List of Android Permission
Android_MinSdkVersion | Value of minSdkVersion
Android_MaxSdkVersion | Value of maxSdkVersion
Android_TargetSdkVersion | Value of targetSdkVersion
File_Size(bytes) | File size (in bytes) of APK
Start_Analysis_Time(milliseconds) | UNIX Time of start
Duration_Analysis_Time(milliseconds) | UNIX Time of duration
Decode_Success | Flag to indicate the success of the analysis (0 success, 1 fail)
Evidence | What was found in the APK for each detected framework, e.g. `Flutter: lib/arm64-v8a/libflutter.so`

## CSV CONTAINING THE COLLECTION OF .JS FILES ##

Regardless of the format result file chosen, the script generates a file with .csv extension containing the following values

Column | Description
----------|------------
appID | The id of analyzed APK
jsFiles | List of js files contained in the APK

## RECOGNITION FRAMEWORKS METHODS ##

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

Web assets are the files under `assets/www/` (Cordova) and `assets/public/` (Capacitor). With `-deep N`, an app with at least N `.html`, `.js` and `.css` files and no other match is reported as `Undefined_Web_App`. An app matching none of the above is reported as `Native`.

## BUILD ##

Requires JDK 21 (Gradle will download one automatically if missing).

	./gradlew shadowJar

The runnable jar is written to `build/libs/ApkCategoryChecker.jar`.

## MINIMUM REQUIREMENTS ##

This software requires Java 21 or higher

## THIRD-PARTY LIBRARIES ##

Apache Commons CSV:   https://commons.apache.org/proper/commons-csv/

Apache Commons CLI:   https://commons.apache.org/proper/commons-cli/
