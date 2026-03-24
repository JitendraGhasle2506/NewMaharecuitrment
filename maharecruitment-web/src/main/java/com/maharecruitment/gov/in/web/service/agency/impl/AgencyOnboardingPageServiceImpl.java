package com.maharecruitment.gov.in.web.service.agency.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.master.repository.ResourceLevelExperienceRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEmploymentEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentDesignationVacancyRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingEmploymentForm;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.service.agency.AgencyOnboardingPageService;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardedEmployeeView;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardingCandidateView;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@Service
@Transactional(readOnly = true)
public class AgencyOnboardingPageServiceImpl implements AgencyOnboardingPageService {

    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^[0-9]{12}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final String DEFAULT_VALUE = "-";

    private final UserRepository userRepository;
    private final AgencyMasterRepository agencyMasterRepository;
    private final RecruitmentInterviewDetailRepository interviewDetailRepository;
    private final AgencyCandidatePreOnboardingRepository preOnboardingRepository;
    private final EmployeeRepository employeeRepository;
    private final RecruitmentDesignationVacancyRepository designationVacancyRepository;
    private final DepartmentRegistrationRepository departmentRegistrationRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final ResourceLevelExperienceRepository resourceLevelExperienceRepository;
    private final FileStorageService fileStorageService;

    public AgencyOnboardingPageServiceImpl(
            UserRepository userRepository,
            AgencyMasterRepository agencyMasterRepository,
            RecruitmentInterviewDetailRepository interviewDetailRepository,
            AgencyCandidatePreOnboardingRepository preOnboardingRepository,
            EmployeeRepository employeeRepository,
            RecruitmentDesignationVacancyRepository designationVacancyRepository,
            DepartmentRegistrationRepository departmentRegistrationRepository,
            SubDepartmentRepository subDepartmentRepository,
            ResourceLevelExperienceRepository resourceLevelExperienceRepository,
            FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.agencyMasterRepository = agencyMasterRepository;
        this.interviewDetailRepository = interviewDetailRepository;
        this.preOnboardingRepository = preOnboardingRepository;
        this.employeeRepository = employeeRepository;
        this.designationVacancyRepository = designationVacancyRepository;
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.subDepartmentRepository = subDepartmentRepository;
        this.resourceLevelExperienceRepository = resourceLevelExperienceRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public AgencyPreOnboardingForm loadPreOnboardingForm(String actorEmail, Long recruitmentInterviewDetailId) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        RecruitmentInterviewDetailEntity candidate = loadSelectedCandidate(recruitmentInterviewDetailId,
                context.agencyId());
        AgencyCandidatePreOnboardingEntity existing = preOnboardingRepository
                .findByInterviewDetailIdAndAgencyIdForForm(recruitmentInterviewDetailId, context.agencyId())
                .orElse(null);

        AgencyPreOnboardingForm form = new AgencyPreOnboardingForm();
        DepartmentInfo departmentInfo = resolveDepartmentInfo(
                candidate.getRecruitmentNotification().getDepartmentRegistrationId());
        BigDecimal minExperienceYears = resolveMinExperienceYears(
                candidate.getDesignationVacancy() != null ? candidate.getDesignationVacancy().getLevelCode() : null);

        form.setPreOnboardingId(existing != null ? existing.getPreOnboardingId() : null);
        form.setRecruitmentInterviewDetailId(candidate.getRecruitmentInterviewDetailId());
        form.setRecruitmentNotificationId(candidate.getRecruitmentNotification() != null
                ? candidate.getRecruitmentNotification().getRecruitmentNotificationId()
                : null);
        form.setRequestId(candidate.getRecruitmentNotification() != null
                ? candidate.getRecruitmentNotification().getRequestId()
                : null);
        form.setProjectName(candidate.getRecruitmentNotification() != null
                && candidate.getRecruitmentNotification().getProjectMst() != null
                        ? candidate.getRecruitmentNotification().getProjectMst().getProjectName()
                        : DEFAULT_VALUE);
        form.setDepartment(departmentInfo.departmentName());
        form.setSubDeptName(departmentInfo.subDepartmentName());
        form.setDesignation(candidate.getDesignationVacancy() != null
                && candidate.getDesignationVacancy().getDesignationMst() != null
                        ? candidate.getDesignationVacancy().getDesignationMst().getDesignationName()
                        : DEFAULT_VALUE);
        form.setLevelCode(
                candidate.getDesignationVacancy() != null ? candidate.getDesignationVacancy().getLevelCode() : null);
        form.setAgencyName(
                candidate.getAgency() != null ? candidate.getAgency().getAgencyName() : context.agencyName());
        form.setMinExperienceYears(minExperienceYears);

        if (existing != null) {
            applyExistingPreOnboarding(form, existing);
        } else {
            form.setName(candidate.getCandidateName());
            form.setEmail(candidate.getCandidateEmail());
            form.setMobile(candidate.getCandidateMobile());
            form.getPreviousEmployments().add(new AgencyPreOnboardingEmploymentForm());
        }

        if (form.getPreviousEmployments().isEmpty()) {
            form.getPreviousEmployments().add(new AgencyPreOnboardingEmploymentForm());
        }

        return form;
    }

    @Override
    @Transactional
    public void savePreOnboarding(String actorEmail, Long recruitmentInterviewDetailId, AgencyPreOnboardingForm form) {
        if (form == null) {
            throw new RecruitmentNotificationException("Pre-onboarding form is required.");
        }

        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        RecruitmentInterviewDetailEntity candidate = loadSelectedCandidate(recruitmentInterviewDetailId,
                context.agencyId());
        AgencyCandidatePreOnboardingEntity existing = preOnboardingRepository
                .findByInterviewDetailIdAndAgencyIdForForm(recruitmentInterviewDetailId, context.agencyId())
                .orElse(null);

        List<NormalizedEmployment> employmentRows = normalizeEmploymentRows(form.getPreviousEmployments());
        BigDecimal minExperienceYears = resolveMinExperienceYears(
                candidate.getDesignationVacancy() != null ? candidate.getDesignationVacancy().getLevelCode() : null);
        ExperienceBreakdown experience = calculateExperience(employmentRows);
        validateForm(form, employmentRows, experience, minExperienceYears);

        List<String> newlyUploadedPaths = new ArrayList<>();
        List<String> replacedPaths = new ArrayList<>();

        try {
            UploadedDocument aadhaarDocument = storeOptionalDocument(
                    form.getAadhaarFile(),
                    "recruitment/agency-pre-onboarding/aadhaar",
                    newlyUploadedPaths);
            UploadedDocument panDocument = storeOptionalDocument(
                    form.getPanFile(),
                    "recruitment/agency-pre-onboarding/pan",
                    newlyUploadedPaths);
            UploadedDocument experienceDocument = storeOptionalDocument(
                    form.getExperienceDoc(),
                    "recruitment/agency-pre-onboarding/experience",
                    newlyUploadedPaths);
            UploadedDocument photoDocument = storeOptionalDocument(
                    form.getUploadImage(),
                    "recruitment/agency-pre-onboarding/photo",
                    newlyUploadedPaths);

            AgencyCandidatePreOnboardingEntity entity = existing != null
                    ? existing
                    : new AgencyCandidatePreOnboardingEntity();

            if (entity.getInterviewDetail() == null) {
                entity.setInterviewDetail(candidate);
            }

            entity.setAgencyUserId(context.userId());
            entity.setCandidateName(form.getName().trim());
            entity.setCandidateEmail(form.getEmail().trim());
            entity.setCandidateMobile(form.getMobile().trim());
            entity.setDateOfBirth(form.getDob());
            entity.setAddress(form.getAddress().trim());
            entity.setJoiningDate(form.getJoiningDate());
            entity.setOnboardingDate(form.getOnboardingDate());
            entity.setAadhaarNumber(form.getAadhaar().trim());
            entity.setPanNumber(form.getPan().trim().toUpperCase());
            entity.setTotalExperienceYears(experience.years());
            entity.setTotalExperienceMonths(experience.months());
            entity.setDocEducationalCert(form.isDocEducationalCert());
            entity.setDocExperienceLetter(form.isDocExperienceLetter());
            entity.setDocRelievingLetter(form.isDocRelievingLetter());
            entity.setDocPayslips(form.isDocPayslips());
            entity.setDocDeclarationForm(form.isDocDeclarationForm());
            entity.setDocNda(form.isDocNda());
            entity.setDocMedicalFitness(form.isDocMedicalFitness());
            entity.setDocAddressProof(form.isDocAddressProof());
            entity.setDocPassportPhoto(form.isDocPassportPhoto());
            entity.setDocAadhaar(form.isDocAadhaar());
            entity.setDocPan(form.isDocPan());
            entity.setAgencyVerified(form.isAgencyFlag());
            entity.setSubmittedAt(LocalDateTime.now());
            entity.replacePreviousEmployments(toEmploymentEntities(employmentRows));

            applyUploadedDocument(
                    aadhaarDocument,
                    entity.getAadhaarFilePath(),
                    replacedPaths,
                    entity::setAadhaarOriginalName,
                    entity::setAadhaarFilePath,
                    entity::setAadhaarFileType,
                    entity::setAadhaarFileSize);
            applyUploadedDocument(
                    panDocument,
                    entity.getPanFilePath(),
                    replacedPaths,
                    entity::setPanOriginalName,
                    entity::setPanFilePath,
                    entity::setPanFileType,
                    entity::setPanFileSize);
            applyUploadedDocument(
                    experienceDocument,
                    entity.getExperienceDocFilePath(),
                    replacedPaths,
                    entity::setExperienceDocOriginalName,
                    entity::setExperienceDocFilePath,
                    entity::setExperienceDocFileType,
                    entity::setExperienceDocFileSize);
            applyUploadedDocument(
                    photoDocument,
                    entity.getPhotoFilePath(),
                    replacedPaths,
                    entity::setPhotoOriginalName,
                    entity::setPhotoFilePath,
                    entity::setPhotoFileType,
                    entity::setPhotoFileSize);

            preOnboardingRepository.save(entity);
            replacedPaths.forEach(fileStorageService::deleteQuietly);
        } catch (RuntimeException ex) {
            newlyUploadedPaths.forEach(fileStorageService::deleteQuietly);
            throw ex;
        }
    }

    @Override
    public List<AgencyOnboardingCandidateView> getOnboardingReadyCandidates(String actorEmail) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        Map<Long, DepartmentInfo> departmentInfoCache = new HashMap<>();

        return preOnboardingRepository.findOnboardingReadyCandidatesByAgency(context.agencyId())
                .stream()
                .map(preOnboarding -> toOnboardingCandidateView(preOnboarding, departmentInfoCache))
                .toList();
    }

    @Override
    public List<AgencyOnboardedEmployeeView> getOnboardedEmployees(String actorEmail) {
        return getEmployeesByStatus(actorEmail, "ACTIVE");
    }

    @Override
    public List<AgencyOnboardedEmployeeView> getEmployeesByStatus(String actorEmail, String status) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase() : "ACTIVE";
        return employeeRepository.findByAgencyAgencyIdAndStatusOrderByOnboardingDateDescEmployeeIdDesc(
                context.agencyId(),
                normalizedStatus)
                .stream()
                .map(this::toOnboardedEmployeeView)
                .toList();
    }

    @Override
    @Transactional
    public void markEmployeeResigned(String actorEmail, Long employeeId, LocalDate resignationDate) {
        if (employeeId == null || employeeId < 1) {
            throw new RecruitmentNotificationException("Employee id is required.");
        }
        if (resignationDate == null) {
            throw new RecruitmentNotificationException("Resignation date is required.");
        }
        if (resignationDate.isAfter(LocalDate.now())) {
            throw new RecruitmentNotificationException("Resignation date cannot be in the future.");
        }

        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        EmployeeEntity employee = employeeRepository.findByEmployeeIdAndAgencyAgencyId(employeeId, context.agencyId())
                .orElseThrow(() -> new RecruitmentNotificationException("Employee not found for this agency."));

        if (employee.getJoiningDate() != null && resignationDate.isBefore(employee.getJoiningDate())) {
            throw new RecruitmentNotificationException("Resignation date cannot be before joining date (" +
                    employee.getJoiningDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
                    + ").");
        }

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
        employee.setResignationDate(resignationDate);
        employeeRepository.save(employee);
    }

    private void applyExistingPreOnboarding(
            AgencyPreOnboardingForm form,
            AgencyCandidatePreOnboardingEntity existing) {
        form.setName(existing.getCandidateName());
        form.setEmail(existing.getCandidateEmail());
        form.setMobile(existing.getCandidateMobile());
        form.setDob(existing.getDateOfBirth());
        form.setAddress(existing.getAddress());
        form.setJoiningDate(existing.getJoiningDate());
        form.setOnboardingDate(existing.getOnboardingDate());
        form.setAadhaar(existing.getAadhaarNumber());
        form.setPan(existing.getPanNumber());
        form.setTotalExperienceYears(existing.getTotalExperienceYears());
        form.setTotalExperienceMonths(existing.getTotalExperienceMonths());
        form.setDocEducationalCert(Boolean.TRUE.equals(existing.getDocEducationalCert()));
        form.setDocExperienceLetter(Boolean.TRUE.equals(existing.getDocExperienceLetter()));
        form.setDocRelievingLetter(Boolean.TRUE.equals(existing.getDocRelievingLetter()));
        form.setDocPayslips(Boolean.TRUE.equals(existing.getDocPayslips()));
        form.setDocDeclarationForm(Boolean.TRUE.equals(existing.getDocDeclarationForm()));
        form.setDocNda(Boolean.TRUE.equals(existing.getDocNda()));
        form.setDocMedicalFitness(Boolean.TRUE.equals(existing.getDocMedicalFitness()));
        form.setDocAddressProof(Boolean.TRUE.equals(existing.getDocAddressProof()));
        form.setDocPassportPhoto(Boolean.TRUE.equals(existing.getDocPassportPhoto()));
        form.setDocAadhaar(Boolean.TRUE.equals(existing.getDocAadhaar()));
        form.setDocPan(Boolean.TRUE.equals(existing.getDocPan()));
        form.setAgencyFlag(Boolean.TRUE.equals(existing.getAgencyVerified()));
        form.setExistingAadhaarFileName(existing.getAadhaarOriginalName());
        form.setExistingAadhaarFilePath(existing.getAadhaarFilePath());
        form.setExistingPanFileName(existing.getPanOriginalName());
        form.setExistingPanFilePath(existing.getPanFilePath());
        form.setExistingExperienceDocFileName(existing.getExperienceDocOriginalName());
        form.setExistingExperienceDocFilePath(existing.getExperienceDocFilePath());
        form.setExistingPhotoFileName(existing.getPhotoOriginalName());
        form.setExistingPhotoFilePath(existing.getPhotoFilePath());

        List<AgencyPreOnboardingEmploymentForm> employmentForms = existing.getPreviousEmployments()
                .stream()
                .map(employment -> {
                    AgencyPreOnboardingEmploymentForm employmentForm = new AgencyPreOnboardingEmploymentForm();
                    employmentForm.setPreOnboardingEmploymentId(employment.getPreOnboardingEmploymentId());
                    employmentForm.setCompanyName(employment.getCompanyName());
                    employmentForm.setDesignation(employment.getDesignation());
                    employmentForm.setStartDate(employment.getStartDate());
                    employmentForm.setEndDate(employment.getEndDate());
                    return employmentForm;
                })
                .toList();
        form.setPreviousEmployments(new ArrayList<>(employmentForms));
    }

    private RecruitmentInterviewDetailEntity loadSelectedCandidate(Long recruitmentInterviewDetailId, Long agencyId) {
        if (recruitmentInterviewDetailId == null || recruitmentInterviewDetailId < 1) {
            throw new RecruitmentNotificationException("Selected candidate id is required.");
        }

        return interviewDetailRepository.findSelectedCandidateByIdAndAgency(recruitmentInterviewDetailId, agencyId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Selected candidate not found or not available for pre-onboarding."));
    }

    private List<NormalizedEmployment> normalizeEmploymentRows(
            List<AgencyPreOnboardingEmploymentForm> employmentForms) {
        List<NormalizedEmployment> normalizedRows = new ArrayList<>();
        if (employmentForms == null || employmentForms.isEmpty()) {
            return normalizedRows;
        }

        for (int index = 0; index < employmentForms.size(); index++) {
            AgencyPreOnboardingEmploymentForm row = employmentForms.get(index);
            if (row == null) {
                continue;
            }

            boolean hasAnyValue = StringUtils.hasText(row.getCompanyName())
                    || StringUtils.hasText(row.getDesignation())
                    || row.getStartDate() != null
                    || row.getEndDate() != null;

            if (!hasAnyValue) {
                continue;
            }

            int rowNumber = index + 1;
            if (!StringUtils.hasText(row.getCompanyName())) {
                throw new RecruitmentNotificationException(
                        "Company name is required for employment row " + rowNumber + ".");
            }
            if (row.getStartDate() == null) {
                throw new RecruitmentNotificationException(
                        "Start date is required for employment row " + rowNumber + ".");
            }
            if (row.getEndDate() == null) {
                throw new RecruitmentNotificationException(
                        "End date is required for employment row " + rowNumber + ".");
            }
            if (row.getEndDate().isBefore(row.getStartDate())) {
                throw new RecruitmentNotificationException(
                        "End date cannot be before start date for employment row " + rowNumber + ".");
            }

            normalizedRows.add(new NormalizedEmployment(
                    rowNumber,
                    row.getCompanyName().trim(),
                    StringUtils.hasText(row.getDesignation()) ? row.getDesignation().trim() : null,
                    row.getStartDate(),
                    row.getEndDate()));
        }

        return normalizedRows;
    }

    private ExperienceBreakdown calculateExperience(List<NormalizedEmployment> employmentRows) {
        int totalMonths = 0;

        for (NormalizedEmployment employment : employmentRows) {
            Period period = Period.between(employment.startDate(), employment.endDate());
            int months = period.getYears() * 12 + period.getMonths();

            if (period.getDays() > 0 || months == 0) {
                months++;
            }

            totalMonths += months;
        }

        return new ExperienceBreakdown(totalMonths / 12, totalMonths % 12, totalMonths);
    }

    private void validateForm(
            AgencyPreOnboardingForm form,
            List<NormalizedEmployment> employmentRows,
            ExperienceBreakdown experience,
            BigDecimal minExperienceYears) {
        if (!StringUtils.hasText(form.getName())) {
            throw new RecruitmentNotificationException("Candidate name is required.");
        }
        if (!StringUtils.hasText(form.getEmail())) {
            throw new RecruitmentNotificationException("Candidate email is required.");
        }
        if (!form.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new RecruitmentNotificationException("Candidate email format is invalid.");
        }
        if (!StringUtils.hasText(form.getMobile())) {
            throw new RecruitmentNotificationException("Candidate mobile is required.");
        }
        if (!form.getMobile().trim().matches("^[0-9]{10,15}$")) {
            throw new RecruitmentNotificationException("Candidate mobile must be 10 to 15 digits.");
        }
        if (form.getDob() == null) {
            throw new RecruitmentNotificationException("Date of birth is required.");
        }
        if (!StringUtils.hasText(form.getAddress())) {
            throw new RecruitmentNotificationException("Address is required.");
        }
        if (form.getJoiningDate() == null) {
            throw new RecruitmentNotificationException("Joining date is required.");
        }
        if (form.getOnboardingDate() == null) {
            throw new RecruitmentNotificationException("Onboarding date is required.");
        }
        if (form.getOnboardingDate().isBefore(form.getJoiningDate())) {
            throw new RecruitmentNotificationException("Onboarding date cannot be before joining date.");
        }
        if (!StringUtils.hasText(form.getAadhaar())) {
            throw new RecruitmentNotificationException("Aadhaar number is required.");
        }
        String aadhaar = form.getAadhaar().trim().replaceAll("\\s+", "");
        if (!AADHAAR_PATTERN.matcher(aadhaar).matches()) {
            throw new RecruitmentNotificationException("Aadhaar number must be 12 digits.");
        }
        if (!StringUtils.hasText(form.getPan())) {
            throw new RecruitmentNotificationException("PAN number is required.");
        }
        String pan = form.getPan().trim().toUpperCase();
        if (!PAN_PATTERN.matcher(pan).matches()) {
            throw new RecruitmentNotificationException("PAN number format is invalid.");
        }
        if (employmentRows.isEmpty()) {
            throw new RecruitmentNotificationException("At least one previous employment entry is required.");
        }
        validateNoOverlappingEmploymentPeriods(employmentRows);
        if (experience.totalMonths() < 1) {
            throw new RecruitmentNotificationException("Previous employment duration must be at least one month.");
        }
        if (!form.isDocEducationalCert()) {
            throw new RecruitmentNotificationException("Educational certificates must be verified.");
        }
        if (!form.isDocExperienceLetter()) {
            throw new RecruitmentNotificationException("Experience letter must be verified.");
        }
        if (!form.isDocMedicalFitness()) {
            throw new RecruitmentNotificationException("Medical fitness certificate must be verified.");
        }
        if (!form.isDocAadhaar()) {
            throw new RecruitmentNotificationException("Aadhaar document must be verified.");
        }
        if (!form.isDocPan()) {
            throw new RecruitmentNotificationException("PAN document must be verified.");
        }
        if (!form.isAgencyFlag()) {
            throw new RecruitmentNotificationException("Agency verification is required before submission.");
        }

        if (minExperienceYears != null) {
            BigDecimal calculatedExperienceYears = BigDecimal.valueOf(experience.totalMonths())
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            if (calculatedExperienceYears.compareTo(minExperienceYears) < 0) {
                throw new RecruitmentNotificationException(
                        "Candidate experience does not meet the minimum level requirement of "
                                + minExperienceYears.stripTrailingZeros().toPlainString()
                                + " year(s).");
            }
        }
    }

    private UploadedDocument storeOptionalDocument(
            MultipartFile file,
            String module,
            List<String> newlyUploadedPaths) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        FileUploadResult uploadResult = fileStorageService.store(file, module);
        newlyUploadedPaths.add(uploadResult.fullPath());
        return new UploadedDocument(
                uploadResult.originalFileName(),
                uploadResult.fullPath(),
                uploadResult.contentType(),
                uploadResult.size());
    }

    private void applyUploadedDocument(
            UploadedDocument document,
            String existingFilePath,
            List<String> replacedPaths,
            Consumer<String> nameConsumer,
            Consumer<String> pathConsumer,
            Consumer<String> typeConsumer,
            Consumer<Long> sizeConsumer) {
        if (document == null) {
            return;
        }

        if (StringUtils.hasText(existingFilePath)) {
            replacedPaths.add(existingFilePath);
        }

        nameConsumer.accept(document.originalName());
        pathConsumer.accept(document.filePath());
        typeConsumer.accept(document.fileType());
        sizeConsumer.accept(document.fileSize());
    }

    private List<AgencyCandidatePreOnboardingEmploymentEntity> toEmploymentEntities(
            List<NormalizedEmployment> employmentRows) {
        List<AgencyCandidatePreOnboardingEmploymentEntity> entities = new ArrayList<>();
        for (NormalizedEmployment employment : employmentRows) {
            AgencyCandidatePreOnboardingEmploymentEntity entity = new AgencyCandidatePreOnboardingEmploymentEntity();
            entity.setCompanyName(employment.companyName());
            entity.setDesignation(employment.designation());
            entity.setStartDate(employment.startDate());
            entity.setEndDate(employment.endDate());
            entities.add(entity);
        }
        return entities;
    }

    private void validateNoOverlappingEmploymentPeriods(List<NormalizedEmployment> employmentRows) {
        List<NormalizedEmployment> sortedRows = new ArrayList<>(employmentRows);
        sortedRows.sort(Comparator
                .comparing(NormalizedEmployment::startDate)
                .thenComparing(NormalizedEmployment::endDate));

        for (int index = 1; index < sortedRows.size(); index++) {
            NormalizedEmployment previous = sortedRows.get(index - 1);
            NormalizedEmployment current = sortedRows.get(index);

            if (!current.startDate().isAfter(previous.endDate())) {
                throw new RecruitmentNotificationException(
                        "Employment row "
                                + current.rowNumber()
                                + " overlaps with employment row "
                                + previous.rowNumber()
                                + ". Please correct the experience dates.");
            }
        }
    }

    private AgencyOnboardingCandidateView toOnboardingCandidateView(
            AgencyCandidatePreOnboardingEntity preOnboarding,
            Map<Long, DepartmentInfo> departmentInfoCache) {
        RecruitmentInterviewDetailEntity candidate = preOnboarding.getInterviewDetail();
        Long departmentRegistrationId = candidate.getRecruitmentNotification() != null
                ? candidate.getRecruitmentNotification().getDepartmentRegistrationId()
                : null;
        DepartmentInfo departmentInfo = departmentInfoCache.computeIfAbsent(
                departmentRegistrationId,
                this::resolveDepartmentInfo);

        return new AgencyOnboardingCandidateView(
                preOnboarding.getPreOnboardingId(),
                candidate.getRecruitmentInterviewDetailId(),
                candidate.getRecruitmentNotification() != null
                        ? candidate.getRecruitmentNotification().getRecruitmentNotificationId()
                        : null,
                candidate.getRecruitmentNotification() != null
                        ? candidate.getRecruitmentNotification().getRequestId()
                        : null,
                candidate.getRecruitmentNotification() != null
                        && candidate.getRecruitmentNotification().getProjectMst() != null
                                ? candidate.getRecruitmentNotification().getProjectMst().getProjectName()
                                : DEFAULT_VALUE,
                departmentInfo.departmentName(),
                departmentInfo.subDepartmentName(),
                preOnboarding.getCandidateName(),
                preOnboarding.getCandidateEmail(),
                preOnboarding.getCandidateMobile(),
                candidate.getDesignationVacancy() != null
                        && candidate.getDesignationVacancy().getDesignationMst() != null
                                ? candidate.getDesignationVacancy().getDesignationMst().getDesignationName()
                                : DEFAULT_VALUE,
                candidate.getDesignationVacancy() != null ? candidate.getDesignationVacancy().getLevelCode() : null,
                preOnboarding.getJoiningDate(),
                preOnboarding.getOnboardingDate(),
                preOnboarding.getSubmittedAt());
    }

    private AgencyOnboardedEmployeeView toOnboardedEmployeeView(EmployeeEntity employee) {
        String projectName = DEFAULT_VALUE;
        if (employee.getPreOnboarding() != null
                && employee.getPreOnboarding().getInterviewDetail() != null
                && employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification() != null
                && employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification()
                        .getProjectMst() != null) {
            projectName = employee.getPreOnboarding().getInterviewDetail().getRecruitmentNotification()
                    .getProjectMst().getProjectName();
        }

        String departmentName = employee.getDepartmentRegistration() != null
                && StringUtils.hasText(employee.getDepartmentRegistration().getDepartmentName())
                        ? employee.getDepartmentRegistration().getDepartmentName()
                        : DEFAULT_VALUE;
        String subDepartmentName = employee.getSubDepartment() != null
                && StringUtils.hasText(employee.getSubDepartment().getSubDeptName())
                        ? employee.getSubDepartment().getSubDeptName()
                        : DEFAULT_VALUE;
        String designationName = employee.getDesignation() != null
                && StringUtils.hasText(employee.getDesignation().getDesignationName())
                        ? employee.getDesignation().getDesignationName()
                        : DEFAULT_VALUE;

        return new AgencyOnboardedEmployeeView(
                employee.getEmployeeId(),
                employee.getEmployeeCode(),
                employee.getRequestId(),
                projectName,
                departmentName,
                subDepartmentName,
                employee.getFullName(),
                employee.getEmail(),
                employee.getMobile(),
                designationName,
                employee.getLevelCode(),
                employee.getJoiningDate(),
                employee.getOnboardingDate(),
                employee.getResignationDate(),
                employee.getStatus());
    }

    private DepartmentInfo resolveDepartmentInfo(Long departmentRegistrationId) {
        if (departmentRegistrationId == null) {
            return new DepartmentInfo(DEFAULT_VALUE, DEFAULT_VALUE);
        }

        Optional<DepartmentRegistrationEntity> registrationOptional = departmentRegistrationRepository.findById(
                departmentRegistrationId);
        if (registrationOptional.isEmpty()) {
            return new DepartmentInfo(DEFAULT_VALUE, DEFAULT_VALUE);
        }

        DepartmentRegistrationEntity registration = registrationOptional.get();
        String departmentName = StringUtils.hasText(registration.getDepartmentName())
                ? registration.getDepartmentName()
                : DEFAULT_VALUE;
        String subDepartmentName = registration.getSubDeptId() == null
                ? DEFAULT_VALUE
                : subDepartmentRepository.findById(registration.getSubDeptId())
                        .map(value -> StringUtils.hasText(value.getSubDeptName()) ? value.getSubDeptName()
                                : DEFAULT_VALUE)
                        .orElse(DEFAULT_VALUE);

        return new DepartmentInfo(departmentName, subDepartmentName);
    }

    private BigDecimal resolveMinExperienceYears(String levelCode) {
        if (!StringUtils.hasText(levelCode)) {
            return null;
        }

        return resourceLevelExperienceRepository.findByLevelCodeIgnoreCaseAndActiveFlagIgnoreCase(levelCode, "Y")
                .map(level -> level.getMinExperience())
                .orElse(null);
    }

    private AgencyUserContext resolveAgencyUserContext(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }

        User user = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new RecruitmentNotificationException("Authenticated user not found."));

        AgencyMaster agency = agencyMasterRepository.findByOfficialEmailIgnoreCase(user.getEmail())
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "No agency profile is linked with this login user."));

        return new AgencyUserContext(user.getId(), agency.getAgencyId(), agency.getAgencyName());
    }

    private record AgencyUserContext(Long userId, Long agencyId, String agencyName) {
    }

    private record DepartmentInfo(String departmentName, String subDepartmentName) {
    }

    private record NormalizedEmployment(
            int rowNumber,
            String companyName,
            String designation,
            LocalDate startDate,
            LocalDate endDate) {
    }

    private record ExperienceBreakdown(int years, int months, int totalMonths) {
    }

    private record UploadedDocument(
            String originalName,
            String filePath,
            String fileType,
            Long fileSize) {
    }
}
