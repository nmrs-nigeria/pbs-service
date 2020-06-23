/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.models;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
public class DbModel {
    
    private String databaseServer;
    private String username;
    private String password;
    private String port;
    private String dBName;
    private String dbPort;
    private String appVersion;
    
    

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    
    public String getDatabaseServer() {
        return databaseServer;
    }

    public void setDatabaseServer(String databaseServer) {
        this.databaseServer = databaseServer;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getdBName() {
        return dBName;
    }

    public void setdBName(String dBName) {
        this.dBName = dBName;
    }

    public String getDbPort() {
        return dbPort;
    }

    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
    }
    
    
    
}
