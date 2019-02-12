/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
@RestController
public class IndexController {

    @RequestMapping(value = "/")
    public String hello() {
        return "Biometric service is running";
    }

}
