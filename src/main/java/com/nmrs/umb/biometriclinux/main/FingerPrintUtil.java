/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.main;

import com.nmrs.umb.biometriclinux.models.AppModel;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.FingerPrintMatchInputModel;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
public interface FingerPrintUtil {
    
    public FingerPrintInfo capture(int fingerPosition, String err, boolean populateImagebytes);
    public int verify(FingerPrintMatchInputModel input);
    public int oldVerify(FingerPrintMatchInputModel input);
    
}
