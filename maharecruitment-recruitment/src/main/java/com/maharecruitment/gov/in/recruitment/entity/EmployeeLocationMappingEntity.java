package com.maharecruitment.gov.in.recruitment.entity;

import com.maharecruitment.gov.in.master.entity.LocationMaster;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "employee_location_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_location_mapping_employee_location",
                        columnNames = { "employee_id", "location_id" })
        },
        indexes = {
                @Index(name = "idx_employee_location_mapping_employee", columnList = "employee_id"),
                @Index(name = "idx_employee_location_mapping_location", columnList = "location_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class EmployeeLocationMappingEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_location_mapping_id")
    private Long employeeLocationMappingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private LocationMaster location;

    @Column(name = "is_primary", nullable = false)
    private Boolean primaryLocation = false;
}
