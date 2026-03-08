package com.maharecruitment.gov.in.master.exception;

@SuppressWarnings("serial")
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}

