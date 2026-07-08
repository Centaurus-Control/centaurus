package de.shadowsoft.centaurus.server.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserManagementExceptionHandler {

    @ExceptionHandler(UserManagementException.class)
    public ProblemDetail handleUserManagementException(UserManagementException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("User management error");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
