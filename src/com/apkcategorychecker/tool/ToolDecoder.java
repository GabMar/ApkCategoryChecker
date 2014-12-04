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
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import brut.androlib.AndrolibException;
import brut.androlib.ApkDecoder;
import brut.androlib.err.CantFindFrameworkResException;
import brut.androlib.err.InFileNotFoundException;
import brut.androlib.err.OutDirExistsException;
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
     * Verbosity
     *
     */
    private static enum Verbosity {
        NORMAL, VERBOSE, QUIET;
      }
    
    /**
     * Method to decode an APK file using Apktool
     * 
     * @param _apkPath Path of APK file
     * @return
     * @throws AndrolibException
     */
    public String DecodeApk(String _apkPath, String _outDecoded) throws AndrolibException{
    	
    	Verbosity verbosity = Verbosity.NORMAL;
    	
    	setupLogging(verbosity);
        
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
		
		    decoder.setOutDir(outDir);
		    
		    decoder.setDecodeSources((short) 0);
		    
		    decoder.setKeepBrokenResources(true);
		    
		    decoder.setDebugMode(true);
		    
		    decoder.setBaksmaliDebugMode(false);
		        
		/*--Decode the APK with Apktool--*/
		
		try {
			decoder.decode();
		} catch (OutDirExistsException ex) {
		    System.err.println("Destination directory (" + outDir.getAbsolutePath() + ") " + "already exists.");

		    System.exit(1);
		  } catch (InFileNotFoundException ex) {
		    System.err.println("Input file was not found or was not readable.");
		    System.exit(1);
		  } catch (CantFindFrameworkResException ex) {
		    System.err.println("Can't find framework resources for package of id: " + String.valueOf(ex.getPkgId()) + ". You must install proper " + "framework files, see project website for more info.");

		    System.exit(1);
		  } catch (IOException ex) {
		    System.err.println("Could not modify file. Please ensure you have permission.");
		    System.exit(1);
		  } catch (DirectoryException ex) {
		    System.err.println("Could not modify internal dex files. Please ensure you have permission.");
		    System.exit(1);
		  }
        
        
        return this.outDir.getPath();
    }
    
    /**
     * Method to set log verbosity
     * 
     * @param verbosity
     */
    private static void setupLogging(Verbosity verbosity)
    {
      Logger logger = Logger.getLogger("");
      for (Handler handler : logger.getHandlers()) {
        logger.removeHandler(handler);
      }
      LogManager.getLogManager().reset();

      if (verbosity == Verbosity.QUIET) {
        return;
      }

      Handler handler = new Handler()
      {
        public void publish(LogRecord record) {
          if (getFormatter() == null) {
            setFormatter(new SimpleFormatter());
          }
          try
          {
            String message = getFormatter().format(record);
            if (record.getLevel().intValue() >= Level.WARNING.intValue())
              System.err.write(message.getBytes());
            else
              System.out.write(message.getBytes());
          }
          catch (Exception exception) {
            reportError(null, exception, 5);
          }
        }

        public void close()
          throws SecurityException
        {
        }

        public void flush()
        {
        }
      };
      logger.addHandler(handler);

      if (verbosity == Verbosity.VERBOSE) {
        handler.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);
      } else {
        handler.setFormatter(new Formatter()
        {
          public String format(LogRecord record) {
            return record.getLevel().toString().charAt(0) + ": " + record.getMessage() + System.getProperty("line.separator");
          }
        });
      }
    }
    
}


