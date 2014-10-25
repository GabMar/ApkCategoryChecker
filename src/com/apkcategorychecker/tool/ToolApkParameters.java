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
package com.apkcategorychecker.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Get the package of APK, the number of html, css and javascript file
 * 
 * @author Gabriele Martini
 *
 */
public class ToolApkParameters {
	
	/**
	 * Unique instance - Pattern Singleton.
	 */
	private static ToolApkParameters instance = null;
	
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
	
	/**
	 * Private Constructor - Pattern Singleton
	 */
	private ToolApkParameters() {}
	
	/**
	 * Method for get instance - Pattern Singleton.
	 * 
	 * @return
	 */
	public static ToolApkParameters getInstance() {
    	if(instance == null) {
            instance = new ToolApkParameters();
         }
         return instance;
    }
	
	/**
	 * Return the package of the APK
	 * 	
	 * @param _pathToAnalyze Path of decoded APK
	 * @return
	 */
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
	
	/**
	 * Set the numbers of html, css and javascript files
	 * 
	 * @param _pathToAnalyze Path of decoded APK
	 */
	public void setWebResources(String _pathToAnalyze){
    	
    	File search_file_path = new File(_pathToAnalyze);

        /*--If _pathToAnalyze is a file update the counters, else if is 
         * a directory call this.setWebResources--*/
    	
        if(search_file_path.isFile()){
            
        	if(	search_file_path.getAbsolutePath().contains(".html")){
        		
        		this._html = this._html + 1;
        		
        	}else 
    		
    		if(	search_file_path.getAbsolutePath().contains(".js")){
        		
        		this._javascript = this._javascript + 1;
        		
        	}else 
        		
    		if(	search_file_path.getAbsolutePath().contains(".css")){
        		
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
	
	/**
	 * Return the number of html files
	 * 
	 * @return
	 */
	public int getHtml() {
		
		int html = this._html;
		this._html = 0;
		return html;
	}

	/**
	 * Return the number of javascript files
	 * 
	 * @return
	 */
	public int getJavascript() {
		
		int javascript = this._javascript;
		this._javascript = 0;
		return javascript;
	}

	/**
	 * Return the number of css files
	 * 
	 * @return
	 */
	public int getCSS() {
		
		int css = this._css;
		this._css = 0;
		return css;
	}
	
	}
