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

import brut.androlib.AndrolibException;
import brut.androlib.ApkDecoder;
import brut.directory.DirectoryException;


/**
 * This class decode an APK in a directory with the name of APK
 *
 * @author Gabriele Martini
 */
public class ToolDecoder {
    
	/**
	 * Instance of ApkDecoder
	 */
    private final ApkDecoder decoder = new ApkDecoder();
    
    /**
     * Name of choosed APK
     */
    private String ApkChoosedName;
    
    /**
     * Path of output directory
     */
    private File outDir;
    
    /**
     * Method to decode an APK file using Apktool
     * 
     * @param _apkPath Path of APK file
     * @return
     * @throws AndrolibException
     */
    public String DecodeApk(String _apkPath, String _outDecoded) throws AndrolibException{
        
        try {
        		/*--Create new file from given path--*/
                
        		File ApkChoosed = new File(_apkPath);
                
                /*--Set the APK file--*/
        		
                decoder.setApkFile(ApkChoosed);
                
                /*--Retrieve the name of the APK to give the name to the output directory--*/
                
                int ApkChoosedNameLength = ApkChoosed.getName().length();
                ApkChoosedName = ApkChoosed.getName();
                ApkChoosedName = ApkChoosedName.substring(0, ApkChoosedNameLength - 4);
                
                /*--Set the output directory--*/
                
                //outDir = new File(_apkPath.substring(0, _apkPath.length() - 4));
                if(_outDecoded == null){
                	this.outDir = new File(ApkChoosedName);
                }else{
                	this.outDir = new File(_outDecoded+ "/" + ApkChoosedName);
                }
                
                try {
                        decoder.setOutDir(outDir);
                        
                        decoder.setDecodeSources((short) 0);
                        
                        
                        
                } catch (AndrolibException e) {
                }
                
                /*--Decode the APK with Apktool--*/
                
                        try {
							decoder.decode();
						} catch (DirectoryException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

        } catch (IOException e) {
        }
        
        
        return this.outDir.getPath();
    }
    
}
