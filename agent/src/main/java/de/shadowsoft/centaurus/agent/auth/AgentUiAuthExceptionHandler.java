package de.shadowsoft.centaurus.agent.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AgentUiAuthExceptionHandler {

    @ExceptionHandler(AgentUiAuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AgentUiAuthenticationException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle("Agent UI authentication failed");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
