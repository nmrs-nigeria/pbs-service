/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class Patient {

    public String patientId;
    public String name;
    public String gender;
    public String birthDate;
    public String Age;
    public String pepFarID;
    public String hospID;

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAge() {
        return Age;
    }

    public void setAge(String age) {
        Age = age;
    }

    public String getPepFarID() {
        return pepFarID;
    }

    public void setPepFarID(String pepFarID) {
        this.pepFarID = pepFarID;
    }

    public String getHospID() {
        return hospID;
    }

    public void setHospID(String hospID) {
        this.hospID = hospID;
    }
}
