package com.maharecruitment.gov.in.master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "agency_escalation_matrix")
public class AgencyEscalationMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "escalation_matrix_id")
    private Long escalationMatrixId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private AgencyMaster agency;

    @Column(name = "contact_name", nullable = false, length = 150)
    private String contactName;

    @Column(name = "mobile_number", nullable = false, length = 10)
    private String mobileNumber;

    @Column(name = "escalation_level", nullable = false, length = 50)
    private String level;

    @Column(name = "designation", nullable = false, length = 100)
    private String designation;

    @Column(name = "company_email_id", nullable = false, length = 255)
    private String companyEmailId;
}
