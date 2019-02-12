/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.dal;

import com.nmrs.umb.biometriclinux.models.AppModel;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.ResponseModel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
public class DbManager {

    private Connection conn = null;
    private Statement statement = null;
    private PreparedStatement ppStatement = null;
    private ResultSet resultSet = null;
    private String server = null;
    private String dbUsername = null;
    private String dbPassword = null;
    private String dbName = null;
    private final String TABLENAME = "biometricInfo";

    @Autowired
    private Environment env;

    public DbManager() {
        server = env.getProperty("app.server");
        dbUsername = env.getProperty("app.username");
        dbPassword = env.getProperty("app.password");
        dbName = env.getProperty("app.dbname");

    }

    private void openConnection() throws ClassNotFoundException, SQLException {

        Class.forName("com.mysql.jdbc.Driver");

        conn = DriverManager
                .getConnection(String.format("jdbc:mysql://%s/%s?user=%s&password=%s", server, dbName, dbUsername, dbPassword));

    }

    private void createFingerPrintTable() throws ClassNotFoundException, SQLException {
        DbManager dbManager = new DbManager();
        dbManager.openConnection();
        statement = conn.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS `biometricInfo` (`"
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
        patientId = 0;

        if (patientId != 0) {
            ppStatement = conn.prepareStatement("SELECT patient_id, template, imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, manufacturer, date_created, creator FROM biometricInfo where patient_id = ? ");
            ppStatement.setInt(1, patientId);
            resultSet = ppStatement.executeQuery();

        } else {
            ////TODO
        }

        return converToFingerPrintList(resultSet);

    }

    private List<FingerPrintInfo> converToFingerPrintList(ResultSet resultSet) throws SQLException {

        List<FingerPrintInfo> fingerInfoList = new ArrayList<>();

        while (resultSet.next()) {

            FingerPrintInfo fingerPrintInfo = new FingerPrintInfo();
            fingerPrintInfo.setCreator(resultSet.getInt("creator"));
            fingerPrintInfo.setDateCreated(resultSet.getDate("date_created"));
            fingerPrintInfo.setPatienId(resultSet.getInt("patient_id"));
            fingerPrintInfo.setTemplate(resultSet.getString("template"));
            fingerPrintInfo.setImageWidth(resultSet.getInt("imageWidth"));
            fingerPrintInfo.setImageHeight(resultSet.getInt("imageHeight"));
            fingerPrintInfo.setImageDPI(resultSet.getInt("imageDPI"));
            fingerPrintInfo.setImageQuality(resultSet.getInt("imageQuality"));
            fingerPrintInfo.setFingerPositions(AppModel.FingerPositions.values()[resultSet.getInt("fingerPosition")]);
            fingerPrintInfo.setSerialNumber(resultSet.getString("serialNumber"));
            fingerPrintInfo.setModel(resultSet.getString("model"));
            fingerPrintInfo.setManufacturer(resultSet.getString("manufacturer"));

            fingerInfoList.add(fingerPrintInfo);

        }
        return fingerInfoList;

    }

    private void closeConnection() throws SQLException {
        conn.close();
        statement.close();
        ppStatement.close();
    }

    public void Save(FingerPrintInfo fingerPrint) throws SQLException {
        //String insertSQL = String.format();
        // insertSQL += String.format(fingerPrint.FingerPositions, fingerPrint.SerialNumber, fingerPrint.Model, fingerPrint.Manufacturer, fingerPrint.Creator);
        ppStatement = conn.prepareStatement("insert into biometricInfo(patient_Id, template, imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, manufacturer, creator, date_created)Values(?,?,?,?,?,?,?,?,?,?,?,NOW())");
        ppStatement.setInt(1, fingerPrint.PatienId);
        ppStatement.setString(2, fingerPrint.Template);
        ppStatement.setInt(3, fingerPrint.ImageWidth);
        ppStatement.setInt(4, fingerPrint.ImageHeight);
        ppStatement.setInt(5, fingerPrint.ImageDPI);
        ppStatement.setInt(6, fingerPrint.ImageQuality);
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

    public Map<String, String> RetrievePatientNameByUniqueId(String patientuniqueId) throws SQLException {

        ppStatement = conn.prepareStatement("SELECT CONCAT(given_name,' ',family_name) AS patient_name, pid.patient_id FROM person_name pn "
                + "INNER JOIN patient_identifier pid ON pn.person_id=pid.patient_id "
                + "WHERE pid.identifier_type=4 AND pid.identifier= ?;");
        ppStatement.setString(1, patientuniqueId);
        resultSet = ppStatement.executeQuery();
        Map<String, String> nameAndPatientId = new HashMap<>();
        while (resultSet.next()) {

            nameAndPatientId.put("name", resultSet.getString("patient_name"));
            nameAndPatientId.put("patientId", resultSet.getString("patient_id"));
            break;
        }

        return nameAndPatientId;

    }

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

    public Map<String, String> RetrievePatientIdByUUID(String UUID) throws SQLException {

        ppStatement = conn.prepareStatement("SELECT CONCAT(given_name,' ',family_name) AS patient_name, p.person_id "
                + "FROM person_name pn INNER JOIN person p ON pn.person_id = p.person_id "
                + "WHERE p.UUID = ?;");

        ppStatement.setString(1, UUID);
        resultSet = ppStatement.executeQuery();
        Map<String, String> nameAndPersonId = new HashMap<>();
        while (resultSet.next()) {

            nameAndPersonId.put("name", resultSet.getString("patient_name"));
            nameAndPersonId.put("person_id", resultSet.getString("person_id"));
            break;
        }

        return nameAndPersonId;

    }

}
