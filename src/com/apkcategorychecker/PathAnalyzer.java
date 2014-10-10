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
