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
import ch.qos.logback.core.Context;
import com.nmrs.umb.biometriclinux.models.AppModel;
import com.nmrs.umb.biometriclinux.models.FingerPrintInfo;
import com.nmrs.umb.biometriclinux.models.FingerPrintMatchInputModel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Date;
import javax.imageio.ImageIO;
import org.jboss.logging.Logger;

/**
 *
 * @author Morrison Idiasirue
 */
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

        if (isDeviceOpen == false) {
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

            long error = jsgFPLib.GetImageQuality(deviceInfo.imageWidth, deviceInfo.imageHeight, imageBuffer1, qualityArray);

            quality = qualityArray[0];

            SGFingerInfo fingerInfo = new SGFingerInfo();
            fingerInfo.FingerNumber = fingerPosition;
            fingerInfo.ImageQuality = quality;
            fingerInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP;
            fingerInfo.ViewNumber = 1;

            error = jsgFPLib.CreateTemplate(fingerInfo, imageBuffer1, imageTemplate);

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

            if (error != SGFDxErrorCode.SGFDX_ERROR_NONE) {
                fingerPrintInfo.setErrorMessage(String.valueOf(error));
            }
            
            if(quality < AppUtil.QUALITY_THRESHOLD){
                fingerPrintInfo.setErrorMessage("-1");
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
        int matchedRecord = 0;
        long error;
        boolean[] matched = new boolean[1];

        try {
            byte[] unknownTemplateArray = Base64.getDecoder().decode(input.getFingerPrintTemplate());

            for (FingerPrintInfo each : input.getFingerPrintTemplateListToMatch()) {
                byte[] fingerTemplate = Base64.getDecoder().decode(each.getTemplate());

                SGISOTemplateInfo sample_info = new SGISOTemplateInfo();
                try{
                error = jsgFPLib.GetIsoTemplateInfo(fingerTemplate, sample_info);
                
                }catch(Exception ex){
                    System.err.println(ex.getMessage());
                }
                
                for (int i = 0; i < sample_info.TotalSamples; i++) {

                    error = jsgFPLib.MatchIsoTemplate(fingerTemplate, i, unknownTemplateArray, 0, SGFDxSecurityLevel.SL_NORMAL, matched);

                    if (matched[0]) {
                        matchedRecord = each.getPatienId();
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Logger.Level.FATAL, ex);
        }

        return matchedRecord;
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

}
