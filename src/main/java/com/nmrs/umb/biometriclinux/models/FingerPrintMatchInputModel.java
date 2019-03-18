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
 public class FingerPrintMatchInputModel {

        private String FingerPrintTemplate;

        private List<FingerPrintInfo> FingerPrintTemplateListToMatch;

    public FingerPrintMatchInputModel(String FingerPrintTemplate, List<FingerPrintInfo> FingerPrintTemplateListToMatch) {
        this.FingerPrintTemplate = FingerPrintTemplate;
        this.FingerPrintTemplateListToMatch = FingerPrintTemplateListToMatch;
    }
        
        

        public String getFingerPrintTemplate() {
            return FingerPrintTemplate;
        }

        public void setFingerPrintTemplate(String FingerPrintTemplate) {
            this.FingerPrintTemplate = FingerPrintTemplate;
        }

        public List<FingerPrintInfo> getFingerPrintTemplateListToMatch() {
            return FingerPrintTemplateListToMatch;
        }

        public void setFingerPrintTemplateListToMatch(List<FingerPrintInfo> FingerPrintTemplateListToMatch) {
            this.FingerPrintTemplateListToMatch = FingerPrintTemplateListToMatch;
        }

    }
