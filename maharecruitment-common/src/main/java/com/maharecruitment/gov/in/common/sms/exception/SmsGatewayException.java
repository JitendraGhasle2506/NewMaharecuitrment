package com.maharecruitment.gov.in.common.sms.exception;

public class SmsGatewayException extends RuntimeException {

    public SmsGatewayException(String message) {
        super(message);
    }

    public SmsGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
