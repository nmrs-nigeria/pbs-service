/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.serviceimpl;


import com.nmrs.umb.biometriclinux.entities.Biometricinfo;
import com.nmrs.umb.biometriclinux.repository.BiometricRepository;
import com.nmrs.umb.biometriclinux.service.IBiometricInfo;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 *
 * @author MORRISON.I
 */

@Component
public class BiometricInfoImpl implements IBiometricInfo{
    
    BiometricRepository biometricRepository;
    
    public BiometricInfoImpl(BiometricRepository _biometricRepository){
    this.biometricRepository = _biometricRepository;
    }

    @Override
    public void migrateOldTemplateRecord() {
        
        List<Biometricinfo> oldTemplates = biometricRepository.GetOldBiometricinfoTemplate();
        oldTemplates.stream()
                .forEach(a -> updateTemplate(a));
        
    }
    
    
    private void updateTemplate(Biometricinfo b){
        b.setNewTemplate(b.getTemplate().getBytes());
        b.setTemplate(null);
        biometricRepository.save(b);
    }
    
}
