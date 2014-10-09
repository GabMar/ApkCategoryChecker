/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class analyze a given path: if the path point to an APK file will be called a istance of APKAnalyzer,
 * if the path point a directory, the directory will be scanned and the APKs contained will be analyzed
 * 
 * @author Gabriele Martini
 *
 */
public class PathAnalyzer {
    
    /*--Attributes--*/
    
    /**
     * The Arraylist of AnalyzerResult
     */
    private final ArrayList resultList = new ArrayList<>();
    /**
     * The istance of APKAnalyzer
     */
    private APKAnalyzer apkAnalyzer;
    /**
     * Istance of AnalyzerResult contained the result of a single APK
     */
    private AnalyzerResult _analyzerResult; 

    /*--Methods--*/
    
    /**
     * 
     * @param _givenPath The given path from CLI interface
     * @param _keepDecodedPath If "true" the directory containing the decoded APK will be manteined
     * @return Return an ArrayList of AnalyzerResult
     * @throws IOException
     */
    public ArrayList Analyze(String _givenPath, boolean _keepDecodedPath) throws IOException {
        
            File file_path = new File(_givenPath);
            
            //Check if is an APK file
            if(file_path.isFile()){
                if(file_path.getAbsolutePath().contains(".apk")){
                    apkAnalyzer = new APKAnalyzer();
                    _analyzerResult = apkAnalyzer.Analyze(_givenPath, file_path.getName(), _keepDecodedPath);
                    this.resultList.add(_analyzerResult);
                }
            }else if(file_path.isDirectory()){
                File[] listOfFiles = file_path.listFiles();
                int length = listOfFiles.length;
                for (int i = 0; i < length; i++) {
                    if (listOfFiles[i].isFile()) {
                        //Decode
                        this.Analyze(listOfFiles[i].getAbsolutePath(), _keepDecodedPath);
                      } else if (listOfFiles[i].isDirectory()) {
                            this.Analyze(listOfFiles[i].getPath(), _keepDecodedPath);
                      }
                }
            }
            
        return this.resultList;
    }

}
