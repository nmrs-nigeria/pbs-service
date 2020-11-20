/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.models;

/**
 *
 * @author MORRISON.I
 */
public class UploadTemplate {

    private String PatientID;
    private String DatimCode;
    private String CaptureData;

    public String getPatientID() {
        return PatientID;
    }

    public void setPatientID(String PatientID) {
        this.PatientID = PatientID;
    }

    public String getDatimCode() {
        return DatimCode;
    }

    public void setDatimCode(String DatimCode) {
        this.DatimCode = DatimCode;
    }

    public String getCaptureData() {
        return CaptureData;
    }

    public void setCaptureData(String CaptureData) {
        this.CaptureData = CaptureData;
    }

    @Override
    public String toString() {
        return "UploadTemplate{" + "PatientID=" + PatientID + ", DatimCode=" + DatimCode + ", CaptureData=" + CaptureData + '}';
    }
    
    

}
