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
package com.apkcategorychecker.framework;

import com.apkcategorychecker.tool.ToolSearch;


/**
 * IBM Worklight Framework
 *
 * @author Gabriele Martini
 */
public class FrameworkIBMWorklight implements Framework {

	/**
	 * Name of Framework
	 */
    private final String FrameworkName = "IBM Worklight";
    
    /**
     * Boolean to check if this Framework uses Apache Cordova
     */
    private final boolean isCordova = true;

    @Override
    public boolean Test(String _pathToAnalyze) {
    	boolean _boolString, _boolFile, _Worklight = false;
    	ToolSearch Searcher = new ToolSearch();
    	_boolString = Searcher.searchStringInFileText(_pathToAnalyze+"/res/xml/config.xml", "com.worklight.androidgap");
    	_boolFile = Searcher.searchFile(_pathToAnalyze, "WLDroidGap.class");
    	if(_boolString && _boolFile){
    		_Worklight = true;
    	}
        return _Worklight;
    }

    @Override
    public String getFrameworkName() {
        return this.FrameworkName;
    }

    @Override
    public boolean checkCordova() {
        return this.isCordova;
    }
}
