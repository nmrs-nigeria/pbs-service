/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

/**
 *
 * @author Morrison Idiasirue <morrison.idiasirue@gmail.com>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FingerPrintInfo {

    public String Manufacturer;
    public String Model;
    public String SerialNumber;
    public Integer ImageWidth;
    public Integer ImageHeight;
    public Integer ImageDPI;
    public Integer ImageQuality;
    public String Image;
    public byte[] ImageByte;
    public String Template;
    public AppModel.FingerPositions FingerPositions;
    public Integer PatienId;
    public Date DateCreated;
    public Integer Creator;
    public String qualityFlag;
    public String ErrorCode;
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

    public Integer getImageWidth() {
        return ImageWidth;
    }

    public void setImageWidth(Integer ImageWidth) {
        this.ImageWidth = ImageWidth;
    }

    public Integer getImageHeight() {
        return ImageHeight;
    }

    public void setImageHeight(Integer ImageHeight) {
        this.ImageHeight = ImageHeight;
    }

    public Integer getImageDPI() {
        return ImageDPI;
    }

    public void setImageDPI(Integer ImageDPI) {
        this.ImageDPI = ImageDPI;
    }

    public Integer getImageQuality() {
        return ImageQuality;
    }

    public void setImageQuality(Integer ImageQuality) {
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

    public Integer getPatienId() {
        return PatienId;
    }

    public void setPatienId(Integer PatienId) {
        this.PatienId = PatienId;
    }

    public Date getDateCreated() {
        return DateCreated;
    }

    public void setDateCreated(Date DateCreated) {
        this.DateCreated = DateCreated;
    }

    public Integer getCreator() {
        return Creator;
    }

    public void setCreator(Integer Creator) {
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

    public String getErrorCode() {
        return ErrorCode;
    }

    public void setErrorCode(String errorCode) {
        ErrorCode = errorCode;
    }
}
