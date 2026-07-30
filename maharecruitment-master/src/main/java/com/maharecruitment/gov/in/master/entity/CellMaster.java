package com.maharecruitment.gov.in.master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "m_cell_master",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_m_cell_master_cell_name", columnNames = "cell_name")
        },
        indexes = {
                @Index(name = "idx_m_cell_master_wing_id", columnList = "wing_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CellMaster extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cell_id")
    private Long cellId;

    @NotBlank(message = "Cell name is required")
    @Column(name = "cell_name", nullable = false, length = 100)
    private String cellName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wing_id", nullable = false)
    private WingMaster wing;

    @Column(name = "active_flag", nullable = false, length = 1)
    @Builder.Default
    private String activeFlag = "Y";

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (cellName != null) {
            cellName = cellName.trim();
        }
        activeFlag = "N".equalsIgnoreCase(activeFlag) ? "N" : "Y";
    }
}
