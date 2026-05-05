package com.maharecruitment.gov.in.web.service.onboarding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
public class CandidateIdentityValidationService {

    private static final Logger log = LoggerFactory.getLogger(CandidateIdentityValidationService.class);

    private final AgencyCandidatePreOnboardingRepository preOnboardingRepository;
    private final EmployeeRepository employeeRepository;

    public CandidateIdentityValidationService(
            AgencyCandidatePreOnboardingRepository preOnboardingRepository,
            EmployeeRepository employeeRepository) {
        this.preOnboardingRepository = preOnboardingRepository;
        this.employeeRepository = employeeRepository;
    }

    public void validateUniqueGovernmentIds(
            Long currentPreOnboardingId,
            String aadhaarNumber,
            String panNumber) {
        validateUniqueAadhaar(currentPreOnboardingId, aadhaarNumber);
        validateUniquePan(currentPreOnboardingId, panNumber);
    }

    public boolean isAadhaarDuplicate(Long currentPreOnboardingId, String aadhaarNumber) {
        String normalizedAadhaar = normalizeAadhaar(aadhaarNumber);
        if (!StringUtils.hasText(normalizedAadhaar)) {
            return false;
        }

        boolean duplicateInPreOnboarding = preOnboardingRepository.existsByAadhaarNumberExcludingPreOnboardingId(
                normalizedAadhaar,
                currentPreOnboardingId);
        boolean duplicateInEmployees = employeeRepository.existsByNormalizedAadhaarNumber(normalizedAadhaar);
        if (duplicateInPreOnboarding || duplicateInEmployees) {
            log.warn(
                    "Duplicate Aadhaar detected during onboarding validation. preOnboardingId={}, maskedAadhaar={}, duplicateInPreOnboarding={}, duplicateInEmployees={}",
                    currentPreOnboardingId,
                    maskAadhaar(normalizedAadhaar),
                    duplicateInPreOnboarding,
                    duplicateInEmployees);
            return true;
        }

        return false;
    }

    private void validateUniqueAadhaar(Long currentPreOnboardingId, String aadhaarNumber) {
        if (isAadhaarDuplicate(currentPreOnboardingId, aadhaarNumber)) {
            throw new RecruitmentNotificationException("Aadhaar number already exists in the system.");
        }
    }

    private void validateUniquePan(Long currentPreOnboardingId, String panNumber) {
        String normalizedPan = normalizePan(panNumber);
        if (!StringUtils.hasText(normalizedPan)) {
            return;
        }

        boolean duplicateInPreOnboarding = preOnboardingRepository.existsByPanNumberExcludingPreOnboardingId(
                normalizedPan,
                currentPreOnboardingId);
        boolean duplicateInEmployees = employeeRepository.existsByNormalizedPanNumber(normalizedPan);
        if (duplicateInPreOnboarding || duplicateInEmployees) {
            log.warn(
                    "Duplicate PAN detected during onboarding validation. preOnboardingId={}, maskedPan={}, duplicateInPreOnboarding={}, duplicateInEmployees={}",
                    currentPreOnboardingId,
                    maskPan(normalizedPan),
                    duplicateInPreOnboarding,
                    duplicateInEmployees);
            throw new RecruitmentNotificationException("PAN number already exists in the system.");
        }
    }

    private String normalizeAadhaar(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", "") : null;
    }

    private String normalizePan(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String maskAadhaar(String value) {
        if (!StringUtils.hasText(value) || value.length() < 4) {
            return "****";
        }
        return "XXXXXXXX" + value.substring(value.length() - 4);
    }

    private String maskPan(String value) {
        if (!StringUtils.hasText(value) || value.length() < 4) {
            return "****";
        }
        return value.substring(0, 3) + "******" + value.substring(value.length() - 1);
    }
}
