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

## CSV CONTAINING THE COLLECTION OF .JS FILES ##

Regardless of the format result file chosen, the script generates a file with .csv extension containing the following values

Column | Description
----------|------------
appID | The id of analyzed APK
jsFiles | List of js files contained in the APK

## RECOGNITION FRAMEWORKS METHODS ##

This section explains how each framework is recognised.

Framework | Recognition method | Reliability
----------|----------|----------
[Apache Cordova](http://cordova.apache.org/) | If is present the string "org.apache.cordova" in "/res/xml/config.xml" AND if is present the class "CordovaActivity" | Strong
[Phonegap](http://phonegap.com/) | If is present the string "http://phonegap.com/ns/1.0" in "/res/xml/config.xml" AND if is present the class "CordovaActivity" | Medium
[Enyo](http://enyojs.com/) | If is present the string "enyo.machine" AND "enyo.kind" in a .js file | Medium
[IBM Worklight](http://www-03.ibm.com/software/products/it/worklight-foundation) | If is present the string "com.worklight.androidgap" in "/res/xml/config.xml" AND if is present the class "WLDroidGap" | Strong
[IUI](http://www.iui-js.org/) | If is present the class "IUI" | Medium
[Kivy](http://kivy.org/) | If is present the string "PythonActivity" in the "AndroidManifest.xml" | Medium
[Mobl](http://www.mobl-lang.org/) | If is present the class "MoblGap", AND if present a file with extension ".mobl" | Strong
[MoSync](http://www.mosync.com/) | If is present the string "MoSyncService" in the "AndroidManifest.xml" | Medium
[Next](http://nextinterfaces.com/b) | If is present the string "nextwebapp" in a file, AND if is present the class "NextWebApp" | Strong
[Quick Connect](http://www.quickconnectfamily.org/qc_hybrid) | If is present the string "function QCNativeFooter", AND if is present the string "qc.handleError" in a file | Medium
[Rho Mobile](http://rhomobile.com/) | If exists the file "rho.dat" | Medium
[Sencha](http://www.sencha.com/products/touch) | If is present the strings "Ext.create" AND "Ext.application" in a file javascript | Strong
[Titanium](http://www.appcelerator.com/) | If is present the class "TitaniumModule", AND if is present the class "TiActivity"| Strong

## BUILD ##

Requires JDK 21 (Gradle will download one automatically if missing).

	./gradlew shadowJar

The runnable jar is written to `build/libs/ApkCategoryChecker.jar`.

## MINIMUM REQUIREMENTS ##

This software requires Java 21 or higher

## THIRD-PARTY LIBRARIES ##

Apache Commons CSV:   https://commons.apache.org/proper/commons-csv/

Apache Commons CLI:   https://commons.apache.org/proper/commons-cli/
