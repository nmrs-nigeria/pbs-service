package com.nmrs.umb.biometriclinux.controllers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.main.FingerPrintUtilImpl;
import com.nmrs.umb.biometriclinux.main.Partition;
import com.nmrs.umb.biometriclinux.models.*;
import com.nmrs.umb.biometriclinux.security.FileEncrypterDecrypter;
import com.nmrs.umb.biometriclinux.security.Key;
import com.nmrs.umb.biometriclinux.security.Utils;
import com.opencsv.CSVReader;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.text.StringEscapeUtils;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.servlet.ServletContext;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class DownloadController {

    private static final String PBS_UPLOAD_FOLDER = "pbs_upload/";
    Logger logger = Logger.getLogger(DownloadController.class);
    ObjectMapper mapper = new ObjectMapper();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

    SecretKey secretKey;
    FileEncrypterDecrypter fileEncrypterDecrypter;

    @Autowired
    DbManager dbManager;

    @Autowired
    ServletContext context;

    @Autowired
    FingerPrintUtilImpl fingerPrintUtilImpl;

    @Value("${verify:false}")
    boolean verify;

    @Value("${keystore:pbsKeyStore}")
    String keystore;

    @Value("${numberOfPatientPerBatch:500}")
    Integer numberOfPatientsPerBatch;

    @Value("${numberOfDevice:0}")
    Integer numberOfDevice;


    @GetMapping("/view")
	public String greeting() {
		return "download";
	}

	@GetMapping("/download")
	public ResponseEntity<Object> getFile(@RequestParam(value="path") String path) {
        String filename = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            File keyst = new File(keystore);
            if(!keyst.exists())  return  ResponseEntity.ok().body("keystore file is missing");
            String passcode = dbManager.getGlobalProperty("pbs_pass");
            if(passcode == null || passcode.isEmpty()) passcode = "changeit";
            secretKey = Key.getSecretKey(keystore, passcode);
            if(secretKey != null) fileEncrypterDecrypter = new FileEncrypterDecrypter(secretKey, "AES/CBC/PKCS5Padding");

			String datimCode = dbManager.getGlobalProperty("facility_datim_code");
			if (path != null && path.equalsIgnoreCase("invalid")) {
				filename = "Patients_with_invalid_fingerprint_data.csv";
				Set<Integer> invalids = dbManager.getPatientsWithInvalidData();
				 if (invalids.size() > 0) byteArrayOutputStream = dbManager.getCsvFilePath(new ArrayList<>(invalids), datimCode);
			} else if (path != null && path.equalsIgnoreCase("lowQuality")) {
				filename = "Patients_with_low_quality_fingerprint_data.csv";
				Set<Integer> lowQuality = dbManager.getPatientsWithLowQualityData();
				if (lowQuality.size() > 0)  byteArrayOutputStream = dbManager.getCsvFilePath(new ArrayList<>(lowQuality), datimCode);
			} else if (path != null && path.equalsIgnoreCase("both")) {
				String facilityName = dbManager.getGlobalProperty("Facility_Name");
				facilityName = facilityName.replaceAll(" ","_");
				filename = datimCode+"-"+facilityName+"-patients-fingerprint-data.csv";
                String zipName = datimCode+"-"+facilityName+"-patients-fingerprint-data.zip";
				Set<Integer> invalids = dbManager.getPatientsWithInvalidData();
				Set<Integer> lowQuality = dbManager.getPatientsWithLowQualityData();
				Set<Integer> none = dbManager.getPatientsWithoutFingerPrintData();
				invalids.addAll(lowQuality);
				invalids.addAll(none);
                int number = numberOfPatientsPerBatch;
				if(numberOfDevice > 0 ){
				    int size = invalids.size();
				    if((size % numberOfDevice) == 0 ){
				        number =  size/numberOfDevice;
                    }else {
                        number =  (size/numberOfDevice) + 1;
                    }
                }
                List<List<Integer>> partitions = Partition.ofSize(new ArrayList<>(invalids), number);
                ByteArrayOutputStream fos = new ByteArrayOutputStream();
                ZipOutputStream zipOut = new ZipOutputStream(fos);
                int i=1;
                for(List<Integer> partition : partitions) {
                    System.out.println("Partition of "+i+" with "+partition.size()+" size");
                    byteArrayOutputStream = dbManager.getCsvFilePath(partition, datimCode);
                    if (byteArrayOutputStream != null) {
                        ZipEntry zipEntry = new ZipEntry(i+"-"+filename);
                        i++;
                        zipOut.putNextEntry(zipEntry);
                        if (fileEncrypterDecrypter != null) {
                            CipherInputStream cipherInputStream = fileEncrypterDecrypter.encrypt(byteArrayOutputStream);
                            byte[] bytes = new byte[1024];
                            int length;
                            while((length = cipherInputStream.read(bytes)) >= 0) {
                                zipOut.write(bytes, 0, length);
                            }
                            cipherInputStream.close();
                        } else {
                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                            byte[] bytes = new byte[1024];
                            int length;
                            while((length = byteArrayInputStream.read(bytes)) >= 0) {
                                zipOut.write(bytes, 0, length);
                            }
                            byteArrayInputStream.close();
                        }
                    }
                }
                zipOut.close();
                fos.close();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fos.toByteArray());
                InputStreamResource file = new InputStreamResource(byteArrayInputStream);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipName)
                        .contentType(MediaType.parseMediaType("application/zip"))
                        .body(file);
			}
			if(byteArrayOutputStream != null) {
                InputStreamResource file;
			    if(fileEncrypterDecrypter != null) {
                    CipherInputStream cipherInputStream = fileEncrypterDecrypter.encrypt(byteArrayOutputStream);
                    file = new InputStreamResource(cipherInputStream);
                }else{
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    file = new InputStreamResource(byteArrayInputStream);
                }

				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
						.contentType(MediaType.parseMediaType("text/csv"))
						.body(file);
			}
		}catch (Exception e){
			System.out.println(e.getMessage());
			return  ResponseEntity.ok()
					.body(e.getMessage());
		}finally {
			try {
				if(byteArrayOutputStream != null) byteArrayOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return  ResponseEntity.ok()
				.body("No patient Data");
	}

    @PostMapping("/upload")
    public ResponseEntity<Object>  uploadFile(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        mapper.configure(
                JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS,
                true
        );
        BufferedReader reader = null;
        try {
            String passcode = dbManager.getGlobalProperty("pbs_pass");
            if(passcode == null || passcode.isEmpty()) passcode = "changeit";
            secretKey = Key.getSecretKey(keystore, passcode);
            if(secretKey != null) fileEncrypterDecrypter = new FileEncrypterDecrypter(secretKey, "AES/CBC/PKCS5Padding");

            if (file.isEmpty()) {
               return  ResponseEntity.ok()
                        .body("Please select a file to upload");
            }

            byte[] bytes = file.getBytes();
            String fileName = context.getRealPath(context.getContextPath()) + PBS_UPLOAD_FOLDER + file.getOriginalFilename();
            Path path = Paths.get(fileName);
            Files.createDirectories(path.getParent());
            Files.deleteIfExists(path);
            Files.createFile(path);
            Files.write(path, bytes);
            if(fileEncrypterDecrypter != null) {
                reader = fileEncrypterDecrypter.decrypt(fileName);
            }else{
                reader = Files.newBufferedReader(path);
            }
           List<String> errorMap = new ArrayList<>();
           Map<String, List<FingerPrintInfo>> fingerPrintInfoMap = new HashMap<>();
            CSVReader  csvReader = new CSVReader(reader);
           int lineNum = 0;
            while (true) {
                String[] input;
                try {
                    input = csvReader.readNext();
                } catch (Exception ex) {
                    input = new String[0];
                }
                if (input == null) break;

                if (input.length > 2 && lineNum != 0) {
                    String patientId;
                    String pepFarId;
                    String inputDatimCode;
                    String json;
                    String date_Captured;
                    if(input.length == 5){
                        patientId = input[0];
                        pepFarId = input[1];
                        inputDatimCode = input[2];
                        json = input[3];
                        date_Captured = input[4];
                    }else{
                        patientId = input[0];
                        pepFarId = null;
                        inputDatimCode = input[1];
                        json = input[2];
                        date_Captured = null;
                    }

                    Date dte = getConvertedDate(date_Captured);
                    boolean noError = true;
                    try {
                        String datimCode = dbManager.getGlobalProperty("facility_datim_code");
                        if (!inputDatimCode.equalsIgnoreCase(datimCode)) {
                            errorMap.add(patientId + "," + pepFarId + "," + inputDatimCode + "," + "DatimCode does not match - this patient does not belong to this facility");
                        } else {

                            CaptureData captureData = mapper.readValue(json, CaptureData.class);
                            List<FingerPrintInfo> fingerPrintInfos = constructPrints(patientId, captureData, dte);
                            List<String> prints = new ArrayList<>();
                            fingerPrintInfos.forEach(print -> {
                                if (verify) {
                                    if (fingerPrintUtilImpl.isValid(print.getTemplate()))
                                        prints.add(print.getTemplate());
                                } else {
                                    prints.add(print.getTemplate());
                                }
                            });

                            if (prints.size() < 6) {
                                noError = false;
                                errorMap.add(patientId + "," + pepFarId + "," + inputDatimCode + "," + "Contains Invalid prints");
                            }

                            //verify
//                    if(verify) {
                            if (noError && Utils.containsDuplicate(fingerPrintInfos, fingerPrintUtilImpl)) {
                                noError = false;
                                errorMap.add(patientId + "," + pepFarId + "," + inputDatimCode + "," + "Biometric contains duplicate fingers kindly rescan");
                            }
//                    }
                            if (noError && verify) {
                                String response = Utils.inDb(prints, dbManager, fingerPrintUtilImpl);
                                if (response != null) {
                                    noError = false;
                                    errorMap.add(patientId + "," + pepFarId + "," + inputDatimCode + "," + response);
                                }
                            }
                            if (noError) {
                                fingerPrintInfoMap.put(patientId, fingerPrintInfos);
                            }
                        }
                    } catch (Exception ex) {
                        errorMap.add(patientId + "," + inputDatimCode + "," + ex.getMessage());
                        redirectAttributes.addFlashAttribute("message", "IOException occurred while processing file");
                        java.util.logging.Logger.getLogger(DownloadController.class.getName()).log(Level.SEVERE, null, ex);

                    }
                }else{
                    if(lineNum != 0) {
                        if (input.length > 0) {
                            errorMap.add(input[0] + ",,," + "Invalid Data");
                        } else {
                            errorMap.add(",,,Invalid Data");
                        }
                    }
                }
                lineNum++;
            }
            try{
                if(fingerPrintInfoMap.size() > 0){
                    dbManager.SaveMapToDatabase(fingerPrintInfoMap, true);
                }
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("message", "IOException occurred while processing file");
                java.util.logging.Logger.getLogger(DownloadController.class.getName()).log(Level.SEVERE, null, ex);

            }finally {
                reader.close();
            }

            if(errorMap.size()>0) {
                ByteArrayOutputStream byteArrayOutputStream = dbManager.getCsvFilePath(errorMap);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                InputStreamResource filInputStreamResource = new InputStreamResource(byteArrayInputStream);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=error_" + file.getOriginalFilename())
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .body(filInputStreamResource);

            }

            if (redirectAttributes.getFlashAttributes().get("message") != null) {
                return  ResponseEntity.ok()
                        .body(redirectAttributes.getFlashAttributes().get("message"));
            }

            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded '" + file.getOriginalFilename() + "'");

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message",ex.getMessage());
            logger.debug(ex.getMessage());

        }finally {
            try {
                if(reader != null) reader.close();
            }catch (Exception ignored){}
        }

        return  ResponseEntity.ok()
                .body(redirectAttributes.getFlashAttributes().get("message"));

    }

    private Date getConvertedDate(String date_captured) {
        Date date = null;
        if(date_captured != null){
            try {
                date = simpleDateFormat.parse(date_captured);
            }catch (Exception ex){
                logger.error(ex.getMessage());
            }
        }
        if(date == null) date = new Date();
        return date;
    }

    @GetMapping("/uploadStatus")
    public String uploadStatus() {
        return "uploadStatus";
    }

    private List<FingerPrintInfo> constructPrints(String patientId, CaptureData captureData, Date dateCaptured) {
        List<FingerPrintInfo> fingerPrintInfos = new ArrayList<>();
        FingerPrintInfo fingerPrintInfo;

        if (captureData.getLeft_index() != null) {

            fingerPrintInfo = constructPrintPerPosition(patientId, captureData.getLeft_index(), AppModel.FingerPositions.LeftIndex, dateCaptured);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getLeft_middle() != null) {
            fingerPrintInfo = constructPrintPerPosition(patientId, captureData.getLeft_middle(), AppModel.FingerPositions.LeftMiddle, dateCaptured);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        
        if (captureData.getLeft_small() != null) {
            fingerPrintInfo = constructPrintPerPosition(patientId, captureData.getLeft_small(), AppModel.FingerPositions.LeftSmall, dateCaptured);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getLeft_thumb() != null) {
            fingerPrintInfo = constructPrintPerPosition(patientId, captureData.getLeft_thumb(), AppModel.FingerPositions.LeftThumb, dateCaptured);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getLeft_wedding() != null) {
            fingerPrintInfo = constructPrintPerPosition(patientId, captureData.getLeft_wedding(), AppModel.FingerPositions.LeftWedding, dateCaptured);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getRight_index() != null) {
            fingerPrintInfo = constructPrintPerPosition(patientId, captureData.getRight_index(), AppModel.FingerPositions.RightIndex, dateCaptured);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getRight_middle() != null) {
            fingerPrintInfo = constructPrintPerPosition(patientId, captureData.getRight_middle(), AppModel.FingerPositions.RightMiddle, dateCaptured);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getRight_small() != null) {
            fingerPrintInfo = constructPrintPerPosition(patientId, captureData.getRight_small(), AppModel.FingerPositions.RightSmall, dateCaptured);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getRight_thumb() != null) {
            fingerPrintInfo = constructPrintPerPosition(patientId, captureData.getRight_thumb(), AppModel.FingerPositions.RightThumb, dateCaptured);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getRight_wedding() != null) {
            fingerPrintInfo = constructPrintPerPosition(patientId, captureData.getRight_wedding(), AppModel.FingerPositions.RightWedding, dateCaptured);
            fingerPrintInfos.add(fingerPrintInfo);
        }

        return fingerPrintInfos;

    }

    private FingerPrintInfo constructPrintPerPosition(String patientId,
            FingerPosition fingerPosition, AppModel.FingerPositions fingerPositions, Date dateCaptured) {
        FingerPrintInfo fingerPrintInfo = new FingerPrintInfo();

        fingerPrintInfo.setDateCreated(dateCaptured);
        fingerPrintInfo.setFingerPositions(fingerPositions);
        fingerPrintInfo.setImageDPI(fingerPosition.getImageDpi());
        fingerPrintInfo.setImageHeight(fingerPosition.getImageHeight());
        fingerPrintInfo.setImageQuality(fingerPosition.getQuality());
        fingerPrintInfo.setImageWidth(fingerPosition.getImageWidth());
        fingerPrintInfo.setPatienId(Integer.parseInt(patientId));
        fingerPrintInfo.setTemplate(fingerPosition.getTemplate());
        fingerPrintInfo.setCreator(0);

        return fingerPrintInfo;

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ColumnPositionMappingStrategy setColumMapping() {
        ColumnPositionMappingStrategy strategy = new ColumnPositionMappingStrategy();
        strategy.setType(UploadTemplate.class);
        String[] columns = new String[]{"PatientID", "DatimCode", "CaptureData"};
        strategy.setColumnMapping(columns);
        return strategy;
    }

}
