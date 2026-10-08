package com.maharecruitment.gov.in.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maharecruitment.gov.in.attendance.repository.ApprovalRecruitmentTypeRuleRepository;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class MahaitApprovalRoutingServiceTest {

    @Mock private ApprovalRecruitmentTypeRuleRepository ruleRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeReportingMappingRepository mappingRepository;

    private RecruitmentTypeApprovalRoutingService service;

    @BeforeEach
    void setUp() {
        service = new RecruitmentTypeApprovalRoutingService(
                ruleRepository, employeeRepository, mappingRepository);
    }

    @Test
    void routesToManagerBeforeHodWhenTheyDiffer() {
        EmployeeEntity manager = employee(201L, "INTERNAL", user(7L));
        EmployeeReportingMappingEntity mapping = mapping(101L, 201L, 9L);

        when(ruleRepository.isEnabledForEmployee(101L)).thenReturn(true);
        when(mappingRepository.findFirstByEmployeeIdOrderByMappingIdDesc(101L))
                .thenReturn(Optional.of(mapping));
        when(employeeRepository.findById(201L)).thenReturn(Optional.of(manager));

        RecruitmentTypeApprovalRoutingService.ApprovalRoute route = service.routeNewRequest(101L);

        assertThat(route.initialStage()).isEqualTo(RecruitmentTypeApprovalRoutingService.STAGE_MANAGER);
        assertThat(route.managerUserId()).isEqualTo(7L);
        assertThat(route.hodUserId()).isEqualTo(9L);
    }

    @Test
    void routesDirectlyToHodWhenManagerAndHodAreSameUser() {
        EmployeeEntity manager = employee(201L, "INTERNAL", user(9L));
        EmployeeReportingMappingEntity mapping = mapping(101L, 201L, 9L);

        when(ruleRepository.isEnabledForEmployee(101L)).thenReturn(true);
        when(mappingRepository.findFirstByEmployeeIdOrderByMappingIdDesc(101L))
                .thenReturn(Optional.of(mapping));
        when(employeeRepository.findById(201L)).thenReturn(Optional.of(manager));

        assertThat(service.routeNewRequest(101L).initialStage())
                .isEqualTo(RecruitmentTypeApprovalRoutingService.STAGE_HOD);
    }

    @Test
    void usesLegacyWorkflowWhenRecruitmentTypeRuleIsNotEnabled() {
        when(ruleRepository.isEnabledForEmployee(101L)).thenReturn(false);

        assertThat(service.routeNewRequest(101L).applies()).isFalse();
    }

    private EmployeeEntity employee(Long id, String recruitmentType, User user) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(id);
        employee.setRecruitmentType(recruitmentType);
        employee.setUser(user);
        return employee;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private EmployeeReportingMappingEntity mapping(Long employeeId, Long managerEmployeeId, Long hodUserId) {
        EmployeeReportingMappingEntity mapping = new EmployeeReportingMappingEntity();
        mapping.setEmployeeId(employeeId);
        mapping.setManagerEmployeeId(managerEmployeeId);
        mapping.setHodUserId(hodUserId);
        return mapping;
    }
}
