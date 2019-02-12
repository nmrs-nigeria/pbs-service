/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.controllers;

import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.main.FingerPrintUtilImpl;
import com.nmrs.umb.biometriclinux.models.AppModel;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.ResponseModel;
import com.nmrs.umb.biometriclinux.models.SaveModel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
@RestController
public class FingerPrintController {

    FingerPrintUtilImpl fingerPrintUtilImpl = new FingerPrintUtilImpl();
    FingerPrintInfo responseObject = null;
    private DbManager dbManager = null;

    @RequestMapping(value = "api/FingerPrint/CapturePrint")
    public ResponseEntity<?> CapturePrint(@RequestParam int fingerPosition) {

        responseObject = new FingerPrintInfo();
        responseObject = fingerPrintUtilImpl.capture(fingerPosition, null, false);

        return new ResponseEntity<>(responseObject, HttpStatus.OK);

    }

    @RequestMapping(value = "api/FingerPrint/CheckForPreviousCapture")
    public ResponseEntity<?> CheckForPreviousCapture(String PatientUUID) {
        List<FingerPrintInfo> fingerPrint = new ArrayList<>();
        try {
            dbManager = new DbManager();
            Map<String, String> patientInfo = dbManager.RetrievePatientIdByUUID(PatientUUID);

            if (patientInfo != null) {
                fingerPrint = dbManager.GetPatientBiometricinfo(Integer.parseInt(patientInfo.get("person_id")));
                return new ResponseEntity<>(fingerPrint, HttpStatus.OK);
            }

        } catch (NumberFormatException | SQLException ex) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return null;
    }

     @RequestMapping(value = "api/FingerPrint/SaveToDatabase")
    public ResponseEntity<?> SaveToDatabase(SaveModel model) {
        dbManager = new DbManager();
        List<FingerPrintInfo> fingerPrint = new ArrayList<>();
        ResponseModel responseModel = new ResponseModel();

        try {
            String patientUUID = model.PatientUUID;
            Map<String, String> patientInfo = dbManager.RetrievePatientIdByUUID(patientUUID);
            if (patientInfo != null) {
                int pid = Integer.parseInt(patientInfo.get("person_id"));
                model.FingerPrintList.stream().forEach(a -> {
                    a.PatienId = pid;
                });

                responseModel = dbManager.SaveToDatabase(fingerPrint);
                return new ResponseEntity<>(responseModel, HttpStatus.OK);

            } else {

                responseModel.setErrorMessage("Invalid patientId supplied");
                responseModel.setIsSuccessful(false);
                return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception ex) {
            responseModel.setErrorMessage(ex.getMessage());
            responseModel.setIsSuccessful(false);
            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
        }

    }

}
