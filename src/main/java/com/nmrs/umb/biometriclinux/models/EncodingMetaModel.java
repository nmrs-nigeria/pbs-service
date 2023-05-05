package com.nmrs.umb.biometriclinux.models;

public class EncodingMetaModel {
	
	private String salt;
	
	private String encodedTemplate;
	
	private String hashed;

	public EncodingMetaModel() {
	}
	
	public EncodingMetaModel(String salt, String encodedTemplate, String hashed) {
		super();
		this.salt = salt;
		this.encodedTemplate = encodedTemplate;
		this.hashed = hashed;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public String getEncodedTemplate() {
		return encodedTemplate;
	}

	public void setEncodedTemplate(String encodedTemplate) {
		this.encodedTemplate = encodedTemplate;
	}

	public String getHashed() {
		return hashed;
	}

	public void setHashed(String hashed) {
		this.hashed = hashed;
	}

}
