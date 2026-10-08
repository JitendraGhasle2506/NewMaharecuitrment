package com.maharecruitment.gov.in.web.service.hr.impl;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeRecruitmentType;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeTypeConversionAuditEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeTypeConversionAuditRepository;
import com.maharecruitment.gov.in.web.service.hr.EmployeeCellMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.EmployeeLocationMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.EmployeeTypeConversionService;

@Service
public class EmployeeTypeConversionServiceImpl implements EmployeeTypeConversionService {

    private static final String INTERNAL = EmployeeRecruitmentType.INTERNAL.name();
    private static final String EXTERNAL = EmployeeRecruitmentType.EXTERNAL.name();
    private static final String MANAGER_TYPE_OTHER = "OTHER";
    private static final long NO_DEPARTMENT_AUDIT_ID = 0L;
    private static final String NO_DEPARTMENT_AUDIT_NAME = "Not mapped";
    private static final Set<String> REPORTING_EMPLOYEE_TYPES = Set.of(INTERNAL, "MAHAIT");

    private final EmployeeRepository employeeRepository;
    private final DepartmentMstRepository departmentRepository;
    private final LocationMasterRepository locationRepository;
    private final CellMasterRepository cellRepository;
    private final UserRepository userRepository;
    private final EmployeeReportingMappingRepository reportingMappingRepository;
    private final EmployeeTypeConversionAuditRepository auditRepository;
    private final EmployeeCellMappingPageService cellMappingService;
    private final EmployeeLocationMappingPageService locationMappingService;

    public EmployeeTypeConversionServiceImpl(
            EmployeeRepository employeeRepository,
            DepartmentMstRepository departmentRepository,
            LocationMasterRepository locationRepository,
            CellMasterRepository cellRepository,
            UserRepository userRepository,
            EmployeeReportingMappingRepository reportingMappingRepository,
            EmployeeTypeConversionAuditRepository auditRepository,
            EmployeeCellMappingPageService cellMappingService,
            EmployeeLocationMappingPageService locationMappingService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.cellRepository = cellRepository;
        this.userRepository = userRepository;
        this.reportingMappingRepository = reportingMappingRepository;
        this.auditRepository = auditRepository;
        this.cellMappingService = cellMappingService;
        this.locationMappingService = locationMappingService;
    }

    @Override
    @Transactional
    public ConversionResult convert(ConversionCommand command) {
        if (command == null || command.employeeId() == null) {
            throw new IllegalArgumentException("Select an employee to convert.");
        }
        String normalizedTarget = normalizeSupportedType(command.targetType());
        EmployeeEntity employee = employeeRepository.findDetailedByEmployeeId(command.employeeId())
                .orElseThrow(() -> new IllegalArgumentException("The selected employee was not found."));

        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())) {
            throw new IllegalArgumentException("Only active employees can be converted.");
        }

        String sourceType = normalizeSupportedType(employee.getRecruitmentType());
        if (sourceType.equals(normalizedTarget)) {
            throw new IllegalArgumentException("The employee is already an "
                    + normalizedTarget.toLowerCase(Locale.ROOT) + " employee.");
        }

        InternalAssignments internalAssignments = normalizedTarget.equals(INTERNAL)
                ? validateInternalAssignments(command)
                : InternalAssignments.empty();

        DepartmentMst department = resolveDepartment(normalizedTarget, command.departmentId());
        LocationMaster location = resolveLocation(command.locationId());

        employee.setRecruitmentType(normalizedTarget);
        employee.setDepartment(department);
        if (INTERNAL.equals(normalizedTarget)
                || (employee.getSubDepartment() != null
                        && (employee.getSubDepartment().getDepartment() == null
                                || !command.departmentId().equals(
                                        employee.getSubDepartment().getDepartment().getDepartmentId())))) {
            employee.setSubDepartment(null);
        }
        employeeRepository.saveAndFlush(employee);

        if (location != null) {
            locationMappingService.updateMapping(
                    employee.getEmployeeId(),
                    List.of(location.getLocationId()),
                    location.getLocationId(),
                    normalizeActor(command.actorLoginId()));
        }

        if (INTERNAL.equals(normalizedTarget)) {
            cellMappingService.updateMapping(
                    employee.getEmployeeId(), command.cellId(), normalizeActor(command.actorLoginId()));
            saveReportingMapping(employee.getEmployeeId(), internalAssignments);
        } else {
            reportingMappingRepository.deleteByEmployeeId(employee.getEmployeeId());
        }

        saveAudit(employee, sourceType, normalizedTarget, department, location, internalAssignments, command);

        return new ConversionResult(
                employee.getFullName(), sourceType, normalizedTarget,
                department == null ? null : department.getDepartmentName(),
                location == null ? null : locationDisplayName(location));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<ConversionAuditView> recentAudits() {
        return auditRepository.findTop20ByOrderByOccurredAtDescAuditIdDesc().stream()
                .map(audit -> new ConversionAuditView(
                        audit.getEmployeeName(),
                        audit.getEmployeeCode(),
                        audit.getSourceType(),
                        audit.getTargetType(),
                        audit.getDepartmentName(),
                        audit.getLocationName(),
                        audit.getCellName(),
                        audit.getReportingHodName(),
                        audit.getReportingManagerName(),
                        audit.getActorLoginId(),
                        audit.getOccurredAt()))
                .toList();
    }

    private InternalAssignments validateInternalAssignments(ConversionCommand command) {
        if (command.cellId() == null) {
            throw new IllegalArgumentException("Select the cell to map for the internal employee.");
        }
        if (command.reportingHodUserId() == null) {
            throw new IllegalArgumentException("Select the reporting HOD.");
        }
        if (command.reportingManagerEmployeeId() == null) {
            throw new IllegalArgumentException("Select the reporting manager.");
        }

        CellMaster cell = cellRepository.findByCellId(command.cellId())
                .orElseThrow(() -> new IllegalArgumentException("The selected cell was not found."));
        User reportingHod = userRepository.findById(command.reportingHodUserId())
                .orElseThrow(() -> new IllegalArgumentException("The selected reporting HOD was not found."));
        if (!Boolean.TRUE.equals(reportingHod.getActive())) {
            throw new IllegalArgumentException("The selected reporting HOD must have an active user account.");
        }
        EmployeeEntity reportingManager = employeeRepository.findById(command.reportingManagerEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("The selected reporting manager was not found."));
        validateReportingPerson(resolveHodEmployee(reportingHod), "Reporting HOD");
        validateReportingPerson(reportingManager, "Reporting manager");
        if (command.employeeId().equals(reportingManager.getEmployeeId())) {
            throw new IllegalArgumentException("An employee cannot report to themselves.");
        }
        String cellName = cell.getWing() == null
                ? cell.getCellName()
                : cell.getWing().getWingName() + " - " + cell.getCellName();
        return new InternalAssignments(
                cell,
                reportingHod,
                reportingManager,
                MANAGER_TYPE_OTHER,
                cellName);
    }

    private void saveReportingMapping(Long employeeId, InternalAssignments assignments) {
        EmployeeReportingMappingEntity mapping = reportingMappingRepository
                .findFirstByEmployeeIdOrderByMappingIdDesc(employeeId)
                .orElseGet(EmployeeReportingMappingEntity::new);
        mapping.setEmployeeId(employeeId);
        mapping.setHodUserId(assignments.hod().getId());
        mapping.setManagerType(MANAGER_TYPE_OTHER);
        mapping.setManagerEmployeeId(assignments.manager().getEmployeeId());
        mapping.setProjectId(null);
        mapping.setReportingType("PRIMARY");
        reportingMappingRepository.save(mapping);
    }

    private EmployeeEntity resolveHodEmployee(User hodUser) {
        return employeeRepository.findByUser_Id(hodUser.getId())
                .orElseGet(() -> employeeRepository.findActiveOnboardedEmployeesByEmailOrMobile(
                                Set.of(normalize(hodUser.getEmail())),
                                Set.of(normalize(hodUser.getMobileNo())))
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Reporting HOD must be an active Internal or MAHAIT employee.")));
    }

    private void validateReportingPerson(EmployeeEntity employee, String label) {
        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())
                || !REPORTING_EMPLOYEE_TYPES.contains(normalizeSupportedReportingType(employee.getRecruitmentType()))) {
            throw new IllegalArgumentException(label + " must be an active Internal or MAHAIT employee.");
        }
    }

    private void saveAudit(
            EmployeeEntity employee,
            String sourceType,
            String targetType,
            DepartmentMst department,
            LocationMaster location,
            InternalAssignments assignments,
            ConversionCommand command) {
        EmployeeTypeConversionAuditEntity audit = new EmployeeTypeConversionAuditEntity();
        audit.setEmployeeId(employee.getEmployeeId());
        audit.setEmployeeName(employee.getFullName());
        audit.setEmployeeCode(employee.getEmployeeCode());
        audit.setSourceType(sourceType);
        audit.setTargetType(targetType);
        // Keep audit inserts compatible with databases where the legacy NOT NULL
        // constraints have not yet been reconciled. This is audit metadata only;
        // the internal employee's department mapping remains null.
        audit.setDepartmentId(department == null ? NO_DEPARTMENT_AUDIT_ID : department.getDepartmentId());
        audit.setDepartmentName(department == null ? NO_DEPARTMENT_AUDIT_NAME : department.getDepartmentName());
        audit.setLocationId(location == null ? null : location.getLocationId());
        audit.setLocationName(location == null ? null : locationDisplayName(location));
        audit.setActorLoginId(normalizeActor(command.actorLoginId()));
        if (assignments.cell() != null) {
            audit.setCellId(assignments.cell().getCellId());
            audit.setCellName(assignments.cellName());
            audit.setReportingHodUserId(assignments.hod().getId());
            audit.setReportingHodName(assignments.hod().getName());
            audit.setManagerType(assignments.managerType());
            audit.setReportingManagerEmployeeId(assignments.manager().getEmployeeId());
            audit.setReportingManagerName(assignments.manager().getFullName());
        }
        auditRepository.save(audit);
    }

    private String normalizeSupportedReportingType(String type) {
        String normalized = EmployeeRecruitmentType.normalizeOrNull(type);
        return normalized == null ? "" : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private DepartmentMst resolveDepartment(String targetType, Long departmentId) {
        if (INTERNAL.equals(targetType)) {
            return null;
        }
        if (departmentId == null) {
            throw new IllegalArgumentException("Select the department to map for the external employee.");
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("The selected department was not found."));
    }

    private LocationMaster resolveLocation(Long locationId) {
        if (locationId == null) {
            return null;
        }
        LocationMaster location = locationRepository.findByLocationId(locationId)
                .orElseThrow(() -> new IllegalArgumentException("The selected location was not found."));
        if (!"Y".equalsIgnoreCase(location.getActiveFlag())) {
            throw new IllegalArgumentException("Select an active location.");
        }
        return location;
    }

    private String locationDisplayName(LocationMaster location) {
        String address = location.getLocationName();
        String office = location.getOfficeName();
        return StringUtils.hasText(office) ? office.trim() + " - " + address : address;
    }

    private String normalizeActor(String actorLoginId) {
        return StringUtils.hasText(actorLoginId) ? actorLoginId.trim() : "SYSTEM";
    }

    private String normalizeSupportedType(String type) {
        String normalized = EmployeeRecruitmentType.normalizeOrNull(type);
        if (!INTERNAL.equals(normalized) && !EXTERNAL.equals(normalized)) {
            throw new IllegalArgumentException("Employee type must be INTERNAL or EXTERNAL.");
        }
        return normalized;
    }

    private record InternalAssignments(
            CellMaster cell,
            User hod,
            EmployeeEntity manager,
            String managerType,
            String cellName) {

        private static InternalAssignments empty() {
            return new InternalAssignments(null, null, null, null, null);
        }
    }
}
