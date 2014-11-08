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

import java.util.ArrayList;

/**
 * This class return a list of available Frameworks
 *
 * @author Gabriele Martini
 */
public class FrameworkPool {
    
	/*--Instance for Singleton Design-Pattern--*/
    private static FrameworkPool instance = null;
    
    /*--List of Frameworks--*/
    private final ArrayList<Framework> listOfFramework;
    
    /*--Constructor--*/
    private FrameworkPool() {
    	
    	/*Creates a new ArrayList*/
        this.listOfFramework = new ArrayList<Framework>();
        
        /*Add UndefinedWebApp Framework*/
        
        Framework UndefinedWebApp;
        UndefinedWebApp = new FrameworkUndefinedWebApp();
        this.listOfFramework.add(UndefinedWebApp);
        
        /*Add Corvova Framework*/
        
        Framework Cordova;
        Cordova = new FrameworkCordova();
        this.listOfFramework.add(Cordova);
        
        /*Add PhoneGap Framework*/
        
        Framework Phonegap;
        Phonegap = new FrameworkPhonegap();
        this.listOfFramework.add(Phonegap);
        
        /*Add Enyo Framework*/
        
        Framework Enyo;
        Enyo = new FrameworkEnyo();
        this.listOfFramework.add(Enyo);
        
        /*Add IBM Worklight Framework*/
        
        Framework Worklight;
        Worklight = new FrameworkIBMWorklight();
        this.listOfFramework.add(Worklight);
        
        /*Add IUI Framework*/
        
        Framework IUI;
        IUI = new FrameworkIUI();
        this.listOfFramework.add(IUI);
        
        /*Add Mobl Framework*/
        
        Framework Mobl;
        Mobl = new FrameworkMobl();
        this.listOfFramework.add(Mobl);
        
        /*Add Next Framework*/
        
        Framework Next;
        Next = new FrameworkNext();
        this.listOfFramework.add(Next);
        
        /*Add Sencha Framework*/
        
        Framework Sencha;
        Sencha = new FrameworkSencha();
        this.listOfFramework.add(Sencha);
        /*Add Kivy Framework*/
        
        Framework Kivy;
        Kivy = new FrameworkKivy();
        this.listOfFramework.add(Kivy);
        
        /*Add MoSync Framework*/
        
        Framework MoSync;
        MoSync = new FrameworkMoSync();
        this.listOfFramework.add(MoSync);
        
        /*Add QuickConnect Framework*/
        
        Framework QuickConnect;
        QuickConnect = new FrameworkQuickConnect();
        this.listOfFramework.add(QuickConnect);
        
        /*Add RhoMobile Framework*/
        
        Framework RhoMobile;
        RhoMobile = new FrameworkRhoMobile();
        this.listOfFramework.add(RhoMobile);
        
        /*Add Titanium Framework*/
        
        Framework Titanium;
        Titanium = new FrameworkTitanium();
        this.listOfFramework.add(Titanium);
        
        
    }
    
    /**
     * Applying Singleton Design-Pattern
     * 
     * @return Instance
     */
    public static FrameworkPool getInstance() {
    	if(instance == null) {
            instance = new FrameworkPool();
         }
         return instance;
    }
    
    /**
     * Get the list of Frameworks
     * 
     * @return List of Frameworks
     */
    public ArrayList<Framework> getFramework(){
        return this.listOfFramework;
    }
}
