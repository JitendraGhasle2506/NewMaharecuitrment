package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_relieving")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRelievingEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relieving_id")
    private Long relievingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(name = "reason_of_relieving")
    private String reasonOfRelieving; // Released, PIP, Return of Service

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "handover_given_to_id")
    private Long handoverGivenToId;

    @Column(name = "status")
    private String status; // e.g., INITIATED, APPROVED, COMPLETED

    @Column(name = "remarks", length = 500)
    private String remarks;
}
