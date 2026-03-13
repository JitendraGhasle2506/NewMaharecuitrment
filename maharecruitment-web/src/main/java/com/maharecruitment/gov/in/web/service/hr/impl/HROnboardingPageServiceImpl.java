package com.maharecruitment.gov.in.web.service.hr.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingEmploymentForm;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardingCandidateView;
import com.maharecruitment.gov.in.web.service.hr.HROnboardingPageService;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeListView;

@Service
@Transactional(readOnly = true)
public class HROnboardingPageServiceImpl implements HROnboardingPageService {

    private final AgencyCandidatePreOnboardingRepository preOnboardingRepository;
    private final DepartmentRegistrationRepository departmentRegistrationRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentProjectApplicationRepository projectApplicationRepository;

    public HROnboardingPageServiceImpl(
            AgencyCandidatePreOnboardingRepository preOnboardingRepository,
            DepartmentRegistrationRepository departmentRegistrationRepository,
            SubDepartmentRepository subDepartmentRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            DepartmentProjectApplicationRepository projectApplicationRepository) {
        this.preOnboardingRepository = preOnboardingRepository;
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.subDepartmentRepository = subDepartmentRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.projectApplicationRepository = projectApplicationRepository;
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
    public void saveOnboarding(Long preOnboardingId, AgencyPreOnboardingForm form, String actorEmail) {
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

        entity.setHrOnboardingDate(form.getHrOnboardingDate());
        entity.setHrOnboardingLocation(form.getHrOnboardingLocation().trim());
        entity.setHrVerified(true);
        entity.setHrUserId(user.getId());
        entity.setOnboardedAt(LocalDateTime.now());

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

        var interview = entity.getInterviewDetail();
        var notification = interview.getRecruitmentNotification();

        employee.setAgency(interview.getAgency());
        employee.setDesignation(interview.getDesignationVacancy().getDesignationMst());
        employee.setLevelCode(interview.getDesignationVacancy().getLevelCode());
        employee.setRequestId(notification.getRequestId());

        // Recruitment Type Logic
        String requestId = notification.getRequestId();
        boolean isExternal = requestId != null && requestId.contains("-E");
        employee.setRecruitmentType(isExternal ? "EXTERNAL" : "INTERNAL");

        if (isExternal && notification.getDepartmentProjectApplicationId() != null) {
            projectApplicationRepository.findById(notification.getDepartmentProjectApplicationId())
                    .ifPresent(app -> {
                        if (app.getDepartmentRegistrationId() != null) {
                            departmentRegistrationRepository.findById(app.getDepartmentRegistrationId())
                                    .ifPresent(reg -> {
                                        employee.setDepartmentRegistration(reg);
                                        if (reg.getSubDeptId() != null) {
                                            subDepartmentRepository.findById(reg.getSubDeptId()).ifPresent(employee::setSubDepartment);
                                        }
                                    });
                        }
                    });
        } else if (notification.getDepartmentRegistrationId() != null) {
            departmentRegistrationRepository.findById(notification.getDepartmentRegistrationId())
                    .ifPresent(reg -> {
                        employee.setDepartmentRegistration(reg);
                        if (reg.getSubDeptId() != null) {
                            subDepartmentRepository.findById(reg.getSubDeptId()).ifPresent(employee::setSubDepartment);
                        }
                    });
        }

        employee.setStatus("ACTIVE");
        EmployeeEntity savedEmployee = employeeRepository.save(employee);

        // Generate Employee Code: EMP + padded ID
        savedEmployee.setEmployeeCode("EMP" + String.format("%06d", savedEmployee.getEmployeeId()));
        employeeRepository.save(savedEmployee);
    }

    @Override
    public Page<EmployeeListView> getOnboardedEmployees(String recruitmentType, Pageable pageable) {
        Page<EmployeeEntity> employees;
        if (StringUtils.hasText(recruitmentType) && !"ALL".equalsIgnoreCase(recruitmentType)) {
            employees = employeeRepository.findByRecruitmentType(recruitmentType.toUpperCase(), pageable);
        } else {
            employees = employeeRepository.findAll(pageable);
        }

        List<EmployeeListView> dtos = employees.getContent().stream()
                .map(this::toEmployeeListView)
                .toList();
        
        return new PageImpl<>(dtos, pageable, employees.getTotalElements());
    }

    private EmployeeListView toEmployeeListView(EmployeeEntity entity) {
        String deptName = entity.getDepartmentRegistration() != null ? entity.getDepartmentRegistration().getDepartmentName() : "-";
        String designationName = entity.getDesignation() != null ? entity.getDesignation().getDesignationName() : "-";

        return new EmployeeListView(
                entity.getEmployeeId(),
                entity.getEmployeeCode(),
                entity.getRequestId(),
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
