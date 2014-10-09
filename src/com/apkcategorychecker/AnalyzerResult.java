/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

/**
 *
 * @author Gabriele Martini
 */
public class AnalyzerResult {
	
	private String _Package;
	private String _APKName;
	private String _APKPath;
	private String _APKFramework;
	
	public String get_Package(){
		return this._Package;
	}
	
	public String get_APKName(){
		return this._APKName;
	}
	
	public String get_APKPath(){
		return this._APKPath;
	}
	
	public String get_APKFramework(){
		return this._APKFramework;
	}
	
	public void set_Package(String _i){
		this._Package = _i;
	}
	
	public void set_APKName(String _name){
		this._APKName = _name;
	}
	
	public void set_APKPath(String _path){
		this._APKPath = _path;
	}
	
	public void set_APKFramework(String _framework){
		this._APKFramework = _framework;
	}
}

