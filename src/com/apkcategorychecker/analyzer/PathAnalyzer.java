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
package com.apkcategorychecker.analyzer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import brut.androlib.AndrolibException;

import com.apkcategorychecker.cli.CommandLineInterface;
import com.apkcategorychecker.json.FactoryJsCSVBuilder;
import com.apkcategorychecker.json.JsCSVBuilder;
import com.apkcategorychecker.tool.ToolDecoder;
import com.apkcategorychecker.tool.ToolDeleteDirectory;
import com.apkcategorychecker.writer.FactoryWriter;
import com.apkcategorychecker.writer.Writer;

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
     * Counter
     */
    private int counter = 0;
	
    /**
     * The Arraylist of JsonElement
     */
    private final ArrayList<JsElement> jsonList = new ArrayList<JsElement>();
    
    /**
     * The istance of APKAnalyzer
     */
    private APKAnalyzer apkAnalyzer;
    
    /**
     * Istance of JsonElement containing the list of js files
     */
    private JsElement _jsonElement;

    /*--Methods--*/
    
    /**
     * Method to analyze the given path
     * 
     * @throws IOException
     * @throws AndrolibException
     */
    public void Analyze() throws IOException, AndrolibException{
    	
    	/*--Get the path to analyze from the command-line--*/
    	
    	String givenPath = CommandLineInterface.getInstance().getPath();
		
    	/*--Get the boolean keep--*/
    	
    	boolean keepDecodedPath = CommandLineInterface.getInstance().getKeep();
    	
    	/*--Get the path of decoded APK--*/
    	
    	String _outDecoded = CommandLineInterface.getInstance().getDecodedPath();
    	
    	/*--Get the output directory pf result file--*/
    	
    	String _outDir = CommandLineInterface.getInstance().getOutDir();
    	
    	/*--Get the current time*/
		
    	String time = this.GetTime();
    	
    	/*Create the CSV file*/
    	
    	String csvPath = this.createCSV(_outDir, time);
		
    	/*--Analyze the Path--*/
    	
		try {
			this.AnalyzePath(givenPath, keepDecodedPath, _outDecoded, csvPath);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*--Build a json file containing the name of all javascript files--*/
		
		this.BuildJson(_outDir, time);
    }
    
    /**
     * Build the json file containing the list of js files
     * 
     * @param outDir
     * @param time
     */
    private void BuildJson(String outDir, String time){
    	
    	/*--Instance of JsonBuilder--*/
    	JsCSVBuilder builder;
    	
    	/*--Choose the correct builder--*/
    	
    	builder = FactoryJsCSVBuilder.getInstance().getJsCSVBuilder();
    	
    	/*--Build the json file--*/
    	
    	builder.BuildJs(this.jsonList, outDir, time);
    }
    
    private String GetTime(){
    	DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss");
    	Date date = new Date();
    	return dateFormat.format(date);
    }

	/**
     * Recursive method to find through a directory
     * 
     * @param _givenPath Path to analyze
     * @param _keepDecodedPath If "true" the directory containing the decoded APK will be maintained
     * @throws IOException
	 * @throws AndrolibException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
     */
	private void AnalyzePath(String _givenPath, boolean _keepDecodedPath, String _outDecoded, String _csvPath) throws IOException, AndrolibException, ParserConfigurationException, SAXException {
		
		/*--Create a new file from given path--*/
    	
        File file_path = new File(_givenPath);
        
        /*--Recursive method to find an APK file from the given path--*/
        
        if(file_path.isFile()){
            if(file_path.getAbsolutePath().contains(".apk")){
            	/*Instance of ToolDecoder; decode an APK file in a Directory with the same name of APK*/
                ToolDecoder tooldecoder = new ToolDecoder();
                String _decodedApkPath = tooldecoder.DecodeApk(_givenPath, _outDecoded);
                apkAnalyzer = new APKAnalyzer();
                apkAnalyzer.Analyze(_givenPath, _decodedApkPath, file_path.getName(), _outDecoded, System.currentTimeMillis(), _csvPath, counter);
                this._jsonElement = apkAnalyzer.AnalyzeJsFiles(_decodedApkPath);
                this.jsonList.add(this._jsonElement);
                this.deleteDirectory(_decodedApkPath, _keepDecodedPath);
                this.counter = this.counter + 1;
            }
        }else if(file_path.isDirectory()){
            File[] listOfFiles = file_path.listFiles();
            int length = listOfFiles.length;
            for (int i = 0; i < length; i++) {
                if (listOfFiles[i].isFile()) {
                    //Decode
                    this.AnalyzePath(listOfFiles[i].getPath(), _keepDecodedPath, _outDecoded, _csvPath);
                  } else if (listOfFiles[i].isDirectory()) {
                        this.AnalyzePath(listOfFiles[i].getPath(), _keepDecodedPath, _outDecoded, _csvPath);
                  }
            }
        }
		
	}

	private void deleteDirectory(String _decodedApkPath, boolean _keepDecodedPath) throws IOException {
	
		/*If _keepDecodedPath is true do nothing, if false remove the directory
         * containing the decoded APK*/
        if(_keepDecodedPath == false){
        	ToolDeleteDirectory.getInstance().DeleteDirectory(_decodedApkPath);
            
        }
		
		
	}
	
	private String createCSV(String _destinationPath, String time){
		
		String format = CommandLineInterface.getInstance().getWriterFormat();
		
		/*--Instance of Writer--*/
    	
    	Writer writer;
    	
    	/*--Choose the correct writer--*/
    	
    	writer = FactoryWriter.getInstance().getWriter(format);
    	
    	
		
		String _pathCSV = "";
		
		/*Check if _destinationPath is an APK or a Directory in order to
         *get the right destination path
         */
         
         File Destination = new File(_destinationPath);
         if(!Destination.exists()){
         	Destination.mkdir();
         }
         if(Destination.isDirectory()){
             _pathCSV = _destinationPath;
         }else if(Destination.isFile()){
             _pathCSV = _destinationPath.substring(0, _destinationPath.length() - 4);
         }
         
         File _fileCSV = new File(_pathCSV+"/Results_"+time+".csv");
         
         /*--Write the results--*/
     	
         try {
			writer.createHeader(_fileCSV.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
         return _fileCSV.getAbsolutePath();
	}

}
