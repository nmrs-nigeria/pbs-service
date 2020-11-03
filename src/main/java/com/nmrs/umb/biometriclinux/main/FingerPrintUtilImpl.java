/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nmrs.umb.biometriclinux.main;

import SecuGen.FDxSDKPro.jni.JSGFPLib;
import SecuGen.FDxSDKPro.jni.SGDeviceInfoParam;
import SecuGen.FDxSDKPro.jni.SGFDxDeviceName;
import SecuGen.FDxSDKPro.jni.SGFDxErrorCode;
import SecuGen.FDxSDKPro.jni.SGFDxSecurityLevel;
import SecuGen.FDxSDKPro.jni.SGFDxTemplateFormat;
import SecuGen.FDxSDKPro.jni.SGFingerInfo;
import SecuGen.FDxSDKPro.jni.SGISOTemplateInfo;
import SecuGen.FDxSDKPro.jni.SGImpressionType;
import com.nmrs.umb.biometriclinux.models.AppModel;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.FingerPrintMatchInputModel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.jboss.logging.Logger;
import org.springframework.stereotype.Component;

/**
 *
 * @author Morrison Idiasirue
 */
@Component
public class FingerPrintUtilImpl implements FingerPrintUtil {

    private JSGFPLib jsgFPLib = null;
    private SGDeviceInfoParam deviceInfo;
    // private BufferedImage bufferedImage;
    private byte[] imageTemplate;
    private boolean isDeviceOpen = false;
    private static String OS = System.getProperty("os.name").toLowerCase();
    

    Logger logger = Logger.getLogger(FingerPrintUtilImpl.class);

    @Override
    public FingerPrintInfo capture(int fingerPosition, String err, boolean populateImagebytes) {

        if (!isDeviceOpen) {
            initializeDevice();
        }

        try {

            BufferedImage bufferedImage = new BufferedImage(deviceInfo.imageWidth, deviceInfo.imageHeight, BufferedImage.TYPE_BYTE_GRAY);
            byte[] imageBuffer1 = ((java.awt.image.DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
            if (jsgFPLib != null) {
                long erorCode = jsgFPLib.GetImageEx(imageBuffer1, 10000, 0, 50);
                if (erorCode == SGFDxErrorCode.SGFDX_ERROR_NONE) {

                    return captureFingerPrint(bufferedImage, imageBuffer1, fingerPosition, err, populateImagebytes);
                } else {

                    jsgFPLib.Close();
                    initializeDevice();
                    bufferedImage.flush();
                    imageBuffer1 = ((java.awt.image.DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

                    logger.log(Logger.Level.INFO, "Device opened successfully");
                    long erorCode2 = jsgFPLib.GetImageEx(imageBuffer1, 10000, 0, 50);
                    return captureFingerPrint(bufferedImage, imageBuffer1, fingerPosition, err, populateImagebytes);

                }
                
            }
        } catch (Exception ex) {

        }
        return null;

    }

    private FingerPrintInfo captureFingerPrint(BufferedImage bufferedImage, byte[] imageBuffer1, int fingerPosition, String err, boolean populateImagebytes) {

        FingerPrintInfo fingerPrintInfo = new FingerPrintInfo();
        try {
            int[] qualityArray = new int[1];
            int quality = 0;
            long nfiqvalue;
            imageTemplate = new byte[deviceInfo.imageWidth*deviceInfo.imageHeight];

            jsgFPLib.GetImageQuality(deviceInfo.imageWidth, deviceInfo.imageHeight, imageBuffer1, qualityArray);

            quality = qualityArray[0];

            SGFingerInfo fingerInfo = new SGFingerInfo();
            fingerInfo.FingerNumber = fingerPosition;
            fingerInfo.ImageQuality = quality;
            fingerInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP;
            fingerInfo.ViewNumber = 1;

            long error = jsgFPLib.CreateTemplate(fingerInfo, imageBuffer1, imageTemplate);

            if(error == SGFDxErrorCode.SGFDX_ERROR_NONE && quality < AppUtil.QUALITY_THRESHOLD){
                fingerPrintInfo.setErrorCode("-1");
                fingerPrintInfo.setErrorMessage("-1" +" - "+errorMap.get("-1"));
            }else if(error != SGFDxErrorCode.SGFDX_ERROR_NONE){
                fingerPrintInfo.setErrorCode(String.valueOf(error));
                fingerPrintInfo.setErrorMessage(error +" - "+errorMap.get(String.valueOf(error)));
            }else {
                fingerPrintInfo.setImageHeight(deviceInfo.imageHeight);
                fingerPrintInfo.setImageWidth(deviceInfo.imageWidth);
                fingerPrintInfo.setImageQuality(quality);
                fingerPrintInfo.setDateCreated(new Date());
                fingerPrintInfo.setImage(convertBytetoImage(bufferedImage));
                fingerPrintInfo.setImageByte((populateImagebytes) ? imageBuffer1 : null);
                if (fingerPosition != 0) {
                    //Java Enum is zero-based
                    fingerPrintInfo.setFingerPositions(AppModel.FingerPositions.values()[fingerPosition - 1]);
                } else {
                    fingerPrintInfo.setFingerPositions(AppModel.FingerPositions.values()[fingerPosition]);
                }

                fingerPrintInfo.setTemplate(Base64.getEncoder().encodeToString(imageTemplate));
                fingerPrintInfo.setImageDPI(deviceInfo.imageDPI);
            }

            //  nfiqvalue = jsgFPLib.ComputeNFIQ(imageBuffer1, deviceInfo.imageWidth, deviceInfo.imageHeight);
            bufferedImage.flush();
            return fingerPrintInfo;
        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
        }

        return null;
    }

    //to verify ISO Templates
    @Override
    public int verify(FingerPrintMatchInputModel input) {
        ExecutorService taskExecutor = Executors.newFixedThreadPool(20);
        ConcurrentHashMap<String, Integer> concurrentHashMap = new ConcurrentHashMap<>();

        int counter = 0;
        if(jsgFPLib == null)  initializeDevice();
        final int[] matchedRecord = {0};
        boolean[] matched = new boolean[1];

        List<List<FingerPrintInfo>> lists = Partition.ofSize(input.getFingerPrintTemplateListToMatch(), 1000);

        try {
            byte[] unknownTemplateArray = Base64.getDecoder().decode(input.getFingerPrintTemplate());

            for(List<FingerPrintInfo> printInfos : lists) {
                Thread thread = new Thread(() -> {
                    int id = getMatchedRecord(printInfos, matched, unknownTemplateArray);
                    if(id > 0) {
                        concurrentHashMap.putIfAbsent("match", id);
                        taskExecutor.shutdownNow();
                    }
                });
                thread.setName("Thread" + counter++);
                taskExecutor.submit(thread);
            }
            taskExecutor.shutdown();
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ignored) {}

            if(concurrentHashMap.get("match") != null) return concurrentHashMap.get("match");
            return 0;

        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
        }

        return matchedRecord[0];
    }

    //to verify ISO Templates
    @Override
    public int oldVerify(FingerPrintMatchInputModel input) {
        if(jsgFPLib == null)  initializeDevice();
        boolean[] matched = new boolean[1];

        try {
            byte[] unknownTemplateArray = Base64.getDecoder().decode(input.getFingerPrintTemplate());
            return getMatchedRecord(input.getFingerPrintTemplateListToMatch(), matched, unknownTemplateArray);
        }catch (Exception ex){
            logger.log(Logger.Level.FATAL, ex);
        }
        return 0;
    }

    private int getMatchedRecord(List<FingerPrintInfo> fingerPrintInfos, boolean[] matched, byte[] unknownTemplateArray) {
        for (FingerPrintInfo each : fingerPrintInfos) {
//            System.out.println("Checking : "+each.getPatienId());
            if(each.getTemplate() != null ){
             byte[] fingerTemplate = Base64.getDecoder().decode(each.getTemplate());
                jsgFPLib.MatchIsoTemplate(fingerTemplate, 0, unknownTemplateArray, 0, SGFDxSecurityLevel.SL_NORMAL, matched);
                if (matched[0]) {
                    return each.getPatienId();
                }
            }
        }
        return 0;
    }

    //for use with default secugen template
    private int verifyDefault(FingerPrintMatchInputModel input) {
        int matchedRecord = 0;
        boolean[] matched = new boolean[1];
        byte[] unknownTemplateArray = Base64.getDecoder().decode(input.getFingerPrintTemplate());
        SGISOTemplateInfo sample_info = new SGISOTemplateInfo();

        for (FingerPrintInfo each : input.getFingerPrintTemplateListToMatch()) {
            byte[] fingerTemplate = Base64.getDecoder().decode(each.getTemplate());
            if (jsgFPLib.MatchTemplate(fingerTemplate, unknownTemplateArray, SGFDxSecurityLevel.SL_NORMAL, matched) == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                if (matched[0]) {
                    matchedRecord = each.getPatienId();
                    break;
                }
            }
        }
        return matchedRecord;
    }

    private void initializeDevice() {

        jsgFPLib = new JSGFPLib();

        deviceInfo = new SGDeviceInfoParam();

        if (jsgFPLib.Init(SGFDxDeviceName.SG_DEV_AUTO) == SGFDxErrorCode.SGFDX_ERROR_NONE) {

            if (jsgFPLib.OpenDevice(SGFDxDeviceName.SG_DEV_AUTO) == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                jsgFPLib.GetDeviceInfo(deviceInfo);

                logger.log(Logger.Level.INFO, "Device opened successfully");
                jsgFPLib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
                isDeviceOpen = true;

            }

        } else {
            throw new IllegalStateException("Device fail to initialize");
        }

    }

    private String convertBytetoImage(BufferedImage bImage2) {

        try {
            String base64Str;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                logger.log(Logger.Level.INFO, "started image conversion");
                ImageIO.write(bImage2, "bmp", outputStream);
                base64Str = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                //System.out.println(base64Str);
            }

            return base64Str;
        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex.getMessage());
        }

        return null;

    }

    public static boolean isWindows() {

        return (OS.indexOf("win") >= 0);

    }

    Map<String, String> errorMap  = new HashMap<String, String>() {{
        put("0", "No error");
        put("1", "Creation failed (fingerprint reader not correctly installed or driver files error)");
        put("2", "Function failed (wrong type of fingerprint reader or not correctly installed)");
        put("3", "Internal (invalid parameters to sensor API)");
        put("5", "DLL load failed");
        put("6", "DLL load failed for driver");
        put("7", "DLL load failed for algorithm");
        put("51", "System file load failure");
        put("52", "Sensor chip initialization failed");
        put("53", "Sensor line dropped");
        put("54", "Timeout");
        put("55", "Device not found");
        put("56", " Driver load failed");
        put("57", " Wrong image");
        put("58", " Lack of bandwidth");
        put("59", " Device busy");
        put("60", " Cannot get serial number of the device");
        put("61", " Unsupported device");
        put("101", " Very low minutiae count");
        put("102", " Wrong template type");
        put("103", " Invalid template");
        put("104", " Invalid template");
        put("105", " Could not extract features");
        put("106", " Match failed");
        put("1000", " No memory");
        put("4000", " Invalid parameter passed to service");
        put("2000", " Internal error");
        put("3000", " Internal error extended");
        put("6000", "Certificate error cannot decode");
        put("10001", "License error");
        put("10002", "Invalid domain");
        put( "10003","License expired");
        put("10004","WebAPI may not have received the origin header from the browser");
        put("-1", "low Quality/Invalid Data");
    }};
}
