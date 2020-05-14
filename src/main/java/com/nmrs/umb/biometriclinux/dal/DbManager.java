/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.dal;

import com.nmrs.umb.biometriclinux.models.AppModel;
import com.nmrs.umb.biometriclinux.models.DbModel;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.ResponseModel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
@Configuration
public class DbManager {

    private Connection conn = null;
    private Statement statement = null;
    private PreparedStatement ppStatement = null;
    private ResultSet resultSet = null;
    private String server = null;
    private String dbUsername = null;
    private String dbPassword = null;
    private String dbName = null;
    private String dbPort = null;
    private final String TABLENAME = "biometricinfo";

    public DbManager(DbModel dbModel) {
        this.server = dbModel.getDatabaseServer();
        this.dbName = dbModel.getdBName();
        this.dbUsername = dbModel.getUsername();
        this.dbPassword = dbModel.getPassword();
        this.dbPort = dbModel.getDbPort();
    }

    public DbManager() {
    }

    public void openConnection() throws ClassNotFoundException, SQLException {

        // String serverUrl = "jdbc:mysql://localhost:3306/openmrs?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        String serverUrl = MessageFormat.format("jdbc:mysql://{0}:{1}/{2}?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", server, dbPort, dbName);

        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager
                .getConnection(serverUrl, dbUsername, dbPassword);
    }

    private void createFingerPrintTable() throws ClassNotFoundException, SQLException {
        //  DbManager dbManager = new DbManager();
        // openConnection();
        statement = conn.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS `biometricinfo` (`"
                + "biometricInfo_Id` INT(11) NOT NULL AUTO_INCREMENT,"
                + "`patient_Id` INT(11) NOT NULL,`template` TEXT NOT NULL,"
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

    public List<FingerPrintInfo> GetPatientBiometricinfo(int patientId) throws SQLException {

        if (patientId != 0) {
            ppStatement = conn.prepareStatement("SELECT patient_id, template, imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, manufacturer, date_created, creator FROM " + TABLENAME + " where patient_id = ? ");
            ppStatement.setInt(1, patientId);
            resultSet = ppStatement.executeQuery();

        } else {
            ppStatement = conn.prepareStatement("SELECT patient_id, template, imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, manufacturer, date_created, creator FROM " + TABLENAME);
            resultSet = ppStatement.executeQuery();
        }

        return converToFingerPrintList(resultSet);

    }

    private List<FingerPrintInfo> converToFingerPrintList(ResultSet resultSet) throws SQLException {

        List<FingerPrintInfo> fingerInfoList = new ArrayList<>();

        while (resultSet.next()) {

            FingerPrintInfo fingerPrintInfo = new FingerPrintInfo();
            fingerPrintInfo.setCreator(0);//default for NMRS
            fingerPrintInfo.setDateCreated(resultSet.getDate("date_created"));
            fingerPrintInfo.setPatienId(resultSet.getInt("patient_id"));
            fingerPrintInfo.setTemplate(resultSet.getString("template"));
            fingerPrintInfo.setImageWidth(resultSet.getInt("imageWidth"));
            fingerPrintInfo.setImageHeight(resultSet.getInt("imageHeight"));
            fingerPrintInfo.setImageDPI(resultSet.getInt("imageDPI"));
            fingerPrintInfo.setImageQuality(resultSet.getInt("imageQuality"));
            fingerPrintInfo.setFingerPositions(AppModel.FingerPositions.valueOf(resultSet.getString("fingerPosition")));
            fingerPrintInfo.setSerialNumber(resultSet.getString("serialNumber"));
            fingerPrintInfo.setModel(resultSet.getString("model"));
            fingerPrintInfo.setManufacturer(resultSet.getString("manufacturer"));

            fingerInfoList.add(fingerPrintInfo);
        }
        return fingerInfoList;

    }

    public void closeConnection() throws SQLException {
        if (Objects.nonNull(conn)) {
            conn.close();
            // statement.close();        
        }
        if (Objects.nonNull(ppStatement)) {
            ppStatement.close();
        }

    }

    public void Save(FingerPrintInfo fingerPrint) throws SQLException {
        //String insertSQL = String.format();
        // insertSQL += String.format(fingerPrint.FingerPositions, fingerPrint.SerialNumber, fingerPrint.Model, fingerPrint.Manufacturer, fingerPrint.Creator);
        ppStatement = conn.prepareStatement("insert into " + TABLENAME + "(patient_Id, template, imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, manufacturer, creator, date_created)Values(?,?,?,?,?,?,?,?,?,?,?,NOW())");
        ppStatement.setInt(1, fingerPrint.getPatienId());
        ppStatement.setString(2, fingerPrint.getTemplate());
        ppStatement.setInt(3, fingerPrint.getImageWidth());
        ppStatement.setInt(4, fingerPrint.getImageHeight());
        ppStatement.setInt(5, fingerPrint.getImageDPI());
        ppStatement.setInt(6, fingerPrint.getImageQuality());
        ppStatement.setString(7, fingerPrint.getFingerPositions().name());
        ppStatement.setString(8, fingerPrint.getSerialNumber());
        ppStatement.setString(9, fingerPrint.getModel());
        ppStatement.setString(10, fingerPrint.getManufacturer());
        ppStatement.setInt(11, fingerPrint.getCreator());

        ppStatement.executeUpdate();

    }

    public ResponseModel SaveToDatabase(List<FingerPrintInfo> fingerPrintList) throws SQLException {

        ResponseModel responseModel = new ResponseModel();

        if (fingerPrintList.isEmpty()) {
            responseModel.setIsSuccessful(false);
            responseModel.setErrorMessage("The request contains an empty list");
        }

        try {

            for (FingerPrintInfo a : fingerPrintList) {
                Save(a);
            }

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
    public Map<String, String> RetrievePatientNameByPatientId(int patientId) throws SQLException {

        ppStatement = conn.prepareStatement("SELECT CONCAT(given_name,' ',family_name) AS patient_name, pid.identifier FROM person_name pn "
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

    public String RetrievePatientNameByPersonId(int personId) throws SQLException {

        ppStatement = conn.prepareStatement("SELECT CONCAT(given_name,' ',family_name) AS patient_name FROM person_name pn "
                + "WHERE person_id = ?;");

        ppStatement.setInt(1, personId);
        resultSet = ppStatement.executeQuery();
        String foundName = "";
        while (resultSet.next()) {

            foundName = resultSet.getString("patient_name");
        }

        return foundName;

    }

    public Map<String, String> RetrievePatientIdAndNameByUUID(String UUID) throws SQLException, ClassNotFoundException {
        //openConnection();
        ppStatement = conn.prepareStatement("SELECT CONCAT(given_name,' ',family_name) AS patient_name, p.person_id "
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

    public int deletePatientBiometricInfo(String patientUid) throws SQLException, ClassNotFoundException {
        // Map<String,String> nameAndPersonMap =  RetrievePatientIdAndNameByUUID(patientUid);
        ppStatement = conn.prepareStatement("DELETE  FROM `biometricinfo` WHERE patient_id in (select p.person_id from person p where p.uuid = ? )");
        ppStatement.setString(1, patientUid);

        return ppStatement.executeUpdate();
    }

}
