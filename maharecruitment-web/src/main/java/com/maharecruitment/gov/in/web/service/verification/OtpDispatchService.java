package com.maharecruitment.gov.in.web.service.verification;

public interface OtpDispatchService {

    void sendMobileOtp(String mobileNo, String otp);

    default void sendMobileOtp(String mobileNo, String otp, String otpReferenceId) {
        sendMobileOtp(mobileNo, otp);
    }

    default void sendMobileOtp(String mobileNo, String otp, String purpose, String otpReferenceId) {
        sendMobileOtp(mobileNo, otp, otpReferenceId);
    }

    void sendEmailOtp(String email, String otp);

    default void sendEmailOtp(String email, String otp, String purpose) {
        sendEmailOtp(email, otp);
    }

    default void sendEmailOtp(String email, String otp, String purpose, String otpReferenceId) {
        sendEmailOtp(email, otp, purpose);
    }
}
