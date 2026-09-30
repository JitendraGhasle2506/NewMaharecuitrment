package com.maharecruitment.gov.in.web.service.profile.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.auth.util.UserValidationUtil;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.profile.UserProfileForm;
import com.maharecruitment.gov.in.web.service.profile.CommonProfileDetails;
import com.maharecruitment.gov.in.web.service.profile.CommonProfileUpdateResult;
import com.maharecruitment.gov.in.web.service.profile.CommonProfileUpdateService;

@Service
public class CommonProfileUpdateServiceImpl implements CommonProfileUpdateService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AgencyCandidatePreOnboardingRepository preOnboardingRepository;
    private final UserAffiliationService userAffiliationService;

    public CommonProfileUpdateServiceImpl(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            AgencyCandidatePreOnboardingRepository preOnboardingRepository,
            UserAffiliationService userAffiliationService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.preOnboardingRepository = preOnboardingRepository;
        this.userAffiliationService = userAffiliationService;
    }

    @Override
    @Transactional(readOnly = true)
    public CommonProfileDetails getDetails(Long userId) {
        return employeeRepository.findByUser_Id(userId)
                .map(employee -> new CommonProfileDetails(true, employee.getDateOfBirth()))
                .orElseGet(() -> new CommonProfileDetails(false, null));
    }

    @Override
    @Transactional
    public CommonProfileUpdateResult update(String currentEmail, UserProfileForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Profile details are required.");
        }

        User user = userAffiliationService.loadUserByEmail(currentEmail);
        EmployeeEntity employee = employeeRepository.findByUser_Id(user.getId()).orElse(null);
        String email = UserValidationUtil.normalizeEmail(form.getEmail());
        String mobileNo = UserValidationUtil.normalizeOptionalMobile(form.getMobileNo());
        if (!StringUtils.hasText(mobileNo)) {
            throw new IllegalArgumentException("Mobile number is required.");
        }

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(email);
        validateUniqueContact(user, employee, email, mobileNo);

        user.setEmail(email);
        user.setMobileNo(mobileNo);
        User savedUser = userRepository.save(user);

        if (employee != null) {
            employee.setEmail(email);
            employee.setMobile(mobileNo);
            if (form.getDateOfBirth() != null) {
                employee.setDateOfBirth(form.getDateOfBirth());
            }
            employeeRepository.save(employee);
            synchronizePreOnboarding(employee.getPreOnboarding(), email, mobileNo, form);
        }

        userAffiliationService.synchronizeUserProfile(savedUser);
        return new CommonProfileUpdateResult(email, mobileNo, emailChanged);
    }

    private void validateUniqueContact(
            User user,
            EmployeeEntity employee,
            String email,
            String mobileNo) {
        boolean employeeEmailExists = employee == null
                ? employeeRepository.existsByEmailIgnoreCase(email)
                : employeeRepository.existsByEmailIgnoreCaseAndEmployeeIdNot(email, employee.getEmployeeId());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId()) || employeeEmailExists) {
            throw new IllegalArgumentException("Email address is already registered.");
        }
        boolean employeeMobileExists = employee == null
                ? employeeRepository.existsByMobile(mobileNo)
                : employeeRepository.existsByMobileAndEmployeeIdNot(mobileNo, employee.getEmployeeId());
        if (userRepository.existsByMobileNoAndIdNot(mobileNo, user.getId()) || employeeMobileExists) {
            throw new IllegalArgumentException("Mobile number is already registered.");
        }
    }

    private void synchronizePreOnboarding(
            AgencyCandidatePreOnboardingEntity preOnboarding,
            String email,
            String mobileNo,
            UserProfileForm form) {
        if (preOnboarding == null || preOnboarding.getPreOnboardingId() == null) {
            return;
        }
        preOnboarding.setCandidateEmail(email);
        preOnboarding.setCandidateMobile(mobileNo);
        if (form.getDateOfBirth() != null) {
            preOnboarding.setDateOfBirth(form.getDateOfBirth());
        }
        preOnboardingRepository.save(preOnboarding);
    }
}
