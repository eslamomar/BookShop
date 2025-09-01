package com.store.onlinebookstore.util;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SecretKeyUtil {

    public static SecretKey fromBase32(String base32Secret) {
        Base32 base32 = new Base32();
        byte[] decodedKey = base32.decode(base32Secret);
        return new SecretKeySpec(decodedKey, "HmacSHA1"); // Google Authenticator default
    }
}
