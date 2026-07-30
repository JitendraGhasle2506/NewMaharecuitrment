package com.maharecruitment.gov.in.master.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "rate_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateMaster extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rate_master_id")
    private Long rateMasterId;

    @NotBlank(message = "Type is required")
    @Column(name = "type", nullable = false, length = 50, unique = true)
    private String type;

    @NotNull(message = "Rate is required")
    @Column(name = "rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal rate;

    @Column(name = "active_flag", length = 1, nullable = false)
    @Builder.Default
    private String activeFlag = "Y";

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (type != null) {
            type = type.trim().toUpperCase();
        }
        activeFlag = (activeFlag == null || !"N".equalsIgnoreCase(activeFlag)) ? "Y" : "N";
    }
}
