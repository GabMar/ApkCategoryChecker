package com.apkcategorychecker.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.jar.*;
import java.util.Enumeration;

public class ToolJar2Class {

	/**
	 * Extract the content of a jar file
	 * 
	 * @param _jarPath Path of jar file
	 * @param _apkDecodedPath Path of decoded APK
	 */
	public void ConvertJar2Class(String _jarPath, String _apkDecodedPath) {
		
		/*--Initialize the jar file and the destination path directory--*/
		File jarFile = new File(_jarPath);
		String destDir = _apkDecodedPath + "/classes";
		
		/*--Instance of a JarFile--*/
		JarFile jar = null;
		try {
			jar = new java.util.jar.JarFile(jarFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Enumeration<JarEntry> en = jar.entries();
		while (en.hasMoreElements())
			try {
				{
					java.util.jar.JarEntry file = (java.util.jar.JarEntry) en.nextElement();
					java.io.File f = new java.io.File(destDir + java.io.File.separator + file.getName());
					if (file.isDirectory()) { // if its a directory, create it
						f.mkdir();
						continue;
					}
					java.io.InputStream is = null;
					try {
						is = jar.getInputStream(file);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} // get the input stream
					java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
					while (is.available() > 0) {  // write contents of 'is' to 'fos'
						fos.write(is.read());
					}
					fos.close();
					is.close();
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}

}
