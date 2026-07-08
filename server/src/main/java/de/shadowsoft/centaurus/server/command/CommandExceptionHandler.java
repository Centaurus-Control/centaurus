package de.shadowsoft.centaurus.server.command;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CommandExceptionHandler {

    @ExceptionHandler(CommandDispatchException.class)
    public ProblemDetail handleCommandDispatchException(CommandDispatchException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Command dispatch failed");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
