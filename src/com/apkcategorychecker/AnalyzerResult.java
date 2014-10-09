/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

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
	
	
	/*--Methods--*/
	
	/**
	 * 
	 * @return Return the APK Package
	 */
	public String get_Package(){
		return this._Package;
	}
	
	/**
	 * 
	 * @return Return the APK Name
	 */
	public String get_APKName(){
		return this._APKName;
	}
	
	/**
	 * 
	 * @return Return the APK Path
	 */
	public String get_APKPath(){
		return this._APKPath;
	}
	
	/**
	 * 
	 * @return Return the APK Framework
	 */
	public String get_APKFramework(){
		return this._APKFramework;
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
}

