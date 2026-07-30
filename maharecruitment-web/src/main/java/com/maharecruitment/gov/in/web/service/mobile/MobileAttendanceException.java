package com.maharecruitment.gov.in.web.service.mobile;

import org.springframework.http.HttpStatus;

public class MobileAttendanceException extends MobileApiException {

    public MobileAttendanceException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }
}
