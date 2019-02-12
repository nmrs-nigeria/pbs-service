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
  public class ResponseModel {

        public boolean IsSuccessful;
        public String ErrorMessage;

        public boolean isIsSuccessful() {
            return IsSuccessful;
        }

        public void setIsSuccessful(boolean IsSuccessful) {
            this.IsSuccessful = IsSuccessful;
        }

        public String getErrorMessage() {
            return ErrorMessage;
        }

        public void setErrorMessage(String ErrorMessage) {
            this.ErrorMessage = ErrorMessage;
        }

    }
