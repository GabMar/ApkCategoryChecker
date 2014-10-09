/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apkcategorychecker;

import brut.androlib.AndrolibException;
import brut.androlib.ApkDecoder;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Gabriele Martini
 */
public class ToolDecoder {
    
    private final ApkDecoder decoder = new ApkDecoder();
    private String ApkChoosedName;
    private String ApkPath;
    private File outDir;
    
    public String DecodeApk(String _apkPath) throws AndrolibException{
        
        try {
                File ApkChoosed = new File(_apkPath);
                decoder.setApkFile(ApkChoosed);
                int ApkChoosedNameLength = ApkChoosed.getName().length();
                ApkChoosedName = ApkChoosed.getName();
                ApkChoosedName = ApkChoosedName.substring(0, ApkChoosedNameLength - 4);
                outDir = new File(_apkPath.substring(0, _apkPath.length() - 4));
                ApkPath = outDir.getAbsolutePath();
                try {
                        decoder.setOutDir(outDir);
                } catch (AndrolibException e) {
                }
                try {
                        decoder.decode();
                } catch (AndrolibException e) {
                }
        } catch (IOException e) {
        }
        
        return _apkPath.substring(0, _apkPath.length() - 4);
    }
    
}
