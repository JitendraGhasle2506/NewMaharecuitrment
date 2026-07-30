package com.maharecruitment.gov.in.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "internal_vacancy_interview_employee",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_internal_vacancy_interview_employee",
                        columnNames = { "internal_vacancy_opening_id", "employee_id" })
        },
        indexes = {
                @Index(
                        name = "idx_internal_vacancy_interview_employee_opening",
                        columnList = "internal_vacancy_opening_id"),
                @Index(
                        name = "idx_internal_vacancy_interview_employee_emp",
                        columnList = "employee_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InternalVacancyInterviewEmployeeEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_vacancy_interview_employee_id")
    private Long internalVacancyInterviewEmployeeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internal_vacancy_opening_id", nullable = false)
    private InternalVacancyOpeningEntity opening;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;
}
