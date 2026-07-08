package de.shadowsoft.centaurus.server.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import de.shadowsoft.centaurus.server.auth.AuthProperties;
import de.shadowsoft.centaurus.server.auth.BootstrapAdminProperties;
import de.shadowsoft.centaurus.server.auth.PasswordChangeRequiredFilter;
import de.shadowsoft.centaurus.server.enrollment.EnrollmentProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties({AuthProperties.class, BootstrapAdminProperties.class, EnrollmentProperties.class})
public class SecurityConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        PasswordChangeRequiredFilter passwordChangeRequiredFilter
    ) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/agent/enroll", "/agent/ws", "/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/machines/*/wake-on-lan").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/machines/*/commands/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/machines/*/script-configurations/*/execute").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/machines/*/functions/*/execute").hasAnyRole("ADMIN", "OPERATOR")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .addFilterAfter(passwordChangeRequiredFilter, BearerTokenAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> authorities(jwt, scopeAuthoritiesConverter));
        return authenticationConverter;
    }

    private Collection<GrantedAuthority> authorities(
        Jwt jwt,
        JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter
    ) {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>(scopeAuthoritiesConverter.convert(jwt));
        String role = jwt.getClaimAsString("role");
        if (StringUtils.hasText(role)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public SecretKey jwtSecretKey(AuthProperties authProperties) {
        byte[] secretBytes;
        if (StringUtils.hasText(authProperties.getJwtSecret())) {
            secretBytes = authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        } else {
            secretBytes = new byte[32];
            new SecureRandom().nextBytes(secretBytes);
            LOGGER.warn("No CENTAURUS_AUTH_JWT_SECRET is configured. Access tokens will be invalid after restart.");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }
}
