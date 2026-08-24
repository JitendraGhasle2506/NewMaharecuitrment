package com.maharecruitment.gov.in.web.service.hr.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserManagementService;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.master.repository.DepartmentMstRepository;
import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;
import com.maharecruitment.gov.in.master.repository.SubDepartmentRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeLocationMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentDesignationVacancyRepository;
import com.maharecruitment.gov.in.web.service.onboarding.CandidateIdentityValidationService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;

@ExtendWith(MockitoExtension.class)
class HROnboardingPageServiceImplTest {

    @Mock
    private AgencyCandidatePreOnboardingRepository preOnboardingRepository;
    @Mock
    private DepartmentRegistrationRepository departmentRegistrationRepository;
    @Mock
    private DepartmentMstRepository departmentRepository;
    @Mock
    private SubDepartmentRepository subDepartmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentProjectApplicationRepository projectApplicationRepository;
    @Mock
    private RecruitmentDesignationVacancyRepository designationVacancyRepository;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AccountNotificationService accountNotificationService;
    @Mock
    private CandidateIdentityValidationService candidateIdentityValidationService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private LocationMasterRepository locationMasterRepository;
    @Mock
    private EmployeeLocationMappingRepository employeeLocationMappingRepository;

    @InjectMocks
    private HROnboardingPageServiceImpl service;

    @Test
    void onboardingLookupUsesPessimisticWriteLock() throws NoSuchMethodException {
        Lock lock = AgencyCandidatePreOnboardingRepository.class
                .getMethod("findByIdForOnboardingUpdate", Long.class)
                .getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void pendingLookupRequiresTheOnboardingMarkerToBeMissing() throws NoSuchMethodException {
        Query query = AgencyCandidatePreOnboardingRepository.class
                .getMethod("findPendingHROnboarding")
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value()).contains("preOnboarding.onboardedAt is null");
    }

    @Test
    void completedOnboardingIsIdempotentAfterLockedRead() {
        AgencyCandidatePreOnboardingEntity preOnboarding = new AgencyCandidatePreOnboardingEntity();
        preOnboarding.setPreOnboardingId(32L);
        preOnboarding.setOnboardedAt(LocalDateTime.now());
        User employeeUser = user(91L, "employee@example.test");
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(75L);
        employee.setEmployeeCode("EMP000075");
        employee.setEmail("employee@example.test");
        employee.setStatus("ACTIVE");
        employee.setUser(employeeUser);
        when(preOnboardingRepository.findByIdForOnboardingUpdate(32L))
                .thenReturn(Optional.of(preOnboarding));
        when(employeeRepository.findByPreOnboardingId(32L)).thenReturn(Optional.of(employee));

        EmployeeOnboardingResult result = service.saveOnboarding(32L, null, "hr@example.test");

        assertThat(result.userId()).isEqualTo(91L);
        assertThat(result.username()).isEqualTo("employee@example.test");
        assertThat(result.temporaryPassword()).isNull();
        verify(preOnboardingRepository).findByIdForOnboardingUpdate(32L);
        verify(employeeRepository, never()).save(any(EmployeeEntity.class));
        verifyNoInteractions(userRepository, userManagementService);
    }

    @Test
    void resumesExistingEmployeeWhenCompletionMarkerIsMissing() {
        RecruitmentNotificationEntity notification = new RecruitmentNotificationEntity();
        notification.setRecruitmentNotificationId(9L);
        notification.setRequestId("REQ-9-E");
        ManpowerDesignationMaster designation = new ManpowerDesignationMaster();
        designation.setDesignationId(4L);
        RecruitmentDesignationVacancyEntity vacancy = new RecruitmentDesignationVacancyEntity();
        vacancy.setRecruitmentDesignationVacancyId(7L);
        vacancy.setNumberOfVacancy(2L);
        vacancy.setDesignationMst(designation);
        vacancy.setLevelCode("L1");
        AgencyMaster agency = new AgencyMaster();
        agency.setAgencyId(5L);
        RecruitmentInterviewDetailEntity interview = new RecruitmentInterviewDetailEntity();
        interview.setRecruitmentInterviewDetailId(63L);
        interview.setRecruitmentNotification(notification);
        interview.setDesignationVacancy(vacancy);
        interview.setAgency(agency);

        AgencyCandidatePreOnboardingEntity preOnboarding = preOnboarding(32L, interview);
        User existingUser = user(91L, "candidate@example.test");
        EmployeeEntity existingEmployee = new EmployeeEntity();
        existingEmployee.setEmployeeId(75L);
        existingEmployee.setEmployeeCode("EMP000075");
        existingEmployee.setStatus("ACTIVE");
        existingEmployee.setUser(existingUser);
        User hrUser = user(12L, "hr@example.test");
        LocationMaster location = new LocationMaster();
        location.setLocationId(11L);
        location.setActiveFlag("Y");
        AgencyPreOnboardingForm form = new AgencyPreOnboardingForm();
        form.setName("Candidate Name");
        form.setEmail("candidate@example.test");
        form.setMobile("9876543210");
        form.setHrOnboardingDate(LocalDate.of(2026, 8, 24));
        form.setHrOnboardingLocation("Mumbai");
        form.setHrVerified(true);
        form.setSelectedLocationIds(List.of(11L));

        when(preOnboardingRepository.findByIdForOnboardingUpdate(32L))
                .thenReturn(Optional.of(preOnboarding));
        when(employeeRepository.findByPreOnboardingId(32L)).thenReturn(Optional.of(existingEmployee));
        when(userRepository.findByEmailIgnoreCase("hr@example.test")).thenReturn(Optional.of(hrUser));
        when(locationMasterRepository.findAllById(Set.of(11L))).thenReturn(List.of(location));
        when(designationVacancyRepository.findByIdForFinalDecisionUpdate(7L, 9L))
                .thenReturn(Optional.of(vacancy));
        when(employeeRepository
                .countByPreOnboardingInterviewDetailDesignationVacancyRecruitmentDesignationVacancyIdAndStatusIgnoreCase(
                        7L,
                        "ACTIVE"))
                .thenReturn(1L);
        when(employeeRepository.save(existingEmployee)).thenReturn(existingEmployee);
        when(employeeLocationMappingRepository.findLocationIdsByEmployeeId(75L)).thenReturn(Set.of(11L));

        EmployeeOnboardingResult result = service.saveOnboarding(32L, form, "hr@example.test");

        assertThat(result.userId()).isEqualTo(91L);
        assertThat(result.temporaryPassword()).isNull();
        assertThat(preOnboarding.getHrVerified()).isTrue();
        assertThat(preOnboarding.getOnboardedAt()).isNotNull();
        assertThat(preOnboarding.getHrUserId()).isEqualTo(12L);
        assertThat(existingEmployee.getPreOnboarding()).isSameAs(preOnboarding);
        assertThat(vacancy.getFillPost()).isEqualTo(1L);
        verify(employeeRepository).save(existingEmployee);
        verify(userManagementService, never()).create(any());
        verify(employeeLocationMappingRepository, never()).saveAll(any());
    }

    @Test
    void rejectsCompletionMarkerWithoutEmployeeRecord() {
        AgencyCandidatePreOnboardingEntity preOnboarding = new AgencyCandidatePreOnboardingEntity();
        preOnboarding.setPreOnboardingId(32L);
        preOnboarding.setOnboardedAt(LocalDateTime.now());
        when(preOnboardingRepository.findByIdForOnboardingUpdate(32L))
                .thenReturn(Optional.of(preOnboarding));
        when(employeeRepository.findByPreOnboardingId(32L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveOnboarding(32L, null, "hr@example.test"))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("Onboarding is marked complete, but the linked employee record is missing.");

        verifyNoInteractions(userRepository);
    }

    private AgencyCandidatePreOnboardingEntity preOnboarding(
            Long preOnboardingId,
            RecruitmentInterviewDetailEntity interview) {
        AgencyCandidatePreOnboardingEntity preOnboarding = new AgencyCandidatePreOnboardingEntity();
        preOnboarding.setPreOnboardingId(preOnboardingId);
        preOnboarding.setInterviewDetail(interview);
        preOnboarding.setCandidateName("Candidate Name");
        preOnboarding.setCandidateEmail("candidate@example.test");
        preOnboarding.setCandidateMobile("9876543210");
        preOnboarding.setAddress("Mumbai");
        preOnboarding.setEmergencyContactName("Emergency Contact");
        preOnboarding.setEmergencyContactRelation("Parent");
        preOnboarding.setEmergencyContactMobile("9876500000");
        preOnboarding.setDateOfBirth(LocalDate.of(1995, 1, 1));
        preOnboarding.setGender("MALE");
        preOnboarding.setBloodGroup("O+");
        preOnboarding.setJoiningDate(LocalDate.of(2026, 8, 24));
        preOnboarding.setPanNumber("ABCDE1234F");
        preOnboarding.setAadhaarNumber("123456789012");
        preOnboarding.setCompanyPayrollMoreThanThreeMonths(false);
        return preOnboarding;
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeOnboardingResult;
