package com.maharecruitment.gov.in.workorder.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.audit.dto.AuditEventView;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.UserService;
import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfile;
import com.maharecruitment.gov.in.common.mahaitprofile.repository.MahaItProfileRepository;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.workorder.dto.WorkOrderForm;
import com.maharecruitment.gov.in.workorder.entity.WorkOrderEmployeeMappingEntity;
import com.maharecruitment.gov.in.workorder.entity.WorkOrderEntity;
import com.maharecruitment.gov.in.workorder.entity.WorkOrderStatus;
import com.maharecruitment.gov.in.workorder.entity.WorkOrderType;
import com.maharecruitment.gov.in.workorder.exception.WorkOrderNotFoundException;
import com.maharecruitment.gov.in.workorder.exception.WorkOrderValidationException;
import com.maharecruitment.gov.in.workorder.repository.WorkOrderEmployeeMappingRepository;
import com.maharecruitment.gov.in.workorder.repository.WorkOrderRepository;
import com.maharecruitment.gov.in.workorder.service.HrWorkOrderService;
import com.maharecruitment.gov.in.workorder.service.WorkOrderDocumentStorageService;
import com.maharecruitment.gov.in.workorder.service.WorkOrderNumberGenerator;
import com.maharecruitment.gov.in.workorder.service.WorkOrderPdfGenerator;
import com.maharecruitment.gov.in.workorder.service.model.EmployeeWorkOrderOptionView;
import com.maharecruitment.gov.in.workorder.service.model.EmployeeWorkOrderSummaryView;
import com.maharecruitment.gov.in.workorder.service.model.GeneratedWorkOrderDocument;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderAgencyOptionView;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderDetailView;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderDocumentContext;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderEmployeeView;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderFormView;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderSummaryView;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderVersionView;

@Service
@Transactional(readOnly = true)
public class HrWorkOrderServiceImpl implements HrWorkOrderService {

    private static final Logger log = LoggerFactory.getLogger(HrWorkOrderServiceImpl.class);
    private static final String MODULE_NAME = "WORK_ORDER";
    private static final String ENTITY_TYPE = "WORK_ORDER";
    private static final String ACTION_GENERATED = "GENERATED";
    private static final String ACTION_EXTENDED = "EXTENDED";
    private static final String ACTION_EMPLOYEE_MAPPED = "EMPLOYEE_MAPPED";
    private static final String ACTION_SUPERSEDED = "SUPERSEDED_BY_EXTENSION";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String RECRUITMENT_TYPE_EXTERNAL = "EXTERNAL";
    private static final String RECRUITMENT_TYPE_INTERNAL = "INTERNAL";
    private static final Long INTERNAL_UNMAPPED_DEPARTMENT_ID = 0L;
    private static final String INTERNAL_UNMAPPED_DEPARTMENT_NAME = "Internal Resources";

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderEmployeeMappingRepository workOrderEmployeeMappingRepository;
    private final EmployeeRepository employeeRepository;
    private final AgencyMasterRepository agencyMasterRepository;
    private final MahaItProfileRepository mahaItProfileRepository;
    private final WorkOrderNumberGenerator workOrderNumberGenerator;
    private final WorkOrderPdfGenerator workOrderPdfGenerator;
    private final WorkOrderDocumentStorageService workOrderDocumentStorageService;
    private final AuditTrailService auditTrailService;
    private final UserService userService;

    public HrWorkOrderServiceImpl(
            WorkOrderRepository workOrderRepository,
            WorkOrderEmployeeMappingRepository workOrderEmployeeMappingRepository,
            EmployeeRepository employeeRepository,
            AgencyMasterRepository agencyMasterRepository,
            MahaItProfileRepository mahaItProfileRepository,
            WorkOrderNumberGenerator workOrderNumberGenerator,
            WorkOrderPdfGenerator workOrderPdfGenerator,
            WorkOrderDocumentStorageService workOrderDocumentStorageService,
            AuditTrailService auditTrailService,
            UserService userService) {
        this.workOrderRepository = workOrderRepository;
        this.workOrderEmployeeMappingRepository = workOrderEmployeeMappingRepository;
        this.employeeRepository = employeeRepository;
        this.agencyMasterRepository = agencyMasterRepository;
        this.mahaItProfileRepository = mahaItProfileRepository;
        this.workOrderNumberGenerator = workOrderNumberGenerator;
        this.workOrderPdfGenerator = workOrderPdfGenerator;
        this.workOrderDocumentStorageService = workOrderDocumentStorageService;
        this.auditTrailService = auditTrailService;
        this.userService = userService;
    }

    @Override
    public Page<WorkOrderSummaryView> getWorkOrders(Pageable pageable) {
        return workOrderRepository.findAllByActiveTrue(pageable).map(this::toSummaryView);
    }

    @Override
    public WorkOrderFormView prepareCreateForm(Long preselectedEmployeeId, Long agencyId, String recruitmentType) {
        EmployeeEntity selectedEmployee = preselectedEmployeeId == null ? null
                : employeeRepository.findDetailedByEmployeeId(preselectedEmployeeId)
                        .orElseThrow(() -> new WorkOrderNotFoundException("Employee not found."));
        ensureEligibleForWorkOrder(selectedEmployee);

        String selectedRecruitmentType = resolveSelectedRecruitmentType(selectedEmployee, recruitmentType);
        Long selectedAgencyId = resolveSelectedAgencyId(selectedEmployee, agencyId);
        List<EmployeeEntity> eligibleEmployees = loadEligibleEmployees(selectedRecruitmentType, selectedAgencyId);

        WorkOrderForm form = new WorkOrderForm();
        form.setRecruitmentType(selectedRecruitmentType);
        form.setAgencyId(selectedAgencyId);
        if (selectedEmployee != null) {
            if (eligibleEmployees.stream().anyMatch(employee -> Objects.equals(
                    employee.getEmployeeId(), selectedEmployee.getEmployeeId()))) {
                form.getEmployeeIds().add(selectedEmployee.getEmployeeId());
            }
            EmployeeCohort cohort = resolveCohort(selectedEmployee);
            form.setSubjectLine(buildDefaultSubject(cohort, WorkOrderType.NEW));
            form.setPurposeSummary(buildDefaultPurposeSummary(cohort, WorkOrderType.NEW));
        } else {
            form.setSubjectLine("Work order for deployment of approved technical resources");
            form.setPurposeSummary("Deployment of approved technical resources as per the HR-approved onboarding roster.");
        }

        return WorkOrderFormView.builder()
                .pageTitle("Generate Work Order")
                .pageSubtitle("Select agency and employee type, then load active employees for whom a work order has not been generated.")
                .extensionMode(false)
                .selectedAgencyId(selectedAgencyId)
                .selectedRecruitmentType(selectedRecruitmentType)
                .form(form)
                .agencyOptions(resolveAgencyOptions())
                .employeeOptions(toEmployeeOptions(eligibleEmployees))
                .build();
    }

    @Override
    public WorkOrderFormView prepareExtensionForm(Long parentWorkOrderId) {
        WorkOrderEntity parentWorkOrder = getManagedWorkOrder(parentWorkOrderId);
        if (!Boolean.TRUE.equals(parentWorkOrder.getLatestVersion())) {
            throw new WorkOrderValidationException("Only the latest work-order version can be extended.");
        }

        EmployeeCohort cohort = EmployeeCohort.from(parentWorkOrder);
        List<EmployeeEntity> activeEmployees = filterByCohort(
                employeeRepository.findByStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(ACTIVE_STATUS),
                cohort);
        Set<Long> activeEmployeeIds = new LinkedHashSet<>();
        activeEmployees.forEach(employee -> activeEmployeeIds.add(employee.getEmployeeId()));

        WorkOrderForm form = new WorkOrderForm();
        form.setParentWorkOrderId(parentWorkOrder.getWorkOrderId());
        form.setAgencyId(parentWorkOrder.getAgencyId());
        form.setRecruitmentType(resolveWorkOrderRecruitmentType(parentWorkOrder));
        form.setWorkOrderType(WorkOrderType.EXTENSION);
        form.setWorkOrderDate(LocalDate.now());
        form.setEffectiveFrom(parentWorkOrder.getEffectiveTo() == null ? LocalDate.now()
                : parentWorkOrder.getEffectiveTo().plusDays(1));
        long previousDays = parentWorkOrder.getEffectiveFrom() != null && parentWorkOrder.getEffectiveTo() != null
                ? Math.max(ChronoUnit.DAYS.between(parentWorkOrder.getEffectiveFrom(), parentWorkOrder.getEffectiveTo()), 30L)
                : 364L;
        form.setEffectiveTo(form.getEffectiveFrom().plusDays(previousDays));
        form.setSubjectLine(buildDefaultSubject(cohort, WorkOrderType.EXTENSION));
        form.setPurposeSummary(parentWorkOrder.getPurposeSummary());
        form.setExtensionReason("Continuity of service delivery for the deployed project resources.");
        form.setEmployeeIds(parentWorkOrder.getEmployeeMappings().stream()
                .map(WorkOrderEmployeeMappingEntity::getEmployeeId)
                .filter(activeEmployeeIds::contains)
                .toList());

        return WorkOrderFormView.builder()
                .pageTitle("Extend Work Order")
                .pageSubtitle("Create the next work-order version for the same deployment cohort.")
                .extensionMode(true)
                .selectedAgencyId(parentWorkOrder.getAgencyId())
                .selectedRecruitmentType(resolveWorkOrderRecruitmentType(parentWorkOrder))
                .form(form)
                .parentWorkOrder(toSummaryView(parentWorkOrder))
                .agencyOptions(List.of())
                .employeeOptions(toEmployeeOptions(activeEmployees))
                .build();
    }

    @Override
    public WorkOrderDetailView previewWorkOrder(WorkOrderForm form) {
        PreparedWorkOrder preparedWorkOrder = prepareValidatedWorkOrder(form);
        WorkOrderEntity parentWorkOrder = preparedWorkOrder.parentWorkOrder();
        EmployeeCohort cohort = preparedWorkOrder.cohort();
        List<WorkOrderEmployeeView> employees = toWorkOrderEmployeeViews(buildEmployeeMappings(
                preparedWorkOrder.selectedEmployees()));

        return WorkOrderDetailView.builder()
                .workOrderNumber("Preview - number will be generated")
                .parentWorkOrderNumber(parentWorkOrder == null ? null : parentWorkOrder.getWorkOrderNumber())
                .parentWorkOrderId(parentWorkOrder == null ? null : parentWorkOrder.getWorkOrderId())
                .rootWorkOrderId(parentWorkOrder == null ? null : parentWorkOrder.getRootWorkOrderId())
                .versionNumber(parentWorkOrder == null ? 1 : safeInt(parentWorkOrder.getVersionNumber(), 1) + 1)
                .extensionSequence(parentWorkOrder == null ? 0 : safeInt(parentWorkOrder.getExtensionSequence(), 0) + 1)
                .workOrderType(form.getWorkOrderType())
                .status(WorkOrderStatus.GENERATED)
                .requestId(cohort.requestId())
                .projectName(cohort.projectName())
                .projectCode(cohort.projectCode())
                .departmentName(cohort.departmentName())
                .subDepartmentName(cohort.subDepartmentName())
                .agencyName(cohort.agencyName())
                .agencyContactName(cohort.agencyContactName())
                .agencyOfficialEmail(cohort.agencyOfficialEmail())
                .agencyAddress(cohort.agencyAddress())
                .referenceNumber("Auto-generated on confirmation")
                .subjectLine(form.getSubjectLine())
                .purposeSummary(form.getPurposeSummary())
                .extensionReason(form.getExtensionReason())
                .workOrderDate(form.getWorkOrderDate())
                .effectiveFrom(form.getEffectiveFrom())
                .effectiveTo(form.getEffectiveTo())
                .documentAvailable(false)
                .employeeCount(employees.size())
                .latestVersion(true)
                .employees(employees)
                .versionHistory(List.of())
                .auditTrail(List.of())
                .build();
    }

    @Override
    @Transactional
    public WorkOrderDetailView generateWorkOrder(WorkOrderForm form, String actorEmail) {
        PreparedWorkOrder preparedWorkOrder = prepareValidatedWorkOrder(form);
        WorkOrderEntity parentWorkOrder = preparedWorkOrder.parentWorkOrder();
        List<EmployeeEntity> selectedEmployees = preparedWorkOrder.selectedEmployees();
        EmployeeCohort cohort = preparedWorkOrder.cohort();

        String workOrderNumber = workOrderNumberGenerator.generate(form.getWorkOrderType(), form.getWorkOrderDate());
        String referenceNumber = workOrderNumber;

        WorkOrderEntity workOrder = WorkOrderEntity.builder()
                .workOrderNumber(workOrderNumber)
                .parentWorkOrderId(parentWorkOrder == null ? null : parentWorkOrder.getWorkOrderId())
                .rootWorkOrderId(parentWorkOrder == null ? null
                        : (parentWorkOrder.getRootWorkOrderId() == null ? parentWorkOrder.getWorkOrderId()
                                : parentWorkOrder.getRootWorkOrderId()))
                .versionNumber(parentWorkOrder == null ? 1 : safeInt(parentWorkOrder.getVersionNumber(), 1) + 1)
                .extensionSequence(parentWorkOrder == null ? 0 : safeInt(parentWorkOrder.getExtensionSequence(), 0) + 1)
                .workOrderType(form.getWorkOrderType())
                .status(WorkOrderStatus.GENERATED)
                .requestId(cohort.requestId())
                .projectName(cohort.projectName())
                .projectCode(cohort.projectCode())
                .departmentRegistrationId(cohort.departmentRegistrationId())
                .departmentName(cohort.departmentName())
                .subDepartmentName(cohort.subDepartmentName())
                .agencyId(cohort.agencyId())
                .agencyName(cohort.agencyName())
                .agencyContactName(cohort.agencyContactName())
                .agencyOfficialEmail(cohort.agencyOfficialEmail())
                .agencyAddress(cohort.agencyAddress())
                .referenceNumber(referenceNumber)
                .subjectLine(form.getSubjectLine())
                .purposeSummary(form.getPurposeSummary())
                .extensionReason(form.getExtensionReason())
                .workOrderDate(form.getWorkOrderDate())
                .effectiveFrom(form.getEffectiveFrom())
                .effectiveTo(form.getEffectiveTo())
                .employeeCount(selectedEmployees.size())
                .latestVersion(true)
                .active(true)
                .build();
        applyAuditMetadata(workOrder, actorEmail);
        workOrder.replaceEmployeeMappings(buildEmployeeMappings(selectedEmployees));

        WorkOrderEntity savedWorkOrder = workOrderRepository.save(workOrder);
        if (savedWorkOrder.getRootWorkOrderId() == null) {
            savedWorkOrder.setRootWorkOrderId(savedWorkOrder.getWorkOrderId());
            savedWorkOrder = workOrderRepository.save(savedWorkOrder);
        }

        GeneratedWorkOrderDocument generatedDocument = workOrderPdfGenerator.generate(buildDocumentContext(
                savedWorkOrder, parentWorkOrder, toWorkOrderEmployeeViews(savedWorkOrder.getEmployeeMappings())));
        savedWorkOrder.setDocumentOriginalName(generatedDocument.originalFileName());
        savedWorkOrder.setDocumentContentType(generatedDocument.contentType());
        savedWorkOrder.setDocumentFileSize(generatedDocument.size());
        savedWorkOrder.setDocumentPath(workOrderDocumentStorageService.store(generatedDocument));
        savedWorkOrder.setUpdatedBy(trimToNull(actorEmail));
        savedWorkOrder.setUpdatedDate(LocalDateTime.now());
        savedWorkOrder = workOrderRepository.save(savedWorkOrder);

        if (parentWorkOrder != null) {
            parentWorkOrder.setLatestVersion(false);
            parentWorkOrder.setUpdatedBy(trimToNull(actorEmail));
            parentWorkOrder.setUpdatedDate(LocalDateTime.now());
            workOrderRepository.save(parentWorkOrder);
            recordAuditEvent(parentWorkOrder.getWorkOrderId(), ACTION_SUPERSEDED, actorEmail,
                    "Work order superseded by extension " + savedWorkOrder.getWorkOrderNumber() + ".",
                    "A new extension version was generated for this work order.",
                    Map.of("extendedByWorkOrderNumber", savedWorkOrder.getWorkOrderNumber()));
        }

        recordAuditEvent(savedWorkOrder.getWorkOrderId(),
                form.getWorkOrderType() == WorkOrderType.EXTENSION ? ACTION_EXTENDED : ACTION_GENERATED,
                actorEmail,
                "Work order " + savedWorkOrder.getWorkOrderNumber() + " generated for " + selectedEmployees.size()
                        + " employee(s).",
                buildAuditDetails(savedWorkOrder, parentWorkOrder),
                Map.of("workOrderType", savedWorkOrder.getWorkOrderType().name(),
                        "requestId", defaultText(savedWorkOrder.getRequestId(), "-"),
                        "employeeCount", savedWorkOrder.getEmployeeCount()));

        for (WorkOrderEmployeeMappingEntity mapping : savedWorkOrder.getEmployeeMappings()) {
            recordAuditEvent(
                    savedWorkOrder.getWorkOrderId(), ACTION_EMPLOYEE_MAPPED, actorEmail,
                    "Employee " + defaultText(mapping.getEmployeeCode(), "-") + " mapped to work order.",
                    "Mapped employee " + defaultText(mapping.getEmployeeName(), "-") + " ("
                            + defaultText(mapping.getEmployeeCode(), "-") + ") to work order "
                            + savedWorkOrder.getWorkOrderNumber() + ".",
                    Map.of("employeeId", mapping.getEmployeeId(),
                            "employeeCode", defaultText(mapping.getEmployeeCode(), "-"),
                            "employeeName", defaultText(mapping.getEmployeeName(), "-")));
        }

        log.info("Work order generated. workOrderId={}, workOrderNumber={}, employeeCount={}",
                savedWorkOrder.getWorkOrderId(), savedWorkOrder.getWorkOrderNumber(), savedWorkOrder.getEmployeeCount());
        return getWorkOrderDetail(savedWorkOrder.getWorkOrderId());
    }

    @Override
    public WorkOrderDetailView getWorkOrderDetail(Long workOrderId) {
        WorkOrderEntity workOrder = getManagedWorkOrder(workOrderId);
        ActorDetails createdActor = resolveActorDetails(workOrder.getCreatedBy());
        ActorDetails updatedActor = resolveActorDetails(workOrder.getUpdatedBy());
        Long rootWorkOrderId = workOrder.getRootWorkOrderId() == null ? workOrder.getWorkOrderId()
                : workOrder.getRootWorkOrderId();

        List<WorkOrderVersionView> versionHistory = workOrderRepository
                .findByRootWorkOrderIdOrderByVersionNumberAscWorkOrderIdAsc(rootWorkOrderId).stream()
                .map(version -> WorkOrderVersionView.builder()
                        .workOrderId(version.getWorkOrderId())
                        .workOrderNumber(version.getWorkOrderNumber())
                        .workOrderType(version.getWorkOrderType())
                        .versionNumber(version.getVersionNumber())
                        .effectiveFrom(version.getEffectiveFrom())
                        .effectiveTo(version.getEffectiveTo())
                        .latestVersion(Boolean.TRUE.equals(version.getLatestVersion()))
                        .build())
                .toList();

        List<AuditEventView> auditTrail = auditTrailService.getTimeline(MODULE_NAME, ENTITY_TYPE,
                String.valueOf(workOrder.getWorkOrderId()));

        return WorkOrderDetailView.builder()
                .workOrderId(workOrder.getWorkOrderId())
                .workOrderNumber(workOrder.getWorkOrderNumber())
                .parentWorkOrderNumber(resolveParentWorkOrderNumber(workOrder.getParentWorkOrderId()))
                .parentWorkOrderId(workOrder.getParentWorkOrderId())
                .rootWorkOrderId(rootWorkOrderId)
                .versionNumber(workOrder.getVersionNumber())
                .extensionSequence(workOrder.getExtensionSequence())
                .workOrderType(workOrder.getWorkOrderType())
                .status(workOrder.getStatus())
                .requestId(workOrder.getRequestId())
                .projectName(workOrder.getProjectName())
                .projectCode(workOrder.getProjectCode())
                .departmentName(workOrder.getDepartmentName())
                .subDepartmentName(workOrder.getSubDepartmentName())
                .agencyName(workOrder.getAgencyName())
                .agencyContactName(workOrder.getAgencyContactName())
                .agencyOfficialEmail(workOrder.getAgencyOfficialEmail())
                .agencyAddress(workOrder.getAgencyAddress())
                .referenceNumber(workOrder.getReferenceNumber())
                .subjectLine(workOrder.getSubjectLine())
                .purposeSummary(workOrder.getPurposeSummary())
                .extensionReason(workOrder.getExtensionReason())
                .workOrderDate(workOrder.getWorkOrderDate())
                .effectiveFrom(workOrder.getEffectiveFrom())
                .effectiveTo(workOrder.getEffectiveTo())
                .documentOriginalName(workOrder.getDocumentOriginalName())
                .documentContentType(workOrder.getDocumentContentType())
                .documentAvailable(StringUtils.hasText(workOrder.getDocumentPath()))
                .employeeCount(workOrder.getEmployeeCount())
                .latestVersion(Boolean.TRUE.equals(workOrder.getLatestVersion()))
                .generatedByName(createdActor.displayName())
                .generatedByLoginId(createdActor.loginId())
                .generatedOn(workOrder.getCreatedDate())
                .updatedByName(updatedActor.displayName())
                .updatedByLoginId(updatedActor.loginId())
                .updatedOn(workOrder.getUpdatedDate())
                .employees(toWorkOrderEmployeeViews(workOrder.getEmployeeMappings()))
                .versionHistory(versionHistory)
                .auditTrail(auditTrail)
                .build();
    }

    @Override
    public List<EmployeeWorkOrderSummaryView> getEmployeeWorkOrders(Long employeeId) {
        if (employeeId == null || employeeId < 1L) {
            return List.of();
        }
        return workOrderEmployeeMappingRepository
                .findByEmployeeIdOrderByWorkOrderWorkOrderDateDescWorkOrderWorkOrderIdDesc(employeeId).stream()
                .map(WorkOrderEmployeeMappingEntity::getWorkOrder)
                .filter(Objects::nonNull)
                .map(workOrder -> EmployeeWorkOrderSummaryView.builder()
                        .workOrderId(workOrder.getWorkOrderId())
                        .workOrderNumber(workOrder.getWorkOrderNumber())
                        .workOrderType(workOrder.getWorkOrderType())
                        .workOrderDate(workOrder.getWorkOrderDate())
                        .effectiveFrom(workOrder.getEffectiveFrom())
                        .effectiveTo(workOrder.getEffectiveTo())
                        .latestVersion(Boolean.TRUE.equals(workOrder.getLatestVersion()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public String getWorkOrderDocumentPath(Long workOrderId) {
        WorkOrderEntity workOrder = getManagedWorkOrder(workOrderId);
        regeneratePdfDocument(workOrder);
        if (!StringUtils.hasText(workOrder.getDocumentPath())) {
            throw new WorkOrderNotFoundException("Generated work-order document is unavailable.");
        }
        return workOrder.getDocumentPath();
    }

    private void regeneratePdfDocument(WorkOrderEntity workOrder) {
        WorkOrderEntity parentWorkOrder = workOrder.getParentWorkOrderId() == null ? null
                : workOrderRepository.findDetailedByWorkOrderId(workOrder.getParentWorkOrderId()).orElse(null);
        GeneratedWorkOrderDocument generatedDocument = workOrderPdfGenerator.generate(buildDocumentContext(
                workOrder,
                parentWorkOrder,
                toWorkOrderEmployeeViews(workOrder.getEmployeeMappings())));
        workOrder.setDocumentOriginalName(generatedDocument.originalFileName());
        workOrder.setDocumentContentType(generatedDocument.contentType());
        workOrder.setDocumentFileSize(generatedDocument.size());
        workOrder.setDocumentPath(workOrderDocumentStorageService.store(generatedDocument));
        workOrder.setUpdatedDate(LocalDateTime.now());
        workOrderRepository.save(workOrder);
        log.info("Work-order PDF regenerated for download. workOrderId={}, workOrderNumber={}",
                workOrder.getWorkOrderId(), workOrder.getWorkOrderNumber());
    }

    private PreparedWorkOrder prepareValidatedWorkOrder(WorkOrderForm form) {
        validateForm(form);

        WorkOrderEntity parentWorkOrder = form.getWorkOrderType() == WorkOrderType.EXTENSION
                ? getManagedWorkOrder(form.getParentWorkOrderId())
                : null;
        if (parentWorkOrder != null && !Boolean.TRUE.equals(parentWorkOrder.getLatestVersion())) {
            throw new WorkOrderValidationException("Only the latest work-order version can be extended.");
        }

        List<EmployeeEntity> selectedEmployees = loadEmployees(form.getEmployeeIds());
        selectedEmployees.forEach(this::ensureEligibleForWorkOrder);
        validateNewWorkOrderRecruitmentTypeSelection(form, selectedEmployees);
        validateNewWorkOrderAgencySelection(form, selectedEmployees);
        validateNewWorkOrderEmployeesAreUnmapped(form, selectedEmployees);

        EmployeeCohort cohort = validateAndResolveSharedCohort(selectedEmployees);
        if (parentWorkOrder != null) {
            validateExtensionCohort(parentWorkOrder, cohort);
        }
        applySystemGeneratedFields(form, cohort);

        List<Long> overlappingEmployeeIds = workOrderEmployeeMappingRepository.findEmployeeIdsWithOverlappingWorkOrders(
                selectedEmployees.stream().map(EmployeeEntity::getEmployeeId).toList(),
                form.getEffectiveFrom(),
                form.getEffectiveTo(),
                WorkOrderStatus.CANCELLED);
        if (!overlappingEmployeeIds.isEmpty()) {
            Set<String> employeeCodes = new LinkedHashSet<>();
            selectedEmployees.stream()
                    .filter(employee -> overlappingEmployeeIds.contains(employee.getEmployeeId()))
                    .forEach(employee -> employeeCodes.add(defaultText(
                            employee.getEmployeeCode(),
                            "EMP-" + employee.getEmployeeId())));
            throw new WorkOrderValidationException(
                    "Selected employee(s) already have overlapping work-order coverage: "
                            + String.join(", ", employeeCodes));
        }

        return new PreparedWorkOrder(parentWorkOrder, selectedEmployees, cohort);
    }

    private void applySystemGeneratedFields(WorkOrderForm form, EmployeeCohort cohort) {
        form.setReferenceNumber(null);
        form.setSubjectLine(buildDefaultSubject(cohort, form.getWorkOrderType()));
    }

    private WorkOrderEntity getManagedWorkOrder(Long workOrderId) {
        if (workOrderId == null || workOrderId < 1L) {
            throw new WorkOrderValidationException("Work-order id is required.");
        }
        return workOrderRepository.findDetailedByWorkOrderId(workOrderId)
                .orElseThrow(() -> new WorkOrderNotFoundException("Work order not found."));
    }

    private List<EmployeeEntity> loadEmployees(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new WorkOrderValidationException("Select at least one employee.");
        }
        List<Long> normalizedIds = employeeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedIds.isEmpty()) {
            throw new WorkOrderValidationException("Select at least one employee.");
        }
        List<EmployeeEntity> employees = employeeRepository.findDetailedByEmployeeIdIn(normalizedIds);
        if (employees.size() != normalizedIds.size()) {
            throw new WorkOrderValidationException("One or more selected employees could not be found.");
        }
        return employees;
    }

    private void validateForm(WorkOrderForm form) {
        if (form == null) {
            throw new WorkOrderValidationException("Work-order form data is required.");
        }
        form.setRecruitmentType(normalizeRecruitmentType(form.getRecruitmentType()));
        if (form.getWorkOrderType() == null) {
            throw new WorkOrderValidationException("Work-order type is required.");
        }
        if (form.getWorkOrderDate() == null) {
            throw new WorkOrderValidationException("Work-order date is required.");
        }
        if (form.getEffectiveFrom() == null || form.getEffectiveTo() == null) {
            throw new WorkOrderValidationException("Effective date range is required.");
        }
        if (form.getEffectiveTo().isBefore(form.getEffectiveFrom())) {
            throw new WorkOrderValidationException("Effective-to date cannot be before effective-from date.");
        }
        if (form.getWorkOrderType() == WorkOrderType.EXTENSION && form.getParentWorkOrderId() == null) {
            throw new WorkOrderValidationException("Parent work order is required for extension.");
        }
        if (form.getWorkOrderType() == WorkOrderType.NEW && (form.getAgencyId() == null || form.getAgencyId() < 1L)) {
            throw new WorkOrderValidationException("Select an agency before generating a work order.");
        }
    }

    private void ensureEligibleForWorkOrder(EmployeeEntity employee) {
        if (employee == null) {
            return;
        }
        if (!ACTIVE_STATUS.equalsIgnoreCase(defaultText(employee.getStatus(), ""))) {
            throw new WorkOrderValidationException("Only active deployed employees can be included in a work order.");
        }
        if (!hasSupportedRecruitmentType(employee.getRecruitmentType())) {
            throw new WorkOrderValidationException("Employee " + defaultText(employee.getEmployeeCode(), "-")
                    + " is missing internal/external recruitment type mapping.");
        }
        if (employee.getAgency() == null || employee.getAgency().getAgencyId() == null) {
            throw new WorkOrderValidationException("Employee " + defaultText(employee.getEmployeeCode(), "-")
                    + " is missing agency mapping.");
        }
        if (!isInternalEmployee(employee)
                && (employee.getDepartmentRegistration() == null
                        || employee.getDepartmentRegistration().getDepartmentRegistrationId() == null)) {
            throw new WorkOrderValidationException("Employee " + defaultText(employee.getEmployeeCode(), "-")
                    + " is missing department mapping.");
        }
        if (!StringUtils.hasText(employee.getRequestId())) {
            throw new WorkOrderValidationException("Employee " + defaultText(employee.getEmployeeCode(), "-")
                    + " is missing request mapping.");
        }
    }

    private EmployeeCohort validateAndResolveSharedCohort(List<EmployeeEntity> employees) {
        if (employees == null || employees.isEmpty()) {
            throw new WorkOrderValidationException("Select at least one employee.");
        }
        EmployeeCohort baseline = resolveCohort(employees.get(0));
        for (EmployeeEntity employee : employees) {
            EmployeeCohort candidate = resolveCohort(employee);
            if (!baseline.matches(candidate)) {
                throw new WorkOrderValidationException(
                        "All selected employees must belong to the same request, employee type/agency, and department.");
            }
        }
        return baseline;
    }

    private void validateExtensionCohort(WorkOrderEntity parentWorkOrder, EmployeeCohort cohort) {
        if (!Objects.equals(parentWorkOrder.getAgencyId(), cohort.agencyId())
                || !Objects.equals(parentWorkOrder.getDepartmentRegistrationId(), cohort.departmentRegistrationId())
                || !equalsIgnoreCase(parentWorkOrder.getRequestId(), cohort.requestId())) {
            throw new WorkOrderValidationException(
                    "Extension employees must belong to the same request, employee type/agency, and department as the existing work order.");
        }
    }

    private void validateNewWorkOrderRecruitmentTypeSelection(
            WorkOrderForm form,
            List<EmployeeEntity> selectedEmployees) {
        if (form.getWorkOrderType() != WorkOrderType.NEW) {
            return;
        }

        String selectedRecruitmentType = normalizeRecruitmentType(form.getRecruitmentType());
        boolean hasDifferentRecruitmentType = selectedEmployees.stream()
                .anyMatch(employee -> !selectedRecruitmentType.equals(resolveRecruitmentType(employee)));
        if (hasDifferentRecruitmentType) {
            throw new WorkOrderValidationException(
                    "Selected employees must match the employee type selected on the work-order form.");
        }
    }

    private void validateNewWorkOrderAgencySelection(WorkOrderForm form, List<EmployeeEntity> selectedEmployees) {
        if (form.getWorkOrderType() != WorkOrderType.NEW) {
            return;
        }
        if (form.getAgencyId() == null) {
            return;
        }

        Long selectedAgencyId = form.getAgencyId();
        boolean hasDifferentAgency = selectedEmployees.stream()
                .anyMatch(employee -> employee.getAgency() == null
                        || !Objects.equals(employee.getAgency().getAgencyId(), selectedAgencyId));
        if (hasDifferentAgency) {
            throw new WorkOrderValidationException(
                    "Selected employees must belong to the agency selected on the work-order form.");
        }
    }

    private void validateNewWorkOrderEmployeesAreUnmapped(WorkOrderForm form, List<EmployeeEntity> selectedEmployees) {
        if (form.getWorkOrderType() != WorkOrderType.NEW) {
            return;
        }

        List<Long> mappedEmployeeIds = workOrderEmployeeMappingRepository.findEmployeeIdsWithActiveWorkOrders(
                selectedEmployees.stream().map(EmployeeEntity::getEmployeeId).toList(),
                WorkOrderStatus.CANCELLED);
        if (mappedEmployeeIds.isEmpty()) {
            return;
        }

        Set<String> employeeCodes = new LinkedHashSet<>();
        selectedEmployees.stream()
                .filter(employee -> mappedEmployeeIds.contains(employee.getEmployeeId()))
                .forEach(employee -> employeeCodes.add(defaultText(
                        employee.getEmployeeCode(),
                        "EMP-" + employee.getEmployeeId())));
        throw new WorkOrderValidationException(
                "Work order is already generated for selected employee(s): " + String.join(", ", employeeCodes));
    }

    private EmployeeCohort resolveCohort(EmployeeEntity employee) {
        AgencyMaster agency = employee.getAgency();
        DepartmentRegistrationEntity departmentRegistration = employee.getDepartmentRegistration();
        RecruitmentNotificationEntity notification = resolveNotification(employee);
        ProjectMst project = notification == null ? null : notification.getProjectMst();
        SubDepartment subDepartment = employee.getSubDepartment();
        boolean internalEmployee = isInternalEmployee(employee);
        return new EmployeeCohort(
                agency.getAgencyId(),
                defaultText(agency.getAgencyName(), "Agency"),
                trimToNull(agency.getContactPersonName()),
                trimToNull(agency.getOfficialEmail()),
                trimToNull(agency.getOfficialAddress()),
                resolveDepartmentRegistrationId(departmentRegistration, internalEmployee),
                resolveDepartmentName(departmentRegistration, internalEmployee),
                subDepartment == null ? null : trimToNull(subDepartment.getSubDeptName()),
                defaultText(employee.getRequestId(), "-"),
                project == null ? "Project" : defaultText(project.getProjectName(), "Project"),
                null);
    }

    private List<EmployeeEntity> filterByCohort(List<EmployeeEntity> employees, EmployeeCohort cohort) {
        return employees.stream()
                .filter(employee -> {
                    try {
                        ensureEligibleForWorkOrder(employee);
                        return cohort.matches(resolveCohort(employee));
                    } catch (WorkOrderValidationException ex) {
                        return false;
                    }
                })
                .toList();
    }

    private Long resolveSelectedAgencyId(EmployeeEntity selectedEmployee, Long requestedAgencyId) {
        if (selectedEmployee != null && selectedEmployee.getAgency() != null) {
            return selectedEmployee.getAgency().getAgencyId();
        }
        return requestedAgencyId != null && requestedAgencyId > 0L ? requestedAgencyId : null;
    }

    private String resolveSelectedRecruitmentType(EmployeeEntity selectedEmployee, String requestedRecruitmentType) {
        if (selectedEmployee != null) {
            return resolveRecruitmentType(selectedEmployee);
        }
        return normalizeRecruitmentType(requestedRecruitmentType);
    }

    private String resolveWorkOrderRecruitmentType(WorkOrderEntity workOrder) {
        return RECRUITMENT_TYPE_EXTERNAL;
    }

    private List<EmployeeEntity> loadEligibleEmployees(String recruitmentType, Long selectedAgencyId) {
        String normalizedRecruitmentType = normalizeRecruitmentType(recruitmentType);
        if (selectedAgencyId == null) {
            return List.of();
        }

        return filterEmployeesWithoutGeneratedWorkOrder(
                employeeRepository.findWorkOrderCandidatesByAgencyRecruitmentTypeAndStatus(
                        selectedAgencyId,
                        normalizedRecruitmentType,
                        ACTIVE_STATUS));
    }

    private List<EmployeeEntity> filterEmployeesWithoutGeneratedWorkOrder(List<EmployeeEntity> employees) {
        if (employees == null || employees.isEmpty()) {
            return List.of();
        }

        List<EmployeeEntity> eligibleEmployees = employees.stream()
                .filter(employee -> {
                    try {
                        ensureEligibleForWorkOrder(employee);
                        return true;
                    } catch (WorkOrderValidationException ex) {
                        log.debug("Employee excluded from work-order agency list. employeeId={}, reason={}",
                                employee == null ? null : employee.getEmployeeId(),
                                ex.getMessage());
                        return false;
                    }
                })
                .toList();
        if (eligibleEmployees.isEmpty()) {
            return List.of();
        }

        List<Long> employeeIds = eligibleEmployees.stream()
                .map(EmployeeEntity::getEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (employeeIds.isEmpty()) {
            return List.of();
        }

        Set<Long> mappedEmployeeIds = new LinkedHashSet<>(
                workOrderEmployeeMappingRepository.findEmployeeIdsWithActiveWorkOrders(
                        employeeIds,
                        WorkOrderStatus.CANCELLED));
        return eligibleEmployees.stream()
                .filter(employee -> !mappedEmployeeIds.contains(employee.getEmployeeId()))
                .toList();
    }

    private List<WorkOrderAgencyOptionView> resolveAgencyOptions() {
        return agencyMasterRepository.findByStatusOrderByAgencyNameAsc(AgencyStatus.ACTIVE).stream()
                .map(agency -> WorkOrderAgencyOptionView.builder()
                        .agencyId(agency.getAgencyId())
                        .agencyName(defaultText(agency.getAgencyName(), "Agency-" + agency.getAgencyId()))
                        .build())
                .toList();
    }

    private List<EmployeeWorkOrderOptionView> toEmployeeOptions(List<EmployeeEntity> employees) {
        return employees.stream()
                .sorted((left, right) -> safeLower(left.getFullName()).compareTo(safeLower(right.getFullName())))
                .map(employee -> EmployeeWorkOrderOptionView.builder()
                        .employeeId(employee.getEmployeeId())
                        .employeeCode(employee.getEmployeeCode())
                        .employeeName(employee.getFullName())
                        .requestId(employee.getRequestId())
                        .projectName(resolveProjectName(employee))
                        .agencyName(resolveEmployeeAgencyName(employee))
                        .departmentName(employee.getDepartmentRegistration() != null
                                ? employee.getDepartmentRegistration().getDepartmentName()
                                : "-")
                        .designationName(resolveDesignationName(employee))
                        .levelCode(employee.getLevelCode())
                        .status(employee.getStatus())
                        .joiningDate(employee.getJoiningDate())
                        .build())
                .toList();
    }

    private List<WorkOrderEmployeeMappingEntity> buildEmployeeMappings(List<EmployeeEntity> employees) {
        List<WorkOrderEmployeeMappingEntity> mappings = new ArrayList<>();
        for (EmployeeEntity employee : employees) {
            mappings.add(WorkOrderEmployeeMappingEntity.builder()
                    .employeeId(employee.getEmployeeId())
                    .employeeCode(defaultText(employee.getEmployeeCode(), "EMP-" + employee.getEmployeeId()))
                    .employeeName(defaultText(employee.getFullName(), "Employee"))
                    .designationName(resolveDesignationName(employee))
                    .levelCode(trimToNull(employee.getLevelCode()))
                    .joiningDate(employee.getJoiningDate())
                    .employmentStatus(defaultText(employee.getStatus(), ACTIVE_STATUS))
                    .build());
        }
        return mappings;
    }

    private List<WorkOrderEmployeeView> toWorkOrderEmployeeViews(List<WorkOrderEmployeeMappingEntity> mappings) {
        return mappings == null ? List.of() : mappings.stream()
                .map(mapping -> WorkOrderEmployeeView.builder()
                        .employeeId(mapping.getEmployeeId())
                        .employeeCode(mapping.getEmployeeCode())
                        .employeeName(mapping.getEmployeeName())
                        .designationName(mapping.getDesignationName())
                        .levelCode(mapping.getLevelCode())
                        .joiningDate(mapping.getJoiningDate())
                        .employmentStatus(mapping.getEmploymentStatus())
                        .build())
                .toList();
    }

    private WorkOrderDocumentContext buildDocumentContext(
            WorkOrderEntity workOrder,
            WorkOrderEntity parentWorkOrder,
            List<WorkOrderEmployeeView> employees) {
        MahaItProfile profile = resolveActiveProfile();
        return WorkOrderDocumentContext.builder()
                .workOrderNumber(workOrder.getWorkOrderNumber())
                .parentWorkOrderNumber(parentWorkOrder == null ? null : parentWorkOrder.getWorkOrderNumber())
                .referenceNumber(workOrder.getReferenceNumber())
                .workOrderType(workOrder.getWorkOrderType())
                .workOrderDate(workOrder.getWorkOrderDate())
                .effectiveFrom(workOrder.getEffectiveFrom())
                .effectiveTo(workOrder.getEffectiveTo())
                .subjectLine(workOrder.getSubjectLine())
                .purposeSummary(workOrder.getPurposeSummary())
                .extensionReason(workOrder.getExtensionReason())
                .requestId(workOrder.getRequestId())
                .projectName(workOrder.getProjectName())
                .projectCode(workOrder.getProjectCode())
                .departmentName(workOrder.getDepartmentName())
                .subDepartmentName(workOrder.getSubDepartmentName())
                .agencyName(workOrder.getAgencyName())
                .agencyContactName(workOrder.getAgencyContactName())
                .agencyOfficialEmail(workOrder.getAgencyOfficialEmail())
                .agencyAddress(workOrder.getAgencyAddress())
                .issuedByOrganizationName(profile.getCompanyName())
                .issuedByAddress(profile.getCompanyAddress())
                .employees(employees)
                .build();
    }

    private WorkOrderSummaryView toSummaryView(WorkOrderEntity workOrder) {
        ActorDetails actorDetails = resolveActorDetails(workOrder.getCreatedBy());
        return WorkOrderSummaryView.builder()
                .workOrderId(workOrder.getWorkOrderId())
                .workOrderNumber(workOrder.getWorkOrderNumber())
                .workOrderType(workOrder.getWorkOrderType())
                .status(workOrder.getStatus())
                .requestId(workOrder.getRequestId())
                .projectName(workOrder.getProjectName())
                .agencyName(workOrder.getAgencyName())
                .departmentName(workOrder.getDepartmentName())
                .workOrderDate(workOrder.getWorkOrderDate())
                .effectiveFrom(workOrder.getEffectiveFrom())
                .effectiveTo(workOrder.getEffectiveTo())
                .employeeCount(workOrder.getEmployeeCount())
                .latestVersion(Boolean.TRUE.equals(workOrder.getLatestVersion()))
                .generatedByName(actorDetails.displayName())
                .generatedOn(workOrder.getCreatedDate())
                .build();
    }

    private void applyAuditMetadata(WorkOrderEntity workOrder, String actorEmail) {
        String normalizedActor = trimToNull(actorEmail);
        LocalDateTime now = LocalDateTime.now();
        workOrder.setCreatedBy(normalizedActor);
        workOrder.setCreatedDate(now);
        workOrder.setUpdatedBy(normalizedActor);
        workOrder.setUpdatedDate(now);
    }

    private void recordAuditEvent(
            Long workOrderId,
            String actionType,
            String actorEmail,
            String summary,
            String details,
            Map<String, Object> metadata) {
        auditTrailService.record(AuditRecordRequest.builder()
                .moduleName(MODULE_NAME)
                .entityType(ENTITY_TYPE)
                .entityId(String.valueOf(workOrderId))
                .actionType(actionType)
                .actorLoginId(actorEmail)
                .activitySummary(summary)
                .activityDetails(details)
                .metadata(metadata)
                .build());
    }

    private String buildAuditDetails(WorkOrderEntity workOrder, WorkOrderEntity parentWorkOrder) {
        StringBuilder builder = new StringBuilder();
        builder.append("Work order ").append(workOrder.getWorkOrderNumber())
                .append(" was generated for request ").append(defaultText(workOrder.getRequestId(), "-"))
                .append(" covering ").append(workOrder.getEmployeeCount()).append(" employee(s)");
        if (parentWorkOrder != null) {
            builder.append(" as an extension of ").append(parentWorkOrder.getWorkOrderNumber());
        }
        builder.append(". Effective period: ").append(workOrder.getEffectiveFrom()).append(" to ")
                .append(workOrder.getEffectiveTo()).append(".");
        return builder.toString();
    }

    private String buildDefaultSubject(EmployeeCohort cohort, WorkOrderType workOrderType) {
        String departmentName = defaultText(cohort.departmentName(), "the department");
        String projectName = defaultText(cohort.projectName(), "the assigned project");
        if (workOrderType == WorkOrderType.EXTENSION) {
            return "Extension of work order for deployment of MahaIT approved technical resources for "
                    + departmentName + " - " + projectName;
        }
        return "Work order for deployment of MahaIT approved technical resources for "
                + departmentName + " - " + projectName;
    }

    private String buildDefaultPurposeSummary(EmployeeCohort cohort, WorkOrderType workOrderType) {
        if (workOrderType == WorkOrderType.EXTENSION) {
            return "Extension of the existing deployment arrangement for continuity of project delivery and operational support.";
        }
        return "Deployment of approved technical resources for request "
                + defaultText(cohort.requestId(), "-") + " under the MahaIT-managed delivery model.";
    }

    private String resolveParentWorkOrderNumber(Long parentWorkOrderId) {
        return parentWorkOrderId == null ? null : workOrderRepository.findById(parentWorkOrderId)
                .map(WorkOrderEntity::getWorkOrderNumber)
                .orElse(null);
    }

    private RecruitmentNotificationEntity resolveNotification(EmployeeEntity employee) {
        AgencyCandidatePreOnboardingEntity preOnboarding = employee.getPreOnboarding();
        if (preOnboarding == null) {
            return null;
        }
        RecruitmentInterviewDetailEntity interviewDetail = preOnboarding.getInterviewDetail();
        return interviewDetail == null ? null : interviewDetail.getRecruitmentNotification();
    }

    private String resolveProjectName(EmployeeEntity employee) {
        RecruitmentNotificationEntity notification = resolveNotification(employee);
        ProjectMst project = notification == null ? null : notification.getProjectMst();
        return project == null ? "-" : defaultText(project.getProjectName(), "-");
    }

    private String resolveDesignationName(EmployeeEntity employee) {
        ManpowerDesignationMaster designation = employee.getDesignation();
        return designation == null ? null : trimToNull(designation.getDesignationName());
    }

    private String resolveEmployeeAgencyName(EmployeeEntity employee) {
        AgencyMaster agency = employee.getAgency();
        return agency == null ? "-" : defaultText(agency.getAgencyName(), "-");
    }

    private Long resolveDepartmentRegistrationId(
            DepartmentRegistrationEntity departmentRegistration,
            boolean internalEmployee) {
        if (departmentRegistration != null && departmentRegistration.getDepartmentRegistrationId() != null) {
            return departmentRegistration.getDepartmentRegistrationId();
        }
        return internalEmployee ? INTERNAL_UNMAPPED_DEPARTMENT_ID : null;
    }

    private String resolveDepartmentName(
            DepartmentRegistrationEntity departmentRegistration,
            boolean internalEmployee) {
        if (departmentRegistration != null) {
            return defaultText(departmentRegistration.getDepartmentName(), "Department");
        }
        return internalEmployee ? INTERNAL_UNMAPPED_DEPARTMENT_NAME : "Department";
    }

    private boolean isInternalEmployee(EmployeeEntity employee) {
        return RECRUITMENT_TYPE_INTERNAL.equals(resolveRecruitmentType(employee));
    }

    private String resolveRecruitmentType(EmployeeEntity employee) {
        if (employee == null) {
            return RECRUITMENT_TYPE_EXTERNAL;
        }
        return normalizeRecruitmentType(employee.getRecruitmentType());
    }

    private boolean hasSupportedRecruitmentType(String recruitmentType) {
        return RECRUITMENT_TYPE_INTERNAL.equalsIgnoreCase(defaultText(recruitmentType, ""))
                || RECRUITMENT_TYPE_EXTERNAL.equalsIgnoreCase(defaultText(recruitmentType, ""));
    }

    private String normalizeRecruitmentType(String recruitmentType) {
        if (RECRUITMENT_TYPE_INTERNAL.equalsIgnoreCase(defaultText(recruitmentType, ""))) {
            return RECRUITMENT_TYPE_INTERNAL;
        }
        return RECRUITMENT_TYPE_EXTERNAL;
    }

    private MahaItProfile resolveActiveProfile() {
        return mahaItProfileRepository.findFirstByActiveTrueOrderByUpdatedDateDesc()
                .orElseGet(() -> mahaItProfileRepository.findFirstByOrderByUpdatedDateDesc()
                        .orElseThrow(() -> new WorkOrderValidationException(
                                "Active MahaIT company profile is not configured.")));
    }

    private ActorDetails resolveActorDetails(String actorLoginId) {
        String normalizedLoginId = trimToNull(actorLoginId);
        if (normalizedLoginId == null) {
            return new ActorDetails("-", null);
        }
        User user = userService.findUserByEmail(normalizedLoginId);
        if (user == null) {
            return new ActorDetails(normalizedLoginId, normalizedLoginId);
        }
        String displayName = trimToNull(user.getName());
        return new ActorDetails(displayName == null ? normalizedLoginId : displayName, normalizedLoginId);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (!StringUtils.hasText(left) && !StringUtils.hasText(right)) {
            return true;
        }
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private int safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static final class PreparedWorkOrder {

        private final WorkOrderEntity parentWorkOrder;
        private final List<EmployeeEntity> selectedEmployees;
        private final EmployeeCohort cohort;

        private PreparedWorkOrder(
                WorkOrderEntity parentWorkOrder,
                List<EmployeeEntity> selectedEmployees,
                EmployeeCohort cohort) {
            this.parentWorkOrder = parentWorkOrder;
            this.selectedEmployees = selectedEmployees;
            this.cohort = cohort;
        }

        private WorkOrderEntity parentWorkOrder() {
            return parentWorkOrder;
        }

        private List<EmployeeEntity> selectedEmployees() {
            return selectedEmployees;
        }

        private EmployeeCohort cohort() {
            return cohort;
        }
    }

    private static final class ActorDetails {

        private final String displayName;
        private final String loginId;

        private ActorDetails(String displayName, String loginId) {
            this.displayName = displayName;
            this.loginId = loginId;
        }

        private String displayName() {
            return displayName;
        }

        private String loginId() {
            return loginId;
        }
    }

    private static final class EmployeeCohort {

        private final Long agencyId;
        private final String agencyName;
        private final String agencyContactName;
        private final String agencyOfficialEmail;
        private final String agencyAddress;
        private final Long departmentRegistrationId;
        private final String departmentName;
        private final String subDepartmentName;
        private final String requestId;
        private final String projectName;
        private final String projectCode;

        private EmployeeCohort(
                Long agencyId,
                String agencyName,
                String agencyContactName,
                String agencyOfficialEmail,
                String agencyAddress,
                Long departmentRegistrationId,
                String departmentName,
                String subDepartmentName,
                String requestId,
                String projectName,
                String projectCode) {
            this.agencyId = agencyId;
            this.agencyName = agencyName;
            this.agencyContactName = agencyContactName;
            this.agencyOfficialEmail = agencyOfficialEmail;
            this.agencyAddress = agencyAddress;
            this.departmentRegistrationId = departmentRegistrationId;
            this.departmentName = departmentName;
            this.subDepartmentName = subDepartmentName;
            this.requestId = requestId;
            this.projectName = projectName;
            this.projectCode = projectCode;
        }

        private static EmployeeCohort from(WorkOrderEntity workOrder) {
            return new EmployeeCohort(
                    workOrder.getAgencyId(),
                    workOrder.getAgencyName(),
                    workOrder.getAgencyContactName(),
                    workOrder.getAgencyOfficialEmail(),
                    workOrder.getAgencyAddress(),
                    workOrder.getDepartmentRegistrationId(),
                    workOrder.getDepartmentName(),
                    workOrder.getSubDepartmentName(),
                    workOrder.getRequestId(),
                    workOrder.getProjectName(),
                    workOrder.getProjectCode());
        }

        private boolean matches(EmployeeCohort other) {
            if (other == null) {
                return false;
            }
            return Objects.equals(agencyId, other.agencyId)
                    && Objects.equals(departmentRegistrationId, other.departmentRegistrationId)
                    && equalsText(requestId, other.requestId);
        }

        private Long agencyId() { return agencyId; }
        private String agencyName() { return agencyName; }
        private String agencyContactName() { return agencyContactName; }
        private String agencyOfficialEmail() { return agencyOfficialEmail; }
        private String agencyAddress() { return agencyAddress; }
        private Long departmentRegistrationId() { return departmentRegistrationId; }
        private String departmentName() { return departmentName; }
        private String subDepartmentName() { return subDepartmentName; }
        private String requestId() { return requestId; }
        private String projectName() { return projectName; }
        private String projectCode() { return projectCode; }

        private boolean equalsText(String left, String right) {
            if (!StringUtils.hasText(left) && !StringUtils.hasText(right)) {
                return true;
            }
            if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
                return false;
            }
            return left.trim().equalsIgnoreCase(right.trim());
        }
    }
}
