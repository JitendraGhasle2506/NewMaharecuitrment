package com.maharecruitment.gov.in.master.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "department_mst",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "department_name" })
        })
@Getter
@Setter
public class DepartmentMst extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long departmentId;

    @Column(name = "department_name", nullable = false, length = 100)
    private String departmentName;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<SubDepartment> subDepartments = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (departmentName != null) {
            departmentName = departmentName.trim();
        }
    }
}
