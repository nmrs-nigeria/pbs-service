/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.controllers;

import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.main.AppUtil;
import com.nmrs.umb.biometriclinux.main.FingerPrintUtilImpl;
import com.nmrs.umb.biometriclinux.models.DbModel;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.FingerPrintMatchInputModel;
import com.nmrs.umb.biometriclinux.models.ResponseModel;
import com.nmrs.umb.biometriclinux.models.SaveModel;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
@RestController
public class FingerPrintController {

    Logger logger = Logger.getLogger(FingerPrintController.class);

    @Autowired
    private Environment env;

    //FingerPrintInfo responseObject = null;
    @RequestMapping(value = "api/FingerPrint/CapturePrint")
    public ResponseEntity<FingerPrintInfo> CapturePrint(@RequestParam int fingerPosition) {

        DbManager dbManager = new DbManager(AppUtil.getDatabaseSource(env));
        FingerPrintUtilImpl fingerPrintUtilImpl = new FingerPrintUtilImpl();
        FingerPrintInfo responseObject = fingerPrintUtilImpl.capture(fingerPosition, null, false);

        //
        try {
            if (Objects.isNull(responseObject.getErrorMessage())) {

                dbManager.openConnection();
                List<FingerPrintInfo> allPrevious = dbManager.GetPatientBiometricinfo(0);

                int matchedPatientId = fingerPrintUtilImpl.verify(new FingerPrintMatchInputModel(responseObject.Template, allPrevious));

                if (matchedPatientId != 0) {
                    String patientName = dbManager.RetrievePatientNameByPersonId(matchedPatientId);

                    String errString = MessageFormat.format("Finger print record already exist for this patient {0} Name : {1} {2} Person Identifier : {3}",
                            "\n", patientName, "\n", matchedPatientId);
                    responseObject.setErrorMessage(errString);
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
        List<FingerPrintInfo> fingerPrint = new ArrayList<>();
        if (Objects.isNull(PatientUUID) || PatientUUID.equals("undefined")) {
            return new ResponseEntity("Invalid Patient Id", HttpStatus.BAD_REQUEST);
        }
        try {
            DbModel dbModel = AppUtil.getDatabaseSource(env);
             DbManager dbManager = new DbManager(dbModel);
            dbManager.openConnection();
            Map<String, String> patientInfo = dbManager.RetrievePatientIdAndNameByUUID(PatientUUID);

            if (patientInfo != null) {
                fingerPrint = dbManager.GetPatientBiometricinfo(Integer.parseInt(patientInfo.get("person_id")));
                dbManager.closeConnection();
                return new ResponseEntity<>(fingerPrint, HttpStatus.OK);
            }

        } catch (NumberFormatException | SQLException | ClassNotFoundException ex) {
            logger.log(Logger.Level.FATAL, ex.getMessage());
            return new ResponseEntity("Error occurred getting patient information",HttpStatus.BAD_REQUEST);
        }
        return null;
    }

    @RequestMapping(value = "api/FingerPrint/SaveToDatabase")
    public ResponseEntity<?> SaveToDatabase(@RequestBody SaveModel model) {
        DbModel dbModel = AppUtil.getDatabaseSource(env);
         DbManager dbManager = new DbManager(dbModel);
        List<FingerPrintInfo> fingerPrint = new ArrayList<>();
        ResponseModel responseModel = new ResponseModel();

        try {
            String patientUUID = model.PatientUUID;
            dbManager.openConnection();
            Map<String, String> patientInfo = dbManager.RetrievePatientIdAndNameByUUID(patientUUID);
            if (patientInfo != null) {
                int pid = Integer.parseInt(patientInfo.get("person_id"));
                model.getFingerPrintList().stream().forEach(a -> {
                    a.setPatienId(pid);
                    fingerPrint.add(a);
                });

                responseModel = dbManager.SaveToDatabase(fingerPrint);
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

    @DeleteMapping(value = "api/FingerPrint/deleteFingerPrint")
    public ResponseEntity<?> deleteFingerPrint(@RequestParam String patientId) {

        DbModel dbModel = AppUtil.getDatabaseSource(env);
         DbManager dbManager = new DbManager(dbModel);
        // ResponseModel responseModel = new ResponseModel();

        try {
            dbManager.openConnection();
            dbManager.deletePatientBiometricInfo(patientId);
            dbManager.closeConnection();
        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);

    }

}
