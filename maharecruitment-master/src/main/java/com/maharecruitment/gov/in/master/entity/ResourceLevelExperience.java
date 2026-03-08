package com.maharecruitment.gov.in.master.entity;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resource_level_experience_mst")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceLevelExperience extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long levelId;

    @NotBlank(message = "Level code is required")
    @Column(name = "level_code", nullable = false, unique = true, length = 10)
    private String levelCode;

    @NotBlank(message = "Level name is required")
    @Column(name = "level_name", nullable = false, length = 50)
    private String levelName;

    @NotNull(message = "Minimum experience is required")
    @Column(name = "min_experience", nullable = false, precision = 4, scale = 2)
    private BigDecimal minExperience;

    @NotNull(message = "Maximum experience is required")
    @Column(name = "max_experience", nullable = false, precision = 4, scale = 2)
    private BigDecimal maxExperience;

    @Column(name = "active_flag", length = 1, nullable = false)
    @Builder.Default
    private String activeFlag = "Y";

    @ManyToMany(mappedBy = "levels", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ManpowerDesignationMaster> designations = new HashSet<>();

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (levelCode != null) {
            levelCode = levelCode.trim().toUpperCase();
        }
        if (levelName != null) {
            levelName = levelName.trim();
        }
        activeFlag = (activeFlag == null || !"N".equalsIgnoreCase(activeFlag)) ? "Y" : "N";
    }
}

