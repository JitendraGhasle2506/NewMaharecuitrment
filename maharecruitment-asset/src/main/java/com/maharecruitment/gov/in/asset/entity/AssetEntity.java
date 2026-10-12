package com.maharecruitment.gov.in.asset.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "asset_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private AssetCategoryEntity assetCategory;

    @Column(name = "asset_code", unique = true, nullable = false, length = 100)
    private String assetCode;

    @Column(name = "asset_name", nullable = false, length = 150)
    private String assetName;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "warranty_expiry_date")
    private LocalDate warrantyExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_type", length = 10)
    private AssetOwnershipType ownershipType = AssetOwnershipType.OWNED;

    @Column(name = "rent_start_date")
    private LocalDate rentStartDate;

    @Column(name = "rent_end_date")
    private LocalDate rentEndDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "AVAILABLE"; // AVAILABLE, ALLOCATED, DAMAGED, RETIRED, LOST

    @Column(name = "notes", length = 1000)
    private String notes;
}
