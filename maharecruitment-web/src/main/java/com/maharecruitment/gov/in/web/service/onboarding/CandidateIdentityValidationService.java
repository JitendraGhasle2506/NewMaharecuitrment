package com.maharecruitment.gov.in.web.service.onboarding;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
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

    public void validateUniqueCandidateDetails(
            Long currentPreOnboardingId,
            String aadhaarNumber,
            String panNumber,
            String email,
            String mobile) {
        validateUniqueAadhaar(currentPreOnboardingId, aadhaarNumber);
        validateUniquePan(currentPreOnboardingId, panNumber);
        validateUniqueEmail(currentPreOnboardingId, email);
        validateUniqueMobile(currentPreOnboardingId, mobile);
    }

    public boolean isAadhaarDuplicate(Long currentPreOnboardingId, String aadhaarNumber) {
        String normalizedAadhaar = normalizeAadhaar(aadhaarNumber);
        if (!StringUtils.hasText(normalizedAadhaar)) {
            return false;
        }
        if (matchesCurrentPreOnboardingValue(
                currentPreOnboardingId,
                normalizedAadhaar,
                this::normalizeAadhaar,
                AgencyCandidatePreOnboardingEntity::getAadhaarNumber)) {
            return false;
        }

        boolean duplicateInPreOnboarding = preOnboardingRepository.existsByAadhaarNumberExcludingPreOnboardingId(
                normalizedAadhaar,
                currentPreOnboardingId);
        boolean duplicateInEmployees = employeeRepository
                .existsByNormalizedAadhaarNumberExcludingPreOnboardingId(normalizedAadhaar, currentPreOnboardingId);
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

    public boolean isPanDuplicate(Long currentPreOnboardingId, String panNumber) {
        String normalizedPan = normalizePan(panNumber);
        if (!StringUtils.hasText(normalizedPan)) {
            return false;
        }
        if (matchesCurrentPreOnboardingValue(
                currentPreOnboardingId,
                normalizedPan,
                this::normalizePan,
                AgencyCandidatePreOnboardingEntity::getPanNumber)) {
            return false;
        }

        boolean duplicateInPreOnboarding = preOnboardingRepository.existsByPanNumberExcludingPreOnboardingId(
                normalizedPan,
                currentPreOnboardingId);
        boolean duplicateInEmployees = employeeRepository
                .existsByNormalizedPanNumberExcludingPreOnboardingId(normalizedPan, currentPreOnboardingId);
        if (duplicateInPreOnboarding || duplicateInEmployees) {
            log.warn(
                    "Duplicate PAN detected during onboarding validation. preOnboardingId={}, maskedPan={}, duplicateInPreOnboarding={}, duplicateInEmployees={}",
                    currentPreOnboardingId,
                    maskPan(normalizedPan),
                    duplicateInPreOnboarding,
                    duplicateInEmployees);
            return true;
        }

        return false;
    }

    public boolean isEmailDuplicate(Long currentPreOnboardingId, String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedEmail)) {
            return false;
        }
        if (matchesCurrentPreOnboardingValue(
                currentPreOnboardingId,
                normalizedEmail,
                this::normalizeEmail,
                AgencyCandidatePreOnboardingEntity::getCandidateEmail)) {
            return false;
        }

        boolean duplicateInPreOnboarding = preOnboardingRepository.existsByCandidateEmailExcludingPreOnboardingId(
                normalizedEmail,
                currentPreOnboardingId);
        boolean duplicateInEmployees = employeeRepository
                .existsByNormalizedEmailExcludingPreOnboardingId(normalizedEmail, currentPreOnboardingId);
        if (duplicateInPreOnboarding || duplicateInEmployees) {
            log.warn(
                    "Duplicate email detected during onboarding validation. preOnboardingId={}, maskedEmail={}, duplicateInPreOnboarding={}, duplicateInEmployees={}",
                    currentPreOnboardingId,
                    maskEmail(normalizedEmail),
                    duplicateInPreOnboarding,
                    duplicateInEmployees);
            return true;
        }

        return false;
    }

    public boolean isMobileDuplicate(Long currentPreOnboardingId, String mobile) {
        String normalizedMobile = normalizeMobile(mobile);
        if (!StringUtils.hasText(normalizedMobile)) {
            return false;
        }
        if (matchesCurrentPreOnboardingValue(
                currentPreOnboardingId,
                normalizedMobile,
                this::normalizeMobile,
                AgencyCandidatePreOnboardingEntity::getCandidateMobile)) {
            return false;
        }

        boolean duplicateInPreOnboarding = preOnboardingRepository.existsByCandidateMobileExcludingPreOnboardingId(
                normalizedMobile,
                currentPreOnboardingId);
        boolean duplicateInEmployees = employeeRepository
                .existsByNormalizedMobileExcludingPreOnboardingId(normalizedMobile, currentPreOnboardingId);
        if (duplicateInPreOnboarding || duplicateInEmployees) {
            log.warn(
                    "Duplicate mobile detected during onboarding validation. preOnboardingId={}, maskedMobile={}, duplicateInPreOnboarding={}, duplicateInEmployees={}",
                    currentPreOnboardingId,
                    maskMobile(normalizedMobile),
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
        if (isPanDuplicate(currentPreOnboardingId, panNumber)) {
            throw new RecruitmentNotificationException("PAN number already exists in the system.");
        }
    }

    private void validateUniqueEmail(Long currentPreOnboardingId, String email) {
        if (isEmailDuplicate(currentPreOnboardingId, email)) {
            throw new RecruitmentNotificationException("Email already exists in the system.");
        }
    }

    private void validateUniqueMobile(Long currentPreOnboardingId, String mobile) {
        if (isMobileDuplicate(currentPreOnboardingId, mobile)) {
            throw new RecruitmentNotificationException("Mobile number already exists in the system.");
        }
    }

    private String normalizeAadhaar(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", "") : null;
    }

    private String normalizePan(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }

    private String normalizeMobile(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", "") : null;
    }

    private boolean matchesCurrentPreOnboardingValue(
            Long currentPreOnboardingId,
            String normalizedSubmittedValue,
            java.util.function.Function<String, String> normalizer,
            java.util.function.Function<AgencyCandidatePreOnboardingEntity, String> fieldExtractor) {
        if (currentPreOnboardingId == null || !StringUtils.hasText(normalizedSubmittedValue)) {
            return false;
        }

        Optional<AgencyCandidatePreOnboardingEntity> currentPreOnboarding = preOnboardingRepository
                .findById(currentPreOnboardingId);
        if (currentPreOnboarding.isEmpty()) {
            return false;
        }

        String currentStoredValue = normalizer.apply(fieldExtractor.apply(currentPreOnboarding.get()));
        return normalizedSubmittedValue.equals(currentStoredValue);
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

    private String maskEmail(String value) {
        if (!StringUtils.hasText(value)) {
            return "****";
        }
        int atIndex = value.indexOf('@');
        if (atIndex <= 1) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(atIndex);
    }

    private String maskMobile(String value) {
        if (!StringUtils.hasText(value) || value.length() < 4) {
            return "****";
        }
        return "XXXXXX" + value.substring(value.length() - 4);
    }
}
