package com.nmrs.umb.biometriclinux.security;

import org.jboss.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;

public class FileEncrypterDecrypter {

    private final SecretKey secretKey;
    private final Cipher cipher;
    Logger logger = Logger.getLogger(FileEncrypterDecrypter.class);

    public FileEncrypterDecrypter(SecretKey secretKey, String cipher) throws Exception {
        this.secretKey = secretKey;
        this.cipher = Cipher.getInstance(cipher);
    }


   public  CipherInputStream  encrypt(ByteArrayOutputStream byteArrayOutputStream) {
       ByteArrayOutputStream byteArrayOutputStreamNew = new ByteArrayOutputStream();
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] fileIv = cipher.getIV();
            byteArrayOutputStreamNew.write(fileIv);
            byteArrayOutputStreamNew.write(byteArrayOutputStream.toByteArray());
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStreamNew.toByteArray());
            return new CipherInputStream(byteArrayInputStream, cipher);
        }catch (Exception ex){
            logger.error(ex.getMessage());
        }
        return null;
    }

    public BufferedReader decrypt(String fileName) {

        try  {
            FileInputStream fileIn = new FileInputStream(fileName);
            byte[] fileIv = new byte[16];
            fileIn.read(fileIv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(fileIv));
            CipherInputStream cipherIn = new CipherInputStream(fileIn, cipher);
            InputStreamReader inputReader = new InputStreamReader(cipherIn);
            return new BufferedReader(inputReader);

        }catch (Exception ex){
            logger.error(ex.getMessage());
        }
        return null;
    }
}