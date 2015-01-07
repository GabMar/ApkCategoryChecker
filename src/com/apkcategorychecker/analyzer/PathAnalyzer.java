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
import com.apkcategorychecker.json.FactoryJsonBuilder;
import com.apkcategorychecker.json.JsonBuilder;
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
     * The Arraylist of AnalyzerResult
     */
    private final ArrayList<AnalyzerResult> resultList = new ArrayList<AnalyzerResult>();
    
    /**
     * The Arraylist of JsonElement
     */
    private final ArrayList<JsonElement> jsonList = new ArrayList<JsonElement>();
    
    /**
     * The istance of APKAnalyzer
     */
    private APKAnalyzer apkAnalyzer;
    
    /**
     * Istance of AnalyzerResult containing the result of a single APK
     */
    private AnalyzerResult _analyzerResult; 
    
    /**
     * Istance of JsonElement containing the list of js files
     */
    private JsonElement _jsonElement;

    /*--Methods--*/
    
    /**
     * Method to analyze the given path
     * 
     * @throws IOException
     * @throws AndrolibException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     */
    public void Analyze() throws IOException, AndrolibException{
    	
    	/*--Get the path to analyze from the command-line--*/
    	
    	String givenPath = CommandLineInterface.getInstance().getPath();
		
    	/*--Get the boolean keep--*/
    	
    	boolean keepDecodedPath = CommandLineInterface.getInstance().getKeep();
    	
    	/*--Get the path of decoded APK--*/
    	
    	String _outDecoded = CommandLineInterface.getInstance().getDecodedPath();
    	
    	/*--Get the format of result file--*/
    	
    	String _format = CommandLineInterface.getInstance().getWriterFormat();
    	
    	/*--Get the output directory pf result file--*/
    	
    	String _outDir = CommandLineInterface.getInstance().getOutDir();
		
    	/*--Analyze the Path--*/
    	
		try {
			this.AnalyzePath(givenPath, keepDecodedPath, _outDecoded);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*--Get the current time*/
		String time = this.GetTime();
        
    	/*--Write the results in a file--*/
		
		this.Write(_format, _outDir, time);
		
		/*--Build a json file containing the name of all javascript files--*/
		
		this.BuildJson(_outDir, time);
    }

    /**
     * Write the results in a file
     * 
     * @param format Format of result file
     * @param outDir Directory of result file
     * @param analyzerResult List of results
     */
    private void Write(String format, String outDir, String time) {
		
    	/*--Instance of Writer--*/
    	
    	Writer writer;
    	
    	/*--Choose the correct writer--*/
    	
    	writer = FactoryWriter.getInstance().getWriter(format);
    	
    	/*--Write the results--*/
    	
    	writer.Write(this.resultList, outDir, time);
		
	}
    
    /**
     * Build the json file containing the list of js files
     * 
     * @param outDir
     * @param time
     */
    private void BuildJson(String outDir, String time){
    	
    	/*--Instance of JsonBuilder--*/
    	JsonBuilder builder;
    	
    	/*--Choose the correct builder--*/
    	
    	builder = FactoryJsonBuilder.getInstance().getJsonBuilder();
    	
    	/*--Build the json file--*/
    	
    	builder.BuildJson(this.jsonList, outDir, time);
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
	private void AnalyzePath(String _givenPath, boolean _keepDecodedPath, String _outDecoded) throws IOException, AndrolibException, ParserConfigurationException, SAXException {
		
		/*--Create a new file from given path--*/
    	
        File file_path = new File(_givenPath);
        
        /*--Recursive method to find an APK file from the given path--*/
        
        if(file_path.isFile()){
            if(file_path.getAbsolutePath().contains(".apk")){
            	/*Instance of ToolDecoder; decode an APK file in a Directory with the same name of APK*/
                ToolDecoder tooldecoder = new ToolDecoder();
                String _decodedApkPath = tooldecoder.DecodeApk(_givenPath, _outDecoded);
                apkAnalyzer = new APKAnalyzer();
                this._analyzerResult = apkAnalyzer.Analyze(_givenPath, _decodedApkPath, file_path.getName(), _outDecoded, System.currentTimeMillis());
                this._jsonElement = apkAnalyzer.AnalyzeJsFiles(_decodedApkPath);
                this.resultList.add(this._analyzerResult);
                this.jsonList.add(this._jsonElement);
                this.deleteDirectory(_decodedApkPath, _keepDecodedPath);
            }
        }else if(file_path.isDirectory()){
            File[] listOfFiles = file_path.listFiles();
            int length = listOfFiles.length;
            for (int i = 0; i < length; i++) {
                if (listOfFiles[i].isFile()) {
                    //Decode
                    this.AnalyzePath(listOfFiles[i].getPath(), _keepDecodedPath, _outDecoded);
                  } else if (listOfFiles[i].isDirectory()) {
                        this.AnalyzePath(listOfFiles[i].getPath(), _keepDecodedPath, _outDecoded);
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

}
