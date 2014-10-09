/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

import java.io.IOException;
import java.util.ArrayList;
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
public class Main {

	/**
	 * @param args
	 */
	private static String _CSVPath;
	private static String _givenPath;
        private static ArrayList resultList = new ArrayList<>();
        private static String _choosedResultFormat;
	
	public static void main(String[] args) throws IOException {
		
		//CLI options creation
		
		Options options = new Options();
		options.addOption("p", true, "Path of APK or Directory containing APKs");
		options.addOption("csv", false, "To obtain a CSV file result");
                options.addOption("o", true, "Destination path for file result");
                options.addOption("k", false, "Keep the decoded APK on the filesystem");
		
		//CLI parser
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd;
		try{
			cmd = parser.parse(options, args);
		}catch (ParseException pe){ usage(options); return; }
		
		//CLI option switch
		
		if(cmd.hasOption("p")){
                    _givenPath = cmd.getOptionValue("p");
                    
                    //Analyzer Instance
                    
                    PathAnalyzer analyzer;
                    analyzer = new PathAnalyzer();
                    
                    //Boolean for the Decoded Apk
                    
                    boolean _keep = false;
                    if(cmd.hasOption("k")){ 
                        _keep = true;
                    }
                    
                    //Analyze given path; return a ArrayList of results
                    
                    resultList = analyzer.Analyze(_givenPath, _keep);
                    
                    //Writing results in a file
                    
                    Writer writer;
                    
                    //Instance a different writer depending on passed cli option
                    
                    if(cmd.hasOption("csv")){
                        _choosedResultFormat = "csv";
                    }
                    
                    writer = FactoryWriter.getInstance().getWriter(_choosedResultFormat);
                    
                    //Create a choosed format file in a given path or in the current directory 
                    //depending on passed optionn's argument 
                    if(cmd.hasOption("o")){
                        writer.Write(resultList, cmd.getOptionValue("o"));
                    }else {
                        writer.Write(resultList, System.getProperty("user.dir"));
                    }
		}
		

	}
	
	private static void usage(Options options){

		// Command line Help Usage
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "CategoryChecker", options );
	}

}
