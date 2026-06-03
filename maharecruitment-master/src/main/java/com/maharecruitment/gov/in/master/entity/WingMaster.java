package com.maharecruitment.gov.in.master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "m_wing_master", uniqueConstraints = {
        @UniqueConstraint(name = "uk_m_wing_master_wing_name", columnNames = "wing_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WingMaster extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wing_id")
    private Long wingId;

    @NotBlank(message = "Wing name is required")
    @Column(name = "wing_name", nullable = false, length = 100)
    private String wingName;

    @Column(name = "active_flag", nullable = false, length = 1)
    @Builder.Default
    private String activeFlag = "Y";

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (wingName != null) {
            wingName = wingName.trim();
        }
        activeFlag = "N".equalsIgnoreCase(activeFlag) ? "N" : "Y";
    }
}
