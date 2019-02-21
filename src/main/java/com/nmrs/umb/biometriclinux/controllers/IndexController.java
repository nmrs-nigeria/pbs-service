/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.controllers;

import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.main.AppUtil;
import com.nmrs.umb.biometriclinux.models.DbModel;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
@RestController
public class IndexController {

    @Autowired
    private Environment env;

    @RequestMapping(value = "/")
    public String status() {
        return "Biometric service is running";
    }

    @RequestMapping(value = "/server")
    public DbModel getDatabaseDetails() {

        DbModel dbModel = new DbModel();
        dbModel.setDatabaseServer(env.getProperty("app.server"));
        dbModel.setPassword(env.getProperty("app.password"));
        dbModel.setUsername(env.getProperty("app.username"));
        dbModel.setdBName(env.getProperty("app.dbname"));
        dbModel.setPort(env.getProperty("server.port"));
        dbModel.setDbPort(env.getProperty("app.dbport"));

        return dbModel;
    }

    @RequestMapping(value = "/retrieve")
    //this is for testing purpose
    public String getPatientID(@RequestParam String uid) {

        try {
//            Map<String, String> res = db.RetrievePatientIdAndNameByUUID(uid);
//            if(res.isEmpty()){
//            return null;
//            }
//            return res.get("person_id");

            DbModel dbModel = AppUtil.getDatabaseSource(env);

            DbManager db = new DbManager(dbModel);

            db.openConnection();
            Map<String, String> res = db.RetrievePatientIdAndNameByUUID(uid);
            if (res.isEmpty()) {
                return null;
            }
            return res.get("person_id");
        } catch (SQLException | ClassNotFoundException ex) {
            Logger.getLogger(IndexController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
