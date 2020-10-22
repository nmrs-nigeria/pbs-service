/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.main;

import com.nmrs.umb.biometriclinux.models.DbModel;
import org.springframework.core.env.Environment;

/**
 *
 * @author MORRISON.I
 */
public class AppUtil {

    public static DbModel getDatabaseSource(Environment env) {
        DbModel dbModel = new DbModel();
        dbModel.setDatabaseServer(env.getProperty("app.server"));
        dbModel.setPassword(env.getProperty("app.password"));
        dbModel.setUsername(env.getProperty("app.username"));
        dbModel.setdBName(env.getProperty("app.dbname"));
        dbModel.setPort(env.getProperty("server.port"));
        dbModel.setDbPort(env.getProperty("app.dbport"));

        return dbModel;
    }

    public static final int QUALITY_THRESHOLD = 60;
    public static final String LOW_QUALITY_FLAG = "low";
    public static final String VALID_QUALITY_FLAG = "normal";

}
