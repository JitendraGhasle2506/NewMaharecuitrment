package com.maharecruitment.gov.in.auth.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "department_registration_master")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentRegistrationEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_registration_id")
    private Long departmentRegistrationId;

    @Column(name = "department_name", length = 200)
    private String departmentName;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "sub_department_id")
    private Long subDeptId;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "active")
    private Boolean active = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DepartmentContactEntity> contacts = new ArrayList<>();

    private String billDepartmentName;

    private String gstNo;

    private String panNo;

    @Column(name = "tan_no")
    private String tanNo;

    private String billAddress;

    @Column(name = "gst_file_path")
    private String gstFilePath;

    @Column(name = "pan_file_path")
    private String panFilePath;

    @Column(name = "tan_file_path")
    private String tanFilePath;

    @Column(name = "is_terms_condition_accepted")
    private Boolean isTermsConditionAccepted;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        normalizeFields();
    }

    @PreUpdate
    public void onUpdate() {
        normalizeFields();
    }

    public void addContact(DepartmentContactEntity contact) {
        contacts.add(contact);
        contact.setDepartment(this);
    }

    public void clearContacts() {
        contacts.forEach(contact -> contact.setDepartment(null));
        contacts.clear();
    }

    private void normalizeFields() {
        if (departmentName != null) {
            departmentName = departmentName.trim();
        }
        if (address != null) {
            address = address.trim();
        }
        if (billDepartmentName != null) {
            billDepartmentName = billDepartmentName.trim();
        }
        if (gstNo != null) {
            gstNo = gstNo.trim().toUpperCase();
        }
        if (panNo != null) {
            panNo = panNo.trim().toUpperCase();
        }
        if (tanNo != null) {
            tanNo = tanNo.trim().toUpperCase();
        }
        if (billAddress != null) {
            billAddress = billAddress.trim();
        }
        isTermsConditionAccepted = Boolean.TRUE.equals(isTermsConditionAccepted);
        active = Boolean.TRUE.equals(active);
    }
}
