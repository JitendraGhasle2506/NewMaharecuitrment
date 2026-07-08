package com.maharecruitment.gov.in.web.service.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class MobileEmployeeDetailsServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeReportingMappingRepository reportingMappingRepository;

    @Mock
    private SubDepartmentRepository subDepartmentRepository;

    @Mock
    private DepartmentMstRepository departmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @TempDir
    private Path tempDir;

    @Test
    void internalEmployeeReturnsDepartmentDesignationInlinePhotoAndReportingManager() throws Exception {
        MobileEmployeeDetailsService service = service();
        User user = user(9L, "Shiv Krushna", "shiva@gmail.com", "7998966981");
        EmployeeEntity employee = employee(101L, "EMP001", "Shiv Krushna", "shiva@gmail.com", "INTERNAL");
        employee.setDepartmentRegistration(departmentRegistration(22L, 2L, "IT Department", 5L));
        employee.setSubDepartment(subDepartment(5L, "Development", department(2L, "IT Department")));
        employee.setDesignation(designation(3L, "Project Manager"));
        byte[] photoBytes = new byte[] { 1, 2, 3, 4, 5 };
        Path photoPath = tempDir.resolve("photo.jpg");
        Files.write(photoPath, photoBytes);
        employee.setPreOnboarding(preOnboarding(photoPath.toString(), "[0.123,0.456]"));
        employee.setEmbedding("[0.987,0.654]");

        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setEmployeeId(101L);
        mapping.setManagerEmployeeId(15L);

        EmployeeEntity manager = employee(15L, "EMP015", "Mahesh Patil", "mahesh@example.com", "INTERNAL");

        when(employeeRepository.findMobileLoginProfileByUserId(9L)).thenReturn(Optional.of(employee));
        when(reportingMappingRepository.findFirstByEmployeeIdOrderByMappingIdDesc(101L))
                .thenReturn(Optional.of(mapping));
        when(employeeRepository.findById(15L)).thenReturn(Optional.of(manager));
        when(fileStorageService.resolveManagedPath(photoPath.toString())).thenReturn(Optional.of(photoPath));

        MobileEmployeeDetails details = service.loadForUser(user);

        assertThat(details.empId()).isEqualTo(101L);
        assertThat(details.employeeCode()).isEqualTo("EMP001");
        assertThat(details.employeeName()).isEqualTo("Shiv Krushna");
        assertThat(details.designationId()).isEqualTo(3L);
        assertThat(details.designationName()).isEqualTo("Project Manager");
        assertThat(details.employeeType()).isEqualTo("INTERNAL");
        assertThat(details.departmentId()).isEqualTo(2L);
        assertThat(details.departmentName()).isEqualTo("IT Department");
        assertThat(details.subDepartmentId()).isEqualTo(5L);
        assertThat(details.subDepartmentName()).isEqualTo("Development");
        assertThat(details.reportingManagerId()).isEqualTo(15L);
        assertThat(details.reportingManagerName()).isEqualTo("Mahesh Patil");
        assertThat(details.reportingDepartmentId()).isNull();
        assertThat(details.reportingDepartmentName()).isNull();
        assertThat(decodedDataImage(details.photoUrl())).isEqualTo(photoBytes);
        assertThat(details.faceData()).isEqualTo("[0.987,0.654]");
    }

    @Test
    void externalEmployeeReportsToMappedDepartmentWhenManagerIsMissing() {
        MobileEmployeeDetailsService service = service();
        User user = user(10L, "External User", "external@example.com", "9888888888");
        EmployeeEntity employee = employee(102L, "EMP002", "External User", "external@example.com", "external");
        employee.setDepartmentRegistration(departmentRegistration(25L, 7L, "Finance Department", null));

        when(employeeRepository.findMobileLoginProfileByUserId(10L)).thenReturn(Optional.of(employee));
        when(reportingMappingRepository.findFirstByEmployeeIdOrderByMappingIdDesc(102L))
                .thenReturn(Optional.empty());

        MobileEmployeeDetails details = service.loadForUser(user);

        assertThat(details.employeeType()).isEqualTo("EXTERNAL");
        assertThat(details.departmentId()).isEqualTo(7L);
        assertThat(details.departmentName()).isEqualTo("Finance Department");
        assertThat(details.subDepartmentId()).isNull();
        assertThat(details.subDepartmentName()).isNull();
        assertThat(details.reportingManagerId()).isNull();
        assertThat(details.reportingManagerName()).isNull();
        assertThat(details.reportingDepartmentId()).isEqualTo(7L);
        assertThat(details.reportingDepartmentName()).isEqualTo("Finance Department");
        assertThat(details.photoUrl()).isNull();
        assertThat(details.faceData()).isNull();
    }

    @Test
    void employeeWithoutSubDepartmentPhotoOrReportingMappingReturnsNullOptionalFields() {
        MobileEmployeeDetailsService service = service();
        User user = user(11L, "No Extras", "noextras@example.com", "9777777777");
        EmployeeEntity employee = employee(103L, "EMP003", "No Extras", "noextras@example.com", "INTERNAL");
        employee.setDepartmentRegistration(departmentRegistration(30L, 8L, "HR Department", null));
        employee.setPreOnboarding(preOnboarding(null));

        when(employeeRepository.findMobileLoginProfileByUserId(11L)).thenReturn(Optional.of(employee));
        when(reportingMappingRepository.findFirstByEmployeeIdOrderByMappingIdDesc(103L))
                .thenReturn(Optional.empty());

        MobileEmployeeDetails details = service.loadForUser(user);

        assertThat(details.departmentId()).isEqualTo(8L);
        assertThat(details.departmentName()).isEqualTo("HR Department");
        assertThat(details.subDepartmentId()).isNull();
        assertThat(details.subDepartmentName()).isNull();
        assertThat(details.photoUrl()).isNull();
        assertThat(details.reportingManagerId()).isNull();
        assertThat(details.reportingManagerName()).isNull();
        assertThat(details.reportingDepartmentId()).isNull();
        assertThat(details.reportingDepartmentName()).isNull();
    }

    @Test
    void usesPreOnboardingPhotoForMappedEmployeeProfile() throws Exception {
        MobileEmployeeDetailsService service = service();
        User user = user(13L, "Primary Employee", "shared@example.com", "9555555555");
        EmployeeEntity primaryProfile = employee(201L, "EMP201", "Primary Employee", "shared@example.com", "INTERNAL");
        primaryProfile.setDepartmentRegistration(departmentRegistration(40L, 9L, "Operations", null));

        byte[] photoBytes = new byte[] { 9, 8, 7 };
        Path photoPath = tempDir.resolve("profile-photo.png");
        Files.write(photoPath, photoBytes);
        primaryProfile.setPreOnboarding(preOnboarding(photoPath.toString(), "[0.789,0.321]"));

        when(employeeRepository.findMobileLoginProfileByUserId(13L)).thenReturn(Optional.of(primaryProfile));
        when(reportingMappingRepository.findFirstByEmployeeIdOrderByMappingIdDesc(201L))
                .thenReturn(Optional.empty());
        when(fileStorageService.resolveManagedPath(photoPath.toString())).thenReturn(Optional.of(photoPath));

        MobileEmployeeDetails details = service.loadForUser(user);

        assertThat(details.empId()).isEqualTo(201L);
        assertThat(decodedDataImage(details.photoUrl(), "image/png")).isEqualTo(photoBytes);
        assertThat(details.faceData()).isEqualTo("[0.789,0.321]");
    }

    @Test
    void usesPreOnboardingEmbeddingWhenEmployeeMasterEmbeddedIsMissing() throws Exception {
        MobileEmployeeDetailsService service = service();
        User user = user(14L, "Fallback Employee", "fallback@example.com", "9444444444");
        EmployeeEntity employee = employee(301L, "EMP301", "Fallback Employee", "fallback@example.com", "INTERNAL");
        Path photoPath = tempDir.resolve("fallback-embedding-photo.jpg");
        Files.write(photoPath, new byte[] { 4, 5, 6 });
        employee.setPreOnboarding(preOnboarding(photoPath.toString(), "[0.111,0.222]"));

        when(employeeRepository.findMobileLoginProfileByUserId(14L)).thenReturn(Optional.of(employee));
        when(reportingMappingRepository.findFirstByEmployeeIdOrderByMappingIdDesc(301L))
                .thenReturn(Optional.empty());
        when(fileStorageService.resolveManagedPath(photoPath.toString())).thenReturn(Optional.of(photoPath));

        MobileEmployeeDetails details = service.loadForUser(user);

        assertThat(details.faceData()).isEqualTo("[0.111,0.222]");
    }

    @Test
    void missingEmployeeProfileReturnsEmptyDetails() {
        MobileEmployeeDetailsService service = service();
        User user = user(12L, "Only User", "onlyuser@example.com", "9666666666");

        when(employeeRepository.findMobileLoginProfileByUserId(12L)).thenReturn(Optional.empty());

        MobileEmployeeDetails details = service.loadForUser(user);

        assertThat(details).isEqualTo(MobileEmployeeDetails.empty());
        verifyNoInteractions(reportingMappingRepository, subDepartmentRepository, departmentRepository, fileStorageService);
    }

    private MobileEmployeeDetailsService service() {
        return new MobileEmployeeDetailsService(
                employeeRepository,
                reportingMappingRepository,
                subDepartmentRepository,
                departmentRepository,
                fileStorageService);
    }

    private User user(Long userId, String name, String email, String mobileNo) {
        User user = new User();
        user.setId(userId);
        user.setName(name);
        user.setEmail(email);
        user.setMobileNo(mobileNo);
        return user;
    }

    private EmployeeEntity employee(
            Long employeeId,
            String employeeCode,
            String fullName,
            String email,
            String recruitmentType) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode(employeeCode);
        employee.setFullName(fullName);
        employee.setEmail(email);
        employee.setMobile("9000000000");
        employee.setRecruitmentType(recruitmentType);
        employee.setStatus("ACTIVE");
        return employee;
    }

    private DepartmentRegistrationEntity departmentRegistration(
            Long departmentRegistrationId,
            Long departmentId,
            String departmentName,
            Long subDepartmentId) {
        DepartmentRegistrationEntity registration = new DepartmentRegistrationEntity();
        registration.setDepartmentRegistrationId(departmentRegistrationId);
        registration.setDepartmentId(departmentId);
        registration.setDepartmentName(departmentName);
        registration.setSubDeptId(subDepartmentId);
        return registration;
    }

    private DepartmentMst department(Long departmentId, String departmentName) {
        DepartmentMst department = new DepartmentMst();
        department.setDepartmentId(departmentId);
        department.setDepartmentName(departmentName);
        return department;
    }

    private SubDepartment subDepartment(Long subDepartmentId, String subDepartmentName, DepartmentMst department) {
        SubDepartment subDepartment = new SubDepartment();
        subDepartment.setSubDeptId(subDepartmentId);
        subDepartment.setSubDeptName(subDepartmentName);
        subDepartment.setDepartment(department);
        return subDepartment;
    }

    private ManpowerDesignationMaster designation(Long designationId, String designationName) {
        ManpowerDesignationMaster designation = new ManpowerDesignationMaster();
        designation.setDesignationId(designationId);
        designation.setDesignationName(designationName);
        return designation;
    }

    private AgencyCandidatePreOnboardingEntity preOnboarding(String photoFilePath) {
        return preOnboarding(photoFilePath, null);
    }

    private AgencyCandidatePreOnboardingEntity preOnboarding(String photoFilePath, String embedding) {
        AgencyCandidatePreOnboardingEntity preOnboarding = new AgencyCandidatePreOnboardingEntity();
        preOnboarding.setPhotoFilePath(photoFilePath);
        preOnboarding.setEmbedding(embedding);
        return preOnboarding;
    }

    private byte[] decodedDataImage(String photoUrl) {
        return decodedDataImage(photoUrl, "image/jpeg");
    }

    private byte[] decodedDataImage(String photoUrl, String contentType) {
        String prefix = "data:" + contentType + ";base64,";
        assertThat(photoUrl).startsWith(prefix);
        String encodedImage = photoUrl.substring(prefix.length());
        return Base64.getDecoder().decode(encodedImage);
    }
}
