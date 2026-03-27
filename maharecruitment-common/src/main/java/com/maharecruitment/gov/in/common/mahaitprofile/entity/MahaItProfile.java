package com.maharecruitment.gov.in.common.mahaitprofile.entity;

import java.time.LocalDateTime;
import java.util.Locale;

import com.maharecruitment.gov.in.auth.entity.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mahait_profile_master", indexes = {
        @Index(name = "idx_mahait_profile_name", columnList = "profile_name"),
        @Index(name = "idx_mahait_company_name", columnList = "company_name"),
        @Index(name = "idx_mahait_cin_number", columnList = "cin_number"),
        @Index(name = "idx_mahait_profile_updated_date", columnList = "updated_date")
})
public class MahaItProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mahait_profile_id")
    private Long mahaItProfileId;

    @Column(name = "profile_name", nullable = false, length = 150)
    private String profileName = "MahaIT Profile";

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "company_address", nullable = false, length = 1000)
    private String companyAddress;

    @Column(name = "cin_number", nullable = false, length = 21)
    private String cinNumber;

    @Column(name = "pan_number", nullable = false, length = 10)
    private String panNumber;

    @Column(name = "gst_number", nullable = false, length = 15)
    private String gstNumber;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "branch_name", nullable = false, length = 150)
    private String branchName;

    @Column(name = "account_holder_name", nullable = false, length = 150)
    private String accountHolderName;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        profileName = trim(profileName);
        companyName = trim(companyName);
        companyAddress = trim(companyAddress);
        cinNumber = upper(trim(cinNumber));
        panNumber = upper(trim(panNumber));
        gstNumber = upper(trim(gstNumber));
        bankName = trim(bankName);
        branchName = trim(branchName);
        accountHolderName = trim(accountHolderName);
        accountNumber = trim(accountNumber);
        ifscCode = upper(trim(ifscCode));
        active = !Boolean.FALSE.equals(active);

        if (getCreatedDate() == null) {
            setCreatedDate(LocalDateTime.now());
        }
        if (getUpdatedDate() == null) {
            setUpdatedDate(getCreatedDate());
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
