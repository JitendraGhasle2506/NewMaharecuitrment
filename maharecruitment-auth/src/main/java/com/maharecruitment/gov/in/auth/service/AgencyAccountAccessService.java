package com.maharecruitment.gov.in.auth.service;

import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserAgencyMappingRepository;

@Service
public class AgencyAccountAccessService {

    public static final String INACTIVE_AGENCY_MESSAGE =
            "Agency account is inactive. Please contact the administrator.";
    public static final String MISSING_AGENCY_MAPPING_MESSAGE =
            "Agency account mapping is missing. Please contact the administrator.";

    private static final String ROLE_AGENCY = "ROLE_AGENCY";

    private final UserAgencyMappingRepository userAgencyMappingRepository;

    public AgencyAccountAccessService(UserAgencyMappingRepository userAgencyMappingRepository) {
        this.userAgencyMappingRepository = userAgencyMappingRepository;
    }

    @Transactional(readOnly = true)
    public void validateLoginAccess(User user) {
        if (user == null || !hasAgencyRole(user)) {
            return;
        }
        if (user.getId() == null || !userAgencyMappingRepository.existsActiveMappingForUserId(user.getId())) {
            throw new DisabledException(MISSING_AGENCY_MAPPING_MESSAGE);
        }
        if (!userAgencyMappingRepository.existsActiveAgencyForUserId(user.getId())) {
            throw new DisabledException(INACTIVE_AGENCY_MESSAGE);
        }
    }

    private boolean hasAgencyRole(User user) {
        return user.getRoles() != null
                && user.getRoles().stream()
                        .anyMatch(role -> role.getName() != null
                                && ROLE_AGENCY.equalsIgnoreCase(role.getName().trim()));
    }
}
