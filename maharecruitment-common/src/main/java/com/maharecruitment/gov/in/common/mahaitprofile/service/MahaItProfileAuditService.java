package com.maharecruitment.gov.in.common.mahaitprofile.service;

import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfileAuditAction;

public interface MahaItProfileAuditService {

    void log(Long mahaitProfileId, MahaItProfileAuditAction action, String details);
}
