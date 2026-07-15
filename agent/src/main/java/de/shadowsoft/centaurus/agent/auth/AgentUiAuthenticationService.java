package de.shadowsoft.centaurus.agent.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shadowsoft.centaurus.agent.config.AgentConfig;
import de.shadowsoft.centaurus.agent.config.AgentConfigStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentUiAuthenticationService {

    private final AgentConfigStore configStore;
    private final AgentUiSessionService sessionService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AgentUiAuthenticationService(
        AgentConfigStore configStore,
        AgentUiSessionService sessionService,
        ObjectMapper objectMapper
    ) {
        this.configStore = configStore;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    public AgentUiSessionService.AgentUiSession login(AgentUiLoginRequest request) {
        String serverUrl = resolveServerUrl();
        try {
            String loginJson = objectMapper.writeValueAsString(Map.of(
                "username", request.username(),
                "password", request.password()
            ));
            HttpRequest loginRequest = HttpRequest.newBuilder(URI.create(serverUrl + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                .build();
            HttpResponse<String> loginResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
            if (loginResponse.statusCode() / 100 != 2) {
                throw new AgentUiAuthenticationException("Server rejected login");
            }
            JsonNode loginBody = objectMapper.readTree(loginResponse.body());
            String accessToken = loginBody.path("accessToken").asText();

            HttpRequest meRequest = HttpRequest.newBuilder(URI.create(serverUrl + "/api/me"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
            HttpResponse<String> meResponse = httpClient.send(meRequest, HttpResponse.BodyHandlers.ofString());
            if (meResponse.statusCode() / 100 != 2) {
                throw new AgentUiAuthenticationException("Could not verify server user");
            }
            JsonNode meBody = objectMapper.readTree(meResponse.body());
            String role = meBody.path("role").asText();
            String username = meBody.path("username").asText(request.username());
            if (!"ADMIN".equals(role)) {
                throw new AgentUiAuthenticationException("Agent UI requires a server ADMIN user");
            }
            return sessionService.create(username, role);
        } catch (AgentUiAuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AgentUiAuthenticationException("Could not authenticate against server", exception);
        }
    }

    private String resolveServerUrl() {
        AgentConfig config = configStore.load();
        if (config.getAgentId() == null) {
            throw new AgentUiAuthenticationException("Agent is not enrolled");
        }
        if (StringUtils.hasText(config.getServerUrl())) {
            return trimTrailingSlash(config.getServerUrl());
        }
        throw new AgentUiAuthenticationException("Enrolled server URL is missing");
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
