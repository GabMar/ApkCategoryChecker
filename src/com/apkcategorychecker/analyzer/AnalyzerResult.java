/*
	<Author: Gabriele Martini
	Description: This Software is a A Command-Line Program written in Java
	to check what Framework it's been used to build the APK>
    Copyright (C) <2014>  <Gabriele Martini>

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
 */
package com.apkcategorychecker.analyzer;

/**
 * This class creates an object that contains the results of the analysis
 *
 * @author Gabriele Martini
 */

/**
 * Constructor of Class
 * @author Gabriele Martini
 *
 */
public class AnalyzerResult {

	/*--Attributes--*/

	/**
	 * APK Package
	 */
	private String _Package;

	/**
	 * APK Name
	 */
	private String _APKName;

	/**
	 * APK Path
	 */
	private String _APKPath;

	/**
	 * Main Framework used
	 */
	private String _APKMainFramework;

	/**
	 * Base Framework used
	 */
	private String _APKBaseFramework;

	/**
	 * Number of HTML files
	 */
	private int _html;

	/**
	 * Numer of Javascript files
	 */
	private int _js;

	/**
	 * Number of CSS files
	 */
	private int _css;

	/**
	 * Value of Debuggable parameter
	 */
	private String _debuggable;

	/**
	 * List of Android Permissions
	 */
	private String _permissions;

	/**
	 * Min Sdk Version
	 */
	private String _minSdkVersion;

	/**
	 * Max Sdk Version
	 */
	private String _maxSdkVersion;

	/**
	 * Target Sdk Version
	 */
	private String _targetSdkVersion;

	/**
	 * File Size
	 */
	private String _fileSize;

	/**
	 * Start time
	 */
	private long _startAnalysis;

	/**
	 * Duration time
	 */
	private long _durationAnalysis;

	/**
	 * Decode success flag
	 */
	private int _decodeSuccess;


	/*--Methods--*/

	public AnalyzerResult(
			String App_ID,
			String APK_File_Name,
			String APK_File_Path,
			String APK_Package,
			String Main_Framework,
			String Base_Framework,
			String HTML,
			String JS,
			String CSS,
			String Android_Debuggable,
			String Android_Permissions,
			String Android_MinSdkVersion,
			String Android_MaxSdkVersion,
			String Android_TargetSdkVersion,
			String File_Size,
			String Start_Analysis_Time,
			String Duration_Analysis_Time,
			String Decode_Success){

		this._APKBaseFramework = Main_Framework;
		this._APKMainFramework = Main_Framework;
		this._APKName = APK_File_Name;
		this._APKPath = APK_File_Path;
		this._css = Integer.parseInt(CSS);
		this._debuggable = Android_Debuggable;
		this._decodeSuccess = Integer.parseInt(Decode_Success);
		this._durationAnalysis = Long.valueOf(Duration_Analysis_Time);
		this._fileSize = File_Size;
		this._html = Integer.parseInt(HTML);
		this._js = Integer.parseInt(JS);
		this._maxSdkVersion = Android_MaxSdkVersion;
		this._minSdkVersion = Android_MinSdkVersion;
		this._Package = APK_Package;
		this._permissions = Android_Permissions;
		this._startAnalysis = Long.valueOf(Start_Analysis_Time);
		this._targetSdkVersion = Android_TargetSdkVersion;
	}

	public AnalyzerResult(){};

	/**
	 * Return the APK Package
	 *
	 * @return
	 */
	public String get_Package(){
		return this._Package;
	}

	/**
	 * Return the APK Name
	 *
	 * @return
	 */
	public String get_APKName(){
		return this._APKName;
	}

	/**
	 * Return the APK Path
	 *
	 * @return
	 */
	public String get_APKPath(){
		return this._APKPath;
	}

	/**
	 * Return the APK Main Framework
	 *
	 * @return
	 */
	public String get_APKMainFramework(){
		return this._APKMainFramework;
	}

	/**
	 * Return the APK Base Framework
	 *
	 * @return
	 */
	public String get_APKBaseFramework(){
		return this._APKBaseFramework;
	}

	/**
	 * Return the number of html files
	 *
	 * @return
	 */
	public int get_html(){
		return this._html;
	}

	/**
	 * Return the number of javascript files
	 *
	 * @return
	 */
	public int get_js(){
		return this._js;
	}

	/**
	 * Return the number of css files
	 *
	 * @return
	 */
	public int get_css(){
		return this._css;
	}

	/**
	 * Return the value of Debuggable parameter
	 *
	 * @return
	 */
	public String get_debuggable(){
		return this._debuggable;
	}

	/**
	 * Return the list of Android Permissions
	 *
	 * @return
	 */
	public String get_permissions(){
		return this._permissions;
	}

	/**
	 * Return the value of minSdkVersion
	 *
	 * @return
	 */
	public String get_minSdkVersion(){
		return this._minSdkVersion;
	}

	/**
	 * Return the value of maxSdkVersion
	 *
	 * @return
	 */
	public String get_maxSdkVersion(){
		return this._maxSdkVersion;
	}

	/**
	 * Return the value of targetSdkVersion
	 *
	 * @return
	 */
	public String get_targetSdkVersion(){
		return this._targetSdkVersion;
	}

	/**
	 * Return the File Size
	 *
	 * @return
	 */
	public String get_fileSize(){
		return this._fileSize;
	}

	/**
	 * Return the start time analysis in milliseconds
	 *
	 * @return
	 */
	public long get_startAnalysis(){
		return this._startAnalysis;
	}

	/**
	 * Return the duration time analysis in milliseconds
	 *
	 * @return
	 */
	public long get_durationAnalysis(){
		return this._durationAnalysis;
	}

	/**
	 * Return the decode flag
	 *
	 * @return
	 */
	public int get_decodeSuccess(){
		return this._decodeSuccess;
	}

	/**
	 *
	 * @param _i String of APK Package
	 */
	public void set_Package(String _i){
		this._Package = _i;
	}

	/**
	 *
	 * @param _name String of APK Name
	 */
	public void set_APKName(String _name){
		this._APKName = _name;
	}

	/**
	 *
	 * @param _path String of APK Path
	 */
	public void set_APKPath(String _path){
		this._APKPath = _path;
	}

	/**
	 *
	 * @param _framework String of APK Main Framework
	 */
	public void set_APKMainFramework(String _framework){
		this._APKMainFramework = _framework;
	}

	/**
	 *
	 * @param _framework String of APK Base Framework
	 */
	public void set_APKBaseFramework(String _framework){
		this._APKBaseFramework = _framework;
	}

	/**
	 * Set the number of html files
	 *
	 * @param html number of html files
	 */
	public void set_html(int html){
		this._html = html;
	}

	/**
	 * Set the number of javascript files
	 *
	 * @param js number of javascript files
	 */
	public void set_js(int js){
		this._js = js;
	}

	/**
	 * Set the number of css files
	 *
	 * @param css number of css files
	 */
	public void set_css(int css){
		this._css = css;
	}

	/**
	 * Set the Debuggable parameter
	 *
	 * @param debuggable Value of debuggable
	 */
	public void set_debuggable(String debuggable){
		this._debuggable = debuggable;
	}

	/**
	 * Set the list of Android Permissions
	 *
	 * @param permissions
	 */
	public void set_permissions(String permissions){
		this._permissions = permissions;
	}

	/**
	 * Set the value of minSdkVersion
	 *
	 * @param minSdk
	 */
	public void set_minSdkVersion(String minSdk){
		this._minSdkVersion = minSdk;
	}

	/**
	 * Set the value of maxSdkVersion
	 *
	 * @param maxSdk
	 */
	public void set_maxSdkVersion(String maxSdk){
		this._maxSdkVersion = maxSdk;
	}

	/**
	 * Set the value of targetSdkVersion
	 *
	 * @param targetSdk
	 */
	public void set_targetSdkVersion(String targetSdk){
		this._targetSdkVersion = targetSdk;
	}

	/**
	 * Set the File Size
	 *
	 * @param fileSize File Size in bytes
	 */
	public void set_fileSize(String fileSize){
		this._fileSize = fileSize;
	}

	/**
	 * Set the start time analysis in milliseconds
	 *
	 * @param _time
	 */
	public void set_startAnalysis(long _time){
		this._startAnalysis = _time;
	}

	/**
	 * Set the duration time analysis in milliseconds
	 *
	 * @param _time
	 */
	public void set_durationAnalysis(long _time){
		this._durationAnalysis = _time;
	}

	/**
	 * Set the decode flag
	 *
	 * @param _flag
	 */
	public void set_decodeSuccess(int _flag){
		this._decodeSuccess = _flag;
	}
}
