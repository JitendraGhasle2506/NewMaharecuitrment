package com.maharecruitment.gov.in.recruitment.entity;

import com.maharecruitment.gov.in.master.entity.CellMaster;

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
        name = "employee_cell_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_cell_mapping_employee",
                        columnNames = "employee_id")
        },
        indexes = {
                @Index(name = "idx_employee_cell_mapping_employee", columnList = "employee_id"),
                @Index(name = "idx_employee_cell_mapping_cell", columnList = "cell_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class EmployeeCellMappingEntity extends RecruitmentAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_cell_mapping_id")
    private Long employeeCellMappingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cell_id", nullable = false)
    private CellMaster cell;
}
