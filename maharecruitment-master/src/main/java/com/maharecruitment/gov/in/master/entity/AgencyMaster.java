package com.maharecruitment.gov.in.master.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "agency_master",
        indexes = {
                @Index(name = "idx_agency_master_name", columnList = "agency_name"),
                @Index(name = "idx_agency_master_email", columnList = "official_email"),
                @Index(name = "idx_agency_master_status", columnList = "status")
        })
public class AgencyMaster extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agency_id")
    private Long agencyId;

    @Column(name = "agency_name", nullable = false, length = 200)
    private String agencyName;

    @Column(name = "official_email", nullable = false, length = 255)
    private String officialEmail;

    @Column(name = "telephone_number", nullable = false, length = 15)
    private String telephoneNumber;

    @Column(name = "agency_type", nullable = false, length = 100)
    private String agencyType;

    @Column(name = "official_address", nullable = false, length = 500)
    private String officialAddress;

    @Column(name = "permanent_address", nullable = false, length = 500)
    private String permanentAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private AgencyEntityType entityType;

    @Column(name = "pan_number", nullable = false, length = 10)
    private String panNumber;

    @Column(name = "pan_copy_path", nullable = false, length = 500)
    private String panCopyPath;

    @Column(name = "certificate_number", nullable = false, length = 100)
    private String certificateNumber;

    @Column(name = "certificate_document_path", nullable = false, length = 500)
    private String certificateDocumentPath;

    @Column(name = "gst_number", nullable = false, length = 15)
    private String gstNumber;

    @Column(name = "gst_document_path", nullable = false, length = 500)
    private String gstDocumentPath;

    @Column(name = "contact_person_name", nullable = false, length = 150)
    private String contactPersonName;

    @Column(name = "contact_person_mobile_no", nullable = false, length = 10)
    private String contactPersonMobileNo;

    @Column(name = "is_msme_registered", nullable = false)
    private Boolean msmeRegistered;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "bank_branch", nullable = false, length = 150)
    private String bankBranch;

    @Column(name = "bank_account_number", nullable = false, length = 30)
    private String bankAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_account_type", nullable = false, length = 20)
    private AgencyBankAccountType bankAccountType;

    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;

    @Column(name = "cancelled_cheque_path", nullable = false, length = 500)
    private String cancelledChequePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgencyStatus status = AgencyStatus.ACTIVE;

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AgencyEscalationMatrix> escalationMatrixEntries = new ArrayList<>();

    public void replaceEscalationMatrixEntries(List<AgencyEscalationMatrix> entries) {
        escalationMatrixEntries.clear();
        if (entries == null) {
            return;
        }
        entries.forEach(this::addEscalationMatrixEntry);
    }

    public void addEscalationMatrixEntry(AgencyEscalationMatrix entry) {
        entry.setAgency(this);
        escalationMatrixEntries.add(entry);
    }
}
