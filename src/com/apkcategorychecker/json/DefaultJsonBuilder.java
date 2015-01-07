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

import org.json.simple.JSONArray;

import com.apkcategorychecker.analyzer.JsonElement;

/**
 * This class writes the name of all javascript files in a json file
 *
 * @author Gabriele Martini
 */
public class DefaultJsonBuilder implements JsonBuilder {
	
	/**
	 * Destination Path
	 */
    private String _destPath;

	@SuppressWarnings("unchecked")
	@Override
	public void BuildJson(ArrayList<JsonElement> jsonList, String _destinationPath, String time) {
		
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
         
         /*Create the json object*/
         JSONArray obj = new JSONArray();
         

         for(int i=0; i<jsonList.size(); i++){
        	 obj.add(jsonList.get(i));
         }
         
         FileWriter file = null;
		try {
			file = new FileWriter(_destPath+"/Results_"+time+".json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
         try {
			file.write(obj.toJSONString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         try {
			file.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         try {
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}

	}

}
