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
