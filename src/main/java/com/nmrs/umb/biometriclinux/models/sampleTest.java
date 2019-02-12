/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.models;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
public class sampleTest {
    
    public static void main(String args[]){
//    String name = "Morrison";
//    String anad = "some time";
//        String ss = String.format("Tell us %s and more stuff %s", name,anad);
//        System.out.println(ss);

        AppModel.FingerPositions gg = AppModel.FingerPositions.values()[0];
        System.out.println(gg.name());
        
    }
    
}
