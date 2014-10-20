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

import brut.androlib.AndrolibException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.apkcategorychecker.framework.Framework;
import com.apkcategorychecker.framework.FrameworkPool;
import com.apkcategorychecker.tool.ToolDecoder;


/**
 * This class analyze an APK file
 * @author Gabriele Martini
 *
 */
public class APKAnalyzer{
    
	/*--Attributes--*/
	
	/**
	 * Path of APK File
	 */
    private String _decodedApkPath;
    
    /**
     * List of Frameworks
     */
    private ArrayList<Framework> listOfFramework;
    
    /**
     * Object containing the result of the Analysis
     */
    private AnalyzerResult _results;
    
    /**
     * Constructor
     */
    public APKAnalyzer() {
        this._results = null;
    }

    /**
     * 
     * @param path Given path of APK
     * @param apkName Name of APK
     * @param _keepDecodedPath If "true" the directory containing the decoded APK will be mantained
     * @return
     * @throws IOException
     */
    public AnalyzerResult Analyze(String path, String apkName, boolean _keepDecodedPath) throws IOException {
        try {
        	
        	
        	/*Instance of ToolDecoder; decode an APK file in a Directory with the same name of APK*/
            ToolDecoder tooldecoder = new ToolDecoder();
            _decodedApkPath = tooldecoder.DecodeApk(path);
            
            /*Get the list of Objects Framework*/
            this.listOfFramework = FrameworkPool.getInstance().getFramework();
            
            /*For every Framework analyze the Decoded APK; if the Framework match return true*/
            for (int i = 0; i < this.listOfFramework.size(); i++) {
            	
            	
                listOfFramework.get(i).setoff();
                if(listOfFramework.get(i).Test(_decodedApkPath)){
                    
                	/*If the Framework match, set the AnalyzerResult Object*/
                    this._results = this.setResults(listOfFramework.get(i).getPackage(_decodedApkPath), 
                                    path, 
                                    apkName,
                                    listOfFramework.get(i).getFrameworkName(),
                                    this.listOfFramework.get(i).checkCordova());
                	}
                	/*If none of the Framework is found, set the AnalyzerResult for Native App*/
                	else if(!listOfFramework.get(i).Test(_decodedApkPath)){
                    if(this._results == null){this._results = this.setResults(listOfFramework.get(i).getPackage(_decodedApkPath), 
                                    path, 
                                    apkName,
                                    "Native",
                                    false);}
                }
        }
        } catch (AndrolibException ex) {
            Logger.getLogger(APKAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /*If _keepDecodedPath is true do nothing, if false remove the directory
         * containing the decoded APK*/
        if(_keepDecodedPath == false){
            File _pathToErase;
            _pathToErase = new File(_decodedApkPath);
            this.DeleteDirectory(_pathToErase);
            
        }
        
        return this._results;
    }
    
    /**
     * 
     * @param _apkpackage Package of APK
     * @param _apkpath Path of APK
     * @param _apkname Name of APK
     * @param _framework Name of Framework used
     * @param _checkCordova If true the Framework uses Apache Cordova
     * @return
     */
    private AnalyzerResult setResults(  String _apkpackage, 
                                        String _apkpath, 
                                        String _apkname, 
                                        String _framework,
                                        boolean _checkCordova){
    	
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
            _settedResults.set_APKFramework("Apache Cordova + " + _framework);
        }else if(_checkCordova && "Apache Cordova".equals(_framework)){
            _settedResults.set_APKFramework(_framework);
        }else if(!_checkCordova){
            _settedResults.set_APKFramework(_framework);
        }
        return _settedResults;
    }
    
    /**
     * Method to delete a directory
     * 
     * @param file Direcrory to delete
     * @throws IOException
     */
    private void DeleteDirectory(File file)
    throws IOException{
	 if(file.isDirectory()){
            //directory is empty, then delete it
            if(file.list().length==0){
               file.delete();
            }else{
               //list all the directory contents
               String files[] = file.list();
               for (String temp : files) {
                  //construct the file structure
                  File fileDelete = new File(file, temp);
                  //recursive delete
                 DeleteDirectory(fileDelete);
               }
               //check the directory again, if empty then delete it
               if(file.list().length==0){
                 file.delete();
               }
            }

        }else{
                //if file, then delete it
                file.delete();
        }
	    }
    

}

