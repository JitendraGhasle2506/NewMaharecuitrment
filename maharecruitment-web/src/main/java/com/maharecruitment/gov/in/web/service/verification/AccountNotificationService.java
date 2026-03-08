package com.maharecruitment.gov.in.web.service.verification;

public interface AccountNotificationService {

    void sendDepartmentCredentials(String email, String contactName, String temporaryPassword);
}
