/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.custom.changeset;

import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.entities.Biometricinfo;
import com.nmrs.umb.biometriclinux.main.BiometricLinuxApplication;
import com.nmrs.umb.biometriclinux.serviceimpl.BiometricInfoImpl;
import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.jvm.JdbcConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author MORRISON.I
 */
public class PBSTransfer implements CustomTaskChange {

    // BiometricInfoImpl biometricInfoImpl;
    DbManager db;

    JdbcConnection databaseConnection;
    private final String TABLENAME = "biometricinfo";
    private PreparedStatement ppStatement = null;

    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
    private ResourceAccessor resourceAccessor;

    @Override
    public void execute(Database dtbs) throws CustomChangeException {
        databaseConnection = (JdbcConnection) dtbs.getConnection();
        try {
            migrateTemplate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public String getConfirmationMessage() {
        return "pbs data transfer";
    }

    @Override
    public void setUp() throws SetupException {
//        ApplicationContext ctx = new AnnotationConfigApplicationContext(BiometricLinuxApplication.class);
//        db = ctx.getBean(DbManager.class);
//        ApplicationContext ctx = new AnnotationConfigApplicationContext(BiometricLinuxApplication.class);
//        biometricInfoImpl = ctx.getBean(BiometricInfoImpl.class);

    }

    @Override
    public void setFileOpener(ResourceAccessor ra) {
        this.resourceAccessor = ra;
    }

    @Override
    public ValidationErrors validate(Database dtbs) {
        return new ValidationErrors();
    }

    public void migrateTemplate() throws Exception {
        List<Biometricinfo> pullOldTemplate = pullOldTemplate();
        pullOldTemplate.stream()
                .forEach(a -> {
                    try {
                        updateOldTemplateInfo(a);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                      //  logger.fatal(Level.SEVERE, ex);
                    }
                });
    }

    private List<Biometricinfo> pullOldTemplate() throws Exception {
        String sql = "SELECT biometricInfo_Id, patient_id, template, CONVERT(new_template USING utf8) as new_template,"
                + " imageWidth, imageHeight, imageDPI,  imageQuality, fingerPosition, serialNumber, model, "
                + "manufacturer, date_created, creator FROM " + TABLENAME + " where "
                + "template IS NOT NULL AND new_template IS NULL ";

        ppStatement = databaseConnection.prepareStatement(sql);
        ResultSet resultSet = ppStatement.executeQuery();

        return converToBiometricInfoList(resultSet);

    }

    private int updateOldTemplateInfo(Biometricinfo biometricinfo) throws Exception {
        String sql = "update " + TABLENAME + " set new_template = ? where biometricInfo_Id = ?";
        ppStatement = databaseConnection.prepareStatement(sql);
        ppStatement.setBlob(1, new ByteArrayInputStream(biometricinfo.getTemplate().getBytes()), biometricinfo.getTemplate().getBytes().length);
        ppStatement.setInt(2, biometricinfo.getBiometricInfoId());

        return ppStatement.executeUpdate();

    }

    private List<Biometricinfo> converToBiometricInfoList(ResultSet resultSet) throws SQLException {

        List<Biometricinfo> biometricInfoList = new ArrayList<>();

        while (resultSet.next()) {
            Biometricinfo templatesInfo = getBiometricInfo(resultSet);
            biometricInfoList.add(templatesInfo);
        }
        return biometricInfoList;

    }
    
    
      private Biometricinfo getBiometricInfo(ResultSet resultSet) throws SQLException {
        Biometricinfo biometricInfo = new Biometricinfo();
        biometricInfo.setCreator(resultSet.getInt("creator"));//default for NMRS
        biometricInfo.setDateCreated(resultSet.getDate("date_created"));
        biometricInfo.setPatientId(resultSet.getInt("patient_id"));
        biometricInfo.setImageWidth(resultSet.getInt("imageWidth"));
        biometricInfo.setImageHeight(resultSet.getInt("imageHeight"));
        biometricInfo.setImageDPI(resultSet.getInt("imageDPI"));
        biometricInfo.setImageQuality(resultSet.getInt("imageQuality"));

        biometricInfo.setFingerPosition(resultSet.getString("fingerPosition"));
        biometricInfo.setSerialNumber(resultSet.getString("serialNumber"));
        biometricInfo.setModel(resultSet.getString("model"));
        biometricInfo.setManufacturer(resultSet.getString("manufacturer"));
        biometricInfo.setTemplate(resultSet.getString("template"));
        biometricInfo.setNewTemplate(resultSet.getBytes("new_template"));
        biometricInfo.setBiometricInfoId(resultSet.getInt("biometricInfo_Id"));

        return biometricInfo;
    }

}
