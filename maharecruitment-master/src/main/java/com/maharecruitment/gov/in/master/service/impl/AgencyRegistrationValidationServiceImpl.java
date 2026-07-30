package com.maharecruitment.gov.in.master.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.service.AgencyRegistrationValidationService;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;

@Service
@Transactional(readOnly = true)
public class AgencyRegistrationValidationServiceImpl implements AgencyRegistrationValidationService {

    private final AgencyMasterRepository agencyMasterRepository;

    public AgencyRegistrationValidationServiceImpl(AgencyMasterRepository agencyMasterRepository) {
        this.agencyMasterRepository = agencyMasterRepository;
    }

    @Override
    public void validateAgencyRegistration(Long agencyId) {
        if (agencyId == null) {
            return;
        }

        if (!agencyMasterRepository.existsById(agencyId)) {
            throw new IllegalArgumentException("Agency registration not found for id: " + agencyId);
        }
    }
}
