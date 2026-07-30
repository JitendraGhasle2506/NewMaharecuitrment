package com.maharecruitment.gov.in.workorder.exception;

public class WorkOrderException extends RuntimeException {

    public WorkOrderException(String message) {
        super(message);
    }

    public WorkOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
