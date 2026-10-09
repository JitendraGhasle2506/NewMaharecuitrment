package com.maharecruitment.gov.in.recruitment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import com.maharecruitment.gov.in.recruitment.dto.hr.InternalVacancyOpeningForm;
import com.maharecruitment.gov.in.recruitment.dto.hr.InternalVacancyRequirementForm;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyHiringRequestType;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyAssessmentService;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyCandidateReviewService;
import com.maharecruitment.gov.in.recruitment.service.InternalVacancyOpeningService;

class HRInternalVacancyOpeningControllerValidationTest {

    private final HRInternalVacancyOpeningController controller = new HRInternalVacancyOpeningController(
            mock(InternalVacancyOpeningService.class),
            mock(InternalVacancyCandidateReviewService.class),
            mock(InternalVacancyAssessmentService.class));

    @Test
    void openActionRejectsMissingSubmissionDetailsAndInterviewAuthorities() {
        InternalVacancyOpeningForm form = new InternalVacancyOpeningForm();
        form.setHiringRequestType(InternalVacancyHiringRequestType.NEW_CANDIDATE);
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "openingForm");

        validate(form, InternalVacancyOpeningStatus.OPEN, bindingResult);

        assertThat(bindingResult.getFieldError("eOfficeApprovalDocument")).isNotNull();
        assertThat(bindingResult.getFieldError("requirements")).isNotNull();
        assertThat(bindingResult.getFieldError("interviewAuthorityUserIds")).isNotNull();
        assertThat(bindingResult.getFieldError("levelTwoInterviewAuthorityUserIds")).isNotNull();
    }

    @Test
    void openActionAcceptsCompleteValidSelections() {
        InternalVacancyOpeningForm form = new InternalVacancyOpeningForm();
        form.setHiringRequestType(InternalVacancyHiringRequestType.NEW_CANDIDATE);
        form.setExistingEOfficeApprovalFileName("approval.pdf");
        form.setRequirements(List.of(validRequirement()));
        form.setInterviewAuthorityUserIds(List.of(10L));
        form.setLevelTwoInterviewAuthorityUserIds(List.of(20L, 21L));
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "openingForm");

        validate(form, InternalVacancyOpeningStatus.OPEN, bindingResult);

        assertThat(bindingResult.hasErrors()).isFalse();
    }

    @Test
    void pendingReplacementRequiresAnEmployeeButNotInterviewAuthorities() {
        InternalVacancyOpeningForm form = new InternalVacancyOpeningForm();
        form.setHiringRequestType(InternalVacancyHiringRequestType.EMPLOYEE_REPLACEMENT);
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "openingForm");

        validate(form, InternalVacancyOpeningStatus.PENDING_HR_APPROVAL, bindingResult);

        assertThat(bindingResult.getFieldError("replacementEmployeeIds")).isNotNull();
        assertThat(bindingResult.getFieldError("interviewAuthorityUserIds")).isNull();
        assertThat(bindingResult.getFieldError("levelTwoInterviewAuthorityUserIds")).isNull();
    }

    private void validate(
            InternalVacancyOpeningForm form,
            InternalVacancyOpeningStatus status,
            BindingResult bindingResult) {
        ReflectionTestUtils.invokeMethod(controller, "validateFormForAction", form, status, bindingResult);
    }

    private InternalVacancyRequirementForm validRequirement() {
        InternalVacancyRequirementForm requirement = new InternalVacancyRequirementForm();
        requirement.setDesignationId(1L);
        requirement.setDesignationName("Developer");
        requirement.setLevelCode("L1");
        requirement.setLevelName("Level 1");
        requirement.setNumberOfVacancy(1L);
        return requirement;
    }
}
