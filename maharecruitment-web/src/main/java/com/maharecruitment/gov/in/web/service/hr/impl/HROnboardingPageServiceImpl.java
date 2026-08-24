package com.maharecruitment.gov.in.web.service.hr.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.auth.dto.UserUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserManagementService;
import com.maharecruitment.gov.in.auth.util.SecurePasswordGenerator;
import com.maharecruitment.gov.in.common.util.SensitiveDataMaskingUtil;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeLocationMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentDesignationVacancyRepository;
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeAgencyFilterProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeListProjection;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingEmploymentForm;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeOnboardingResult;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardingCandidateView;
import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeAgencyFilterView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeListView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeOnboardingDetailView;
import com.maharecruitment.gov.in.web.service.onboarding.CandidateIdentityValidationService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;

@Service
@Transactional(readOnly = true)
public class HROnboardingPageServiceImpl implements HROnboardingPageService {

    private static final Logger log = LoggerFactory.getLogger(HROnboardingPageServiceImpl.class);
    private static final String EMPLOYEE_ROLE_NAME = "ROLE_EMPLOYEE";
    private static final String PHOTO_MODULE = "recruitment/agency-pre-onboarding/photo";

    private final AgencyCandidatePreOnboardingRepository preOnboardingRepository;
    private final DepartmentRegistrationRepository departmentRegistrationRepository;
    private final DepartmentMstRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentProjectApplicationRepository projectApplicationRepository;
    private final RecruitmentDesignationVacancyRepository designationVacancyRepository;
    private final UserManagementService userManagementService;
    private final RoleRepository roleRepository;
    private final AccountNotificationService accountNotificationService;
    private final CandidateIdentityValidationService candidateIdentityValidationService;
    private final FileStorageService fileStorageService;
    private final LocationMasterRepository locationMasterRepository;
    private final EmployeeLocationMappingRepository employeeLocationMappingRepository;

    public HROnboardingPageServiceImpl(
            AgencyCandidatePreOnboardingRepository preOnboardingRepository,
            DepartmentRegistrationRepository departmentRegistrationRepository,
            DepartmentMstRepository departmentRepository,
            SubDepartmentRepository subDepartmentRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            DepartmentProjectApplicationRepository projectApplicationRepository,
            RecruitmentDesignationVacancyRepository designationVacancyRepository,
            UserManagementService userManagementService,
            RoleRepository roleRepository,
            AccountNotificationService accountNotificationService,
            CandidateIdentityValidationService candidateIdentityValidationService,
            FileStorageService fileStorageService,
            LocationMasterRepository locationMasterRepository,
            EmployeeLocationMappingRepository employeeLocationMappingRepository) {
        this.preOnboardingRepository = preOnboardingRepository;
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.departmentRepository = departmentRepository;
        this.subDepartmentRepository = subDepartmentRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.designationVacancyRepository = designationVacancyRepository;
        this.userManagementService = userManagementService;
        this.roleRepository = roleRepository;
        this.accountNotificationService = accountNotificationService;
        this.candidateIdentityValidationService = candidateIdentityValidationService;
        this.fileStorageService = fileStorageService;
        this.locationMasterRepository = locationMasterRepository;
        this.employeeLocationMappingRepository = employeeLocationMappingRepository;
    }

    @Override
    public List<AgencyOnboardingCandidateView> getPendingHROnboardingCandidates() {
        Map<Long, String[]> departmentInfoCache = new HashMap<>();
        return preOnboardingRepository.findPendingHROnboarding()
                .stream()
                .map(entity -> toOnboardingCandidateView(entity, departmentInfoCache))
                .toList();
    }

    @Override
    public AgencyPreOnboardingForm loadOnboardingForm(Long preOnboardingId) {
        AgencyCandidatePreOnboardingEntity entity = preOnboardingRepository.findById(preOnboardingId)
                .orElseThrow(() -> new RecruitmentNotificationException("Onboarding record not found."));

        AgencyPreOnboardingForm form = new AgencyPreOnboardingForm();
        form.setPreOnboardingId(entity.getPreOnboardingId());
        form.setHrFlow(true); // MARK AS HR FLOW
        
        // Map common fields
        form.setRecruitmentInterviewDetailId(entity.getInterviewDetail().getRecruitmentInterviewDetailId());
        form.setRecruitmentNotificationId(entity.getInterviewDetail().getRecruitmentNotification().getRecruitmentNotificationId());
        form.setRequestId(entity.getInterviewDetail().getRecruitmentNotification().getRequestId());
        form.setProjectName(entity.getInterviewDetail().getRecruitmentNotification().getProjectMst().getProjectName());
        
        String[] deptInfo = resolveDepartmentInfo(entity.getInterviewDetail().getRecruitmentNotification().getDepartmentRegistrationId());
        form.setDepartment(deptInfo[0]);
        form.setSubDeptName(deptInfo[1]);
        
        form.setDesignation(entity.getInterviewDetail().getDesignationVacancy().getDesignationMst().getDesignationName());
        form.setLevelCode(entity.getInterviewDetail().getDesignationVacancy().getLevelCode());
        form.setAgencyName(entity.getInterviewDetail().getAgency().getAgencyName());
        
        form.setName(entity.getCandidateName());
        form.setEmail(entity.getCandidateEmail());
        form.setMobile(entity.getCandidateMobile());
        form.setDob(entity.getDateOfBirth());
        form.setGender(entity.getGender());
        form.setBloodGroup(entity.getBloodGroup());
        form.setAddress(entity.getAddress());
        form.setEmergencyContactName(entity.getEmergencyContactName());
        form.setEmergencyContactRelation(entity.getEmergencyContactRelation());
        form.setEmergencyContactMobile(entity.getEmergencyContactMobile());
        form.setEmergencyContactAltMobile(entity.getEmergencyContactAltMobile());
        form.setJoiningDate(entity.getJoiningDate());
        form.setOnboardingDate(entity.getOnboardingDate());
        form.setAadhaar(entity.getAadhaarNumber());
        form.setPan(entity.getPanNumber());
        form.setTotalExperienceYears(entity.getTotalExperienceYears());
        form.setTotalExperienceMonths(entity.getTotalExperienceMonths());
        
        // Checklist status (Agency verified)
        form.setDocEducationalCert(entity.getDocEducationalCert());
        form.setDocExperienceLetter(entity.getDocExperienceLetter());
        form.setDocRelievingLetter(entity.getDocRelievingLetter());
        form.setDocPayslips(entity.getDocPayslips());
        form.setDocDeclarationForm(entity.getDocDeclarationForm());
        form.setDocNda(entity.getDocNda());
        form.setDocMedicalFitness(entity.getDocMedicalFitness());
        form.setDocAddressProof(entity.getDocAddressProof());
        form.setDocPassportPhoto(entity.getDocPassportPhoto());
        form.setDocAadhaar(entity.getDocAadhaar());
        form.setDocPan(entity.getDocPan());
        form.setCompanyPayrollMoreThanThreeMonths(Boolean.TRUE.equals(entity.getCompanyPayrollMoreThanThreeMonths()));
        form.setAgencyFlag(entity.getAgencyVerified());

        // Existing files
        form.setExistingAadhaarFileName(entity.getAadhaarOriginalName());
        form.setExistingAadhaarFilePath(entity.getAadhaarFilePath());
        form.setExistingPanFileName(entity.getPanOriginalName());
        form.setExistingPanFilePath(entity.getPanFilePath());
        form.setExistingExperienceDocFileName(entity.getExperienceDocOriginalName());
        form.setExistingExperienceDocFilePath(entity.getExperienceDocFilePath());
        form.setExistingCompanyPayrollProofFileName(entity.getCompanyPayrollProofOriginalName());
        form.setExistingCompanyPayrollProofFilePath(entity.getCompanyPayrollProofFilePath());
        form.setExistingPhotoFileName(entity.getPhotoOriginalName());
        form.setExistingPhotoFilePath(entity.getPhotoFilePath());

        // HR Fields
        form.setHrOnboardingDate(entity.getHrOnboardingDate());
        form.setHrOnboardingLocation(entity.getHrOnboardingLocation());
        form.setHrVerified(entity.getHrVerified());

        // Employment History
        List<AgencyPreOnboardingEmploymentForm> employments = entity.getPreviousEmployments().stream()
                .map(emp -> {
                    AgencyPreOnboardingEmploymentForm f = new AgencyPreOnboardingEmploymentForm();
                    f.setPreOnboardingEmploymentId(emp.getPreOnboardingEmploymentId());
                    f.setCompanyName(emp.getCompanyName());
                    f.setDesignation(emp.getDesignation());
                    f.setStartDate(emp.getStartDate());
                    f.setEndDate(emp.getEndDate());
                    return f;
                }).toList();
        form.setPreviousEmployments(new ArrayList<>(employments));

        return form;
    }

    @Override
    @Transactional
    public EmployeeOnboardingResult saveOnboarding(Long preOnboardingId, AgencyPreOnboardingForm form, String actorEmail) {
        log.info(
                "Processing HR onboarding. preOnboardingId={}, actorEmail={}",
                preOnboardingId,
                actorEmail);
        AgencyCandidatePreOnboardingEntity entity = preOnboardingRepository.findByIdForOnboardingUpdate(preOnboardingId)
                .orElseThrow(() -> new RecruitmentNotificationException("Onboarding record not found."));
        EmployeeEntity existingEmployee = employeeRepository.findByPreOnboardingId(preOnboardingId).orElse(null);

        if (entity.getOnboardedAt() != null) {
            if (existingEmployee == null) {
                throw new RecruitmentNotificationException(
                        "Onboarding is marked complete, but the linked employee record is missing.");
            }
            if (hasCompletedEmployeeAccount(existingEmployee)) {
                log.info(
                        "HR onboarding request is already complete. preOnboardingId={}, employeeId={}",
                        preOnboardingId,
                        existingEmployee.getEmployeeId());
                return existingEmployeeAccountResult(existingEmployee);
            }
            log.warn(
                    "Resuming incomplete employee account setup for completed onboarding marker. preOnboardingId={}, employeeId={}",
                    preOnboardingId,
                    existingEmployee.getEmployeeId());
        }
        if (existingEmployee != null && "RESIGNED".equalsIgnoreCase(existingEmployee.getStatus())) {
            throw new RecruitmentNotificationException(
                    "The linked employee is resigned and cannot be reactivated through onboarding.");
        }

        User user = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new RecruitmentNotificationException("User not found: " + actorEmail));

        if (form.getHrOnboardingDate() == null) {
            throw new RecruitmentNotificationException("HR Onboarding Date is required.");
        }
        if (!StringUtils.hasText(form.getHrOnboardingLocation())) {
            throw new RecruitmentNotificationException("HR Onboarding Location is required.");
        }
        if (!form.isHrVerified()) {
            throw new RecruitmentNotificationException("HR Verification is required.");
        }
        List<LocationMaster> selectedLocations = resolveSelectedEmployeeLocations(form.getSelectedLocationIds());
        validateEmployeeAccountData(form);
        candidateIdentityValidationService.validateUniqueCandidateDetails(
                entity.getPreOnboardingId(),
                entity.getAadhaarNumber(),
                entity.getPanNumber(),
                entity.getCandidateEmail(),
                entity.getCandidateMobile());

        var interview = entity.getInterviewDetail();
        var notification = interview.getRecruitmentNotification();
        if (interview.getDesignationVacancy() == null
                || interview.getDesignationVacancy().getRecruitmentDesignationVacancyId() == null) {
            throw new RecruitmentNotificationException("Candidate vacancy mapping is missing.");
        }

        RecruitmentDesignationVacancyEntity vacancy = designationVacancyRepository.findByIdForFinalDecisionUpdate(
                interview.getDesignationVacancy().getRecruitmentDesignationVacancyId(),
                notification.getRecruitmentNotificationId()).orElseThrow(
                        () -> new RecruitmentNotificationException("Designation vacancy mapping not found."));
        long filledCount = employeeRepository
                .countByPreOnboardingInterviewDetailDesignationVacancyRecruitmentDesignationVacancyIdAndStatusIgnoreCase(
                        interview.getDesignationVacancy().getRecruitmentDesignationVacancyId(),
                        "ACTIVE");
        long vacancyCount = vacancy.getNumberOfVacancy() == null || vacancy.getNumberOfVacancy() < 0
                ? 0L
                : vacancy.getNumberOfVacancy();
        boolean existingEmployeeOccupiesVacancy = existingEmployee != null
                && "ACTIVE".equalsIgnoreCase(existingEmployee.getStatus());
        long resultingFilledCount = filledCount + (existingEmployeeOccupiesVacancy ? 0L : 1L);
        if (resultingFilledCount > vacancyCount) {
            throw new RecruitmentNotificationException(
                    "All vacancies are already filled for this designation and level. This candidate cannot be onboarded.");
        }

        DepartmentRegistrationEntity departmentRegistration = resolveDepartmentRegistration(entity);
        List<String> newlyUploadedPaths = new ArrayList<>();
        List<String> replacedPaths = new ArrayList<>();
        FileUploadResult uploadedPhoto = null;

        try {
            uploadedPhoto = storeOptionalPhoto(form.getUploadImage(), newlyUploadedPaths);

            entity.setHrOnboardingDate(form.getHrOnboardingDate());
            entity.setHrOnboardingLocation(form.getHrOnboardingLocation().trim());
            entity.setHrVerified(true);
            entity.setHrUserId(user.getId());
            entity.setOnboardedAt(LocalDateTime.now());
            vacancy.setFillPost(resultingFilledCount);

            if (uploadedPhoto != null) {
                applyUploadedPhoto(entity, uploadedPhoto, replacedPaths);
            }

            designationVacancyRepository.save(vacancy);
            preOnboardingRepository.save(entity);

            // Create once, or resume a legacy employee row whose HR completion state is incomplete.
            EmployeeEntity employee = existingEmployee != null ? existingEmployee : new EmployeeEntity();
            if (existingEmployee == null) {
                employee.setEmployeeCode(generateTemporaryEmployeeCode());
            }
            employee.setPreOnboarding(entity);
            employee.setFullName(entity.getCandidateName());
            employee.setEmail(entity.getCandidateEmail());
            employee.setMobile(entity.getCandidateMobile());
            employee.setAddress(entity.getAddress());
            employee.setEmergencyContactName(entity.getEmergencyContactName());
            employee.setEmergencyContactRelation(entity.getEmergencyContactRelation());
            employee.setEmergencyContactMobile(entity.getEmergencyContactMobile());
            employee.setEmergencyContactAltMobile(entity.getEmergencyContactAltMobile());
            employee.setDateOfBirth(entity.getDateOfBirth());
            employee.setGender(entity.getGender());
            employee.setBloodGroup(entity.getBloodGroup());
            employee.setJoiningDate(entity.getJoiningDate());
            employee.setOnboardingDate(entity.getHrOnboardingDate());
            employee.setPanNumber(entity.getPanNumber());
            employee.setAadhaarNumber(entity.getAadhaarNumber());
            employee.setCompanyPayrollMoreThanThreeMonths(
                    Boolean.TRUE.equals(entity.getCompanyPayrollMoreThanThreeMonths()));

            employee.setAgency(interview.getAgency());
            employee.setDesignation(interview.getDesignationVacancy().getDesignationMst());
            employee.setLevelCode(interview.getDesignationVacancy().getLevelCode());
            employee.setRequestId(notification.getRequestId());
            applyDepartmentRegistration(employee, departmentRegistration);

            // Recruitment Type Logic
            String requestId = notification.getRequestId();
            boolean isExternal = requestId != null && requestId.contains("-E");
            employee.setRecruitmentType(isExternal ? "EXTERNAL" : "INTERNAL");

            employee.setStatus("ACTIVE");
            EmployeeEntity savedEmployee = employeeRepository.save(employee);

            if (!hasPermanentEmployeeCode(savedEmployee.getEmployeeCode())) {
                savedEmployee.setEmployeeCode("EMP" + String.format("%06d", savedEmployee.getEmployeeId()));
                employeeRepository.save(savedEmployee);
            }

            EmployeeOnboardingResult accountResult = savedEmployee.getUser() == null
                    ? createEmployeeAccessAccount(entity, departmentRegistration, savedEmployee)
                    : existingEmployeeAccountResult(savedEmployee);
            saveMissingEmployeeLocationMappings(savedEmployee, selectedLocations);
            replacedPaths.forEach(fileStorageService::deleteQuietly);
            log.info(
                    "HR onboarding completed successfully. preOnboardingId={}, actorEmail={}, employeeId={}, reusedEmployee={}",
                    preOnboardingId,
                    actorEmail,
                    savedEmployee.getEmployeeId(),
                    existingEmployee != null);
            return accountResult;
        } catch (RuntimeException ex) {
            newlyUploadedPaths.forEach(fileStorageService::deleteQuietly);
            log.warn(
                    "HR onboarding failed. preOnboardingId={}, actorEmail={}, reason={}",
                    preOnboardingId,
                    actorEmail,
                    ex.getMessage());
            throw ex;
        }
    }

    @Override
    public Page<EmployeeListView> getOnboardedEmployees(String recruitmentType, Pageable pageable) {
        return getOnboardedEmployees(recruitmentType, null, pageable);
    }

    @Override
    public Page<EmployeeListView> getEmployeesByStatus(String recruitmentType, String status, Pageable pageable) {
        return getEmployeesByStatus(recruitmentType, status, null, pageable);
    }

    @Override
    public Page<EmployeeListView> getOnboardedEmployees(String recruitmentType, String searchText, Pageable pageable) {
        return getEmployeesByStatus(recruitmentType, "ACTIVE", searchText, pageable);
    }

    @Override
    public Page<EmployeeListView> getEmployeesByStatus(
            String recruitmentType,
            String status,
            String searchText,
            Pageable pageable) {
        return getEmployeesByStatus(recruitmentType, status, searchText, null, pageable);
    }

    @Override
    public Page<EmployeeListView> getEmployeesByStatus(
            String recruitmentType,
            String status,
            String searchText,
            Long agencyId,
            Pageable pageable) {
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase() : "ACTIVE";
        String normalizedRecruitmentType = normalizeRecruitmentType(recruitmentType);
        Long normalizedAgencyId = agencyId != null && agencyId > 0 ? agencyId : null;
        String searchPattern = buildEmployeeSearchPattern(searchText);

        Page<EmployeeListProjection> employees = employeeRepository.findEmployeeListPageByStatusAndFilters(
                normalizedStatus,
                normalizedRecruitmentType,
                normalizedAgencyId,
                searchPattern,
                pageable);
        return employees.map(this::toEmployeeListView);
    }

    @Override
    public List<EmployeeAgencyFilterView> getAgencyFilterOptions(String status) {
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase() : "ACTIVE";
        return employeeRepository.findAgencyFilterOptionsByEmployeeStatus(normalizedStatus)
                .stream()
                .map(this::toEmployeeAgencyFilterView)
                .toList();
    }

    @Override
    public EmployeeOnboardingDetailView loadEmployeeDetail(Long employeeId) {
        if (employeeId == null || employeeId < 1) {
            throw new RecruitmentNotificationException("Valid employee id is required.");
        }

        EmployeeEntity employee = employeeRepository.findDetailedByEmployeeId(employeeId)
                .orElseThrow(() -> new RecruitmentNotificationException("Employee not found."));
        AgencyPreOnboardingForm onboardingForm = employee.getPreOnboarding() != null
                && employee.getPreOnboarding().getPreOnboardingId() != null
                        ? loadOnboardingForm(employee.getPreOnboarding().getPreOnboardingId())
                        : toEmployeeDetailForm(employee);
        onboardingForm.setAadhaar(SensitiveDataMaskingUtil.maskAadhaar(onboardingForm.getAadhaar()));
        return new EmployeeOnboardingDetailView(
                employee.getEmployeeId(),
                employee.getEmployeeCode(),
                employee.getStatus(),
                employee.getRecruitmentType(),
                employee.getResignationDate(),
                onboardingForm);
    }

    @Override
    @Transactional
    public void markEmployeeResigned(Long employeeId) {
        if (employeeId == null || employeeId < 1) {
            throw new RecruitmentNotificationException("Employee id is required.");
        }

        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RecruitmentNotificationException("Employee not found."));
        if ("RESIGNED".equalsIgnoreCase(employee.getStatus())) {
            throw new RecruitmentNotificationException("Employee is already marked as resigned.");
        }
        if (employee.getPreOnboarding() == null
                || employee.getPreOnboarding().getInterviewDetail() == null
                || employee.getPreOnboarding().getInterviewDetail().getDesignationVacancy() == null
                || employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification() == null) {
            throw new RecruitmentNotificationException("Employee vacancy mapping is missing.");
        }

        var interview = employee.getPreOnboarding().getInterviewDetail();
        var vacancy = designationVacancyRepository.findByIdForFinalDecisionUpdate(
                interview.getDesignationVacancy().getRecruitmentDesignationVacancyId(),
                interview.getRecruitmentNotification().getRecruitmentNotificationId()).orElseThrow(
                        () -> new RecruitmentNotificationException("Employee vacancy mapping not found."));
        long filledCount = vacancy.getFillPost() == null || vacancy.getFillPost() < 0 ? 0L : vacancy.getFillPost();
        if (filledCount > 0) {
            vacancy.setFillPost(filledCount - 1);
            designationVacancyRepository.save(vacancy);
        }

        employee.setStatus("RESIGNED");
        employeeRepository.save(employee);
    }

    private boolean hasCompletedEmployeeAccount(EmployeeEntity employee) {
        return employee.getUser() != null
                && hasPermanentEmployeeCode(employee.getEmployeeCode())
                && "ACTIVE".equalsIgnoreCase(employee.getStatus());
    }

    private boolean hasPermanentEmployeeCode(String employeeCode) {
        if (!StringUtils.hasText(employeeCode)) {
            return false;
        }
        String normalizedCode = employeeCode.trim().toUpperCase();
        return !"PENDING".equals(normalizedCode) && !normalizedCode.startsWith("TMP-");
    }

    private EmployeeOnboardingResult existingEmployeeAccountResult(EmployeeEntity employee) {
        User existingUser = employee.getUser();
        if (existingUser == null) {
            throw new RecruitmentNotificationException("Employee account setup is incomplete.");
        }
        String username = StringUtils.hasText(existingUser.getEmail())
                ? existingUser.getEmail()
                : employee.getEmail();
        return new EmployeeOnboardingResult(existingUser.getId(), username, null, null);
    }

    private EmployeeOnboardingResult createEmployeeAccessAccount(
            AgencyCandidatePreOnboardingEntity entity,
            DepartmentRegistrationEntity departmentRegistration,
            EmployeeEntity savedEmployee) {
        Role employeeRole = roleRepository.findByNameIgnoreCase(EMPLOYEE_ROLE_NAME)
                .orElseThrow(() -> new RecruitmentNotificationException("Employee role is not configured."));

        String temporaryPassword = SecurePasswordGenerator.generate(12);
        String employeeCode = savedEmployee.getEmployeeCode();
        if (!StringUtils.hasText(employeeCode)) {
            throw new RecruitmentNotificationException("Employee code could not be generated.");
        }

        UserUpsertRequest request = new UserUpsertRequest();
        request.setName(entity.getCandidateName());
        request.setEmail(entity.getCandidateEmail());
        request.setMobileNo(entity.getCandidateMobile());
        request.setPassword(temporaryPassword);
        request.setDepartmentRegistrationId(
                departmentRegistration != null ? departmentRegistration.getDepartmentRegistrationId() : null);
        request.setAgencyId(null);
        request.setRoleIds(List.of(employeeRole.getId()));

        User createdUser = userManagementService.create(request);
        savedEmployee.setUser(createdUser);
        employeeRepository.save(savedEmployee);

        String notificationWarning = null;
        try {
            accountNotificationService.sendEmployeeCredentials(
                    createdUser.getEmail(),
                    createdUser.getMobileNo(),
                    createdUser.getName(),
                    createdUser.getEmail(),
                    temporaryPassword);
        } catch (RuntimeException ex) {
            log.warn("Employee credential notification failed for userId={}, email={}",
                    createdUser.getId(),
                    createdUser.getEmail(),
                    ex);
            notificationWarning = "Employee account was created, but credential delivery could not be completed. "
                    + "Please share the login details manually.";
        }

        return new EmployeeOnboardingResult(
                createdUser.getId(),
                createdUser.getEmail(),
                temporaryPassword,
                notificationWarning);
    }

    private List<LocationMaster> resolveSelectedEmployeeLocations(List<Long> selectedLocationIds) {
        Set<Long> uniqueLocationIds = new LinkedHashSet<>();
        if (selectedLocationIds != null) {
            selectedLocationIds.stream()
                    .filter(id -> id != null && id > 0)
                    .forEach(uniqueLocationIds::add);
        }
        if (uniqueLocationIds.isEmpty()) {
            throw new RecruitmentNotificationException("Select at least one employee location.");
        }

        List<LocationMaster> locations = locationMasterRepository.findAllById(uniqueLocationIds);
        if (locations.size() != uniqueLocationIds.size()) {
            throw new RecruitmentNotificationException("One or more selected employee locations are invalid.");
        }
        List<LocationMaster> inactiveLocations = locations.stream()
                .filter(location -> !"Y".equalsIgnoreCase(location.getActiveFlag()))
                .toList();
        if (!inactiveLocations.isEmpty()) {
            throw new RecruitmentNotificationException("Inactive employee locations cannot be mapped.");
        }

        Map<Long, LocationMaster> locationById = new HashMap<>();
        locations.forEach(location -> locationById.put(location.getLocationId(), location));
        return uniqueLocationIds.stream()
                .map(locationById::get)
                .toList();
    }

    private void saveMissingEmployeeLocationMappings(EmployeeEntity employee, List<LocationMaster> locations) {
        Set<Long> existingLocationIds = employeeLocationMappingRepository
                .findLocationIdsByEmployeeId(employee.getEmployeeId());
        List<EmployeeLocationMappingEntity> mappings = locations.stream()
                .filter(location -> !existingLocationIds.contains(location.getLocationId()))
                .map(location -> {
                    EmployeeLocationMappingEntity mapping = new EmployeeLocationMappingEntity();
                    mapping.setEmployee(employee);
                    mapping.setLocation(location);
                    return mapping;
                })
                .toList();
        if (!mappings.isEmpty()) {
            employeeLocationMappingRepository.saveAll(mappings);
        }
    }

    private void validateEmployeeAccountData(AgencyPreOnboardingForm form) {
        if (!StringUtils.hasText(form.getName())) {
            throw new RecruitmentNotificationException("Candidate name is required to create the employee account.");
        }
        if (!StringUtils.hasText(form.getEmail())) {
            throw new RecruitmentNotificationException("Candidate email is required to create the employee account.");
        }
        if (!StringUtils.hasText(form.getMobile())) {
            throw new RecruitmentNotificationException("Candidate mobile number is required to create the employee account.");
        }
    }

    private FileUploadResult storeOptionalPhoto(MultipartFile file, List<String> newlyUploadedPaths) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        FileUploadResult uploadResult = fileStorageService.store(file, PHOTO_MODULE);
        newlyUploadedPaths.add(uploadResult.fullPath());
        return uploadResult;
    }

    private void applyUploadedPhoto(
            AgencyCandidatePreOnboardingEntity entity,
            FileUploadResult uploadResult,
            List<String> replacedPaths) {
        if (entity.getPhotoFilePath() != null && !entity.getPhotoFilePath().isBlank()) {
            replacedPaths.add(entity.getPhotoFilePath());
        }

        entity.setPhotoOriginalName(uploadResult.originalFileName());
        entity.setPhotoFilePath(uploadResult.fullPath());
        entity.setPhotoFileType(uploadResult.contentType());
        entity.setPhotoFileSize(uploadResult.size());
    }

    private DepartmentRegistrationEntity resolveDepartmentRegistration(AgencyCandidatePreOnboardingEntity entity) {
        if (entity == null || entity.getInterviewDetail() == null
                || entity.getInterviewDetail().getRecruitmentNotification() == null) {
            log.warn("Pre-onboarding record is missing interview/notification linkage. Proceeding without department mapping.");
            return null;
        }

        var notification = entity.getInterviewDetail().getRecruitmentNotification();
        if (notification.getDepartmentProjectApplicationId() != null) {
            return projectApplicationRepository.findById(notification.getDepartmentProjectApplicationId())
                    .map(app -> {
                        if (app.getDepartmentRegistrationId() == null) {
                            log.warn("Department registration is missing for projectApplicationId={}. Proceeding without department mapping.",
                                    notification.getDepartmentProjectApplicationId());
                            return null;
                        }
                        return departmentRegistrationRepository.findById(app.getDepartmentRegistrationId())
                                .orElseGet(() -> {
                                    log.warn("Department registration not found for id={}. Proceeding without department mapping.",
                                            app.getDepartmentRegistrationId());
                                    return null;
                                });
                    })
                    .orElse(null);
        }

        if (notification.getDepartmentRegistrationId() != null) {
            return departmentRegistrationRepository.findById(notification.getDepartmentRegistrationId())
                    .orElseGet(() -> {
                        log.warn("Department registration not found for id={}. Proceeding without department mapping.",
                                notification.getDepartmentRegistrationId());
                        return null;
                    });
        }

        log.warn("No department registration found for HR onboarding. preOnboardingId={}", entity.getPreOnboardingId());
        return null;
    }

    private void applyDepartmentRegistration(EmployeeEntity employee, DepartmentRegistrationEntity departmentRegistration) {
        if (departmentRegistration == null) {
            employee.setDepartmentRegistration(null);
            employee.setDepartment(null);
            employee.setSubDepartment(null);
            return;
        }

        employee.setDepartmentRegistration(departmentRegistration);
        SubDepartment subDepartment = null;
        if (departmentRegistration.getSubDeptId() != null) {
            subDepartment = subDepartmentRepository.findById(departmentRegistration.getSubDeptId()).orElse(null);
        }
        employee.setSubDepartment(subDepartment);
        employee.setDepartment(resolveDepartment(departmentRegistration, subDepartment));
    }

    private DepartmentMst resolveDepartment(
            DepartmentRegistrationEntity departmentRegistration,
            SubDepartment subDepartment) {
        if (subDepartment != null && subDepartment.getDepartment() != null) {
            return subDepartment.getDepartment();
        }
        if (departmentRegistration == null || departmentRegistration.getDepartmentId() == null) {
            return null;
        }
        return departmentRepository.findById(departmentRegistration.getDepartmentId()).orElse(null);
    }

    private String generateTemporaryEmployeeCode() {
        return "TMP-" + UUID.randomUUID().toString().replace("-", "");
    }

    private EmployeeListView toEmployeeListView(EmployeeListProjection employee) {
        return new EmployeeListView(
                employee.getEmployeeId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                displayValue(employee.getDesignation()),
                employee.getMahaitJoiningDate(),
                employee.getRecruitmentType(),
                displayValue(employee.getAgencyName()),
                employee.getStatus());
    }

    private EmployeeAgencyFilterView toEmployeeAgencyFilterView(EmployeeAgencyFilterProjection agency) {
        return new EmployeeAgencyFilterView(agency.getAgencyId(), agency.getAgencyName());
    }

    private AgencyPreOnboardingForm toEmployeeDetailForm(EmployeeEntity employee) {
        AgencyPreOnboardingForm form = new AgencyPreOnboardingForm();
        form.setHrFlow(true);
        form.setRequestId(employee.getRequestId());
        form.setDepartment(employee.getDepartmentRegistration() != null
                ? employee.getDepartmentRegistration().getDepartmentName()
                : employee.getDepartment() != null ? employee.getDepartment().getDepartmentName() : null);
        form.setSubDeptName(employee.getSubDepartment() != null
                ? employee.getSubDepartment().getSubDeptName()
                : null);
        form.setDesignation(employee.getDesignation() != null
                ? employee.getDesignation().getDesignationName()
                : null);
        form.setLevelCode(employee.getLevelCode());
        form.setAgencyName(employee.getAgency() != null ? employee.getAgency().getAgencyName() : null);
        form.setName(employee.getFullName());
        form.setEmail(employee.getEmail());
        form.setMobile(employee.getMobile());
        form.setDob(employee.getDateOfBirth());
        form.setGender(employee.getGender());
        form.setBloodGroup(employee.getBloodGroup());
        form.setAddress(employee.getAddress());
        form.setEmergencyContactName(employee.getEmergencyContactName());
        form.setEmergencyContactRelation(employee.getEmergencyContactRelation());
        form.setEmergencyContactMobile(employee.getEmergencyContactMobile());
        form.setEmergencyContactAltMobile(employee.getEmergencyContactAltMobile());
        form.setJoiningDate(employee.getJoiningDate());
        form.setOnboardingDate(employee.getOnboardingDate());
        form.setHrOnboardingDate(employee.getOnboardingDate());
        form.setAadhaar(employee.getAadhaarNumber());
        form.setPan(employee.getPanNumber());
        form.setCompanyPayrollMoreThanThreeMonths(
                Boolean.TRUE.equals(employee.getCompanyPayrollMoreThanThreeMonths()));
        form.setExistingPhotoFileName(StringUtils.getFilename(employee.getPhotoPath()));
        form.setExistingPhotoFilePath(employee.getPhotoPath());
        return form;
    }

    private String displayValue(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String normalizeRecruitmentType(String recruitmentType) {
        if (!StringUtils.hasText(recruitmentType) || "ALL".equalsIgnoreCase(recruitmentType)) {
            return null;
        }
        return recruitmentType.trim().toUpperCase();
    }

    private String buildEmployeeSearchPattern(String searchText) {
        if (!StringUtils.hasText(searchText)) {
            return null;
        }
        return "%" + searchText.trim().toUpperCase() + "%";
    }

    private AgencyOnboardingCandidateView toOnboardingCandidateView(
            AgencyCandidatePreOnboardingEntity entity,
            Map<Long, String[]> cache) {
        
        Long deptRegId = entity.getInterviewDetail().getRecruitmentNotification().getDepartmentRegistrationId();
        String[] deptInfo = cache.computeIfAbsent(deptRegId, this::resolveDepartmentInfo);

        return new AgencyOnboardingCandidateView(
                entity.getPreOnboardingId(),
                entity.getInterviewDetail().getRecruitmentInterviewDetailId(),
                entity.getInterviewDetail().getRecruitmentNotification().getRecruitmentNotificationId(),
                entity.getInterviewDetail().getRecruitmentNotification().getRequestId(),
                entity.getInterviewDetail().getRecruitmentNotification().getProjectMst().getProjectName(),
                deptInfo[0],
                deptInfo[1],
                entity.getCandidateName(),
                entity.getCandidateEmail(),
                entity.getCandidateMobile(),
                entity.getInterviewDetail().getDesignationVacancy().getDesignationMst().getDesignationName(),
                entity.getInterviewDetail().getDesignationVacancy().getLevelCode(),
                entity.getJoiningDate(),
                entity.getOnboardingDate(),
                entity.getSubmittedAt()
        );
    }

    private String[] resolveDepartmentInfo(Long departmentRegistrationId) {
        if (departmentRegistrationId == null) return new String[]{"-", "-"};
        
        return departmentRegistrationRepository.findById(departmentRegistrationId)
                .map(reg -> {
                    String dept = StringUtils.hasText(reg.getDepartmentName()) ? reg.getDepartmentName() : "-";
                    String subDept = reg.getSubDeptId() == null ? "-" :
                            subDepartmentRepository.findById(reg.getSubDeptId())
                                    .map(s -> StringUtils.hasText(s.getSubDeptName()) ? s.getSubDeptName() : "-")
                                    .orElse("-");
                    return new String[]{dept, subDept};
                }).orElse(new String[]{"-", "-"});
    }
}
