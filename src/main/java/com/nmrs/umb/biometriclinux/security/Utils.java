package com.nmrs.umb.biometriclinux.security;

import com.nmrs.umb.biometriclinux.controllers.FingerPrintController;
import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.main.FingerPrintUtilImpl;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.FingerPrintMatchInputModel;
import org.jboss.logging.Logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    static Logger logger = Logger.getLogger(Utils.class);

    public static boolean containsDuplicate(List<FingerPrintInfo> fingerPrint, FingerPrintUtilImpl fingerPrintUtil) {
        try {
            int index = 0;
            while (index < fingerPrint.size()) {
                List<FingerPrintInfo> compare = new ArrayList<>(fingerPrint);
                compare.remove(index);
                int matchedId = fingerPrintUtil.verify(new FingerPrintMatchInputModel(fingerPrint.get(index).getTemplate(), compare));
                if (matchedId != 0) {
                    return true;
                }
                index++;
            }
        }catch (Exception ignored){

        }
        return false;
    }

    public static String inDb(List<String> fingerPrint, DbManager dbManager, FingerPrintUtilImpl fingerPrintUtil) {

        try {
            dbManager.getConnection();
            System.out.println("getting all fingerprint in the database");
            List<FingerPrintInfo> allPrevious = dbManager.GetPatientBiometricinfo(0);
            int matchedPatientId = fingerPrintUtil.verify(new FingerPrintMatchInputModel(allPrevious, fingerPrint));
            if (matchedPatientId != 0) {
                String patientName = dbManager.RetrievePatientNameByPersonId(matchedPatientId);
                return MessageFormat.format("Finger print record already exist for this patient {0} Name : {1} {2} Person Identifier : {3} ",
                        "\n", patientName, "\n", matchedPatientId);
            }
        }catch (Exception ex){
            logger.log(Logger.Level.FATAL, ex);
        }
        return null;
    }
    
    public static String inDbPerPatient(List<FingerPrintInfo> fingerPrint, List<String> prints, int pid, DbManager dbManager, FingerPrintUtilImpl fingerPrintUtil) {

        try {
            dbManager.getConnection();
            System.out.println("getting all fingerprint in the database for a patient");
            List<FingerPrintInfo> allPrevious = dbManager.GetPatientBiometricinfo(pid);
            int matchedPatientId = fingerPrintUtil.verify(new FingerPrintMatchInputModel(allPrevious, prints));
            if (matchedPatientId != 0) {
                String patientName = dbManager.RetrievePatientNameByPersonId(matchedPatientId);
                return MessageFormat.format("Finger print record already exist for this patient {0} Name : {1} {2} Person Identifier : {3} ",
                        "\n", patientName, "\n", matchedPatientId);
            }
        }catch (Exception ex){
            logger.log(Logger.Level.FATAL, ex);
        }
        return null;
    }

    
    
    
}
