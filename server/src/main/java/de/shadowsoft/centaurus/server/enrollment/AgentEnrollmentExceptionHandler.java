package de.shadowsoft.centaurus.server.enrollment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AgentEnrollmentExceptionHandler {

    @ExceptionHandler(AgentEnrollmentException.class)
    public ProblemDetail handleAgentEnrollmentException(AgentEnrollmentException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Agent enrollment failed");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
