package com.maharecruitment.gov.in.recruitment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class RecruitmentInterviewDetailEntityTest {

    @Test
    void immediateJoiningUsesLastWorkingDayWhenProvided() {
        RecruitmentInterviewDetailEntity candidate = candidate("Immediate");
        candidate.setLastWorkingDay(LocalDate.of(2026, 10, 31));

        assertThat(candidate.getTentativeJoiningDate()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void delayedJoiningIsCalculatedFromLastWorkingDayWhenProvided() {
        RecruitmentInterviewDetailEntity candidate = candidate("30 days");
        candidate.setLastWorkingDay(LocalDate.of(2026, 10, 31));

        assertThat(candidate.getTentativeJoiningDate()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void delayedJoiningUsesSubmissionDateWhenLastWorkingDayIsAbsent() {
        RecruitmentInterviewDetailEntity candidate = candidate("15 days");

        assertThat(candidate.getTentativeJoiningDate()).isEqualTo(LocalDate.of(2026, 10, 28));
    }

    private RecruitmentInterviewDetailEntity candidate(String joiningTime) {
        RecruitmentInterviewDetailEntity candidate = new RecruitmentInterviewDetailEntity();
        candidate.setJoiningTime(joiningTime);
        candidate.setCreatedDateTime(LocalDateTime.of(2026, 10, 13, 9, 30));
        return candidate;
    }
}
