package com.apkcategorychecker.tool;

import java.io.File;
import java.io.IOException;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.v3.Dex2jar;

public class ToolDex2Jar {
	
	/**
	 * Method to convert the .dex file in a .jar file
	 * 
	 * @param _dexPath Path of .dex file
	 * @param _apkDecodedPath Path of decoded APK
	 */
	public void ConvertDex2Jar (String _dexPath, String _apkDecodedPath){
		
        /*--Instance of new file--*/
        File _dexClass = new File(_dexPath);
        
        try {
        	
        	/*--Instance of a new Dex2Jar reader--*/
			DexFileReader reader = new DexFileReader(_dexClass);
			
			/*--Make the directory "classes" inside the directory of decoded APK--*/
			File _classesPath = new File(_apkDecodedPath + "/classes");
			_classesPath.mkdir();
			
			/*--Output file--*/
			File file = new File(_apkDecodedPath + "/classes/ApkDecodeddex2jar.jar");
			
			/*--Conversion .dex to .jar--*/
			Dex2jar.from(reader).to(file);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
