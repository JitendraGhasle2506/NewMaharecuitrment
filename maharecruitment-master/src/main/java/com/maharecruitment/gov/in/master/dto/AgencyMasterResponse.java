package com.maharecruitment.gov.in.master.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.maharecruitment.gov.in.master.entity.AgencyBankAccountType;
import com.maharecruitment.gov.in.master.entity.AgencyEntityType;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyMasterResponse {

    private Long agencyId;
    private String agencyName;
    private String officialEmail;
    private String telephoneNumber;
    private String agencyType;
    private String officialAddress;
    private String permanentAddress;
    private AgencyEntityType entityType;
    private String panNumber;
    private String panCopyPath;
    private String certificateNumber;
    private String certificateDocumentPath;
    private String gstNumber;
    private String gstDocumentPath;
    private String contactPersonName;
    private String contactPersonMobileNo;
    private Boolean msmeRegistered;
    private String bankName;
    private String bankBranch;
    private String bankAccountNumber;
    private AgencyBankAccountType bankAccountType;
    private String ifscCode;
    private String cancelledChequePath;
    private AgencyStatus status;
    private List<AgencyEscalationMatrixResponse> escalationMatrixEntries = new ArrayList<>();
    private LocalDateTime createdDateTime;
    private LocalDateTime updatedDateTime;
    private String provisionedUserEmail;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String temporaryPassword;
    private Boolean agencyUserCreated;
}
