/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.main.FingerPrintUtilImpl;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.FingerPrintMatchInputModel;
import com.nmrs.umb.biometriclinux.models.ResponseModel;
import com.nmrs.umb.biometriclinux.models.SaveModel;
import com.nmrs.umb.biometriclinux.security.Utils;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    @Value("${bulk.verify:true}")
    boolean bulkVerify;

    @Value("${verify:false}")
    boolean verify;

    @RequestMapping(value = "api/FingerPrint/CapturePrint")
    public ResponseEntity<FingerPrintInfo> CapturePrint(@RequestParam int fingerPosition) {
        FingerPrintInfo responseObject;


        try {
             responseObject = fingerPrintUtilImpl.capture(fingerPosition, null, false);

            System.out.println("captured");

            if (Objects.isNull(responseObject.getErrorMessage())) {
                if(verify) {
                    if (!bulkVerify) {
                        dbManager.getConnection();
                        System.out.println("getting all fingerprint in the database");
                        List<FingerPrintInfo> allPrevious = dbManager.GetPatientBiometricinfo(0);

                        int matchedPatientId = fingerPrintUtilImpl.verify(new FingerPrintMatchInputModel(responseObject.Template, allPrevious));

                        if (matchedPatientId != 0) {
                            String patientName = dbManager.RetrievePatientNameByPersonId(matchedPatientId);

                            String errString = MessageFormat.format("Finger print record already exist for this patient {0} Name : {1} {2} Person Identifier : {3} ",
                                    "\n", patientName, "\n", matchedPatientId);
                            responseObject.setErrorMessage(errString);
                        }
                    }
                }
            }

            if (responseObject.getErrorMessage() == null)  responseObject.setErrorMessage("");

        } catch (Exception ex) {
            responseObject = new FingerPrintInfo();
            responseObject.setErrorMessage(ex.getMessage());
            logger.log(Logger.Level.FATAL, ex);
        } finally {
            try {
                dbManager.closeConnection();
            } catch (SQLException ex) {
                responseObject = new FingerPrintInfo();
                responseObject.setErrorMessage(ex.getMessage());
                  logger.log(Logger.Level.FATAL, ex);
            }
        }
        return new ResponseEntity<>(responseObject, HttpStatus.OK);

    }

    @RequestMapping(value = "api/FingerPrint/PimsCapturePrint")
    public ResponseEntity<FingerPrintInfo> PimsCapturePrint(@RequestParam int fingerPosition, @RequestParam String verifyOption) {
        FingerPrintInfo responseObject;


        try {
            responseObject = fingerPrintUtilImpl.capture(fingerPosition, null, false);

            System.out.println("captured");

            if (Objects.isNull(responseObject.getErrorMessage())) {

//                if(verify) {
//                    if (!bulkVerify) {
            if(verifyOption.equalsIgnoreCase("facilityverify")) {
                dbManager.getConnection();
                System.out.println("getting all fingerprint in the database");
                List<FingerPrintInfo> allPrevious = dbManager.GetPatientBiometricinfo(0);

                int matchedPatientId = fingerPrintUtilImpl.verify(new FingerPrintMatchInputModel(responseObject.Template, allPrevious));

                if (matchedPatientId != 0) {
                    String patientName = dbManager.RetrievePatientNameByPersonId(matchedPatientId);

                    String errString = MessageFormat.format("Finger print record already exist for this patient {0} Name : {1} {2} Person Identifier : {3} ",
                            "\n", patientName, "\n", matchedPatientId);
                    responseObject.setErrorMessage(errString);
                }
            }

//                    }
//                }


            }

            if (responseObject.getErrorMessage() == null)  responseObject.setErrorMessage("");

        } catch (Exception ex) {
            responseObject = new FingerPrintInfo();
            responseObject.setErrorMessage(ex.getMessage());
            logger.log(Logger.Level.FATAL, ex);
        } finally {
            try {
                dbManager.closeConnection();
            } catch (SQLException ex) {
                responseObject = new FingerPrintInfo();
                responseObject.setErrorMessage(ex.getMessage());
                logger.log(Logger.Level.FATAL, ex);
            }
        }
        return new ResponseEntity<>(responseObject, HttpStatus.OK);

    }

    @RequestMapping(value = "api/FingerPrint/reCapturePrint")
    public ResponseEntity<FingerPrintInfo> reCapturePrint(@RequestParam int fingerPosition, @RequestParam String patientId) {

        FingerPrintInfo responseObject = fingerPrintUtilImpl.capture(fingerPosition, null, false);

        try {
            if (responseObject != null && Objects.isNull(responseObject.getErrorMessage())) {
                if(verify) {
                    if (!bulkVerify) {
                        dbManager.getConnection();
                        List<FingerPrintInfo> allPrevious = dbManager.GetPatientBiometricInfoExcept(patientId);

                        int matchedPatientId = fingerPrintUtilImpl.verify(new FingerPrintMatchInputModel(responseObject.Template, allPrevious));

                        if (matchedPatientId != 0) {
                            String patientName = dbManager.RetrievePatientNameByPersonId(matchedPatientId);

                            String errString = MessageFormat.format("Finger print record already exist for this patient {0} Name : {1} {2} Person Identifier : {3}",
                                    "\n", patientName, "\n", matchedPatientId);
                            responseObject.setErrorMessage(errString);
                        }
                    }
                }
                if (responseObject.getErrorMessage() == null)  responseObject.setErrorMessage("");
            }

        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
            responseObject = new FingerPrintInfo();
            responseObject.setErrorMessage(ex.getMessage());
        } finally {
            try {
                dbManager.closeConnection();
            } catch (SQLException ex) {
                responseObject = new FingerPrintInfo();
                responseObject.setErrorMessage(ex.getMessage());
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
            return new ResponseEntity("Error occurred getting information - "+ex.getMessage(),HttpStatus.BAD_REQUEST);
        }
        return null;
    }

    @RequestMapping(value = "api/FingerPrint/SaveToDatabase")
    public ResponseEntity<?> SaveToDatabase(@RequestBody SaveModel model) {
        List<FingerPrintInfo> fingerPrint = new ArrayList<>();
        List<String> prints = new ArrayList<>();
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
                    if(a.getTemplate() != null) {
                        prints.add(a.getTemplate());
                    }
                });
                //verify
                if(Utils.containsDuplicate(fingerPrint,fingerPrintUtilImpl)){
                    responseModel.setErrorMessage("Biometric contains duplicate fingers kindly rescan");
                    responseModel.setIsSuccessful(false);
                    return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
                }
                if(bulkVerify) {
                    if(verify) {
                        String response = Utils.inDb(prints, dbManager, fingerPrintUtilImpl);
                        if (response != null) {
                            responseModel.setErrorMessage(response);
                            responseModel.setIsSuccessful(false);
                            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
                        }
                    }
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
            responseModel = new ResponseModel();
            responseModel.setErrorMessage("Error occurrd while performing your request - "+ex.getMessage());
            responseModel.setIsSuccessful(false);
            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
        }

    }

    @RequestMapping(value = "api/FingerPrint/reSaveToDatabase")
    public ResponseEntity<?> reSaveToDatabase(@RequestBody SaveModel model) {
        List<FingerPrintInfo> fingerPrint = new ArrayList<>();
        List<String> prints = new ArrayList<>();
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
                    if(a.getTemplate() != null) {
                        prints.add(a.getTemplate());
                    }
                });

                //verify
                if(Utils.containsDuplicate(fingerPrint,fingerPrintUtilImpl)){
                    responseModel.setErrorMessage("Biometric contains duplicate fingers kindly rescan");
                    responseModel.setIsSuccessful(false);
                    return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
                }
                if(bulkVerify) {
                    if(verify) {
                        String response = Utils.inDb(prints, dbManager, fingerPrintUtilImpl);
                        if (response != null) {
                            responseModel.setErrorMessage(response);
                            responseModel.setIsSuccessful(false);
                            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
                        }
                    }
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
            responseModel = new ResponseModel();
            responseModel.setErrorMessage("Error occurrd while performing your request - "+ex.getMessage());
            responseModel.setIsSuccessful(false);
            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
        }

    }

    @DeleteMapping(value = "api/FingerPrint/deleteFingerPrint")
    public ResponseEntity<?> deleteFingerPrint(@RequestParam String patientId) {
        try {
            dbManager.getConnection();
            dbManager.deletePatientBiometricInfo(patientId);
            dbManager.closeConnection();
        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
            return new ResponseEntity("Error occurred getting patient information - "+ex.getMessage(),HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "api/FingerPrint/ReSaveFingerprintVerificationToDatabase")
    public ResponseEntity<?> saveRecapturedFingerprintVerificationToDatabase(@RequestBody SaveModel model) {
        List<FingerPrintInfo> fingerPrint = new ArrayList<>();
        List<String> prints = new ArrayList<>();
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
                    if(a.getTemplate() != null) {
                        prints.add(a.getTemplate());
                    }
                });
              
                responseModel = dbManager.saveRecapturedFingerprintVerificationToDatabase(fingerPrint, false);
                dbManager.closeConnection();
                return new ResponseEntity<>(responseModel, HttpStatus.OK);
            } else {
                responseModel.setErrorMessage("Invalid patientId supplied");
                responseModel.setIsSuccessful(false);
                dbManager.closeConnection();
                return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception ex) {
            responseModel = new ResponseModel();
            responseModel.setErrorMessage("Error occurrd while performing your request - "+ex.getMessage());
            responseModel.setIsSuccessful(false);
            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
        }

    }
    
    @RequestMapping(value = "api/FingerPrint/verificationCapturePrint")
    public ResponseEntity<FingerPrintInfo> verificationCapturePrint(@RequestParam int fingerPosition, @RequestParam String patientId) {

        FingerPrintInfo responseObject = fingerPrintUtilImpl.capture(fingerPosition, null, false);

        try {
            if (responseObject != null && Objects.isNull(responseObject.getErrorMessage())) {
                  dbManager.getConnection();                 
                  int pid = Integer.parseInt(patientId);
                  
                  List<FingerPrintInfo> allPrevious = dbManager.GetPatientBiometricinfo(pid);

                  int matchedPatientId = fingerPrintUtilImpl.verify(new FingerPrintMatchInputModel(responseObject.Template, allPrevious));
                        
                  if (matchedPatientId != 0) {
                	  String patientName = dbManager.RetrievePatientNameByPersonId(matchedPatientId);

                      String errString = MessageFormat.format("Finger print record already exist for this patient {0} Name : {1} {2} Person Identifier : {3}",
                                    "\n", patientName, "\n", matchedPatientId);
                      responseObject.setErrorMessage(errString);
                  }
                  
               
                if (responseObject.getErrorMessage() == null)  responseObject.setErrorMessage("");
            }

        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
            responseObject = new FingerPrintInfo();
            responseObject.setErrorMessage(ex.getMessage());
        } finally {
            try {
                dbManager.closeConnection();
            } catch (SQLException ex) {
                responseObject = new FingerPrintInfo();
                responseObject.setErrorMessage(ex.getMessage());
                logger.log(Logger.Level.FATAL, ex);
            }
        }

        return new ResponseEntity<>(responseObject, HttpStatus.OK);

    }




}
