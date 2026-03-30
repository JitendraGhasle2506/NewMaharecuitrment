package com.maharecruitment.gov.in.auth.service;

import com.maharecruitment.gov.in.auth.dto.UserPasswordChangeRequest;
import com.maharecruitment.gov.in.auth.dto.UserProfileUpdateRequest;
import com.maharecruitment.gov.in.auth.dto.UserProfileView;

public interface CurrentUserProfileService {

    UserProfileView getProfile(String email);

    UserProfileView updateProfile(String email, UserProfileUpdateRequest request);

    void changePassword(String email, UserPasswordChangeRequest request);
}
