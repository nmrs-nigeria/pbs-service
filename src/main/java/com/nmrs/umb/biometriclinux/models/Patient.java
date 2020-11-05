/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Patient {

    public String name;
    public String id;
    public String identifier;
    public String phoneNumber;
    public Integer address;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getAddress() {
        return address;
    }

    public void setAddress(Integer address) {
        this.address = address;
    }
}
