/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.models;

import java.util.List;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
 public class SaveModel {

        public List<FingerPrintInfo> FingerPrintList;
        public String PatientUUID;

        public List<FingerPrintInfo> getFingerPrintList() {
            return FingerPrintList;
        }

        public void setFingerPrintList(List<FingerPrintInfo> FingerPrintList) {
            this.FingerPrintList = FingerPrintList;
        }

        public String getPatientUUID() {
            return PatientUUID;
        }

        public void setPatientUUID(String PatientUUID) {
            this.PatientUUID = PatientUUID;
        }

    }
