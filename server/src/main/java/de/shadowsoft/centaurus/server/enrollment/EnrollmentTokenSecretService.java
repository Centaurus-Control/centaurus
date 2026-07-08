package de.shadowsoft.centaurus.server.enrollment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentTokenSecretService {

    private final SecureRandom secureRandom = new SecureRandom();

    public EnrollmentTokenSecret createToken() {
        byte[] tokenBytes = new byte[48];
        secureRandom.nextBytes(tokenBytes);
        String token = "raet_" + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        return new EnrollmentTokenSecret(token, hash(token));
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
