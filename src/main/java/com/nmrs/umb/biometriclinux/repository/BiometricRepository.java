/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.repository;

import com.nmrs.umb.biometriclinux.entities.Biometricinfo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;

/**
 *
 * @author MORRISON.I
 */
@Repository
public interface BiometricRepository extends JpaRepository<Biometricinfo, Object>, QueryByExampleExecutor<Biometricinfo> {

    @Query("select u from Biometricinfo u where u.template IS NOT NULL AND u.newTemplate IS NULL ")
    List<Biometricinfo> GetOldBiometricinfoTemplate();
    

}
