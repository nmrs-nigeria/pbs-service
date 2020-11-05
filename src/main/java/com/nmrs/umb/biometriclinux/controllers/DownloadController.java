package com.nmrs.umb.biometriclinux.controllers;

import com.nmrs.umb.biometriclinux.dal.DbManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
			if (path != null && path.equalsIgnoreCase("invalid")) {
				filename = "Patients_with_invalid_fingerprint_data.csv";
				Set<Integer> invalids = dbManager.getPatientsWithInvalidData();
				byteArrayInputStream = dbManager.getCsvFilePath(invalids);
			} else if (path != null && path.equalsIgnoreCase("lowQuality")) {
				filename = "Patients_with_low_quality_fingerprint_data.csv";
				Set<Integer> lowQuality = dbManager.getPatientsWithLowQualityData();
				byteArrayInputStream = dbManager.getCsvFilePath(lowQuality);
			}
			if(byteArrayInputStream != null) {
				InputStreamResource file = new InputStreamResource(byteArrayInputStream);

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
		return null;
	}

}
