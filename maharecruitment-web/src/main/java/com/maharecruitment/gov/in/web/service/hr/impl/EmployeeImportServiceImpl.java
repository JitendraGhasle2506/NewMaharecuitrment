package com.maharecruitment.gov.in.web.service.hr.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.auth.dto.UserUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.auth.service.UserManagementService;
import com.maharecruitment.gov.in.auth.util.SecurePasswordGenerator;
import com.maharecruitment.gov.in.auth.util.UserValidationUtil;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.SubDepartment;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationMasterRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeLocationMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeImportResult;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeImportRowResult;
import com.maharecruitment.gov.in.web.service.hr.EmployeeImportService;

@Service
public class EmployeeImportServiceImpl implements EmployeeImportService {

    private static final int MAX_IMPORT_ROWS = 3_000;
    private static final String EMPLOYEE_ROLE_NAME = "ROLE_EMPLOYEE";
    private static final String ACTIVE_FLAG = "Y";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String AADHAAR_EMPLOYEE_CODE_PREFIX = "MahaI";
    private static final String LEGACY_EMPLOYEE_CODE_PREFIX = "EMP";
    private static final String TEMPORARY_EMPLOYEE_CODE_PREFIX = "TMP-";
    private static final List<String> CSV_HEADERS = List.of(
            "employeeCode",
            "recruitmentType",
            "fullName",
            "email",
            "mobile",
            "dateOfBirth",
            "gender",
            "bloodGroup",
            "address",
            "emergencyContactName",
            "emergencyContactRelation",
            "emergencyContactMobile",
            "emergencyContactAltMobile",
            "joiningDate",
            "onboardingDate",
            "panNumber",
            "aadhaarNumber",
            "departmentRegistrationId",
            "subDepartmentId",
            "subDepartmentName",
            "designationId",
            "agencyId",
            "levelCode",
            "requestId",
            "status",
            "locationIds",
            "temporaryPassword");
    private static final LocalDate DEFAULT_DATE_OF_BIRTH = LocalDate.of(1900, 1, 1);
    private static final String DEFAULT_TEXT_VALUE = "NOT_PROVIDED";
    private static final String DEFAULT_BLOOD_GROUP = "NA";
    private static final String DEFAULT_GENDER = "NOT_SPECIFIED";
    private static final Pattern MOBILE_CANDIDATE_PATTERN = Pattern.compile("\\+?\\d[\\d\\s().-]{8,}\\d");
    private static final Pattern ORDINAL_DAY_PATTERN = Pattern.compile("(?i)\\b(\\d{1,2})(st|nd|rd|th)\\b");
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            numericDateFormatter("d-M-uuuu"),
            numericDateFormatter("d/M/uuuu"),
            numericDateFormatter("d.M.uuuu"),
            monthNameFormatter("d MMM uuuu"),
            monthNameFormatter("d MMMM uuuu"),
            monthNameFormatter("d-MMM-uuuu"),
            monthNameFormatter("d-MMMM-uuuu"),
            reducedYearMonthNameFormatter("d-MMM-"),
            reducedYearMonthNameFormatter("d-MMMM-"));
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "recruitmenttype",
            "fullname",
            "email",
            "mobile",
            "joiningdate",
            "onboardingdate",
            "agencyid");

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final UserManagementService userManagementService;
    private final UserAffiliationService userAffiliationService;
    private final RoleRepository roleRepository;
    private final DepartmentRegistrationRepository departmentRegistrationRepository;
    private final DepartmentMstRepository departmentRepository;
    private final SubDepartmentRepository subDepartmentRepository;
    private final ManpowerDesignationMasterRepository designationRepository;
    private final AgencyMasterRepository agencyRepository;
    private final LocationMasterRepository locationRepository;
    private final EmployeeLocationMappingRepository employeeLocationMappingRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public EmployeeImportServiceImpl(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            UserManagementService userManagementService,
            UserAffiliationService userAffiliationService,
            RoleRepository roleRepository,
            DepartmentRegistrationRepository departmentRegistrationRepository,
            DepartmentMstRepository departmentRepository,
            SubDepartmentRepository subDepartmentRepository,
            ManpowerDesignationMasterRepository designationRepository,
            AgencyMasterRepository agencyRepository,
            LocationMasterRepository locationRepository,
            EmployeeLocationMappingRepository employeeLocationMappingRepository,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.userManagementService = userManagementService;
        this.userAffiliationService = userAffiliationService;
        this.roleRepository = roleRepository;
        this.departmentRegistrationRepository = departmentRegistrationRepository;
        this.departmentRepository = departmentRepository;
        this.subDepartmentRepository = subDepartmentRepository;
        this.designationRepository = designationRepository;
        this.agencyRepository = agencyRepository;
        this.locationRepository = locationRepository;
        this.employeeLocationMappingRepository = employeeLocationMappingRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public EmployeeImportResult importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a CSV file.");
        }

        List<CsvRow> csvRows = readCsvRows(file);
        if (csvRows.isEmpty()) {
            throw new IllegalArgumentException("CSV file does not contain employee rows.");
        }
        if (csvRows.size() > MAX_IMPORT_ROWS) {
            throw new IllegalArgumentException("CSV file must not contain more than " + MAX_IMPORT_ROWS + " rows.");
        }

        Set<String> importedCodes = new LinkedHashSet<>();
        Set<String> importedEmails = new LinkedHashSet<>();
        Set<String> importedMobiles = new LinkedHashSet<>();
        List<EmployeeImportRowResult> results = new ArrayList<>();

        for (CsvRow csvRow : csvRows) {
            EmployeeImportRow importRow = null;
            try {
                importRow = toImportRow(csvRow);
                validateDuplicateInFile(importRow, importedCodes, importedEmails, importedMobiles);
                EmployeeImportRow selectedRow = importRow;
                EmployeeImportRowResult result = transactionTemplate.execute(
                        status -> importEmployee(selectedRow));
                results.add(Objects.requireNonNull(result));
            } catch (RuntimeException ex) {
                results.add(toFailedResult(csvRow, importRow, rootMessage(ex)));
            }
        }

        int successCount = (int) results.stream().filter(EmployeeImportRowResult::success).count();
        int failureCount = results.size() - successCount;
        return new EmployeeImportResult(results.size(), successCount, failureCount, results);
    }

    @Override
    public byte[] buildCsvTemplate() {
        List<List<String>> rows = List.of(
                CSV_HEADERS,
                List.of(
                        "",
                        "INTERNAL",
                        "Rahul Patil",
                        "rahul.patil@example.com",
                        "9876543210",
                        "1990-01-15",
                        "MALE",
                        "O+",
                        "Mumbai Office Address",
                        "Suresh Patil",
                        "Father",
                        "9876543211",
                        "",
                        "2026-07-01",
                        "2026-07-01",
                        "ABCDE1234F",
                        "123412341234",
                        "1",
                        "1",
                        "",
                        "1",
                        "1",
                        "L1",
                        "REQ-INT-001",
                        "ACTIVE",
                        "1|2",
                        ""),
                List.of(
                        "",
                        "EXTERNAL",
                        "Anita Sharma",
                        "anita.sharma@example.com",
                        "9876543220",
                        "1992-03-20",
                        "FEMALE",
                        "A+",
                        "Pune Office Address",
                        "Ramesh Sharma",
                        "Father",
                        "9876543221",
                        "",
                        "2026-07-01",
                        "2026-07-01",
                        "PQRST1234L",
                        "567856785678",
                        "",
                        "",
                        "Existing Sub Department",
                        "1",
                        "1",
                        "L2",
                        "REQ-EXT-001",
                        "ACTIVE",
                        "3",
                        ""));

        String csv = rows.stream()
                .map(this::toCsvLine)
                .collect(Collectors.joining("\r\n")) + "\r\n";
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String csvTemplateFileName() {
        return "employee-import-template.csv";
    }

    private EmployeeImportRowResult importEmployee(EmployeeImportRow row) {
        Role employeeRole = roleRepository.findByNameIgnoreCase(EMPLOYEE_ROLE_NAME)
                .orElseThrow(() -> new IllegalArgumentException("Employee role is not configured."));
        EmployeeEntity employee = resolveEmployee(row);
        boolean newEmployee = employee.getEmployeeId() == null;

        validateEmployeeUniqueness(row, employee);
        DepartmentRegistrationEntity departmentRegistration = resolveDepartmentRegistration(row.departmentRegistrationId());
        SubDepartment subDepartment = resolveSubDepartment(
                row.subDepartmentId(),
                row.subDepartmentName(),
                departmentRegistration);
        DepartmentMst department = resolveDepartment(departmentRegistration, subDepartment);
        ManpowerDesignationMaster designation = resolveDesignation(row.designationId());
        AgencyMaster agency = resolveAgency(row.agencyId());

        applyEmployee(row, employee, departmentRegistration, department, subDepartment, designation, agency);
        applyGeneratedEmployeeCode(row, employee, newEmployee);
        if (newEmployee && !StringUtils.hasText(employee.getEmployeeCode())) {
            employee.setEmployeeCode(generateTemporaryEmployeeCode());
        }

        EmployeeEntity savedEmployee = employeeRepository.save(employee);
        if (newEmployee && !StringUtils.hasText(row.employeeCode())) {
            savedEmployee.setEmployeeCode(resolveFinalEmployeeCode(savedEmployee));
            savedEmployee = employeeRepository.save(savedEmployee);
        }

        UserAccountResult userResult = upsertUser(row, savedEmployee, departmentRegistration, employeeRole);
        if (savedEmployee.getUser() == null
                || !Objects.equals(savedEmployee.getUser().getId(), userResult.user().getId())) {
            savedEmployee.setUser(userResult.user());
            savedEmployee = employeeRepository.save(savedEmployee);
        }
        saveLocationMappings(savedEmployee, row.locationIds());

        String action = newEmployee ? "CREATED" : "UPDATED";
        String message = "Employee " + action.toLowerCase(Locale.ROOT) + " and user "
                + userResult.action().toLowerCase(Locale.ROOT) + " successfully.";
        return new EmployeeImportRowResult(
                row.rowNumber(),
                true,
                action,
                savedEmployee.getEmployeeId(),
                userResult.user().getId(),
                savedEmployee.getEmployeeCode(),
                savedEmployee.getFullName(),
                savedEmployee.getEmail(),
                savedEmployee.getRecruitmentType(),
                userResult.user().getEmail(),
                userResult.temporaryPassword(),
                message);
    }

    private EmployeeEntity resolveEmployee(EmployeeImportRow row) {
        if (StringUtils.hasText(row.employeeCode())) {
            return employeeRepository.findByEmployeeCodeIgnoreCase(row.employeeCode())
                    .orElseGet(EmployeeEntity::new);
        }

        return userRepository.findByEmailIgnoreCase(row.email())
                .flatMap(user -> employeeRepository.findByUser_Id(user.getId()))
                .orElseGet(EmployeeEntity::new);
    }

    private void applyGeneratedEmployeeCode(EmployeeImportRow row, EmployeeEntity employee, boolean newEmployee) {
        if (StringUtils.hasText(row.employeeCode())) {
            return;
        }
        if (!canReplaceEmployeeCode(employee, newEmployee)) {
            return;
        }

        String aadhaarEmployeeCode = resolveAvailableAadhaarEmployeeCode(
                row.aadhaarNumber(), employee.getEmployeeId());
        if (!StringUtils.hasText(aadhaarEmployeeCode)) {
            return;
        }
        employee.setEmployeeCode(aadhaarEmployeeCode);
    }

    private String resolveFinalEmployeeCode(EmployeeEntity employee) {
        if (StringUtils.hasText(employee.getEmployeeCode())
                && !employee.getEmployeeCode().startsWith(TEMPORARY_EMPLOYEE_CODE_PREFIX)) {
            return employee.getEmployeeCode();
        }
        return LEGACY_EMPLOYEE_CODE_PREFIX + String.format("%06d", employee.getEmployeeId());
    }

    private boolean canReplaceEmployeeCode(EmployeeEntity employee, boolean newEmployee) {
        if (newEmployee) {
            return true;
        }
        String currentCode = employee.getEmployeeCode();
        return !StringUtils.hasText(currentCode) || currentCode.startsWith(TEMPORARY_EMPLOYEE_CODE_PREFIX);
    }

    private String generateEmployeeCodeFromAadhaar(String aadhaarNumber, int suffixLength) {
        String aadhaarDigits = digitsOnly(aadhaarNumber);
        if (aadhaarDigits.length() < suffixLength) {
            return null;
        }
        return AADHAAR_EMPLOYEE_CODE_PREFIX
                + aadhaarDigits.substring(aadhaarDigits.length() - suffixLength);
    }

    private String digitsOnly(String value) {
        return StringUtils.hasText(value) ? value.replaceAll("\\D", "") : "";
    }

    private String resolveAvailableAadhaarEmployeeCode(String aadhaarNumber, Long employeeId) {
        String fourDigitCode = generateEmployeeCodeFromAadhaar(aadhaarNumber, 4);
        if (!StringUtils.hasText(fourDigitCode)) {
            return null;
        }
        if (isEmployeeCodeAvailable(fourDigitCode, employeeId)) {
            return fourDigitCode;
        }

        String threeDigitCode = generateEmployeeCodeFromAadhaar(aadhaarNumber, 3);
        if (isEmployeeCodeAvailable(threeDigitCode, employeeId)) {
            return threeDigitCode;
        }

        throw new IllegalArgumentException(
                "Generated employee codes " + fourDigitCode + " and " + threeDigitCode
                        + " already exist for other employees.");
    }

    private boolean isEmployeeCodeAvailable(String employeeCode, Long employeeId) {
        return employeeRepository.findByEmployeeCodeIgnoreCase(employeeCode)
                .map(existing -> Objects.equals(existing.getEmployeeId(), employeeId))
                .orElse(true);
    }

    private void validateEmployeeUniqueness(EmployeeImportRow row, EmployeeEntity employee) {
        Long employeeId = employee.getEmployeeId();
        Long excludedEmployeeId = employeeId != null ? employeeId : -1L;
        if (StringUtils.hasText(row.employeeCode())) {
            employeeRepository.findByEmployeeCodeIgnoreCase(row.employeeCode())
                    .filter(existing -> !Objects.equals(existing.getEmployeeId(), employeeId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Employee code already exists for another employee.");
                    });
        }
        if (employeeRepository.existsByEmailIgnoreCaseAndEmployeeIdNot(row.email(), excludedEmployeeId)) {
            throw new IllegalArgumentException("Email already exists for another employee.");
        }
        if (employeeRepository.existsByMobileAndEmployeeIdNot(row.mobile(), excludedEmployeeId)) {
            throw new IllegalArgumentException("Mobile number already exists for another employee.");
        }

        Long excludedUserId = employee.getUser() != null && employee.getUser().getId() != null
                ? employee.getUser().getId()
                : userRepository.findByEmailIgnoreCase(row.email())
                .map(User::getId)
                .orElse(-1L);
        if (userRepository.existsByMobileNoAndIdNot(row.mobile(), excludedUserId)) {
            throw new IllegalArgumentException("Mobile number already exists for another user.");
        }
    }

    private UserAccountResult upsertUser(
            EmployeeImportRow row,
            EmployeeEntity employee,
            DepartmentRegistrationEntity departmentRegistration,
            Role employeeRole) {
        User existingUser = employee.getUser() != null && employee.getUser().getId() != null
                ? employee.getUser()
                : userRepository.findByEmailIgnoreCase(row.email()).orElse(null);
        if (existingUser != null && !Boolean.TRUE.equals(existingUser.getActive())) {
            throw new IllegalArgumentException("Inactive user already exists for this email. Reactivate manually.");
        }

        if (existingUser == null) {
            String temporaryPassword = StringUtils.hasText(row.temporaryPassword())
                    ? UserValidationUtil.validatePassword(row.temporaryPassword())
                    : SecurePasswordGenerator.generate(12);
            UserUpsertRequest request = new UserUpsertRequest();
            request.setName(row.fullName());
            request.setEmail(row.email());
            request.setMobileNo(row.mobile());
            request.setPassword(temporaryPassword);
            request.setDepartmentRegistrationId(
                    departmentRegistration != null ? departmentRegistration.getDepartmentRegistrationId() : null);
            request.setAgencyId(null);
            request.setRoleIds(List.of(employeeRole.getId()));
            User created = userManagementService.create(request);
            return new UserAccountResult(created, "CREATED", temporaryPassword);
        }

        existingUser.setName(UserValidationUtil.normalizeName(row.fullName()));
        existingUser.setEmail(UserValidationUtil.normalizeEmail(row.email()));
        existingUser.setMobileNo(UserValidationUtil.normalizeOptionalMobile(row.mobile()));
        existingUser.setDepartmentRegistrationId(departmentRegistration);
        if (StringUtils.hasText(row.temporaryPassword())) {
            existingUser.setPassword(passwordEncoder.encode(UserValidationUtil.validatePassword(row.temporaryPassword())));
            existingUser.setPasswordChangeRequired(true);
        }
        if (existingUser.getRoles().stream().noneMatch(role -> Objects.equals(role.getId(), employeeRole.getId()))) {
            existingUser.addRole(employeeRole);
        }

        User saved = userRepository.save(existingUser);
        userAffiliationService.synchronizeUserProfile(saved);
        userAffiliationService.synchronizePrimaryDepartment(saved, departmentRegistration);
        return new UserAccountResult(saved, "UPDATED", StringUtils.hasText(row.temporaryPassword())
                ? row.temporaryPassword()
                : null);
    }

    private void applyEmployee(
            EmployeeImportRow row,
            EmployeeEntity employee,
            DepartmentRegistrationEntity departmentRegistration,
            DepartmentMst department,
            SubDepartment subDepartment,
            ManpowerDesignationMaster designation,
            AgencyMaster agency) {
        if (StringUtils.hasText(row.employeeCode())) {
            employee.setEmployeeCode(row.employeeCode());
        }
        employee.setRecruitmentType(row.recruitmentType());
        employee.setFullName(row.fullName());
        employee.setEmail(row.email());
        employee.setMobile(row.mobile());
        employee.setDateOfBirth(row.dateOfBirth());
        employee.setGender(row.gender());
        employee.setBloodGroup(row.bloodGroup());
        employee.setAddress(row.address());
        employee.setEmergencyContactName(row.emergencyContactName());
        employee.setEmergencyContactRelation(row.emergencyContactRelation());
        employee.setEmergencyContactMobile(row.emergencyContactMobile());
        employee.setEmergencyContactAltMobile(row.emergencyContactAltMobile());
        employee.setJoiningDate(row.joiningDate());
        employee.setOnboardingDate(row.onboardingDate());
        employee.setPanNumber(row.panNumber());
        employee.setAadhaarNumber(row.aadhaarNumber());
        employee.setDepartmentRegistration(departmentRegistration);
        employee.setDepartment(department);
        employee.setSubDepartment(subDepartment);
        employee.setDesignation(designation);
        employee.setAgency(agency);
        employee.setLevelCode(row.levelCode());
        employee.setRequestId(row.requestId());
        employee.setStatus(row.status());
        employee.setCompanyPayrollMoreThanThreeMonths(false);
    }

    private void saveLocationMappings(EmployeeEntity employee, Set<Long> locationIds) {
        if (employee.getEmployeeId() == null || locationIds.isEmpty()) {
            return;
        }

        List<LocationMaster> locations = locationRepository.findAllById(locationIds);
        if (locations.size() != locationIds.size()) {
            throw new IllegalArgumentException("One or more location IDs are invalid.");
        }

        List<LocationMaster> inactiveLocations = locations.stream()
                .filter(location -> !ACTIVE_FLAG.equalsIgnoreCase(location.getActiveFlag()))
                .toList();
        if (!inactiveLocations.isEmpty()) {
            throw new IllegalArgumentException("Inactive locations cannot be mapped.");
        }

        Set<Long> existingLocationIds = employeeLocationMappingRepository
                .findByEmployeeEmployeeIdOrderByLocationLocationNameAsc(employee.getEmployeeId())
                .stream()
                .map(EmployeeLocationMappingEntity::getLocation)
                .filter(Objects::nonNull)
                .map(LocationMaster::getLocationId)
                .collect(Collectors.toSet());

        List<EmployeeLocationMappingEntity> mappingsToSave = locations.stream()
                .filter(location -> !existingLocationIds.contains(location.getLocationId()))
                .map(location -> {
                    EmployeeLocationMappingEntity mapping = new EmployeeLocationMappingEntity();
                    mapping.setEmployee(employee);
                    mapping.setLocation(location);
                    return mapping;
                })
                .toList();
        if (!mappingsToSave.isEmpty()) {
            employeeLocationMappingRepository.saveAll(mappingsToSave);
        }
    }

    private EmployeeImportRow toImportRow(CsvRow row) {
        String email = normalizeEmail(required(row, "email"));
        String mobile = normalizeRequiredMobile(required(row, "mobile"));
        String temporaryPassword = optional(row, "temporaryPassword");
        if (StringUtils.hasText(temporaryPassword)) {
            temporaryPassword = UserValidationUtil.validatePassword(temporaryPassword);
        }

        return new EmployeeImportRow(
                row.rowNumber(),
                normalizeEmployeeCode(optional(row, "employeeCode")),
                normalizeRecruitmentType(required(row, "recruitmentType")),
                UserValidationUtil.normalizeName(required(row, "fullName")),
                email,
                mobile,
                parseOptionalDate(optional(row, "dateOfBirth"), "dateOfBirth", DEFAULT_DATE_OF_BIRTH),
                normalizeOptionalText(optional(row, "gender"), DEFAULT_GENDER),
                normalizeOptionalText(optional(row, "bloodGroup"), DEFAULT_BLOOD_GROUP),
                normalizeOptionalText(optional(row, "address"), DEFAULT_TEXT_VALUE),
                normalizeOptionalText(optional(row, "emergencyContactName"), DEFAULT_TEXT_VALUE),
                normalizeOptionalText(optional(row, "emergencyContactRelation"), DEFAULT_TEXT_VALUE),
                normalizeOptionalMobile(optional(row, "emergencyContactMobile"), mobile),
                normalizeOptionalMobile(optional(row, "emergencyContactAltMobile")),
                parseDate(required(row, "joiningDate"), "joiningDate"),
                parseDate(required(row, "onboardingDate"), "onboardingDate"),
                normalizePan(optional(row, "panNumber")),
                normalizeAadhaar(optional(row, "aadhaarNumber")),
                parseOptionalLong(optional(row, "departmentRegistrationId"), "departmentRegistrationId"),
                parseOptionalLong(optional(row, "subDepartmentId"), "subDepartmentId"),
                normalizeOptionalText(optional(row, "subDepartmentName")),
                parseOptionalLong(optional(row, "designationId"), "designationId"),
                parseRequiredLong(required(row, "agencyId"), "agencyId"),
                normalizeOptionalText(optional(row, "levelCode")),
                normalizeOptionalText(optional(row, "requestId")),
                normalizeStatus(optional(row, "status")),
                parseLocationIds(optional(row, "locationIds")),
                temporaryPassword);
    }

    private void validateDuplicateInFile(
            EmployeeImportRow row,
            Set<String> importedCodes,
            Set<String> importedEmails,
            Set<String> importedMobiles) {
        if (StringUtils.hasText(row.employeeCode())
                && !importedCodes.add(row.employeeCode().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Employee code is duplicated in this CSV file.");
        }
        if (!importedEmails.add(row.email().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Email is duplicated in this CSV file.");
        }
        if (!importedMobiles.add(row.mobile())) {
            throw new IllegalArgumentException("Mobile number is duplicated in this CSV file.");
        }
    }

    private DepartmentRegistrationEntity resolveDepartmentRegistration(Long departmentRegistrationId) {
        if (departmentRegistrationId == null) {
            return null;
        }
        return departmentRegistrationRepository.findById(departmentRegistrationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Department registration ID not found: " + departmentRegistrationId));
    }

    private SubDepartment resolveSubDepartment(
            Long subDepartmentId,
            String subDepartmentName,
            DepartmentRegistrationEntity departmentRegistration) {
        if (subDepartmentId != null) {
            return subDepartmentRepository.findById(subDepartmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Sub department ID not found: " + subDepartmentId));
        }
        if (StringUtils.hasText(subDepartmentName)) {
            return resolveSubDepartmentByName(subDepartmentName, departmentRegistration);
        }
        if (departmentRegistration == null || departmentRegistration.getSubDeptId() == null) {
            return null;
        }
        Long registrationSubDepartmentId = departmentRegistration.getSubDeptId();
        return subDepartmentRepository.findById(registrationSubDepartmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sub department ID not found: " + registrationSubDepartmentId));
    }

    private SubDepartment resolveSubDepartmentByName(
            String subDepartmentName,
            DepartmentRegistrationEntity departmentRegistration) {
        String normalizedSubDepartmentName = normalizeLookupText(subDepartmentName);
        Long departmentId = departmentRegistration != null ? departmentRegistration.getDepartmentId() : null;
        List<SubDepartment> matches = departmentId != null
                ? subDepartmentRepository.findByDepartmentDepartmentIdAndSubDeptNameIgnoreCaseOrderBySubDeptIdAsc(
                        departmentId,
                        normalizedSubDepartmentName)
                : subDepartmentRepository.findBySubDeptNameIgnoreCaseOrderBySubDeptIdAsc(normalizedSubDepartmentName);

        if (matches.isEmpty()) {
            String scope = departmentId != null ? " for department ID " + departmentId : "";
            throw new IllegalArgumentException("Sub department name not found" + scope + ": " + subDepartmentName);
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "Sub department name matches multiple departments. Use subDepartmentId or departmentRegistrationId: "
                            + subDepartmentName);
        }
        return matches.get(0);
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

    private ManpowerDesignationMaster resolveDesignation(Long designationId) {
        if (designationId == null) {
            return null;
        }
        return designationRepository.findByDesignationIdAndActiveFlagIgnoreCase(designationId, ACTIVE_FLAG)
                .orElseThrow(() -> new IllegalArgumentException("Active designation ID not found: " + designationId));
    }

    private AgencyMaster resolveAgency(Long agencyId) {
        AgencyMaster agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Agency ID not found: " + agencyId));
        if (agency.getStatus() != AgencyStatus.ACTIVE) {
            throw new IllegalArgumentException("Agency is not active: " + agencyId);
        }
        return agency;
    }

    private List<CsvRow> readCsvRows(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<CsvRecord> records = readCsvRecords(reader);
            if (records.isEmpty()) {
                throw new IllegalArgumentException("CSV header row is required.");
            }

            CsvRecord headerRecord = records.get(0);
            List<String> headers = parseCsvLine(stripBom(headerRecord.value())).stream()
                    .map(this::normalizeHeader)
                    .toList();
            validateHeaders(headers);

            List<CsvRow> rows = new ArrayList<>();
            for (int recordIndex = 1; recordIndex < records.size(); recordIndex++) {
                CsvRecord record = records.get(recordIndex);
                List<String> values = parseCsvLine(record.value());
                Map<String, String> valueByHeader = new LinkedHashMap<>();
                for (int index = 0; index < headers.size(); index++) {
                    String value = index < values.size() ? values.get(index) : "";
                    valueByHeader.put(headers.get(index), value);
                }
                if (valueByHeader.values().stream().allMatch(value -> !StringUtils.hasText(value))) {
                    continue;
                }
                rows.add(new CsvRow(record.startLineNumber(), valueByHeader));
            }
            return rows;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read uploaded CSV file.", ex);
        }
    }

    private List<CsvRecord> readCsvRecords(BufferedReader reader) throws IOException {
        List<CsvRecord> records = new ArrayList<>();
        StringBuilder currentRecord = new StringBuilder();
        boolean quoted = false;
        int startLineNumber = 0;
        int lineNumber = 0;
        String line;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (currentRecord.length() == 0) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                startLineNumber = lineNumber;
            } else {
                currentRecord.append('\n');
            }
            currentRecord.append(line);
            quoted = updateQuotedState(line, quoted);
            if (!quoted) {
                records.add(new CsvRecord(startLineNumber, currentRecord.toString()));
                currentRecord.setLength(0);
                startLineNumber = 0;
            }
        }

        if (quoted) {
            throw new IllegalArgumentException(
                    "CSV row starting at line " + startLineNumber + " has an unclosed quoted value.");
        }
        return records;
    }

    private boolean updateQuotedState(String line, boolean quoted) {
        boolean currentQuoted = quoted;
        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (value != '"') {
                continue;
            }
            if (currentQuoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                index++;
                continue;
            }
            currentQuoted = !currentQuoted;
        }
        return currentQuoted;
    }

    private void validateHeaders(List<String> headers) {
        Set<String> presentHeaders = new LinkedHashSet<>(headers);
        List<String> missingHeaders = REQUIRED_HEADERS.stream()
                .filter(required -> !presentHeaders.contains(required))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new IllegalArgumentException("CSV is missing required columns: " + String.join(", ", missingHeaders));
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (value == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (value == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(value);
        }
        if (quoted) {
            throw new IllegalArgumentException("CSV row has an unclosed quoted value.");
        }
        values.add(current.toString().trim());
        return values;
    }

    private EmployeeImportRowResult toFailedResult(CsvRow csvRow, EmployeeImportRow row, String message) {
        return new EmployeeImportRowResult(
                csvRow.rowNumber(),
                false,
                "FAILED",
                null,
                null,
                row != null ? row.employeeCode() : optional(csvRow, "employeeCode"),
                row != null ? row.fullName() : optional(csvRow, "fullName"),
                row != null ? row.email() : optional(csvRow, "email"),
                row != null ? row.recruitmentType() : optional(csvRow, "recruitmentType"),
                null,
                null,
                message);
    }

    private String rootMessage(RuntimeException ex) {
        if (ex.getCause() instanceof RuntimeException runtimeException && runtimeException.getMessage() != null) {
            return runtimeException.getMessage();
        }
        return ex.getMessage() != null ? ex.getMessage() : "Import failed.";
    }

    private String required(CsvRow row, String header) {
        String value = optional(row, header);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(header + " is required.");
        }
        return value;
    }

    private String optional(CsvRow row, String header) {
        return row.values().getOrDefault(normalizeHeader(header), "").trim();
    }

    private String normalizeHeader(String header) {
        return stripBom(header).trim().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private String stripBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF'
                ? value.substring(1)
                : value;
    }

    private String normalizeEmployeeCode(String employeeCode) {
        return normalizeOptionalText(employeeCode);
    }

    private String normalizeRecruitmentType(String recruitmentType) {
        String normalized = normalizeRequiredText(recruitmentType, "recruitmentType").toUpperCase(Locale.ROOT);
        if (!"INTERNAL".equals(normalized) && !"EXTERNAL".equals(normalized)) {
            throw new IllegalArgumentException("recruitmentType must be INTERNAL or EXTERNAL.");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return ACTIVE_STATUS;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ACTIVE_STATUS.equals(normalized) && !"RESIGNED".equals(normalized)) {
            throw new IllegalArgumentException("status must be ACTIVE or RESIGNED.");
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLookupText(String value) {
        return normalizeRequiredText(value, "subDepartmentName");
    }

    private String normalizeOptionalText(String value, String defaultValue) {
        String normalized = normalizeOptionalText(value);
        return normalized != null ? normalized : defaultValue;
    }

    private String normalizeEmail(String value) {
        try {
            return UserValidationUtil.normalizeEmail(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("email: " + ex.getMessage(), ex);
        }
    }

    private String normalizeRequiredMobile(String value) {
        String mobile = normalizeOptionalMobile(value);
        if (!StringUtils.hasText(mobile)) {
            throw new IllegalArgumentException("mobile number is required.");
        }
        return mobile;
    }

    private String normalizeOptionalMobile(String value, String defaultMobile) {
        String mobile = normalizeOptionalMobile(value);
        return StringUtils.hasText(mobile) ? mobile : defaultMobile;
    }

    private String normalizeOptionalMobile(String value) {
        try {
            String extractedMobile = extractFirstMobileCandidate(value);
            return UserValidationUtil.normalizeOptionalMobile(extractedMobile);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private String extractFirstMobileCandidate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        Matcher matcher = MOBILE_CANDIDATE_PATTERN.matcher(trimmed);
        while (matcher.find()) {
            String digits = matcher.group().replaceAll("\\D", "");
            if (digits.length() >= 10 && digits.length() <= 15) {
                return digits;
            }
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() >= 10 && digits.length() <= 15) {
            return digits;
        }
        return trimmed;
    }

    private String normalizePan(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : DEFAULT_TEXT_VALUE;
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("panNumber must not exceed 255 characters.");
        }
        return normalized;
    }

    private String normalizeAadhaar(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_TEXT_VALUE;
        }
        String normalized = normalizeRequiredText(value, "aadhaarNumber").replaceAll("\\s+", "");
        String digitsOnly = normalized.replaceAll("\\D", "");
        if (digitsOnly.length() < 4) {
            throw new IllegalArgumentException("aadhaarNumber must contain at least 4 digits.");
        }
        return normalized;
    }

    private LocalDate parseDate(String value, String fieldName) {
        String normalized = normalizeDateText(value);
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ex) {
                // Try the next supported import format.
            }
        }
        throw new IllegalArgumentException(
                fieldName + " must be a valid date. Preferred format is yyyy-MM-dd.");
    }

    private LocalDate parseOptionalDate(String value, String fieldName, LocalDate defaultValue) {
        return StringUtils.hasText(value) ? parseDate(value, fieldName) : defaultValue;
    }

    private String normalizeDateText(String value) {
        String normalized = normalizeRequiredText(value, "date")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        normalized = ORDINAL_DAY_PATTERN.matcher(normalized).replaceAll("$1");
        return normalized.replaceAll("(?i)\\bsept\\b", "Sep");
    }

    private static DateTimeFormatter monthNameFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.STRICT);
    }

    private static DateTimeFormatter reducedYearMonthNameFormatter(String prefixPattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(prefixPattern)
                .appendValueReduced(ChronoField.YEAR, 2, 2, 1950)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.STRICT);
    }

    private static DateTimeFormatter numericDateFormatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern)
                .withResolverStyle(ResolverStyle.STRICT);
    }

    private Long parseOptionalLong(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseRequiredLong(value, fieldName);
    }

    private Long parseRequiredLong(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < 1) {
                throw new IllegalArgumentException(fieldName + " must be greater than zero.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.", ex);
        }
    }

    private Set<Long> parseLocationIds(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        Set<Long> locationIds = new LinkedHashSet<>();
        String[] parts = value.split("[|;]");
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            locationIds.add(parseOptionalLong(part, "locationIds"));
        }
        return locationIds;
    }

    private String generateTemporaryEmployeeCode() {
        return "TMP-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String toCsvLine(List<String> values) {
        return values.stream()
                .map(this::csvValue)
                .collect(Collectors.joining(","));
    }

    private String csvValue(String value) {
        String safeValue = value == null ? "" : value;
        boolean requiresQuoting = safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n")
                || safeValue.contains("\r");
        String escaped = safeValue.replace("\"", "\"\"");
        return requiresQuoting ? "\"" + escaped + "\"" : escaped;
    }

    private record CsvRow(int rowNumber, Map<String, String> values) {
    }

    private record CsvRecord(int startLineNumber, String value) {
    }

    private record EmployeeImportRow(
            int rowNumber,
            String employeeCode,
            String recruitmentType,
            String fullName,
            String email,
            String mobile,
            LocalDate dateOfBirth,
            String gender,
            String bloodGroup,
            String address,
            String emergencyContactName,
            String emergencyContactRelation,
            String emergencyContactMobile,
            String emergencyContactAltMobile,
            LocalDate joiningDate,
            LocalDate onboardingDate,
            String panNumber,
            String aadhaarNumber,
            Long departmentRegistrationId,
            Long subDepartmentId,
            String subDepartmentName,
            Long designationId,
            Long agencyId,
            String levelCode,
            String requestId,
            String status,
            Set<Long> locationIds,
            String temporaryPassword) {

        public EmployeeImportRow {
            locationIds = locationIds == null ? Set.of() : Set.copyOf(locationIds);
        }
    }

    private record UserAccountResult(User user, String action, String temporaryPassword) {
    }
}
