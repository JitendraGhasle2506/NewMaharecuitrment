package com.maharecruitment.gov.in.web.service.verification;

public interface RegistrationOtpNotificationService {

    void sendMobileOtp(String mobileNo, String otp);

    void sendEmailOtp(String email, String otp);

    void sendDepartmentCredentials(String email, String contactName, String temporaryPassword);
}
