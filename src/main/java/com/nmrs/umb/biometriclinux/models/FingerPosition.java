/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.models;

/**
 *
 * @author MORRISON.I
 */
public class FingerPosition {
    private Integer quality;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer imageDpi;
    private String template;

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
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

    public Integer getImageDpi() {
        return imageDpi;
    }

    public void setImageDpi(Integer imageDpi) {
        this.imageDpi = imageDpi;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }
    
    
    
    
}
