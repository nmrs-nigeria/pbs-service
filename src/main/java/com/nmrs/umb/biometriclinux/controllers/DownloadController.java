package com.nmrs.umb.biometriclinux.controllers;

import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.security.FileEncrypterDecrypter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.FileEncodingApplicationListener;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

@Controller
public class DownloadController {

	@Autowired
	DbManager dbManager;

	@GetMapping("/view")
	public String greeting() {
		return "download";
	}

	@GetMapping("/download")
	public ResponseEntity<Object> getFile(@RequestParam(value="path") String path) {
		String filename = null;
		ByteArrayInputStream byteArrayInputStream = null;
		try {
			String datimCode = dbManager.getGlobalProperty("facility_datim_code");
			if (path != null && path.equalsIgnoreCase("invalid")) {
				filename = "Patients_with_invalid_fingerprint_data.csv";
				Set<Integer> invalids = dbManager.getPatientsWithInvalidData();
				 if (invalids.size() > 0) byteArrayInputStream = dbManager.getCsvFilePath(invalids, datimCode);
			} else if (path != null && path.equalsIgnoreCase("lowQuality")) {
				filename = "Patients_with_low_quality_fingerprint_data.csv";
				Set<Integer> lowQuality = dbManager.getPatientsWithLowQualityData();
				if (lowQuality.size() > 0)  byteArrayInputStream = dbManager.getCsvFilePath(lowQuality, datimCode);
			} else if (path != null && path.equalsIgnoreCase("both")) {
				String facilityName = dbManager.getGlobalProperty("Facility_Name");
				filename = datimCode+"_"+facilityName+"_patients_fingerprint_data.csv";
				Set<Integer> invalids = dbManager.getPatientsWithInvalidData();
				Set<Integer> lowQuality = dbManager.getPatientsWithLowQualityData();
				Set<Integer> none = dbManager.getPatientsWithoutFingerPrintData();
				invalids.addAll(lowQuality);
				invalids.addAll(none);
				if (invalids.size() > 0)  byteArrayInputStream = dbManager.getCsvFilePath(invalids, datimCode);
			}
			if(byteArrayInputStream != null) {
				FileEncrypterDecrypter fileEncrypterDecrypter = new FileEncrypterDecrypter(KeyGenerator.getInstance("AES").generateKey(), "AES/CBC/PKCS5Padding");
				CipherInputStream cipherInputStream = fileEncrypterDecrypter.encrypt(byteArrayInputStream);
				InputStreamResource file = new InputStreamResource(cipherInputStream);

				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
						.contentType(MediaType.parseMediaType("application/csv"))
						.body(file);
			}
		}catch (Exception e){
			System.out.println(e.getMessage());
			return  ResponseEntity.ok()
					.body(e.getMessage());
		}finally {
			try {
				if(byteArrayInputStream != null) byteArrayInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return  ResponseEntity.ok()
				.body("No patient Data");
	}

}
