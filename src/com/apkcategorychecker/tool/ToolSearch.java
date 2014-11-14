package com.apkcategorychecker.tool;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToolSearch {
	
	/**
	 * Boolean used by the method searchStringInFileText
	 */
	private boolean _boolSearchStringInFileText = false;
	
	/**
	 * Boolean used by the method searchStringInFileTextExt
	 */
	private boolean _boolSearchStringInFileTextExt = false;
	
	/**
	 * Boolean used by the method searchRegExInFileText
	 */
	private boolean _boolSearchRegExInFileText = false;
	
	/**
	 * Boolean used by the method searchRegExInFileTextExt
	 */
	private boolean _boolSearchRegExInFileTextExt = false;
	
	/**
	 * Boolean used by the method searchFile
	 */
	private boolean _boolSearchFile = false;
	
	
	/**
	 * Search a string in a file
	 * 
	 * @param _path File path
	 * @param _word Word to search for
	 * @return
	 */
	public boolean searchStringInFileText(String _path, String _word){
		if(!this._boolSearchStringInFileText){
            File search_file_path = new File(_path);

            //If File
            if(search_file_path.isFile()){
                ToolFileToString readStringifyedFile;
                readStringifyedFile = new ToolFileToString();
                String findWord = readStringifyedFile.readFile(_path);
                this._boolSearchStringInFileText = findWord.contains(_word);

            }else if(search_file_path.isDirectory()){
                File[] listOfFiles = search_file_path.listFiles();
                int length = listOfFiles.length;
                for (int i = 0; i < length; i++) {
                    if (listOfFiles[i].isFile()) {
                        this.searchStringInFileText(listOfFiles[i].getAbsolutePath(), _word);
                      } else if (listOfFiles[i].isDirectory()) {
                        this.searchStringInFileText(listOfFiles[i].getAbsolutePath(), _word);
                      }
                }
            }
		}
        return this._boolSearchStringInFileText;
	}
	
	/**
	 * Search a string in a file with given extension
	 * 
	 * @param _path File path
	 * @param _word Word to search for
	 * @param _ext Extension of the file
	 * @return
	 */
	public boolean searchStringInFileTextExt(String _path, String _word, String _ext){
		if(!this._boolSearchStringInFileTextExt){
            File search_file_path = new File(_path);

            //If File
            if(search_file_path.isFile() && _path.contains(_ext)){
                ToolFileToString readStringifyedFile;
                readStringifyedFile = new ToolFileToString();
                String findWord = readStringifyedFile.readFile(_path);
                this._boolSearchStringInFileText = findWord.contains(_word);

            }else if(search_file_path.isDirectory()){
                File[] listOfFiles = search_file_path.listFiles();
                int length = listOfFiles.length;
                for (int i = 0; i < length; i++) {
                    if (listOfFiles[i].isFile()) {
                        this.searchStringInFileText(listOfFiles[i].getAbsolutePath(), _word);
                      } else if (listOfFiles[i].isDirectory()) {
                        this.searchStringInFileText(listOfFiles[i].getAbsolutePath(), _word);
                      }
                }
            }
		}
        return this._boolSearchStringInFileText;
	}
	
	/**
	 * Search a string in a file by regular expression
	 * 
	 * @param _path File path
	 * @param _regEx Regular expression
	 * @return
	 */
	public boolean searchRegExInFileText(String _path, String _regEx){
		if(!this._boolSearchRegExInFileText){
            File search_file_path = new File(_path);

            //If File
            if(search_file_path.isFile()){
                ToolFileToString readStringifyedFile;
                readStringifyedFile = new ToolFileToString();
                String findWord = readStringifyedFile.readFile(_path);
                Pattern _regExPattern = Pattern.compile(_regEx);
                Matcher m = _regExPattern.matcher(findWord);
                this._boolSearchRegExInFileText = m.find();

            }else if(search_file_path.isDirectory()){
                File[] listOfFiles = search_file_path.listFiles();
                int length = listOfFiles.length;
                for (int i = 0; i < length; i++) {
                    if (listOfFiles[i].isFile()) {
                        this.searchRegExInFileText(listOfFiles[i].getAbsolutePath(), _regEx);
                      } else if (listOfFiles[i].isDirectory()) {
                        this.searchRegExInFileText(listOfFiles[i].getAbsolutePath(), _regEx);
                      }
                }
            }
		}
        return this._boolSearchRegExInFileText;
	}
	
	/**
	 * Search a string in a file with given extension by regular expression
	 * 
	 * @param _path File path
	 * @param _regEx Regular expression
	 * @param _ext Extension of the file
	 * @return
	 */
	public boolean searchRegExInFileTextExt(String _path, String _regEx, String _ext){
		if(!this._boolSearchRegExInFileTextExt){
            File search_file_path = new File(_path);

            //If File
            if(search_file_path.isFile() && _path.contains(_ext)){
                ToolFileToString readStringifyedFile;
                readStringifyedFile = new ToolFileToString();
                String findWord = readStringifyedFile.readFile(_path);
                Pattern _regExPattern = Pattern.compile(_regEx);
                Matcher m = _regExPattern.matcher(findWord);
                this._boolSearchRegExInFileTextExt = m.find();

            }else if(search_file_path.isDirectory()){
                File[] listOfFiles = search_file_path.listFiles();
                int length = listOfFiles.length;
                for (int i = 0; i < length; i++) {
                    if (listOfFiles[i].isFile()) {
                        this.searchRegExInFileTextExt(listOfFiles[i].getAbsolutePath(), _regEx, _ext);
                      } else if (listOfFiles[i].isDirectory()) {
                        this.searchRegExInFileTextExt(listOfFiles[i].getAbsolutePath(), _regEx, _ext);
                      }
                }
            }
		}
        return this._boolSearchRegExInFileTextExt;
	}
	
	/**
	 * Search a specific file by the name
	 * 
	 * @param _path File path
	 * @param _filename File name to search for
	 * @return
	 */
	public boolean searchFile(String _path, String _filename){
		if(!this._boolSearchFile){
			
			File search_file_path = new File(_path);
			
			if(search_file_path.isFile() && search_file_path.getName().contentEquals(_filename)){
                this._boolSearchFile = true;

            }else if(search_file_path.isDirectory()){
                File[] listOfFiles = search_file_path.listFiles();
                int length = listOfFiles.length;
                for (int i = 0; i < length; i++) {
                    if (listOfFiles[i].isFile()) {
                        this.searchFile(listOfFiles[i].getAbsolutePath(), _filename);
                      } else if (listOfFiles[i].isDirectory()) {
                        this.searchFile(listOfFiles[i].getAbsolutePath(), _filename);
                      }
                }
            }
		}
		return this._boolSearchFile;
	}

}
