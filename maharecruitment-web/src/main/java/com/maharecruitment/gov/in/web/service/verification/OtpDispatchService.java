package com.maharecruitment.gov.in.web.service.verification;

public interface OtpDispatchService {

    void sendMobileOtp(String mobileNo, String otp);

    void sendEmailOtp(String email, String otp);

    default void sendEmailOtp(String email, String otp, String purpose) {
        sendEmailOtp(email, otp);
    }
}
