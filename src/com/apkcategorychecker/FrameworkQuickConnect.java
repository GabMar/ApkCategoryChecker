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
package com.apkcategorychecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author gabriele
 */
public class FrameworkQuickConnect implements Framework{

    private final String FrameworkName = "QuickConnect";
    private boolean isCordova = false;
    private boolean QuickConnect = false;
    private boolean founded = false;

    @Override
    public boolean Test(String _pathToAnalyze) {
        File isdirqc = new File(_pathToAnalyze + "/assets/QCJSLib");
        this.QuickConnect = isdirqc.exists();
        if(!this.QuickConnect){
            this.QuickConnect = this.searchString(_pathToAnalyze, "qc.handleError");
        }
        return this.QuickConnect;
    }

    @Override
    public String getFrameworkName() {
        return this.FrameworkName;
    }

    @Override
    public String getPackage(String _pathToAnalyze) {
        
        String target = "package";
        String path = _pathToAnalyze+"/AndroidManifest.xml";
        String Fpackage = null;
        try {
            File file = new File(path);
            BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                new FileInputStream(file)));
            String line;
            while((line = br.readLine()) != null) {
                if(line.indexOf(target) != -1)
                    break;
            }
            String[] s = line.split("package=");
            Fpackage = s[1].substring(1, s[1].length() -1);
            br.close();
        } catch(IOException e) {
            System.out.println("read error: " + e.getMessage());
        }
        
        
        return Fpackage;
    }

    @Override
    public boolean checkCordova() {
        return this.isCordova;
    }
    
    @Override
    public void setoff(){
        this.QuickConnect = false;
        this.founded = false;
    };
    
    private boolean searchString(String _pathSearch, String _word){
        
        if(this.founded == false){
            File search_file_path = new File(_pathSearch);

            //If File
            if(search_file_path.isFile()){
                FileToString readStringifyedFile;
                readStringifyedFile = new FileToString();
                String findWord = readStringifyedFile.readFile(_pathSearch);
                this.founded = findWord.contains(_word);

            }else if(search_file_path.isDirectory()){
                File[] listOfFiles = search_file_path.listFiles();
                int length = listOfFiles.length;
                for (int i = 0; i < length; i++) {
                    if (listOfFiles[i].isFile()) {
                        this.searchString(listOfFiles[i].getAbsolutePath(), _word);
                      } else if (listOfFiles[i].isDirectory()) {
                        this.searchString(listOfFiles[i].getAbsolutePath(), _word);
                      }
                }
            }
        }
        return this.founded;
    }
    
}
