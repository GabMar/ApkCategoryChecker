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
 * This factory choose what kind of writer will be used based on the
 * parameter passed from the Command-Line
 *
 * @author Gabriele Martini
 */
public class FactoryWriter {
    
	/*Constructor*/
    private FactoryWriter() {
    }
    
    /**
     * Applying the Singleton Design-Pattern
     * 
     * @return
     */
    public static FactoryWriter getInstance() {
        return FactoryWriterHolder.INSTANCE;
    }
    
    private static class FactoryWriterHolder {

        private static final FactoryWriter INSTANCE = new FactoryWriter();
    }
    
    /**
     * This method choose the writer
     * 
     * @param cliParameter Parameter given from the Command-Line
     * @return
     */
    public Writer getWriter(String cliParameter) {
        Writer writer = null;
        if("csv".equals(cliParameter)){
            writer = new WriterCSV();
        }
        
        return writer;
    }
}
