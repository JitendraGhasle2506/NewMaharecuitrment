package com.maharecruitment.gov.in.web.service.verification;

public interface AccountNotificationService {

    void sendDepartmentCredentials(
            String email,
            String mobileNo,
            String contactName,
            String username,
            String temporaryPassword);

    void sendEmployeeCredentials(
            String email,
            String mobileNo,
            String contactName,
            String username,
            String temporaryPassword);

    void sendAgencyCredentials(String email, String contactName, String temporaryPassword);
}
