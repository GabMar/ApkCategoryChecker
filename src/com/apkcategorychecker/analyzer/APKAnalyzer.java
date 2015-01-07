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


import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import brut.androlib.AndrolibException;

import com.apkcategorychecker.framework.Framework;
import com.apkcategorychecker.framework.FrameworkPool;
import com.apkcategorychecker.tool.ToolApkParameters;
import com.apkcategorychecker.tool.ToolDex2Jar;
import com.apkcategorychecker.tool.ToolJar2Class;
import com.googlecode.dex2jar.DexException;

//import org.apache.commons.io.IOUtils;


/**
 * This class analyze an APK file
 * @author Gabriele Martini
 *
 */
public class APKAnalyzer{
    
	/*--Attributes--*/
    
    /**
     * List of Frameworks
     */
    private ArrayList<Framework> listOfFramework;
    
    /**
     * Object containing the result of the Analysis
     */
    private AnalyzerResult _results;
    
    /**
     * Object containing the list of javascript files
     */
    private JsElement _jsonElement;
    
    /**
     * Constructor
     */
    public APKAnalyzer() {
        this._results = null;
        this._jsonElement = null;
    }

    /**
     * 
     * @param path Given path of APK
     * @param apkName Name of APK
     * @param _keepDecodedPath If "true" the directory containing the decoded APK will be mantained
     * @return
     * @throws IOException
     * @throws AndrolibException 
     */
    public AnalyzerResult Analyze(String path, String _decodedApkPath, String apkName, String _outDecoded, long _startTime) throws IOException, AndrolibException {
        try {
        	
            /*If an error occurred while decoding apk set minimal information*/
            if(_decodedApkPath == null){
            	this._results = this.setResults("UNDEFINED", 
                                    path, 
                                    apkName,
                                    "UNDEFINED",
                                    false,
                                    0,
                                    0,
                                    0,
                                    "UNDEFINED",
                                    "UNDEFINED",
                                    "UNDEFINED",
                                    "UNDEFINED",
                                    "UNDEFINED",
                                    "UNDEFINED",
                                    _startTime,
                                    1);
            	return this._results;
            }
            
            /*Instance of ToolDex2Jar; convert the .dex file in .jar file*/
            ToolDex2Jar dex2jar = new ToolDex2Jar();
            try {
            dex2jar.ConvertDex2Jar(_decodedApkPath.concat("/classes.dex"), _decodedApkPath);
            } catch(DexException ex){
            	System.err.println("Unable to convert file classes.dex, writing minimal results.");
            	this._results = this.setResults(ToolApkParameters.getInstance().getPackage(_decodedApkPath), 
                        path, 
                        apkName,
                        "UNDEFINED",
                        false,
                        ToolApkParameters.getInstance().getHtml(_decodedApkPath),
                        ToolApkParameters.getInstance().getJavascript(_decodedApkPath),
                        ToolApkParameters.getInstance().getCSS(_decodedApkPath),
                        ToolApkParameters.getInstance().getDebuggable(_decodedApkPath),
                        ToolApkParameters.getInstance().getPermission(_decodedApkPath),
                        ToolApkParameters.getInstance().getSdkVersion(_decodedApkPath, "min"),
                        ToolApkParameters.getInstance().getSdkVersion(_decodedApkPath, "max"),
                        ToolApkParameters.getInstance().getSdkVersion(_decodedApkPath, "target"),
                        ToolApkParameters.getInstance().getFileSize(path),
                        _startTime,
                        1);
            	return this._results;
            }
            
            /*--Instance of ToolJar2Class; extract the content of a .jar--*/
            ToolJar2Class jar2class = new ToolJar2Class();
            jar2class.ConvertJar2Class(_decodedApkPath + "/classes/ApkDecodeddex2jar.jar", _decodedApkPath);
            
            /*Get the list of Objects Framework*/
            this.listOfFramework = FrameworkPool.getInstance().getFramework();
            
            
            /*For every Framework analyze the Decoded APK; if the Framework match return true*/
            for (int i = 0; i < this.listOfFramework.size(); i++) {
            	
                if(listOfFramework.get(i).Test(_decodedApkPath)){
                    
                	/*If the Framework match, set the AnalyzerResult Object*/
                    this._results = this.setResults(ToolApkParameters.getInstance().getPackage(_decodedApkPath), 
                                    path, 
                                    apkName,
                                    listOfFramework.get(i).getFrameworkName(),
                                    this.listOfFramework.get(i).checkCordova(),
                                    ToolApkParameters.getInstance().getHtml(_decodedApkPath),
                                    ToolApkParameters.getInstance().getJavascript(_decodedApkPath),
                                    ToolApkParameters.getInstance().getCSS(_decodedApkPath),
                                    ToolApkParameters.getInstance().getDebuggable(_decodedApkPath),
                                    ToolApkParameters.getInstance().getPermission(_decodedApkPath),
                                    ToolApkParameters.getInstance().getSdkVersion(_decodedApkPath, "min"),
                                    ToolApkParameters.getInstance().getSdkVersion(_decodedApkPath, "max"),
                                    ToolApkParameters.getInstance().getSdkVersion(_decodedApkPath, "target"),
                                    ToolApkParameters.getInstance().getFileSize(path),
                                    _startTime,
                                    0);
                	}
                	/*If none of the Framework is found, set the AnalyzerResult for Native App*/
                	else if(!listOfFramework.get(i).Test(_decodedApkPath)){
                    if(this._results == null){this._results = this.setResults(ToolApkParameters.getInstance().getPackage(_decodedApkPath), 
                                    path, 
                                    apkName,
                                    "Native",
                                    false,
                                    ToolApkParameters.getInstance().getHtml(_decodedApkPath),
                                    ToolApkParameters.getInstance().getJavascript(_decodedApkPath),
                                    ToolApkParameters.getInstance().getCSS(_decodedApkPath),
                                    ToolApkParameters.getInstance().getDebuggable(_decodedApkPath),
                                    ToolApkParameters.getInstance().getPermission(_decodedApkPath),
                                    ToolApkParameters.getInstance().getSdkVersion(_decodedApkPath, "min"),
                                    ToolApkParameters.getInstance().getSdkVersion(_decodedApkPath, "max"),
                                    ToolApkParameters.getInstance().getSdkVersion(_decodedApkPath, "target"),
                                    ToolApkParameters.getInstance().getFileSize(path),
                                    _startTime,
                                    0);}
                }
        }
        } catch (ParserConfigurationException | SAXException ex) {
            Logger.getLogger(APKAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return this._results;
    }
    
    public JsElement AnalyzeJsFiles(String _decodedApkPath) throws ParserConfigurationException, SAXException, IOException{
    	
    	if(_decodedApkPath == null){
    		this._jsonElement = this.setElement("UNDEFINED", "UNDEFINED");
    		return this._jsonElement;
    	}
    	
    	this._jsonElement = this.setElement(
    			ToolApkParameters.getInstance().getPackage(_decodedApkPath), 
    			ToolApkParameters.getInstance().getJsFiles(_decodedApkPath)
    			);
    	
    	return this._jsonElement;
    }
    
    
    /**
     * Set the results
     * 
     * @param _apkpackage
     * @param _apkpath
     * @param _apkname
     * @param _framework
     * @param _checkCordova
     * @param _html
     * @param _javascript
     * @param _css
     * @param _debuggable
     * @param _permissions
     * @param _minSdkVersion
     * @param _maxSdkVersion
     * @param _targetSdkVersion
     * @param _fileSize
     * @param _startTime
     * @param _flag
     * @return
     */
    private AnalyzerResult setResults(  String _apkpackage, 
                                        String _apkpath, 
                                        String _apkname, 
                                        String _framework,
                                        boolean _checkCordova,
                                        int _html,
                                        int _javascript,
                                        int _css,
                                        String _debuggable,
                                        String _permissions,
                                        String _minSdkVersion,
                                        String _maxSdkVersion,
                                        String _targetSdkVersion,
                                        String _fileSize,
                                        long _startTime,
                                        int _flag){
    	
    	/*Instance of AnalyzerResult*/
        AnalyzerResult _settedResults = new AnalyzerResult();
        
        /*Set the Package*/
        _settedResults.set_Package(_apkpackage);
        
        /*Set the Path*/
        _settedResults.set_APKPath(_apkpath);
        
        /*Set the Name*/
        _settedResults.set_APKName(_apkname);
        
        /*Set the Framework used*/
        if(_checkCordova && !"Apache Cordova".equals(_framework)){
            _settedResults.set_APKBaseFramework("Apache Cordova");
            _settedResults.set_APKMainFramework(_framework);
        }else if(_checkCordova && "Apache Cordova".equals(_framework)){
        	_settedResults.set_APKBaseFramework("");
            _settedResults.set_APKMainFramework(_framework);
        }else if(!_checkCordova){
        	_settedResults.set_APKBaseFramework("");
            _settedResults.set_APKMainFramework(_framework);
        }
        
        /*Set the number of html files*/
        _settedResults.set_html(_html);
        
        /*Set the number of javascript files*/
        _settedResults.set_js(_javascript);
        
        /*Set the number of css files*/
        _settedResults.set_css(_css);
        
        /*Set the debuggable parameter*/
        _settedResults.set_debuggable(_debuggable);
        
        /*List of Android Permission*/
        _settedResults.set_permissions(_permissions);
        
        /*Value of minSdkVersion*/
        _settedResults.set_minSdkVersion(_minSdkVersion);
        
        /*Value of maxSdkVersion*/
        _settedResults.set_maxSdkVersion(_maxSdkVersion);
        
        /*Value of taregtSdkVersion*/
        _settedResults.set_targetSdkVersion(_targetSdkVersion);
        
        /*Set the File Size*/
        _settedResults.set_fileSize(_fileSize);
        
        /*Set the Start Time in milliseconds*/
        _settedResults.set_startAnalysis(_startTime);
        
        /*Set the Duration Time in milliseconds*/
        _settedResults.set_durationAnalysis(System.currentTimeMillis() - _startTime);
        
        /*Set the Decode Flag*/
        _settedResults.set_decodeSuccess(_flag);
        
        return _settedResults;
    }
    
    private JsElement setElement(String _appId, String _jsList){
    	
    	/*Instance of JsonElement*/
    	JsElement _element = new JsElement();
    	
    	/*Set the App Id*/
    	_element.set_id(_appId);
    	
    	/*Set the list of js files*/
    	_element.set_JsFiles(_jsList);
    	
    	return _element;
    };

}

