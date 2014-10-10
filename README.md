ApkCategoryChecker
==================

A Command-Line Program written in Java to decode the APK using ApkTool and check what Framework it's been used to build the APK.

<b>The command-line parameters:</b>

-p 		: 	The Path of an APK or the Path of a directory containing APKs

-d		:	Use current directory

-csv	:	Export the results in a CSV file

-o 		:	The destination directory of the result's file. If this parameter is missing, the result's file will be exported on the working path

-k 		:	Keep the directory of the encoded APK 

<b>3rd-part Applications:</b>

Apktool:   https://github.com/iBotPeaches/Apktool

Apache Commons CSV:   http://commons.apache.org/proper/commons-csv/

Apache Commons CLI:   http://commons.apache.org/proper/commons-cli/usage.html