package com.maharecruitment.gov.in.web.service.mobile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.mobile.MobileEmployeeDetails;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@Service
public class MobileEmployeeDetailsService {

    private static final Logger log = LoggerFactory.getLogger(MobileEmployeeDetailsService.class);
    private static final String EMPLOYEE_TYPE_INTERNAL = "INTERNAL";
    private static final String EMPLOYEE_TYPE_EXTERNAL = "EXTERNAL";
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";

    private final EmployeeRepository employeeRepository;
    private final EmployeeReportingMappingRepository reportingMappingRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final DepartmentMstRepository departmentRepository;
    private final FileStorageService fileStorageService;

    public MobileEmployeeDetailsService(
            EmployeeRepository employeeRepository,
            EmployeeReportingMappingRepository reportingMappingRepository,
            SubDepartmentRepository subDepartmentRepository,
            DepartmentMstRepository departmentRepository,
            FileStorageService fileStorageService) {
        this.employeeRepository = employeeRepository;
        this.reportingMappingRepository = reportingMappingRepository;
        this.subDepartmentRepository = subDepartmentRepository;
        this.departmentRepository = departmentRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public MobileEmployeeDetails loadForUser(User user) {
        if (user == null || user.getId() == null) {
            return MobileEmployeeDetails.empty();
        }

        EmployeeEntity employee = employeeRepository.findMobileLoginProfileByUserId(user.getId()).orElse(null);
        if (employee == null) {
            log.debug("Employee profile not found for mobile login userId={}", user.getId());
            return MobileEmployeeDetails.empty();
        }
        if (!isActiveEmployee(employee)) {
            log.debug("Active employee profile not found for mobile login userId={}", user.getId());
            return MobileEmployeeDetails.empty();
        }

        DepartmentInfo departmentInfo = resolveDepartmentInfo(employee);
        SubDepartmentInfo subDepartmentInfo = resolveSubDepartmentInfo(employee);
        String employeeType = normalizeEmployeeType(employee.getRecruitmentType());
        ReportingInfo reportingInfo = resolveReportingInfo(employee, departmentInfo, employeeType);

        return new MobileEmployeeDetails(
                employee.getEmployeeId(),
                textOrNull(employee.getEmployeeCode()),
                textOrNull(employee.getFullName()),
                buildPhotoDataUri(List.of(employee)),
                resolveFaceData(List.of(employee)),
                designationId(employee),
                designationName(employee),
                departmentInfo.id(),
                departmentInfo.name(),
                subDepartmentInfo.id(),
                subDepartmentInfo.name(),
                employeeType,
                reportingInfo.managerId(),
                reportingInfo.managerName(),
                reportingInfo.departmentId(),
                reportingInfo.departmentName());
    }

    private ReportingInfo resolveReportingInfo(
            EmployeeEntity employee,
            DepartmentInfo departmentInfo,
            String employeeType) {
        EmployeeReportingMappingEntity mapping = employee.getEmployeeId() == null
                ? null
                : reportingMappingRepository
                        .findFirstByEmployeeIdOrderByMappingIdDesc(employee.getEmployeeId())
                        .orElse(null);
        EmployeeEntity manager = resolveReportingManager(mapping);
        Long managerId = manager != null ? manager.getEmployeeId() : null;
        String managerName = manager != null ? textOrNull(manager.getFullName()) : null;

        if (EMPLOYEE_TYPE_EXTERNAL.equals(employeeType)) {
            return new ReportingInfo(managerId, managerName, departmentInfo.id(), departmentInfo.name());
        }

        return new ReportingInfo(managerId, managerName, null, null);
    }

    private EmployeeEntity resolveReportingManager(EmployeeReportingMappingEntity mapping) {
        if (mapping == null || mapping.getManagerEmployeeId() == null) {
            return null;
        }

        return employeeRepository.findById(mapping.getManagerEmployeeId()).orElse(null);
    }

    private Long designationId(EmployeeEntity employee) {
        ManpowerDesignationMaster designation = employee.getDesignation();
        return designation != null ? designation.getDesignationId() : null;
    }

    private String designationName(EmployeeEntity employee) {
        ManpowerDesignationMaster designation = employee.getDesignation();
        return designation != null ? textOrNull(designation.getDesignationName()) : null;
    }

    private DepartmentInfo resolveDepartmentInfo(EmployeeEntity employee) {
        DepartmentRegistrationEntity registration = employee.getDepartmentRegistration();
        SubDepartment subDepartment = employee.getSubDepartment();
        DepartmentMst mappedDepartment = subDepartment != null ? subDepartment.getDepartment() : null;

        Long departmentId = firstNonNull(
                registration != null ? registration.getDepartmentId() : null,
                mappedDepartment != null ? mappedDepartment.getDepartmentId() : null,
                registration != null ? registration.getDepartmentRegistrationId() : null);

        String departmentName = firstText(
                registration != null ? registration.getDepartmentName() : null,
                mappedDepartment != null ? mappedDepartment.getDepartmentName() : null);
        if (departmentName == null) {
            departmentName = lookupDepartmentName(departmentId);
        }

        return new DepartmentInfo(departmentId, departmentName);
    }

    private SubDepartmentInfo resolveSubDepartmentInfo(EmployeeEntity employee) {
        SubDepartment subDepartment = employee.getSubDepartment();
        DepartmentRegistrationEntity registration = employee.getDepartmentRegistration();
        Long subDepartmentId = firstNonNull(
                subDepartment != null ? subDepartment.getSubDeptId() : null,
                registration != null ? registration.getSubDeptId() : null);

        String subDepartmentName = firstText(subDepartment != null ? subDepartment.getSubDeptName() : null);
        if (subDepartmentName == null) {
            subDepartmentName = lookupSubDepartmentName(subDepartmentId);
        }

        return new SubDepartmentInfo(subDepartmentId, subDepartmentName);
    }

    private String lookupDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return null;
        }

        return departmentRepository.findById(departmentId)
                .map(DepartmentMst::getDepartmentName)
                .map(this::textOrNull)
                .orElse(null);
    }

    private String lookupSubDepartmentName(Long subDepartmentId) {
        if (subDepartmentId == null) {
            return null;
        }

        return subDepartmentRepository.findById(subDepartmentId)
                .map(SubDepartment::getSubDeptName)
                .map(this::textOrNull)
                .orElse(null);
    }

    private String buildPhotoDataUri(List<EmployeeEntity> profiles) {
        for (EmployeeEntity profile : profiles) {
            String photoDataUri = buildPhotoDataUri(profile);
            if (photoDataUri != null) {
                return photoDataUri;
            }
        }
        return null;
    }

    private String resolveFaceData(List<EmployeeEntity> profiles) {
        for (EmployeeEntity profile : profiles) {
            String faceData = resolveFaceData(profile);
            if (faceData != null) {
                return faceData;
            }
        }
        return null;
    }

    private String resolveFaceData(EmployeeEntity employee) {
        String employeeEmbedding = textOrNull(employee.getEmbedding());
        if (employeeEmbedding != null) {
            return employeeEmbedding;
        }
        AgencyCandidatePreOnboardingEntity preOnboarding = employee.getPreOnboarding();
        return preOnboarding != null ? textOrNull(preOnboarding.getEmbedding()) : null;
    }

    private String buildPhotoDataUri(EmployeeEntity employee) {
        AgencyCandidatePreOnboardingEntity preOnboarding = employee.getPreOnboarding();
        if (preOnboarding == null || !StringUtils.hasText(preOnboarding.getPhotoFilePath())) {
            return null;
        }

        String photoFilePath = preOnboarding.getPhotoFilePath().trim();
        Path path = fileStorageService.resolveManagedPath(photoFilePath).orElse(null);
        if (path == null) {
            log.warn("Skipping unmanaged or missing employee photo path for employeeId={}", employee.getEmployeeId());
            return null;
        }

        try {
            String contentType = resolveImageContentType(path);
            if (!StringUtils.hasText(contentType) || !contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
                log.warn("Skipping non-image employee photo path for employeeId={}", employee.getEmployeeId());
                return null;
            }

            byte[] imageBytes = Files.readAllBytes(path);
            if (imageBytes.length == 0) {
                return null;
            }

            String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
            return "data:" + contentType + ";base64," + encodedImage;
        } catch (IOException | RuntimeException ex) {
            log.warn("Unable to inline employee photo for employeeId={}", employee.getEmployeeId(), ex);
            return null;
        }
    }

    private String resolveImageContentType(Path path) throws IOException {
        String detected = Files.probeContentType(path);
        if (StringUtils.hasText(detected)) {
            return detected.trim().toLowerCase(Locale.ROOT);
        }

        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".gif")) {
            return "image/gif";
        }
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return null;
    }

    private String normalizeEmployeeType(String employeeType) {
        if (!StringUtils.hasText(employeeType)) {
            return null;
        }

        String normalized = employeeType.trim().toUpperCase(Locale.ROOT);
        if (EMPLOYEE_TYPE_INTERNAL.equals(normalized) || EMPLOYEE_TYPE_EXTERNAL.equals(normalized)) {
            return normalized;
        }
        return normalized;
    }

    private boolean isActiveEmployee(EmployeeEntity employee) {
        return employee != null
                && StringUtils.hasText(employee.getStatus())
                && "ACTIVE".equalsIgnoreCase(employee.getStatus().trim());
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            String normalized = textOrNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record DepartmentInfo(Long id, String name) {
    }

    private record SubDepartmentInfo(Long id, String name) {
    }

    private record ReportingInfo(
            Long managerId,
            String managerName,
            Long departmentId,
            String departmentName) {
    }
}
