/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.dal;

import com.nmrs.umb.biometriclinux.main.AppUtil;
import com.nmrs.umb.biometriclinux.main.FingerPrintUtilImpl;
import com.nmrs.umb.biometriclinux.models.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
@Configuration
public class DbManager {

    @Autowired
    Environment env;
    @Autowired
    FingerPrintUtilImpl fingerPrintUtilImpl;
    
    private Connection conn = null;
    private Statement statement = null;
    private PreparedStatement ppStatement = null;
    private ResultSet resultSet = null;
    private final String TABLENAME = "biometricinfo";

    Logger logger = Logger.getLogger(DbManager.class);
    
    public Connection openConnection() throws ClassNotFoundException, SQLException {
        String server = env.getProperty("app.server");
        String dbName = env.getProperty("app.dbname");
        String dbUsername = env.getProperty("app.username");
        String dbPassword = env.getProperty("app.password");
        String dbPort = env.getProperty("app.dbport");

        // String serverUrl = "jdbc:mysql://localhost:3306/openmrs?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        String serverUrl = MessageFormat.format("jdbc:mysql://{0}:{1}/{2}?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false", server, dbPort, dbName);
        
        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager
                .getConnection(serverUrl, dbUsername, dbPassword);
        return conn;
    }

    public Connection getConnection() throws Exception {
        return this.openConnection();
    }
    
    private void createFingerPrintTable() throws Exception {
        //  DbManager dbManager = new DbManager();
        // openConnection();
        statement = getConnection().createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS `biometricinfo` (`"
                + "biometricInfo_Id` INT(11) NOT NULL AUTO_INCREMENT,"
                + "`patient_Id` INT(11) NOT NULL,`template` TEXT ,`new_template` LONGBLOB,"
                + "`imageWidth` INT(11) DEFAULT NULL,`imageHeight` INT(11) DEFAULT NULL,"
                + "`imageDPI` INT(11) DEFAULT NULL,"
                + "`imageQuality` INT(11) DEFAULT NULL,"
                + "`fingerPosition` VARCHAR(50) DEFAULT NULL,"
                + "`serialNumber` VARCHAR(255) DEFAULT NULL,"
                + "`model` VARCHAR(255) DEFAULT NULL,"
                + "`manufacturer` VARCHAR(255) DEFAULT NULL,"
                + "`creator` INT(11) DEFAULT NULL,"
                + "`date_created` DATETIME DEFAULT NULL,"
                + "PRIMARY KEY(`biometricInfo_Id`),"
                + "FOREIGN KEY(patient_Id) REFERENCES patient(patient_Id),"
                + "FOREIGN KEY(creator) REFERENCES patient(creator)) ENGINE = MYISAM AUTO_INCREMENT = 2 DEFAULT CHARSET = utf8; ");
        
    }
    
    public List<FingerPrintInfo> GetPatientBiometricinfo(int patientId) throws Exception {
        
        if (patientId != 0) {
            String sql = "SELECT patient_id, COALESCE(template, CONVERT(new_template USING utf8)) as template," +
                    " imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, " +
                    "manufacturer, date_created, creator FROM " + TABLENAME + " where patient_id = ? ";
            ppStatement = getConnection().prepareStatement(sql);
            ppStatement.setInt(1, patientId);
            resultSet = ppStatement.executeQuery();
        } else {
            String sql = "SELECT patient_id, COALESCE(template, CONVERT(new_template USING utf8)) as template, imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, manufacturer, date_created, creator FROM " + TABLENAME;
            ppStatement = getConnection().prepareStatement(sql);
            resultSet = ppStatement.executeQuery();
        }
        
        return converToFingerPrintList(resultSet);
    }

    public List<FingerPrintInfo> GetPatientBiometricInfoExcept(String patientUUID) throws Exception {

        String sql = "SELECT patient_id, COALESCE(template, CONVERT(new_template USING utf8)) as template," +
                " imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, " +
                "manufacturer, date_created, creator FROM " + TABLENAME +" where " +
                "(patient_id != (select p.person_id from person p where p.uuid = ? )) ";

        ppStatement = getConnection().prepareStatement(sql);
        ppStatement.setString(1, patientUUID);

        resultSet = ppStatement.executeQuery();
        return converToFingerPrintList(resultSet);

    }

    public FingerPrintInfo GetPatientBiometricInfo(int patientId, String fingerPosition) throws Exception {

        String sql = "SELECT patient_id, COALESCE(template, CONVERT(new_template USING utf8)) as template," +
                " imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, " +
                "manufacturer, date_created, creator FROM " + TABLENAME +" where patient_id = ? AND fingerPosition = ? ";

        ppStatement = getConnection().prepareStatement(sql);
        ppStatement.setString(1, String.valueOf(patientId));
        ppStatement.setString(2, fingerPosition);

        resultSet = ppStatement.executeQuery();
        List<FingerPrintInfo> fingerInfoList = converToFingerPrintList(resultSet);
        if (fingerInfoList.size() > 0) return fingerInfoList.get(0);
        return null;

    }

    public Set<Integer> getPatientsWithLowQualityData() throws Exception {

        String sql = "SELECT patient_id, COALESCE(template, CONVERT(new_template USING utf8)) as template," +
                " imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, " +
                "manufacturer, date_created, creator FROM " + TABLENAME +" where imageQuality < ? ORDER BY patient_id";

        ppStatement = getConnection().prepareStatement(sql);
        ppStatement.setInt(1, AppUtil.QUALITY_THRESHOLD);

        resultSet = ppStatement.executeQuery();
        Set<Integer> printInfos =  convertToDistinctFingerPrintList(resultSet, false);
        this.closeConnection();
        return printInfos;

    }

    public Set<Integer> getPatientsWithoutFingerPrintData() throws Exception {
    String sql = "select distinct p.patient_id from patient p where p.patient_id not in (SELECT distinct b.patient_id from " + TABLENAME +" b)" +
            "and p.voided = false";

        ppStatement = getConnection().prepareStatement(sql);

        resultSet = ppStatement.executeQuery();
        Set<Integer> printInfos =  convertToDistinctList(resultSet);
        this.closeConnection();
        return printInfos;

    }

    public Set<Integer> getPatientsWithInvalidData() throws Exception {

        String sql = "SELECT patient_id, COALESCE(template, CONVERT(new_template USING utf8)) as template," +
                " imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, " +
                "manufacturer, date_created, creator FROM " + TABLENAME + " ORDER BY patient_id";

        ppStatement = getConnection().prepareStatement(sql);

        resultSet = ppStatement.executeQuery();
        Set<Integer> printInfos =  convertToDistinctFingerPrintList(resultSet, true);
        this.closeConnection();
        return printInfos;

    }

    private Set<Integer> convertToDistinctList(ResultSet resultSet) throws SQLException {

        Set<Integer> patientIds = new HashSet<>();

        while (resultSet.next()) {
            if(resultSet.getString("patient_id") != null) {
                patientIds.add(Integer.parseInt(resultSet.getString("patient_id")));
            }
        }
        return patientIds;

    }

    private Set<Integer> convertToDistinctFingerPrintList(ResultSet resultSet, boolean returnOnlyInValid) throws SQLException {

        Set<Integer> patientIds = new HashSet<>();

        while (resultSet.next()) {
            if(returnOnlyInValid){
                if(resultSet.getString("template") != null && !fingerPrintUtilImpl.isValid(resultSet.getString("template"))) {
                    FingerPrintInfo fingerPrintInfo = getFingerPrintInfo(resultSet);
                    patientIds.add(fingerPrintInfo.getPatienId());
                }
            }else{
                FingerPrintInfo fingerPrintInfo = getFingerPrintInfo(resultSet);
                patientIds.add(fingerPrintInfo.getPatienId());
            }
        }
        return patientIds;

    }

    private List<FingerPrintInfo> converToFingerPrintList(ResultSet resultSet) throws SQLException {
        
        List<FingerPrintInfo> fingerInfoList = new ArrayList<>();
        
        while (resultSet.next()) {
            FingerPrintInfo fingerPrintInfo = getFingerPrintInfo(resultSet);
            fingerInfoList.add(fingerPrintInfo);
        }
        return fingerInfoList;
        
    }
    private List<Patient> convertToPatientList(ResultSet resultSet) throws SQLException {

        List<Patient> patients = new ArrayList<>();
        while (resultSet.next()) {
            Patient patient = new Patient();
            patient.setPatientId(resultSet.getString("PatientId"));
            patient.setName(resultSet.getString("Name"));
            patient.setGender(resultSet.getString("Gender"));
            patient.setBirthDate(resultSet.getString("BirthDate"));
            patient.setAge(resultSet.getString("Age"));
            patient.setPepFarID(resultSet.getString("PepfarID"));
            patient.setHospID(resultSet.getString("HospID"));
            patients.add(patient);
        }
        return patients;
    }


    private FingerPrintInfo getFingerPrintInfo(ResultSet resultSet) throws SQLException {
        FingerPrintInfo fingerPrintInfo = new FingerPrintInfo();
        fingerPrintInfo.setCreator(0);//default for NMRS
        fingerPrintInfo.setDateCreated(resultSet.getDate("date_created"));
        fingerPrintInfo.setPatienId(resultSet.getInt("patient_id"));
        fingerPrintInfo.setImageWidth(resultSet.getInt("imageWidth"));
        fingerPrintInfo.setImageHeight(resultSet.getInt("imageHeight"));
        fingerPrintInfo.setImageDPI(resultSet.getInt("imageDPI"));
        fingerPrintInfo.setImageQuality(resultSet.getInt("imageQuality"));
        if (resultSet.getInt("imageQuality") < AppUtil.QUALITY_THRESHOLD) {
            fingerPrintInfo.setQualityFlag(AppUtil.LOW_QUALITY_FLAG);
        } else {
            fingerPrintInfo.setQualityFlag(AppUtil.VALID_QUALITY_FLAG);
        }

        fingerPrintInfo.setFingerPositions(AppModel.FingerPositions.valueOf(resultSet.getString("fingerPosition")));
        fingerPrintInfo.setSerialNumber(resultSet.getString("serialNumber"));
        fingerPrintInfo.setModel(resultSet.getString("model"));
        fingerPrintInfo.setManufacturer(resultSet.getString("manufacturer"));

        if (resultSet.getString("template") != null) {
            if (fingerPrintUtilImpl.isValid(resultSet.getString("template"))) {
                fingerPrintInfo.setTemplate(resultSet.getString("template"));
            } else {
                fingerPrintInfo.setQualityFlag(AppUtil.INVALID_FINGER_PRINTS);
            }
        }
        return fingerPrintInfo;
    }

    public ByteArrayInputStream getCsvFilePath(Set<Integer> patientIds, String datimCode){
       try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (final CSVPrinter printer = new CSVPrinter(new PrintWriter(out),
                    CSVFormat.DEFAULT.withHeader("PatientID", "Name", "Gender", "BirthDate", "Age", "PepfarID", "HospID", "DatimCode"))) {
                    List<Patient> patients = getPatientDetails(patientIds);
                    for(Patient patient: patients) {
                        if (patient != null) {
                            printer.printRecord(patient.getPatientId(), patient.getName(), patient.getGender(),
                                    patient.getBirthDate(), patient.getAge(), patient.getPepFarID(), patient.getHospID(), datimCode);
                        }
                    }
                printer.flush();
                return new ByteArrayInputStream(out.toByteArray());
            }
        }
        catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
        }
        return null;
    }

    public void closeConnection() throws SQLException {
        if (Objects.nonNull(conn)) {
            conn.close();
        }
        if (Objects.nonNull(ppStatement)) {
            ppStatement.close();
        }
        
    }
    
    public void Save(FingerPrintInfo fingerPrint, boolean update) throws Exception {
        String sql = "insert into " + TABLENAME + "(patient_Id, imageWidth, imageHeight, imageDPI,  " +
                "imageQuality, fingerPosition, serialNumber, model, manufacturer, creator, date_created, new_template, template)" +
                "Values(?,?,?,?,?,?,?,?,?,?,NOW(),?,?)";
        if(update) {
            sql = "UPDATE " + TABLENAME + " SET " +
                    "patient_Id = ?, " +
                    "imageWidth = ?, " +
                    "imageHeight = ?, " +
                    "imageDPI = ?, " +
                    "imageQuality = ?, " +
                    "fingerPosition = ?, " +
                    "serialNumber = ?, " +
                    "model = ?, " +
                    "manufacturer = ?, " +
                    "creator = ?, " +
                    "date_created = NOW(), " +
                    "new_template = ?, " +
                    "template = ? WHERE patient_id = ? AND fingerPosition = ? ";
        }

        ppStatement = getConnection().prepareStatement(sql);
        ppStatement.setInt(1, fingerPrint.getPatienId());
        ppStatement.setInt(2, fingerPrint.getImageWidth());
        ppStatement.setInt(3, fingerPrint.getImageHeight());
        ppStatement.setInt(4, fingerPrint.getImageDPI());
        ppStatement.setInt(5, fingerPrint.getImageQuality());
        ppStatement.setString(6, fingerPrint.getFingerPositions().name());
        ppStatement.setString(7, fingerPrint.getSerialNumber());
        ppStatement.setString(8, fingerPrint.getModel());
        ppStatement.setString(9, fingerPrint.getManufacturer());
        ppStatement.setInt(10, fingerPrint.getCreator());
        ppStatement.setBlob(11, new ByteArrayInputStream(fingerPrint.getTemplate().getBytes()), fingerPrint.getTemplate().getBytes().length);
        ppStatement.setNull(12, Types.NULL);
        if(update) {
            ppStatement.setInt(13, fingerPrint.getPatienId());
            ppStatement.setString(14, fingerPrint.getFingerPositions().name());
        }
        
        ppStatement.executeUpdate();
        
    }

    public void updatePatientTable(Integer patientId) throws Exception {
        String sql = "UPDATE patient SET date_changed = NOW() WHERE `patient_id` = ? ;";
        ppStatement = getConnection().prepareStatement(sql);
        ppStatement.setInt(1, patientId);
        ppStatement.executeUpdate();
    }

    public List<Patient> getPatientDetails(Set<Integer> patientIds) throws Exception {
        String sql = "SELECT " +
                "    AA.PatientId," +
                "    AA.Name," +
                "    AA.Gender," +
                "    AA.BirthDate," +
                "    AA.Age," +
                "    BB.PepfarID," +
                "    BB.HospID " +
                "FROM" +
                "    (SELECT " +
                "        p.person_id AS PatientId," +
                "            CONCAT(pn.family_name, ' ', pn.given_name) AS Name," +
                "            IF(p.gender = 'M', 'MALE', 'FEMALE') AS Gender," +
                "            IF(p.birthdate_estimated IS NOT TRUE, p.birthdate, 'Estimated') AS BirthDate," +
                "            TIMESTAMPDIFF(YEAR, p.birthdate, CURDATE()) AS Age" +
                "    FROM" +
                "        person p, person_name pn" +
                "    WHERE" +
                "        p.person_id = pn.person_id" +
                "            AND p.person_id in (" + String.join("", Collections.nCopies(patientIds.size()-1, "?,")) + "? )"+
                ") AA" +
                "        LEFT JOIN" +
                "    (SELECT " +
                "        a.patient_id, a.pepfar AS PepfarID, b.hos AS HospID" +
                "    FROM" +
                "        ((SELECT " +
                "        patient_id, identifier AS pepfar" +
                "    FROM" +
                "        patient_identifier" +
                "    WHERE" +
                "        identifier_type = 4) a" +
                "    LEFT JOIN (SELECT " +
                "        patient_id, identifier AS hos" +
                "    FROM" +
                "        patient_identifier" +
                "    WHERE" +
                "        identifier_type = 5) b ON a.patient_id = b.patient_id)" +
                "    GROUP BY a.patient_id) BB ON AA.PatientId = BB.patient_id";

        ppStatement = getConnection().prepareStatement(sql);
        int i = 1;
        for(Integer patientId :patientIds ) {
            ppStatement.setInt(i, patientId);
            i++;
        }

        resultSet = ppStatement.executeQuery();
        List<Patient> patients = convertToPatientList(resultSet);
        this.closeConnection();
        return patients;
    }
    
    public ResponseModel SaveToDatabase(List<FingerPrintInfo> fingerPrintList, boolean update) throws Exception {
        
        ResponseModel responseModel = new ResponseModel();
        
        if (fingerPrintList.isEmpty()) {
            responseModel.setIsSuccessful(false);
            responseModel.setErrorMessage("The request contains an empty list");
        }
        
        try {
            
            for (FingerPrintInfo a : fingerPrintList) {
                if(update){
                    Save(a, this.GetPatientBiometricInfo(a.getPatienId(), a.getFingerPositions().name()) != null);
                }else {
                    Save(a, false);
                }

            }

            updatePatientTable(fingerPrintList.get(0).getPatienId());
            
            responseModel.setIsSuccessful(true);
            responseModel.setErrorMessage("Saved successfully");
        } catch (SQLException ex) {
            responseModel.setIsSuccessful(false);
            responseModel.setErrorMessage(ex.getMessage());
        }
        
        return responseModel;
    }

//    public Map<String, String> RetrievePatientNameByUniqueId(String patientuniqueId) throws SQLException {
//
//        ppStatement = conn.prepareStatement("SELECT CONCAT(given_name,' ',family_name) AS patient_name, pid.patient_id FROM person_name pn "
//                + "INNER JOIN patient_identifier pid ON pn.person_id=pid.patient_id "
//                + "WHERE pid.identifier_type=4 AND pid.identifier= ?;");
//        ppStatement.setString(1, patientuniqueId);
//        resultSet = ppStatement.executeQuery();
//        Map<String, String> nameAndPatientId = new HashMap<>();
//        while (resultSet.next()) {
//
//            nameAndPatientId.put("name", resultSet.getString("patient_name"));
//            nameAndPatientId.put("patientId", resultSet.getString("patient_id"));
//            break;
//        }
//
//        return nameAndPatientId;
//
//    }
//
//   
    //this is a database Id not the unique pepfar nor hospital Id
    public Map<String, String> RetrievePatientNameByPatientId(int patientId) throws Exception {
        
        ppStatement = getConnection().prepareStatement("SELECT CONCAT(given_name,' ',family_name) AS patient_name, pid.identifier FROM person_name pn "
                + "INNER JOIN patient_identifier pid ON pn.person_id = pid.patient_id "
                + "WHERE pid.identifier_type = 4 AND pid.patient_id = ?;");
        
        ppStatement.setInt(1, patientId);
        resultSet = ppStatement.executeQuery();
        Map<String, String> nameAndIdentifier = new HashMap<>();
        while (resultSet.next()) {
            
            nameAndIdentifier.put("name", resultSet.getString("patient_name"));
            nameAndIdentifier.put("Identifier", resultSet.getString("identifier"));
            break;
        }
        
        return nameAndIdentifier;
        
    }
    
    public String RetrievePatientNameByPersonId(int personId) throws Exception {
        
        ppStatement = getConnection().prepareStatement("SELECT CONCAT(given_name,' ',family_name) AS patient_name FROM person_name pn "
                + "WHERE person_id = ?;");
        
        ppStatement.setInt(1, personId);
        resultSet = ppStatement.executeQuery();
        String foundName = "";
        while (resultSet.next()) {
            
            foundName = resultSet.getString("patient_name");
        }
        
        return foundName;
        
    }
    
    public Map<String, String> RetrievePatientIdAndNameByUUID(String UUID) throws Exception {
        //openConnection();
        ppStatement = getConnection().prepareStatement("SELECT CONCAT(given_name,' ',family_name) AS patient_name, p.person_id "
                + "FROM person_name pn INNER JOIN person p ON pn.person_id = p.person_id "
                + "WHERE p.UUID = ?;");
        
        ppStatement.setString(1, UUID);
        resultSet = ppStatement.executeQuery();
        Map<String, String> nameAndPersonMap = new HashMap<>();
        while (resultSet.next()) {
            
            nameAndPersonMap.put("name", resultSet.getString("patient_name"));
            nameAndPersonMap.put("person_id", resultSet.getString("person_id"));
            break;
        }
        //closeConnection();
        return nameAndPersonMap;
        
    }
    
    public int deletePatientBiometricInfo(String patientUid) throws Exception {
        // Map<String,String> nameAndPersonMap =  RetrievePatientIdAndNameByUUID(patientUid);
        ppStatement = getConnection().prepareStatement("DELETE  FROM `biometricinfo` WHERE patient_id in (select p.person_id from person p where p.uuid = ? )");
        ppStatement.setString(1, patientUid);
        
        return ppStatement.executeUpdate();
    }

    public int deleteSpecificPatientBiometricInfo(String patientUid, String fingerPosition) throws Exception {
        // Map<String,String> nameAndPersonMap =  RetrievePatientIdAndNameByUUID(patientUid);
        ppStatement = getConnection().prepareStatement("DELETE  FROM `biometricinfo` WHERE patient_id in (select p.person_id from person p where p.uuid = ? ) and fingerPosition = ?)");
        ppStatement.setString(1, patientUid);
        ppStatement.setString(2, fingerPosition);

        return ppStatement.executeUpdate();
    }

    public String getGlobalProperty(String property) throws Exception {
        ppStatement = getConnection().prepareStatement("SELECT property_value FROM global_property WHERE property = ?;");
        ppStatement.setString(1, property);
        resultSet = ppStatement.executeQuery();
        while (resultSet.next()) {
            return resultSet.getString("property_value");
        }
        return  null;
    }
}
