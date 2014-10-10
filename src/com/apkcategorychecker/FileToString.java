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
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * This class convert a text file to a String
 *
 * @author Gabriele Martini
 */
public class FileToString {
    
	/**
	 * 
	 * @param pathname Path of the file
	 * @return
	 */
    public String readFile(String pathname)  {

    	/*Create the File*/
	    File file = new File(pathname);
	    
	    /*Create the StringBuilder*/
	    StringBuilder fileContents = new StringBuilder((int)file.length());
	    
	    /*Instance of Scanner*/
	    Scanner scanner;
	    
	    /*Conversion*/
		try {
			scanner = new Scanner(file);
			String lineSeparator = System.getProperty("line.separator");
			try {
		        while(scanner.hasNextLine()) {        
		            fileContents.append(scanner.nextLine()).append(lineSeparator);
		        }
		        
		    } finally {
		        scanner.close();
		    }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fileContents.toString();
	    

	    
	}
    
}
