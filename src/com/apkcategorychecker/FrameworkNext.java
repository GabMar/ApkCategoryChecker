/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
public class FrameworkNext implements Framework{

    private final String FrameworkName = "Next";
    private final boolean isCordova = true;
    private boolean Next = false;
    private boolean founded = false;

    @Override
    public boolean Test(String _pathToAnalyze) {
        this.Next = this.searchString(_pathToAnalyze + "/assets/www/", "nextwebapp");
        return this.Next;
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
        this.Next = false;
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
