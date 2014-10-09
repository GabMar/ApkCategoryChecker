/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

/**
 *
 * @author Gabriele Martini
 */
public interface Framework {
    //package,name,path,framework
    public boolean Test(String _pathToAnalyze);
    public String getFrameworkName();
    public String getPackage(String _pathToAnalyze);
    public boolean checkCordova();
    public void setoff();
    
}
