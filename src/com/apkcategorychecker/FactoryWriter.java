/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

/**
 *
 * @author gabriele
 */
public class FactoryWriter {
    
    private FactoryWriter() {
    }
    
    public static FactoryWriter getInstance() {
        return FactoryWriterHolder.INSTANCE;
    }
    
    private static class FactoryWriterHolder {

        private static final FactoryWriter INSTANCE = new FactoryWriter();
    }
    
    public Writer getWriter(String cliParameter) {
        Writer writer = null;
        if("csv".equals(cliParameter)){
            writer = new WriterCSV();
        }
        
        return writer;
    }
}
