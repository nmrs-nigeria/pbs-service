/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.controllers;

import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.main.FingerPrintUtilImpl;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.FingerPrintMatchInputModel;
import com.nmrs.umb.biometriclinux.models.ResponseModel;
import com.nmrs.umb.biometriclinux.models.SaveModel;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
@RestController
public class FingerPrintController {

    Logger logger = Logger.getLogger(FingerPrintController.class);

    @Autowired
    FingerPrintUtilImpl fingerPrintUtilImpl;

    @Autowired
    DbManager dbManager;

    @RequestMapping(value = "api/FingerPrint/CapturePrint")
    public ResponseEntity<FingerPrintInfo> CapturePrint(@RequestParam int fingerPosition) {

        FingerPrintInfo responseObject = fingerPrintUtilImpl.capture(fingerPosition, null, false);

        try {
            if (Objects.isNull(responseObject.getErrorMessage())) {
                dbManager.getConnection();
                List<FingerPrintInfo> allPrevious = dbManager.GetPatientBiometricinfo(0);

                int matchedPatientId = fingerPrintUtilImpl.verify(new FingerPrintMatchInputModel(responseObject.Template, allPrevious));

                if (matchedPatientId != 0) {
                    String patientName = dbManager.RetrievePatientNameByPersonId(matchedPatientId);

                    String errString = MessageFormat.format("Finger print record already exist for this patient {0} Name : {1} {2} Person Identifier : {3}",
                            "\n", patientName, "\n", matchedPatientId);
                    responseObject.setErrorMessage(errString);
                }else {
                    responseObject.setErrorMessage("");
                }
            }
        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
        } finally {
            try {
                dbManager.closeConnection();
            } catch (SQLException ex) {
                  logger.log(Logger.Level.FATAL, ex);
            }
        }

        return new ResponseEntity<>(responseObject, HttpStatus.OK);

    }

    @RequestMapping(value = "api/FingerPrint/reCapturePrint")
    public ResponseEntity<FingerPrintInfo> reCapturePrint(@RequestParam int fingerPosition, @RequestParam String patientId) {

        FingerPrintInfo responseObject = fingerPrintUtilImpl.capture(fingerPosition, null, false);

        try {
            if (Objects.isNull(responseObject.getErrorMessage())) {
                dbManager.getConnection();
                List<FingerPrintInfo> allPrevious = dbManager.GetPatientBiometricInfoExcept(patientId,fingerPosition);

                int matchedPatientId = fingerPrintUtilImpl.verify(new FingerPrintMatchInputModel(responseObject.Template, allPrevious));

                if (matchedPatientId != 0) {
                    responseObject.setErrorMessage("This finger has been captured");
                }else {
                    responseObject.setErrorMessage("");
                }
            }
        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
        } finally {
            try {
                dbManager.closeConnection();
            } catch (SQLException ex) {
                logger.log(Logger.Level.FATAL, ex);
            }
        }

        return new ResponseEntity<>(responseObject, HttpStatus.OK);

    }

    @RequestMapping(value = "api/FingerPrint/CheckForPreviousCapture")
    public ResponseEntity<?> CheckForPreviousCapture(@RequestParam String PatientUUID) {
        List<FingerPrintInfo> fingerPrint;
        if (Objects.isNull(PatientUUID) || PatientUUID.equals("undefined")) {
            return new ResponseEntity("Invalid Patient Id", HttpStatus.BAD_REQUEST);
        }
        try {
            dbManager.getConnection();
            Map<String, String> patientInfo = dbManager.RetrievePatientIdAndNameByUUID(PatientUUID);

            if (patientInfo != null) {
                fingerPrint = dbManager.GetPatientBiometricinfo(Integer.parseInt(patientInfo.get("person_id")));
                dbManager.closeConnection();
                return new ResponseEntity<>(fingerPrint, HttpStatus.OK);
            }

        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex.getMessage());
            return new ResponseEntity("Error occurred getting patient information",HttpStatus.BAD_REQUEST);
        }
        return null;
    }

    @RequestMapping(value = "api/FingerPrint/SaveToDatabase")
    public ResponseEntity<?> SaveToDatabase(@RequestBody SaveModel model) {
        List<FingerPrintInfo> fingerPrint = new ArrayList<>();
        ResponseModel responseModel = new ResponseModel();
        
        if(model.FingerPrintList.size() < 6){
            responseModel.setErrorMessage("Biometric must contain 6 or more fingers");
            responseModel.setIsSuccessful(false);
            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
        }

        try {
            String patientUUID = model.PatientUUID;
            dbManager.getConnection();
            Map<String, String> patientInfo = dbManager.RetrievePatientIdAndNameByUUID(patientUUID);
            if (patientInfo != null) {
                int pid = Integer.parseInt(patientInfo.get("person_id"));
                model.getFingerPrintList().forEach(a -> {
                    a.setPatienId(pid);
                    a.setCreator(0);
                    fingerPrint.add(a);
                });

                //verify
                if(containsDuplicate(fingerPrint)){
                    responseModel.setErrorMessage("Biometric contains duplicate fingers kindly rescan");
                    responseModel.setIsSuccessful(false);
                    return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
                }

                responseModel = dbManager.SaveToDatabase(fingerPrint, false);
                dbManager.closeConnection();
                return new ResponseEntity<>(responseModel, HttpStatus.OK);

            } else {

                responseModel.setErrorMessage("Invalid patientId supplied");
                responseModel.setIsSuccessful(false);
                dbManager.closeConnection();
                return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception ex) {
            responseModel.setErrorMessage("Error occurrd while performing your request");
            responseModel.setIsSuccessful(false);
            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
        }

    }

    @RequestMapping(value = "api/FingerPrint/reSaveToDatabase")
    public ResponseEntity<?> reSaveToDatabase(@RequestBody SaveModel model) {
        List<FingerPrintInfo> fingerPrint = new ArrayList<>();
        ResponseModel responseModel = new ResponseModel();

        try {
            String patientUUID = model.PatientUUID;
            dbManager.getConnection();
            Map<String, String> patientInfo = dbManager.RetrievePatientIdAndNameByUUID(patientUUID);
            if (patientInfo != null) {
                int pid = Integer.parseInt(patientInfo.get("person_id"));
                model.getFingerPrintList().forEach(a -> {
                    a.setPatienId(pid);
                    a.setCreator(0);
                    fingerPrint.add(a);
                });

                //verify
                if(containsDuplicate(fingerPrint)){
                    responseModel.setErrorMessage("Biometric contains duplicate fingers kindly rescan");
                    responseModel.setIsSuccessful(false);
                    return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
                }

                responseModel = dbManager.SaveToDatabase(fingerPrint, true);
                dbManager.closeConnection();
                return new ResponseEntity<>(responseModel, HttpStatus.OK);

            } else {

                responseModel.setErrorMessage("Invalid patientId supplied");
                responseModel.setIsSuccessful(false);
                dbManager.closeConnection();
                return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception ex) {
            responseModel.setErrorMessage("Error occurrd while performing your request");
            responseModel.setIsSuccessful(false);
            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
        }

    }

    private boolean containsDuplicate(List<FingerPrintInfo> fingerPrint) {
        int index = 0;
        while (index < fingerPrint.size()) {
            List<FingerPrintInfo> compare = new ArrayList<>(fingerPrint);
            compare.remove(index);
            int matchedId = fingerPrintUtilImpl.verify(new FingerPrintMatchInputModel(fingerPrint.get(index).getTemplate(),compare));
            if (matchedId != 0) {
               return  true;
            }
            index++;
        }
        return false;
    }

    @DeleteMapping(value = "api/FingerPrint/deleteFingerPrint")
    public ResponseEntity<?> deleteFingerPrint(@RequestParam String patientId) {
        try {
            dbManager.getConnection();
            dbManager.deletePatientBiometricInfo(patientId);
            dbManager.closeConnection();
        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
