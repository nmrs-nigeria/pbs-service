/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.models;

import java.util.Date;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
public class FingerPrintInfo {

    public String Manufacturer;
    public String Model;
    public String SerialNumber;
    public int ImageWidth;
    public int ImageHeight;
    public int ImageDPI;
    public int ImageQuality;
    public String Image;
    public byte[] ImageByte;
    public String Template;
    public AppModel.FingerPositions FingerPositions;
    public int PatienId;
    public Date DateCreated;
    public int Creator;
    public String qualityFlag;

    public String ErrorMessage;

    public String getManufacturer() {
        return Manufacturer;
    }

    public void setManufacturer(String Manufacturer) {
        this.Manufacturer = Manufacturer;
    }

    public String getModel() {
        return Model;
    }

    public void setModel(String Model) {
        this.Model = Model;
    }

    public String getSerialNumber() {
        return SerialNumber;
    }

    public void setSerialNumber(String SerialNumber) {
        this.SerialNumber = SerialNumber;
    }

    public int getImageWidth() {
        return ImageWidth;
    }

    public void setImageWidth(int ImageWidth) {
        this.ImageWidth = ImageWidth;
    }

    public int getImageHeight() {
        return ImageHeight;
    }

    public void setImageHeight(int ImageHeight) {
        this.ImageHeight = ImageHeight;
    }

    public int getImageDPI() {
        return ImageDPI;
    }

    public void setImageDPI(int ImageDPI) {
        this.ImageDPI = ImageDPI;
    }

    public int getImageQuality() {
        return ImageQuality;
    }

    public void setImageQuality(int ImageQuality) {
        this.ImageQuality = ImageQuality;
    }

    public String getImage() {
        return Image;
    }

    public void setImage(String Image) {
        this.Image = Image;
    }

    public byte[] getImageByte() {
        return ImageByte;
    }

    public void setImageByte(byte[] ImageByte) {
        this.ImageByte = ImageByte;
    }

    public String getTemplate() {
        return Template;
    }

    public void setTemplate(String Template) {
        this.Template = Template;
    }

    public AppModel.FingerPositions getFingerPositions() {
        return FingerPositions;
    }

    public void setFingerPositions(AppModel.FingerPositions FingerPositions) {
        this.FingerPositions = FingerPositions;
    }

    public int getPatienId() {
        return PatienId;
    }

    public void setPatienId(int PatienId) {
        this.PatienId = PatienId;
    }

    public Date getDateCreated() {
        return DateCreated;
    }

    public void setDateCreated(Date DateCreated) {
        this.DateCreated = DateCreated;
    }

    public int getCreator() {
        return Creator;
    }

    public void setCreator(int Creator) {
        this.Creator = Creator;
    }

    public String getErrorMessage() {
        return ErrorMessage;
    }

    public void setErrorMessage(String ErrorMessage) {
        this.ErrorMessage = ErrorMessage;
    }

    public String getQualityFlag() {
        return qualityFlag;
    }

    public void setQualityFlag(String qualityFlag) {
        this.qualityFlag = qualityFlag;
    }

}
