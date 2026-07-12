package de.shadowsoft.centaurus.server.trustcert;

public class TrustedCertificateException extends RuntimeException {

    public TrustedCertificateException(String message) {
        super(message);
    }

    public TrustedCertificateException(String message, Throwable cause) {
        super(message, cause);
    }
}
