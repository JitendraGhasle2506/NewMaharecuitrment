package com.maharecruitment.gov.in.web.dto.agency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AgencyCandidateBatchFormValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validateRejectsInvalidCandidateEmail() {
        AgencyCandidateBatchForm form = buildValidForm();
        form.getCandidates().get(0).setEmail("invalid-email");

        Set<ConstraintViolation<AgencyCandidateBatchForm>> violations = validator.validate(form);

        assertEquals("Candidate email must be valid", findViolationMessage(violations, "candidates[0].email"));
    }

    @Test
    void validateRejectsInvalidCandidateMobile() {
        AgencyCandidateBatchForm form = buildValidForm();
        form.getCandidates().get(0).setMobile("98765");

        Set<ConstraintViolation<AgencyCandidateBatchForm>> violations = validator.validate(form);

        assertEquals(
                "Candidate mobile must be 10 digits",
                findViolationMessage(violations, "candidates[0].mobile"));
    }

    @Test
    void validateRejectsCandidateNameStartingWithSpace() {
        AgencyCandidateBatchForm form = buildValidForm();
        form.getCandidates().get(0).setCandidateName(" Test");

        Set<ConstraintViolation<AgencyCandidateBatchForm>> violations = validator.validate(form);

        assertEquals("Candidate name must not start with a space", findViolationMessage(violations, "candidates[0].candidateName"));
    }

    @Test
    void validateRejectsCandidateNameContainingNumbers() {
        AgencyCandidateBatchForm form = buildValidForm();
        form.getCandidates().get(0).setCandidateName("Test1");

        Set<ConstraintViolation<AgencyCandidateBatchForm>> violations = validator.validate(form);

        assertEquals("Candidate name must not contain numbers", findViolationMessage(violations, "candidates[0].candidateName"));
    }

    @Test
    void validateRejectsCandidateNameTooShort() {
        AgencyCandidateBatchForm form = buildValidForm();
        form.getCandidates().get(0).setCandidateName("T");

        Set<ConstraintViolation<AgencyCandidateBatchForm>> violations = validator.validate(form);

        assertEquals("Candidate name must be between 2 and 100 characters", findViolationMessage(violations, "candidates[0].candidateName"));
    }

    @Test
    void validateRejectsCandidateNameTooLong() {
        AgencyCandidateBatchForm form = buildValidForm();
        form.getCandidates().get(0).setCandidateName("A".repeat(101));

        Set<ConstraintViolation<AgencyCandidateBatchForm>> violations = validator.validate(form);

        assertEquals("Candidate name must be between 2 and 100 characters", findViolationMessage(violations, "candidates[0].candidateName"));
    }

    @Test
    void validateAcceptsValidCandidateContactDetails() {
        AgencyCandidateBatchForm form = buildValidForm();

        Set<ConstraintViolation<AgencyCandidateBatchForm>> violations = validator.validate(form);

        assertTrue(violations.isEmpty());
    }

    private AgencyCandidateBatchForm buildValidForm() {
        AgencyCandidateRowForm rowForm = new AgencyCandidateRowForm();
        rowForm.setCandidateName("Test Candidate");
        rowForm.setEmail("candidate@example.com");
        rowForm.setMobile("9876543210");
        rowForm.setCandidateEducation("B.E.");
        rowForm.setTotalExp(new BigDecimal("5.0"));
        rowForm.setRelevantExp(new BigDecimal("3.0"));
        rowForm.setJoiningTime("Immediate");

        AgencyCandidateBatchForm form = new AgencyCandidateBatchForm();
        form.setDesignationVacancyId(1L);
        form.getCandidates().add(rowForm);
        return form;
    }

    private String findViolationMessage(
            Set<ConstraintViolation<AgencyCandidateBatchForm>> violations,
            String propertyPath) {
        return violations.stream()
                .filter(violation -> propertyPath.equals(violation.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected violation for " + propertyPath));
    }
}
