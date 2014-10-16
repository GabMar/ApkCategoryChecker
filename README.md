# Apk Category Checker #
==================

## DESCRIPTION ##

A Command-Line Program written in Java to decode the APK using ApkTool and check what Framework it's been used to build the APK.

## Command Line Parameters ##


-p 		: 	The Path of an APK or the Path of a directory containing APKs

-d		:	Use current directory

-csv	:	Export the results in a CSV file

-o 		:	The destination directory of the result's file. If this parameter is missing, the result's file will be exported on the working path

-k 		:	Keep the directory of the encoded APK 

## USAGE EXAMPLE ##

* To analyze the directory containing the `ApkCategoryChecker.jar` and put the CSV result file in the same directory, open a terminal, navigate in the directory and type:

	`java -jar ApkCategoryChecker.jar -d -csv`

* To analyze a directory or an APK file and put the CSV result file in a different directory, open a terminal, navgate in the directory containing the 'ApkCategoryChecker.jar' file and type:

	`java -jar ApkCategoryChecker.jar -p /Path/of/The/Directory/or/APK/To/Analyze -csv -o /Destination/Path/for/Result/File`

* If you want to maintain the directory containing the decoded APK, add the parameter -k:

	`java -jar ApkCategoryChecker.jar -p /Path/of/The/Directory/or/APK/To/Analyze -csv -k`

## 3rd-part Applications: ##

Apktool:   https://github.com/iBotPeaches/Apktool

Apache Commons CSV:   http://commons.apache.org/proper/commons-csv/

Apache Commons CLI:   http://commons.apache.org/proper/commons-cli/usage.html