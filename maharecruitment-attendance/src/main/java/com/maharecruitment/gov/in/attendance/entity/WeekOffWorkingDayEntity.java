package com.maharecruitment.gov.in.attendance.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "week_off_working_day")
public class WeekOffWorkingDayEntity extends AttendanceAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "working_date", nullable = false)
    private LocalDate workingDate;

    @Column(name = "office_order_original_name", nullable = false, length = 255)
    private String officeOrderOriginalName;

    @Column(name = "office_order_stored_name", nullable = false, length = 255)
    private String officeOrderStoredName;

    @Column(name = "office_order_path", nullable = false, length = 1000)
    private String officeOrderPath;

    @Column(name = "office_order_content_type", length = 150)
    private String officeOrderContentType;

    @Column(name = "office_order_file_size")
    private Long officeOrderFileSize;

    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @PrePersist
    @PreUpdate
    void normalize() {
        active = !Boolean.FALSE.equals(active);
    }
}
