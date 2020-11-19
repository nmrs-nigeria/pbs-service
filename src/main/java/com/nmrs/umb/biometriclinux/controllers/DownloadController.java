package com.nmrs.umb.biometriclinux.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmrs.umb.biometriclinux.dal.DbManager;
import com.nmrs.umb.biometriclinux.models.AppModel;
import com.nmrs.umb.biometriclinux.models.CaptureData;
import com.nmrs.umb.biometriclinux.models.FingerPosition;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.UploadTemplate;
import com.opencsv.bean.ColumnPositionMappingStrategy;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import org.jboss.logging.Logger;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DownloadController {

    private static final String PBS_UPLOAD_FOLDER = "pbs_upload";
    Logger logger = Logger.getLogger(DownloadController.class);
    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    DbManager dbManager;

    @Autowired
    ServletContext context;

    @GetMapping("/view")
    public String greeting() {
        return "download";
    }

    @GetMapping("/download")
    public ResponseEntity<Object> getFile(@RequestParam(value = "path") String path) {
        String filename = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            String datimCode = dbManager.getGlobalProperty("facility_datim_code");
            if (path != null && path.equalsIgnoreCase("invalid")) {
                filename = "Patients_with_invalid_fingerprint_data.csv";
                Set<Integer> invalids = dbManager.getPatientsWithInvalidData();
                if (invalids.size() > 0) {
                    byteArrayInputStream = dbManager.getCsvFilePath(invalids, datimCode);
                }
            } else if (path != null && path.equalsIgnoreCase("lowQuality")) {
                filename = "Patients_with_low_quality_fingerprint_data.csv";
                Set<Integer> lowQuality = dbManager.getPatientsWithLowQualityData();
                if (lowQuality.size() > 0) {
                    byteArrayInputStream = dbManager.getCsvFilePath(lowQuality, datimCode);
                }
            } else if (path != null && path.equalsIgnoreCase("both")) {
                String facilityName = dbManager.getGlobalProperty("Facility_Name");
                filename = datimCode + "_" + facilityName + "_patients_fingerprint_data.csv";
                Set<Integer> invalids = dbManager.getPatientsWithInvalidData();
                Set<Integer> lowQuality = dbManager.getPatientsWithLowQualityData();
                Set<Integer> none = dbManager.getPatientsWithoutFingerPrintData();
                invalids.addAll(lowQuality);
                invalids.addAll(none);
                if (invalids.size() > 0) {
                    byteArrayInputStream = dbManager.getCsvFilePath(invalids, datimCode);
                }
            }
            if (byteArrayInputStream != null) {
                InputStreamResource file = new InputStreamResource(byteArrayInputStream);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                        .contentType(MediaType.parseMediaType("application/csv"))
                        .body(file);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.ok()
                    .body(e.getMessage());
        } finally {
            try {
                if (byteArrayInputStream != null) {
                    byteArrayInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ResponseEntity.ok()
                .body("No patient Data");
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:uploadStatus";
        }

        try {

            byte[] bytes = file.getBytes();

            Path path = Paths.get(context.getRealPath(context.getContextPath()) + PBS_UPLOAD_FOLDER + file.getOriginalFilename());
            Files.write(path, bytes);

            Reader reader = Files.newBufferedReader(path);
            CsvToBean<UploadTemplate> csvToBean = new CsvToBeanBuilder(reader)
                    .withType(UploadTemplate.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withMappingStrategy(setColumMapping())
                    .withSkipLines(1)//skip headers
                    .build();

            List<UploadTemplate> templateList = csvToBean.parse();
            templateList.stream().forEach(a -> {
                try {
                    CaptureData captureData = mapper.readValue(a.getCaptureData(), CaptureData.class);
                    List<FingerPrintInfo> fingerPrintInfos = constructPrints(a, captureData);
                    dbManager.SaveToDatabase(fingerPrintInfos, false);
                } catch (IOException ex) {
                    redirectAttributes.addFlashAttribute("message", "IOException occurred while processing file");
                    java.util.logging.Logger.getLogger(DownloadController.class.getName()).log(Level.SEVERE, null, ex);

                } catch (Exception ex) {
                    redirectAttributes.addFlashAttribute("message", "error occurred while processing file");
                    java.util.logging.Logger.getLogger(DownloadController.class.getName()).log(Level.SEVERE, null, ex);

                }
            });

            if (redirectAttributes.getFlashAttributes().get("message") != null) {
                return "redirect:uploadStatus";
            }

            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded '" + file.getOriginalFilename() + "'");

        } catch (IOException | IllegalStateException ex) {
            logger.debug(ex.getMessage());
        }

        return "redirect:/uploadStatus";

    }

    @GetMapping("/uploadStatus")
    public String uploadStatus() {
        return "uploadStatus";
    }

    private List<FingerPrintInfo> constructPrints(UploadTemplate uploadTemplate, CaptureData captureData) {
        List<FingerPrintInfo> fingerPrintInfos = new ArrayList<>();
        FingerPrintInfo fingerPrintInfo;

        if (captureData.getLeft_index() != null) {

            fingerPrintInfo = constructPrintPerPosition(uploadTemplate, captureData.getLeft_index(), AppModel.FingerPositions.LeftIndex);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getLeft_middle() != null) {
            fingerPrintInfo = constructPrintPerPosition(uploadTemplate, captureData.getLeft_middle(), AppModel.FingerPositions.LeftMiddle);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        
        if (captureData.getLeft_small() != null) {
            fingerPrintInfo = constructPrintPerPosition(uploadTemplate, captureData.getLeft_small(), AppModel.FingerPositions.LeftSmall);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getLeft_thumb() != null) {
            fingerPrintInfo = constructPrintPerPosition(uploadTemplate, captureData.getLeft_thumb(), AppModel.FingerPositions.LeftThumb);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getLeft_wedding() != null) {
            fingerPrintInfo = constructPrintPerPosition(uploadTemplate, captureData.getLeft_wedding(), AppModel.FingerPositions.LeftWedding);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getRight_index() != null) {
            fingerPrintInfo = constructPrintPerPosition(uploadTemplate, captureData.getRight_index(), AppModel.FingerPositions.RightIndex);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getRight_middle() != null) {
            fingerPrintInfo = constructPrintPerPosition(uploadTemplate, captureData.getRight_middle(), AppModel.FingerPositions.RightMiddle);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getRight_small() != null) {
            fingerPrintInfo = constructPrintPerPosition(uploadTemplate, captureData.getRight_small(), AppModel.FingerPositions.RightSmall);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getRight_thumb() != null) {
            fingerPrintInfo = constructPrintPerPosition(uploadTemplate, captureData.getRight_thumb(), AppModel.FingerPositions.RightThumb);
            fingerPrintInfos.add(fingerPrintInfo);
        } 
        if (captureData.getRight_wedding() != null) {
            fingerPrintInfo = constructPrintPerPosition(uploadTemplate, captureData.getRight_wedding(), AppModel.FingerPositions.RightWedding);
            fingerPrintInfos.add(fingerPrintInfo);
        }

        return fingerPrintInfos;

    }

    private FingerPrintInfo constructPrintPerPosition(UploadTemplate uploadTemplate,
            FingerPosition fingerPosition, AppModel.FingerPositions fingerPositions) {
        FingerPrintInfo fingerPrintInfo = new FingerPrintInfo();

        fingerPrintInfo.setDateCreated(new Date());
        fingerPrintInfo.setFingerPositions(fingerPositions);
        fingerPrintInfo.setImageDPI(fingerPosition.getImageDpi());
        fingerPrintInfo.setImageHeight(fingerPosition.getImageHeight());
        fingerPrintInfo.setImageQuality(fingerPosition.getQuality());
        fingerPrintInfo.setImageWidth(fingerPosition.getImageWidth());
        fingerPrintInfo.setPatienId(Integer.parseInt(uploadTemplate.getPatientID()));
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
