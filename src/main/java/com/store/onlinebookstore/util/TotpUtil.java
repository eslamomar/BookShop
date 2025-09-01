package com.store.onlinebookstore.util;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;

@Component
public class TotpUtil {

    private final TimeBasedOneTimePasswordGenerator totp;

    public TotpUtil() throws Exception {
        // Default: 30-second window, 6 digits, HMAC-SHA1
        this.totp = new TimeBasedOneTimePasswordGenerator(
                Duration.ofSeconds(30), 6, "HmacSHA1"
        );
    }

    public boolean verifyCode(String code, SecretKey key) {
        try {
            int expected = totp.generateOneTimePassword(key, Instant.now());
            return Integer.parseInt(code) == expected;
        } catch (Exception e) {
            return false;
        }
    }
}
