/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

import brut.androlib.AndrolibException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Gabriele Martini
 *
 */
public class APKAnalyzer{
    
    private String _decodedApkPath;
    private ArrayList<Framework> listOfFramework;
    private AnalyzerResult _results;
    private boolean _keepDecodedPath;

    public APKAnalyzer() {
        this._results = null;
    }

    public AnalyzerResult Analyze(String path, String apkName, boolean _keepDecodedPath) throws IOException {
        try {
            ToolDecoder tooldecoder = new ToolDecoder();
            _decodedApkPath = tooldecoder.DecodeApk(path);
            this.listOfFramework = FrameworkPool.getInstance().getFramework();
            for (int i = 0; i < this.listOfFramework.size(); i++) {
                listOfFramework.get(i).setoff();
                if(listOfFramework.get(i).Test(_decodedApkPath)){
                    
                    this._results = this.setResults(listOfFramework.get(i).getPackage(_decodedApkPath), 
                                    path, 
                                    apkName,
                                    listOfFramework.get(i).getFrameworkName(),
                                    this.listOfFramework.get(i).checkCordova());
                }else if(!listOfFramework.get(i).Test(_decodedApkPath)){
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
        
        if(_keepDecodedPath == false){
            File _pathToErase;
            _pathToErase = new File(_decodedApkPath);
            this.DeleteDirectory(_pathToErase);
            
        }
        
        return this._results;
    }
    
    private AnalyzerResult setResults(  String _apkpackage, 
                                        String _apkpath, 
                                        String _apkname, 
                                        String _framework,
                                        boolean _checkCordova){
        AnalyzerResult _settedResults = new AnalyzerResult();
        _settedResults.set_Package(_apkpackage);
        _settedResults.set_APKPath(_apkpath);
        _settedResults.set_APKName(_apkname);
        if(_checkCordova && !"Apache Cordova".equals(_framework)){
            _settedResults.set_APKFramework("Apache Cordova + " + _framework);
        }else if(_checkCordova && "Apache Cordova".equals(_framework)){
            _settedResults.set_APKFramework(_framework);
        }else if(!_checkCordova){
            _settedResults.set_APKFramework(_framework);
        }
        return _settedResults;
    }
    
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

