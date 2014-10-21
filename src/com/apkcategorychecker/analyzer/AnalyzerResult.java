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
	 * Framework used
	 */
	private String _APKFramework;
	
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
	 * Return the APK Framework
	 * 
	 * @return 
	 */
	public String get_APKFramework(){
		return this._APKFramework;
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
	 * @param _framework String of APK Framework
	 */
	public void set_APKFramework(String _framework){
		this._APKFramework = _framework;
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
}

