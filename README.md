ApkCategoryChecker
==================

A Command-Line Program written in Java to decode the APK using ApkTool and check what Framework it's been used to build the APK.

The command-line parameters:

-p 		: 	The Path of an APK or the Path of a directory containing APKs

-csv	:	Export the results in a CSV file

-o 		:	The destination directory of the result's file. If this parameter is missing, the result's file will be exported on the working path

-k 		:	Keep the directory of the encoded APK 

3rd-part Applications:

Apktool:   https://github.com/iBotPeaches/Apktool

Apache Commons CSV:   http://commons.apache.org/proper/commons-csv/

Apache Commons CLI:   http://commons.apache.org/proper/commons-cli/usage.html