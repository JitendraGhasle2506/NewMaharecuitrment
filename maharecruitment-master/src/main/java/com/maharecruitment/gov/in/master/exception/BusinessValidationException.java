package com.maharecruitment.gov.in.master.exception;

public class BusinessValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BusinessValidationException(String message) {
        super(message);
    }
}

