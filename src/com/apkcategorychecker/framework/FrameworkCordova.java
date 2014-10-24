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

import com.apkcategorychecker.tool.ToolSearch;

/**
 * Cordova Framework
 *
 * @author Gabriele Martini
 */
public class FrameworkCordova implements Framework {
    
	/**
	 * Name of Framework
	 */
    private final String FrameworkName = "Apache Cordova";
    
    /**
     * Boolean to check if this Framework uses Apache Cordova
     */
    private final boolean isCordova = true;
    
    /**
     * Boolean to check if the APK matches the Framework
     */
    private boolean Cordova = false;

    /**
     * Number of html files
     */
    private int _html = 0;
    
    /**
     * Number of javascript files
     */
    private int _javascript = 0;
    
    /**
     * Number of css files
     */
    private int _css = 0;

    @Override
    public boolean Test(String _pathToAnalyze) {
        this.control(_pathToAnalyze);
        if(this.Cordova){
            this.setWebResources(_pathToAnalyze);
        }
        return this.Cordova;
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
        this.Cordova = false;
        this._html = 0;
        this._javascript = 0;
        this._css = 0;
    }
    
    /**
     * Multiple control of framework
     * 
     * @param _path APK decoded path
     */
    private void control(String _path){
    	boolean _boolString, _boolFile = false;
    	ToolSearch Searcher = new ToolSearch();
    	_boolString = Searcher.searchStringInFileText(_path+"/res/xml/config.xml", "org.apache.cordova");
    	_boolFile = Searcher.searchFile(_path, "CordovaActivity.class");
    	if(_boolString && _boolFile){
    		this.Cordova = true;
    	}
    }

    private void setWebResources(String _pathToAnalyze){
        
        File search_file_path = new File(_pathToAnalyze);

        /*--If _pathToAnalyze is a file update the counters, else if is 
         * a directory call this.setWebResources--*/
        
        if(search_file_path.isFile()){
            
            if( search_file_path.getAbsolutePath().contains(".html")){
                
                this._html = this._html + 1;
                
            }else 
            
            if( search_file_path.getAbsolutePath().contains(".js")){
                
                this._javascript = this._javascript + 1;
                
            }else 
                
            if( search_file_path.getAbsolutePath().contains(".css")){
                
                this._css = this._css + 1;
            }

        }else if(search_file_path.isDirectory()){
            File[] listOfFiles = search_file_path.listFiles();
            int length = listOfFiles.length;
            for (int i = 0; i < length; i++) {
                if (listOfFiles[i].isFile()) {
                    this.setWebResources(listOfFiles[i].getAbsolutePath());
                  } else if (listOfFiles[i].isDirectory()) {
                    this.setWebResources(listOfFiles[i].getAbsolutePath());
                  }
            }
        }
    }
        
    

    @Override
    public int getHtml() {
        
        return this._html;
    }

    @Override
    public int getJavascript() {
        
        return this._javascript;
    }

    @Override
    public int getCSS() {
        
        return this._css;
    }

    
}
