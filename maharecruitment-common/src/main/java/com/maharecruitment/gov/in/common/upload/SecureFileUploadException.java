package com.maharecruitment.gov.in.common.upload;

public class SecureFileUploadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SecureFileUploadException(String message) {
        super(message);
    }

    public SecureFileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
