package com.maharecruitment.gov.in.common.mahaitprofile.dto;

import java.time.LocalDateTime;

import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfileAuditAction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MahaItProfileAuditResponse {

    private Long auditId;
    private Long mahaItProfileId;
    private MahaItProfileAuditAction actionType;
    private Long actorUserId;
    private String actorUsername;
    private LocalDateTime actionTimestamp;
    private String details;
}
