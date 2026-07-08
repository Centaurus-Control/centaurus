package de.shadowsoft.centaurus.agent.enrollment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Service;

@Service
public class AgentEnrollmentClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AgentEnrollmentClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public AgentEnrollmentResponse enroll(String serverUrl, AgentEnrollmentRequest request) {
        URI enrollmentUri = URI.create(serverUrl + "/api/agent/enroll");
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(enrollmentUri)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toJson(request)))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new EnrollmentException("Server rejected enrollment: HTTP " + response.statusCode() + " " + response.body());
            }
            return objectMapper.readValue(response.body(), AgentEnrollmentResponse.class);
        } catch (IOException exception) {
            throw new EnrollmentException(
                "Could not call enrollment endpoint " + enrollmentUri + ": " + exception.getMessage(),
                exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EnrollmentException("Enrollment request was interrupted", exception);
        }
    }

    private String toJson(AgentEnrollmentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new EnrollmentException("Could not encode enrollment request", exception);
        }
    }
}
