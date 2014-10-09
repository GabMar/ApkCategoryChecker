/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

import java.util.ArrayList;

/**
 *
 * @author Gabriele Martini
 */
public class FrameworkPool {
    
    private static FrameworkPool instance = null;
    private final ArrayList<Framework> listOfFramework;
    
    private FrameworkPool() {
        this.listOfFramework = new ArrayList<Framework>();
        
        //Add Corvova Framework
        
        Framework Cordova;
        Cordova = new FrameworkCordova();
        this.listOfFramework.add(Cordova);
        
        //Add Enyo Framework
        
        Framework Enyo;
        Enyo = new FrameworkEnyo();
        this.listOfFramework.add(Enyo);
        
        //Add IUI Framework
        
        Framework IUI;
        IUI = new FrameworkIUI();
        this.listOfFramework.add(IUI);
        
        //Add Mobl Framework
        
        Framework Mobl;
        Mobl = new FrameworkMobl();
        this.listOfFramework.add(Mobl);
        
        //Add Next Framework
        
        Framework Next;
        Next = new FrameworkNext();
        this.listOfFramework.add(Next);
        
        //Add Sencha Framework
        
        Framework Sencha;
        Sencha = new FrameworkSencha();
        this.listOfFramework.add(Sencha);
        
        //Add Canappi Framework
        
        Framework Canappi;
        Canappi = new FrameworkCanappi();
        this.listOfFramework.add(Canappi);
        
        //Add Kivy Framework
        
        Framework Kivy;
        Kivy = new FrameworkKivy();
        this.listOfFramework.add(Kivy);
        
        //Add MoSync Framework
        
        Framework MoSync;
        MoSync = new FrameworkMoSync();
        this.listOfFramework.add(MoSync);
        
        //Add QuickConnect Framework
        
        Framework QuickConnect;
        QuickConnect = new FrameworkQuickConnect();
        this.listOfFramework.add(QuickConnect);
        
        //Add RhoMobile Framework
        
        Framework RhoMobile;
        RhoMobile = new FrameworkRhoMobile();
        this.listOfFramework.add(RhoMobile);
        
        //Add Titanium Framework
        
        Framework Titanium;
        Titanium = new FrameworkTitanium();
        this.listOfFramework.add(Titanium);
        
    }
    
    public static FrameworkPool getInstance() {
    	if(instance == null) {
            instance = new FrameworkPool();
         }
         return instance;
    }
    
    public ArrayList<Framework> getFramework(){
        return this.listOfFramework;
    }
}
