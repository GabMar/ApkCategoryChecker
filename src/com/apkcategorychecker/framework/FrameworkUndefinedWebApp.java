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
package com.apkcategorychecker.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.apkcategorychecker.cli.CommandLineInterface;

/**
 * UndefinedWebApp Framework
 *
 * @author Gabriele Martini
 */
public class FrameworkUndefinedWebApp implements Framework{

	/**
	 * Name of Framework
	 */
    private final String FrameworkName = "Undefined_Web_App";
    
    /**
     * Boolean to check if this Framework uses Apache Cordova
     */
    private boolean isCordova = false;
    
    /**
     * Boolean to check if the APK matches the Framework
     */
    private boolean UndefinedWebApp = false;
    
    /**
     * Boolean used by the method searchString
     */
    private boolean founded = false;
    
    /**
     * Level of deepness
     */
    int _deep = CommandLineInterface.getInstance().getDeep();
    
    /**
     * Number of Web Resource File founded
     */
    int i_res = 0;

    @Override
    public boolean Test(String _pathToAnalyze) {
        this.UndefinedWebApp = this.searchFile(_pathToAnalyze);
        return this.UndefinedWebApp;
    }

    @Override
    public String getFrameworkName() {
        return this.FrameworkName;
    }

    @Override
    public String getPackage(String _pathToAnalyze) {
        
        String target = "package";
        String path = _pathToAnalyze+"/AndroidManifest.xml";
        String Fpackage = null;
        try {
            File file = new File(path);
            BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                new FileInputStream(file)));
            String line;
            while((line = br.readLine()) != null) {
                if(line.indexOf(target) != -1)
                    break;
            }
            String[] s = line.split("package=");
            Fpackage = s[1].substring(1, s[1].length() -2);
            br.close();
        } catch(IOException e) {
            System.out.println("read error: " + e.getMessage());
        }
        
        
        return Fpackage;
    }

    @Override
    public boolean checkCordova() {
        return this.isCordova;
    }
    
    @Override
    public void setoff(){
        this.UndefinedWebApp = false;
        this.founded = false; 
        this.i_res = 0;
    };
    
    
    /**
     * Method to search Web Resource Files
     * 
     * @param _pathSearch Path of file
     * @return
     */
    private boolean searchFile(String _pathSearch){
        
        if(this.founded == false){
            
        	File search_file_path = new File(_pathSearch);

            //If File
            if(search_file_path.isFile()){
                
            	if(	search_file_path.getAbsolutePath().contains(".html") ||
            		search_file_path.getAbsolutePath().contains(".js")	||
            		search_file_path.getAbsolutePath().contains(".css")){
            		
            		this.i_res = this.i_res + 1;
            	}

            }else if(search_file_path.isDirectory()){
                File[] listOfFiles = search_file_path.listFiles();
                int length = listOfFiles.length;
                for (int i = 0; i < length; i++) {
                    if (listOfFiles[i].isFile()) {
                        this.searchFile(listOfFiles[i].getAbsolutePath());
                      } else if (listOfFiles[i].isDirectory()) {
                        this.searchFile(listOfFiles[i].getAbsolutePath());
                      }
                }
            }
        }
        
        if(this.i_res >= this._deep){
        	this.founded = true;
        }
        return this.founded;
    }
    
}
