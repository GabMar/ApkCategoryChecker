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

import com.apkcategorychecker.cli.CommandLineInterface;
import com.apkcategorychecker.tool.ToolApkParameters;

/**
 * UndefinedWebApp Framework
 *
 * @author Gabriele Martini
 */
public class FrameworkUndefinedWebApp implements Framework{

	/**
	 * Name of Framework
	 */
    private final String FrameworkName = "Undefined_Web_App";
    
    /**
     * Boolean to check if this Framework uses Apache Cordova
     */
    private boolean isCordova = false;
    
    /**
     * Level of deepness
     */
    int _deep = CommandLineInterface.getInstance().getDeep();

    @Override
    public boolean Test(String _pathToAnalyze) {
        return this.compareParameters(_pathToAnalyze);
    }

    @Override
    public String getFrameworkName() {
        return this.FrameworkName;
    }

    @Override
    public boolean checkCordova() {
        return this.isCordova;
    }
    
    /**
     * Method to search Web Resource Files
     * 
     * @param _pathSearch Path of file
     * @return
     */
    private boolean compareParameters(String _pathSearch){
    	
    	int _html = ToolApkParameters.getInstance().getHtml(_pathSearch);
    	int _javascript = ToolApkParameters.getInstance().getJavascript(_pathSearch);
    	int _css = ToolApkParameters.getInstance().getCSS(_pathSearch);
    	int _res = _html + _javascript + _css;
        
        boolean _compare = false;
        
        if(_res >= this._deep && this._deep != 0){
        	_compare = true;
        }
        return _compare;
    }
}
