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
package com.apkcategorychecker.cli;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author Gabriele Martini
 *
 */
public class CommandLineInterface {
	
	/**
	 * Unique instance - Pattern Singleton.
	 */
	private static CommandLineInterface instance = null;
	
	/**
	 * String of given path
	 */
	private String _givenPath;
	
	/**
	 * String of current directory
	 */
	private String _currentDirectory = System.getProperty("user.dir");
	
	/**
	 * Boolean for keep decoded Apk
	 */
	private boolean _keep = false;

	/**
	 * Format of result file
	 */
	private String _choosedResultFormat;
	
	/**
	 * Output directory
	 */
	private String _outDir;
	
	/**
	 * Private Constructor - Pattern Singleton
	 */
	private CommandLineInterface(String[] args) {
	
		/*--CLI options creation--*/
		
		Options options = new Options();
		
		/*--Add options--*/
		options.addOption("p", true, "Path of APK or Directory containing APKs");
		options.addOption("d", false, "Use current directory");
		options.addOption("csv", false, "To obtain a CSV file result");
        options.addOption("o", true, "Destination path for file result");
        options.addOption("k", false, "Keep the decoded APK on the filesystem");
		
		/*--CLI parser--*/
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd;
		try{
			cmd = parser.parse(options, args);
		}catch (ParseException pe){ usage(options); return; }
		
		/*--CLI options switch--*/
		
		if(cmd.hasOption("p")){
			
			/*--Take the given path--*/
            this._givenPath = cmd.getOptionValue("p");
		
		}
		
		if(cmd.hasOption("d")){
			
			this._givenPath = _currentDirectory;
		}
		
		/*--If k parameter is passed, the directory of decoded APK will be maintained--*/
        
        if(cmd.hasOption("k")){ 
            this._keep = true;
        }
        
        if(cmd.hasOption("csv")){
        	this._choosedResultFormat = "csv";
        }
        
        if(cmd.hasOption("o")){
        	this._outDir = cmd.getOptionValue("o");
        }else {
        	this._outDir = _currentDirectory;
        }
	}
	
	/* --Methods--*/
	/**
	 * Method for get instance - Pattern Singleton.
	 * 
	 * @return
	 */
	public static CommandLineInterface getInstance(String[] args) {
    	if(instance == null) {
            instance = new CommandLineInterface(args);
         }
         return instance;
    }
	
	/**
	 * Method to get the givenpath
	 * 
	 * @return
	 */
	public String getPath(){
		return this._givenPath;
	}
	
	/**
	 * Method to get the boolean Keep
	 * 
	 * @return
	 */
	public boolean getKeep(){
		return this._keep;
	}
	
	/**
	 * Method to get the result file format
	 * 
	 * @return
	 */
	public String getWriterFormat() {
		return  this._choosedResultFormat;
	}
	
	/**
	 * Method to get the output directory
	 * 
	 * @return
	 */
	public String getOutDir() {
		return this._outDir;
	}
	
	/**
	 * Method to print the Command-Line Helper
	 * 
	 * @param options CLI Options
	 */
	private static void usage(Options options){

		/*--Command line Help Usage--*/
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "CategoryChecker", options );
	}

}
