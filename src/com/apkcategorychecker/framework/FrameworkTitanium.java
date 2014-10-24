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
 * Titanium Framework
 *
 * @author Gabriele Martini
 */
public class FrameworkTitanium implements Framework{

	/**
	 * Name of Framework
	 */
    private final String FrameworkName = "Titanium";
    
    /**
     * Boolean to check if this Framework uses Apache Cordova
     */
    private boolean isCordova = false;
    
    /**
     * Boolean to check if the APK matches the Framework
     */
    private boolean Titanium = false;

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
    	boolean _boolFile1, _boolFile2 = false;
    	ToolSearch Searcher = new ToolSearch();
    	_boolFile1 = Searcher.searchFile(_pathToAnalyze, "TitaniumModule.class");
    	_boolFile2 = Searcher.searchFile(_pathToAnalyze, "TiActivity.class");
    	if(_boolFile1 && _boolFile2){
    		this.Titanium = true;
    	}
        if(this.Titanium){
            this.setWebResources(_pathToAnalyze);
        }
        return this.Titanium;
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
        this.Titanium = false;
        this._html = 0;
        this._javascript = 0;
        this._css = 0;
    };
    
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
