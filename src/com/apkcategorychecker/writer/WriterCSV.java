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
package com.apkcategorychecker.writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.apkcategorychecker.analyzer.AnalyzerResult;

/**
 * This class writes the results in a CSV file
 *
 * @author Gabriele Martini
 */
public class WriterCSV implements Writer {

	@Override
    public void Write(AnalyzerResult result, String _csvPath, int _counter) {
        
        try {
        	
        	ArrayList<AnalyzerResult> resultList = new ArrayList<AnalyzerResult>();
        	resultList.add(result);
        	
            /*--Create the CSVFormat object--*/
            
            CSVFormat format = CSVFormat.DEFAULT.withHeader();
            
            /*--Writing in a CSV file--*/
            
            FileWriter _out = new FileWriter(_csvPath, true);
            CSVPrinter printer;
            printer = new CSVPrinter(_out, format.withDelimiter('#'));
            
            /*--Retrieve APKResult and Write in file--*/
            
            Iterator<AnalyzerResult> it = resultList.iterator();
            while(it.hasNext())
            {
                AnalyzerResult _resultElement = it.next();
                List<String> resultData = new ArrayList<>();
                resultData.add(String.valueOf(_counter));
                resultData.add(_resultElement.get_APKName());
                resultData.add(_resultElement.get_APKPath());
                resultData.add(_resultElement.get_Package());
                resultData.add(_resultElement.get_APKMainFramework());
                resultData.add(_resultElement.get_APKBaseFramework());
                resultData.add(String.valueOf(_resultElement.get_html()));
                resultData.add(String.valueOf(_resultElement.get_js()));
                resultData.add(String.valueOf(_resultElement.get_css()));
                resultData.add(_resultElement.get_debuggable());
                resultData.add(_resultElement.get_permissions());
                resultData.add(_resultElement.get_minSdkVersion());
                resultData.add(_resultElement.get_maxSdkVersion());
                resultData.add(_resultElement.get_targetSdkVersion());
                resultData.add(_resultElement.get_fileSize());
                resultData.add(String.valueOf(_resultElement.get_startAnalysis()));
                resultData.add(String.valueOf(_resultElement.get_durationAnalysis()));
                resultData.add(String.valueOf(_resultElement.get_decodeSuccess()));
                printer.printRecord(resultData);
            }
            
            /*--Close the printer--*/
            printer.close();
            System.out.println("Record added to CSV file");
            this.removeBlankLines(_csvPath);
            
        } catch (IOException ex) {
            Logger.getLogger(WriterCSV.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

	private void removeBlankLines(String _csvPath) {
		try {
			FileReader fr = new FileReader(_csvPath); 
			BufferedReader br = new BufferedReader(fr); 
			FileWriter fw = new FileWriter(_csvPath + ".temp"); 
			String line;
	
			while((line = br.readLine()) != null)
			{ 
			    //line = line.trim(); // remove leading and trailing whitespace
			    if (line.length() != 0) // don't write out blank lines
			    {
			        fw.write(line, 0, line.length());
			    	fw.append("\r");
			    }
			} 
			fr.close();
			fw.close();
			File old = new File(_csvPath);
			old.delete();
			File clean = new File(_csvPath + ".temp");
			clean.renameTo(old);
		} catch(FileNotFoundException e){
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
		
	

	@Override
	public void createHeader(String _csvPath) throws IOException {
		/*--Create the CSVFormat object--*/
        
        CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(',');
        
        /*--Writing in a CSV file--*/
        
        File _fileCSV = new File(_csvPath);
        FileWriter _out;
		_out = new FileWriter(_fileCSV);
        CSVPrinter printer;
		printer = new CSVPrinter(_out, format.withDelimiter('#'));
        System.out.println("Creating the CSV file....");
        try {
            printer.printRecord("App_ID",
            					"APK_File_Name",
            					"APK_File_Path",
            					"APK_Package",
            					"Main_Framework",
            					"Base_Framework",
            					"HTML",
            					"JS",
            					"CSS",
            					"Android_Debuggable",
            					"Android_Permissions",
            					"Android_MinSdkVersion",
            					"Android_MaxSdkVersion",
            					"Android_TargetSdkVersion",
            					"File_Size(Bytes)",
            					"Start_Analysis_Time(milliseconds)",
            					"Duration_Analysis_Time(milliseconds)",
            					"Decode_Success");
        } catch (IOException ex) {
            Logger.getLogger(WriterCSV.class.getName()).log(Level.SEVERE, null, ex);
        }
        printer.close();
		
	}
    
}
