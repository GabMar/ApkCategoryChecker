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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    
	/**
	 * Destination Path
	 */
    private String _destPath;

    @Override
    public void Write(ArrayList<AnalyzerResult> resultList, String _destinationPath) {
        
        try {

            /*Check if _destinationPath is an APK or a Directory in order to
            *get the right destination path
            */
            
            File Destination = new File(_destinationPath);
            if(!Destination.exists()){
            	Destination.mkdir();
            }
            if(Destination.isDirectory()){
                this._destPath = _destinationPath;
            }else if(Destination.isFile()){
                this._destPath = _destinationPath.substring(0, _destinationPath.length() - 4);
            }
            
            /*--Create the CSVFormat object--*/
            
            CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(',');
            
            /*--Writing in a CSV file--*/
            
            DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss");
            Date date = new Date();
            File _fileCSV = new File(_destPath+"/Results_"+dateFormat.format(date)+".csv");
            FileWriter _out = new FileWriter(_fileCSV);
            CSVPrinter printer;
            printer = new CSVPrinter(_out, format.withDelimiter('#'));
            System.out.println("********");
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
                					"File_Size(Bytes)",
                					"Start_Analysis_Time(milliseconds)",
                					"Duration_Analysis_Time(milliseconds)");
            } catch (IOException ex) {
                Logger.getLogger(WriterCSV.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            /*--Retrieve APKResult and Write in file--*/
            
            int i=0;
            Iterator<AnalyzerResult> it = resultList.iterator();
            while(it.hasNext())
            {
                AnalyzerResult _resultElement = it.next();
                List<String> resultData = new ArrayList<>();
                resultData.add(String.valueOf(i));
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
                resultData.add(_resultElement.get_fileSize());
                resultData.add(String.valueOf(_resultElement.get_startAnalysis()));
                resultData.add(String.valueOf(_resultElement.get_durationAnalysis()));
                i++;
                printer.printRecord(resultData);
            }
            
            /*--Close the printer--*/
            printer.close();
            
        } catch (IOException ex) {
            Logger.getLogger(WriterCSV.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
