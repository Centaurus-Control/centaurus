package de.shadowsoft.centaurus.server.agent;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AgentManagementExceptionHandler {

    @ExceptionHandler(AgentNotFoundException.class)
    public ProblemDetail handleAgentNotFoundException(AgentNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Agent not found");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
