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

/**
 * This class is an Interface of a Framework
 *
 * @author Gabriele Martini
 */
public interface Framework {
    
	/**
	 * Method to test if the APK it's been created with this Framework
	 * 
	 * @param _pathToAnalyze Path of decoded APK
	 * @return
	 */
    public boolean Test(String _pathToAnalyze);
    
    /**
     * Get the Framework Name
     * 
     * @return
     */
    public String getFrameworkName();
    
    /**
     * Get the Package of APK
     * 
     * @param _pathToAnalyze Path of decoded APK
     * @return
     */
    public String getPackage(String _pathToAnalyze);
    
    /**
     * Check if a Framework uses Apache Cordova
     * 
     * @return
     */
    public boolean checkCordova();
    
    /**
     * Clean parameters
     */
    public void setoff();
    
}
