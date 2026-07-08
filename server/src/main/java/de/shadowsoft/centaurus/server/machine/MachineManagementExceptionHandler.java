package de.shadowsoft.centaurus.server.machine;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MachineManagementExceptionHandler {

    @ExceptionHandler(MachineNotFoundException.class)
    public ProblemDetail handleMachineNotFoundException(MachineNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Machine not found");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(MachineManagementException.class)
    public ProblemDetail handleMachineManagementException(MachineManagementException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Machine management failed");
        problemDetail.setDetail(exception.getMessage());
        return problemDetail;
    }
}
