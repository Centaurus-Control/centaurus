package de.shadowsoft.centaurus.agent.enrollment;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentBundleParser {

    private static final String PREFIX = "raenroll:";

    private final ObjectMapper objectMapper;

    public EnrollmentBundleParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EnrollmentBundle parse(String bundle) {
        if (bundle == null || !bundle.startsWith(PREFIX)) {
            throw new EnrollmentException("Enrollment bundle must start with raenroll:");
        }
        try {
            String encodedPayload = bundle.substring(PREFIX.length());
            byte[] payloadBytes = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readValue(new String(payloadBytes, StandardCharsets.UTF_8), EnrollmentBundle.class);
        } catch (IllegalArgumentException | IOException exception) {
            throw new EnrollmentException("Enrollment bundle is invalid", exception);
        }
    }
}
