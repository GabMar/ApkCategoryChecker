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
 * @author Gabriele
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
	 * File Size
	 */
	private String _fileSize;
	
	
	/*--Methods--*/
	
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
	 * Return the File Size
	 * 
	 * @return
	 */
	public String get_fileSize(){
		return this._fileSize;
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
	 * Set the File Size
	 * 
	 * @param fileSize File Size in bytes
	 */
	public void set_fileSize(String fileSize){
		this._fileSize = fileSize;
	}
}

