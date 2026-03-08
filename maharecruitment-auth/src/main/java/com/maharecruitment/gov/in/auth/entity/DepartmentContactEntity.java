package com.maharecruitment.gov.in.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "department_contact")
@Data
@NoArgsConstructor
public class DepartmentContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_contact_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "department_registration_id")
    private DepartmentRegistrationEntity department;

    @Column(name = "contact_name", nullable = false, length = 150)
    private String contactName;

    @Column(name = "designation", nullable = false, length = 150)
    private String designation;

    @Column(name = "mobile_no", nullable = false, length = 10)
    private String mobileNo;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "is_primary_contact", nullable = false)
    private Boolean primaryContact = false;

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (contactName != null) {
            contactName = contactName.trim();
        }
        if (designation != null) {
            designation = designation.trim();
        }
        if (mobileNo != null) {
            mobileNo = mobileNo.trim();
        }
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        primaryContact = Boolean.TRUE.equals(primaryContact);
    }
}
