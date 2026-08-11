package com.maharecruitment.gov.in.web.service.verification;

import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;

public class OtpDeliveryException extends RuntimeException {

    private final VerificationChannel channel;

    public OtpDeliveryException(VerificationChannel channel, String message, Throwable cause) {
        super(message, cause);
        this.channel = channel == null ? VerificationChannel.EMAIL : channel.canonical();
    }

    public VerificationChannel getChannel() {
        return channel;
    }
}
