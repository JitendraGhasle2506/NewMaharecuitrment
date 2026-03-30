package com.maharecruitment.gov.in.auth.service;

import com.maharecruitment.gov.in.auth.dto.UserAffiliationView;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.User;

public interface UserAffiliationService {

    User loadUserByEmail(String email);

    UserAffiliationView getAffiliation(User user);

    UserAffiliationView getAffiliationByEmail(String email);

    DepartmentRegistrationEntity resolvePrimaryDepartmentRegistration(User user);

    Long resolvePrimaryAgencyId(User user);

    void synchronizeUserProfile(User user);

    void synchronizePrimaryDepartment(User user, DepartmentRegistrationEntity departmentRegistration);

    void synchronizePrimaryAgency(User user, Long agencyId);
}
