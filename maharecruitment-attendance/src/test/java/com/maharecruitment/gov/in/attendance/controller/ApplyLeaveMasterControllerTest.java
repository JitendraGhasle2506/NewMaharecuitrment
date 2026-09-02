package com.maharecruitment.gov.in.attendance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;
import com.maharecruitment.gov.in.attendance.service.LeaveApplicationService;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.master.entity.LeaveEntity;
import com.maharecruitment.gov.in.master.repository.LeaveRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class ApplyLeaveMasterControllerTest {

    @Mock
    private LeaveApplicationService leaveApplicationService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveRepository leaveRepository;

    private ApplyLeaveMasterController controller;

    @BeforeEach
    void setUp() {
        controller = new ApplyLeaveMasterController(
                leaveApplicationService,
                employeeRepository,
                leaveRepository);
    }

    @Test
    void applyLeaveShowsPrimaryTypesFirstAndRemovesExcludedTypes() {
        EmployeeEntity employee = employee(101L);
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findAll()).thenReturn(List.of(
                leave("SL", "Special Leave"),
                leave("ML", "Medical Leave"),
                leave("HPL", "Half Pay Leave"),
                leave("EL", "Earned Leave"),
                leave("OD", "Official Duty"),
                leave("CL", "Casual Leave"),
                leave("RH", "Restricted Holiday")));

        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.showApplyLeaveForm(model, authenticatedSession());

        assertThat(view).isEqualTo("attendance/apply-leave");
        assertThat(leaveTypes(model))
                .extracting(LeaveEntity::getLeaveName)
                .containsExactly(
                        "Earned Leave",
                        "Casual Leave",
                        "Medical Leave",
                        "Comp Off",
                        "Official Duty");
    }

    @Test
    void submitLeaveRejectsAnExcludedLeaveType() {
        EmployeeEntity employee = employee(101L);
        when(employeeRepository.findByUser_Id(7L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findAll()).thenReturn(List.of(
                leave("EL", "Earned Leave"),
                leave("SL", "Special Leave")));

        LeaveApplicationEntity application = new LeaveApplicationEntity();
        application.setLeaveType("SL");
        application.setLeaveCategory("FULL_DAY");
        application.setStartDate(LocalDate.of(2026, 9, 3));
        application.setEndDate(LocalDate.of(2026, 9, 3));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(application, "leaveApplication");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.submitLeaveApplication(
                application,
                bindingResult,
                model,
                authenticatedSession(),
                new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("attendance/apply-leave");
        assertThat(model.get("error")).isEqualTo("Please select an available leave type.");
        assertThat(leaveTypes(model))
                .extracting(LeaveEntity::getLeaveName)
                .doesNotContain("Special Leave");
        verify(leaveApplicationService, never()).saveLeaveApplication(any());
    }

    @SuppressWarnings("unchecked")
    private List<LeaveEntity> leaveTypes(ExtendedModelMap model) {
        return (List<LeaveEntity>) model.get("leaveTypes");
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SESSION_USER", new SessionUserDTO(
                7L,
                "Employee",
                "employee@example.gov.in",
                List.of("ROLE_EMPLOYEE"),
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()));
        return session;
    }

    private EmployeeEntity employee(Long employeeId) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        return employee;
    }

    private LeaveEntity leave(String code, String name) {
        LeaveEntity leave = new LeaveEntity();
        leave.setLeaveCode(code);
        leave.setLeaveName(name);
        return leave;
    }
}
