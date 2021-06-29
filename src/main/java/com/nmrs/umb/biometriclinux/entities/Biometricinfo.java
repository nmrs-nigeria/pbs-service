/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.entities;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author MORRISON.I
 */
@Entity
@Table(name = "biometricinfo")
@NamedQueries({
    @NamedQuery(name = "Biometricinfo.findAll", query = "SELECT b FROM Biometricinfo b")})
public class Biometricinfo implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "biometricInfo_Id")
    private Integer biometricInfoId;
    @Basic(optional = false)
    @Column(name = "patient_Id")
    private int patientId;
    @Lob
    @Column(name = "template")
    private String template;
    @Column(name = "imageWidth")
    private Integer imageWidth;
    @Column(name = "imageHeight")
    private Integer imageHeight;
    @Column(name = "imageDPI")
    private Integer imageDPI;
    @Column(name = "imageQuality")
    private Integer imageQuality;
    @Column(name = "fingerPosition")
    private String fingerPosition;
    @Column(name = "serialNumber")
    private String serialNumber;
    @Column(name = "model")
    private String model;
    @Column(name = "manufacturer")
    private String manufacturer;
    @Column(name = "creator")
    private Integer creator;
    @Column(name = "date_created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreated;
    @Lob
    @Column(name = "new_template")
    private byte[] newTemplate;

    public Biometricinfo() {
    }

    public Biometricinfo(Integer biometricInfoId) {
        this.biometricInfoId = biometricInfoId;
    }

    public Biometricinfo(Integer biometricInfoId, int patientId) {
        this.biometricInfoId = biometricInfoId;
        this.patientId = patientId;
    }

    public Integer getBiometricInfoId() {
        return biometricInfoId;
    }

    public void setBiometricInfoId(Integer biometricInfoId) {
        this.biometricInfoId = biometricInfoId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public Integer getImageDPI() {
        return imageDPI;
    }

    public void setImageDPI(Integer imageDPI) {
        this.imageDPI = imageDPI;
    }

    public Integer getImageQuality() {
        return imageQuality;
    }

    public void setImageQuality(Integer imageQuality) {
        this.imageQuality = imageQuality;
    }

    public String getFingerPosition() {
        return fingerPosition;
    }

    public void setFingerPosition(String fingerPosition) {
        this.fingerPosition = fingerPosition;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Integer getCreator() {
        return creator;
    }

    public void setCreator(Integer creator) {
        this.creator = creator;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public byte[] getNewTemplate() {
        return newTemplate;
    }

    public void setNewTemplate(byte[] newTemplate) {
        this.newTemplate = newTemplate;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (biometricInfoId != null ? biometricInfoId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Biometricinfo)) {
            return false;
        }
        Biometricinfo other = (Biometricinfo) object;
        if ((this.biometricInfoId == null && other.biometricInfoId != null) || (this.biometricInfoId != null && !this.biometricInfoId.equals(other.biometricInfoId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.example.demo.entities.Biometricinfo[ biometricInfoId=" + biometricInfoId + " ]";
    }
    
}
