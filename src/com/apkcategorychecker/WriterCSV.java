/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

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

/**
 *
 * @author gabriele
 */
public class WriterCSV implements Writer {
    
    private String _destPath;

    @Override
    public void Write(ArrayList resultList, String _destinationPath) {
        
        try {

            //Check if _destinationPath is an APK or a Directory in order to
            //get the right destination path
            
            File Destination = new File(_destinationPath);
            if(Destination.isDirectory()){
                this._destPath = _destinationPath;
            }else if(Destination.isFile()){
                this._destPath = _destinationPath.substring(0, _destinationPath.length() - 4);
            }
            
            //Create the CSVFormat object
            
            CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(',');
            
            //CSV Write Example using CSVPrinter
            DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss");
            Date date = new Date();
            File _fileCSV = new File(_destPath+"/Results_"+dateFormat.format(date)+".csv");
            FileWriter _out = new FileWriter(_fileCSV);
            CSVPrinter printer;
            printer = new CSVPrinter(_out, format.withDelimiter('#'));
            System.out.println("********");
            try {
                printer.printRecord("ID","APK Name","APK Path","APK Package","Framework");
            } catch (IOException ex) {
                Logger.getLogger(WriterCSV.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //Retrieve APKResult and Write in file
            
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
                resultData.add(_resultElement.get_APKFramework());
                i++;
                printer.printRecord(resultData);
            }
            
            //close the printer
            printer.close();
            
        } catch (IOException ex) {
            Logger.getLogger(WriterCSV.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
