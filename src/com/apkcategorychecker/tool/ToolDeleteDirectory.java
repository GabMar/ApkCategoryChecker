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

import java.io.File;
import java.io.IOException;

public class ToolDeleteDirectory {
	
	/**
	 * Unique instance - Pattern Singleton.
	 */
	private static ToolDeleteDirectory instance = null;
	
	/**
	 * Private Constructor - Pattern Singleton
	 */
	private ToolDeleteDirectory() {}
	
	/**
	 * Method for get instance - Pattern Singleton.
	 * 
	 * @return
	 */
	public static ToolDeleteDirectory getInstance() {
    	if(instance == null) {
            instance = new ToolDeleteDirectory();
         }
         return instance;
    }
	
	/**
     * Method to delete a directory
     * 
     * @throws IOException
     */
    public void DeleteDirectory(String path) throws IOException{
    	File file = new File(path);
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
                 DeleteDirectory(fileDelete.getAbsolutePath());
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
