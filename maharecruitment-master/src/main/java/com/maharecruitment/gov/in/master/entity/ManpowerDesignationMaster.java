package com.maharecruitment.gov.in.master.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "manpower_designation_master",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "category", "designation_name" })
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManpowerDesignationMaster extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long designationId;

    @NotBlank(message = "Category is required")
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @NotBlank(message = "Designation name is required")
    @Column(name = "designation_name", nullable = false, length = 200)
    private String designationName;

    @Column(name = "role_name", length = 200)
    private String roleName;

    @Column(name = "educational_qualification", length = 500)
    private String educationalQualification;

    @Column(name = "certification", length = 500)
    private String certification;

    @Column(name = "active_flag", length = 1, nullable = false)
    @Builder.Default
    private String activeFlag = "Y";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "designation_level_map",
            joinColumns = @JoinColumn(name = "designation_id"),
            inverseJoinColumns = @JoinColumn(name = "level_code", referencedColumnName = "level_code"))
    @Builder.Default
    private Set<ResourceLevelExperience> levels = new HashSet<>();

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (category != null) {
            category = category.trim();
        }
        if (designationName != null) {
            designationName = designationName.trim();
        }
        if (roleName != null) {
            roleName = roleName.trim();
        }
        if (educationalQualification != null) {
            educationalQualification = educationalQualification.trim();
        }
        if (certification != null) {
            certification = certification.trim();
        }
        activeFlag = (activeFlag == null || !"N".equalsIgnoreCase(activeFlag)) ? "Y" : "N";
    }
}

