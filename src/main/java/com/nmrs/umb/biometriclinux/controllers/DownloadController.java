package com.nmrs.umb.biometriclinux.controllers;

import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
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
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DownloadController {

    private static String PBS_UPLOAD_FOLDER = "C://pbs_upload/";
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
		return  ResponseEntity.ok()
				.body("No patient Data");
	}

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes){
        
         if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:uploadStatus";
        }
         
         try{
             
          byte[] bytes = file.getBytes();
            Path path = Paths.get(PBS_UPLOAD_FOLDER + file.getOriginalFilename());
            Files.write(path, bytes);
            
            Reader reader = Files.newBufferedReader(path);
             CsvToBean<FingerPrintInfo> csvToBean = new CsvToBeanBuilder(reader)
                     .withType(FingerPrintInfo.class)
                     .withIgnoreLeadingWhiteSpace(true)
                     .build();
             
             List<FingerPrintInfo> templateList = csvToBean.parse();
             dbManager.SaveToDatabase(templateList, false);
             
         
         }catch(Exception ex){
         
         }
        
        return null;
    
    }
    
    
     @GetMapping("/uploadStatus")
    public String uploadStatus() {
        return "uploadStatus";
    }
}
