package com.maharecruitment.gov.in.asset.entity;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "asset_allocation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private AssetEntity asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(name = "allocated_date", nullable = false)
    private LocalDate allocatedDate;

    @Column(name = "returned_date")
    private LocalDate returnedDate;

    @Column(name = "allocation_status", nullable = false, length = 50)
    private String allocationStatus = "ASSIGNED"; // ASSIGNED, RETURNED, PENDING_RETURN

    @Column(name = "remarks", length = 1000)
    private String remarks;
}
