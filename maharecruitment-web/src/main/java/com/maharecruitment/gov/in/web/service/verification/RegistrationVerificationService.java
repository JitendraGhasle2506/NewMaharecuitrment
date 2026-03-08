package com.maharecruitment.gov.in.web.service.verification;

import jakarta.servlet.http.HttpSession;

public interface RegistrationVerificationService {

    void sendMobileOtp(HttpSession session, String mobileNo);

    boolean verifyMobileOtp(HttpSession session, String mobileNo, String otp);

    boolean isMobileVerified(HttpSession session, String mobileNo);

    void sendEmailOtp(HttpSession session, String email);

    boolean verifyEmailOtp(HttpSession session, String email, String otp);

    boolean isEmailVerified(HttpSession session, String email);

    void clear(HttpSession session);
}
