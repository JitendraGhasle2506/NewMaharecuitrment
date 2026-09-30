package com.maharecruitment.gov.in.web.service.profile;

import com.maharecruitment.gov.in.web.dto.profile.UserProfileForm;

public interface CommonProfileUpdateService {

    CommonProfileDetails getDetails(Long userId);

    CommonProfileUpdateResult update(String currentEmail, UserProfileForm form);
}
