package com.maharecruitment.gov.in.web.service.mobile;

import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.mobile.MobilePasswordUpdateRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobilePasswordUpdateResponse;
import com.maharecruitment.gov.in.web.dto.mobile.MobileProfileContactUpdateRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileProfileResponse;

public interface MobileProfileService {

    MobileProfileResponse getProfile(Long employeeId);

    MobileProfileResponse updateContact(MobileProfileContactUpdateRequest request);

    MobileProfileResponse updatePhoto(Long employeeId, MultipartFile photo, String embedding);

    MobilePasswordUpdateResponse changePassword(MobilePasswordUpdateRequest request);

    MobilePasswordUpdateResponse resetPassword(MobilePasswordUpdateRequest request);
}
