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
package com.apkcategorychecker.json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.apkcategorychecker.analyzer.JsElement;
import com.apkcategorychecker.writer.WriterCSV;

/**
 * This class writes the name of all javascript files in a json file
 *
 * @author Gabriele Martini
 */
public class DefaultJsCSVBuilder implements JsCSVBuilder {
	
	/**
	 * Destination Path
	 */
    private String _destPath;

	@Override
	public void BuildJs(ArrayList<JsElement> jsList, String _destinationPath, String time) {
		
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
            
            File _fileCSV = new File(_destPath+"/Results-JS FILES_"+time+".csv");
            FileWriter _out = new FileWriter(_fileCSV);
            CSVPrinter printer;
            printer = new CSVPrinter(_out, format.withDelimiter('#'));
            System.out.println("Creating " + "Results-JS FILES_"+time+".csv ....");
            try {
                printer.printRecord("appID",
                					"jsFiles");
            } catch (IOException ex) {
                Logger.getLogger(WriterCSV.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            /*--Retrieve APKResult and Write in file--*/
            
            @SuppressWarnings("unused")
			int i=0;
            Iterator<JsElement> it = jsList.iterator();
            while(it.hasNext())
            {
                JsElement _resultElement = it.next();
                List<String> resultData = new ArrayList<>();
                resultData.add(_resultElement.get_Id());
                resultData.add(_resultElement.get_JsFiles());
                i++;
                printer.printRecord(resultData);
            }
            
            /*--Close the printer--*/
            printer.close();
            System.out.println("Results-JS FILES_"+time+".csv created");
            
        } catch (IOException ex) {
            Logger.getLogger(WriterCSV.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}