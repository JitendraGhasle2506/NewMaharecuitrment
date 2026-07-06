package com.maharecruitment.gov.in.web.service.mobile;

import org.springframework.http.HttpStatus;

public class MobileAttendanceException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public MobileAttendanceException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
