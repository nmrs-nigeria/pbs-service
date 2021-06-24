/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.custom.changeset;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;


import liquibase.change.custom.CustomTaskChange;

/**
 *
 * @author MORRISON.I
 */
public class PBSTransfer implements CustomTaskChange {

    
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
    private ResourceAccessor resourceAccessor;
    
    @Override
    public void execute(Database dtbs) throws CustomChangeException {
        System.out.println("RAN THE UPDATES");
    }

    @Override
    public String getConfirmationMessage() {
        return "pbs data transfer";
    }

    @Override
    public void setUp() throws SetupException {
      ;
    }

    @Override
    public void setFileOpener(ResourceAccessor ra) {
        this.resourceAccessor = ra;
    }

    @Override
    public ValidationErrors validate(Database dtbs) {
        return new ValidationErrors();
    }
    
}
