/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.main;

import com.nmrs.umb.biometriclinux.models.DbModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 *
 * @author MORRISON.I
 */
@Component
public class AppUtil {
    public static final int QUALITY_THRESHOLD = 60;
    public static final String LOW_QUALITY_FLAG = "low";
    public static final String VALID_QUALITY_FLAG = "normal";
    public static final String INVALID_FINGER_PRINTS = "invalid";
}
