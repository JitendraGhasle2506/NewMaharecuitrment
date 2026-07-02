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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "m_location_master", uniqueConstraints = {
        @UniqueConstraint(name = "uk_m_location_master_location_name", columnNames = "location_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationMaster extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long locationId;

    @NotBlank(message = "Address is required")
    @Column(name = "location_name", nullable = false, length = 150)
    private String locationName;

    @Column(name = "office_name", length = 150)
    private String officeName;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "radius_meters", nullable = false)
    @Builder.Default
    private Integer radiusMeters = 100;

    @Column(name = "active_flag", nullable = false, length = 1)
    @Builder.Default
    private String activeFlag = "Y";

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (locationName != null) {
            locationName = locationName.trim();
        }
        if (officeName != null) {
            officeName = officeName.trim();
        }
        if (radiusMeters == null) {
            radiusMeters = 100;
        }
        activeFlag = "N".equalsIgnoreCase(activeFlag) ? "N" : "Y";
    }
}
