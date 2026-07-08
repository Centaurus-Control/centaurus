package de.shadowsoft.centaurus.server.scriptconfig;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ScriptConfigurationExceptionHandler {

    @ExceptionHandler(ScriptConfigurationException.class)
    public ProblemDetail handleScriptConfigurationException(ScriptConfigurationException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Script configuration failed");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
