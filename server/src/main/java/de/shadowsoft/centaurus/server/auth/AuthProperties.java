package de.shadowsoft.centaurus.server.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "centaurus.auth")
public class AuthProperties {

    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(365);
    private String refreshCookieName = "centaurus_refresh_token";
    private RefreshCookieSecureMode refreshCookieSecureMode = RefreshCookieSecureMode.AUTO;
    private String jwtSecret = "";

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    public void setRefreshCookieName(String refreshCookieName) {
        this.refreshCookieName = refreshCookieName;
    }

    public RefreshCookieSecureMode getRefreshCookieSecureMode() {
        return refreshCookieSecureMode;
    }

    public void setRefreshCookieSecureMode(RefreshCookieSecureMode refreshCookieSecureMode) {
        this.refreshCookieSecureMode = refreshCookieSecureMode;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }
}
