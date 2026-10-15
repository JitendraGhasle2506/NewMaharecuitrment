package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.DepartmentMst;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee_additional_charge")
@Getter
@Setter
public class AdditionalCharge extends RecruitmentAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private EmployeeEntity employee;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_employee_id")
    private EmployeeEntity replacedEmployee;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_user_id")
    private User replacedUser;
    @ElementCollection
    @CollectionTable(name = "additional_charge_team", joinColumns = @JoinColumn(name = "charge_id"))
    @Column(name = "employee_id", nullable = false)
    @org.hibernate.annotations.BatchSize(size = 20)
    private Set<Long> teamEmployeeIds = new LinkedHashSet<>();
    @Column(nullable = false, length = 100)
    private String position;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentMst department;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cell_id")
    private CellMaster cell;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "order_reference", length = 150)
    private String orderReference;
    @Column(length = 1000)
    private String responsibilities;
    @Column(name = "assigned_by", nullable = false, length = 255)
    private String assignedBy;
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    @Column(name = "revoked_by", length = 255)
    private String revokedBy;
    @Version
    private Long version;
}
