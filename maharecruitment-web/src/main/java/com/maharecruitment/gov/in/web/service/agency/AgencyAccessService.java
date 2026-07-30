package com.maharecruitment.gov.in.web.service.agency;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.AgencyAccountAccessService;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;

@Service
public class AgencyAccessService {

    private final UserAffiliationService userAffiliationService;
    private final AgencyMasterRepository agencyMasterRepository;

    public AgencyAccessService(
            UserAffiliationService userAffiliationService,
            AgencyMasterRepository agencyMasterRepository) {
        this.userAffiliationService = userAffiliationService;
        this.agencyMasterRepository = agencyMasterRepository;
    }

    @Transactional(readOnly = true)
    public AgencyUserContext requireActiveAgencyContext(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }

        User user;
        try {
            user = userAffiliationService.loadUserByEmail(actorEmail.trim());
        } catch (IllegalArgumentException ex) {
            throw new RecruitmentNotificationException(AgencyAccountAccessService.MISSING_AGENCY_MAPPING_MESSAGE);
        }
        Long agencyId = userAffiliationService.resolvePrimaryAgencyId(user);
        if (agencyId == null) {
            throw new RecruitmentNotificationException(AgencyAccountAccessService.MISSING_AGENCY_MAPPING_MESSAGE);
        }

        AgencyMaster agency = agencyMasterRepository.findById(agencyId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        AgencyAccountAccessService.MISSING_AGENCY_MAPPING_MESSAGE));

        if (!Boolean.TRUE.equals(user.getActive()) || AgencyStatus.ACTIVE != agency.getStatus()) {
            throw new RecruitmentNotificationException(AgencyAccountAccessService.INACTIVE_AGENCY_MESSAGE);
        }

        return new AgencyUserContext(user.getId(), agency.getAgencyId(), agency.getAgencyName());
    }
}
