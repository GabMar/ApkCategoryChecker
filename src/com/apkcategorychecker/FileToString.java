/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 *
 * @author gabriele
 */
public class FileToString {
    
    public String readFile(String pathname)  {

	    File file = new File(pathname);
	    StringBuilder fileContents = new StringBuilder((int)file.length());
	    Scanner scanner;
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
