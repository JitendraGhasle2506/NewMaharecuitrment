package com.maharecruitment.gov.in.master.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "sub_department",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "department_id", "sub_dept_name" })
        })
@Getter
@Setter
public class SubDepartment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subDeptId;

    @Column(name = "sub_dept_name", nullable = false, length = 100)
    private String subDeptName;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentMst department;

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (subDeptName != null) {
            subDeptName = subDeptName.trim();
        }
    }
}
