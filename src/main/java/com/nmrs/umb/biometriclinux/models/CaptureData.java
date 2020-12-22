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
public class CaptureData {

    private FingerPosition left_index;
    private FingerPosition left_middle;
    private FingerPosition left_small;
    private FingerPosition left_thumb;
    private FingerPosition left_wedding;
    private FingerPosition right_wedding;
    private FingerPosition right_thumb;
    private FingerPosition right_small;
    private FingerPosition right_index;
    private FingerPosition right_middle;


    public CaptureData(){}
    public CaptureData(FingerPosition left_index, FingerPosition left_middle, FingerPosition left_small, FingerPosition left_thumb, FingerPosition left_wedding, FingerPosition right_wedding, FingerPosition right_thumb, FingerPosition right_small, FingerPosition right_index, FingerPosition right_middle) {
        this.left_index = left_index;
        this.left_middle = left_middle;
        this.left_small = left_small;
        this.left_thumb = left_thumb;
        this.left_wedding = left_wedding;
        this.right_wedding = right_wedding;
        this.right_thumb = right_thumb;
        this.right_small = right_small;
        this.right_index = right_index;
        this.right_middle = right_middle;
    }

    public FingerPosition getLeft_index() {
        return left_index;
    }

    public void setLeft_index(FingerPosition left_index) {
        this.left_index = left_index;
    }

    public FingerPosition getLeft_middle() {
        return left_middle;
    }

    public void setLeft_middle(FingerPosition left_middle) {
        this.left_middle = left_middle;
    }

    public FingerPosition getLeft_small() {
        return left_small;
    }

    public void setLeft_small(FingerPosition left_small) {
        this.left_small = left_small;
    }

    public FingerPosition getLeft_thumb() {
        return left_thumb;
    }

    public void setLeft_thumb(FingerPosition left_thumb) {
        this.left_thumb = left_thumb;
    }

    public FingerPosition getLeft_wedding() {
        return left_wedding;
    }

    public void setLeft_wedding(FingerPosition left_wedding) {
        this.left_wedding = left_wedding;
    }

    public FingerPosition getRight_wedding() {
        return right_wedding;
    }

    public void setRight_wedding(FingerPosition right_wedding) {
        this.right_wedding = right_wedding;
    }

    public FingerPosition getRight_thumb() {
        return right_thumb;
    }

    public void setRight_thumb(FingerPosition right_thumb) {
        this.right_thumb = right_thumb;
    }

    public FingerPosition getRight_small() {
        return right_small;
    }

    public void setRight_small(FingerPosition right_small) {
        this.right_small = right_small;
    }

    public FingerPosition getRight_index() {
        return right_index;
    }

    public void setRight_index(FingerPosition right_index) {
        this.right_index = right_index;
    }

    public FingerPosition getRight_middle() {
        return right_middle;
    }

    public void setRight_middle(FingerPosition right_middle) {
        this.right_middle = right_middle;
    }
    
    
    

}
