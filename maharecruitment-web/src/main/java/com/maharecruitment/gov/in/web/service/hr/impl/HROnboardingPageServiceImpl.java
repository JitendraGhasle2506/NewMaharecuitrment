package com.maharecruitment.gov.in.web.service.hr.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentDesignationVacancyRepository;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingEmploymentForm;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeOnboardingResult;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardingCandidateView;
import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeListView;
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
    private final SubDepartmentRepository subDepartmentRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentProjectApplicationRepository projectApplicationRepository;
    private final RecruitmentDesignationVacancyRepository designationVacancyRepository;
    private final UserManagementService userManagementService;
    private final RoleRepository roleRepository;
    private final AccountNotificationService accountNotificationService;
    private final FileStorageService fileStorageService;

    public HROnboardingPageServiceImpl(
            AgencyCandidatePreOnboardingRepository preOnboardingRepository,
            DepartmentRegistrationRepository departmentRegistrationRepository,
            SubDepartmentRepository subDepartmentRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            DepartmentProjectApplicationRepository projectApplicationRepository,
            RecruitmentDesignationVacancyRepository designationVacancyRepository,
            UserManagementService userManagementService,
            RoleRepository roleRepository,
            AccountNotificationService accountNotificationService,
            FileStorageService fileStorageService) {
        this.preOnboardingRepository = preOnboardingRepository;
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.subDepartmentRepository = subDepartmentRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.projectApplicationRepository = projectApplicationRepository;
        this.designationVacancyRepository = designationVacancyRepository;
        this.userManagementService = userManagementService;
        this.roleRepository = roleRepository;
        this.accountNotificationService = accountNotificationService;
        this.fileStorageService = fileStorageService;
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
        form.setAddress(entity.getAddress());
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
        form.setAgencyFlag(entity.getAgencyVerified());

        // Existing files
        form.setExistingAadhaarFileName(entity.getAadhaarOriginalName());
        form.setExistingAadhaarFilePath(entity.getAadhaarFilePath());
        form.setExistingPanFileName(entity.getPanOriginalName());
        form.setExistingPanFilePath(entity.getPanFilePath());
        form.setExistingExperienceDocFileName(entity.getExperienceDocOriginalName());
        form.setExistingExperienceDocFilePath(entity.getExperienceDocFilePath());
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
        AgencyCandidatePreOnboardingEntity entity = preOnboardingRepository.findById(preOnboardingId)
                .orElseThrow(() -> new RecruitmentNotificationException("Onboarding record not found."));

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
        validateEmployeeAccountData(form);
        if (entity.getOnboardedAt() != null) {
            throw new RecruitmentNotificationException("Candidate is already onboarded.");
        }

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
        if (filledCount >= vacancyCount) {
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
            vacancy.setFillPost(filledCount + 1);

            if (uploadedPhoto != null) {
                applyUploadedPhoto(entity, uploadedPhoto, replacedPaths);
            }

            designationVacancyRepository.save(vacancy);
            preOnboardingRepository.save(entity);

            // CREATE EMPLOYEE RECORD
            EmployeeEntity employee = new EmployeeEntity();
            employee.setEmployeeCode("PENDING"); // TEMPORARY PLACEHOLDER TO AVOID NOT-NULL CONSTRAINT
            employee.setPreOnboarding(entity);
            employee.setFullName(entity.getCandidateName());
            employee.setEmail(entity.getCandidateEmail());
            employee.setMobile(entity.getCandidateMobile());
            employee.setAddress(entity.getAddress());
            employee.setDateOfBirth(entity.getDateOfBirth());
            employee.setJoiningDate(entity.getJoiningDate());
            employee.setOnboardingDate(entity.getOnboardingDate());
            employee.setPanNumber(entity.getPanNumber());
            employee.setAadhaarNumber(entity.getAadhaarNumber());

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

            // Generate Employee Code: EMP + padded ID
            savedEmployee.setEmployeeCode("EMP" + String.format("%06d", savedEmployee.getEmployeeId()));
            employeeRepository.save(savedEmployee);

            EmployeeOnboardingResult accountResult = createEmployeeAccessAccount(entity, departmentRegistration,
                    savedEmployee);
            replacedPaths.forEach(fileStorageService::deleteQuietly);
            return accountResult;
        } catch (RuntimeException ex) {
            newlyUploadedPaths.forEach(fileStorageService::deleteQuietly);
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
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase() : "ACTIVE";
        String normalizedRecruitmentType = normalizeRecruitmentType(recruitmentType);
        String searchPattern = buildEmployeeSearchPattern(searchText);

        Page<EmployeeEntity> employees = employeeRepository.findPageByStatusAndFilters(
                normalizedStatus,
                normalizedRecruitmentType,
                searchPattern,
                pageable);

        List<EmployeeListView> dtos = employees.getContent().stream()
                .map(this::toEmployeeListView)
                .toList();
        
        return new PageImpl<>(dtos, pageable, employees.getTotalElements());
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
            employee.setSubDepartment(null);
            return;
        }

        employee.setDepartmentRegistration(departmentRegistration);
        if (departmentRegistration.getSubDeptId() != null) {
            subDepartmentRepository.findById(departmentRegistration.getSubDeptId())
                    .ifPresent(employee::setSubDepartment);
        }
    }

    private EmployeeListView toEmployeeListView(EmployeeEntity entity) {
        String deptName = entity.getDepartmentRegistration() != null ? entity.getDepartmentRegistration().getDepartmentName() : "-";
        String designationName = entity.getDesignation() != null ? entity.getDesignation().getDesignationName() : "-";
        String projectName = "-";
        if (entity.getPreOnboarding() != null
                && entity.getPreOnboarding().getInterviewDetail() != null
                && entity.getPreOnboarding().getInterviewDetail().getRecruitmentNotification() != null
                && entity.getPreOnboarding().getInterviewDetail().getRecruitmentNotification().getProjectMst() != null
                && StringUtils.hasText(entity.getPreOnboarding().getInterviewDetail().getRecruitmentNotification()
                        .getProjectMst().getProjectName())) {
            projectName = entity.getPreOnboarding().getInterviewDetail().getRecruitmentNotification().getProjectMst()
                    .getProjectName();
        }

        return new EmployeeListView(
                entity.getEmployeeId(),
                entity.getEmployeeCode(),
                entity.getRequestId(),
                projectName,
                entity.getFullName(),
                entity.getEmail(),
                entity.getMobile(),
                designationName,
                deptName,
                entity.getJoiningDate(),
                entity.getRecruitmentType(),
                entity.getAgency() != null ? entity.getAgency().getAgencyName() : "-",
                entity.getStatus());
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
