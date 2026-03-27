package com.maharecruitment.gov.in.common.mahaitprofile.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.common.mahaitprofile.dto.MahaItProfileAuditResponse;
import com.maharecruitment.gov.in.common.mahaitprofile.dto.MahaItProfileRequest;
import com.maharecruitment.gov.in.common.mahaitprofile.dto.MahaItProfileResponse;

public interface MahaItProfileService {

    Page<MahaItProfileResponse> getAll(Pageable pageable);

    MahaItProfileResponse getById(Long mahaitProfileId);

    MahaItProfileResponse create(MahaItProfileRequest request);

    MahaItProfileResponse update(Long mahaitProfileId, MahaItProfileRequest request);

    List<MahaItProfileAuditResponse> getAuditTrail(Long mahaitProfileId);
}
