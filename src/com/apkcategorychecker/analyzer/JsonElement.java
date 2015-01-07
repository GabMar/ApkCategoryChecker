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
package com.apkcategorychecker.analyzer;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

/**
 * This class creates an object that contains the app id and the list of javascript files
 *
 * @author Gabriele Martini
 */
public class JsonElement implements JSONAware {
	
	/**
	 * APK Id
	 */
	private String _AppId;
	
	/**
	 * List of javascript files
	 */
	private String _JsFiles;
	
	/**
	 * Return the APK Id
	 * 
	 * @return 
	 */
	public String get_Id(){
		return this._AppId;
	}
	
	/**
	 * Return the Javascript list of files
	 * 
	 * @return 
	 */
	public String get_JsFiles(){
		return this._JsFiles;
	}
	
	/**
	 * Set the App Id
	 * 
	 * @param id
	 */
	public void set_id(String id){
		this._AppId = id;
	}
	
	/**
	 * Set the Javascript list of files
	 * 
	 * @param list
	 */
	public void set_JsFiles(String list){
		this._JsFiles = list;
	}

	@Override
	public String toJSONString() {
		StringBuffer sb = new StringBuffer();
        
        sb.append("{");
        
        sb.append(JSONObject.escape("appID"));
        sb.append(":");
        sb.append("\"" + JSONObject.escape(this._AppId) + "\"");
        
        sb.append(",");
        
        sb.append(JSONObject.escape("jsFiles"));
        sb.append(":[");
        sb.append(this._JsFiles);
        sb.append("]");
        
        sb.append("}");
        
        return sb.toString();
	}

}
