# APK CATEGORY CHECKER #

Author: [Gabriele Martini](https://github.com/GabMar)

## DESCRIPTION ##

A Command-Line Program written in [Java](http://en.wikipedia.org/wiki/Java_%28programming_language%29) to decode an APK file or a directory containing APK files using ApkTool and Dex2Jar and check what Framework it's been used to build the APK.

## COMMAND LINE PARAMETERS ##

Parameter | Description
----------|------------
-p | The Path of an APK or the Path of a directory containing APKs
-csv | Export the results in a [CSV](http://it.wikipedia.org/wiki/Comma-separated_values) file with specified path
-k | Keep the directory of the encoded APK 
-deep | Number of threshold to detect if defined APK is hybrid

## USAGE EXAMPLE ##

* To analyze the directory containing the `ApkCategoryChecker.jar` and put the CSV result file in the same directory, open a terminal, navigate in the directory and type:

	`java -jar ApkCategoryChecker.jar -p . -csv .`

* To analyze a directory or an APK file and put the CSV result file in a different directory, open a terminal, navigate in the directory containing the 'ApkCategoryChecker.jar' file and type:

	`java -jar ApkCategoryChecker.jar -p /Path/of/The/Directory/or/APK/To/Analyze -csv /Destination/Path/for/Result/File`

* If you want to maintain the directory containing the decoded APK, add the parameter -k:

	`java -jar ApkCategoryChecker.jar -p /Path/of/The/Directory/or/APK/To/Analyze -csv . -k .`

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
HTML | Number of ".html" file used to build the APK
JS | Number of ".js" file used to build the APK
CSS | Number of ".css" file used to build the APK
Android_Debuggable | The value of Debuggable parameter in AndroidManifest.xml
Android_Permissions | List of Android Permission
Android_MinSdkVersion | Value of minSdkVersion
Android_MaxSdkVersion | Value of maxSdkVersion
Android_TargetSdkVersion | Value of targetSdkVersion
File_Size(bytes) | File size (in bytes) of APK
Start_Analysis_Time(milliseconds) | UNIX Time of start
Duration_Analysis_Time(milliseconds) | UNIX Time of duration
Decode_Success | Flag to indicate the success of the decoding (0 success, 1 fail)

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
[Apache Cordova](http://cordova.apache.org/) | If is present the string "org.apache.cordova" in "/res/xml/config.xml" AND if is present the file "CordovaActivity.class" | Strong
[Phonegap](http://phonegap.com/) | If is present the string "http://phonegap.com/ns/1.0" in "/res/xml/config.xml" AND if is present the file "CordovaActivity.class" | Medium
[Enyo](http://enyojs.com/) | If is present the string "enyo.machine" AND "enyo.kind" in a .js file | Medium
[IBM Worklight](http://www-03.ibm.com/software/products/it/worklight-foundation) | If is present the string "com.worklight.androidgap" in "/res/xml/config.xml" AND if is present the file "WLDroidGap.class" | Strong
[IUI](http://www.iui-js.org/) | If is present the file "IUI.class" | Medium
[Kivy](http://kivy.org/) | If is present the string "PythonActivity" in the "AndroidManifest.xml" | Medium
[Mobl](http://www.mobl-lang.org/) | If is present the file "MoblGap.class", AND if present a file with extension ".mobl" | Strong
[MoSync](http://www.mosync.com/) | If is present the string "MoSyncService" in the "AndroidManifest.xml" | Medium
[Next](http://nextinterfaces.com/b) | If is present the string "nextwebapp" in a file, AND if is present the file "NextWebApp.class" | Strong
[Quick Connect](http://www.quickconnectfamily.org/qc_hybrid) | If is present the string "function QCNativeFooter", AND if is present the string "qc.handleError" in a file | Medium
[Rho Mobile](http://rhomobile.com/) | If exists the file "rho.dat" | Medium
[Sencha](http://www.sencha.com/products/touch) | If is present the strings "Ext.create" AND "Ext.application" in a file javascript | Strong
[Titanium](http://www.appcelerator.com/) | If is present the file "TitaniumModule.class", AND if is present the file "TiActivity.class"| Strong

## MINIMUM REQUIREMENTS ##

This software requires Java 1.7 or higher

## LICENSE ##

Copyright (C) 2014  Gabriele Martini

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

## THIRD-PARTY APPLICATIONS: ##

Apktool:   https://github.com/iBotPeaches/Apktool

Dex2Jar:	https://code.google.com/p/dex2jar/

Apache Commons CSV:   http://commons.apache.org/proper/commons-csv/

Apache Commons CLI:   http://commons.apache.org/proper/commons-cli/usage.html