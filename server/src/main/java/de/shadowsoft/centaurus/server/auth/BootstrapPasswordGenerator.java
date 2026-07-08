package de.shadowsoft.centaurus.server.auth;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class BootstrapPasswordGenerator {

    private static final char[] PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();
    private static final int PASSWORD_LENGTH = 24;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generatePassword() {
        char[] password = new char[PASSWORD_LENGTH];
        for (int index = 0; index < password.length; index++) {
            password[index] = PASSWORD_CHARS[secureRandom.nextInt(PASSWORD_CHARS.length)];
        }
        return new String(password);
    }
}
