package com.maharecruitment.gov.in.web.controller.hr;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeTypeConversionForm;
import com.maharecruitment.gov.in.web.service.hr.EmployeeCellMappingPageService;
import com.maharecruitment.gov.in.web.service.hr.EmployeeTypeConversionService;
import com.maharecruitment.gov.in.web.service.hr.EmployeeTypeConversionService.ConversionCommand;
import com.maharecruitment.gov.in.web.service.hr.EmployeeTypeConversionService.ConversionResult;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr/employee-type-conversion")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class EmployeeTypeConversionController {

    private static final String INTERNAL = "INTERNAL";
    private static final String EXTERNAL = "EXTERNAL";

    private final EmployeeRepository employeeRepository;
    private final DepartmentMstRepository departmentRepository;
    private final LocationMasterRepository locationRepository;
    private final EmployeeTypeConversionService conversionService;
    private final EmployeeCellMappingPageService cellMappingService;
    private final UserRepository userRepository;

    public EmployeeTypeConversionController(
            EmployeeRepository employeeRepository,
            DepartmentMstRepository departmentRepository,
            LocationMasterRepository locationRepository,
            EmployeeTypeConversionService conversionService,
            EmployeeCellMappingPageService cellMappingService,
            UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.conversionService = conversionService;
        this.cellMappingService = cellMappingService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String showConversionScreen(Model model) {
        if (!model.containsAttribute("conversionForm")) {
            EmployeeTypeConversionForm form = new EmployeeTypeConversionForm();
            form.setTargetType(EXTERNAL);
            model.addAttribute("conversionForm", form);
        }
        populateReferenceData(model);
        return "hr/employee-type-conversion";
    }

    @PostMapping
    public String convert(
            @Valid @ModelAttribute("conversionForm") EmployeeTypeConversionForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateReferenceData(model);
            return "hr/employee-type-conversion";
        }

        try {
            ConversionResult result = conversionService.convert(new ConversionCommand(
                    form.getEmployeeId(),
                    form.getTargetType(),
                    form.getDepartmentId(),
                    form.getLocationId(),
                    form.getCellId(),
                    form.getReportingHodUserId(),
                    form.getManagerType(),
                    form.getReportingManagerEmployeeId(),
                    principal == null ? null : principal.getName()));
            redirectAttributes.addFlashAttribute("successMessage",
                    result.employeeName() + " was converted from " + title(result.sourceType())
                            + " to " + title(result.targetType())
                            + (result.departmentName() == null
                                    ? ". Department mapping was cleared for the internal employee."
                                    : " and mapped to " + result.departmentName() + ".")
                            + (result.locationName() == null
                                    ? ""
                                    : " Primary location: " + result.locationName() + "."));
            redirectAttributes.addAttribute("direction",
                    INTERNAL.equals(result.targetType()) ? "external-to-internal" : "internal-to-external");
        } catch (IllegalArgumentException | IllegalStateException | RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("conversionForm", form);
        }
        return "redirect:/hr/employee-type-conversion";
    }

    private void populateReferenceData(Model model) {
        model.addAttribute("internalEmployees", employeeOptions(INTERNAL));
        model.addAttribute("externalEmployees", employeeOptions(EXTERNAL));
        model.addAttribute("departments", departmentRepository.findAllByOrderByDepartmentNameAsc().stream()
                .map(department -> new DepartmentOption(
                        department.getDepartmentId(), department.getDepartmentName()))
                .toList());
        model.addAttribute("locations", locationRepository.findByActiveFlagIgnoreCaseOrderByLocationNameAsc("Y").stream()
                .map(location -> new LocationOption(location.getLocationId(), locationDisplayName(location)))
                .toList());
        model.addAttribute("cells", cellMappingService.availableActiveCells());
        List<ReportingPersonOption> reportingPeople = reportingPeople();
        model.addAttribute("reportingPeople", reportingPeople);
    }

    private List<ReportingPersonOption> reportingPeople() {
        List<EmployeeEntity> employees = employeeRepository
                .findActiveCellAuthorityEmployees(Set.of(INTERNAL, "MAHAIT"));
        List<EmployeeEntity> unlinked = employees.stream()
                .filter(employee -> employee.getUser() == null)
                .toList();
        Map<String, User> usersByEmail = userRepository.findActiveUsersByNormalizedEmailIn(
                        normalizedValues(unlinked.stream().map(EmployeeEntity::getEmail).toList()))
                .stream()
                .collect(Collectors.toMap(
                        user -> normalize(user.getEmail()), Function.identity(), (first, ignored) -> first));
        Map<String, User> usersByMobile = userRepository.findActiveUsersByMobileNumberIn(
                        normalizedValues(unlinked.stream().map(EmployeeEntity::getMobile).toList()))
                .stream()
                .collect(Collectors.toMap(
                        user -> normalize(user.getMobileNo()), Function.identity(), (first, ignored) -> first));

        return employees.stream().map(employee -> {
            User user = employee.getUser();
            if (user == null) {
                user = usersByEmail.get(normalize(employee.getEmail()));
            }
            if (user == null) {
                user = usersByMobile.get(normalize(employee.getMobile()));
            }
            boolean activeAccount = user != null && user.getId() != null && Boolean.TRUE.equals(user.getActive());
            String code = employee.getEmployeeCode() == null ? "" : employee.getEmployeeCode().trim();
            String label = employee.getFullName()
                    + (code.isEmpty() ? "" : " (" + code + ")")
                    + " - " + title(employee.getRecruitmentType());
            return new ReportingPersonOption(
                    employee.getEmployeeId(), activeAccount ? user.getId() : null, label, activeAccount);
        }).toList();
    }

    private Collection<String> normalizedValues(List<String> values) {
        return values.stream().map(this::normalize).filter(value -> !value.isEmpty()).collect(Collectors.toSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<EmployeeOption> employeeOptions(String type) {
        return employeeRepository
                .findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(type, "ACTIVE")
                .stream()
                .map(this::toOption)
                .toList();
    }

    private EmployeeOption toOption(EmployeeEntity employee) {
        DepartmentMst department = employee.getDepartment();
        String designation = employee.getDesignation() == null
                ? "Not assigned"
                : employee.getDesignation().getDesignationName();
        return new EmployeeOption(
                employee.getEmployeeId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                designation,
                department == null ? null : department.getDepartmentId(),
                department == null ? "Not mapped" : department.getDepartmentName());
    }

    private String title(String value) {
        return value.charAt(0) + value.substring(1).toLowerCase();
    }

    private String locationDisplayName(LocationMaster location) {
        String address = location.getLocationName();
        String office = location.getOfficeName();
        return office == null || office.isBlank() ? address : office.trim() + " - " + address;
    }

    public record EmployeeOption(
            Long id,
            String code,
            String name,
            String email,
            String designation,
            Long departmentId,
            String departmentName) {
    }

    public record DepartmentOption(Long id, String name) {
    }

    public record LocationOption(Long id, String name) {
    }

    public record ReportingPersonOption(Long employeeId, Long userId, String label, boolean activeAccount) {
    }
}
