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
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;



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
	 * Counter
	 */
	private int _counter = 0;
	
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
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public String getPackage(String _pathToAnalyze) throws ParserConfigurationException, SAXException, IOException {
	        
	        String path = _pathToAnalyze+"/AndroidManifest.xml";
	        String Fpackage = null;
	        
	        File fXmlFile = new File(path);
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(fXmlFile);
	    	doc.getDocumentElement().normalize();
	    	
	    	NodeList nList = doc.getElementsByTagName("manifest");
	    	Node nNode = nList.item(0);
	    	Element eElement = (Element) nNode;
	    	Fpackage = eElement.getAttribute("package");
	        
	        return Fpackage;
	    }
	
	/**
	 * Return the Debuggable parameter from AndroidManifest
	 * 
	 * @param _pathToAnalyze Path of decoded APK
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public String getDebuggable(String _pathToAnalyze) throws ParserConfigurationException, SAXException, IOException {
        
        String path = _pathToAnalyze+"/AndroidManifest.xml";
        String Fpackage = null;
        
        File fXmlFile = new File(path);
    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    	Document doc = dBuilder.parse(fXmlFile);
    	doc.getDocumentElement().normalize();
    	
    	NodeList nList = doc.getElementsByTagName("application");
    	Node nNode = nList.item(0);
    	Element eElement = (Element) nNode;
    	Fpackage = eElement.getAttribute("android:debuggable");
        
        return Fpackage;
    }
	
	/**
	 * Return the list of Android Permission
	 * 
	 * @param _pathToAnalyze Path of decoded APK
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public String getPermission(String _pathToAnalyze) throws ParserConfigurationException, SAXException, IOException {
        
        String path = _pathToAnalyze+"/AndroidManifest.xml";
        ArrayList<String> _list = new ArrayList<String>();
        File fXmlFile = new File(path);
    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    	Document doc = dBuilder.parse(fXmlFile);
    	doc.getDocumentElement().normalize();
    	
    	NodeList nList = doc.getElementsByTagName("uses-permission");
    	for (int temp = 0; temp < nList.getLength(); temp++) {
    		 
    		Node nNode = nList.item(temp);
     
    		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
     
    			Element eElement = (Element) nNode;
    			String _current = eElement.getAttribute("android:name");
    			_list.add(_current);
     
    		}
    	}
    	String _result = _list.toString().substring(1, _list.toString().length() - 1);
        
        return _result;
    }
	
	/**
	 * Return the value of choosed SdkVersion
	 * 
	 * @param _pathToAnalyze
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public String getSdkVersion(String _pathToAnalyze, String _value) throws ParserConfigurationException, SAXException, IOException{
		String path = _pathToAnalyze+"/AndroidManifest.xml";
        ArrayList<String> _list = new ArrayList<String>();
        File fXmlFile = new File(path);
    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    	Document doc = dBuilder.parse(fXmlFile);
    	doc.getDocumentElement().normalize();
    	
    	NodeList nList = doc.getElementsByTagName("uses-sdk");
    	if(nList.getLength() == 0){
    		return "UNDEFINED";
    	}
    	for (int temp = 0; temp < nList.getLength(); temp++) {
    		 
    		Node nNode = nList.item(temp);
     
    		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
     
    			Element eElement = (Element) nNode;
    			String _current = eElement.getAttribute("android:"+_value+"SdkVersion");
    			if(	_current == "" || 
					_current == null || 
					_current == " "){
    				
    				_current = "UNDEFINED";
    			}
    			_list.add(_current);
     
    		}
    	}
    	String _result = _list.toString().substring(1, _list.toString().length() - 1);
        
        return _result;
	}
	
	private void getJsFilesFinder(String _pathToAnalyze, ArrayList<String> _list){
		
		
		File search_file_path = new File(_pathToAnalyze);

        /*--If _pathToAnalyze is a js file, put the name in the list--*/
    	
        if(search_file_path.isFile()){
            
        	if(	search_file_path.getAbsolutePath().contains(".js")){
        		
        		_list.add(search_file_path.getName());
        		
        	}

        }else if(search_file_path.isDirectory()){
            File[] listOfFiles = search_file_path.listFiles();
            int length = listOfFiles.length;
            for (int i = 0; i < length; i++) {
                if (listOfFiles[i].isFile()) {
                    this.getJsFilesFinder(listOfFiles[i].getAbsolutePath(), _list);
                  } else if (listOfFiles[i].isDirectory()) {
                    this.getJsFilesFinder(listOfFiles[i].getAbsolutePath(), _list);
                  }
            }
        }
        
	}
	
	/**
	 * Return a string containing the js files
	 * 
	 * @param _pathToAnalyze
	 * @return
	 */
	public String getJsFiles(String _pathToAnalyze){
		ArrayList<String> _list = new ArrayList<String>();
		this.getJsFilesFinder(_pathToAnalyze, _list);
		String _result = _list.toString().substring(1, _list.toString().length() - 1);
        return _result;
	}
	
	/**
	 * Get number of files
	 * 
	 * @param _pathToAnalyze Path of decoded APK
	 */
	public void getNumber(String _pathToAnalyze, String _extension){
    	
    	File search_file_path = new File(_pathToAnalyze);

        /*--If _pathToAnalyze is a file update the counters, else if is 
         * a directory call this.setWebResources--*/
    	
        if(search_file_path.isFile()){
            
        	if(	search_file_path.getAbsolutePath().contains(_extension)){
        		
        		this._counter = this._counter + 1;
        		
        	}

        }else if(search_file_path.isDirectory()){
            File[] listOfFiles = search_file_path.listFiles();
            int length = listOfFiles.length;
            for (int i = 0; i < length; i++) {
                if (listOfFiles[i].isFile()) {
                    this.getNumber(listOfFiles[i].getAbsolutePath(), _extension);
                  } else if (listOfFiles[i].isDirectory()) {
                    this.getNumber(listOfFiles[i].getAbsolutePath(), _extension);
                  }
            }
        }
    }
	
	/**
	 * Return the number of html files
	 * 
	 * @return
	 */
	public int getHtml(String _pathToAnalyze) {
		
		this._counter = 0;
		this.getNumber(_pathToAnalyze, ".html");
		return this._counter;
	}

	/**
	 * Return the number of javascript files
	 * 
	 * @return
	 */
	public int getJavascript(String _pathToAnalyze) {
		
		this._counter = 0;
		this.getNumber(_pathToAnalyze, ".js");
		return this._counter;
	}

	/**
	 * Return the number of css files
	 * 
	 * @return
	 */
	public int getCSS(String _pathToAnalyze) {
		
		this._counter = 0;
		this.getNumber(_pathToAnalyze, ".css");
		return this._counter;
	}
	
	/**
	 * Return the File Size
	 * 
	 * @param _path Path of File
	 * @return
	 */
	public String getFileSize(String _path) {
		long _size;
		
		File _file = new File(_path);
		_size = _file.length();
		String _s = String.valueOf(_size);
		
		
		return _s;
	}
	
	
	}
