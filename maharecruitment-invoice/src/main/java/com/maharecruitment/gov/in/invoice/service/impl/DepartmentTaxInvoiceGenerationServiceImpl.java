package com.maharecruitment.gov.in.invoice.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationActivityRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceEmployeePreviewView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationApplicationView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationFilter;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationOptionView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationPreviewView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceGenerationResultView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;
import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceEntity;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.repository.DepartmentTaxInvoiceRepository;
import com.maharecruitment.gov.in.invoice.repository.EmployeeTaxInvoiceRepository.InvoicedEmployee;
import com.maharecruitment.gov.in.invoice.service.DepartmentTaxInvoiceGenerationService;
import com.maharecruitment.gov.in.invoice.service.DepartmentTaxInvoiceService;
import com.maharecruitment.gov.in.invoice.service.EmployeeTaxInvoiceBuilder;
import com.maharecruitment.gov.in.invoice.service.EmployeeTaxInvoiceService;
import com.maharecruitment.gov.in.invoice.service.InvoiceEmployeeMappings;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeProjectMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeProjectMappingRepository;

@Service
@Transactional(readOnly = true)
public class DepartmentTaxInvoiceGenerationServiceImpl implements DepartmentTaxInvoiceGenerationService {

    private static final String DEFAULT_ACTOR = "SYSTEM";
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final DepartmentMstRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final ProjectMstRepository projectRepository;
    private final EmployeeProjectMappingRepository employeeProjectMappingRepository;
    private final DepartmentProjectApplicationRepository applicationRepository;
    private final DepartmentProjectApplicationActivityRepository activityRepository;
    private final DepartmentTaxInvoiceRepository invoiceRepository;
    private final DepartmentTaxInvoiceService taxInvoiceService;
    private final EmployeeTaxInvoiceBuilder employeeTaxInvoiceBuilder;
    private final EmployeeTaxInvoiceService employeeTaxInvoiceService;

    public DepartmentTaxInvoiceGenerationServiceImpl(
            DepartmentMstRepository departmentRepository,
            SubDepartmentRepository subDepartmentRepository,
            ProjectMstRepository projectRepository,
            EmployeeProjectMappingRepository employeeProjectMappingRepository,
            DepartmentProjectApplicationRepository applicationRepository,
            DepartmentProjectApplicationActivityRepository activityRepository,
            DepartmentTaxInvoiceRepository invoiceRepository,
            DepartmentTaxInvoiceService taxInvoiceService,
            EmployeeTaxInvoiceBuilder employeeTaxInvoiceBuilder,
            EmployeeTaxInvoiceService employeeTaxInvoiceService) {
        this.departmentRepository = departmentRepository;
        this.subDepartmentRepository = subDepartmentRepository;
        this.projectRepository = projectRepository;
        this.employeeProjectMappingRepository = employeeProjectMappingRepository;
        this.applicationRepository = applicationRepository;
        this.activityRepository = activityRepository;
        this.invoiceRepository = invoiceRepository;
        this.taxInvoiceService = taxInvoiceService;
        this.employeeTaxInvoiceBuilder = employeeTaxInvoiceBuilder;
        this.employeeTaxInvoiceService = employeeTaxInvoiceService;
    }

    @Override
    public List<TaxInvoiceGenerationOptionView> getDepartmentOptions() {
        return departmentRepository.findAll(Sort.by(Sort.Order.asc("departmentName"))).stream()
                .map(department -> new TaxInvoiceGenerationOptionView(
                        department.getDepartmentId(),
                        defaultIfBlank(department.getDepartmentName(), "Department " + department.getDepartmentId()),
                        null,
                        department.getDepartmentId(),
                        null))
                .toList();
    }

    @Override
    public List<TaxInvoiceGenerationOptionView> getSubDepartmentOptions(Long departmentId) {
        List<SubDepartment> subDepartments = departmentId == null
                ? subDepartmentRepository.findAllByOrderBySubDeptNameAsc()
                : subDepartmentRepository.findByDepartmentDepartmentIdOrderBySubDeptNameAsc(departmentId);
        return subDepartments.stream()
                .map(subDepartment -> new TaxInvoiceGenerationOptionView(
                        subDepartment.getSubDeptId(),
                        defaultIfBlank(subDepartment.getSubDeptName(), "Sub Department " + subDepartment.getSubDeptId()),
                        null,
                        resolveDepartmentId(subDepartment, departmentId),
                        subDepartment.getSubDeptId()))
                .toList();
    }

    @Override
    public List<TaxInvoiceGenerationOptionView> getProjectOptions(Long departmentId, Long subDepartmentId) {
        return resolveScopedProjects(normalizeOptionalId(departmentId))
                .stream()
                .map(this::toProjectOption)
                .toList();
    }

    /**
     * Projects whose project_mst.department_id matches the selected department.
     */
    private List<ProjectMst> resolveScopedProjects(Long departmentId) {
        if (departmentId == null) {
            return List.of();
        }
        return projectRepository.findProjectsByDepartmentIdNative(departmentId);
    }

    @Override
    public TaxInvoiceGenerationPreviewView preview(TaxInvoiceGenerationFilter filter) {
        ResolvedFilter resolvedFilter = validateAndResolveFilter(filter);
        List<TaxInvoiceEmployeePreviewView> employees = loadEmployeePreview(resolvedFilter);
        List<TaxInvoiceGenerationApplicationView> applications = loadApplicationPreview(resolvedFilter);
        return new TaxInvoiceGenerationPreviewView(employees, applications);
    }

    @Override
    @Transactional
    public TaxInvoiceGenerationResultView generate(TaxInvoiceGenerationFilter filter, String actorEmail) {
        ResolvedFilter resolvedFilter = validateAndResolveFilter(filter);
        List<TaxInvoiceGenerationApplicationView> candidates = loadApplicationPreview(resolvedFilter);
        String resolvedActor = StringUtils.hasText(actorEmail) ? actorEmail.trim() : DEFAULT_ACTOR;

        int generatedCount = 0;
        int alreadyGeneratedCount = 0;
        int failedCount = 0;
        List<TaxInvoiceGenerationApplicationView> results = new ArrayList<>();

        for (TaxInvoiceGenerationApplicationView candidate : candidates) {
            if (candidate.isGenerated()) {
                alreadyGeneratedCount++;
                results.add(withStatus(candidate, "Already Generated", "Tax invoice already exists."));
                continue;
            }

            try {
                TaxInvoiceView generated = taxInvoiceService.generateForApplication(
                        candidate.getApplicationId(),
                        resolvedActor);
                generatedCount++;
                results.add(new TaxInvoiceGenerationApplicationView(
                        candidate.getApplicationId(),
                        generated.getDepartmentTaxInvoiceId(),
                        candidate.getRequestId(),
                        generated.getTiNumber(),
                        generated.getTiDate(),
                        candidate.getProjectName(),
                        candidate.getProjectCode(),
                        candidate.getDepartmentName(),
                        candidate.getSubDepartmentName(),
                        "Generated",
                        "Tax invoice generated successfully."));
            } catch (RuntimeException ex) {
                failedCount++;
                results.add(withStatus(candidate, "Failed", ex.getMessage()));
            }
        }

        return new TaxInvoiceGenerationResultView(
                generatedCount,
                alreadyGeneratedCount,
                failedCount,
                List.copyOf(results));
    }

    @Override
    public List<TaxInvoiceEmployeePreviewView> loadProjectEmployees(TaxInvoiceGenerationFilter filter) {
        ResolvedFilter resolvedFilter = validateAndResolveFilter(filter);
        if (resolvedFilter.projectId() == null) {
            throw new TaxInvoiceException("Select a project to load its employees.");
        }
        return loadEmployeePreview(resolvedFilter);
    }

    @Override
    public TaxInvoiceView buildEmployeeInvoice(TaxInvoiceGenerationFilter filter) {
        ResolvedFilter resolvedFilter = validateAndResolveFilter(filter);
        if (resolvedFilter.projectId() == null) {
            throw new TaxInvoiceException("Select a project to generate the employee-wise tax invoice.");
        }
        ProjectMst project = projectRepository.findById(resolvedFilter.projectId())
                .orElseThrow(() -> new TaxInvoiceException("Selected project was not found."));
        List<EmployeeProjectMappingEntity> mappings = InvoiceEmployeeMappings.uniqueEmployees(
                employeeProjectMappingRepository.findCurrentProjectEmployeesForTaxInvoice(
                        resolvedFilter.departmentId(),
                        employeeSubDepartmentScope(resolvedFilter),
                        resolvedFilter.projectId()));
        // Employees already billed for any day of this period are left off, so the same days are never billed twice.
        Map<Long, InvoicedEmployee> invoiced = findInvoiced(mappings, resolvedFilter);
        List<EmployeeProjectMappingEntity> billable = mappings.stream()
                .filter(mapping -> !invoiced.containsKey(mapping.getEmployee().getEmployeeId()))
                .toList();
        if (!mappings.isEmpty() && billable.isEmpty()) {
            throw new TaxInvoiceException("All employees of the selected project are already invoiced for "
                    + PERIOD_FORMAT.format(resolvedFilter.startDate()) + " to "
                    + PERIOD_FORMAT.format(resolvedFilter.endDate()) + ".");
        }
        mappings = billable;
        return employeeTaxInvoiceBuilder.build(
                project,
                resolvedFilter.departmentId(),
                resolvedFilter.subDepartmentId(),
                mappings,
                resolvedFilter.startDate(),
                resolvedFilter.endDate());
    }

    private List<TaxInvoiceEmployeePreviewView> loadEmployeePreview(ResolvedFilter filter) {
        List<EmployeeProjectMappingEntity> mappings = InvoiceEmployeeMappings.uniqueEmployees(
                employeeProjectMappingRepository.findCurrentProjectEmployeesForTaxInvoice(
                        filter.departmentId(),
                        employeeSubDepartmentScope(filter),
                        filter.projectId()));
        Map<Long, InvoicedEmployee> invoiced = findInvoiced(mappings, filter);
        return mappings.stream()
                .map(mapping -> toEmployeePreview(mapping, filter.startDate(), filter.endDate(),
                        invoiced.get(mapping.getEmployee().getEmployeeId())))
                .toList();
    }

    private Map<Long, InvoicedEmployee> findInvoiced(List<EmployeeProjectMappingEntity> mappings, ResolvedFilter filter) {
        if (filter.startDate() == null || filter.endDate() == null) {
            return Map.of();
        }
        return employeeTaxInvoiceService.findInvoicedEmployees(
                mappings.stream().map(mapping -> mapping.getEmployee().getEmployeeId()).toList(),
                filter.startDate(), filter.endDate());
    }

    /**
     * Project options are scoped by department only, so a chosen project already fixes the scope;
     * applying the subdepartment on top would drop the project's employees when the two disagree.
     */
    private Long employeeSubDepartmentScope(ResolvedFilter filter) {
        return filter.projectId() != null ? null : filter.subDepartmentId();
    }

    private List<TaxInvoiceGenerationApplicationView> loadApplicationPreview(ResolvedFilter filter) {
        List<DepartmentProjectApplicationEntity> candidates = applicationRepository.findCompletedTaxInvoiceCandidates(
                filter.departmentId(),
                filter.subDepartmentId(),
                filter.projectId(),
                DepartmentApplicationStatus.COMPLETED);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, LocalDate> issueDates = resolveIssueDates(candidates);
        List<DepartmentProjectApplicationEntity> filteredCandidates = candidates.stream()
                .filter(application -> isWithinRange(
                        issueDates.get(application.getDepartmentProjectApplicationId()),
                        filter.startDate(),
                        filter.endDate()))
                .toList();
        if (filteredCandidates.isEmpty()) {
            return List.of();
        }

        Map<Long, DepartmentTaxInvoiceEntity> invoicesByApplicationId = loadExistingInvoices(filteredCandidates);
        NameLookup nameLookup = loadNameLookup(filteredCandidates);

        return filteredCandidates.stream()
                .map(application -> toApplicationView(
                        application,
                        issueDates.get(application.getDepartmentProjectApplicationId()),
                        invoicesByApplicationId.get(application.getDepartmentProjectApplicationId()),
                        nameLookup))
                .toList();
    }

    private Map<Long, LocalDate> resolveIssueDates(List<DepartmentProjectApplicationEntity> applications) {
        Map<Long, LocalDate> issueDates = new LinkedHashMap<>();
        List<Long> applicationIds = applications.stream()
                .map(DepartmentProjectApplicationEntity::getDepartmentProjectApplicationId)
                .filter(Objects::nonNull)
                .toList();

        applications.forEach(application -> issueDates.put(
                application.getDepartmentProjectApplicationId(),
                resolveFallbackIssueDate(application)));

        if (applicationIds.isEmpty()) {
            return issueDates;
        }

        Map<Long, LocalDate> auditorApprovedDates = new LinkedHashMap<>();
        activityRepository.findIssueActivitiesForApplications(
                applicationIds,
                DepartmentApplicationStatus.AUDITOR_APPROVED).stream()
                .filter(activity -> activity.getApplication() != null)
                .filter(activity -> activity.getActionTimestamp() != null)
                .forEach(activity -> auditorApprovedDates.putIfAbsent(
                        activity.getApplication().getDepartmentProjectApplicationId(),
                        activity.getActionTimestamp().toLocalDate()));
        auditorApprovedDates.forEach(issueDates::put);
        return issueDates;
    }

    private Map<Long, DepartmentTaxInvoiceEntity> loadExistingInvoices(
            List<DepartmentProjectApplicationEntity> applications) {
        List<Long> applicationIds = applications.stream()
                .map(DepartmentProjectApplicationEntity::getDepartmentProjectApplicationId)
                .filter(Objects::nonNull)
                .toList();
        if (applicationIds.isEmpty()) {
            return Map.of();
        }
        return invoiceRepository.findByDepartmentProjectApplicationIdIn(applicationIds).stream()
                .collect(Collectors.toMap(
                        DepartmentTaxInvoiceEntity::getDepartmentProjectApplicationId,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new));
    }

    private NameLookup loadNameLookup(List<DepartmentProjectApplicationEntity> applications) {
        List<Long> departmentIds = applications.stream()
                .map(DepartmentProjectApplicationEntity::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> subDepartmentIds = applications.stream()
                .map(DepartmentProjectApplicationEntity::getSubDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> departments = departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(
                        DepartmentMst::getDepartmentId,
                        department -> defaultIfBlank(department.getDepartmentName(),
                                "Department " + department.getDepartmentId()),
                        (first, second) -> first,
                        LinkedHashMap::new));
        Map<Long, String> subDepartments = subDepartmentRepository.findAllById(subDepartmentIds).stream()
                .collect(Collectors.toMap(
                        SubDepartment::getSubDeptId,
                        subDepartment -> defaultIfBlank(subDepartment.getSubDeptName(),
                                "Sub Department " + subDepartment.getSubDeptId()),
                        (first, second) -> first,
                        LinkedHashMap::new));
        return new NameLookup(departments, subDepartments);
    }

    private ResolvedFilter validateAndResolveFilter(TaxInvoiceGenerationFilter filter) {
        if (filter == null) {
            throw new TaxInvoiceException("Tax invoice generation filter is required.");
        }
        Long departmentId = requirePositiveId(filter.getDepartmentId(), "Select a department.");
        Long subDepartmentId = normalizeOptionalId(filter.getSubDepartmentId());
        Long projectId = normalizeOptionalId(filter.getProjectId());
        LocalDate startDate = filter.getStartDate();
        LocalDate endDate = filter.getEndDate();

        if (startDate == null) {
            throw new TaxInvoiceException("Start date is required.");
        }
        if (endDate == null) {
            throw new TaxInvoiceException("End date is required.");
        }
        if (startDate.isAfter(endDate)) {
            throw new TaxInvoiceException("Start date cannot be after end date.");
        }
        if (!departmentRepository.existsById(departmentId)) {
            throw new TaxInvoiceException("Selected department was not found.");
        }
        if (subDepartmentId != null
                && subDepartmentRepository.findBySubDeptIdAndDepartmentDepartmentId(subDepartmentId, departmentId)
                        .isEmpty()) {
            throw new TaxInvoiceException("Selected subdepartment does not belong to the selected department.");
        }
        if (projectId != null) {
            validateProjectSelection(projectId, departmentId);
        }

        return new ResolvedFilter(departmentId, subDepartmentId, projectId, startDate, endDate);
    }

    private void validateProjectSelection(Long projectId, Long departmentId) {
        if (!projectRepository.existsById(projectId)) {
            throw new TaxInvoiceException("Selected project was not found.");
        }
        boolean inScope = resolveScopedProjects(departmentId).stream()
                .anyMatch(scoped -> Objects.equals(scoped.getProjectId(), projectId));
        if (!inScope) {
            throw new TaxInvoiceException("Selected project does not belong to the selected department.");
        }
    }

    private TaxInvoiceGenerationApplicationView toApplicationView(
            DepartmentProjectApplicationEntity application,
            LocalDate issueDate,
            DepartmentTaxInvoiceEntity invoice,
            NameLookup nameLookup) {
        return new TaxInvoiceGenerationApplicationView(
                application.getDepartmentProjectApplicationId(),
                invoice == null ? null : invoice.getDepartmentTaxInvoiceId(),
                application.getRequestId(),
                invoice == null ? null : invoice.getTiNumber(),
                invoice == null ? issueDate : invoice.getTiDate(),
                defaultIfBlank(application.getProjectName(), "-"),
                defaultIfBlank(application.getProjectCode(), "-"),
                nameLookup.departmentName(application.getDepartmentId()),
                nameLookup.subDepartmentName(application.getSubDepartmentId()),
                invoice == null ? "Ready" : "Already Generated",
                invoice == null ? "Ready for generation." : "Tax invoice already exists.");
    }

    private TaxInvoiceEmployeePreviewView toEmployeePreview(
            EmployeeProjectMappingEntity mapping,
            LocalDate periodStart,
            LocalDate periodEnd,
            InvoicedEmployee invoiced) {
        EmployeeEntity employee = mapping.getEmployee();
        ProjectMst project = mapping.getProject();
        return new TaxInvoiceEmployeePreviewView(
                employee.getEmployeeId(),
                defaultIfBlank(employee.getEmployeeCode(), "-"),
                defaultIfBlank(employee.getFullName(), "-"),
                defaultIfBlank(employee.getEmail(), "-"),
                employee.getDesignation() == null ? "-" : defaultIfBlank(employee.getDesignation().getDesignationName(), "-"),
                defaultIfBlank(employee.getLevelCode(), "-"),
                defaultIfBlank(employee.getRecruitmentType(), "-"),
                resolveDepartmentName(employee, project),
                resolveSubDepartmentName(employee, project),
                project == null ? "-" : defaultIfBlank(project.getProjectName(), "-"),
                employee.getOnboardingDate(),
                employee.getResignationDate(),
                countDaysOnProject(employee, periodStart, periodEnd),
                invoiced == null ? null : invoiced.tiNumber() + ", " + PERIOD_FORMAT.format(invoiced.billedFrom())
                        + " to " + PERIOD_FORMAT.format(invoiced.billedTo()));
    }

    /** Same window EmployeeTaxInvoiceBuilder bills: the period clipped to onboarding and resignation dates. */
    private long countDaysOnProject(EmployeeEntity employee, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate from = employee.getOnboardingDate() != null && employee.getOnboardingDate().isAfter(periodStart)
                ? employee.getOnboardingDate()
                : periodStart;
        LocalDate to = employee.getResignationDate() != null && employee.getResignationDate().isBefore(periodEnd)
                ? employee.getResignationDate()
                : periodEnd;
        return from.isAfter(to) ? 0 : ChronoUnit.DAYS.between(from, to) + 1;
    }

    private TaxInvoiceGenerationApplicationView withStatus(
            TaxInvoiceGenerationApplicationView candidate,
            String status,
            String message) {
        return new TaxInvoiceGenerationApplicationView(
                candidate.getApplicationId(),
                candidate.getDepartmentTaxInvoiceId(),
                candidate.getRequestId(),
                candidate.getTiNumber(),
                candidate.getIssueDate(),
                candidate.getProjectName(),
                candidate.getProjectCode(),
                candidate.getDepartmentName(),
                candidate.getSubDepartmentName(),
                status,
                defaultIfBlank(message, "-"));
    }

    private TaxInvoiceGenerationOptionView toProjectOption(ProjectMst project) {
        return new TaxInvoiceGenerationOptionView(
                project.getProjectId(),
                defaultIfBlank(project.getProjectName(), "Project " + project.getProjectId()),
                defaultIfBlank(project.getProjectCode(), null),
                project.getDepartmentId(),
                project.getSubDepartmentId());
    }

    private Long resolveDepartmentId(SubDepartment subDepartment, Long fallbackDepartmentId) {
        if (subDepartment.getDepartment() != null) {
            return subDepartment.getDepartment().getDepartmentId();
        }
        return fallbackDepartmentId;
    }

    private LocalDate resolveFallbackIssueDate(DepartmentProjectApplicationEntity application) {
        if (application.getUpdatedDate() != null) {
            return application.getUpdatedDate().toLocalDate();
        }
        if (application.getCreatedDate() != null) {
            return application.getCreatedDate().toLocalDate();
        }
        return LocalDate.now();
    }

    private boolean isWithinRange(LocalDate value, LocalDate startDate, LocalDate endDate) {
        return value != null && !value.isBefore(startDate) && !value.isAfter(endDate);
    }

    private String resolveDepartmentName(EmployeeEntity employee, ProjectMst project) {
        if (employee.getDepartmentRegistration() != null
                && StringUtils.hasText(employee.getDepartmentRegistration().getDepartmentName())) {
            return employee.getDepartmentRegistration().getDepartmentName().trim();
        }
        if (employee.getDepartment() != null && StringUtils.hasText(employee.getDepartment().getDepartmentName())) {
            return employee.getDepartment().getDepartmentName().trim();
        }
        if (project != null && project.getDepartment() != null
                && StringUtils.hasText(project.getDepartment().getDepartmentName())) {
            return project.getDepartment().getDepartmentName().trim();
        }
        return "-";
    }

    private String resolveSubDepartmentName(EmployeeEntity employee, ProjectMst project) {
        if (employee.getSubDepartment() != null && StringUtils.hasText(employee.getSubDepartment().getSubDeptName())) {
            return employee.getSubDepartment().getSubDeptName().trim();
        }
        if (project != null && project.getSubDepartment() != null
                && StringUtils.hasText(project.getSubDepartment().getSubDeptName())) {
            return project.getSubDepartment().getSubDeptName().trim();
        }
        return "-";
    }

    private Long requirePositiveId(Long value, String message) {
        Long normalized = normalizeOptionalId(value);
        if (normalized == null) {
            throw new TaxInvoiceException(message);
        }
        return normalized;
    }

    private Long normalizeOptionalId(Long value) {
        return value == null || value < 1 ? null : value;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private record ResolvedFilter(
            Long departmentId,
            Long subDepartmentId,
            Long projectId,
            LocalDate startDate,
            LocalDate endDate) {
    }

    private record NameLookup(Map<Long, String> departments, Map<Long, String> subDepartments) {

        String departmentName(Long departmentId) {
            if (departmentId == null) {
                return "-";
            }
            return departments.getOrDefault(departmentId, "Department " + departmentId);
        }

        String subDepartmentName(Long subDepartmentId) {
            if (subDepartmentId == null) {
                return "-";
            }
            return subDepartments.getOrDefault(subDepartmentId, "Sub Department " + subDepartmentId);
        }
    }
}
