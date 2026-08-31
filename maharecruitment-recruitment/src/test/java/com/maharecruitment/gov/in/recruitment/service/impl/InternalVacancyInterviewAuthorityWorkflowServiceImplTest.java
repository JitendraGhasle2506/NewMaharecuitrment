package com.maharecruitment.gov.in.recruitment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyInterviewAuthorityEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyInterviewEmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyInterviewRoleEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentCandidateStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInterviewDetailEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentAssessmentFeedbackRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInternalLevelTwoScheduleRepository;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyAssessmentService;
import com.maharecruitment.gov.in.recruitment.service.model.DepartmentInterviewWorkflowDetailView;

class InternalVacancyInterviewAuthorityWorkflowServiceImplTest {

    private static final String REQUEST_ID = "REQ-20260825-I0001";
    private static final String ACTOR_EMAIL = "alice@example.com";

    @Test
    void feedbackDetailShowsAssignedAuthorityNamesAndDesignations() {
        Role projectManagerRole = new Role();
        projectManagerRole.setId(2L);
        projectManagerRole.setName("ROLE_PROJECT_MANAGER");

        User alice = new User();
        alice.setId(15L);
        alice.setName("Alice Patil");
        alice.setEmail(ACTOR_EMAIL);
        alice.addRole(projectManagerRole);

        EmployeeEntity bob = new EmployeeEntity();
        bob.setEmployeeId(22L);
        bob.setFullName("Bob Jadhav");
        bob.setEmail("bob@example.com");
        bob.setLevelCode("l3");
        ManpowerDesignationMaster designation = new ManpowerDesignationMaster();
        designation.setDesignationName("Solution Architect");
        bob.setDesignation(designation);

        InternalVacancyOpeningEntity opening = new InternalVacancyOpeningEntity();
        opening.setInternalVacancyOpeningId(9L);
        InternalVacancyInterviewRoleEntity roleAssignment = new InternalVacancyInterviewRoleEntity();
        roleAssignment.setRole(projectManagerRole);
        opening.addInterviewRole(roleAssignment);
        InternalVacancyInterviewAuthorityEntity userAssignment = new InternalVacancyInterviewAuthorityEntity();
        userAssignment.setUser(alice);
        opening.addInterviewAuthority(userAssignment);
        InternalVacancyInterviewEmployeeEntity employeeAssignment = new InternalVacancyInterviewEmployeeEntity();
        employeeAssignment.setEmployee(bob);
        opening.addInterviewEmployee(employeeAssignment);

        RecruitmentNotificationEntity notification = new RecruitmentNotificationEntity();
        notification.setRecruitmentNotificationId(21L);
        notification.setRequestId(REQUEST_ID);
        notification.setInternalVacancyOpening(opening);

        RecruitmentInterviewDetailEntity candidate = new RecruitmentInterviewDetailEntity();
        candidate.setRecruitmentInterviewDetailId(66L);
        candidate.setRecruitmentNotification(notification);
        candidate.setCandidateStatus(RecruitmentCandidateStatus.INTERVIEW_SCHEDULED_BY_AGENCY);

        UserRepository userRepository = mock(UserRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        RecruitmentInterviewDetailRepository interviewDetailRepository =
                mock(RecruitmentInterviewDetailRepository.class);
        RecruitmentAssessmentFeedbackRepository feedbackRepository =
                mock(RecruitmentAssessmentFeedbackRepository.class);
        RecruitmentInternalLevelTwoScheduleRepository scheduleRepository =
                mock(RecruitmentInternalLevelTwoScheduleRepository.class);
        InternalVacancyAssessmentService assessmentService = mock(InternalVacancyAssessmentService.class);

        when(userRepository.findByEmailIgnoreCase(ACTOR_EMAIL)).thenReturn(Optional.of(alice));
        when(employeeRepository.findByUser_Id(15L)).thenReturn(Optional.empty());
        when(interviewDetailRepository.findByIdForInternalVacancyInterviewWorkflowView(
                REQUEST_ID,
                66L,
                15L,
                null)).thenReturn(Optional.of(candidate));

        InternalVacancyInterviewAuthorityWorkflowServiceImpl service =
                new InternalVacancyInterviewAuthorityWorkflowServiceImpl(
                        userRepository,
                        employeeRepository,
                        interviewDetailRepository,
                        feedbackRepository,
                        scheduleRepository,
                        assessmentService);

        DepartmentInterviewWorkflowDetailView result =
                service.getInterviewWorkflowDetail(ACTOR_EMAIL, REQUEST_ID, 66L);

        assertEquals(
                List.of("Alice Patil", "Bob Jadhav"),
                result.getInterviewAuthorities().stream().map(authority -> authority.getName()).toList());
        assertEquals(
                List.of("PROJECT MANAGER", "Solution Architect (L3)"),
                result.getInterviewAuthorities().stream().map(authority -> authority.getDesignation()).toList());
    }

    @Test
    void feedbackTemplateRendersInterviewAuthorityTable() throws Exception {
        String template = new ClassPathResource(
                "templates/interview-authority/internal-vacancy-feedback-form.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(template.contains("Interview Authorities"));
        assertTrue(template.contains("workflowDetail.interviewAuthorities"));
        assertTrue(template.contains("authority.name"));
        assertTrue(template.contains("authority.designation"));
    }
}
