package com.maharecruitment.gov.in.common.mahaitprofile.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MahaItProfileResponse {

    private Long mahaItProfileId;
    private String profileName;
    private String companyName;
    private String companyAddress;
    private String cinNumber;
    private String panNumber;
    private String gstNumber;
    private String bankName;
    private String branchName;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private Boolean active;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
}
