package com.nmrs.umb.biometriclinux.security;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.security.KeyStore;

public class Key {

    public static SecretKey getSecretKey(String keystore, String passcode) throws Exception {
        //Loading the the KeyStore object
        char[] password = passcode.toCharArray();

        //Creating the KeyStore object
        FileInputStream fis = new FileInputStream(keystore);
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(fis, password);

        //Creating the KeyStore.ProtectionParameter object
        KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(password);


        //Creating the KeyStore.SecretKeyEntry object
        KeyStore.SecretKeyEntry secretKeyEnt = (KeyStore.SecretKeyEntry)keyStore.getEntry("pbsKeyAlias", protectionParam);

        //Creating SecretKey object
        return secretKeyEnt.getSecretKey();
    }
}
