package de.shadowsoft.centaurus.server.auth;

import de.shadowsoft.centaurus.server.user.User;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;

    public JwtTokenService(JwtEncoder jwtEncoder, AuthProperties authProperties) {
        this.jwtEncoder = jwtEncoder;
        this.authProperties = authProperties;
    }

    public AuthTokenResponse createAccessToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(authProperties.getAccessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("centaurus-server")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.getId().toString())
            .claim("username", user.getUsername())
            .claim("role", user.getRole().name())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthTokenResponse(token, "Bearer", expiresAt);
    }
}
