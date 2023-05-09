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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Date;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
    private final String BIOMETRICVERIFICATIONINFO = "biometricverificationinfo";

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
        List<FingerPrintInfo>  printInfos = converToFingerPrintList(resultSet);
        closeConnection();
        return printInfos;
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

    public FingerPrintInfo GetPatientBiometricInfo(int patientId, String fingerPosition, Connection connection) throws Exception {
        String sql = "SELECT patient_id, COALESCE(template, CONVERT(new_template USING utf8)) as template," +
                " imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, " +
                "manufacturer, date_created, creator FROM " + TABLENAME +" where patient_id = ? AND fingerPosition = ? ";

        ppStatement = connection.prepareStatement(sql);
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
//    String sql = "select distinct p.patient_id from patient p where p.patient_id not in (SELECT distinct b.patient_id from " + TABLENAME +" b)" +
//            "and p.voided = false";

    String sql = "SELECT DISTINCT alll.main_id as patient_id FROM (" +
            "SELECT " +
            "a.person_id AS main_id, " +
            "b.person_id," +
            "(TIMESTAMPDIFF(DAY, a.obs_datetime, CURDATE()) -  b.value_numeric) AS diff " +
            "FROM (SELECT person_id, MAX(obs_datetime) AS obs_datetime FROM obs WHERE concept_id IN (159368) AND voided = FALSE GROUP BY person_id) a" +
            " INNER JOIN " +
            "(SELECT person_id, obs_datetime, value_numeric" +
            "           FROM obs" +
            "           WHERE concept_id IN (159368) AND voided = FALSE) b " +
            "ON a.person_id = b.person_id AND a.obs_datetime = b.obs_datetime " +
            "HAVING diff < 28) alll " +
            "WHERE " +
            "alll.main_id NOT IN (SELECT DISTINCT b.patient_id FROM biometricinfo b)";

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
            patient.setPhoneNumber(resultSet.getString("PhoneNumber"));
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

    public ByteArrayOutputStream getCsvFilePath(List<Integer> patientIds, String datimCode){
       try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (final CSVPrinter printer = new CSVPrinter(new PrintWriter(out),
                    CSVFormat.DEFAULT.withHeader("PatientID", "Name", "Gender", "BirthDate", "Age", "PepfarID", "HospID", "DatimCode", "PhoneNumber"))) {
                    List<Patient> patients = getPatientDetails(patientIds);
                System.out.println("In Patient size "+ patientIds.size());
                    System.out.println("Out Patient size "+ patients.size());
                    for(Patient patient: patients) {
                        if (patient != null) {
                            System.out.println(patient.getPepFarID());
                            printer.printRecord(patient.getPatientId(), patient.getName(), patient.getGender(),
                                    patient.getBirthDate(), patient.getAge(), patient.getPepFarID(), patient.getHospID(), datimCode, patient.getPhoneNumber());
                        }else{
                            System.out.println("Patient is null ");
                        }
                    }
                printer.flush();
                return out;
            }
        }
        catch (Exception ex) {
           System.out.println(ex.getMessage());
            logger.log(Logger.Level.FATAL, ex);
        }
        return null;
    }

    public ByteArrayOutputStream getCsvFilePath(List<String> lines){
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (final CSVPrinter printer = new CSVPrinter(new PrintWriter(out),
                    CSVFormat.DEFAULT.withHeader("PatientID", "PepFarId", "DatimCode", "Error Message"))) {
                for(String patient: lines) {
                    String[] unit = patient.split(",");
                        printer.printRecord(unit[0], unit[1], unit[2], unit[3]);
                }
                printer.flush();
                return out;
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
    
    public void Save(FingerPrintInfo fingerPrint, boolean update, Connection connection) throws Exception {
        String sql = "insert into " + TABLENAME + "(patient_Id, imageWidth, imageHeight, imageDPI,  " +
                "imageQuality, fingerPosition, serialNumber, model, manufacturer, creator, date_created, new_template, template)" +
                "Values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
                    "date_created = ?, " +
                    "new_template = ?, " +
                    "template = ? WHERE patient_id = ? AND fingerPosition = ? ";
        }

        ppStatement = connection.prepareStatement(sql);
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
        ppStatement.setDate(11, getDate(fingerPrint.getDateCreated()));
        ppStatement.setBlob(12, new ByteArrayInputStream(fingerPrint.getTemplate().getBytes()), fingerPrint.getTemplate().getBytes().length);
        ppStatement.setNull(13, Types.NULL);
        if(update) {
            ppStatement.setInt(14, fingerPrint.getPatienId());
            ppStatement.setString(15, fingerPrint.getFingerPositions().name());
        }
        
        ppStatement.executeUpdate();
        
    }

    private java.sql.Date getDate(Date dateCaptured) {
        if(dateCaptured == null) dateCaptured = new Date();
        ZoneId zoneId = ZoneId.of ( "Africa/Lagos" );
        ZonedDateTime zdt = ZonedDateTime.ofInstant ( dateCaptured.toInstant() , zoneId );
        LocalDate localDate = zdt.toLocalDate();
        return java.sql.Date.valueOf( localDate );
    }

    public void updatePatientTable(Integer patientId) throws Exception {
        String sql = "UPDATE patient SET date_changed = NOW() WHERE `patient_id` = ? ;";
        ppStatement = getConnection().prepareStatement(sql);
        ppStatement.setInt(1, patientId);
        ppStatement.executeUpdate();
        this.closeConnection();
    }

    public List<Patient> getPatientDetails(List<Integer> patientIds) throws Exception {
        String sql = "SELECT " +
                "    AA.PatientId," +
                "    AA.Name," +
                "    AA.Gender," +
                "    AA.BirthDate," +
                "    AA.Age," +
                "    BB.PepfarID," +
                "    BB.HospID," +
                "    AA.PhoneNumber " +
                "FROM" +
                "    (SELECT " +
                "        AAA.PatientId," +
                "            AAA.Name," +
                "            AAA.Gender," +
                "            AAA.BirthDate," +
                "            AAA.Age," +
                "            person_attribute.value AS PhoneNumber" +
                "    FROM" +
                "        (SELECT " +
                "        p.person_id AS PatientId," +
                "            CONCAT(pn.family_name, ' ', pn.given_name) AS Name," +
                "            IF(p.gender = 'M', 'MALE', 'FEMALE') AS Gender," +
                "            IF(p.birthdate_estimated IS NOT TRUE, p.birthdate, 'Estimated') AS BirthDate," +
                "            TIMESTAMPDIFF(YEAR, p.birthdate, CURDATE()) AS Age" +
                "    FROM" +
                "        person p, person_name pn" +
                "    WHERE" +
                "        p.person_id = pn.person_id AND pn.voided = false  AND p.voided = false " +
                "            AND p.person_id IN (" + String.join("", Collections.nCopies(patientIds.size()-1, "?,")) + "? )"+") AAA" +
                "    LEFT JOIN person_attribute ON AAA.PatientId = person_attribute.person_id" +
                "        AND person_attribute.person_attribute_type_id = 8 AND person_attribute.voided = false ) AA" +
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

        System.out.println(sql);

        ppStatement = getConnection().prepareStatement(sql);
        int i = 1;
        StringBuilder g = new StringBuilder();
        for(Integer patientId :patientIds ) {
            ppStatement.setInt(i, patientId);
            g.append(",").append(patientId);

            i++;
        }
        System.out.println(g);

        resultSet = ppStatement.executeQuery();
        List<Patient> patients = convertToPatientList(resultSet);
        this.closeConnection();
        return patients;
    }
    
    public ResponseModel SaveToDatabase(List<FingerPrintInfo> fingerPrintList, boolean update) throws Exception {
        
        ResponseModel responseModel = new ResponseModel();
        Connection connection = this.openConnection();
        
        if (fingerPrintList.isEmpty()) {
            responseModel.setIsSuccessful(false);
            responseModel.setErrorMessage("The request contains an empty list");
        }
        
        try {
            
            for (FingerPrintInfo a : fingerPrintList) {
                if(update){
                    Save(a, this.GetPatientBiometricInfo(a.getPatienId(), a.getFingerPositions().name(), connection) != null, connection);
                }else {
                    Save(a, false, connection);
                }

            }

            updatePatientTable(fingerPrintList.get(0).getPatienId());
            
            responseModel.setIsSuccessful(true);
            responseModel.setErrorMessage("Saved successfully");
            this.closeConnection();
        } catch (SQLException ex) {
            responseModel.setIsSuccessful(false);
            responseModel.setErrorMessage(ex.getMessage());
        }finally {
            this.closeConnection();
        }
        
        return responseModel;
    }

    public ResponseModel SaveMapToDatabase(Map<String, List<FingerPrintInfo>> fingerPrintMap, boolean update) throws Exception {

        ResponseModel responseModel = new ResponseModel();
        Connection connection = this.openConnection();

        if (fingerPrintMap.isEmpty()) {
            responseModel.setIsSuccessful(false);
            responseModel.setErrorMessage("The request contains an empty list");
        }

        try {
            for(String key : fingerPrintMap.keySet()) {
                for (FingerPrintInfo a : fingerPrintMap.get(key)) {
                    if (update) {
                        Save(a, this.GetPatientBiometricInfo(a.getPatienId(), a.getFingerPositions().name(), connection) != null, connection);
                    } else {
                        Save(a, false, connection);
                    }

                }
                updatePatientTable(Integer.parseInt(key));
            }


            responseModel.setIsSuccessful(true);
            responseModel.setErrorMessage("Saved successfully");
            this.closeConnection();
        } catch (SQLException ex) {
            responseModel.setIsSuccessful(false);
            responseModel.setErrorMessage(ex.getMessage());
        }finally {
            this.closeConnection();
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

        System.out.println("Person UUID inside Db Manager Class "+UUID);

        while (resultSet.next()) {
            
            nameAndPersonMap.put("name", resultSet.getString("patient_name"));
            nameAndPersonMap.put("person_id", resultSet.getString("person_id"));
            System.out.println("Person ID inside Db Manager Class "+resultSet.getString("person_id"));
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
            String value =resultSet.getString("property_value");
            this.closeConnection();
            return value;
        }
        this.closeConnection();
        return  null;
    }
    
    public ResponseModel saveRecapturedFingerprintVerificationToDatabase(List<FingerPrintInfo> fingerPrintList, boolean update) throws Exception {
        
        ResponseModel responseModel = new ResponseModel();
        Connection connection = this.openConnection();
        
        if (fingerPrintList.isEmpty()) {
            responseModel.setIsSuccessful(false);
            responseModel.setErrorMessage("The request contains an empty list");
        }
        
        try {
            
            for (FingerPrintInfo a : fingerPrintList) {
            	EncodingMetaModel enModel = new EncodingMetaModel();
            	enModel = ecProcess(a);

                if(this.GetPatientBiometricVerificationInfo(a.getPatienId(), a.getFingerPositions().name(), connection) != null){

                	saveVerification(a, enModel, true, connection);
                }else {
                	saveVerification(a, enModel, false, connection);
                }

            }

            updatePatientTable(fingerPrintList.get(0).getPatienId());
            
            responseModel.setIsSuccessful(true);
            responseModel.setErrorMessage("Fingerprint Re-capture saved Successfully");
            this.closeConnection();
        } catch (SQLException ex) {
            responseModel.setIsSuccessful(false);
            responseModel.setErrorMessage(ex.getMessage());
        }finally {
            this.closeConnection();
        }
        
        return responseModel;
    }

    private EncodingMetaModel ecProcess(FingerPrintInfo a) {
    	EncodingMetaModel en = new EncodingMetaModel();
    	BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    	//String sa = BCrypt.gensalt(12);
        String sa = "$2a$12$o7Q/zmGmyNUK7VdvtBSRzu";
    	String hashed = BCrypt.hashpw(a.getTemplate(), sa);
    	System.out.println("Hashed: " +hashed);
    	String encryptedTemp = bcrypt.encode(hashed);
        System.out.println("encryptedTemp: " +encryptedTemp);
    	en.setSalt(sa);
    	en.setHashed(hashed);
    	en.setEncodedTemplate(encryptedTemp);
    	
		return en;
	}

	public void saveVerification(FingerPrintInfo fingerPrint, EncodingMetaModel en, boolean update, Connection connection) throws Exception {
        String sql = "insert into " + BIOMETRICVERIFICATIONINFO + "(patient_Id, imageWidth, imageHeight, imageDPI,  " +
                "imageQuality, fingerPosition, serialNumber, model, manufacturer, creator, date_created, new_template, template, encoded_template, hashed)" +
                "Values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        if(update) {
            sql = "UPDATE " + BIOMETRICVERIFICATIONINFO + " SET " +
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
                    "date_created = ?, " +
                    "new_template = ?, " +
                    "template = ?, " +
                    "encoded_template = ?, " +
                    "hashed = ?, " +
                    "recapture_count = recapture_count + 1 " +
                    "WHERE patient_id = ? AND fingerPosition = ? ";
        }

        ppStatement = connection.prepareStatement(sql);
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
        ppStatement.setDate(11, getDate(fingerPrint.getDateCreated()));
        ppStatement.setBlob(12, new ByteArrayInputStream(fingerPrint.getTemplate().getBytes()), fingerPrint.getTemplate().getBytes().length);
        ppStatement.setNull(13, Types.NULL);  
        
      //  ppStatement.setString(14, en.getSalt());
        ppStatement.setString(14, en.getEncodedTemplate());
        ppStatement.setString(15, en.getHashed());
        
        if(update) {
            ppStatement.setInt(16, fingerPrint.getPatienId());
            ppStatement.setString(17, fingerPrint.getFingerPositions().name());
        }
        
        ppStatement.executeUpdate();
        
    }
    
    public FingerPrintInfo GetPatientBiometricVerificationInfo(int patientId, String fingerPosition, Connection connection) throws Exception {
        String sql = "SELECT patient_id, COALESCE(template, CONVERT(new_template USING utf8)) as template," +
                " imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, " +
                "manufacturer, date_created, creator FROM " + BIOMETRICVERIFICATIONINFO +" where patient_id = ? AND fingerPosition = ? ";

        ppStatement = connection.prepareStatement(sql);
        ppStatement.setString(1, String.valueOf(patientId));
        ppStatement.setString(2, fingerPosition);

        resultSet = ppStatement.executeQuery();
        List<FingerPrintInfo> fingerInfoList = converToFingerPrintList(resultSet);
        if (fingerInfoList.size() > 0) return fingerInfoList.get(0);
        return null;

    }
    
    
    
    
    
}
