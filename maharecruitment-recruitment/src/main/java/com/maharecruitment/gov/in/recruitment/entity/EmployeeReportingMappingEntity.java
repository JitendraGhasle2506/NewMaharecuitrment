package com.maharecruitment.gov.in.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_reporting_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeReportingMappingEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;

    @Column(name = "hod_user_id", nullable = false)
    private Long hodUserId;

    @Column(name = "manager_type", nullable = false, length = 10)
    private String managerType; // STM or PM

    @Column(name = "manager_employee_id")
    private Long managerEmployeeId; // The STM or PM's Employee ID

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId; // The Assigned Internal Employee ID
}
