package de.shadowsoft.centaurus.server.trustcert;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TrustedCertificateExceptionHandler {

    @ExceptionHandler(TrustedCertificateException.class)
    public ResponseEntity<Map<String, String>> handleTrustedCertificateException(TrustedCertificateException exception) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", exception.getMessage()));
    }
}
