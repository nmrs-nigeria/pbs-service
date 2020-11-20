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

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
    @Autowired
    AppUtil appUtil;



//    @Autowired
//    BuildProperties buildProperties;

    @RequestMapping(value = "/")
    public String status() {
        return "Biometric service is running";
    }

    @RequestMapping(value = "/server")
    public DbModel getDatabaseDetails() {

        MavenXpp3Reader reader = new MavenXpp3Reader();
        DbModel dbModel = new DbModel();
//        Model model;
//        try {
//            if ((new File("pom.xml")).exists()) {
//                model = reader.read(new FileReader("pom.xml"));
//            } else {
//                model = reader.read(
//                        new InputStreamReader(
//                                Application.class.getResourceAsStream(
//                                        "/META-INF/maven/com.nmrs.umb/biometric-linux/pom.xml"
//                                )
//                        )
//                );
//            }
//
//            dbModel.setAppVersion(model.getVersion());
//
//        } catch (Exception ex) {
//            Logger.getLogger(IndexController.class.getName()).log(Level.SEVERE, null, ex);
//        }

        dbModel.setDatabaseServer(env.getProperty("app.server"));
        dbModel.setPassword(env.getProperty("app.password"));
        dbModel.setUsername(env.getProperty("app.username"));
        dbModel.setdBName(env.getProperty("app.dbname"));
        dbModel.setPort(env.getProperty("server.port"));
        dbModel.setDbPort(env.getProperty("app.dbport"));
//       dbModel.setAppVersion(buildProperties.getVersion());

        return dbModel;
    }

    @RequestMapping(value = "/retrieve")
    //this is for testing purpose
    public String getPatientID(@RequestParam String uid) {

        try {

            DbManager db = new DbManager();

            db.openConnection();
            Map<String, String> res = db.RetrievePatientIdAndNameByUUID(uid);
            if (res.isEmpty()) {
                return null;
            }
            return res.get("person_id");
        } catch (Exception ex) {
            Logger.getLogger(IndexController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
